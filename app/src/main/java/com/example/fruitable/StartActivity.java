package com.example.fruitable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.Toast;

public class StartActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);


        ImageButton btnStart = findViewById(R.id.btn_scan);
        btnStart.setOnClickListener(v -> {
            Toast.makeText(this, "Click OK", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(StartActivity.this, CameraActivity.class));
        });
    }
}
