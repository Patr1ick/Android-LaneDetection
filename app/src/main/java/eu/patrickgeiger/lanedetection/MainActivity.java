package eu.patrickgeiger.lanedetection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;

    CameraListener cameraListener;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == SUCCESS) {
                Log.d("OpenCV", "OpenCV loaded successfully");
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise buttons
        Button btn = findViewById(R.id.button);
        btn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AboutActivity.class)));

        // Get permission to use the camera, if already granted, OpenCV is initialised
        getPermission();
    }


    /**
     * Checks if the permission to use the camera is granted. If already granted, OpenCV is initialised.
     */
    private void getPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        } else {
            initOpenCV();
        }
    }

    /**
     * Initialise OpenCV and the camera
     */
    private void initOpenCV() {
        if (OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV loaded", Toast.LENGTH_SHORT).show();

            // Initialise the Camera
            cameraBridgeViewBase = findViewById(R.id.cameraView);
            cameraListener = new CameraListener(findViewById(R.id.switch1), findViewById(R.id.switch2));
            cameraBridgeViewBase.setCvCameraViewListener(cameraListener);
            cameraBridgeViewBase.enableView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check if the permission is granted or denied
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You have to give the permission!", Toast.LENGTH_SHORT).show();
            getPermission();
        } else if (grantResults.length > 0) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            initOpenCV();
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                    "OpenCV",
                    "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            );
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }
}