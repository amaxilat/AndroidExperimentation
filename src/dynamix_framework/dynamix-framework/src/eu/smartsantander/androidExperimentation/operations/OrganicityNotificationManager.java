package eu.smartsantander.androidExperimentation.operations;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.Notification;

import org.ambientdynamix.core.HomeActivity;
import org.ambientdynamix.core.R;


/**
 * Created by mylonasg on 9/29/15.
 *
 * Deal uniformly with notifications - lame initial implementation to replace dynamix notifications
 */


public class OrganicityNotificationManager {

    private final NotificationManager notificationManager;
    private final Context context;

    public OrganicityNotificationManager(Context androidContext) {
        this.context = androidContext.getApplicationContext();
        notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    public void postOrganicityNotification(String title, String subject) {

        //Intent intent = new Intent(this, NotificationReceiver.class);
        Intent intent = new Intent(this.context, HomeActivity.class);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this.context, (int) System.currentTimeMillis(), intent, 0);


        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n  = new Notification.Builder(this.context)
                .setContentTitle(title)
                .setContentText(subject)
                .setSmallIcon(R.drawable.status_bar_icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();


        notificationManager.notify(0, n);
    }

    public void clearOrganicityNotifications() {

    }

}
