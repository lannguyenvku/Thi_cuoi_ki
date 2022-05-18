package com.example.myapplication.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.adapters.GroupAddedUsersAdapter;
import com.example.myapplication.adapters.GroupUsersAdapter;
import com.example.myapplication.databinding.ActivityCreateGroupBinding;
import com.example.myapplication.models.Group;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {

    public static ActivityCreateGroupBinding binding;
    private PreferenceManager preferenceManager;
    public static TextView createGroup;
    private FirebaseFirestore database;
    private String groupNameString;
    public static GroupUsersAdapter usersAdapter;
    public static RecyclerView addedUsersList;
    public static List<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        binding= ActivityCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        addedUsersList=binding.listUserAdded;

        createGroup=binding.createGroup;
        database=FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());

        getUsers();

        binding.imageBack.setOnClickListener(v-> {
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            GroupUsersAdapter.userCount =0;
            finish();
        });

        createGroup.setOnClickListener(v ->{
            createGroup();
        });

        binding.inputSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                usersAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                usersAdapter.getFilter().filter(newText);
                return false;
            }
        });

    }

    private void createGroup(){
        if(TextUtils.isEmpty(binding.groupName.getText().toString())){
            Toast.makeText(getApplicationContext(),"Vui lòng nhập tên nhóm..",Toast.LENGTH_SHORT).show();
        }else if(GroupUsersAdapter.userListAdded.size() <2 || GroupAddedUsersAdapter.usersAddeds.size() <2){
            Toast.makeText(getApplicationContext(),"Số lượng phải lớn hơn 2..",Toast.LENGTH_SHORT).show();
        }
        else
        {
            groupNameString=binding.groupName.getText().toString();

            GroupUsersAdapter.userListAdded.add(0,new User(preferenceManager.getString(Contants.KEY_NAME),preferenceManager.getString(Contants.KEY_IMAGE),preferenceManager.getString(Contants.KEY_EMAIL),null,preferenceManager.getString(Contants.KEY_USER_ID)));
            HashMap<String,Object> message = new HashMap<>();
            for(int i=0;i<GroupUsersAdapter.userListAdded.size();i++){
                message.put("user_id_"+i,GroupUsersAdapter.userListAdded.get(i).id);
                message.put("user_name_"+i,GroupUsersAdapter.userListAdded.get(i).name);
                message.put("user_image_"+i,GroupUsersAdapter.userListAdded.get(i).image);
                message.put("user_email_"+i,GroupUsersAdapter.userListAdded.get(i).email);
                message.put("group_name",groupNameString);
                message.put("lastMessage","");
                message.put("last_date_message",new Date());
                message.put("user_count",GroupUsersAdapter.userListAdded.size());
                message.put("date_create",new Date());
            }
            database.collection(Contants.KEY_COLLECTION_GROUP).add(message).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Group group=new Group();
                    group.name=groupNameString;
                    group.id=documentReference.getId();
                    group.image=preferenceManager.getString(Contants.KEY_IMAGE);
                    group.user_count= Long.valueOf(GroupUsersAdapter.userListAdded.size());
                    Intent intent=new Intent(getApplicationContext(),GroupChatActivity.class);
                    intent.putExtra("group",group);
                    startActivity(intent);
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(),"Lỗi, không thể tạo nhóm",Toast.LENGTH_SHORT).show();
                }
            });

        }

    }


    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Contants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Contants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult() != null){
                       users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user=new User(queryDocumentSnapshot.getString(Contants.KEY_NAME),queryDocumentSnapshot.getString(Contants.KEY_IMAGE),
                                    queryDocumentSnapshot.getString(Contants.KEY_EMAIL),queryDocumentSnapshot.getString(Contants.KEY_FCM_TOKEN),queryDocumentSnapshot.getId());
                            users.add(user);
                        }
                        if(users.size() >0){
                            usersAdapter = new GroupUsersAdapter(users);
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        }else{
                            showErrorMessage();
                        }
                    }else{
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s","Không có ai"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }


}