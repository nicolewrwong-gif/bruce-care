package __PKG__;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

// Self-update from the "latest" GitHub release (the workflow publishes bruce-care.apk and latest.json).
// Android always asks the person to confirm the install, and only accepts an APK signed with the
// same key as the installed app, so a wrong file can't replace the app.
@CapacitorPlugin(name = "BruceUpdate")
public class BruceUpdatePlugin extends Plugin {
    private static final String BASE = "https://github.com/__REPO__/releases/download/latest/";

    private HttpURLConnection open(String name) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + name).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setUseCaches(false);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("Cache-Control", "no-cache");
        c.setRequestProperty("User-Agent", "BruceCare");
        return c;
    }

    @SuppressWarnings("deprecation")
    private long installedCode() throws Exception {
        PackageInfo pi = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
        return Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
    }

    // Resolves { available, current, latest, versionName }.
    @PluginMethod
    public void check(final PluginCall call) {
        new Thread(() -> {
            try {
                HttpURLConnection c = open("latest.json");
                int code = c.getResponseCode();
                if (code != 200) {
                    call.reject("HTTP " + code);
                    return;
                }
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                try (InputStream in = c.getInputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                }
                JSONObject j = new JSONObject(bo.toString("UTF-8"));
                long latest = j.optLong("versionCode", 0);
                long cur = installedCode();
                JSObject r = new JSObject();
                r.put("available", latest > cur);
                r.put("current", cur);
                r.put("latest", latest);
                r.put("versionName", j.optString("versionName", ""));
                call.resolve(r);
            } catch (Exception e) {
                call.reject(String.valueOf(e.getMessage()));
            }
        }).start();
    }

    // Downloads the latest APK and opens Android's installer.
    @PluginMethod
    public void install(final PluginCall call) {
        if (Build.VERSION.SDK_INT >= 26 && !getContext().getPackageManager().canRequestPackageInstalls()) {
            Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getContext().getPackageName()));
            s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(s);
            call.reject("Allow installs from this app in the settings that just opened, then tap Update now again.");
            return;
        }
        new Thread(() -> {
            try {
                File dir = new File(getContext().getCacheDir(), "update");
                dir.mkdirs();
                File tmp = new File(dir, "bruce-care.tmp");
                File apk = new File(dir, "bruce-care.apk");
                tmp.delete();
                apk.delete();
                HttpURLConnection c = open("bruce-care.apk");
                int code = c.getResponseCode();
                if (code != 200) {
                    call.reject("Download failed (HTTP " + code + ")");
                    return;
                }
                try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                if (tmp.length() < 1000000 || !tmp.renameTo(apk)) {
                    tmp.delete();
                    call.reject("Download was incomplete. Try again.");
                    return;
                }
                PackageInstaller pi = getContext().getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams sp =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                if (Build.VERSION.SDK_INT >= 31)
                    sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                int sid = pi.createSession(sp);
                try (PackageInstaller.Session s = pi.openSession(sid)) {
                    try (InputStream in = new java.io.FileInputStream(apk);
                         OutputStream out = s.openWrite("bruce-care.apk", 0, apk.length())) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        s.fsync(out);
                    }
                    Intent cb = new Intent(getContext(), BruceInstallReceiver.class);
                    PendingIntent pend = PendingIntent.getBroadcast(getContext(), sid, cb,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0));
                    s.commit(pend.getIntentSender());
                }
                call.resolve();
            } catch (Exception e) {
                call.reject("Couldn't download the update: " + e.getMessage());
            }
        }).start();
    }
}
