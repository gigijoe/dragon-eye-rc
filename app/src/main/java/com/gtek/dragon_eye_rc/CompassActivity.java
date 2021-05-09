package com.gtek.dragon_eye_rc;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class CompassActivity extends AppCompatActivity {

    private AttitudeIndicator mAttitudeIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        mAttitudeIndicator = (AttitudeIndicator) findViewById(R.id.attitude_indicator);
        mAttitudeIndicator.setAttitude(30, 20);
    }
}