package com.example.fruitable;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class OptionActivity extends Activity {
    private static final int REQ_CAMERA_PERMISSION = 1001;
    private String pendingChoice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        Button btnFruit = findViewById(R.id.btn_fruit);
        Button btnVeg = findViewById(R.id.btn_vegetable);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.btn_fruit) {
                    pendingChoice = "fruit";
                } else {
                    pendingChoice = "vegetable";
                }
                // Request camera permission immediately
                if (ContextCompat.checkSelfPermission(OptionActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(OptionActivity.this,
                            new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
                } else {
                    startCameraActivity();
                }
            }
        };

        btnFruit.setOnClickListener(listener);
        btnVeg.setOnClickListener(listener);
    }

    private void startCameraActivity() {
        if (pendingChoice == null) pendingChoice = "fruit";
        Intent i = new Intent(OptionActivity.this, CameraActivity.class);
        i.putExtra("choice", pendingChoice);
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity();
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk melakukan scan", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
