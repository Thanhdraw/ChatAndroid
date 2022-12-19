package com.example.chat_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;

import com.example.chat_app.R;
import com.example.chat_app.databinding.ActivityChatBinding;
import com.example.chat_app.models.User;
import com.example.chat_app.utilities.Contants;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receiverUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
    }

    private void loadReceiverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(Contants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());

    }
}