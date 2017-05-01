package com.ranjan.malav.bobblekeyboardproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class PreviewActivity extends AppCompatActivity {
    String TAG = "PreviewActivity";
    String dir;
    String fileName;
    String fileDirectory;
    int orientation;
    Bitmap rotatedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        Intent intent = getIntent();
        dir = intent.getStringExtra("dir");
        fileName = intent.getStringExtra("fileName");
        fileDirectory = "/sdcard/" + dir + "/" + fileName;
        ImageView preview = (ImageView) findViewById(R.id.image_preview);

        File imgFile = new  File(fileDirectory);
        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            try {
                ExifInterface exif = new ExifInterface(fileDirectory);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(rotation);
                Matrix matrix = new Matrix();
                if (rotation != 0f) {matrix.preRotate(rotationInDegrees);}
                rotatedImage = Bitmap.createBitmap(myBitmap,0,0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
            }catch(IOException ex){
                Log.e(TAG, "Failed to get Exif data", ex);
            }
            preview.setImageBitmap(rotatedImage);
        } else {
            Log.d(TAG, "onCreate: file not found.");
        }
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //not going to previous activity since its launched from inside picture callback I guess.
        //this is not a good solution but should work for now.
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }
}
