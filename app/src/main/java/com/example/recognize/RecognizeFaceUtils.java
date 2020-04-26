package com.example.recognize;

import android.content.Context;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class RecognizeFaceUtils {
    public static final String MODEL_PATH = "file:///android_asset/optimized_facenet.pb";
    public static final String INPUT_NAME = "input";
    public static final String OUTPUT_NAME = "embeddings";



    public static float[] predict(TensorFlowInferenceInterface tf, float values[]){
        float[] PREDICTIONS = new float[128];
        //Pass input into the tensorflow
        tf.feed(INPUT_NAME, values, 1, 160, 160, 3);

        //compute predictions
        tf.run(new String[]{OUTPUT_NAME});

        //copy the output into the PREDICTIONS array
        tf.fetch(OUTPUT_NAME, PREDICTIONS);
        return PREDICTIONS;
    }

}
