package com.flipkart.ameyadubey.csvdata;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;

/**
 * Created by ameya.dubey on 31/5/16.
 */

public class NewFile extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_file);
    }

    public void createFile(View v){
        EditText mEdit   = (EditText)findViewById(R.id.editText);
        TextView prompt = (TextView) findViewById(R.id.prompt);

        String filename = mEdit.getText().toString() + ".txt";
        if(filename.isEmpty()){
            prompt.setText("Please enter a filename.");
        }
        else{
            // create file with name filename
            // exit activity
            File dir = new File("/storage/sdcard0/Document/Datalogs");
            if (!dir.exists()){
                dir.mkdirs();
            }

            File newFile = new File(dir.getPath() + File.separator + filename);

            try{
                newFile.createNewFile();
                Intent returnIntent = new Intent();
                returnIntent.putExtra("file",newFile);
                setResult(Activity.RESULT_OK , returnIntent);
                finish();
            }catch (java.io.IOException e){
                prompt.setText("Could not create file. " + newFile.getPath());
                e.printStackTrace();
            }

        }
    }
}