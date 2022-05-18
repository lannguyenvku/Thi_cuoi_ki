package com.example.myapplication.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.adapters.ChatAdapter;
import com.example.myapplication.adapters.GroupChatAdapter;
import com.example.myapplication.databinding.ActivityChatBinding;
import com.example.myapplication.listeners.GroupListener;
import com.example.myapplication.models.ChatMessage;
import com.example.myapplication.models.Group;
import com.example.myapplication.models.GroupChatMessage;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class GroupChatActivity extends BaseActivity implements GroupListener {

    private ActivityChatBinding binding;
    private Group receiverGroup;
    private PreferenceManager preferenceManager;
    private List<GroupChatMessage> chatMessages;
    private GroupChatAdapter groupChatAdapter;
    private FirebaseFirestore database;
    private Uri imageUri;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        receiverGroup=(Group) getIntent().getSerializableExtra("group");
        binding.textName.setText(receiverGroup.name);


        setListeners();
        init();
        listenMessage();

    }

    private void init(){
        preferenceManager= new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<GroupChatMessage>();
        database= FirebaseFirestore.getInstance();

        chatMessages = new ArrayList<>();
        groupChatAdapter=new GroupChatAdapter( chatMessages,
                getBitmapFromEncodedString("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    "),
                preferenceManager.getString(Contants.KEY_USER_ID));
        binding.chatRecyclerView.setAdapter(groupChatAdapter);

    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v->{
            onBackPressed();
        });
        binding.layoutSend.setOnClickListener(v->
                sendMessage(null)
        );
        binding.layoutSendImage.setOnClickListener(v ->  openGallery());
        binding.imageInfo.setOnClickListener(v->{
            Group group=new Group();
            group.id=receiverGroup.id;
            onGroupClicked(group);
        });
    }


    private void listenMessage(){
            database.collection(Contants.KEY_COLLECTION_CHAT)
                    .whereEqualTo("group_id",receiverGroup.id)
                    .addSnapshotListener(eventListener);

    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) ->{
        if(error != null){
            return;
        }
        if(value != null){
            int count =chatMessages.size();
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    GroupChatMessage chatMessage = new GroupChatMessage();
                    chatMessage.senderId=documentChange.getDocument().getString(Contants.KEY_SENDER_ID);
                    chatMessage.groupId=documentChange.getDocument().getString(Contants.KEY_RECEIVER_ID);
                    chatMessage.senderName=documentChange.getDocument().getString(Contants.KEY_SENDER_NAME);
                    chatMessage.senderImage=documentChange.getDocument().getString(Contants.KEY_SENDER_IMAGE);
                    chatMessage.message=documentChange.getDocument().getString(Contants.KEY_MESSAGE);
                    chatMessage.image_message=documentChange.getDocument().getString(Contants.KEY_MESSAGE_IMAGE);
                    chatMessage.dateTime= getReadableDatabase(documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP));
                    chatMessage.dateObject=documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages,(obj1, obj2)-> obj1.dateObject.compareTo(obj2.dateObject));
            if(count ==0){
                groupChatAdapter.notifyDataSetChanged();
            }else{
                groupChatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() -1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    };

    private String getReadableDatabase(Date date){
        return new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void sendMessage(String encodedImage){
        String messageText=binding.inputMessage.getText().toString();
                        HashMap<String,Object> message = new HashMap<>();
                        GroupChatMessage chatMessage = new GroupChatMessage();
                        message.put("group_id",receiverGroup.id);
                        message.put(Contants.KEY_SENDER_ID,preferenceManager.getString(Contants.KEY_USER_ID));
                        message.put(Contants.KEY_SENDER_NAME,preferenceManager.getString(Contants.KEY_NAME));
                        message.put(Contants.KEY_SENDER_IMAGE,preferenceManager.getString(Contants.KEY_IMAGE));
        if(messageText.isEmpty()){
            message.put(Contants.KEY_MESSAGE_IMAGE,encodedImage);
        }else{
            message.put(Contants.KEY_MESSAGE,messageText);
        }
                        message.put(Contants.KEY_TIMESTAMP,new Date());
                        database.collection(Contants.KEY_COLLECTION_CHAT).add(message);

                        database.collection(Contants.KEY_COLLECTION_GROUP).document(receiverGroup.id)
                                .update("lastMessage",messageText,
                                        "last_date_message",new Date());

            database.collection(Contants.KEY_COLLECTION_GROUP).document(receiverGroup.id).get().addOnSuccessListener(
                    new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            HashMap<String ,Object> conversion = new HashMap<>();
                            conversion.put(Contants.KEY_SENDER_ID, preferenceManager.getString(Contants.KEY_USER_ID));
                            conversion.put(Contants.KEY_SENDER_NAME,preferenceManager.getString(Contants.KEY_NAME));
                            conversion.put(Contants.KEY_SENDER_IMAGE,preferenceManager.getString(Contants.KEY_IMAGE));
                            conversion.put("group_id",receiverGroup.id);
                            conversion.put("group_name",receiverGroup.name);
                            conversion.put("admin_image",receiverGroup.image);
                            conversion.put("leader_image",receiverGroup.second_image);
                            if(messageText.isEmpty()){
                                conversion.put(Contants.KEY_LAST_MESSAGE,"IMAGE");
                            }else{
                                conversion.put(Contants.KEY_LAST_MESSAGE,messageText);
                            }
                            conversion.put(Contants.KEY_TIMESTAMP,new Date());
                            conversion.put("group_user_count",receiverGroup.user_count);
                            for(int i=0;i<receiverGroup.user_count;i++){
                                if(!documentSnapshot.getString("user_id_"+i).equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                                    conversion.put(Contants.KEY_RECEIVER_ID+"_"+i,documentSnapshot.getString("user_id_"+i));
                                    conversion.put(Contants.KEY_RECEIVER_NAME+"_"+i,documentSnapshot.getString("user_name_"+i));
                                    conversion.put(Contants.KEY_RECEIVER_IMAGE+"_"+i,documentSnapshot.getString("user_image_"+i));
                                }
                            }
                        }
                    }
            );

        binding.inputMessage.setText(null);

    }


    @Override
    public void onGroupClicked(Group group) {
        Intent intent = new Intent(getApplicationContext(),InfoActivity.class);
        intent.putExtra("group",group);
        startActivity(intent);
        finish();
    }



    public static Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    private void openGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);
    }


    private String encodedImage(Bitmap bitmap){
        int previewWidth=180;
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
                            encodedImage=encodedImage(bitmap);
                            sendMessage(encodedImage);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );


}