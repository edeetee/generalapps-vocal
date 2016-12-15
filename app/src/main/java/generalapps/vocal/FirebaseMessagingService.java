package generalapps.vocal;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

/**
 * Created by edeetee on 23/09/2016.
 */

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("FirebaseMessagingServ", remoteMessage.getData().toString());
        Map<String, String> data =  remoteMessage.getData();
        Intent notifIntent = new Intent(MainActivity.context, MainActivity.class);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP);
        for (Map.Entry<String, String> entry : data.entrySet()) {
            notifIntent.putExtra(entry.getKey(), entry.getValue());
        }
        RemoteMessage.Notification remoteNotification = remoteMessage.getNotification();

        if(Boolean.valueOf(data.get("vocalNotification"))){
            NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(MainActivity.context);

            mNotificationManager.notify(0, new NotificationCompat.Builder(MainActivity.context)
                    .setContentTitle(remoteNotification.getTitle())
                    .setContentText(remoteNotification.getBody())
                    .setSound(remoteNotification.getSound() == null ? null : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(MainActivity.context, 0, notifIntent, FLAG_UPDATE_CURRENT)).build());
        }
    }
}
