package com.example.myapplication.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemContainerGroupBinding;
import com.example.myapplication.listeners.GroupListener;
import com.example.myapplication.models.Group;
import com.example.myapplication.utilities.Contants;

import java.text.DateFormat;
import java.util.List;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.UserViewHolder>{

    private final List<Group> groups;
    private final GroupListener groupListener;


    public GroupsAdapter(List<Group> groups, GroupListener groupListener) {
        this.groups = groups;
        this.groupListener=groupListener;
    }

    @NonNull
    @Override
    public GroupsAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerGroupBinding itemContainerGroupBinding = ItemContainerGroupBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,false
        );
        return new GroupsAdapter.UserViewHolder(itemContainerGroupBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupsAdapter.UserViewHolder holder, int position) {
        holder.setGroupData(groups.get(position));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        ItemContainerGroupBinding binding;

        UserViewHolder(ItemContainerGroupBinding itemContainerGroupBinding){
            super(itemContainerGroupBinding.getRoot());
            binding=itemContainerGroupBinding;
        }

        void setGroupData(Group group){
            binding.textName.setText(group.name );
            binding.imageAdmin.setImageBitmap(getUserImage(String.valueOf(group.image)));
            binding.imageLeader.setImageBitmap(getUserImage(String.valueOf(group.second_image)));
            if(!group.lastMessage.isEmpty()){
                if(group.lastMessage.equals("IMAGE")){
                        binding.textRecentMessage.setText("Bạn đã gửi 1 ảnh");

                }else{
                    binding.textRecentMessage.setText(group.lastMessage);
                }
                binding.textRecentMessage.setText(group.lastMessage);
            }
            binding.day.setText(DateFormat.getDateInstance().format(group.lastDateMessage));
            binding.getRoot().setOnClickListener(v->groupListener.onGroupClicked(group));
        }

    }

    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

}
