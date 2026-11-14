package dev.sultan;

import android.content.Context;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Locale;

/** Tiny web server on the TV so a phone on the same Wi-Fi can send a photo, a video or a new account. */
final class LocalServer {
    interface Callback {
        /** Runs on a background thread. Returns the message shown on the phone ("OK" if null). */
        String onUpload(String route, String type, File file);
    }

    private static final long MAX = 150L * 1024 * 1024;
    private static final String CSS =
        "body{font-family:-apple-system,Roboto,sans-serif;background:#0B1026;color:#fff;text-align:center;padding:32px}"
        + "label,button{display:block;margin:20px auto;padding:18px;border-radius:16px;background:#3B5BDB;color:#fff;font-size:18px;border:0;max-width:320px;width:100%;box-sizing:border-box}"
        + "input[type=email],input[type=password]{display:block;margin:12px auto;padding:14px;border-radius:12px;border:0;font-size:18px;max-width:320px;width:100%;box-sizing:border-box}"
        + "#m{margin-top:20px;opacity:.85}";

    final String token;
    int port;
    private final Context ctx;
    private final Callback cb;
    private ServerSocket ss;
    private volatile boolean running;

    LocalServer(Context c, Callback cb) {
        this.ctx = c.getApplicationContext();
        this.cb = cb;
        byte[] b = new byte[6];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format(Locale.US, "%02x", x));
        token = sb.toString();
    }

    boolean start() {
        for (int p = 8080; p < 8100; p++) {
            try { ss = new ServerSocket(p); port = p; break; } catch (IOException e) { ss = null; }
        }
        if (ss == null) return false;
        running = true;
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    final Socket s = ss.accept();
                    new Thread(() -> handle(s)).start();
                } catch (IOException e) { break; }
            }
        });
        t.setDaemon(true);
        t.start();
        return true;
    }

    void stop() {
        running = false;
        try { if (ss != null) ss.close(); } catch (IOException e) { }
    }

    static String ip() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception e) { }
        return null;
    }

    /** Address the phone should open, or null when the TV has no network address. */
    String url(String route) {
        String ip = ip();
        return ip == null ? null : "http://" + ip + ":" + port + "/" + token + "/" + route;
    }

    static String readText(File f) {
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
            if (sb.length() > 8192) break;
        }
        if (c == -1 && sb.length() == 0) return null;
        return sb.toString();
    }

    private void handle(Socket s) {
        try (Socket sock = s) {
            sock.setSoTimeout(15000);
            InputStream in = new BufferedInputStream(sock.getInputStream());
            OutputStream out = sock.getOutputStream();
            String line = readLine(in);
            if (line == null) return;
            String[] parts = line.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1];
            int q = path.indexOf('?');
            if (q >= 0) path = path.substring(0, q);
            long len = 0;
            String type = "";
            String h;
            while ((h = readLine(in)) != null && !h.isEmpty()) {
                String lh = h.toLowerCase(Locale.US);
                if (lh.startsWith("content-length:")) len = Long.parseLong(h.substring(15).trim());
                else if (lh.startsWith("x-type:")) type = h.substring(7).trim();
            }
            String prefix = "/" + token + "/";
            if (!path.startsWith(prefix)) { respond(out, 404, "text/plain", "Not found"); return; }
            String route = path.substring(prefix.length());
            if (!route.equals("wall") && !route.equals("share") && !route.equals("account")) {
                respond(out, 404, "text/plain", "Not found");
                return;
            }
            if (method.equals("GET")) {
                respond(out, 200, "text/html; charset=utf-8", page(route));
            } else if (method.equals("POST")) {
                if (len <= 0 || len > MAX) { respond(out, 413, "text/plain", "Too big / كبير جداً"); return; }
                File f = new File(ctx.getCacheDir(), "upload_" + route + ".bin");
                long left = len;
                try (FileOutputStream fo = new FileOutputStream(f)) {
                    byte[] buf = new byte[65536];
                    while (left > 0) {
                        int r = in.read(buf, 0, (int) Math.min(buf.length, left));
                        if (r < 0) break;
                        fo.write(buf, 0, r);
                        left -= r;
                    }
                }
                if (left > 0) { respond(out, 400, "text/plain", "Failed / فشل"); return; }
                String msg = cb.onUpload(route, type, f);
                respond(out, 200, "text/plain; charset=utf-8", msg == null ? "OK ✓" : msg);
            } else {
                respond(out, 405, "text/plain", "No");
            }
        } catch (Exception e) { /* connection dropped; nothing to do */ }
    }

    private static void respond(OutputStream out, int code, String mime, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + code + " X\r\nContent-Type: " + mime + "\r\nContent-Length: " + b.length
            + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));
        out.write(b);
        out.flush();
    }

    private String page(String route) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset=utf-8>")
          .append("<meta name=viewport content=\"width=device-width,initial-scale=1\"><title>Jaw System</title><style>")
          .append(CSS).append("</style></head><body><h2>Jaw System • جو سيستم</h2>");
        if (route.equals("account")) {
            sb.append("<p>Create your Jaw account<br>أنشئ حساب جو</p>")
              .append("<input id=e type=email placeholder=\"Email / البريد\">")
              .append("<input id=p type=password placeholder=\"Password 6+ / كلمة المرور\">")
              .append("<button id=b>Create / إنشاء</button><div id=m></div><script>")
              .append("var m=document.getElementById('m');")
              .append("document.getElementById('b').onclick=function(){")
              .append("var e=document.getElementById('e').value,p=document.getElementById('p').value;")
              .append("fetch(location.pathname,{method:'POST',body:'email='+encodeURIComponent(e)+'&pass='+encodeURIComponent(p)})")
              .append(".then(function(r){return r.text()}).then(function(t){m.textContent=t})")
              .append(".catch(function(){m.textContent='Failed / فشل'})};</script>");
        } else {
            String accept = route.equals("share") ? "image/*,video/*" : "image/*";
            String title = route.equals("share")
                ? "Send a photo or video to the TV<br>أرسل صورة أو فيديو إلى التلفاز"
                : "Choose a photo for the wallpaper<br>اختر صورة للخلفية";
            sb.append("<p>").append(title).append("</p>")
              .append("<label>Choose / اختر<input id=f type=file accept=\"").append(accept).append("\" hidden></label>")
              .append("<div id=m></div><script>")
              .append("var f=document.getElementById('f'),m=document.getElementById('m');")
              .append("f.onchange=function(){var x=f.files[0];if(!x)return;m.textContent='Sending... / جارٍ الإرسال';")
              .append("fetch(location.pathname,{method:'POST',headers:{'X-Type':x.type.indexOf('video')==0?'video':'image'},body:x})")
              .append(".then(function(r){return r.text()}).then(function(t){m.textContent=t})")
              .append(".catch(function(){m.textContent='Failed / فشل'})};</script>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
