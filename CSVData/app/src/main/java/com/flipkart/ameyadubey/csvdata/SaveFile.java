package com.flipkart.ameyadubey.csvdata;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Integer;

/**
 * Created by ameya.dubey on 31/5/16.
 */

public class SaveFile extends Activity {
    private File csvFile ;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_file);

        csvFile = (File) getIntent().getExtras().get("file");
    }

    public void saveFile(View v){
        EditText mEdit   = (EditText)findViewById(R.id.editText2);
        TextView prompt = (TextView) findViewById(R.id.textView);

        int steps;
        try {
            steps = Integer.parseInt(mEdit.getText().toString());
            if(steps < 0){
                prompt.setText("Please enter valid integer");
                return;
            }
        }
        catch(NumberFormatException e){
            prompt.setText("Please enter valid integer");
            return;
        }

        String filename = csvFile.getName();
        File fileDirectory = csvFile.getParentFile();

        // Removes extension from original filename and adds "_steps.txt" to it.
        String newname = filename.replaceFirst("[.][^.]+$", "") + "_steps.txt";

        File newFile = new File(fileDirectory , newname);

        try{
            newFile.createNewFile();
        }catch (IOException e){
            prompt.setText("Could not create file.");
        }

        try {
            PrintWriter writer = new PrintWriter(newFile);
            writer.println(steps);
            writer.close();
        } catch (FileNotFoundException e) {
            prompt.setText("Could not write to file.");
        }
        finish();
    }
}