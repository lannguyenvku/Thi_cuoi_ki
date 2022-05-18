package com.example.myapplication.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.activities.CreateGroupActivity;
import com.example.myapplication.databinding.ItemContainerUserGroupBinding;
import com.example.myapplication.models.User;
import com.example.myapplication.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class GroupUsersAdapter extends RecyclerView.Adapter<GroupUsersAdapter.UserViewHolder> implements Filterable {

    private List<User> users;
    private List<User> oldUsers;
    public static int userCount =0;
    private PreferenceManager preferenceManager;
    public static List<User> userListAdded;
    public static GroupAddedUsersAdapter groupAddedUserAdapter;
    public static List<CheckBox> checkBoxList;


    public GroupUsersAdapter(List<User> users) {
        this.users = users;
        this.oldUsers=users;
    }


    @NonNull
    @Override
    public GroupUsersAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserGroupBinding itemContainerUserGroupBinding = ItemContainerUserGroupBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,false
        );
        return new GroupUsersAdapter.UserViewHolder(itemContainerUserGroupBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupUsersAdapter.UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }


    @Override
    public int getItemCount() {
        return users.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String strSearch=charSequence.toString();
               if(strSearch.isEmpty()){
                 users=oldUsers;
               }else{
                List<User> list= new ArrayList<>();
                for(User user: oldUsers){
                    if(user.name.toLowerCase().contains(strSearch.toLowerCase())){
                        list.add(user);
                    }
                }
                users=list;
               }
               FilterResults filterResults=new FilterResults();
               filterResults.values=users;
               return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
      users=(List<User>)filterResults.values;
      notifyDataSetChanged();
            }
        };
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
         ItemContainerUserGroupBinding binding;

         UserViewHolder(ItemContainerUserGroupBinding itemContainerUserGroupBinding){
             super(itemContainerUserGroupBinding.getRoot());
             binding=itemContainerUserGroupBinding;
             preferenceManager= new PreferenceManager(itemView.getContext());
             userListAdded=new ArrayList<User>();
             checkBoxList=new ArrayList<CheckBox>();
         }

         void setUserData(User user){
             binding.textName.setText(user.name);
             binding.textEmail.setText(user.email);
             binding.imageProfile.setImageBitmap(getUserImage(user.image));
             LinearLayoutManager layoutManager= new LinearLayoutManager(itemView.getContext());
             layoutManager.setOrientation(RecyclerView.HORIZONTAL);
             CreateGroupActivity.addedUsersList.setLayoutManager(layoutManager);

             LayoutAnimationController LeftToRightlayoutAnimationController=AnimationUtils.loadLayoutAnimation(itemView.getContext(), R.anim.layout_animation_left_to_right);

             itemView.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {

                     if(binding.createNew.isChecked()==false){
                         checkBoxList.add(binding.createNew);
                         binding.createNew.setChecked(true);
                         userListAdded.add(user);
                         CreateGroupActivity.addedUsersList.setVisibility(View.VISIBLE);
                         CreateGroupActivity.addedUsersList.setLayoutAnimation(LeftToRightlayoutAnimationController);
                         groupAddedUserAdapter=new GroupAddedUsersAdapter(userListAdded);
                         CreateGroupActivity.addedUsersList.setAdapter(groupAddedUserAdapter);
                     }else{
                         binding.createNew.setChecked(false);
                         checkBoxList.remove(binding.createNew);
                         userListAdded.remove(user);

                         groupAddedUserAdapter=new GroupAddedUsersAdapter(userListAdded);
                         CreateGroupActivity.addedUsersList.setAdapter(groupAddedUserAdapter);
                         if(userListAdded.size() <1){
                             CreateGroupActivity.addedUsersList.setVisibility(View.GONE);
                         }
                     }

                     if(userListAdded.size() >1){
                         CreateGroupActivity.createGroup.setTextColor(Color.WHITE);
                         CreateGroupActivity.createGroup.setEnabled(true);
                     }
                     else{
                         CreateGroupActivity.createGroup.setTextColor(Color.GRAY);
                         CreateGroupActivity.createGroup.setEnabled(false);
                     }

                 }
             });

         }
    }


    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

}
