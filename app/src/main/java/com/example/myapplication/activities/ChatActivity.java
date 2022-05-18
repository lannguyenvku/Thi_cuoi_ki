package com.example.myapplication.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.myapplication.adapters.ChatAdapter;
import com.example.myapplication.databinding.ActivityChatBinding;
import com.example.myapplication.listeners.GroupListener;
import com.example.myapplication.listeners.UserListener;
import com.example.myapplication.models.ChatMessage;
import com.example.myapplication.models.Group;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.Objects;

public class ChatActivity extends BaseActivity implements UserListener, GroupListener {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId=null;
    private Boolean isReceiverAvailable=false;
    private Uri imageUri;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        receiverUser=(User) getIntent().getSerializableExtra(Contants.KEY_USER);

        setListeners();
            loadReceivedDetails();
            init();
            listenMessage();
    }

    private void init(){
        preferenceManager= new PreferenceManager(getApplicationContext());
        database=FirebaseFirestore.getInstance();
            chatMessages = new ArrayList<>();
            chatAdapter = new ChatAdapter(
                    chatMessages,
                    getBitmapFromEncodedString(receiverUser.image),
                    preferenceManager.getString(Contants.KEY_USER_ID)
            );
            binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void sendMessage(String encodedImage){
        HashMap<String,Object> message = new HashMap<>();
        String messageText=binding.inputMessage.getText().toString();
            message.put(Contants.KEY_SENDER_ID,preferenceManager.getString(Contants.KEY_USER_ID));
            message.put(Contants.KEY_RECEIVER_ID,receiverUser.id);
            if(messageText.isEmpty()){
                message.put(Contants.KEY_MESSAGE_IMAGE,encodedImage);
            }else{
                message.put(Contants.KEY_MESSAGE,messageText);
            }
            message.put(Contants.KEY_TIMESTAMP,new Date());
            database.collection(Contants.KEY_COLLECTION_CHAT).add(message);
            if(conversionId != null){
                if(messageText.isEmpty()){
                    updateConversion("IMAGE");
                }else{
                    updateConversion(messageText);
                }
            }else{
                HashMap<String ,Object> conversion = new HashMap<>();
                conversion.put(Contants.KEY_SENDER_ID, preferenceManager.getString(Contants.KEY_USER_ID));
                conversion.put(Contants.KEY_SENDER_NAME,preferenceManager.getString(Contants.KEY_NAME));
                conversion.put(Contants.KEY_SENDER_IMAGE,preferenceManager.getString(Contants.KEY_IMAGE));
                conversion.put(Contants.KEY_RECEIVER_ID,receiverUser.id);
                conversion.put(Contants.KEY_RECEIVER_NAME,receiverUser.name);
                conversion.put(Contants.KEY_RECEIVER_IMAGE,receiverUser.image);
                if(messageText.isEmpty()){
                    conversion.put(Contants.KEY_LAST_MESSAGE,"IMAGE");
                }else{
                    conversion.put(Contants.KEY_LAST_MESSAGE,messageText);
                }
                conversion.put(Contants.KEY_TIMESTAMP,new Date());
                addConversion(conversion);
            }
        binding.inputMessage.setText(null);
    }

    private void listenOffTime(){
            database.collection(Contants.KEY_COLLECTION_USERS).document(receiverUser.id)
                    .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    Date currentTime=new Date();
                    //Ngày
                    long onDay=documentSnapshot.getTimestamp("activeTime").toDate().getDate();
                    long curDay=currentTime.getDate();
                    long offDay=curDay-onDay;
                    //Giờ
                    long onTimeHour=documentSnapshot.getTimestamp("activeTime").toDate().getHours();
                    long curTimeHour=currentTime.getHours();
                    long offTimeHour=curTimeHour-onTimeHour;
                    //Phút
                    long onTimeMinute=documentSnapshot.getTimestamp("activeTime").toDate().getMinutes();
                    long curTimeMinute=currentTime.getMinutes();
                    long offTimeMinute=curTimeMinute-onTimeMinute;

                        if(offTimeHour>=1 && offDay <1){
                            binding.textOffAvailability.setText("Hoạt động "+String.valueOf(offTimeHour)+" giờ trước");
                        }
                        else if(offTimeHour<1 && offDay <1 && offTimeMinute >0){
                            binding.textOffAvailability.setText("Hoạt động "+String.valueOf(offTimeMinute)+" phút trước");
                        }
                        else if( offDay >= 1){
                            binding.textOffAvailability.setText("Hoạt động "+String.valueOf(offDay)+" ngày trước");
                        }
                        else{
                            binding.textOffAvailability.setText("Hoạt động vài giây trước");
                        }

                }
            });

    }

    private void listenAvailabilityOfReceiver(){
            database.collection(Contants.KEY_COLLECTION_USERS).document(
                    receiverUser.id
            ).addSnapshotListener(ChatActivity.this, (value, error) -> {
                if (error != null) {
                    return;
                }
                if (value != null) {
                    if (value.getLong(Contants.KEY_AVAILABILITY) != null) {
                        int availability = Objects.requireNonNull(
                                value.getLong(Contants.KEY_AVAILABILITY)
                        ).intValue();
                        isReceiverAvailable = availability == 1;
                    }
                }
                if (isReceiverAvailable) {
                    binding.textAvailability.setVisibility(View.VISIBLE);
                    binding.textOffAvailability.setVisibility(View.GONE);
                } else {
                    binding.textAvailability.setVisibility(View.GONE);
                    binding.textOffAvailability.setVisibility(View.VISIBLE);
                    listenOffTime();
                }
            });
    }

    private void listenMessage(){
            database.collection(Contants.KEY_COLLECTION_CHAT)
                    .whereEqualTo(Contants.KEY_SENDER_ID,preferenceManager.getString(Contants.KEY_USER_ID))
                    .whereEqualTo(Contants.KEY_RECEIVER_ID,receiverUser.id)
                    .addSnapshotListener(eventListener);
            database.collection(Contants.KEY_COLLECTION_CHAT)
                    .whereEqualTo(Contants.KEY_SENDER_ID,receiverUser.id)
                    .whereEqualTo(Contants.KEY_RECEIVER_ID,preferenceManager.getString(Contants.KEY_USER_ID))
                    .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value,error) ->{
        if(error != null){
            return;
        }
        if(value != null){
            int count =chatMessages.size();
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId=documentChange.getDocument().getString(Contants.KEY_SENDER_ID);
                    chatMessage.receiverId=documentChange.getDocument().getString(Contants.KEY_RECEIVER_ID);
                    chatMessage.message=documentChange.getDocument().getString(Contants.KEY_MESSAGE);
                    chatMessage.image_message=documentChange.getDocument().getString(Contants.KEY_MESSAGE_IMAGE);
                    chatMessage.dateTime= getReadableDatabase(documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP));
                    chatMessage.dateObject=documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages,(obj1,obj2)-> obj1.dateObject.compareTo(obj2.dateObject));
            if(count ==0){
                chatAdapter.notifyDataSetChanged();
            }else{
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() -1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversionId == null){
            checkForConversion();
        }
    };



    public static Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    private void loadReceivedDetails(){
            binding.textName.setText(receiverUser.name);
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v->{
            onBackPressed();
        });
        binding.layoutSend.setOnClickListener(v->sendMessage(null));
        binding.layoutSendImage.setOnClickListener(v ->  openGallery());
        binding.imageInfo.setOnClickListener(v->{
                User user=new User(receiverUser.name,receiverUser.image,"","",receiverUser.id);
                onUserClicked(user);
        });
    }

    private String getReadableDatabase(Date date){
   return new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String,Object> conversation){
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message){
            DocumentReference documentReference =
                    database.collection(Contants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
            documentReference.update(
                    Contants.KEY_LAST_MESSAGE,message,
                    Contants.KEY_SENDER_ID,preferenceManager.getString(Contants.KEY_USER_ID),
                    Contants.KEY_SENDER_NAME,preferenceManager.getString(Contants.KEY_NAME),
                    Contants.KEY_SENDER_IMAGE,preferenceManager.getString(Contants.KEY_IMAGE),
                    Contants.KEY_RECEIVER_ID,receiverUser.id,
                    Contants.KEY_RECEIVER_NAME,receiverUser.name,
                    Contants.KEY_RECEIVER_IMAGE,receiverUser.image,
                    Contants.KEY_TIMESTAMP, new Date()
            );
    }


    private void checkForConversion(){
        if(chatMessages.size() !=0){
                checkForConversionRemotely(
                        preferenceManager.getString(Contants.KEY_USER_ID),
                        receiverUser.id
                );
                checkForConversionRemotely(
                        receiverUser.id,
                        preferenceManager.getString(Contants.KEY_USER_ID)
                );
        }
    }

    private void checkForConversionRemotely(String senderId,String receiverId){
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Contants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Contants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() >0){
            DocumentSnapshot documentSnapshot=task.getResult().getDocuments().get(0);
            conversionId=documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),InfoActivity.class);
        intent.putExtra(Contants.KEY_USER,user);
        startActivity(intent);
    }

    @Override
    public void onGroupClicked(Group group) {
        Intent intent = new Intent(getApplicationContext(),InfoActivity.class);
        intent.putExtra("group",group);
        startActivity(intent);
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