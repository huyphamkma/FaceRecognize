package com.example.recognize;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

public class RecognizeActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    Intent intent = getIntent();
    TextView textView;
    ImageView imageView;
    Button button;
    private static final String TAG = "Recognize::Activity";
    CascadeClassifier cascadeClassifier;
    Bitmap bitmap;
    float[] floatValues = new float[160 * 160 * 3];
    String root = Environment.getExternalStorageDirectory().toString();
    File myDir = new File(root + "/recognize");
    Map<Integer, String> labelMap;
    Handler mHandler;
    int size = 0;
    int idCamera = 0;
    Set<String> uniqueNames = new HashSet<String>();
    String[] uniqueNamesArray = new String[10];

    //PATH TO OUR MODEL FILE AND NAMES OF THE INPUT AND OUTPUT NODES
    private String MODEL_PATH = "file:///android_asset/optimized_facenet.pb";
    private String INPUT_NAME = "input";
    private String OUTPUT_NAME = "embeddings";

    private TensorFlowInferenceInterface tf;
    float[] PREDICTIONS = new float[128];
    float[][] value = new float[1000][129];

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;






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
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
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
                size++;
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

        intent = getIntent();

        textView = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);

        cameraBridgeViewBase = findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
       // int idCamera = intent.getIntExtra("idCamera", 0);
      //  Toast.makeText(this, "id = "+idCamera, Toast.LENGTH_SHORT).show();
        cameraBridgeViewBase.setCameraIndex(intent.getIntExtra("idCamera", 0));


        tf = new TensorFlowInferenceInterface(getAssets(),MODEL_PATH);

        labelMap = new HashMap<>();
        loadLabelData();
        loadTrainData();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String tempName = msg.obj.toString();
//                if(!tempName.equals("unknow")){
//                    uniqueNames.add(tempName);
//                    uniqueNamesArray = uniqueNames.toArray(new String[uniqueNames.size()]);
//                    StringBuilder strBuilder = new StringBuilder();
//                    for (int i = 0; i < uniqueNamesArray.length; i++) {
//                        strBuilder.append(uniqueNamesArray[i] + "\n");
//                    }
//                    String textToDisplay = strBuilder.toString();
//                    textView.setText(tempName);
//                }
                if(!tempName.equals("")){
                    imageView.setImageResource(R.drawable.ic_green);
                    textView.setText(tempName);
                }else {
                    imageView.setImageResource(R.drawable.ic_red);
                    textView.setText("unknow");
                }
            }

        };
     //   Toast.makeText(this, size+"", Toast.LENGTH_SHORT).show();

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


      //  Imgproc.resize(mGray, mGray, new Size(mGray.width(),(float)mGray.height()/ ((float)mGray.width()/ (float)mGray.height())));



        MatOfRect list_face = new MatOfRect();

   //    cascadeClassifier.detectMultiScale(mGray, list_face);
        cascadeClassifier.detectMultiScale(mGray,list_face,1.2,4,0|CASCADE_SCALE_IMAGE , new Size(50,50), new Size());

//
//        cascadeClassifier.detectMultiScale(mGray, list_face, 1.1,2,2,
//                new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        Rect[] list = list_face.toArray();

        if(list.length > 0) {
            Message msg = new Message();
            msg.obj="";
            for(int i = 0; i < list.length; i++) {
                Rect r = list[i];
                Mat m = mGray.submat(r);
//            int x1 = (int) r.x;
//            int y1 = (int) r.y;
//            int x2 = x1 + r.width;
//            int y2 = y1 + r.height;
//            Mat m = mGray.submat(y1, y2, x1, x2);
                Bitmap bitmapRecognize = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(m, bitmapRecognize);

                bitmap = bitmapRecognize;


                //Resize the image into 160 x 160
                Bitmap resized_image = ImageUtils.processBitmap(bitmapRecognize, 160);

                //Normalize the pixels
                floatValues = ImageUtils.normalizeBitmap(resized_image, 160, 80.5f, 1.0f);

                // 160 80.5f 1.0f

                //Pass input into the tensorflow
                tf.feed(INPUT_NAME, floatValues, 1, 160, 160, 3);

                //compute predictions
                tf.run(new String[]{OUTPUT_NAME});

                //copy the output into the PREDICTIONS array
                tf.fetch(OUTPUT_NAME, PREDICTIONS);

                float min = 99999;
                int vtMin = 0;
                for (int j = 0; j < size; j++) {
                    float dis = TinhKhoangCach(PREDICTIONS, value[j]);
                    if (dis < min) {
                        min = dis;
                        vtMin = (int) value[j][128];
                    }
                }

                String name = labelMap.get(vtMin);

                if (min < 0.3) {
                    msg.obj += name+"\n";
                } else {
                    msg.obj += "";
                }

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
