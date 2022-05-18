package com.example.myapplication.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.GroupsAdapter;
import com.example.myapplication.listeners.GroupListener;
import com.example.myapplication.listeners.UserListener;
import com.example.myapplication.adapters.UsersAdapter;
import com.example.myapplication.databinding.ActivityUserBinding;
import com.example.myapplication.models.Group;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserActivity extends BaseActivity implements UserListener{

    private ActivityUserBinding binding;
    private PreferenceManager preferenceManager;
    private BottomSheetDialog bsDialogCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v-> onBackPressed());
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
                        List<User> users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user =new User(queryDocumentSnapshot.getString(Contants.KEY_NAME)
                            ,queryDocumentSnapshot.getString(Contants.KEY_IMAGE),
                                    queryDocumentSnapshot.getString(Contants.KEY_EMAIL),
                                    queryDocumentSnapshot.getString(Contants.KEY_FCM_TOKEN),
                                    queryDocumentSnapshot.getId());
                            users.add(user);
                        }
                        if(users.size() >0){
                            UsersAdapter usersAdapter = new UsersAdapter(users,this);
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                            binding.textErrorMessage.setVisibility(View.GONE);
                        }else{
                            binding.textErrorMessage.setText(String.format("%s","Không có bạn bè "));
                            binding.textErrorMessage.setVisibility(View.VISIBLE);
                        }
                    }else{
                        binding.textErrorMessage.setText(String.format("%s","Không có bạn bè "));
                        binding.textErrorMessage.setVisibility(View.VISIBLE);
                    }
                });
    }


    private void loading(Boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Contants.KEY_USER,user);
        startActivity(intent);
    }

}