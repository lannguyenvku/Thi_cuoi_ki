package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivitySignInBinding;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager=new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Contants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding= ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
    }

    private void setListener(){
        binding.textCreateNewAccount.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
         binding.buttonSignIn.setOnClickListener(v->{
             if(isValidSignInDetails()){
                 signIn();
             }
         });
    }

    private void signIn(){
  loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Contants.KEY_COLLECTION_USERS)
                .whereEqualTo(Contants.KEY_EMAIL,binding.inputEmail.getText().toString())
                .whereEqualTo(Contants.KEY_PASSWORD,binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() !=null
                    && task.getResult().getDocuments().size() >0){
                        DocumentSnapshot documentSnapshot=task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Contants.KEY_IS_SIGNED_IN,true);
                        preferenceManager.putString("order",String.valueOf(documentSnapshot.getLong("order")));
                        preferenceManager.putString(Contants.KEY_USER_ID,documentSnapshot.getId());
                        preferenceManager.putString(Contants.KEY_NAME,documentSnapshot.getString(Contants.KEY_NAME));
                        preferenceManager.putString(Contants.KEY_IMAGE,documentSnapshot.getString(Contants.KEY_IMAGE));
                        preferenceManager.putString(Contants.KEY_EMAIL,documentSnapshot.getString(Contants.KEY_EMAIL));
                    Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    }else{
                        loading(false);
                        showToast("Không thể đăng nhập");
                    }
                });
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails(){
        if(binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Nhập email");
            return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Nhập email hợp lệ");
            return false;
        }else if (binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Nhập mật khẩu");
            return false;
        }else{
            return true;
        }
    }

}