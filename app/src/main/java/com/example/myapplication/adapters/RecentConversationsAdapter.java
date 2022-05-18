package com.example.myapplication.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemContainerRecentConversionBinding;
import com.example.myapplication.listeners.ConversionListener;
import com.example.myapplication.models.ChatMessage;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.Contants;
import com.example.myapplication.utilities.PreferenceManager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;

    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener=conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
    holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder{
        ItemContainerRecentConversionBinding binding;
        PreferenceManager preferenceManager;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding){
            super(itemContainerRecentConversionBinding.getRoot());
            binding=itemContainerRecentConversionBinding;
            preferenceManager= new PreferenceManager(binding.getRoot().getContext());
        }

        void setData(ChatMessage chatMessage){
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            if(chatMessage.message.equals("IMAGE")){
                if(chatMessage.senderId.equals(preferenceManager.getString(Contants.KEY_USER_ID))){
                    binding.textRecentMessage.setText("Bạn đã gửi 1 ảnh");
                }else{
                    String[] parts = chatMessage.conversionName.split(" ");
                    binding.textRecentMessage.setText(parts[0]+" đã gửi 1 ảnh");
                }
            }else{
                binding.textRecentMessage.setText(chatMessage.message);
            }
            Calendar calendar = Calendar.getInstance();
            int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
            int messDay=chatMessage.dateObject.getDate();
            if(messDay == currentDay){
                binding.day.setText(String.valueOf(chatMessage.dateObject.getHours()+":"+chatMessage.dateObject.getMinutes()));
            }
            else{
                binding.day.setText(DateFormat.getDateInstance().format(chatMessage.dateObject));
            }

            binding.getRoot().setOnClickListener(v->{
                User user = new User(chatMessage.conversionName,chatMessage.conversionImage,null,null,chatMessage.conversionId);
                conversionListener.onConversionClicked(user);
            });
        }
    }

    private Bitmap getConversionImage(String encodedImage){
        byte[] bytes= Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }


}
