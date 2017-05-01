package com.ranjan.malav.bobblekeyboardproject;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

//TODO selfie is upside down

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static String TAG = "MainActivity";
    private final int MY_PERMISSIONS_REQUEST= 100;
    private final int MY_PERMISSION_REQUEST_STORAGE = 99;
    boolean cameraPermission = false;
    boolean storagePermission = false;
    FrameLayout preview;
    int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
    ImageView captureIV, switchIV;
    private Camera mCamera;
    private int orientation;
    private CustomCamera customCamera;
    private ExifInterface exif;
    private int degrees = -1;
    private SensorManager sensorManager = null;
    private File sdRoot;
    private String dir;

    private String fileName;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()).toString() + ".jpg";

            File mkDir = new File(sdRoot, dir);
            mkDir.mkdirs();

            File pictureFile = new File(sdRoot, dir + fileName);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                Log.d(TAG, "onPictureTaken: "+e.getMessage());
            }

            // Adding Exif data for the orientation
            //6-portrait, 3-RH landscape, 1-LH landscape(default), 8-flipped portrait
            try {
                Log.d(TAG, "onPictureTaken: orientation is "+orientation+" cameraID "+cameraID);
                exif = new ExifInterface("/sdcard/" + dir + fileName);
                if(cameraID == 1 && orientation == 6){
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + 8+" cameraID "+cameraID);
                } else if (cameraID == 1 && orientation == 8){
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + 6+" cameraID "+cameraID);
                }
                else {
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + orientation + " cameraID " + cameraID);
                }
                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }
            startPreviewActivity();
        }
    };

    private void startPreviewActivity(){
        //starting preview activity after saving the image.
        //better way is to preview image to user before saving.
        Intent intent = new Intent(MainActivity.this, PreviewActivity.class);
        intent.putExtra("dir", dir);
        intent.putExtra("fileName", fileName);
        startActivity(intent);
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void switchCameraID() {
        //this will be called on multi camera phone only.
        if (cameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
            cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }

    private int getCameraID() {
        return cameraID;
    }

    public Camera getCameraInstance(int id) {
        Camera c = null;
        try {
            c = Camera.open(id);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Couldn't connect to camera.", Toast.LENGTH_SHORT).show();
        }
        return c;
    }

    private boolean checkAndRequestPermissions() {
        int permissionCAMERA = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        int storagePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionCAMERA != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MY_PERMISSIONS_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                if(grantResults.length == 1 && permissions[0].equals(Manifest.permission.CAMERA)
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    onResume();
                }
                else if(grantResults.length == 1 && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    onResume();
                } else if (grantResults.length == 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    onResume();
                }else {
                    Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //check if device ha a camera
        if (!checkCameraHardware(this)) {
            Toast.makeText(this, "Your device doesn't seem to have a camera attached.", Toast.LENGTH_SHORT).show();
            finish();
        }

        //different layout for single and multi camera devices
        if (Camera.getNumberOfCameras() == 1) {
            setContentView(R.layout.activity_main_single);
        } else {
            setContentView(R.layout.activity_main_double);
            switchIV = (ImageView) findViewById(R.id.switchIV); //switch image view for animation
        }
         //capture image view for animation

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sdRoot = Environment.getExternalStorageDirectory();
        dir = "/DCIM/Camera/";
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: is called");
        super.onResume();

        if(checkAndRequestPermissions()){
            cameraPermission = true;
            storagePermission = true;
        }

        preview = (FrameLayout) findViewById(R.id.camera_preview); //camera preview
        captureIV = (ImageView) findViewById(R.id.captureIV);
        if (cameraPermission) {
            cameraID = getCameraID(); //cameraID is initialized with back facing camera
            mCamera = getCameraInstance(cameraID);
            Camera.Parameters params = mCamera.getParameters();

            //choosing best available picture size from the camera.
            List<Camera.Size> sizeList = params.getSupportedPictureSizes();
            Camera.Size bestSize = null;
            bestSize = sizeList.get(0);
            for (int i = 1; i < sizeList.size(); i++) {
                if ((sizeList.get(i).width * sizeList.get(i).height) >
                        (bestSize.width * bestSize.height)) {
                    Log.d(TAG, "onResume: " + sizeList.get(i).width + " " + sizeList.get(i).height + " best yet " +
                            bestSize.width + " " + bestSize.height);
                    bestSize = sizeList.get(i);
                }
            }
            params.setPictureSize(bestSize.width, bestSize.height);
            mCamera.setParameters(params);
            //assigning camera to camera preview
            customCamera = new CustomCamera(this, mCamera);
            preview.addView(customCamera);
            mCamera.startPreview();
        }
        //sensor listener for saving correctly oriented image.
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void switchCamera(View view) {
        switchCameraID();
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            customCamera.surfaceDestroyed(customCamera.getHolder());
            customCamera.getHolder().removeCallback(customCamera);
            customCamera.destroyDrawingCache();
            preview.removeView(customCamera);
            mCamera = null;
        }
        customCamera.removeCallbacks(null);
        customCamera = null;
        onResume();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        customCamera.surfaceDestroyed(customCamera.getHolder());
        customCamera.getHolder().removeCallback(customCamera);
        customCamera.destroyDrawingCache();
        preview.removeView(customCamera);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void captureImage(View view) {
        mCamera.takePicture(null, null, mPicture);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                RotateAnimation animation = null;
                if (event.values[0] < 4 && event.values[0] > -4) {
                    if (event.values[1] > 0 && orientation != ExifInterface.ORIENTATION_ROTATE_90) {
                        // UP
                        orientation = ExifInterface.ORIENTATION_ROTATE_90;
                        animation = getRotateAnimation(0);
                        degrees = 0;
                    } else if (event.values[1] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_270) {
                        // UP SIDE DOWN
                        orientation = ExifInterface.ORIENTATION_ROTATE_270;
                        animation = getRotateAnimation(180);
                        degrees = 180;
                    }
                } else if (event.values[1] < 4 && event.values[1] > -4) {
                    if (event.values[0] > 0 && orientation != ExifInterface.ORIENTATION_NORMAL) {
                        // LEFT
                        orientation = ExifInterface.ORIENTATION_NORMAL;
                        animation = getRotateAnimation(90);
                        degrees = 90;
                    } else if (event.values[0] < 0 && orientation != ExifInterface.ORIENTATION_ROTATE_180) {
                        // RIGHT
                        orientation = ExifInterface.ORIENTATION_ROTATE_180;
                        animation = getRotateAnimation(270);
                        degrees = 270;
                    }
                }
                if (animation != null) {
                    if(Camera.getNumberOfCameras() > 1) {
                        switchIV.startAnimation(animation);
                        captureIV.startAnimation(animation);
                    } else {
                        captureIV.startAnimation(animation);
                    }
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private RotateAnimation getRotateAnimation(float toDegrees) {
        float compensation = 0;

        if (Math.abs(degrees - toDegrees) > 180) {
            compensation = 360;
        }
        if (toDegrees == 0) {
            compensation = -compensation;
        }
        RotateAnimation animation = new RotateAnimation(degrees, toDegrees - compensation,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(250);
        animation.setFillAfter(true);
        return animation;
    }

}
