package com.example.opencvfacedetection.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import com.unity3d.player.UnityPlayerActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CustomMainActivity extends UnityPlayerActivity implements
        CvCameraViewListener2 {

    private static final String TAG = "Unity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private Context mContext;
    private Mat rgba;
    private Mat gray;
    private Bitmap bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
    private CameraBridgeViewBase cameraView;

    private float relativeFaceSize = 0.2f;
    private int absoluteFaceSize = 0;

    private CascadeClassifier detector;

    private NativeBitmap mNativeBitmap;

    private int mTextureId = 0;

    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    // System.loadLibrary("library name");

                    this.createDetector();
                    if (cameraView == null) { return; }
                    cameraView.enableView();
                    cameraView.setCameraIndex(0);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }

        private void createDetector() {
            File cascadeFile;

            try {
                InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                FileOutputStream os = new FileOutputStream(cascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();

                detector = new CascadeClassifier(cascadeFile.getAbsolutePath());
                if (detector.empty()) {
                    Log.e(TAG, "Failed to load cascade classifier");
                    detector = null;
                } else
                    Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

                cascadeDir.delete();
            } catch(IOException e) {
                Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        ViewGroup rootView = (ViewGroup)this.findViewById(android.R.id.content);

        getLayoutInflater().inflate(R.layout.face_detect_surface_view, rootView);
        cameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);

        cameraView.setMaxFrameSize(1280, 720);
        cameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        cameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, loaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) { cameraView.disableView(); }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "onCameraViewStarted");
        rgba = new Mat();
        gray = new Mat();

        mNativeBitmap = new NativeBitmap();
    }

    @Override
    public void onCameraViewStopped() {
        rgba.release();
        gray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        inputFrame.rgba().copyTo(rgba);
        inputFrame.gray().copyTo(gray);

        if (this.absoluteFaceSize == 0) {
            this.calcFaceSize(rgba.height());
        }

        MatOfRect faces = new MatOfRect();

        this.detector.detectMultiScale(gray, faces, 1.1, 2, 2,
                new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(rgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        Core.flip(this.rgba, this.rgba, 0);
        Utils.matToBitmap(this.rgba, this.bmp);

        return rgba;
    }

    public int getTextureId() {
        return mTextureId;
    }

    public void setTextureId(int textureId) {
        mTextureId = textureId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
    }

    private void calcFaceSize(int height) {
        this.absoluteFaceSize = Math.round(height * relativeFaceSize);
    }

    public int createTexture() {
        mNativeBitmap = new NativeBitmap();
        return mNativeBitmap.createTexture(this.bmp);
    }

    public void updateTexture() {
        mNativeBitmap.getPlane().updateTexture(this.bmp);
    }
}
