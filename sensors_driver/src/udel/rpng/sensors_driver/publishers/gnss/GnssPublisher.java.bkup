package udel.rpng.sensors_driver.publishers.gnss;

import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import android.location.LocationManager;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.support.annotation.RequiresApi;
import android.os.Build;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import sensor_msgs.NavSatFix;
import sensor_msgs.NavSatStatus;
import geometry_msgs.AccelWithCovarianceStamped;
import udel.rpng.sensors_driver.MainActivity;
import udel.rpng.sensors_driver.R;

public class GnssPublisher implements NodeMain {

    boolean init=true;

    // "Constant used in the location settings dialog."
    // Not sure why this is needed... -pgeneva
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Fused location provider
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;

    LocationManager mLocationManager;
    GnssMeasurementsEvent.Callback mGnssMeasurementsEventCallback;
    GnssNavigationMessage.Callback mGnssNavigationMessageCallback;
    private PseudorangePositionVelocityFromRealTimeEvents
            mPseudorangePositionVelocityFromRealTimeEvents;
    private HandlerThread mPositionVelocityCalculationHandlerThread;  // ThreadのLooper持ち
    private Handler mMyPositionVelocityCalculationHandler;
    double[] posSolution;
    double[] velSolution;
    double[] pvUncertainty;

    double[] mGroundTruth;

    private static final long EARTH_RADIUS_METERS = 6371000;
    private int mCurrentColor = Color.rgb(0x4a, 0x5f, 0x70);


    // My current location
    private Location mCurrentLocation;

    // View objects and the main activity
    private TextView tvLocation;
    private String mLastUpdateTime;
    private String robotName;
    private String TAG = "GnssPublisher";
    private MainActivity mainAct;


    // Our ROS publish node
    private Publisher<NavSatFix> pub_fix;
    private Publisher<AccelWithCovarianceStamped> pub_accel;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public GnssPublisher(MainActivity mainAct, String robotName) {
        // Get our textzone
        this.mainAct = mainAct;
        this.robotName = robotName;
        tvLocation = (TextView) mainAct.findViewById(R.id.titleTextGPS);
        // Set our clients
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mainAct);
        mSettingsClient = LocationServices.getSettingsClient(mainAct);


        /* Velocity */
        mLocationManager = (LocationManager) mainAct.getApplicationContext().getSystemService(mainAct.getApplicationContext().LOCATION_SERVICE);
        mPositionVelocityCalculationHandlerThread =
                new HandlerThread("Position From Realtime Pseudoranges");
        mPositionVelocityCalculationHandlerThread.start();
        mMyPositionVelocityCalculationHandler =
                new Handler(mPositionVelocityCalculationHandlerThread.getLooper());
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    mPseudorangePositionVelocityFromRealTimeEvents = new PseudorangePositionVelocityFromRealTimeEvents();
                }
                catch (Exception e) {
                    Log.e(TAG, " Exception in constructing PseudorangePositionFromRealTimeEvents : ", e);
                }
            }
        };
        mMyPositionVelocityCalculationHandler.post(r);



        // Create a location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.i(TAG,"onLocationResult");
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
//                publishGnssFix(locationResult.getLocations());
                updateUI();
                if(mCurrentLocation.getProvider()==LocationManager.NETWORK_PROVIDER){
                    if (mPseudorangePositionVelocityFromRealTimeEvents == null){
                        return;
                    }
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mPseudorangePositionVelocityFromRealTimeEvents.setReferencePosition(
                                        ((int) (mCurrentLocation.getLatitude() * 1E3)) *10000,
                                        ((int) (mCurrentLocation.getLongitude() * 1E3)) *10000,
                                        ((int) (mCurrentLocation.getAltitude() * 1E3)) *10000);
                            } catch (Exception e) {
                                Log.e(TAG, " Exception setting reference location : ", e);
                            }
                        }
                    };
                    mMyPositionVelocityCalculationHandler.post(r);
                }
                else if (mCurrentLocation.getProvider() == LocationManager.GPS_PROVIDER){
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (mPseudorangePositionVelocityFromRealTimeEvents == null) {
                                return;
                            }
                            double[] posSolution =
                                    mPseudorangePositionVelocityFromRealTimeEvents.getPositionSolutionLatLngDeg();
                            double[] velSolution =
                                    mPseudorangePositionVelocityFromRealTimeEvents.getVelocitySolutionEnuMps();
                            double[] pvUncertainty =
                                    mPseudorangePositionVelocityFromRealTimeEvents
                                            .getPositionVelocityUncertaintyEnu();
                            if (Double.isNaN(posSolution[0])) {
                                logPositionFromRawDataEvent("No Position Calculated Yet");
                                logPositionError("And no offset calculated yet...");
                            } else {
//                                        if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED
//                                                && mResidualPlotStatus != RESIDUAL_MODE_AT_INPUT_LOCATION) {
                                updateGroundTruth(posSolution);
//                                        }
                                String formattedLatDegree = new DecimalFormat("##.######").format(posSolution[0]);
                                String formattedLngDegree = new DecimalFormat("##.######").format(posSolution[1]);
                                String formattedAltMeters = new DecimalFormat("##.#").format(posSolution[2]);
                                logPositionFromRawDataEvent(
                                        "latDegrees = "
                                                + formattedLatDegree
                                                + " lngDegrees = "
                                                + formattedLngDegree
                                                + "altMeters = "
                                                + formattedAltMeters);
                                String formattedVelocityEastMps =
                                        new DecimalFormat("##.###").format(velSolution[0]);
                                String formattedVelocityNorthMps =
                                        new DecimalFormat("##.###").format(velSolution[1]);
                                String formattedVelocityUpMps =
                                        new DecimalFormat("##.###").format(velSolution[2]);
                                logVelocityFromRawDataEvent(
                                        "Velocity East = "
                                                + formattedVelocityEastMps
                                                + "mps"
                                                + " Velocity North = "
                                                + formattedVelocityNorthMps
                                                + "mps"
                                                + "Velocity Up = "
                                                + formattedVelocityUpMps
                                                + "mps");

                                String formattedPosUncertaintyEastMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[0]);
                                String formattedPosUncertaintyNorthMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[1]);
                                String formattedPosUncertaintyUpMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[2]);
                                logPositionUncertainty(
                                        "East = "
                                                + formattedPosUncertaintyEastMeters
                                                + "m North = "
                                                + formattedPosUncertaintyNorthMeters
                                                + "m Up = "
                                                + formattedPosUncertaintyUpMeters
                                                + "m");
                                String formattedVelUncertaintyEastMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[3]);
                                String formattedVelUncertaintyNorthMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[4]);
                                String formattedVelUncertaintyUpMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[5]);
                                logVelocityUncertainty(
                                        "East = "
                                                + formattedVelUncertaintyEastMeters
                                                + "mps North = "
                                                + formattedVelUncertaintyNorthMeters
                                                + "mps Up = "
                                                + formattedVelUncertaintyUpMeters
                                                + "mps");
                                String formattedOffsetMeters =
                                        new DecimalFormat("##.######")
                                                .format(
                                                        getDistanceMeters(
                                                                mCurrentLocation.getLatitude(),
                                                                mCurrentLocation.getLongitude(),
                                                                posSolution[0],
                                                                posSolution[1]));
                                logPositionError("position offset = " + formattedOffsetMeters + " meters");
                                String formattedSpeedOffsetMps =
                                        new DecimalFormat("##.###")
                                                .format(
                                                        Math.abs(
                                                                mCurrentLocation.getSpeed()
                                                                        - Math.sqrt(
                                                                        Math.pow(velSolution[0], 2)
                                                                                + Math.pow(velSolution[1], 2))));
                                logVelocityError("speed offset = " + formattedSpeedOffsetMps + " mps");
                            }
                            logLocationEvent("onLocationChanged: " + mCurrentLocation);
                            publish();
                        }
                    };
                    mMyPositionVelocityCalculationHandler.post(r);
                }
                else{
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (mPseudorangePositionVelocityFromRealTimeEvents == null){
                                return;
                            }
                            try {
                                mPseudorangePositionVelocityFromRealTimeEvents.setReferencePosition(
                                        ((int) (mCurrentLocation.getLatitude() * 1E3)) *10000,
                                        ((int) (mCurrentLocation.getLongitude() * 1E3)) *10000,
                                        ((int) (mCurrentLocation.getAltitude() * 1E3)) *10000);
                            } catch (Exception e) {
                                Log.e(TAG, " Exception setting reference location : ", e);
                            }

                            double[] posSolution =
                                    mPseudorangePositionVelocityFromRealTimeEvents.getPositionSolutionLatLngDeg();
                            double[] velSolution =
                                    mPseudorangePositionVelocityFromRealTimeEvents.getVelocitySolutionEnuMps();
                            double[] pvUncertainty =
                                    mPseudorangePositionVelocityFromRealTimeEvents
                                            .getPositionVelocityUncertaintyEnu();
                            if (Double.isNaN(posSolution[0])) {
                                logPositionFromRawDataEvent("No Position Calculated Yet");
                                logPositionError("And no offset calculated yet...");
                            } else {
//                                        if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED
//                                                && mResidualPlotStatus != RESIDUAL_MODE_AT_INPUT_LOCATION) {
                                updateGroundTruth(posSolution);
//                                        }
                                String formattedLatDegree = new DecimalFormat("##.######").format(posSolution[0]);
                                String formattedLngDegree = new DecimalFormat("##.######").format(posSolution[1]);
                                String formattedAltMeters = new DecimalFormat("##.#").format(posSolution[2]);
                                logPositionFromRawDataEvent(
                                        "latDegrees = "
                                                + formattedLatDegree
                                                + " lngDegrees = "
                                                + formattedLngDegree
                                                + "altMeters = "
                                                + formattedAltMeters);
                                String formattedVelocityEastMps =
                                        new DecimalFormat("##.###").format(velSolution[0]);
                                String formattedVelocityNorthMps =
                                        new DecimalFormat("##.###").format(velSolution[1]);
                                String formattedVelocityUpMps =
                                        new DecimalFormat("##.###").format(velSolution[2]);
                                logVelocityFromRawDataEvent(
                                        "Velocity East = "
                                                + formattedVelocityEastMps
                                                + "mps"
                                                + " Velocity North = "
                                                + formattedVelocityNorthMps
                                                + "mps"
                                                + "Velocity Up = "
                                                + formattedVelocityUpMps
                                                + "mps");

                                String formattedPosUncertaintyEastMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[0]);
                                String formattedPosUncertaintyNorthMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[1]);
                                String formattedPosUncertaintyUpMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[2]);
                                logPositionUncertainty(
                                        "East = "
                                                + formattedPosUncertaintyEastMeters
                                                + "m North = "
                                                + formattedPosUncertaintyNorthMeters
                                                + "m Up = "
                                                + formattedPosUncertaintyUpMeters
                                                + "m");
                                String formattedVelUncertaintyEastMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[3]);
                                String formattedVelUncertaintyNorthMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[4]);
                                String formattedVelUncertaintyUpMeters =
                                        new DecimalFormat("##.###").format(pvUncertainty[5]);
                                logVelocityUncertainty(
                                        "East = "
                                                + formattedVelUncertaintyEastMeters
                                                + "mps North = "
                                                + formattedVelUncertaintyNorthMeters
                                                + "mps Up = "
                                                + formattedVelUncertaintyUpMeters
                                                + "mps");
                                String formattedOffsetMeters =
                                        new DecimalFormat("##.######")
                                                .format(
                                                        getDistanceMeters(
                                                                mCurrentLocation.getLatitude(),
                                                                mCurrentLocation.getLongitude(),
                                                                posSolution[0],
                                                                posSolution[1]));
                                logPositionError("position offset = " + formattedOffsetMeters + " meters");
                                String formattedSpeedOffsetMps =
                                        new DecimalFormat("##.###")
                                                .format(
                                                        Math.abs(
                                                                mCurrentLocation.getSpeed()
                                                                        - Math.sqrt(
                                                                        Math.pow(velSolution[0], 2)
                                                                                + Math.pow(velSolution[1], 2))));
                                logVelocityError("speed offset = " + formattedSpeedOffsetMps + " mps");
                            }
                            logLocationEvent("onLocationChanged: " + mCurrentLocation);
                            publish();
                        }
                    };
                    mMyPositionVelocityCalculationHandler.post(r);
                }
            }
        };
        // Build the location request
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Build the location settings request object
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        // Create a navigation callback
        mGnssNavigationMessageCallback =
                new GnssNavigationMessage.Callback() {
                    @Override
                    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                        Log.i(TAG,"onGnssNavigationMessageReceived");
                        super.onGnssNavigationMessageReceived(event);
//                        if (event.getType() == GnssNavigationMessage.TYPE_GPS_L1CA) {
                            mPseudorangePositionVelocityFromRealTimeEvents.parseHwNavigationMessageUpdates(event);
//                        }

                    }
                };

        mGnssMeasurementsEventCallback =
                new GnssMeasurementsEvent.Callback() {
                    @Override
                    public void onGnssMeasurementsReceived(final GnssMeasurementsEvent event) {
                        Log.i(TAG,"onGnssMeasurementsReceived");
                        super.onGnssMeasurementsReceived(event);
                        final Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mPseudorangePositionVelocityFromRealTimeEvents.setCorrectedResidualComputationTruthLocationLla(posSolution);
                                    mPseudorangePositionVelocityFromRealTimeEvents.computePositionVelocitySolutionsFromRawMeas(event);    /** main computation */
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        mMyPositionVelocityCalculationHandler.post(r);
                    }
                };

        mLocationManager.registerGnssNavigationMessageCallback(mGnssNavigationMessageCallback);
        mLocationManager.registerGnssMeasurementsCallback(mGnssMeasurementsEventCallback);

    }

    private void updateUI() {
        // Return if we do not have a position
        if (mCurrentLocation == null)
            return;
        // Else we are good to display
        String lat = String.valueOf(mCurrentLocation.getLatitude());
        String lng = String.valueOf(mCurrentLocation.getLongitude());
        tvLocation.setText("At Time: " + mLastUpdateTime + "\n" +
                "Latitude: " + lat + "\n" +
                "Longitude: " + lng + "\n" +
                "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
                "Provider: " + mCurrentLocation.getProvider());
        Log.d(TAG, tvLocation.getText().toString().replace("\n", " | "));
    }

/*
    private void publishGnssFix(List<Location> locs) {
        // Check that we have a location
        if (locs == null || locs.size() < 1)
            return;
        // We are good, lets publish
        for (Location location : locs) {
            NavSatFix fix = this.pub_fix.newMessage();
            fix.getHeader().setStamp(new Time(location.getTime()));
            fix.getHeader().setFrameId("android/"+robotName+"/gps");
            fix.getStatus().setStatus(NavSatStatus.STATUS_FIX);
            fix.getStatus().setService(NavSatStatus.SERVICE_GPS);
            fix.setLatitude(mCurrentLocation.getLatitude());
            fix.setLongitude(mCurrentLocation.getLongitude());
            fix.setAltitude(mCurrentLocation.getAltitude());
            fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
            double covariance = mCurrentLocation.getAccuracy()*mCurrentLocation.getAccuracy();
            double[] tmpCov = {covariance, 0, 0, 0, covariance, 0, 0, 0, covariance};
            fix.setPositionCovariance(tmpCov);
            pub_fix.publish(fix);
        }
        // Debug
        Log.d(TAG, "published = "+locs.size());
    }
*/

    private void publish() {
        if (posSolution==null || velSolution==null || pvUncertainty==null){
            return;
        }

        NavSatFix fix = pub_fix.newMessage();
        fix.getHeader().setStamp(new Time(mCurrentLocation.getTime()));
        fix.getHeader().setFrameId("android/"+robotName+"/gnss/fixENU");
        fix.getStatus().setStatus(NavSatStatus.STATUS_FIX);
        fix.getStatus().setService(NavSatStatus.SERVICE_GPS);
        fix.setLatitude(posSolution[0]);
        fix.setLongitude(posSolution[1]);
        fix.setAltitude(posSolution[2]);
        double[] pos_cov={pvUncertainty[0],0.0,0.0,0.0,pvUncertainty[1],0.0,0.0,0.0,pvUncertainty[2]};
        fix.setPositionCovariance(pos_cov);
        fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
        pub_fix.publish(fix);

        AccelWithCovarianceStamped accel=pub_accel.newMessage();
        accel.getHeader().setStamp(new Time(mCurrentLocation.getTime()));
        accel.getHeader().setFrameId("android/"+robotName+"/gnss/accelENU");
        accel.getAccel().getAccel().getLinear().setX(velSolution[0]);
        accel.getAccel().getAccel().getLinear().setY(velSolution[1]);
        accel.getAccel().getAccel().getLinear().setZ(velSolution[2]);
        double[] accel_cov={
                pvUncertainty[3],   0.0,                0.0,                0.0,    0.0,    0.0,
                0.0,                pvUncertainty[4],   0.0,                0.0,    0.0,    0.0,
                0.0,                0.0,                pvUncertainty[5],   0.0,    0.0,    0.0,
                0.0,                0.0,                0.0,                0.0,    0.0,    0.0,
                0.0,                0.0,                0.0,                0.0,    0.0,    0.0,
                0.0,                0.0,                0.0,                0.0,    0.0,    0.0};
        accel.getAccel().setCovariance(accel_cov);
        pub_accel.publish(accel);
    }


    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("sensors_driver/navsatfix_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Start location updates
        startLocationUpdates();
        // Create our pub_fix, pub_accel
        try {
            this.pub_fix = connectedNode.newPublisher("android/" + robotName + "/gnss/fix", "sensor_msgs/NavSatFix");
            this.pub_accel= connectedNode.newPublisher("android/" + robotName + "/gnss/accelENU", "geometry_msgs/AccelWithCovarianceStamped");

        } catch (Exception e) {
            if (connectedNode != null) {
//                connectedNode.getLog().fatal(e);
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onShutdown(Node node) {
        stopLocationUpdates();
    }

    @Override
    public void onShutdownComplete(Node node) {}

    @Override
    public void onError(Node node, Throwable throwable) {}

    //===========================================================================================
    //===========================================================================================
    //===========================================================================================
    //===========================================================================================


    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(mainAct, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");
                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        updateUI();
                    }
                })
                .addOnFailureListener(mainAct, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(mainAct, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(mainAct, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateUI();
                    }
                });
    }


    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Return the distance (measured along the surface of the sphere) between 2 points
     */
    public double getDistanceMeters(
            double lat1Degree, double lng1Degree, double lat2Degree, double lng2Degree) {

        double deltaLatRadian = Math.toRadians(lat2Degree - lat1Degree);
        double deltaLngRadian = Math.toRadians(lng2Degree - lng1Degree);

        double a =
                Math.sin(deltaLatRadian / 2) * Math.sin(deltaLatRadian / 2)
                        + Math.cos(Math.toRadians(lat1Degree))
                        * Math.cos(Math.toRadians(lat2Degree))
                        * Math.sin(deltaLngRadian / 2)
                        * Math.sin(deltaLngRadian / 2);
        double angularDistanceRad = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * angularDistanceRad;
    }


    /**
     * Update the ground truth for pseudorange residual analysis based on the user activity.
     */
    private synchronized void updateGroundTruth(double[] posSolution) {

        // In case of switching between modes, last ground truth from previous mode will be used.
        if (mGroundTruth == null) {
            // If mGroundTruth has not been initialized, we set it to be the same as position solution
            mGroundTruth = new double[] {0.0, 0.0, 0.0};
            mGroundTruth[0] = posSolution[0];
            mGroundTruth[1] = posSolution[1];
            mGroundTruth[2] = posSolution[2];
        }
    }




    private void logEvent(String tag, String message, int color) {
        String composedTag = TAG +": "+ tag;
        Log.d(composedTag, message);
        logText(tag, message, color);
    }

    private void logText(String tag, String text, int color) {
//        UIResultComponent component = getUiResultComponent();
//        if (component != null) {
//            component.logTextResults(tag, text, color);
//        }
    }

    public void logLocationEvent(String event) {
//        mCurrentColor = getNextColor();
        logEvent("Location", event, mCurrentColor);
    }

    private void logPositionFromRawDataEvent(String event) {
        logEvent("Calculated Position From Raw Data", event + "\n", mCurrentColor);
    }

    private void logVelocityFromRawDataEvent(String event) {
        logEvent("Calculated Velocity From Raw Data", event + "\n", mCurrentColor);
    }

    private void logPositionError(String event) {
        logEvent(
                "Offset between the reported position and Google's WLS position based on reported "
                        + "measurements",
                event + "\n",
                mCurrentColor);
    }

    private void logVelocityError(String event) {
        logEvent(
                "Offset between the reported velocity and "
                        + "Google's computed velocity based on reported measurements ",
                event + "\n",
                mCurrentColor);
    }

    private void logPositionUncertainty(String event) {
        logEvent("Uncertainty of the calculated position from Raw Data", event + "\n", mCurrentColor);
    }

    private void logVelocityUncertainty(String event) {
        logEvent("Uncertainty of the calculated velocity from Raw Data", event + "\n", mCurrentColor);
    }

//    private synchronized int getNextColor() {
//        ++mCurrentColorIndex;
//        return mRgbColorArray[mCurrentColorIndex % mRgbColorArray.length];
//    }

}