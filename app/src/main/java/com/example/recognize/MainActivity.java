package com.example.recognize;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {
    Button buttonTrain, buttonRecognize, buttonManage;
    RadioGroup radioGroupCam;
    RadioButton radioFrontCam, radioBackCam;
    int idCamera = 0;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonTrain = findViewById(R.id.buttonExit);
        buttonRecognize = findViewById(R.id.buttonRecognize);
        buttonManage = findViewById(R.id.buttonManage);
        radioGroupCam = findViewById(R.id.radioGroupCam);
        radioBackCam = findViewById(R.id.radioBackCam);
        radioFrontCam = findViewById(R.id.radioFrontCam);
        radioBackCam.setChecked(true);

        radioGroupCam.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

            }
        });

        radioBackCam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                idCamera = 1;
            }
        });

        radioFrontCam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                idCamera = 0;
            }
        });

        buttonRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecognizeActivity.class);
                intent.putExtra("idCamera", idCamera);
                startActivity(intent);
            }
        });

        buttonTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TrainActivity.class);
                intent.putExtra("idCamera", idCamera);
                startActivity(intent);
            }
        });

        buttonManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ManageActivity.class);
                startActivity(intent);
            }
        });
    }



}
