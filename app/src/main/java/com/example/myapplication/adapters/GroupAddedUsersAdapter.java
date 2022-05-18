package com.example.myapplication.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.activities.CreateGroupActivity;
import com.example.myapplication.databinding.ItemContainerAddedUserGroupBinding;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class GroupAddedUsersAdapter extends RecyclerView.Adapter<GroupAddedUsersAdapter.UserViewHolder>{

    public static List<User> usersAddeds;

    public GroupAddedUsersAdapter(List<User> usersAddeds) {
        this.usersAddeds=usersAddeds;
    }

    @NonNull
    @Override
    public GroupAddedUsersAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerAddedUserGroupBinding itemContainerUserGroupBinding = ItemContainerAddedUserGroupBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,false
        );
        return new GroupAddedUsersAdapter.UserViewHolder(itemContainerUserGroupBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupAddedUsersAdapter.UserViewHolder holder, int position) {
        holder.setUserData(usersAddeds.get(position));
    }

    @Override
    public int getItemCount() {
        return usersAddeds.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {

        ItemContainerAddedUserGroupBinding binding;

        public UserViewHolder(@NonNull ItemContainerAddedUserGroupBinding itemContainerUserGroupBinding) {
            super(itemContainerUserGroupBinding.getRoot());
            binding=itemContainerUserGroupBinding;
        }

        void setUserData(User userAdded){
            binding.imageProfile.setImageBitmap(getUserImage(userAdded.image));
            binding.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    for(int i=0;i<GroupUsersAdapter.checkBoxList.size();i++){
                        if(userAdded.id.equals(GroupUsersAdapter.userListAdded.get(i).id)){
                            GroupUsersAdapter.checkBoxList.get(i).setChecked(false);
                            GroupUsersAdapter.checkBoxList.remove(i);
                        }
                    }

                    removeItem(getAdapterPosition());

                    if(usersAddeds.size() <1){
                        CreateGroupActivity.addedUsersList.setVisibility(View.GONE);
                    }else if(usersAddeds.size() <2){
                        CreateGroupActivity.createGroup.setTextColor(Color.GRAY);
                        CreateGroupActivity.createGroup.setEnabled(false);
                    }else{
                        CreateGroupActivity.createGroup.setTextColor(Color.WHITE);
                        CreateGroupActivity.createGroup.setEnabled(true);
                    }

                }
            });
        }

    }

    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    private void removeItem(int i){
        usersAddeds.remove(i);
        notifyDataSetChanged();

    }
}
