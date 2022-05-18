package com.example.myapplication.activities;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivitySignUpBinding;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;
    private int order;
    private List userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userList=new ArrayList();
        preferenceManager= new PreferenceManager(getApplicationContext());
        setListener();
    }

    private void setListener(){
        binding.textSignIn.setOnClickListener(v-> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v->{
            if(isValidSignUpDetails()){
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void signUp(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
database.collection(Contants.KEY_COLLECTION_USERS).get().addOnCompleteListener(
        new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                HashMap<String,Object> user = new HashMap<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    userList.add(document.getId());
                }
                order=userList.size();
                user.put("order",order+1);
                user.put(Contants.KEY_NAME,binding.inputName.getText().toString());
                user.put(Contants.KEY_EMAIL,binding.inputEmail.getText().toString());
                user.put(Contants.KEY_PASSWORD,binding.inputPassword.getText().toString());
                user.put("activeTime",new Date());
                user.put(Contants.KEY_IMAGE,encodedImage);

                database.collection(Contants.KEY_COLLECTION_USERS).add(user)
                        .addOnSuccessListener(documentReference -> {
                            loading(false);
                            preferenceManager.putBoolean(Contants.KEY_IS_SIGNED_IN,true);
                            preferenceManager.putString(Contants.KEY_USER_ID,documentReference.getId());
                            preferenceManager.putString(Contants.KEY_NAME,binding.inputName.getText().toString());
                            preferenceManager.putString(Contants.KEY_IMAGE,encodedImage);
                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .addOnFailureListener(exception ->{
                            loading(false);
                            showToast(exception.getMessage());
                        });

            }
        }
);
    }

    private String encodedImage(Bitmap bitmap){
        int previewWidth=150;
        int previewHeight=bitmap.getHeight() * previewWidth/bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode()== RESULT_OK){
                    if(result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage=encodedImage(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean isValidSignUpDetails() {
        if (encodedImage == null) {
            showToast("Chọn ảnh đại diện");
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Nhập tên");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Nhập email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Nhập email hợp lệ");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Nhập mật khẩu");
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Nhập lại mật khẩu");
            return false;
        } else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("Mật khẩu không trùng khớp");
            return false;
        } else {
            return true;
        }
    }

     private void loading(Boolean isLoading){
       if (isLoading){
           binding.buttonSignUp.setVisibility(View.INVISIBLE);
           binding.progressBar.setVisibility(View.VISIBLE);
       }else{
           binding.progressBar.setVisibility(View.INVISIBLE);
           binding.buttonSignUp.setVisibility(View.VISIBLE);
       }
        }


    }
