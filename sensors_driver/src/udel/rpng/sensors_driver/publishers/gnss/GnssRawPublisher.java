/*
 * Copyright (c) 2011, Chad Rockey
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Android Sensors Driver nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package udel.rpng.sensors_driver.publishers.gnss;

import android.graphics.Color;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.TextView;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import sensor_msgs.NavSatFix;
import geometry_msgs.AccelWithCovarianceStamped;
import sensor_msgs.NavSatStatus;
import udel.rpng.sensors_driver.MainActivity;
import udel.rpng.sensors_driver.R;

import java.util.concurrent.TimeUnit;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author tal.regev@gmail.com  (Tal Regev)
 */
public class GnssRawPublisher implements NodeMain {

    private MainActivity mainAct;
    private String robotName;
    private String TAG = "GnssRawPublisher";

    private GnssRawThread grThread;
    private Publisher<NavSatFix> pub_fix;
    private Publisher<AccelWithCovarianceStamped> pub_accel;

    LocationManager mLocationManager;
    GnssMeasurementsEvent.Callback mGnssMeasurementsEventCallback;
    LocationListener mLocationListener;

    private static final long LOCATION_RATE_GPS_MS = TimeUnit.SECONDS.toMillis(1L);
    private static final long LOCATION_RATE_NETWORK_MS = TimeUnit.SECONDS.toMillis(60L);

    //    public GnssRawPublisher(String robotName){
    public GnssRawPublisher(MainActivity mainAct, String robotName){
        this.mainAct=mainAct;
        this.robotName=robotName;

        mLocationManager = (LocationManager) mainAct.getApplicationContext().getSystemService(mainAct.getApplicationContext().LOCATION_SERVICE);

        Log.i(TAG, "hoge");

    }


//    public GnssRawPublisher(SensorManager manager, int sensorDelay, String robotName) {
//        this.sensorManager = manager;
//        this.sensorDelay = sensorDelay;
//        this.robotName = robotName;
//    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("android_"+robotName+"_sensors_driver/gnss_raw_publisher");
    }

    public void onError(Node node, Throwable throwable) {
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onStart(ConnectedNode node) {
        try {
//            List<Sensor> mfList = this.sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);

            this.pub_fix = node.newPublisher("android/" + robotName + "/gnss_raw/fixENU", "sensor_msgs/NavSatFix");
            this.pub_accel= node.newPublisher("android/" + robotName + "/gnss_raw/accelENU", "geometry_msgs/AccelWithCovarianceStamped");

            Log.i(TAG, "thread");

            this.grThread=new GnssRawThread(mainAct,pub_fix,pub_accel);

            mLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.i("hoge","hoge2");
                    grThread.onLocationChanged(location);
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {
                    Log.i(TAG, "Enabled by "+s);
                    grThread.onProviderEnabled(s);
                }

                @Override
                public void onProviderDisabled(String s) {
                    Log.i(TAG, "Disabled by "+s);
                    grThread.onProviderDisabled(s);
                }
            };

            mGnssMeasurementsEventCallback = new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent eventArgs) {
                    Log.i("hoge33","hoge5");
                    grThread.onGnssMeasurementsReceived(eventArgs);
                }
            };

//            mLocationManager.requestLocationUpdates(
//                    LocationManager.NETWORK_PROVIDER,
//                    300,
//                    1 /* minDistance */,
//                    mLocationListener);
//            mLocationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER,
//                    300,
//                    1 /* minDistance */,
//                    mLocationListener);

//            mLocationManager.requestLocationUpdates(
//                    LocationManager.NETWORK_PROVIDER,
//                    LOCATION_RATE_NETWORK_MS,
//                    0.0f /* minDistance */,
//                    mLocationListener);
//            mLocationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER,
//                    LOCATION_RATE_GPS_MS,
//                    0.0f /* minDistance */,
//                    mLocationListener);

            mLocationManager.registerGnssMeasurementsCallback(mGnssMeasurementsEventCallback);

//            if (mfList.size() > 0) {
//                this.publisher = node.newPublisher("android/" + robotName + "/gnss_raw/", "sensor_msgs/MagneticField");
//                this.sensorListener = new SensorListener(this.publisher);
//                this.mfThread = new MagneticFieldThread(this.sensorManager, this.sensorListener);
//                this.mfThread.start();
//            }

        } catch (Exception e) {
            if (node != null) {
//                node.getLog().fatal(e);
            } else {
                e.printStackTrace();
            }
        }
    }

    //@Override
    public void onShutdown(Node arg0) {

        Log.i("shutdown","hog3");
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.unregisterGnssMeasurementsCallback(mGnssMeasurementsEventCallback);

//        if (this.mfThread == null) {
//            return;
//        }
//
//        this.mfThread.shutdown();
//
//        try {
//            this.mfThread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    //@Override
    public void onShutdownComplete(Node arg0) {
    }


    private class GnssRawThread implements GnssListener{

        private MainActivity mainAct;
        private TextView tvLocation;
        private String TAG = "GnssRawPublisher";

        private Publisher<NavSatFix> pub_fix;
        private Publisher<AccelWithCovarianceStamped> pub_accel;

        NavSatFix fix;
        AccelWithCovarianceStamped accel;

        private static final long EARTH_RADIUS_METERS = 6371000;
        private PseudorangePositionVelocityFromRealTimeEvents
                mPseudorangePositionVelocityFromRealTimeEvents;
        private HandlerThread mPositionVelocityCalculationHandlerThread;  // ThreadのLooper持ち
        private Handler mMyPositionVelocityCalculationHandler;
        private int mCurrentColor = Color.rgb(0x4a, 0x5f, 0x70);
        private int mCurrentColorIndex = 0;
        private boolean mAllowShowingRawResults = true;
        private int[] mRgbColorArray = {
                Color.rgb(0x4a, 0x5f, 0x70),
                Color.rgb(0x7f, 0x82, 0x5f),
                Color.rgb(0xbf, 0x90, 0x76),
                Color.rgb(0x82, 0x4e, 0x4e),
                Color.rgb(0x66, 0x77, 0x7d)
        };
        private int mResidualPlotStatus;
        private double[] mGroundTruth = null;
        private int mPositionSolutionCount = 0;

//        public GnssRawThread(Publisher<NavSatFix> pub_fix, Publisher<AccelWithCovarianceStamped> pub_accel){

        public GnssRawThread(MainActivity mainAct, Publisher<NavSatFix> pub_fix, Publisher<AccelWithCovarianceStamped> pub_accel){

            this.mainAct=mainAct;
            this.tvLocation = (TextView) mainAct.findViewById(R.id.titleTextGPS);

//            tvLocation.setText("Start GnssRawPublisher!");
            Log.i(TAG, "thread start");

            this.pub_fix =pub_fix;
            this.pub_accel=pub_accel;

            mPositionVelocityCalculationHandlerThread =
                    new HandlerThread("Position From Realtime Pseudoranges");
            mPositionVelocityCalculationHandlerThread.start();
            mMyPositionVelocityCalculationHandler =
                    new Handler(mPositionVelocityCalculationHandlerThread.getLooper());

            final Runnable r =
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mPseudorangePositionVelocityFromRealTimeEvents =
                                        new PseudorangePositionVelocityFromRealTimeEvents();
                            } catch (Exception e) {
                                Log.e(
                                        TAG,
                                        " Exception in constructing PseudorangePositionFromRealTimeEvents : ",
                                        e);
                            }
                        }
                    };

            mMyPositionVelocityCalculationHandler.post(r);

        }

        //    private UIResultComponent uiResultComponent;
//
//    public synchronized UIResultComponent getUiResultComponent() {
//        return uiResultComponent;
//    }
//
//    public synchronized void setUiResultComponent(UIResultComponent value) {
//        uiResultComponent = value;
//    }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "Enabled by "+provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(TAG, "Disabled by "+provider);
        }

        @Override
        public void onGnssStatusChanged(GnssStatus gnssStatus) {}

        /**
         * Update the reference location in {@link PseudorangePositionVelocityFromRealTimeEvents} if the
         * received location is a network location.
         */
        // call when location information changed
        @Override
        public void onLocationChanged(final Location location) {
            Log.i(TAG, "onLocationChanged");
            if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {  // cell tower and wifi based, rapid but low accuracy
                final Runnable r =
                        new Runnable() {
                            @Override
                            public void run() {
                                if (mPseudorangePositionVelocityFromRealTimeEvents == null) {
                                    return;
                                }
                                try {
                                    mPseudorangePositionVelocityFromRealTimeEvents.setReferencePosition(
                                            (int) (location.getLatitude() * 1E7),
                                            (int) (location.getLongitude() * 1E7),
                                            (int) (location.getAltitude() * 1E7));
                                } catch (Exception e) {
//                                Log.e(GnssContainer.TAG, " Exception setting reference location : ", e);
                                }
                            }
                        };

                mMyPositionVelocityCalculationHandler.post(r);

            }
            else if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {   // gps signal based, accurate but slow
                final Runnable r =
                        new Runnable() {
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

                                fix=pub_fix.newMessage();
                                fix.getHeader().setStamp(new Time(location.getTime()));
                                fix.getHeader().setFrameId("android/"+robotName+"/gnss_raw/fixENU");
                                fix.getStatus().setStatus(NavSatStatus.STATUS_FIX);
                                fix.getStatus().setService(NavSatStatus.SERVICE_GPS);
                                fix.setLatitude(posSolution[0]);
                                fix.setLongitude(posSolution[1]);
                                fix.setAltitude(posSolution[2]);
                                double[] pos_cov={pvUncertainty[0],0.0,0.0,0.0,pvUncertainty[1],0.0,0.0,0.0,pvUncertainty[2]};
                                fix.setPositionCovariance(pos_cov);
                                fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
                                pub_fix.publish(fix);

                                accel=pub_accel.newMessage();
                                accel.getHeader().setStamp(new Time(location.getTime()));
                                accel.getHeader().setFrameId("android/"+robotName+"/gnss_raw/accelENU");
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

//                                if (Double.isNaN(posSolution[0])) {
//                                    logPositionFromRawDataEvent("No Position Calculated Yet");
//                                    logPositionError("And no offset calculated yet...");
//                                } else {
//                                    if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED
//                                            && mResidualPlotStatus != RESIDUAL_MODE_AT_INPUT_LOCATION) {
//                                        updateGroundTruth(posSolution);
//                                    }
//                                    String formattedLatDegree = new DecimalFormat("##.######").format(posSolution[0]);
//                                    String formattedLngDegree = new DecimalFormat("##.######").format(posSolution[1]);
//                                    String formattedAltMeters = new DecimalFormat("##.#").format(posSolution[2]);
//                                    logPositionFromRawDataEvent(
//                                            "latDegrees = "
//                                                    + formattedLatDegree
//                                                    + " lngDegrees = "
//                                                    + formattedLngDegree
//                                                    + "altMeters = "
//                                                    + formattedAltMeters);
//                                    String formattedVelocityEastMps =
//                                            new DecimalFormat("##.###").format(velSolution[0]);
//                                    String formattedVelocityNorthMps =
//                                            new DecimalFormat("##.###").format(velSolution[1]);
//                                    String formattedVelocityUpMps =
//                                            new DecimalFormat("##.###").format(velSolution[2]);
//                                    logVelocityFromRawDataEvent(
//                                            "Velocity East = "
//                                                    + formattedVelocityEastMps
//                                                    + "mps"
//                                                    + " Velocity North = "
//                                                    + formattedVelocityNorthMps
//                                                    + "mps"
//                                                    + "Velocity Up = "
//                                                    + formattedVelocityUpMps
//                                                    + "mps");
//
//                                    String formattedPosUncertaintyEastMeters =
//                                            new DecimalFormat("##.###").format(pvUncertainty[0]);
//                                    String formattedPosUncertaintyNorthMeters =
//                                            new DecimalFormat("##.###").format(pvUncertainty[1]);
//                                    String formattedPosUncertaintyUpMeters =
//                                            new DecimalFormat("##.###").format(pvUncertainty[2]);
//                                    logPositionUncertainty(
//                                            "East = "
//                                                    + formattedPosUncertaintyEastMeters
//                                                    + "m North = "
//                                                    + formattedPosUncertaintyNorthMeters
//                                                    + "m Up = "
//                                                    + formattedPosUncertaintyUpMeters
//                                                    + "m");
//                                    String formattedVelUncertaintyEastMeters =
//                                            new DecimalFormat("##.###").format(pvUncertainty[3]);
//                                    String formattedVelUncertaintyNorthMeters =
//                                            new DecimalFormat("##.###").format(pvUncertainty[4]);
//                                    String formattedVelUncertaintyUpMeters =
//                                            new DecimalFormat("##.###").format(pvUncertainty[5]);
//                                    logVelocityUncertainty(
//                                            "East = "
//                                                    + formattedVelUncertaintyEastMeters
//                                                    + "mps North = "
//                                                    + formattedVelUncertaintyNorthMeters
//                                                    + "mps Up = "
//                                                    + formattedVelUncertaintyUpMeters
//                                                    + "mps");
//                                    String formattedOffsetMeters =
//                                            new DecimalFormat("##.######")
//                                                    .format(
//                                                            getDistanceMeters(
//                                                                    location.getLatitude(),
//                                                                    location.getLongitude(),
//                                                                    posSolution[0],
//                                                                    posSolution[1]));
//                                    logPositionError("position offset = " + formattedOffsetMeters + " meters");
//                                    String formattedSpeedOffsetMps =
//                                            new DecimalFormat("##.###")
//                                                    .format(
//                                                            Math.abs(
//                                                                    location.getSpeed()
//                                                                            - Math.sqrt(
//                                                                            Math.pow(velSolution[0], 2)
//                                                                                    + Math.pow(velSolution[1], 2))));
//                                    logVelocityError("speed offset = " + formattedSpeedOffsetMps + " mps");
//                                }
//                                logLocationEvent("onLocationChanged: " + location);
//                                if (!Double.isNaN(posSolution[0])) {
//                                    updateMapViewWithPositions(
//                                            posSolution[0],
//                                            posSolution[1],
//                                            location.getLatitude(),
//                                            location.getLongitude(),
//                                            location.getTime());
//                                } else {
//                                    clearMapMarkers();
//                                }
                            }
                        };
                mMyPositionVelocityCalculationHandler.post(r);
            }
        }

        @Override
        public void onLocationStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onGnssMeasurementsReceived(final GnssMeasurementsEvent event) {
            Log.i(TAG, "onGnssMeasurementsReceived");
            mAllowShowingRawResults = true;
            final Runnable r =
                    new Runnable() {
                        @Override
                        public void run() {
//                        mMainActivity.runOnUiThread(
//                                new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        mPlotFragment.updateCnoTab(event);
//                                    }
//                                });
                            if (mPseudorangePositionVelocityFromRealTimeEvents == null) {
                                return;
                            }
                            try {
//                            if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED
//                                    && mResidualPlotStatus != RESIDUAL_MODE_AT_INPUT_LOCATION) {
                                // The position at last epoch is used for the residual analysis.
                                // This is happening by updating the ground truth for pseudorange before using the
                                // new arriving pseudoranges to compute a new position.
                                mPseudorangePositionVelocityFromRealTimeEvents
                                        .setCorrectedResidualComputationTruthLocationLla(mGroundTruth);
//                            }
                                mPseudorangePositionVelocityFromRealTimeEvents
                                        .computePositionVelocitySolutionsFromRawMeas(event);    /** main computation */
                                // Running on main thread instead of in parallel will improve the thread safety
//                            if (mResidualPlotStatus != RESIDUAL_MODE_DISABLED) {
//                                mMainActivity.runOnUiThread(
//                                        new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                mPlotFragment.updatePseudorangeResidualTab(
//                                                        mPseudorangePositionVelocityFromRealTimeEvents
//                                                                .getPseudorangeResidualsMeters(),
//                                                        TimeUnit.NANOSECONDS.toSeconds(
//                                                                event.getClock().getTimeNanos()));
//                                            }
//                                        }
//                                );
//                            } else {
//                                mMainActivity.runOnUiThread(
//                                        new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                // Here we create gaps when the residual plot is disabled
//                                                mPlotFragment.updatePseudorangeResidualTab(
//                                                        GpsMathOperations.createAndFillArray(
//                                                                GpsNavigationMessageStore.MAX_NUMBER_OF_SATELLITES, Double.NaN),
//                                                        TimeUnit.NANOSECONDS.toSeconds(
//                                                                event.getClock().getTimeNanos()));
//                                            }
//                                        }
//                                );
//                            }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    };
            mMyPositionVelocityCalculationHandler.post(r);
        }

        @Override
        public void onGnssMeasurementsStatusChanged(int status) {}

        @Override
        public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
            if (event.getType() == GnssNavigationMessage.TYPE_GPS_L1CA) {
                mPseudorangePositionVelocityFromRealTimeEvents.parseHwNavigationMessageUpdates(event);
            }
        }

        @Override
        public void onGnssNavigationMessageStatusChanged(int status) {}

        @Override
        public void onNmeaReceived(long l, String s) {}

        @Override
        public void onListenerRegistration(String listener, boolean result) {}

        @Override
        public void onTTFFReceived(long l){}


    }

//    private class MagneticFieldThread extends Thread {
//        private final SensorManager sensorManager;
//        private final Sensor mfSensor;
//        private SensorListener sensorListener;
//        private Looper threadLooper;
//
//        private MagneticFieldThread(SensorManager sensorManager, SensorListener sensorListener) {
//            this.sensorManager = sensorManager;
//            this.sensorListener = sensorListener;
//            this.mfSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        }
//
//
//        public void run() {
//            Looper.prepare();
//            this.threadLooper = Looper.myLooper();
//            this.sensorManager.registerListener(this.sensorListener, this.mfSensor, sensorDelay);
//            Looper.loop();
//        }
//
//
//        public void shutdown() {
//            this.sensorManager.unregisterListener(this.sensorListener);
//            if (this.threadLooper != null) {
//                this.threadLooper.quit();
//            }
//        }
//    }

//    private class SensorListener implements SensorEventListener {
//
//        private Publisher<MagneticField> publisher;
//
//        private SensorListener(Publisher<MagneticField> publisher) {
//            this.publisher = publisher;
//        }
//
//        //	@Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        }
//
//        //	@Override
//        public void onSensorChanged(SensorEvent event) {
//            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//                MagneticField msg = this.publisher.newMessage();
//                long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
//                msg.getHeader().setStamp(Time.fromMillis(time_delta_millis + event.timestamp / 1000000));
//                msg.getHeader().setFrameId("/android/"+robotName+"magnetic_field");// TODO Make parameter
//
//                msg.getMagneticField().setX(event.values[0] / 1e6);
//                msg.getMagneticField().setY(event.values[1] / 1e6);
//                msg.getMagneticField().setZ(event.values[2] / 1e6);
//
//                double[] tmpCov = {0, 0, 0, 0, 0, 0, 0, 0, 0}; // TODO Make Parameter
//                msg.setMagneticFieldCovariance(tmpCov);
//
//                publisher.publish(msg);
//            }
//        }
//    }

}
