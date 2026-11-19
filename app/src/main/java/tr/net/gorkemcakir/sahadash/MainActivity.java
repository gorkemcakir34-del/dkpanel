package tr.net.gorkemcakir.sahadash;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import com.google.firebase.messaging.FirebaseMessaging;
import android.webkit.*;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private WebView web;
    private ValueCallback<Uri[]> fileCb;
    private final String base = BuildConfig.BASE_URL;

    private final ActivityResultLauncher<Intent> picker = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), r -> {
            if (fileCb == null) return;
            Uri[] res = WebChromeClient.FileChooserParams.parseResult(r.getResultCode(), r.getData());
            fileCb.onReceiveValue(res);
            fileCb = null;
        });

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(0xFF000000);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setBuiltInZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.addJavascriptInterface(new Bridge(), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                String sch = u.getScheme() == null ? "" : u.getScheme();
                if (sch.equals("blob") || sch.equals("file")) return false;
                if (u.toString().startsWith(base) ) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) {}
                return true;
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) v.loadUrl("file:///android_asset/offline.html");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try { picker.launch(p.createIntent()); }
                catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });
        web.setDownloadListener((url, ua, cd, mime, len) -> {
            String name = URLUtil.guessFileName(url, cd, mime);
            if (url.startsWith("blob:")) {
                String js = "(async()=>{try{const r=await fetch('" + url + "');const b=await r.blob();"
                    + "const f=new FileReader();f.onloadend=function(){Android.saveBase64(f.result.split(',')[1],"
                    + "'" + name.replace("'", "") + "','" + (mime == null ? "application/octet-stream" : mime) + "');};"
                    + "f.readAsDataURL(b);}catch(e){Android.toast('İndirme başarısız');}})();";
                web.evaluateJavascript(js, null);
                return;
            }
            try {
                DownloadManager.Request q = new DownloadManager.Request(Uri.parse(url));
                q.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
                q.addRequestHeader("User-Agent", ua);
                q.setMimeType(mime);
                q.setTitle(name);
                q.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                q.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(q);
                Toast.makeText(this, "İndiriliyor: " + name, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "İndirme başarısız", Toast.LENGTH_SHORT).show();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (web.canGoBack()) web.goBack(); else finish();
            }
        });

        // Push: bildirim izni (Android 13+) ve FCM jetonu
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        try {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(t ->
                getSharedPreferences("push", MODE_PRIVATE).edit().putString("token", t).apply());
        } catch (Exception e) { }

        String hedef = hedefUrl(getIntent());
        if (b != null) web.restoreState(b); else web.loadUrl(hedef != null ? hedef : base);
    }

    /** Bildirime tıklanınca gelen yolu (mesajlar.php?id=..) tam adrese çevirir. */
    private String hedefUrl(Intent in) {
        if (in == null) return null;
        String u = in.getStringExtra("url");
        if (u == null || u.isEmpty() || u.contains("://") || u.startsWith("/")) return null;
        in.removeExtra("url");
        return base + u;
    }

    @Override protected void onNewIntent(Intent in) {
        super.onNewIntent(in);
        setIntent(in);
        String h = hedefUrl(in);
        if (h != null) web.loadUrl(h);
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }
    @Override protected void onPause() { super.onPause(); CookieManager.getInstance().flush(); }

    class Bridge {
        @JavascriptInterface public String fcmToken() { return getSharedPreferences("push", MODE_PRIVATE).getString("token", ""); }
        @JavascriptInterface public void retry() { runOnUiThread(() -> web.loadUrl(base)); }
        @JavascriptInterface public void toast(String m) { runOnUiThread(() -> Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show()); }
        @JavascriptInterface public void saveBase64(String data, String name, String mime) {
            try {
                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, mime);
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                ContentResolver cr = getContentResolver();
                Uri u = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                try (OutputStream os = cr.openOutputStream(u)) { os.write(bytes); }
                toast("İndirilenler klasörüne kaydedildi: " + name);
            } catch (Exception e) { toast("Kaydedilemedi"); }
        }
    }
}
