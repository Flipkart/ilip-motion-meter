package com.flipkart.ameyadubey.csvdata;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.io.File;
import java.text.SimpleDateFormat;

public class CsvData extends Activity implements SensorEventListener{

    public static final int FILENAME_REQUEST = 1;
    private File csvFile;
    private SensorManager sensorManager;
    private Sensor mAccelerometer;
    //private Sensor mMagnetometer;
    private PrintWriter writer;

    private TextView x;
    private TextView y;
    private TextView z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csv_data);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //mMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

         x = (TextView) findViewById(R.id.textViewX);
         y = (TextView) findViewById(R.id.textViewY);
         z = (TextView) findViewById(R.id.textViewZ);
    }

    public void createNewFile(View view){
        Intent intent = new Intent(CsvData.this , NewFile.class);
        startActivityForResult(intent , FILENAME_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == FILENAME_REQUEST) {
            if(resultCode == Activity.RESULT_OK){
                csvFile = (File) data.getExtras().get("file");
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }


    }//onActivityResult

    public void startRecord(View v) throws IOException {
        sensorManager.registerListener(this , mAccelerometer ,SensorManager.SENSOR_DELAY_GAME);
        //sensorManager.registerListener(this , mMagnetometer ,SensorManager.SENSOR_DELAY_GAME);
        writer = new PrintWriter(csvFile);
        writer.println("");
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        x.setText(String.valueOf(event.values[0]));
        y.setText(String.valueOf(event.values[1]));
        z.setText(String.valueOf(event.values[2]));

        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Timestamp(System.currentTimeMillis()));
        String dataString = timeStamp
                + " " + String.valueOf(event.values[0])
                + " " + String.valueOf(event.values[1])
                + " " + String.valueOf(event.values[2]);

        if (event.sensor == mAccelerometer) {
            writer.println("A " + dataString);
        } /*else if (event.sensor == mMagnetometer) {
            writer.println("M " + dataString);
        }*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor , int accuracy){

    }

    public void stopAndSave(View v){
        writer.close();
        sensorManager.unregisterListener(this , mAccelerometer);
        Intent intent = new Intent(CsvData.this , SaveFile.class);
        intent.putExtra("file" , csvFile);
        startActivity(intent);
        //sensorManager.unregisterListener(this , mMagnetometer);
    }
}