package com.example.recognize;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileUtils {
    static String root = Environment.getExternalStorageDirectory().toString();
    static File myDir = new File(root + "/recognize");
    private static final int MAXCAPACITY = 100;
    static int lengthTrainData;
    static int lengthLabelData;



    public static String[][] loadLabelData(){
        String arrLabel[][] = new String[MAXCAPACITY][2];
        File output = new File(myDir, "label_data");
        int i = 0;
        try {
            BufferedReader buf = new BufferedReader(new FileReader(output));
            String s = "";
            while ((s = buf.readLine()) != null) {
                String[] split = s.split(";");
                // labelMap.put(Integer.parseInt(split[1]),split[0]);
                arrLabel[i][0] = split[0];
                arrLabel[i][1] = split[1];
                i++;
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lengthLabelData = i;
        return arrLabel;
    }

    public static float[][] loadTrainData(){
        float[][] value = new float[MAXCAPACITY][129];
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
                lengthTrainData++;
                i++;
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }


    public static void writeTrainData(int id, float[] PREDICTIONS){
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
    }

    public static void writeLabelData(String name, int id){
        File output = new File(myDir, "label_data");
        try {
            FileWriter fw = new FileWriter(output, true);
            fw.append(name+";"+id+"\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getLengthTrainData(){
        return lengthTrainData;
    }

    public static int getLengthLabelData(){
        return lengthLabelData;
    }

}
