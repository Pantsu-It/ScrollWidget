package com.qianmo.mynestedscrolling;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.pantsu.scrollwidget.R;
import com.pantsu.scrollwidget.view.MainActivityCopy;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.header).setOnClickListener(view -> {
      Intent intent = new Intent(MainActivity.this, MainActivityCopy.class);
      startActivity(intent);
    });
  }
}
