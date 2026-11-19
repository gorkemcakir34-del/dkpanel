package tr.net.gorkemcakir.sahadash;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class PushService extends FirebaseMessagingService {
    static final String CHANNEL = "mesajlar";

    @Override public void onNewToken(String token) {
        getSharedPreferences("push", MODE_PRIVATE).edit().putString("token", token).apply();
    }

    @Override public void onMessageReceived(RemoteMessage m) {
        Map<String, String> d = m.getData();
        String baslik = d.get("baslik");
        String metin = d.get("metin");
        String url = d.get("url");
        if (baslik == null || baslik.isEmpty()) baslik = "Saha Operasyon";
        if (metin == null) metin = "";

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Mesajlar", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (url != null) i.putExtra("url", url);
        int id = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pi = PendingIntent.getActivity(this, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(baslik)
            .setContentText(metin)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(metin))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi);
        try { nm.notify(id, b.build()); } catch (SecurityException e) { /* bildirim izni verilmemiş */ }
    }
}
