package __PKG__;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

public class BruceWidgetProvider extends AppWidgetProvider {
    static final String PREFS = "bruce_widget";
    static final String ACTION_DONE = "__PKG__.WIDGET_DONE";
    static final Object LOCK = new Object();

    static final int[] ROW = {R.id.r1, R.id.r2, R.id.r3, R.id.r4, R.id.r5};
    static final int[] NAME = {R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5};
    static final int[] SUB = {R.id.s1, R.id.s2, R.id.s3, R.id.s4, R.id.s5};
    static final int[] BTN = {R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) draw(c, m, id);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if (i != null && ACTION_DONE.equals(i.getAction())) {
            handleDone(c, i.getIntExtra("id", -1));
            return;
        }
        super.onReceive(c, i);
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, BruceWidgetProvider.class));
        for (int id : ids) draw(c, m, id);
    }

    static void handleDone(Context c, int itemId) {
        if (itemId < 0) return;
        boolean cancel = true;
        int owner = itemId;
        int ck = 99;
        synchronized (LOCK) {
            try {
                SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                JSONArray items = new JSONArray(p.getString("items", "[]"));
                JSONArray keep = new JSONArray();
                JSONObject target = null;
                for (int k = 0; k < items.length(); k++) {
                    JSONObject o = items.getJSONObject(k);
                    if (o.optInt("id", -1) == itemId) target = o;
                    else keep.put(o);
                }
                if (target != null) {
                    owner = target.optInt("own", itemId);
                    ck = target.optInt("ck", 99);
                    // Medicines sharing one grouped reminder: only cancel it once the last one is done.
                    for (int k = 0; k < keep.length(); k++) {
                        JSONObject o = keep.getJSONObject(k);
                        if (o.optInt("own", o.optInt("id", -1)) == owner) cancel = false;
                    }
                }
                JSONArray q = new JSONArray(p.getString("queue", "[]"));
                JSONObject e = new JSONObject();
                e.put("id", itemId);
                e.put("at", System.currentTimeMillis());
                q.put(e);
                p.edit().putString("items", keep.toString()).putString("queue", q.toString()).commit();
            } catch (Exception ignored) {}
        }
        if (cancel) cancelReminders(c, owner, ck);
        refresh(c);
    }

    // Best effort: stop pending overdue reminders for this item. If it fails, the app
    // clears them the next time it opens.
    static void cancelReminders(Context c, int itemId, int maxK) {
        try {
            Class<?> cls = Class.forName("com.capacitorjs.plugins.localnotifications.TimedNotificationPublisher");
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            int base = 2000 + itemId * 100;
            for (int k = 0; k <= maxK; k++) {
                int nid = k == 0 ? 1000 + itemId : base + k;
                Intent in = new Intent(c, cls);
                PendingIntent pi = PendingIntent.getBroadcast(c, nid, in,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) {
                    am.cancel(pi);
                    pi.cancel();
                }
                nm.cancel(nid);
            }
        } catch (Throwable ignored) {}
    }

    static void draw(Context c, AppWidgetManager m, int widgetId) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.bruce_widget);
        v.setTextViewText(R.id.w_title, p.getString("title", "Bruce"));

        JSONArray items;
        try {
            items = new JSONArray(p.getString("items", "[]"));
        } catch (Exception e) {
            items = new JSONArray();
        }

        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open != null) {
            PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.w_root, pi);
            v.setOnClickPendingIntent(R.id.w_head, pi);
            v.setOnClickPendingIntent(R.id.w_empty, pi);
        }

        long now = System.currentTimeMillis();
        int n = Math.min(items.length(), ROW.length);
        v.setViewVisibility(R.id.w_empty, n == 0 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < ROW.length; i++) {
            if (i >= n) {
                v.setViewVisibility(ROW[i], View.GONE);
                continue;
            }
            JSONObject o = items.optJSONObject(i);
            if (o == null) {
                v.setViewVisibility(ROW[i], View.GONE);
                continue;
            }
            long due = o.optLong("due", 0);
            String sub = o.optString("s", "");
            boolean od = o.optBoolean("od", false);
            if (due > 0) {
                od = now >= due;
                String t = DateFormat.getTimeFormat(c).format(new Date(due));
                String dz = o.optString("dz", "");
                sub = (od ? "Overdue, was due " : "Give by ") + t + (dz.isEmpty() ? "" : " · " + dz);
            }
            v.setViewVisibility(ROW[i], View.VISIBLE);
            v.setTextViewText(NAME[i], o.optString("n", ""));
            v.setTextViewText(SUB[i], sub);
            v.setViewVisibility(SUB[i], sub.isEmpty() ? View.GONE : View.VISIBLE);
            v.setTextColor(SUB[i], od ? Color.parseColor("#FF8F84") : Color.parseColor("#C4C4C9"));

            int id = o.optInt("id", -1);
            Intent d = new Intent(c, BruceWidgetProvider.class);
            d.setAction(ACTION_DONE);
            d.setData(Uri.parse("bruce://done/" + id));
            d.putExtra("id", id);
            PendingIntent dp = PendingIntent.getBroadcast(c, 100 + i, d,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(BTN[i], dp);
        }
        m.updateAppWidget(widgetId, v);
    }
}
