package com.example.myapplication.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.adapters.UsersAdapter;
import com.example.myapplication.databinding.ActivityUserBinding;
import com.example.myapplication.listeners.UserListener;
import com.example.myapplication.models.Group;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.databinding.ActivityInfoBinding;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InfoActivity extends BaseActivity implements UserListener {
    private User receiverUser;
    private ActivityInfoBinding binding;
    private Group receiverGroup;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database=FirebaseFirestore.getInstance();

        receiverUser=(User) getIntent().getSerializableExtra(Contants.KEY_USER);
        receiverGroup=(Group) getIntent().getSerializableExtra("group");
        preferenceManager= new PreferenceManager(getApplicationContext());

        binding.imageBack.setOnClickListener(v-> {
            onBackPressed();
        });

        if(receiverUser != null){
            binding.infoName.setText(String.valueOf(receiverUser.name));
            binding.infoEmail.setText("ID :"+String.valueOf(receiverUser.id));
            binding.infoImage.setImageBitmap(getUserImage(receiverUser.image));

        }
        else{
            binding.infoEmail.setText("Danh sách thành viên :");
            FirebaseFirestore database= FirebaseFirestore.getInstance();
            database.collection(Contants.KEY_COLLECTION_GROUP).document(receiverGroup.id).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        List<User> groupUserList=new ArrayList<>();
                        String group_name=documentSnapshot.getString("group_name");
                        long group_user_count=documentSnapshot.getLong("user_count");
                        binding.infoImage.setImageBitmap(getUserImage(documentSnapshot.getString("user_image_0")));
                        binding.textName.setText("Thông tin nhóm");
                        binding.infoName.setText(String.valueOf(group_name));

                        for(int i=0;i< group_user_count;i++){
                            if(i==0 && documentSnapshot.getString("user_id_"+i).equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                                groupUserList.add(new User(documentSnapshot.getString("user_name_"+i)+" (Bạn) (Quản trị viên)",documentSnapshot.getString("user_image_"+i),
                                        documentSnapshot.getString("user_email_"+i),null,documentSnapshot.getString("user_id_"+i)));
                                binding.deleteGroup.setVisibility(View.VISIBLE);
                            }
                            else if(i==0){
                                groupUserList.add(new User(documentSnapshot.getString("user_name_"+i)+" (Quản trị viên)",documentSnapshot.getString("user_image_"+i),
                                        documentSnapshot.getString("user_email_"+i),null,documentSnapshot.getString("user_id_"+i)));
                            }
                            else if(documentSnapshot.getString("user_id_"+i).equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                                groupUserList.add(new User(documentSnapshot.getString("user_name_"+i)+" (Bạn)",documentSnapshot.getString("user_image_"+i),
                                        documentSnapshot.getString("user_email_"+i),null,documentSnapshot.getString("user_id_"+i)));
                            }
                            else{
                                groupUserList.add(new User(documentSnapshot.getString("user_name_"+i),documentSnapshot.getString("user_image_"+i),
                                        documentSnapshot.getString("user_email_"+i),null,documentSnapshot.getString("user_id_"+i)));
                            }
                        }
                        UsersAdapter usersAdapter=new UsersAdapter(groupUserList,this);
                      binding.groupUserList.setAdapter(usersAdapter);

                    });
            binding.deleteGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteGroup();
                }
            });
        }


    }

    private void deleteGroup()
    {
        AlertDialog.Builder builder=new AlertDialog.Builder(InfoActivity.this);
        builder.setCancelable(true);

        View view=getLayoutInflater().inflate(R.layout.alert_dialog_layout,null);
        Button canelB=view.findViewById(R.id.cancelB);
        Button confirmB=view.findViewById(R.id.confirmB);

        builder.setView(view);

        AlertDialog alertDialog=builder.create();

        canelB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        confirmB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.collection(Contants.KEY_COLLECTION_GROUP).document(receiverGroup.id).delete().addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Intent intent =new Intent(getApplicationContext(),MainActivity.class);
                                startActivity(intent);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                finish();
                            }
                        }
                );
            }
        });

        alertDialog.show();
    }



    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }


    @Override
    public void onUserClicked(User user) {

    }
}