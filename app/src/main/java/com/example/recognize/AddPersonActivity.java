package com.example.recognize;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.Serializable;

public class AddPersonActivity extends AppCompatActivity implements Serializable {
    ImageView imageView;
    Button button;
    EditText editText;
    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);

        intent = getIntent();
        Bitmap bitmap = intent.getParcelableExtra("bitmap");

        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.buttonAdd);
        editText = findViewById(R.id.editText);

        imageView.setImageBitmap(bitmap);

    }
}
