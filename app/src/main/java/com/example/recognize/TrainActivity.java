package com.example.recognize;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
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
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TrainActivity extends AppCompatActivity implements Serializable, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Train::Activity";
    CascadeClassifier cascadeClassifier;
    Mat mRgba, mGray;
    Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    CameraBridgeViewBase cameraBridgeViewBase;
    Button buttonCapture, buttonAdd;
    ImageView imageView;
    EditText editTextName;
    Bitmap bitmapImagePreview, bitmapTrain;
    float[] floatValues = new float[160 * 160 * 3];
    String root = Environment.getExternalStorageDirectory().toString();
    File myDir = new File(root + "/recognize");
    private String MODEL_PATH = "file:///android_asset/optimized_facenet.pb";
    private String INPUT_NAME = "input";
    private String OUTPUT_NAME = "embeddings";
    private TensorFlowInferenceInterface tf;
    float[] PREDICTIONS = new float[128];
    Map<String, Integer> labelMap;
    static {
        System.loadLibrary("tensorflow_inference");
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    loadModelDetect();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void loadModelDetect() {
        try {
            // Copy data tu file XML sang 1 file de openCv co the doc duoc du lieu
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }
        cameraBridgeViewBase.enableView();
    }

    private void loadLabelData(){
        File output = new File(myDir, "label_data");
        try {
            BufferedReader buf = new BufferedReader(new FileReader(output));
            String s = "";
            while ((s = buf.readLine()) != null) {
                String[] split = s.split(";");
                labelMap.put(split[0], Integer.parseInt(split[1]));
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        if (labelMap.isEmpty()){
//            Toast.makeText(this, "EMPTY", Toast.LENGTH_SHORT).show();
//        }
    }

    private int checkName(String name){
        int id = 0;
        if(labelMap.isEmpty())
            return 1;
        if(labelMap.containsKey(name)){
            id = labelMap.get(name);
        }else{
            id = labelMap.size()+1;
        }
        return id;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_train);


        cameraBridgeViewBase = findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);

        buttonCapture = findViewById(R.id.buttonTrain);
        buttonAdd = findViewById(R.id.buttonAdd);
        editTextName = findViewById(R.id.editTextName);
        imageView = findViewById(R.id.imageView2);

        tf = new TensorFlowInferenceInterface(getAssets(),MODEL_PATH);
        labelMap = new HashMap<>();

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        loadLabelData();


        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setImageBitmap(bitmapImagePreview);
                bitmapTrain = bitmapImagePreview;
            }
        });
        
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLabelData();
                String name = editTextName.getText().toString().trim();
                if(!editTextName.getText().toString().isEmpty()){
                    //Resize the image into 160 x 160
                    Bitmap resized_image = ImageUtils.processBitmap(bitmapTrain,160);

                    //Normalize the pixels
                    floatValues = ImageUtils.normalizeBitmap(resized_image,160,80.5f,1.0f);

                    //Pass input into the tensorflow
                    tf.feed(INPUT_NAME,floatValues,1,160,160,3);

                    //compute predictions
                    tf.run(new String[]{OUTPUT_NAME});

                    //copy the output into the PREDICTIONS array
                    tf.fetch(OUTPUT_NAME,PREDICTIONS);

                    int id = checkName(name);


                    // ghi gia tri cua vector vao file train_data
                    File input = new File(myDir, "train_data");
                    try {
                        FileWriter fw = new FileWriter(input, true);
                        for(int i = 0; i < 128; i++){
                            fw.append(PREDICTIONS[i]+" ");
                        }
                        fw.append(id+"\n");
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // ghi label vao file label_data
                    File output = new File(myDir, "label_data");
                    try {
                        FileWriter fw = new FileWriter(output, true);
                        fw.append(name+";"+id+"\n");
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

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

        cascadeClassifier.detectMultiScale(mRgba, list_face);

        Rect[] list = list_face.toArray();


        if(list.length == 1) {
            Rect r = list[0];
            Mat m = mRgba.submat(r);
            bitmapImagePreview = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(m, bitmapImagePreview);
        }


        for(int i = 0; i < list.length; i++){
            Imgproc.rectangle(mRgba, new Point(list[i].x, list[i].y),
                    new Point(list[i].x+list[i].width,list[i].y+list[i].height),
                    FACE_RECT_COLOR,2);
        }

        return mRgba;
    }


}
