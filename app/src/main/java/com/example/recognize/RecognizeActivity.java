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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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


import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

public class RecognizeActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private Intent intent = getIntent();
    private TextView textView;
    private ImageView imageView;
    private static final String TAG = "Recognize::Activity";
    private CascadeClassifier cascadeClassifier;
    private DetectFaceUtils detectFaceUtils;
    private float[] floatValues = new float[160 * 160 * 3];
    private String[][] arrLabel;
    private int lengthLabelData;
    private Handler mHandler;
    private int lengthTrainData = 0;
    private TensorFlowInferenceInterface tfModel;
    private float[] PREDICTIONS = new float[128];
    private float[][] value = new float[FileUtils.MAXCAPACITY][129];

    private Mat mRgba, mGray;
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


    private float computeDistance(float[] x, float[] y){
        float sum = 0;
        for(int i = 0; i < 128; i++){
            sum += (x[i]-y[i])*(x[i]-y[i]);
        }
        return (float) Math.sqrt(sum);
    }


    private String getName(int id){
        for(int i = 0; i < lengthLabelData; i++){
            if(Integer.parseInt(arrLabel[i][1]) == id)
                return arrLabel[i][0];
        }
        return "Unknow";
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
        cameraBridgeViewBase.setCameraIndex(intent.getIntExtra("idCamera", 0));


        tfModel = new TensorFlowInferenceInterface(getAssets(),RecognizeFaceUtils.MODEL_PATH);

        arrLabel =  FileUtils.loadLabelData();
        lengthLabelData = FileUtils.getLengthLabelData();
        value = FileUtils.loadTrainData();
        lengthTrainData = FileUtils.getLengthTrainData();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String tempName = msg.obj.toString();
                if(!tempName.equals("")){
                    imageView.setImageResource(R.drawable.ic_green);
                    textView.setText(tempName);
                }else {
                    imageView.setImageResource(R.drawable.ic_red);
                    textView.setText("unknow");
                }
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

        cascadeClassifier.detectMultiScale(mGray,list_face,1.2,6,0|CASCADE_SCALE_IMAGE , new Size(70,70), new Size());

        Rect[] list = list_face.toArray();

        if(list.length > 0) {
            Message msg = new Message();
            msg.obj="";
            for(int i = 0; i < list.length; i++) {
                Rect r = list[i];
                Mat m = mGray.submat(r);

                // tang do tuong phan, can bang sang
                m = ImageUtils.equalizeImage(m);

                // Giam nhieu cua anh
                m = ImageUtils.medianBlur(m);

                Bitmap bitmapRecognize = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(m, bitmapRecognize);

            //    bitmap = bitmapRecognize;


                //Resize the image into 160 x 160
                Bitmap resized_image = ImageUtils.processBitmap(bitmapRecognize, 160);

                //Normalize the pixels
                floatValues = ImageUtils.normalizeBitmap(resized_image, 160, 80.5f, 1.0f);

                PREDICTIONS = RecognizeFaceUtils.predict(tfModel, floatValues);

                float min = 99999;
                int idMin = 0;
                for (int j = 0; j < lengthTrainData; j++) {
                    float dis = computeDistance(PREDICTIONS, value[j]);
                    if (dis < min) {
                        min = dis;
                        idMin = (int) value[j][128];
                    }
                }

             //   String name = labelMap.get(vtMin);
                String name = getName(idMin);

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
                    ImageUtils.FACE_RECT_COLOR,2);
        }


        return mRgba;
    }
}
