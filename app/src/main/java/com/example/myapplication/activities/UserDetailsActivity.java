package com.example.myapplication.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityInfoBinding;
import com.example.myapplication.databinding.ActivityUserDetailsBinding;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UserDetailsActivity extends BaseActivity {

    private ActivityUserDetailsBinding binding;
    private PreferenceManager preferenceManager;
    private BottomSheetDialog bottomSheetDialog,bsDialogEditName;

    private Uri imageUri;

    private String encodedImage;
    private  FirebaseFirestore database;
    private ProgressDialog progressDialog;
    public static String timeOff = "hh:mm a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityUserDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        preferenceManager=new PreferenceManager(getApplicationContext());
        getInfo();

        binding.imageBack.setOnClickListener(v-> {
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        });
        binding.signOutBtn.setOnClickListener(v-> signOut());

        database = FirebaseFirestore.getInstance();
        progressDialog=new ProgressDialog(this);

        initActionClick();

    }

    private void getInfo(){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Contants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Contants.KEY_USER_ID)).get().addOnSuccessListener(documentSnapshot -> {
  String userName=documentSnapshot.getString("name");
  String imageProfile=documentSnapshot.getString("image");
            Glide.with(UserDetailsActivity.this).load(getUserImage(imageProfile)).into(binding.infoImage);
            binding.infoName.setText(userName);
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

    }


    private void initActionClick() {
        binding.fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomSheetPickPhoto();
            }
        });
        binding.lnEditName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomSheetEditName();
            }
        });
    }

    private void showBottomSheetPickPhoto(){
     View view=getLayoutInflater().inflate(R.layout.bottom_sheet_pick,null);

        ((View) view.findViewById(R.id.ln_gallery)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
bottomSheetDialog.dismiss();
            }
        });

        ((View) view.findViewById(R.id.ln_camera)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"Camera",Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog=new BottomSheetDialog(this);
     bottomSheetDialog.setContentView(view);

     bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogInterface dialogInterface) {
             bottomSheetDialog=null;
         }
     });

        bottomSheetDialog.show();

    }

    private void showBottomSheetEditName(){
        View view=getLayoutInflater().inflate(R.layout.bottom_sheet_edit_name,null);
final EditText edUserName=view.findViewById(R.id.ed_username);
        bsDialogEditName=new BottomSheetDialog(this);
        bsDialogEditName.setContentView(view);
        edUserName.setText(preferenceManager.getString(Contants.KEY_NAME));
        ((View) view.findViewById(R.id.btn_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsDialogEditName.dismiss();
            }
        });

        ((View) view.findViewById(R.id.btn_save)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(edUserName.getText().toString()))
                {
                    showToast("Lỗi, vui lòng nhập tên !");
                } else if((edUserName.getText().toString()).equals(preferenceManager.getString(Contants.KEY_NAME))){
                    showToast("Lỗi, giống tên cũ !");
                }
                else{
                    updateNameToFirebase(edUserName.getText().toString());
                }

                bsDialogEditName.dismiss();
            }
        });

        bsDialogEditName.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                bsDialogEditName=null;
            }
        });

        bsDialogEditName.show();

    }

    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    private void signOut(){
        showToast("Đang đăng xuất...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Contants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Contants.KEY_USER_ID)
                );
        HashMap<String , Object> updates = new HashMap<>();
        updates.put(Contants.KEY_FCM_TOKEN, FieldValue.delete());
        updates.put(Contants.KEY_AVAILABILITY,0);
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    Intent intent = new Intent(getApplicationContext(),SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e->showToast("Không thể đăng xuất"));
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }


    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);
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
                        imageUri = result.getData().getData();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.infoImage.setImageBitmap(bitmap);
                            encodedImage=encodedImage(bitmap);
                            updateImageToFirebase(encodedImage);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void updateImageToFirebase(String encodedImage){
        progressDialog.setMessage("Đang tải...");
        progressDialog.show();
        DocumentReference documentReference =
                database.collection(Contants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Contants.KEY_USER_ID)
                );
        documentReference.update(Contants.KEY_IMAGE,encodedImage).addOnSuccessListener(e -> {
            progressDialog.dismiss();
            showToast("Cập nhật thành công !");

            database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String documentID=document.getId();

                                    database.collection(Contants.KEY_COLLECTION_CONVERSATIONS).document(documentID)
                                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(documentSnapshot.getString(Contants.KEY_RECEIVER_ID).equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                                                database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                                                        .document(documentSnapshot.getId())
                                                        .update(Contants.KEY_RECEIVER_IMAGE,encodedImage);
                                            }
                                            else
                                            {
                                                database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                                                        .document(documentSnapshot.getId())
                                                        .update(Contants.KEY_SENDER_IMAGE,encodedImage);
                                            }
                                        }
                                    });

                                }
                            }
                        }
                    });

            database.collection(Contants.KEY_COLLECTION_GROUP)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String documentID=document.getId();

                                    database.collection(Contants.KEY_COLLECTION_GROUP).document(documentID)
                                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            long user_count=documentSnapshot.getLong("user_count");
                                            for(int i=0;i<user_count;i++){
                                                if(documentSnapshot.getString("user_id_"+i).equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                                                    database.collection(Contants.KEY_COLLECTION_GROUP)
                                                            .document(documentSnapshot.getId())
                                                            .update("user_image_0",encodedImage);
                                                }
                                            }

                                        }
                                    });

                                }
                            }
                        }
                    });


        }).addOnFailureListener(e->{
                    showToast("Lỗi !");
                    progressDialog.dismiss();
                } );

    }

    private void updateNameToFirebase(String newName){
        progressDialog.setMessage("Đang tải...");
        progressDialog.show();
        DocumentReference documentReference =
                database.collection(Contants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Contants.KEY_USER_ID)
                );
        documentReference.update(Contants.KEY_NAME,newName).addOnSuccessListener(e -> {
            progressDialog.dismiss();
            showToast("Cập nhật thành công !");
            getInfo();

            database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String documentID=document.getId();

                                    database.collection(Contants.KEY_COLLECTION_CONVERSATIONS).document(documentID)
                                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            if(documentSnapshot.getString(Contants.KEY_RECEIVER_ID).equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                                                database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                                                        .document(documentSnapshot.getId())
                                                        .update(Contants.KEY_RECEIVER_NAME,newName);
                                            }
                                            else
                                            {
                                                database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                                                        .document(documentSnapshot.getId())
                                                        .update(Contants.KEY_SENDER_NAME,newName);
                                            }
                                        }
                                    });

                                }
                            }
                        }
                    });

            database.collection(Contants.KEY_COLLECTION_GROUP)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String documentID=document.getId();

                                    database.collection(Contants.KEY_COLLECTION_GROUP).document(documentID)
                                            .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                        @Override
                                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                                            long user_count=documentSnapshot.getLong("user_count");
                                            for(int i=0;i<user_count;i++){
                                                if(documentSnapshot.getString("user_id_"+i).equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                                                    database.collection(Contants.KEY_COLLECTION_GROUP)
                                                            .document(documentSnapshot.getId())
                                                            .update("user_name_0",newName);
                                                }
                                            }

                                        }
                                    });

                                }
                            }
                        }
                    });


        }).addOnFailureListener(e->{
                    showToast("Lỗi !");
                    progressDialog.dismiss();
                } );


    }




}