package com.example.chat_app.firebase;


//import android.util.Log;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.chat_app.R;
import com.example.chat_app.activities.ChatActivity;
import com.example.chat_app.models.User;
import com.example.chat_app.utilities.Contants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
//        Log.d("FCM", "Token: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
//        Log.d("FCM","Message: " + message.getNotification().getBody());
        User user = new User();
        user.id = message.getData().get(Contants.KEY_USER_ID);
        user.name = message.getData().get(Contants.KEY_NAME);
        user.token =message.getData().get(Contants.KEY_FCM_TOKEN);
        int notificationId = new Random().nextInt();
        String channelId = "chat_message";
        Intent intent = new Intent(this, ChatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Contants.KEY_USER, user);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, intent,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,channelId);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(user.name);
        builder.setContentText(message.getData().get(Contants.KEY_MESSAGE));
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(
                message.getData().get(Contants.KEY_MESSAGE)
        ));
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence channelName = "chat Message";
            String channelDescription = "this notification channel is used for chat message";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId,channelName,importance);
            channel.setDescription(channelDescription);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(notificationId,builder.build());

    }
}
