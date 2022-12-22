package com.example.chat_app.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.example.chat_app.adapters.ChatAdapter;
import com.example.chat_app.databinding.ActivityChatBinding;
import com.example.chat_app.models.ChatMessage;
import com.example.chat_app.models.User;
import com.example.chat_app.utilities.Contants;
import com.example.chat_app.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversationId = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessage();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Contants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void senMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Contants.KEY_SENDER_ID, preferenceManager.getString(Contants.KEY_USER_ID));
        message.put(Contants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Contants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Contants.KEY_TIMESTAMP, new Date());
        database.collection(Contants.KEY_COLLECTION_CHAT).add(message);
        if (conversationId != null){
            updateConversion(binding.inputMessage.getText().toString());
        }else{
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Contants.KEY_SENDER_ID,preferenceManager.getString(Contants.KEY_USER_ID));
            conversion.put(Contants.KEY_SENDER_NAME,preferenceManager.getString(Contants.KEY_NAME));
            conversion.put(Contants.KEY_SENDER_IMAGE,preferenceManager.getString(Contants.KEY_IMAGE));
            conversion.put(Contants.KEY_RECEIVER_ID,receiverUser.id);
            conversion.put(Contants.KEY_RECEIVER_NAME,receiverUser.name);
            conversion.put(Contants.KEY_RECEIVER_IMAGE,receiverUser.image);
            conversion.put(Contants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversion.put(Contants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        binding.inputMessage.setText(null);
    }

    private void listenMessage() {
        database.collection(Contants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Contants.KEY_SENDER_ID, preferenceManager.getString(Contants.KEY_USER_ID))
                .whereEqualTo(Contants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Contants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Contants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Contants.KEY_RECEIVER_ID, preferenceManager.getString(Contants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Contants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Contants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Contants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Contants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversationId == null){
            checkForConversation();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Contants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> senMessage());
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);

    }

    private void addConversion(HashMap<String,Object> conversion){
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void updateConversion(String message){
        DocumentReference documentReference =
                database.collection(Contants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Contants.KEY_LAST_MESSAGE, message,
                Contants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversation(){
        if (chatMessages.size() != 0){
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

    private void checkForConversionRemotely(String senderId, String receiverId){
        database.collection(Contants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Contants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Contants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();

        }
    };
}