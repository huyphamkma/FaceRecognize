package com.example.recognize;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ManageActivity extends AppCompatActivity {
    public static ListView listView;
    ArrayList<Person> dsPerson;
    public static PersonAdapter adtPerson;
    String root = Environment.getExternalStorageDirectory().toString();
    File myDir = new File(root + "/recognize");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage);

        listView = findViewById(R.id.listView);
        dsPerson = new ArrayList<>();
        loadLabelData();
        adtPerson = new PersonAdapter(ManageActivity.this, R.layout.item_listview, dsPerson);
        listView.setAdapter(adtPerson);

    }


    private void loadLabelData(){
        File output = new File(myDir, "label_data");
        try {
            BufferedReader buf = new BufferedReader(new FileReader(output));
            String s = "";
            while ((s = buf.readLine()) != null) {
                String[] split = s.split(";");
                Person person = new Person(Integer.parseInt(split[1]), split[0]);
                if(!isContainsPerson(person)){
                    dsPerson.add(person);
                }
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean isContainsPerson(Person person){
        for (int i = 0; i < dsPerson.size(); i++){
            if(dsPerson.get(i).getId() == person.getId())
                return true;
        }
        return false;
    }
}
