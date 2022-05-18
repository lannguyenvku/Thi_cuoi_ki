package com.example.myapplication.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.activities.ChatActivity;
import com.example.myapplication.databinding.ItemContainerGroupMessageBinding;
import com.example.myapplication.databinding.ItemContainerReceivedMessageBinding;
import com.example.myapplication.databinding.ItemContainerSentMessageBinding;
import com.example.myapplication.models.ChatMessage;
import com.example.myapplication.models.GroupChatMessage;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;

import java.util.List;

public class GroupChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private final List<GroupChatMessage> chatMessages;
    private final Bitmap receiverProfileImage;
    private final String senderId;


    public GroupChatAdapter(List<GroupChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MessageViewHolder(
                    ItemContainerGroupMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((MessageViewHolder) holder).setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }



    static class MessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainerGroupMessageBinding binding;
        private PreferenceManager preferenceManager;

        MessageViewHolder(ItemContainerGroupMessageBinding itemContainerGroupMessageBinding){
            super(itemContainerGroupMessageBinding.getRoot());
            binding =itemContainerGroupMessageBinding;
            preferenceManager= new PreferenceManager(binding.getRoot().getContext());

        }


        void setData(GroupChatMessage chatMessage){
            if(chatMessage.senderId.equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                binding.name.setText("Bạn");
            }
            else{
                binding.name.setText(chatMessage.senderName);
            }
            binding.imageProfile.setImageBitmap(ChatActivity.getBitmapFromEncodedString(String.valueOf(chatMessage.senderImage)));
            if(chatMessage.message != null){
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.textMessage.setText(chatMessage.message);
                binding.imageMessage.setVisibility(View.GONE);
            }else {
                binding.textMessage.setVisibility(View.GONE);
                binding.imageMessage.setVisibility(View.VISIBLE);
                binding.imageMessage.setImageBitmap(ChatActivity.getBitmapFromEncodedString(String.valueOf(chatMessage.image_message)));
            }
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }


}
