package com.example.recognize;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
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
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RecognizeActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    TextView textView;
    private static final String TAG = "Recognize::Activity";
    CascadeClassifier cascadeClassifier;
    Bitmap bitmap;
    float[] floatValues = new float[160 * 160 * 3];
    String root = Environment.getExternalStorageDirectory().toString();
    File myDir = new File(root + "/recognize");
    Map<Integer, String> labelMap;
    Handler mHandler;

    //PATH TO OUR MODEL FILE AND NAMES OF THE INPUT AND OUTPUT NODES
    private String MODEL_PATH = "file:///android_asset/optimized_facenet.pb";
    private String INPUT_NAME = "input";
    private String OUTPUT_NAME = "embeddings";
    private TensorFlowInferenceInterface tf;
    float[] PREDICTIONS = new float[128];
    float[][] value = new float[100][129];


    Mat mRgba, mGray;
    Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    CameraBridgeViewBase cameraBridgeViewBase;

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
                labelMap.put(Integer.parseInt(split[1]),split[0]);
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (labelMap.isEmpty()){
            Toast.makeText(this, "EMPTY", Toast.LENGTH_SHORT).show();
        }
    }

    private float TinhKhoangCach(float[] x, float[] y){
        float sum = 0;
        for(int i = 0; i < 128; i++){
            sum += (x[i]-y[i])*(x[i]-y[i]);
        }
        return (float) Math.sqrt(sum);
    }


    private void loadTrainData() {
        File output = new File(myDir, "train_data");
        try {
            BufferedReader buf = new BufferedReader(new FileReader(output));
            String s = "";
            int i = 0;
            while ((s = buf.readLine()) != null) {
                String[] split = s.split(" ");
                for(int j = 0; j < 129; j++){
                    value[i][j] = Float.parseFloat(split[j]);
                }
                i++;
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_recognize);

        textView = findViewById(R.id.textView);

        cameraBridgeViewBase = findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);

        tf = new TensorFlowInferenceInterface(getAssets(),MODEL_PATH);

        labelMap = new HashMap<>();
        loadLabelData();
        loadTrainData();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String tempName = msg.obj.toString();
                textView.setText(tempName);
            }

        };
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
            bitmap  = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(m, bitmap);

            floatValues = new float[160 * 160 * 3];

            //Resize the image into 160 x 160
            Bitmap resized_image = ImageUtils.processBitmap(bitmap,160);

            //Normalize the pixels
            floatValues = ImageUtils.normalizeBitmap(resized_image,160,80.5f,1.0f);

            //Pass input into the tensorflow
            tf.feed(INPUT_NAME,floatValues,1,160,160,3);

            //compute predictions
            tf.run(new String[]{OUTPUT_NAME});

            //copy the output into the PREDICTIONS array
            tf.fetch(OUTPUT_NAME,PREDICTIONS);

            float min = 99999;
            int vtMin = 0;
            for(int i = 0; i < labelMap.size(); i++){
                float dis = TinhKhoangCach(PREDICTIONS, value[i]);
                if(dis < min){
                    min = dis;
                    vtMin = (int) value[i][128];
                }
            }

            String name = labelMap.get(vtMin);
            Message msg = new Message();
            if(min < 0.4){
                msg.obj = name;
            }else{
                msg.obj = "unknow";
            }
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
