package udel.rpng.sensors_driver.publishers.gnss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import org.ros.message.Time;
import org.ros.node.topic.Publisher;

import geometry_msgs.AccelWithCovarianceStamped;
import sensor_msgs.NavSatFix;
import sensor_msgs.NavSatStatus;
import udel.rpng.sensors_driver.MainActivity;

public class GnssLoggerReceiver extends BroadcastReceiver {

    private String TAG = "GnssLoggerReceiver";

    private String robotName;
    private MainActivity mainAct;
    private TextView tvLocation;

    private Publisher<NavSatFix> pub_fix;
    private Publisher<AccelWithCovarianceStamped> pub_accel;

    int numberOfUsefulSatellities=0;
    String strCalculatedPositionVelocity="";
    double[] posSolution=new double[3];
    double[] velSolution=new double[3];
    double[] posUncertainty=new double[3];
    double[] velUncertainty=new double[3];

    boolean isGnssloggerLaunched=false;
    boolean isLocationChecked=false;
    boolean isMeasurementChecked=false;

//    boolean onLocationChanged=false;
    boolean onGnssMeasurementsReceived=false;


/*
    */
/**
     * @param robotName
     * @param tvLocation
     * @param pub_fix
     * @param pub_accel
     *//*

    GnssLoggerReceiver(String robotName,TextView tvLocation,Publisher<NavSatFix> pub_fix,Publisher<AccelWithCovarianceStamped> pub_accel){
        this.robotName=robotName;
        this.tvLocation=tvLocation;
        this.pub_fix=pub_fix;
        this.pub_accel=pub_accel;
    }
*/


    /**
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG,"onReceive");

        String className=intent.getStringExtra("className");
        if(className.equals("MainActivity"))
            runMainActivity(intent);
        else if(className.equals("SettingsFragment"))
            runSettingsFragment(intent);
        else if(className.equals("RealTimePositionVelocityCalculator"))
            runRealTimePositionVelocityCalculator(intent);
        else
            Log.w(TAG,"No class");

        uiUpdate();

    }

    /**
     * @param intent
     */
    private void runMainActivity(Intent intent){
        isGnssloggerLaunched=intent.getBooleanExtra("isGnssloggerLaunched",false);
        Log.d(TAG,"isGnssloggerLaunched = "+Boolean.toString(isGnssloggerLaunched));
    }

    /**
     * @param intent
     */
    private void runSettingsFragment(Intent intent){
        isLocationChecked=intent.getBooleanExtra("isLocationChecked",false);
        isMeasurementChecked=intent.getBooleanExtra("isMeasurementChecked",false);
        Log.d(TAG,"isLocationChecked = "+Boolean.toString(isLocationChecked));
        Log.d(TAG,"isMeasurementChecked = "+Boolean.toString(isMeasurementChecked));

    }

    /**
     * @param intent
     */
    private void runRealTimePositionVelocityCalculator(Intent intent){

        numberOfUsefulSatellities=intent.getIntExtra("numberOfUsefulSatellities",0);
        strCalculatedPositionVelocity=intent.getStringExtra("pos_vel");

        String[] values=strCalculatedPositionVelocity.split(" ");
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

        publish();

    }

    /**
     *
     */
    private void publish() {

        NavSatFix fix = pub_fix.newMessage();
        fix.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
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
        accel.getHeader().setStamp(Time.fromMillis(System.currentTimeMillis()));
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

    /**
     *
     */
    private void uiUpdate(){

        String strGnssloggerLaunched="GNSS Logger: ";
        if(isGnssloggerLaunched)
            strGnssloggerLaunched+="on";
        else
            strGnssloggerLaunched+="off";

        String strLocationMeasurementChecked="Location & Measurements Checked: ";
        if(isLocationChecked && isMeasurementChecked)
            strLocationMeasurementChecked+="on";
        else
            strLocationMeasurementChecked+="off";

        String lat="Latitude: "+posSolution[0];
        String lon="Longitude: "+posSolution[1];
        String velE="VelocityEast: "+velSolution[0];
        String velN="VelocityNorth: "+velSolution[1];

        String text= strGnssloggerLaunched+"\n"+
                strLocationMeasurementChecked+"\n"+
                lat+"\n"+lon+"\n"+velE+"\n"+velN;

        tvLocation.setText(text);

    }


}
