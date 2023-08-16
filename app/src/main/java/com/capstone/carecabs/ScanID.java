package com.capstone.carecabs;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class ScanID extends AppCompatActivity {
    private ImageButton imgBackBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_id);
        imgBackBtn = findViewById(R.id.imgBackBtn);

        imgBackBtn.setOnClickListener(v -> {
            finish();
        });
    }
}