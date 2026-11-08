package __PKG__;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "BruceWidget")
public class BruceWidgetPlugin extends Plugin {

    // Called by the app whenever the list changes.
    @PluginMethod
    public void update(PluginCall call) {
        synchronized (BruceWidgetProvider.LOCK) {
            SharedPreferences p = getContext().getSharedPreferences(BruceWidgetProvider.PREFS, Context.MODE_PRIVATE);
            JSArray items = call.getArray("items");
            JSONArray keep = new JSONArray();
            try {
                // Hide anything already ticked off on the widget but not yet read by the app.
                Set<Integer> pending = new HashSet<>();
                JSONArray q = new JSONArray(p.getString("queue", "[]"));
                for (int i = 0; i < q.length(); i++) pending.add(q.getJSONObject(i).optInt("id", -1));
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject o = items.getJSONObject(i);
                        if (!pending.contains(o.optInt("id", -1))) keep.put(o);
                    }
                }
            } catch (Exception ignored) {}
            p.edit()
                .putString("title", call.getString("title", "Bruce"))
                .putString("items", keep.toString())
                .commit();
        }
        BruceWidgetProvider.refresh(getContext());
        call.resolve();
    }

    // Called by the app on open: returns the Done taps made on the widget and clears them.
    @PluginMethod
    public void drain(PluginCall call) {
        String q;
        synchronized (BruceWidgetProvider.LOCK) {
            SharedPreferences p = getContext().getSharedPreferences(BruceWidgetProvider.PREFS, Context.MODE_PRIVATE);
            q = p.getString("queue", "[]");
            p.edit().putString("queue", "[]").commit();
        }
        JSObject r = new JSObject();
        try {
            r.put("queue", new JSArray(q));
        } catch (Exception e) {
            r.put("queue", new JSArray());
        }
        call.resolve(r);
    }
}
