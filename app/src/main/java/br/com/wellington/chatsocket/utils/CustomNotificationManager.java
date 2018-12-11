package br.com.wellington.chatsocket.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;


import br.com.wellington.chatsocket.R;
import br.com.wellington.chatsocket.activity.MainActivity;
import br.com.wellington.chatsocket.service.WebSocketService;

/**
 * Classe de Gerenciamento de Notificação
 *
 * @author Wellington
 * @version 1.0 - 11/01/2017.
 */
public class CustomNotificationManager {

    private static long[] patternVibrate = new long[]{150, 300, 150, 600};

    public static void showNotification(Context context, String title, String content, String ticker) {
        NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String CHANNEL_ID = "my_channel_01";// The id of the channel.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            if (mNM != null) {
                mNM.createNotificationChannel(mChannel);
            }
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID);
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("newMessages", WebSocketService.messagesReceivedInactive);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setTicker(ticker);
        notification.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        notification.setContentTitle(title);
        notification.setStyle(new NotificationCompat.BigTextStyle().bigText(content));
        notification.setContentText(ticker);
        notification.setSmallIcon(R.drawable.ic_stat_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification.setPriority(Notification.PRIORITY_HIGH);
        }
        notification.setAutoCancel(true);
        notification.setContentIntent(contentIntent);
        notification.setLights(0xff00ff00, 1000, 700);
        notification.setVibrate(patternVibrate);
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification.setSound(uri);

        Notification n = notification.build();
        n.vibrate = patternVibrate;
        n.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL;

        mNM.notify(R.string.app_name, n);

    }

}
