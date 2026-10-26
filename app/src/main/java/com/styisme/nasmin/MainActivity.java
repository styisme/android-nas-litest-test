package com.styisme.nasmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView statusView, ipView;
    private Button toggleBtn;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        AuthManager.init(this);
        statusView = findViewById(R.id.statusView);
        ipView = findViewById(R.id.ipView);
        toggleBtn = findViewById(R.id.toggleBtn);
        toggleBtn.setOnClickListener(v -> {
            if (HttpServerService.running) {
                stopService(new Intent(this, HttpServerService.class));
                new Handler().postDelayed(this::update, 500);
            } else {
                startService(new Intent(this, HttpServerService.class));
                new Handler().postDelayed(this::update, 500);
            }
        });
        if (!HttpServerService.running) startService(new Intent(this, HttpServerService.class));
    }

    @Override protected void onResume() { super.onResume(); update(); }

    private void update() {
        boolean r = HttpServerService.running;
        statusView.setText(r ? "服务状态：运行中" : "服务状态：已停止");
        toggleBtn.setText(r ? "停止服务" : "启动服务");
        String s = "访问地址：http://" + getIp() + ":8080";
        if (!HttpServerService.lastError.isEmpty()) s += "\n⚠️ " + HttpServerService.lastError;
        if (DatabaseManager.isDefaultPassword()) s += "\n⚠️ 正在使用默认密码 admin，请尽快修改";
        ipView.setText(s);
    }

    private String getIp() {
        try {
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip == 0) return "127.0.0.1";
            return String.format(Locale.US, "%d.%d.%d.%d",
                    (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        } catch (Throwable t) { return "127.0.0.1"; }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "修改管理员密码");
        menu.add(0, 2, 0, "关于");
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) { showChangePwd(); return true; }
        if (item.getItemId() == 2) {
            new AlertDialog.Builder(this).setTitle("关于 NAS Min")
                    .setMessage("版本: 1.0.0\n\n极限精简版，仅保留文件服务主线，仅监听 8080。")
                    .setPositiveButton("确定", null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChangePwd() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(48, 24, 48, 24);
        EditText et = new EditText(this);
        et.setHint("新密码");
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        l.addView(et);
        new AlertDialog.Builder(this).setTitle("修改管理员密码").setView(l)
                .setPositiveButton("保存", (d, w) -> {
                    String p = et.getText().toString();
                    if (p.length() < 4) {
                        Toast.makeText(this, "密码至少 4 位", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DatabaseManager.getInstance().changePassword("admin", p);
                    Toast.makeText(this, "已修改", Toast.LENGTH_SHORT).show();
                    update();
                }).setNegativeButton("取消", null).show();
    }
}
