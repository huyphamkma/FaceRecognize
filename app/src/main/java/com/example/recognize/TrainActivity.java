package com.example.recognize;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

public class TrainActivity extends AppCompatActivity implements Serializable, CameraBridgeViewBase.CvCameraViewListener2 {
    Intent intent;
    private static final String TAG = "Train::Activity";
    private static final int MAXCAPACITY = 100;
    CascadeClassifier cascadeClassifier;
    DetectFaceUtils detectFaceUtils;
    Mat mRgba, mGray;
    Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    CameraBridgeViewBase cameraBridgeViewBase;
    Button buttonExit, buttonAdd;
    ImageView imageView;
    EditText editTextName;
    Bitmap bitmapImagePreview;
    float[] floatValues = new float[160 * 160 * 3];
    String root = Environment.getExternalStorageDirectory().toString();
    File myDir = new File(root + "/recognize");

    private TensorFlowInferenceInterface tf;
    float[] PREDICTIONS = new float[128];

    String[][] arrLabel;
    int lengthLabel;
    static {
        System.loadLibrary("tensorflow_inference");
    }
    Handler mHandler;




    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                //    loadModelDetect();
                    detectFaceUtils = new DetectFaceUtils(getApplication());
                    cascadeClassifier = detectFaceUtils.loadModelDetect();
                    cameraBridgeViewBase.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    private int checkName(String name){

        if(lengthLabel == 0)
            return 1;
        for(int i = 0; i < lengthLabel; i++){
            if(arrLabel[i][0] == name){
                return Integer.parseInt(arrLabel[i][1]);
            }
        }
        for(int i = 1; i <= MAXCAPACITY; i++){
            boolean check = true;
            for(int j = 0; j < lengthLabel; j++){
                if(Integer.parseInt(arrLabel[j][1]) == i) {
                    check = false;
                    break;
                }
            }
            if(check == true){
                return i;
            }
        }
        return -1;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_train);

        intent = getIntent();

        cameraBridgeViewBase = findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);

        cameraBridgeViewBase.setCameraIndex(intent.getIntExtra("idCamera", 0));

        buttonExit = findViewById(R.id.buttonExit);
        buttonAdd = findViewById(R.id.buttonAdd);
        editTextName = findViewById(R.id.editTextName);
        imageView = findViewById(R.id.imageView2);

        tf = new TensorFlowInferenceInterface(getAssets(),RecognizeFaceUtils.MODEL_PATH);
        arrLabel = new String[MAXCAPACITY][2];

        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        arrLabel = FileUtils.loadLabelData();
        lengthLabel = FileUtils.getLengthLabelData();


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj=="IMG")
                {
                    imageView.setImageBitmap(bitmapImagePreview);
                }
            }
        };


        buttonExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });
        
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setImageBitmap(bitmapImagePreview);
                String name = editTextName.getText().toString().trim();
                if(!editTextName.getText().toString().isEmpty()){
                    //Resize the image into 160 x 160

                    Bitmap resized_image = ImageUtils.processBitmap(bitmapImagePreview,160);

                    //Normalize the pixels
                    floatValues = ImageUtils.normalizeBitmap(resized_image,160,80.5f,1.0f);

                    PREDICTIONS = RecognizeFaceUtils.predict(tf, floatValues);

                    int id = checkName(name);

                    FileUtils.writeTrainData(id, PREDICTIONS);

                    FileUtils.writeLabelData(name, id);

                    Toast.makeText(TrainActivity.this, "Add succes: "+name, Toast.LENGTH_SHORT).show();

                }else{
                    Toast.makeText(TrainActivity.this, "Add a person name !", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


        MatOfRect list_face = new MatOfRect();

        cascadeClassifier.detectMultiScale(mGray,list_face,1.1,6,0|CASCADE_SCALE_IMAGE, new Size(70,70), new Size());

        Rect[] list = list_face.toArray();


        if(list.length == 1 && !editTextName.getText().toString().isEmpty()) {
            Rect r = list[0];
            Mat m = mGray.submat(r);

            // tang do tuong phan, can bang sang
            m = ImageUtils.equalizeImage(m);

            // Giam nhieu cua anh
            m = ImageUtils.medianBlur(m);

            Bitmap bitmap= Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(m, bitmap);
            bitmapImagePreview = bitmap;

            Message msg = new Message();
            String textTochange = "IMG";
            msg.obj = textTochange;
            mHandler.sendMessage(msg);

        }


        for(int i = 0; i < list.length; i++){
            Imgproc.rectangle(mRgba, new Point(list[i].x, list[i].y),
                    new Point(list[i].x+list[i].width,list[i].y+list[i].height),
                    FACE_RECT_COLOR,2);
        }

        return mRgba;
    }


}
