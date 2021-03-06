package com.example.recognize;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PersonAdapter extends ArrayAdapter<Person> {
    private Activity context;
    private int resource;
    private List<Person> objects;
    private ArrayList<Person> dsPerson;
    private float[][] trainValues = new float[100][129];
    private String[][] arrLabel;
    private int lengthLabelData;
    private int idPersonDelete = 0;


    public PersonAdapter(Activity context, int resource, List<Person> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = this.context.getLayoutInflater();
        View row = inflater.inflate(this.resource, null);

        TextView textViewName = row.findViewById(R.id.textViewName);
        TextView textViewId = row.findViewById(R.id.textViewId);
        ImageView imageViewDelete = row.findViewById(R.id.imageViewDelete);

        final Person person = this.objects.get(position);
        dsPerson = new ArrayList<>();

        textViewName.setText(person.getName());
        textViewId.setText(person.getId()+"");
        imageViewDelete.setImageResource(R.drawable.delete);

        imageViewDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder adb=new AlertDialog.Builder(getContext());
                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete " + person.getName());
                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        idPersonDelete = person.getId();
                        try {
                            delete();
                            ManageActivity.adtPerson.notifyDataSetChanged();
                            context.finish();
                            context.startActivity(context.getIntent());
                            Toast.makeText(getContext(), "Deleted " + person.getName(), Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }});
                adb.show();
            }

        });

        return row;
    }

    private void delete() throws IOException {

        trainValues = FileUtils.loadTrainData();
        arrLabel = FileUtils.loadLabelData();
        lengthLabelData = FileUtils.getLengthLabelData();
        loadData();


        // ghi file train
        File input = new File(FileUtils.myDir, "train_data");
        FileWriter fw = new FileWriter(input, false);
        for(int i = 0; i < dsPerson.size(); i++){
            if(trainValues[i][128] != idPersonDelete){
                for(int j = 0; j < 128; j++){
                    fw.append(trainValues[i][j]+" ");
                }
                fw.append((int)trainValues[i][128]+"\n");
            }
        }
        fw.close();


        //ghi file label
        input = new File(FileUtils.myDir, "label_data");
        fw = new FileWriter(input, false);
        for(int i = 0; i < dsPerson.size(); i++){
            if(dsPerson.get(i).getId() != idPersonDelete){
                fw.append(dsPerson.get(i).getName()+";"+dsPerson.get(i).getId()+"\n");
            }
        }
        fw.close();

    }

    private void loadData(){
        for(int i = 0; i < lengthLabelData; i++){
            Person person = new Person(Integer.parseInt(arrLabel[i][1]), arrLabel[i][0]);
            dsPerson.add(person);
        }
    }


}
