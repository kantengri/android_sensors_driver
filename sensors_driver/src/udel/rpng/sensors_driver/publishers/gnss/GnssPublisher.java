package udel.rpng.sensors_driver.publishers.gnss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import android.util.Log;

import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import java.util.Map;

import sensor_msgs.NavSatFix;
import sensor_msgs.NavSatStatus;
import geometry_msgs.AccelWithCovarianceStamped;
import udel.rpng.sensors_driver.MainActivity;
import udel.rpng.sensors_driver.R;

import static java.lang.Double.NaN;

public class GnssPublisher implements NodeMain{

        // View objects and the main activity
    private String robotName;
    private String TAG = "GnssPublisher";
    private MainActivity mainAct;
    private TextView tvLocation;
    private Context mContext;
    private IntentFilter mIntentFilter;
    private GnssLoggerReceiver mGnssLoggerReceiver;

    boolean mIsShutdown=false;

    SharedPreferences mGnssLogger;
//    SharedPreferences.OnSharedPreferenceChangeListener mGnssLoggerListener;
//    SharedPreferences.Editor mGnssLoggerEditor;

    String value_pre="";

    Time time=Time.fromMillis(System.currentTimeMillis());
    double[] posSolution=new double[3];
    double[] velSolution=new double[3];
    double[] posUncertainty=new double[3];
    double[] velUncertainty=new double[3];

    // Our ROS publish node
    private Publisher<NavSatFix> pub_fix;
    private Publisher<AccelWithCovarianceStamped> pub_accel;

    public GnssPublisher(MainActivity mainAct, String robotName) {
        // Get our textzone
        this.mainAct = mainAct;
        this.robotName = robotName;
        tvLocation = (TextView) mainAct.findViewById(R.id.titleTextGnss);

        try {
            mContext = mainAct.createPackageContext("com.google.android.apps.location.gps.gnsslogger", Context.CONTEXT_RESTRICTED);
            mGnssLogger = mContext.getSharedPreferences("GnssLog", Context.MODE_PRIVATE);
//            mGnssLoggerEditor = mGnssLogger.edit();

//            mIntent=new Intent();
//            mIntent.setAction("MY_INTENT");

//            mIntent = mainAct.getIntent();
//            mIntent.setClassName("com.google.android.apps.location.gps.gnsslogger","com.google.android.apps.location.gps.gnsslogger.MainActivity");
//            mainAct.startService(mIntent);
//
        }
        catch (NameNotFoundException e) {
            Log.e("SharedPref", e.getLocalizedMessage());
        }

    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("sensors_driver/gnss_publisher");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        try {
            this.pub_fix = connectedNode.newPublisher("android/" + robotName + "/gnss/fix", "sensor_msgs/NavSatFix");
            this.pub_accel = connectedNode.newPublisher("android/" + robotName + "/gnss/accelENU", "geometry_msgs/AccelWithCovarianceStamped");

            mGnssLoggerReceiver = new GnssLoggerReceiver(robotName,tvLocation,pub_fix,pub_accel);
            mIntentFilter=new IntentFilter("Gnss");
            mainAct.registerReceiver(mGnssLoggerReceiver,mIntentFilter);
            mGnssLoggerReceiver.isGnssloggerLaunched=mGnssLogger.getBoolean("isGnssloggerLaunched",false);
            mGnssLoggerReceiver.isLocationChecked=mGnssLogger.getBoolean("isLocationChecked",false);
            mGnssLoggerReceiver.isMeasurementChecked=mGnssLogger.getBoolean("isMeasurementChecked",false);
            uiUpdate();

            while (!mIsShutdown){
                Thread.sleep(1000);
                uiUpdate();
            }


//            while(!mIsShutdown){
//                checkGnssLoggerChanged();
//                Thread.sleep(1000);
//            }



//            mBroadcastReceiver = new BroadcastReceiver() {
//                @Override
//                public void onReceive(Context context, Intent intent) {
////                    Bundle bundle=intent.getExtras();
////                    String str= Integer.toString(bundle.getInt("key"));
////                    Log.d(TAG,str);
//                    Log.d(TAG,"Message Received");
//                }
//            };
//            mFilter=new IntentFilter();
//            mFilter.addAction("Gnss");
//            mainAct.registerReceiver(mBroadcastReceiver,mFilter);

//            mGnssLoggerListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//                @Override
//                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//                    // TODO
//                    onGnssLoggerChanged(prefs, key);
//                }
//            };
//            mGnssLogger.registerOnSharedPreferenceChangeListener(mGnssLoggerListener);



//            int i=0;

/*            while(!mIsShutdown){

                boolean isGnssloggerLaunched=mGnssLogger.getBoolean("isGnssloggerLaunched",false);
                if(!isGnssloggerLaunched){
                    tvLocation.setText("Not launched GnssLogger App");
                }
                else{
                    boolean isLocationChecked=mGnssLogger.getBoolean("isLocationChecked",false);
                    boolean isMeasurementChecked=mGnssLogger.getBoolean("isMeasurementChecked",false);

                    if(!isLocationChecked || !isMeasurementChecked){
                        tvLocation.setText("Checking Location and Measurement Label to Start");
                    }
                    else{
                        String display="";
                        boolean onGnssMeasurementsReceived=mGnssLogger.getBoolean("onGnssMeasurementsReceived",false);
                        if(onGnssMeasurementsReceived){
                            display+="Received GNSS Measurements";
                            mGnssLoggerEditor.putBoolean("onGnssMeasurementsReceived",false);
                            mGnssLoggerEditor.commit();
                        }
                        else{
                            display+="Not Received GNSS Measurements";
                        }
                        boolean onLocationChanged=mGnssLogger.getBoolean("onLocationChanged",false);
                        if(onLocationChanged){

                            display+="Calculated Position and Velocity";
                            String pos_vel = mGnssLogger.getString("pos_vel","");
                            update(pos_vel);

                            mGnssLoggerEditor.putBoolean("onLocationChanged",false);
                            mGnssLoggerEditor.commit();
                        }
                        else{
                            display+="Not Calculated Position and Velocity";
                        }
                        tvLocation.setText(display);
                    }

                }

                Thread.sleep(100);

            }*/


        }
        catch (Exception e) {
            if (connectedNode != null) {
//                connectedNode.getLog().fatal(e);
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onShutdown(Node node) {
        Log.d(TAG,"onShutdown");
        mainAct.unregisterReceiver(mGnssLoggerReceiver);
        mIsShutdown=true;
    }

    @Override
    public void onShutdownComplete(Node node) {}

    @Override
    public void onError(Node node, Throwable throwable) {}

    /**
     *
     */
    private void uiUpdate() {

        String strGnssloggerLaunched = "GNSS Logger: ";
        if (mGnssLoggerReceiver.isGnssloggerLaunched)
            strGnssloggerLaunched += "on";
        else
            strGnssloggerLaunched += "off";

        String strLocationMeasurementChecked = "Location & Measurements: ";
        if (mGnssLoggerReceiver.isLocationChecked && mGnssLoggerReceiver.isMeasurementChecked)
            strLocationMeasurementChecked += "on";
        else
            strLocationMeasurementChecked += "off";

        String numS= Integer.toString(mGnssLoggerReceiver.numberOfUsefulSatellities);

        String lat = "Latitude: " + mGnssLoggerReceiver.posSolution[0];
        String lon = "Longitude: " + mGnssLoggerReceiver.posSolution[1];
        String velE = "VelocityEast: " + mGnssLoggerReceiver.velSolution[0];
        String velN = "VelocityNorth: " + mGnssLoggerReceiver.velSolution[1];

/*
        String text = strGnssloggerLaunched + "\n" +
                strLocationMeasurementChecked + "\n" +
                lat + "\n" + lon + "\n" + velE + "\n" + velN;
*/

        String text = strGnssloggerLaunched + "\n" +
                strLocationMeasurementChecked + "\n" +
                numS+ "\n"+
                mGnssLoggerReceiver.strCalculatedPositionVelocity;

        tvLocation.setText(text);

    }


    private class GnssLoggerReceiver extends BroadcastReceiver {

        private String TAG = "GnssLoggerReceiver";

        private String robotName;
        private MainActivity mainAct;
        private TextView tvLocation;

        private Publisher<NavSatFix> pub_fix;
        private Publisher<AccelWithCovarianceStamped> pub_accel;

        int numberOfUsefulSatellities = 0;
        String strCalculatedPositionVelocity = "";
        double[] posSolution = {NaN,NaN,NaN};
        double[] velSolution = {NaN,NaN,NaN};
        double[] posUncertainty = {NaN,NaN,NaN};
        double[] velUncertainty = {NaN,NaN,NaN};

        boolean isGnssloggerLaunched = false;
        boolean isLocationChecked = false;
        boolean isMeasurementChecked = false;

        GnssLoggerReceiver(){};

        /**
         * @param robotName
         * @param tvLocation
         * @param pub_fix
         * @param pub_accel
         */
        GnssLoggerReceiver(String robotName, TextView tvLocation, Publisher<NavSatFix> pub_fix, Publisher<AccelWithCovarianceStamped> pub_accel) {
            this.robotName = robotName;
            this.tvLocation = tvLocation;
            this.pub_fix = pub_fix;
            this.pub_accel = pub_accel;
//            uiUpdate();
        }


        /**
         * @param context
         * @param intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "onReceive");

            String className = intent.getStringExtra("className");
            if (className.equals("MainActivity"))
                runMainActivity(intent);
            else if (className.equals("SettingsFragment"))
                runSettingsFragment(intent);
            else if (className.equals("RealTimePositionVelocityCalculator"))
                runRealTimePositionVelocityCalculator(intent);
            else
                Log.w(TAG, "No class");

            uiUpdate();

        }

        /**
         * @param intent
         */
        private void runMainActivity(Intent intent) {
            isGnssloggerLaunched = intent.getBooleanExtra("isGnssloggerLaunched", false);
            Log.d(TAG, "isGnssloggerLaunched = " + Boolean.toString(isGnssloggerLaunched));
        }

        /**
         * @param intent
         */
        private void runSettingsFragment(Intent intent) {
            isLocationChecked = intent.getBooleanExtra("isLocationChecked", false);
            isMeasurementChecked = intent.getBooleanExtra("isMeasurementChecked", false);
            Log.d(TAG, "isLocationChecked = " + Boolean.toString(isLocationChecked));
            Log.d(TAG, "isMeasurementChecked = " + Boolean.toString(isMeasurementChecked));

        }

        /**
         * @param intent
         */
        private void runRealTimePositionVelocityCalculator(Intent intent) {

            numberOfUsefulSatellities = intent.getIntExtra("numberOfUsefulSatellities", 0);
            strCalculatedPositionVelocity = intent.getStringExtra("calculatedPositionVelocity");

            Log.d(TAG,strCalculatedPositionVelocity);

            if(strCalculatedPositionVelocity!="") {
                String[] values = strCalculatedPositionVelocity.split(" ");
                posSolution[0] = Double.valueOf(values[0]);
                posSolution[1] = Double.valueOf(values[1]);
                posSolution[2] = Double.valueOf(values[2]);
                velSolution[0] = Double.valueOf(values[3]);
                velSolution[1] = Double.valueOf(values[4]);
                velSolution[2] = Double.valueOf(values[5]);
                posUncertainty[0] = Double.valueOf(values[6]);
                posUncertainty[1] = Double.valueOf(values[7]);
                posUncertainty[2] = Double.valueOf(values[8]);
                velUncertainty[0] = Double.valueOf(values[9]);
                velUncertainty[1] = Double.valueOf(values[10]);
                velUncertainty[2] = Double.valueOf(values[11]);

                publish();
            }
        }

        /**
         *
         */
        private void publish() {

            NavSatFix fix = pub_fix.newMessage();
            fix.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
            fix.getHeader().setFrameId("android/" + robotName + "/gnss/fixENU");
            fix.getStatus().setStatus(NavSatStatus.STATUS_FIX);
            fix.getStatus().setService(NavSatStatus.SERVICE_GPS);
            fix.setLatitude(posSolution[0]);
            fix.setLongitude(posSolution[1]);
            fix.setAltitude(posSolution[2]);
            double[] pos_cov = {posUncertainty[0], 0.0, 0.0, 0.0, posUncertainty[1], 0.0, 0.0, 0.0, posUncertainty[2]};
            fix.setPositionCovariance(pos_cov);
            fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
            pub_fix.publish(fix);

            AccelWithCovarianceStamped accel = pub_accel.newMessage();
            accel.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
            accel.getHeader().setFrameId("android/" + robotName + "/gnss/accelENU");
            accel.getAccel().getAccel().getLinear().setX(velSolution[0]);
            accel.getAccel().getAccel().getLinear().setY(velSolution[1]);
            accel.getAccel().getAccel().getLinear().setZ(velSolution[2]);
            double[] accel_cov = {
                    velUncertainty[0], 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, velUncertainty[1], 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, velUncertainty[2], 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            accel.getAccel().setCovariance(accel_cov);
            pub_accel.publish(accel);
        }

    }


        /**
         *
         * @param gnssLogger
         * @param s
         */
 /*   private void onGnssLoggerChanged(SharedPreferences gnssLogger, String s){

        Log.d(TAG,"Called onGnssLoggerChanged");
        boolean isGnssloggerLaunched=gnssLogger.getBoolean("isGnssloggerLaunched",false);
        if(!isGnssloggerLaunched){
            String text="Not launched GnssLogger App";
            Log.d(TAG,text);
            tvLocation.setText(text);
        }
        else{
            boolean isLocationChecked=gnssLogger.getBoolean("isLocationChecked",false);
            boolean isMeasurementChecked=gnssLogger.getBoolean("isMeasurementChecked",false);

            if(!isLocationChecked || !isMeasurementChecked){
                String text="Clicking Location and Measurement Label to Start";
                Log.d(TAG,text);
                tvLocation.setText(text);
            }
            else{
                String display="";
                boolean onGnssMeasurementsReceived=gnssLogger.getBoolean("onGnssMeasurementsReceived",false);
                if(onGnssMeasurementsReceived){
                    display+="Received GNSS Measurements\n";
                    mGnssLoggerEditor.putBoolean("onGnssMeasurementsReceived",false);
                    mGnssLoggerEditor.commit();
                }
                else{
                    display+="Not Received GNSS Measurements\n";
                }
                boolean onLocationChanged=gnssLogger.getBoolean("onLocationChanged",false);
                if(onLocationChanged){

                    display+="Calculated Position and Velocity\n";
                    String pos_vel = gnssLogger.getString("pos_vel","");
                    update(pos_vel);

                    mGnssLoggerEditor.putBoolean("onLocationChanged",false);
                    mGnssLoggerEditor.commit();
                }
                else{
                    display+="Not Calculated Position and Velocity\n";
                }
                tvLocation.setText(display);
            }

        }




    }*/

    /**
     *
     */
/*
    private void checkGnssLoggerChanged(){

        Log.d(TAG,"Called checkGnssLoggerChanged");
        if(!mGnssLoggerReceiver.isGnssloggerLaunched){
            String text="Not launched GnssLogger App";
            Log.d(TAG,text);
            tvLocation.setText(text);
        }
        else {
            if(!mGnssLoggerReceiver.isLocationChecked || !mGnssLoggerReceiver.isMeasurementChecked){
                String text="Clicking Location and Measurement Label to Start";
                Log.d(TAG,text);
                tvLocation.setText(text);
            }
            else{
                String display="";
                if(mGnssLoggerReceiver.onGnssMeasurementsReceived){
                    display+="Received GNSS Measurements\n";
                    mGnssLoggerEditor.putBoolean("onGnssMeasurementsReceived",false);
                    mGnssLoggerEditor.commit();
                }
                else{
                    display+="Not Received GNSS Measurements\n";
                }
                if(!mGnssLoggerReceiver.pos_vel.equals("")){
                    display+="Calculated Position and Velocity\n";
                    String pos_vel = mGnssLoggerReceiver.pos_vel;
                    update(pos_vel);

                    mGnssLoggerEditor.putBoolean("onLocationChanged",false);
                    mGnssLoggerEditor.commit();
                }
                else{
                    display+="Not Calculated Position and Velocity\n";
                }
                tvLocation.setText(display);
            }
        }

    }
*/

    /**
     *
     */
/*
    private void update(String pos_vel){

        String[] values=pos_vel.split(" ");
        posSolution[0]=Double.valueOf(values[0]);
        posSolution[1]=Double.valueOf(values[1]);
        posSolution[2]=Double.valueOf(values[2]);
        velSolution[0]=Double.valueOf(values[3]);
        velSolution[1]=Double.valueOf(values[4]);
        velSolution[2]=Double.valueOf(values[5]);
        posUncertainty[0]=Double.valueOf(values[6]);
        posUncertainty[1]=Double.valueOf(values[7]);
        posUncertainty[2]=Double.valueOf(values[8]);
        velUncertainty[0]=Double.valueOf(values[9]);
        velUncertainty[1]=Double.valueOf(values[10]);
        velUncertainty[2]=Double.valueOf(values[11]);

        if(Double.isNaN(posSolution[0]) || Double.isNaN(posSolution[1]) || Double.isNaN(posSolution[2]) ||
                Double.isNaN(velSolution[0]) || Double.isNaN(velSolution[1]) || Double.isNaN(velSolution[2]) ||
                Double.isNaN(posUncertainty[0]) || Double.isNaN(posUncertainty[1]) || Double.isNaN(posUncertainty[2]) ||
                Double.isNaN(velUncertainty[0]) || Double.isNaN(velUncertainty[1]) || Double.isNaN(velUncertainty[2])){
            tvLocation.setText("GnssLogger Outputing Nan Values");
        }

        publish();
        tvLocation.setText("");

    }
*/

    /**
     *
     */
/*
    private void publish() {

        NavSatFix fix = pub_fix.newMessage();
        fix.getHeader().setStamp(time);
        fix.getHeader().setStamp(time);
        fix.getHeader().setFrameId("android/"+robotName+"/gnss/fixENU");
        fix.getStatus().setStatus(NavSatStatus.STATUS_FIX);
        fix.getStatus().setService(NavSatStatus.SERVICE_GPS);
        fix.setLatitude(posSolution[0]);
        fix.setLongitude(posSolution[1]);
        fix.setAltitude(posSolution[2]);
        double[] pos_cov={posUncertainty[0],0.0,0.0,0.0,posUncertainty[1],0.0,0.0,0.0,posUncertainty[2]};
        fix.setPositionCovariance(pos_cov);
        fix.setPositionCovarianceType(NavSatFix.COVARIANCE_TYPE_APPROXIMATED);
        pub_fix.publish(fix);

        AccelWithCovarianceStamped accel=pub_accel.newMessage();
        long time_delta_millis = System.currentTimeMillis() - SystemClock.uptimeMillis();
        accel.getHeader().setStamp(time);
        accel.getHeader().setFrameId("android/"+robotName+"/gnss/accelENU");
        accel.getAccel().getAccel().getLinear().setX(velSolution[0]);
        accel.getAccel().getAccel().getLinear().setY(velSolution[1]);
        accel.getAccel().getAccel().getLinear().setZ(velSolution[2]);
        double[] accel_cov={
                velUncertainty[0],   0.0,                0.0,                0.0,    0.0,    0.0,
                0.0,                velUncertainty[1],   0.0,                0.0,    0.0,    0.0,
                0.0,                0.0,                velUncertainty[2],   0.0,    0.0,    0.0,
                0.0,                0.0,                0.0,                0.0,    0.0,    0.0,
                0.0,                0.0,                0.0,                0.0,    0.0,    0.0,
                0.0,                0.0,                0.0,                0.0,    0.0,    0.0};
        accel.getAccel().setCovariance(accel_cov);
        pub_accel.publish(accel);
    }
*/



}