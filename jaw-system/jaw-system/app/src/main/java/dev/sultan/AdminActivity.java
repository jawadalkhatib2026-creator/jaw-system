package dev.sultan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import org.json.JSONArray;

public class AdminActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Accounts.adminOk) { finish(); return; }
        render();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        Accounts.adminOk = false;
    }

    private String[] emails() {
        JSONArray a = Accounts.all(this);
        String[] out = new String[a.length()];
        for (int i = 0; i < out.length; i++) out[i] = a.optJSONObject(i).optString("email");
        return out;
    }

    private void confirm(Runnable action) {
        new AlertDialog.Builder(this).setMessage(R.string.admin_sure)
            .setPositiveButton(R.string.ok, (d, w) -> {
                action.run();
                Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show();
                render();
            })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void render() {
        LinearLayout[] pg = new LinearLayout[1];
        ScrollView sv = Ui.screen(this, getString(R.string.admin_panel), pg);
        LinearLayout page = pg[0];
        View first = Ui.row(this, page, getString(R.string.admin_accounts, Accounts.count(this)), null, v -> {
            String[] e = emails();
            new AlertDialog.Builder(this).setTitle(R.string.jaw_accounts)
                .setMessage(e.length == 0 ? getString(R.string.no_accounts) : android.text.TextUtils.join("\n", e))
                .setPositiveButton(R.string.close, null).show();
        });
        Ui.row(this, page, getString(R.string.admin_delete_one), null, v -> {
            String[] e = emails();
            if (e.length == 0) { Toast.makeText(this, R.string.no_accounts, Toast.LENGTH_SHORT).show(); return; }
            new AlertDialog.Builder(this).setTitle(R.string.admin_delete_one)
                .setItems(e, (d, which) -> { Accounts.delete(this, e[which]); render(); })
                .setNegativeButton(R.string.cancel, null).show();
        });
        Ui.row(this, page, getString(R.string.admin_delete_all), null, v -> confirm(() -> Accounts.clear(this)));
        String mode = Store.accountMode(this);
        Ui.row(this, page, getString(R.string.account_type), getString(Store.modeLabel(mode)), v -> {
            Store.setAccountMode(this, Store.nextMode(mode));
            render();
        });
        Ui.row(this, page, getString(R.string.admin_reset_home), null, v -> confirm(() -> Store.resetApps(this)));
        Ui.row(this, page, getString(R.string.admin_reset_wall), null, v -> confirm(() -> Store.wallpaperFile(this).delete()));
        Ui.row(this, page, getString(R.string.admin_clear_channels), null, v -> confirm(() -> {
            JSONArray a = Store.channels(this);
            for (int i = 0; i < a.length(); i++) Store.removeChannel(this, a.optJSONObject(i).optString("id"));
        }));
        Ui.row(this, page, getString(R.string.admin_wipe), null, v -> confirm(() -> {
            JSONArray a = Store.channels(this);
            for (int i = 0; i < a.length(); i++) Store.removeChannel(this, a.optJSONObject(i).optString("id"));
            Store.wallpaperFile(this).delete();
            Accounts.clear(this);
            Store.sp(this).edit().clear().apply();
        }));
        setContentView(sv);
        first.requestFocus();
    }
}
