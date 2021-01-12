package com.datanthuastic.tripbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DetailActivity extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText placeNameText, cityNameText, dateText;
    Button button;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        imageView = findViewById(R.id.imageView);
        placeNameText = findViewById(R.id.placeNameText);
        cityNameText = findViewById(R.id.cityNameText);
        dateText = findViewById(R.id.dateText);
        button = findViewById(R.id.button);

        database = this.openOrCreateDatabase("Trips",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.matches("new")){
            placeNameText.setText("");
            cityNameText.setText("");
            dateText.setText("");
            button.setVisibility(View.VISIBLE);

            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.selectimage);
        }
        else {
            int tripId = intent.getIntExtra("tripId", 1);
            button.setVisibility(View.INVISIBLE);

            try {

                Cursor cursor = database.rawQuery("SELECT * FROM trips WHERE id = ?",new String[] {String.valueOf(tripId)});

                int placeNameIndex = cursor.getColumnIndex("placename");
                int cityNameIndex = cursor.getColumnIndex("cityname");
                int dateIndex = cursor.getColumnIndex("date");
                int imageIndex = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    placeNameText.setText(cursor.getString(placeNameIndex));
                    cityNameText.setText(cursor.getString(cityNameIndex));
                    dateText.setText(cursor.getString(dateIndex));

                    byte[] bytes = cursor.getBlob(imageIndex);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);
                }

                cursor.close();

            } catch (Exception e) {

            }
        }




    }

    public void selectImage(View view){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
        else{
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery,2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2 && resultCode == RESULT_OK && data != null){

            Uri imageData = data.getData();

            try {

                if (Build.VERSION.SDK_INT >= 28){
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);
                }
                else {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageData);
                    imageView.setImageBitmap(selectedImage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save(View view){
        String placeName = placeNameText.getText().toString();
        String cityName = cityNameText.getText().toString();
        String date = dateText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database = this.openOrCreateDatabase("Trips",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS trips (id INTEGER PRIMARY KEY, placename VARCHAR, cityname VARCHAR, date VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO trips (placename, cityname, date, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);

            sqLiteStatement.bindString(1,placeName);
            sqLiteStatement.bindString(2,cityName);
            sqLiteStatement.bindString(3,date);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.execute();

        } catch (Exception e){

        }

        Intent intent = new Intent(DetailActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        //finish();


    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if(bitmapRatio > 1){
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        }
        else{
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image,width,height,true);
    }

}