package dev.sultan;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Apps {
    /** Installed apps that have a launcher entry, as {package, label}, sorted by label. Excludes this app. */
    static List<String[]> list(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, String> found = new LinkedHashMap<>();
        String[] cats = {Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER};
        for (String cat : cats) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
                String p = r.activityInfo.packageName;
                if (p.equals(c.getPackageName())) continue;
                found.put(p, r.loadLabel(pm).toString());
            }
        }
        List<String[]> out = new ArrayList<>();
        for (Map.Entry<String, String> e : found.entrySet()) out.add(new String[]{e.getKey(), e.getValue()});
        Collections.sort(out, (a, b) -> a[1].compareToIgnoreCase(b[1]));
        return out;
    }
}
