package app.dmonly;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final long PAUSE_MS = 15 * 60 * 1000L;
    private static final long REFRESH_MS = 1500;

    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private TextView serviceStatus;
    private Switch switchEnabled;
    private Button btnPause;
    private TextView pauseStatus;
    private Switch switchStrict;
    private Switch switchHomeOnBackout;
    private RadioGroup actionGroup;
    private EditText redirectUrl;
    private EditText blockPatterns;
    private EditText allowPatterns;
    private TextView inspector;

    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            refreshInspector();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = Prefs.get(this);

        serviceStatus = (TextView) findViewById(R.id.service_status);
        switchEnabled = (Switch) findViewById(R.id.switch_enabled);
        btnPause = (Button) findViewById(R.id.btn_pause);
        pauseStatus = (TextView) findViewById(R.id.pause_status);
        switchStrict = (Switch) findViewById(R.id.switch_strict);
        switchHomeOnBackout = (Switch) findViewById(R.id.switch_home_on_backout);
        actionGroup = (RadioGroup) findViewById(R.id.action_group);
        redirectUrl = (EditText) findViewById(R.id.redirect_url);
        blockPatterns = (EditText) findViewById(R.id.block_patterns);
        allowPatterns = (EditText) findViewById(R.id.allow_patterns);
        inspector = (TextView) findViewById(R.id.inspector);

        findViewById(R.id.btn_open_accessibility).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });

        switchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                prefs.edit().putBoolean(Prefs.KEY_ENABLED, checked).apply();
            }
        });
        switchStrict.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                prefs.edit().putBoolean(Prefs.KEY_STRICT, checked).apply();
            }
        });
        switchHomeOnBackout.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                prefs.edit().putBoolean(Prefs.KEY_HOME_ON_BACKOUT, checked).apply();
            }
        });
        actionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String a = Prefs.ACTION_INBOX;
                if (checkedId == R.id.action_back) {
                    a = Prefs.ACTION_BACK;
                } else if (checkedId == R.id.action_home) {
                    a = Prefs.ACTION_HOME;
                }
                prefs.edit().putString(Prefs.KEY_ACTION, a).apply();
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Prefs.isPaused(prefs)) {
                    prefs.edit().putLong(Prefs.KEY_PAUSED_UNTIL, 0L).apply();
                    Toast.makeText(MainActivity.this, R.string.toast_resumed, Toast.LENGTH_SHORT).show();
                } else {
                    prefs.edit().putLong(Prefs.KEY_PAUSED_UNTIL, System.currentTimeMillis() + PAUSE_MS).apply();
                    Toast.makeText(MainActivity.this, R.string.toast_paused, Toast.LENGTH_SHORT).show();
                }
                refreshStatus();
            }
        });

        findViewById(R.id.btn_test_inbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUrl();
                if (!Prefs.openInbox(MainActivity.this, Prefs.redirectUrl(prefs))) {
                    Toast.makeText(MainActivity.this, R.string.toast_no_instagram, Toast.LENGTH_LONG).show();
                }
            }
        });

        findViewById(R.id.btn_save_rules).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUrl();
                prefs.edit()
                        .putString(Prefs.KEY_BLOCK, blockPatterns.getText().toString())
                        .putString(Prefs.KEY_ALLOW, allowPatterns.getText().toString())
                        .apply();
                Toast.makeText(MainActivity.this, R.string.toast_rules_saved, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_reset_rules).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit()
                        .remove(Prefs.KEY_BLOCK)
                        .remove(Prefs.KEY_ALLOW)
                        .remove(Prefs.KEY_REDIRECT_URL)
                        .apply();
                loadFields();
                Toast.makeText(MainActivity.this, R.string.toast_rules_saved, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshStatus();
                refreshInspector();
            }
        });
        findViewById(R.id.btn_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("DM Only inspector", inspector.getText()));
                    Toast.makeText(MainActivity.this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadFields();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshInspector();
        handler.removeCallbacks(refresher);
        handler.postDelayed(refresher, REFRESH_MS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresher);
        saveUrl();
        super.onPause();
    }

    private void saveUrl() {
        String url = redirectUrl.getText().toString().trim();
        if (url.isEmpty()) {
            prefs.edit().remove(Prefs.KEY_REDIRECT_URL).apply();
        } else if (!url.equals(Prefs.redirectUrl(prefs))) {
            prefs.edit().putString(Prefs.KEY_REDIRECT_URL, url).apply();
        }
    }

    private void loadFields() {
        switchEnabled.setChecked(Prefs.enabled(prefs));
        switchStrict.setChecked(Prefs.strict(prefs));
        switchHomeOnBackout.setChecked(Prefs.homeOnBackout(prefs));
        String a = Prefs.action(prefs);
        if (Prefs.ACTION_BACK.equals(a)) {
            actionGroup.check(R.id.action_back);
        } else if (Prefs.ACTION_HOME.equals(a)) {
            actionGroup.check(R.id.action_home);
        } else {
            actionGroup.check(R.id.action_inbox);
        }
        redirectUrl.setText(Prefs.redirectUrl(prefs));
        blockPatterns.setText(Prefs.blockPatterns(prefs));
        allowPatterns.setText(Prefs.allowPatterns(prefs));
    }

    private void refreshStatus() {
        boolean on = Prefs.isServiceEnabled(this) || GuardService.isRunning();
        serviceStatus.setText(on ? R.string.status_service_on : R.string.status_service_off);
        if (Prefs.isPaused(prefs)) {
            pauseStatus.setText(getString(R.string.pause_status_active,
                    timeFormat.format(new Date(Prefs.pausedUntil(prefs)))));
            btnPause.setText(R.string.btn_resume);
        } else {
            pauseStatus.setText(R.string.pause_status_none);
            btnPause.setText(R.string.btn_pause);
        }
    }

    private void refreshInspector() {
        Decision last = GuardService.lastDecision();
        if (last == null) {
            inspector.setText(R.string.inspector_empty);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Last screen at ").append(timeFormat.format(new Date(last.time))).append(": ")
                .append(last.blocked ? "BLOCKED" : "ALLOWED").append("\n");
        sb.append("  reason: ").append(last.reason).append("\n");
        sb.append("  action: ").append(last.action).append("\n");
        Snapshot s = last.snapshot;
        sb.append("  nodes scanned: ").append(s.nodeCount).append(s.truncated ? " (limit reached)" : "").append("\n\n");
        sb.append("Visible view ids (").append(s.ids.size()).append("):\n");
        for (String id : s.ids) {
            sb.append("  ").append(id).append("\n");
        }
        sb.append("\nContent descriptions (").append(s.descs.size()).append("):\n");
        for (String d : s.descs) {
            sb.append("  \"").append(d).append("\"\n");
        }
        List<Decision> history = GuardService.history();
        sb.append("\nRecent decisions:\n");
        for (Decision d : history) {
            sb.append("  ").append(timeFormat.format(new Date(d.time))).append("  ")
                    .append(d.blocked ? "BLOCKED  " : "allowed  ")
                    .append(d.reason).append(" -> ").append(d.action).append("\n");
        }
        inspector.setText(sb.toString());
    }
}
