package dev.sultan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.function.BiConsumer;

public class AccountsActivity extends Activity {
    private LocalServer server;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }

    private void render() {
        LinearLayout[] pg = new LinearLayout[1];
        ScrollView sv = Ui.screen(this, getString(R.string.jaw_accounts), pg);
        LinearLayout page = pg[0];
        String user = Store.user(this);
        android.widget.TextView status = Ui.text(this,
            user == null ? getString(R.string.not_signed_in) : getString(R.string.signed_in_as, user), 18, 0xCCFFFFFF);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.bottomMargin = Ui.dp(this, 20);
        page.addView(status, sp);

        final String mode = Store.accountMode(this);
        View first = null;
        if (!"google".equals(mode)) {
            if (user == null) first = Ui.row(this, page, getString(R.string.signin), null, v -> signIn());
            else first = Ui.row(this, page, getString(R.string.signout), null, v -> { Store.setUser(this, null); render(); });
            Ui.row(this, page, getString(R.string.create_here), null, v -> createOnTv());
            Ui.row(this, page, getString(R.string.create_phone), null, v -> createFromPhone());
        }
        if (!"jaw".equals(mode)) {
            View g = Ui.row(this, page, getString(R.string.add_google), null, v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT)
                        .putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"}));
                } catch (Exception e) {
                    Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
                }
            });
            if (first == null) first = g;
            Ui.row(this, page, getString(R.string.open_play), null, v -> {
                Intent li = Store.launch(getPackageManager(), "com.android.vending");
                if (li != null) startActivity(li);
                else Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
            });
        }
        View last = Ui.row(this, page, getString(R.string.account_type), getString(Store.modeLabel(mode)), v -> {
            Store.setAccountMode(this, Store.nextMode(mode));
            render();
        });
        if (first == null) first = last;
        setContentView(sv);
        first.requestFocus();
    }

    private void credentials(int titleRes, BiConsumer<String, String> done) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 24), Ui.dp(this, 16), Ui.dp(this, 24), 0);
        EditText e = new EditText(this);
        e.setHint(R.string.email);
        e.setSingleLine();
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText p = new EditText(this);
        p.setHint(R.string.password);
        p.setSingleLine();
        p.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(e);
        box.addView(p);
        new AlertDialog.Builder(this).setTitle(titleRes).setView(box)
            .setPositiveButton(R.string.ok, (d, w) -> done.accept(e.getText().toString(), p.getText().toString()))
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void signIn() {
        credentials(R.string.signin, (email, pass) -> {
            if (Accounts.isAdmin(email, pass)) {
                Accounts.adminOk = true;
                startActivity(new Intent(this, AdminActivity.class));
            } else if (Accounts.verify(this, email, pass)) {
                Store.setUser(this, email.trim().toLowerCase(Locale.US));
                render();
            } else {
                Toast.makeText(this, R.string.acct_invalid, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createOnTv() {
        credentials(R.string.create_here, (email, pass) -> {
            String err = Accounts.create(this, email, pass);
            if (err != null) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
            Store.setUser(this, email.trim().toLowerCase(Locale.US));
            Toast.makeText(this, R.string.acct_created, Toast.LENGTH_SHORT).show();
            render();
        });
    }

    private void createFromPhone() {
        if (server != null) server.stop();
        server = new LocalServer(this, (route, type, f) -> {
            String body = LocalServer.readText(f);
            f.delete();
            String email = "", pass = "";
            try {
                for (String kv : body.split("&")) {
                    String[] a = kv.split("=", 2);
                    if (a.length < 2) continue;
                    if (a[0].equals("email")) email = URLDecoder.decode(a[1], "UTF-8");
                    if (a[0].equals("pass")) pass = URLDecoder.decode(a[1], "UTF-8");
                }
            } catch (Exception e) { return "Error / خطأ"; }
            String err = Accounts.create(this, email, pass);
            if (err == null) runOnUiThread(() -> {
                Toast.makeText(this, R.string.acct_created, Toast.LENGTH_SHORT).show();
                render();
            });
            return err == null ? "Account created ✓ / تم إنشاء الحساب" : err;
        });
        String url = server.start() ? server.url("account") : null;
        if (url == null) { Toast.makeText(this, R.string.conn_none, Toast.LENGTH_LONG).show(); server.stop(); return; }
        Qr.show(this, getString(R.string.create_phone), url, server);
    }
}
