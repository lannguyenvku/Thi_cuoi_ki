package com.example.myapplication.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.example.myapplication.adapters.GroupsAdapter;
import com.example.myapplication.adapters.RecentConversationsAdapter;
import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.listeners.ConversionListener;
import com.example.myapplication.listeners.GroupListener;
import com.example.myapplication.models.ChatMessage;
import com.example.myapplication.models.Group;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener, GroupListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> converations;
    private List<Group> groups;
    private RecentConversationsAdapter conversationsAdapter;
    private GroupsAdapter groupsAdapter;
    private FirebaseFirestore database;
    private BottomSheetDialog bsDialogCreate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager=new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
         setListeners();
         listenConversations();
    }

    private void init(){
        converations=new ArrayList<>();
        groups=new ArrayList<>();
        conversationsAdapter=new RecentConversationsAdapter(converations,this);
        groupsAdapter=new GroupsAdapter(groups,this);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
        database=FirebaseFirestore.getInstance();
        binding.friend.setEnabled(false);
    }

    private void setListeners(){
        binding.fabNewChat.setOnClickListener(v->
                startActivity(new Intent(getApplicationContext(),UserActivity.class)));
        binding.imageProfile.setOnClickListener(v->{
            openUserDetails();
        });
        binding.createNew.setOnClickListener(v->{
            openCreateBottom();
        });
        binding.friend.setOnClickListener(v -> {
                showFriendChat();
            });
        binding.group.setOnClickListener(v->{
            showGroupChat();
        });
    }


    private void showFriendChat(){
        binding.friend.setEnabled(false);
        binding.group.setEnabled(true);
        binding.conversationsRecyclerView.setAdapter(conversationsAdapter);
    }

    private void showGroupChat(){
        binding.group.setEnabled(false);
        binding.friend.setEnabled(true);
        binding.conversationsRecyclerView.setAdapter(groupsAdapter);
    }

    private void openUserDetails(){
        Intent intent=new Intent(getApplicationContext(),UserDetailsActivity.class);
        startActivity(intent);
    }

    private void openCreateBottom(){
        View view=getLayoutInflater().inflate(R.layout.bottom_sheet_pick_feature,null);
        bsDialogCreate=new BottomSheetDialog(this);
        bsDialogCreate.setContentView(view);
        ((View) view.findViewById(R.id.ln_createGroup)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Intent intent = new Intent(getApplicationContext(), CreateGroupActivity.class);
               startActivity(intent);
            }
        });

        bsDialogCreate.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                bsDialogCreate=null;
            }
        });

        bsDialogCreate.show();

    }


    private void loadUserDetails(){
        database.collection(Contants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Contants.KEY_USER_ID)).get().addOnSuccessListener(documentSnapshot -> {
            String userName=documentSnapshot.getString("name");
            String imageProfile=documentSnapshot.getString("image");
            byte[] bytes= Base64.decode(imageProfile,Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

            binding.imageProfile.setImageBitmap(bitmap);
        });


    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void listenConversations(){
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Contants.KEY_SENDER_ID,preferenceManager.getString(Contants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Contants.KEY_RECEIVER_ID,preferenceManager.getString(Contants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

        database.collection(Contants.KEY_COLLECTION_GROUP).get().addOnCompleteListener(
                new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() != null){
                            for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                for(int i=0;i<queryDocumentSnapshot.getLong("user_count");i++){
                                    if (preferenceManager.getString(Contants.KEY_USER_ID).equals(queryDocumentSnapshot.getString("user_id_" +i))) {
                                        database.collection(Contants.KEY_COLLECTION_GROUP)
                                                .whereEqualTo("user_id_"+i,preferenceManager.getString(Contants.KEY_USER_ID))
                                                .addSnapshotListener(groupEventListener);
                                    }
                                    }
                            }
                            }
                    }
                }
        );

    }

    private final EventListener<QuerySnapshot> eventListener=(value,error) ->{
      if(error != null){
          return;
      }
      if(value != null){
          for(DocumentChange documentChange : value.getDocumentChanges()){
              if(documentChange.getType() == DocumentChange.Type.ADDED){
                  String senderId= documentChange.getDocument().getString(Contants.KEY_SENDER_ID);
                  String receiverId = documentChange.getDocument().getString(Contants.KEY_RECEIVER_ID);
                  ChatMessage chatMessage = new ChatMessage();
                  chatMessage.senderId =senderId;
                  chatMessage.receiverId=receiverId;
                  if(preferenceManager.getString(Contants.KEY_USER_ID).equals(senderId)){
                      chatMessage.conversionImage=documentChange.getDocument().getString(Contants.KEY_RECEIVER_IMAGE);
                      chatMessage.conversionName=documentChange.getDocument().getString(Contants.KEY_RECEIVER_NAME );
                      chatMessage.conversionId=documentChange.getDocument().getString(Contants.KEY_RECEIVER_ID);
                  }else{
                      chatMessage.conversionImage=documentChange.getDocument().getString(Contants.KEY_SENDER_IMAGE);
                      chatMessage.conversionName=documentChange.getDocument().getString(Contants.KEY_SENDER_NAME );
                      chatMessage.conversionId=documentChange.getDocument().getString(Contants.KEY_SENDER_ID);
                  }
                  chatMessage.message=documentChange.getDocument().getString(Contants.KEY_LAST_MESSAGE);
                  chatMessage.image_message=documentChange.getDocument().getString(Contants.KEY_MESSAGE_IMAGE);
                  chatMessage.dateObject=documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP);
                  converations.add(chatMessage);
              }else if(documentChange.getType() == DocumentChange.Type.MODIFIED){
                  for (int i=0;i<converations.size();i++){
                      String senderId= documentChange.getDocument().getString(Contants.KEY_SENDER_ID);
                      String receiverId= documentChange.getDocument().getString(Contants.KEY_RECEIVER_ID);
                      if(converations.get(i).senderId.equals(senderId) && converations.get(i).receiverId.equals(receiverId)){
                          converations.get(i).message=documentChange.getDocument().getString(Contants.KEY_LAST_MESSAGE);
                          converations.get(i).dateObject = documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP);
                          break;
                      }
                  }
              }
          }
          Collections.sort(converations,(obj1,obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
          conversationsAdapter.notifyDataSetChanged();
          binding.conversationsRecyclerView.smoothScrollToPosition(0);
          binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
          binding.progressBar.setVisibility(View.GONE);
      }
    };


    private final EventListener<QuerySnapshot> groupEventListener=(value,error) ->{
        if(error != null){
            return;
        }
        if(value != null){
            for(DocumentChange documentChange : value.getDocumentChanges()){
                    String groupId = documentChange.getDocument().getId();
                    Group group=new Group();
                group.id=groupId;
                group.name=documentChange.getDocument().getString("group_name");
                group.user_count=documentChange.getDocument().getLong("user_count");
                group.image= documentChange.getDocument().getString("user_image_0");
                group.second_image=documentChange.getDocument().getString("user_image_1");
                group.lastMessage=documentChange.getDocument().getString(Contants.KEY_LAST_MESSAGE);
                group.lastDateMessage=documentChange.getDocument().getDate("last_date_message");
                    groups.add(group);
            }
            Collections.sort(groups,(obj1,obj2) -> obj2.lastDateMessage.compareTo(obj1.lastDateMessage));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };


    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){
        FirebaseFirestore database =FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Contants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Contants.KEY_USER_ID)
                );
        documentReference.update(Contants.KEY_FCM_TOKEN,token)
                .addOnFailureListener(e-> showToast("Lỗi token"));
    }

    @Override
    public void onConversionClicked(User user) {
            Intent intent=new Intent(getApplicationContext(),ChatActivity.class);
            intent.putExtra(Contants.KEY_USER,user);
            startActivity(intent);
    }

    @Override
    public void onGroupClicked(Group group) {
        Intent intent=new Intent(getApplicationContext(),GroupChatActivity.class);
        intent.putExtra("group",group);
        startActivity(intent);
    }
}
