package com.flipkart.ameyadubey.advancedpedometer;

/*  This code follows the algorithm and the conventions in the article
    "Step Detection Robust against the Dynamics of Smartphones"
    by Hwan-hee Lee , Suji Choi and Myeong-jin Lee
    published in "Sensors 2015" , 26 October 2015
 */

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
//import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class AdvancedPedometer extends Activity {

    private TextView textViewX;
    private TextView textViewY;
    private TextView textViewZ;

//    private TextView textSensitive;
    private TextView textViewSteps;
//    private Button buttonReset;

    private SensorManager sensorManager;

    public final int STATE_INIT = 2;
    public final int STATE_PEAK = 1;
    public final int STATE_VALLEY = -1;
    public final int STATE_INTERMEDIATE = 0;

    private int count;      // number of steps.

    private int n;          // Time
    private int n_p;        // Time of last peak
    private int n_v;        // Time of last valley

    private double a_p; // magnitude of acceleration of the recent peak
    private double a_v; // magnitude of acceleration of the recent valley

    private double mu_a;    // Step Average
    private double sigma_a; // Step Deviation
    private int K;          // Window size of sliding window for calculating the step deviation
    private double alpha;   // Magnitude scale constant

    private double mu_p;    // the average time interval between adjacent peaks
    private double sigma_p; // the standard deviation of the time interval between adjacent peaks
    private double Th_p;    // the adaptive time threshold for peaks

    private double mu_v;    // the average time interval between adjacent valleys
    private double sigma_v; // the standard deviation of the time interval between adjacent valleys
    private double Th_v;    // the adaptive time threshold for valleys

    private int M;          // Window size of sliding window for calculating the peak-valley statistics
    private double beta;    // Time scale constant

    private double basic_thresh_1;
    private double basic_thresh_2;

    private int since_last_peak;
    private int S_n_minus_1;

    private SeekBar seekBar;
    private final String TAG1 = "n_val";
    private final String TAG2 = "sigma_a";

    private class Storage{

        private double store[];
        private int peaks[];
        private int valleys[];

        private int magwindow;
        private int peakvalwindow;

        private int maghead;
        private int peakhead;
        private int valleyhead;

        private int maginserted;
        public int peakinserted;
        public int valleyinserted;

        public double magsum ;
        public double magsumsquares ;

        public int peaksum;
        public int peaksumsquares;
        public int valleysum;
        public int valleysumsquares;

        Storage(int mymagwindow , int mypeakvalwindow){
            maghead = 0;
            peakhead = 0;
            valleyhead = 0;

            maginserted = 0;
            peakinserted = 0;
            valleyinserted = 0;

            magwindow = mymagwindow;
            peakvalwindow = mypeakvalwindow;

            store = new double[magwindow];
            peaks = new int[peakvalwindow];
            valleys = new int[peakvalwindow];

            magsum = 0.0;
            magsumsquares = 0.0;

            peaksum = 0;
            valleysum = 0;
            peaksumsquares = 0;
            valleysumsquares = 0;
        }

        private double getLast(int k){
            //Log.i(TAG2 , String.valueOf(maginserted));
            if (k > maginserted)
                throw new OutOfMemoryError();
            else
                return  store[(maghead - k + magwindow) % magwindow];
        }

        private void insertNew(double value){
            magsum += value;
            magsumsquares += (value*value);

            if(maginserted < magwindow)
                maginserted++;

            if(maginserted >= magwindow){
                double toSubtract = store[maghead];
                magsum -= toSubtract;
                magsumsquares -= (toSubtract * toSubtract);
            }

            store[maghead] = value;
            maghead = (maghead + 1) % magwindow ;
        }

        private void insertPeak(int mytime){
            int newtime = mytime - n_p;
            peaksum += newtime;
            peaksumsquares += (newtime*newtime);

            if(peakinserted < peakvalwindow)
                peakinserted++;

            if(peakinserted >= peakvalwindow){
                int toSubtract = peaks[peakhead];
                peaksum -= toSubtract;
                peaksumsquares -= (toSubtract * toSubtract);
            }

            peaks[peakhead] = newtime;
            peakhead = (peakhead + 1) % peakvalwindow ;
        }

        private void insertValley(int mytime){
            int newtime = mytime - n_v;
            valleysum += newtime;
            valleysumsquares += (newtime*newtime);

            if(valleyinserted < peakvalwindow)
                valleyinserted++;

            if(valleyinserted >= peakvalwindow){
                int toSubtract = valleys[valleyhead];
                valleysum -= toSubtract;
                valleysumsquares -= (toSubtract * toSubtract);
            }

            valleys[valleyhead] = newtime;
            valleyhead = (valleyhead + 1) % peakvalwindow ;
        }

        private void reset(int mymagwindow , int mypeakvalwindow){
            maghead = 0;
            peakhead = 0;
            valleyhead = 0;

            maginserted = 0;
            peakinserted = 0;
            valleyinserted = 0;

            magwindow = mymagwindow;
            peakvalwindow = mypeakvalwindow;

            magsum = 0.0;
            magsumsquares = 0.0;

            peaksum = 0;
            valleysum = 0;
            peaksumsquares = 0;
            valleysumsquares = 0;
        }
    }

    private Storage myStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedometer);

        textViewX = (TextView) findViewById(R.id.textViewX);
        textViewY = (TextView) findViewById(R.id.textViewY);
        textViewZ = (TextView) findViewById(R.id.textViewZ);

        textViewSteps = (TextView) findViewById(R.id.textSteps);
//        textSensitive = (TextView) findViewById(R.id.textSensitive);

//        buttonReset = (Button) findViewById(R.id.buttonReset);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        seekBar.setProgress(10);
        seekBar.setOnSeekBarChangeListener(seekBarListener);

        count = 0;
        textViewSteps.setText(String.valueOf(count));

        n = -1; // Sensor event received by OnSensorChanged goes to a_n_plus_1. (a_{n+1})
        n_p = 0;
        n_v = 0;
        a_p = 9.615380952381;
        a_v = 9.615380952381;

        mu_a = 9.615380952381;
        sigma_a = 0.0;

        mu_p = Double.POSITIVE_INFINITY;
        mu_v = Double.POSITIVE_INFINITY;
        sigma_p = 0.0;
        sigma_v = 0.0;

        Th_p = Double.POSITIVE_INFINITY;
        Th_v = Double.POSITIVE_INFINITY;

        K = 25;
        M = 10;
        alpha = 1.06763; // default : 4.0
        beta = 1.5 ; // default : 1/3


        S_n_minus_1 = STATE_INIT;
        myStore = new Storage(K , M);

        since_last_peak = 0;
        basic_thresh_1 = 12.5 ;
        basic_thresh_2 = 7.0;
        enableAccelerometerListening();
    }

    private void enableAccelerometerListening() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(sensorEventListener , sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
    }

    private SensorEventListener sensorEventListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    textViewX.setText(String.valueOf(x));
                    textViewY.setText(String.valueOf(y));
                    textViewZ.setText(String.valueOf(z));

                    double a_n_plus_1 = Math.sqrt((double)(x*x + y*y + z*z));

                    if (n < 1){
                        myStore.insertNew(a_n_plus_1);
                        n++;
                        return;
                    }

                    double a_n = myStore.getLast(1);
                    double a_n_minus_1 = myStore.getLast(2);

                    Log.i(TAG1 , String.valueOf(n));

                    myStore.insertNew(a_n_plus_1);

                    int S_c = STATE_INTERMEDIATE; // current state ?

                    if(a_n > Math.max( Math.max(a_n_minus_1 , a_n_plus_1) , Math.max(mu_a + (sigma_a/alpha) , basic_thresh_1) ) ) {
                        S_c = STATE_PEAK;
                        Log.i("peak" , "1");
                        since_last_peak = 0;
                    }
                    else if(a_n < Math.min( Math.min(a_n_minus_1 , a_n_plus_1) , Math.min(mu_a - (sigma_a/alpha) , basic_thresh_2)) ) {
                        S_c = STATE_VALLEY;
                        Log.i("valley" , "1");
                        since_last_peak++;
                    }
                    else{
                        since_last_peak++;
                    }

                    int S_n = STATE_INTERMEDIATE ;

                    if(S_c == STATE_PEAK){
                        Log.i("log" , "peak here");
                        if(S_n_minus_1 == STATE_INIT){
                            S_n = STATE_PEAK;
                            update_peak(a_n , n);
                            Log.i("cases" , "1");
                        }
                        else if((S_n_minus_1 == STATE_VALLEY) && (n - n_p > Th_p)){
                            S_n = STATE_PEAK;
                            update_peak(a_n , n);
                            mu_a = (a_p + a_v) / 2 ;
                            Log.i("cases" , "2");
                        }
                        else if((S_n_minus_1 == STATE_PEAK ) && (n - n_p <= Th_p) && (a_n > a_p)){
                            Log.i("log" , "3 ,"+ String.valueOf(S_n_minus_1) + " " + String.valueOf(n - n_p > Th_p) + " " + String.valueOf(a_n > a_p));
                            Log.i("cases" , "3");
                            update_peak(a_n , n);
                        }
                        else {
                            Log.i("cases", "4");
                            if ((n - n_p <= Th_p) && (a_n > a_p)) {
                                Log.i("log", "4 ," + String.valueOf(S_n_minus_1) + " " + String.valueOf(n - n_p > Th_p) + " " + String.valueOf(a_n > a_p));
                            }
                        }
                    }
                    else if(S_c == STATE_VALLEY){
                        Log.i("log" , "valley here");
                        if((S_n_minus_1 == STATE_PEAK || S_n_minus_1 == STATE_INTERMEDIATE || S_n_minus_1 == STATE_VALLEY) && (n - n_v > Th_v)){
                            S_n = STATE_VALLEY;
                            update_valley(a_n , n);
                            count++;
                            textViewSteps.setText(String.valueOf(count));
                            mu_a = (a_p + a_v) / 2 ;
                            Log.i("cases" , "5");
                        }
                        else if((S_n_minus_1 == STATE_VALLEY || S_n_minus_1 == STATE_INTERMEDIATE || S_n_minus_1 == STATE_PEAK) && (n - n_v <=  Th_v) && (a_n < a_v)){
                            Log.i("log" , "6 ,"+ String.valueOf(S_n_minus_1) + " " + String.valueOf(n - n_v > Th_v) + " " + String.valueOf(a_n < a_v));
                            Log.i("cases" , "6");
                            update_valley(a_n , n);
                        }
                        else{
                            Log.i("cases" , "7");
                            Log.i("log" , "7 ,"+ String.valueOf(S_n_minus_1) + " " + String.valueOf(n - n_v > Th_v) + " " + String.valueOf(a_n < a_v));
                        }
                    }
                    else{
                        Log.i("cases" , "8");
                    }

                    // Updating sigma_a
                    int denom = (K < n) ? K : n ;
                    sigma_a = Math.sqrt((myStore.magsumsquares / denom) - (Math.pow(myStore.magsum / denom , 2)));
                    Log.i(TAG2 , String.valueOf(sigma_a));
                    S_n_minus_1 = S_n ;
                    n++;

                    if (since_last_peak > 3*n_p){
                        sigma_a /= 2;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }

                private void update_peak(double axlr , int time){
                    // update mu_p , sigma_p , Th_p with time
                    myStore.insertPeak(time);
                    int denom = ((myStore.peakinserted) < M) ? myStore.peakinserted : M ;
                    mu_p = myStore.peaksum / denom ;
                    sigma_p = Math.sqrt((myStore.peaksumsquares / denom) - (mu_p*mu_p));
                    Th_p = mu_p - (sigma_p/beta);
                    n_p = time;
                    a_p = axlr;
                }

                private void update_valley(double axlr , int time){
                    // update mu_v , sigma_v, Th_v with time
                    myStore.insertValley(time);
                    int denom = ((myStore.valleyinserted) < M) ? myStore.valleyinserted : M ;
                    mu_v = myStore.valleysum / denom ;
                    sigma_v = Math.sqrt((myStore.valleysumsquares / denom) - (mu_v*mu_v));
                    Th_v = mu_v - (sigma_v/beta);
                    n_v = time;
                    a_v = axlr;
                }
            };

    public void resetSteps(View v){
        count = 0;
        textViewSteps.setText(String.valueOf(count));

        n = -1; // Sensor event received by OnSensorChanged goes to a_n_plus_1. (a_{n+1})
        n_p = 0;
        n_v = 0;
        a_p = 9.615380952381;
        a_v = 9.615380952381;

        mu_a = 9.615380952381;
        sigma_a = 0.0;

        mu_p = Double.POSITIVE_INFINITY;
        mu_v = Double.POSITIVE_INFINITY;
        sigma_p = 0.0;
        sigma_v = 0.0;

        Th_p = Double.POSITIVE_INFINITY;
        Th_v = Double.POSITIVE_INFINITY;

        S_n_minus_1 = STATE_INIT;
        myStore.reset(K , M);
    }

    private OnSeekBarChangeListener seekBarListener =
            new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };
    // end class AdvancedPedometer
}   // Kuki Monsta