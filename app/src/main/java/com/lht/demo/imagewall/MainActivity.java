package com.lht.demo.imagewall;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ZooImageView zooImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        zooImageView = (ZooImageView) findViewById(R.id.ziv);



    }
}
