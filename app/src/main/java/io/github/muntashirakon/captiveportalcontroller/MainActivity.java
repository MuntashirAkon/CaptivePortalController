package io.github.muntashirakon.captiveportalcontroller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    private Switch enableSwitch;
    private Spinner cpMode;
    private AutoCompleteTextView captivePortalHttpsUrl;
    private AutoCompleteTextView captivePortalHttpUrl;
    private AutoCompleteTextView captivePortalFallbackUrl;
    private AutoCompleteTextView captivePortalOtherFallbackUrls;

    private final String userAgent = System.getProperty("http.agent");

    private final BroadcastReceiver cpControllerWatcher = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Utils.ACTION_CP_MODE_CHANGED.equals(action)) {
                if (cpMode != null) {
                    int newMode = ConnectivityManager.getOurCaptivePortalMode(MainActivity.this);
                    if (cpMode.getSelectedItemPosition() != newMode) {
                        cpMode.setSelection(newMode);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectivityManager.initPrefsIfNotAlready(this);
        ConnectivityManager.checkCaptivePortalMode(this);
        enableSwitch = findViewById(android.R.id.toggle);
        enableSwitch.setChecked(ConnectivityManager.controllerEnabled(this));
        enableSwitch.setOnCheckedChangeListener(this);
        cpMode = findViewById(R.id.spinner);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cpMode.setAdapter(ArrayAdapter.createFromResource(this, R.array.cp_modes, android.R.layout.simple_list_item_1));
        } else {
            cpMode.setAdapter(ArrayAdapter.createFromResource(this, R.array.cp_modes_pre26, android.R.layout.simple_list_item_1));
        }
        cpMode.setSelection(ConnectivityManager.getOurCaptivePortalMode(this));
        cpMode.setOnItemSelectedListener(this);
        TextView summary = findViewById(android.R.id.summary);
        summary.setText(generateSummary());
        // Inputs
        captivePortalHttpsUrl = findViewById(R.id.captive_portal_https_url);
        captivePortalHttpUrl = findViewById(R.id.captive_portal_http_url);
        captivePortalFallbackUrl = findViewById(R.id.captive_portal_fallback_url);
        captivePortalOtherFallbackUrls = findViewById(R.id.captive_portal_other_fallback_urls);
        // Compatibility
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            captivePortalHttpsUrl.setVisibility(View.GONE);
            captivePortalFallbackUrl.setVisibility(View.GONE);
            captivePortalOtherFallbackUrls.setVisibility(View.GONE);
        }
        // Set suggestions
        captivePortalHttpsUrl.setAdapter(ArrayAdapter.createFromResource(this, R.array.captive_portal_https_url_hints, android.R.layout.simple_list_item_1));
        captivePortalHttpUrl.setAdapter(ArrayAdapter.createFromResource(this, R.array.captive_portal_http_url_hints, android.R.layout.simple_list_item_1));
        captivePortalFallbackUrl.setAdapter(ArrayAdapter.createFromResource(this, R.array.captive_portal_fallback_url_hints, android.R.layout.simple_list_item_1));
        captivePortalOtherFallbackUrls.setAdapter(ArrayAdapter.createFromResource(this, R.array.captive_portal_other_fallback_urls_hints, android.R.layout.simple_list_item_1));
        // Save servers
        Button saveServersButton = findViewById(android.R.id.button1);
        saveServersButton.setOnClickListener(v -> {
            Editable https = captivePortalHttpsUrl.getText();
            Editable http = captivePortalHttpUrl.getText();
            Editable fallback = captivePortalFallbackUrl.getText();
            Editable otherFallback = captivePortalOtherFallbackUrls.getText();
            ConnectivityManager.setCaptivePortalServers(this,
                    TextUtils.isEmpty(https) ? null : https.toString(),
                    TextUtils.isEmpty(http) ? null : http.toString(),
                    TextUtils.isEmpty(fallback) ? null : fallback.toString(),
                    TextUtils.isEmpty(otherFallback) ? null : otherFallback.toString());
            // Regenerate summary
            summary.setText(generateSummary());
        });
        // User agent
        LinearLayout uaLayout = findViewById(R.id.user_agent_layout);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            uaLayout.setVisibility(View.GONE);
        }
        EditText uaInput = findViewById(R.id.captive_portal_user_agent);
        Button saveUaButton = findViewById(android.R.id.button2);
        saveUaButton.setOnClickListener(v -> {
            Editable ua = uaInput.getText();
            ConnectivityManager.setCaptivePortalUserAgent(this, TextUtils.isEmpty(ua) ? null : ua.toString());
            // Regenerate summary
            summary.setText(generateSummary());
        });
        // (S)NTP Server
        EditText ntpServerInput = findViewById(R.id.ntp_server);
        Button saveNtpServerButton = findViewById(android.R.id.button3);
        saveNtpServerButton.setOnClickListener(v -> {
            Editable ntpServer = ntpServerInput.getText();
            ConnectivityManager.setNtpServer(this, TextUtils.isEmpty(ntpServer) ? null : ntpServer.toString());
            // Regenerate summary
            summary.setText(generateSummary());
        });
        registerReceiver(cpControllerWatcher, new IntentFilter(Utils.ACTION_CP_MODE_CHANGED));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(cpControllerWatcher);
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            if (ConnectivityManager.canWriteToGlobalSettings(this)) {
                // Permission granted, enable controller
                ConnectivityManager.setControllerEnabled(this, true);
            } else {
                // Permission not granted, display warning
                // No chance of race-condition here
                enableSwitch.setChecked(false);
                Utils.getPermissionDialog(this).show();
            }
        } else {
            // Disable controller
            ConnectivityManager.setControllerEnabled(this, false);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Set captive portal mode directly
        ConnectivityManager.setCaptivePortalMode(this, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public CharSequence generateSummary() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            boolean https = ConnectivityManager.useHttps(this);
            String serverHost = Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_SERVER);
            String serverUrl = String.format("%s://%s%s", https ? "https" : "http", serverHost, "/generate_204");
            return String.format("Server: %s\nUser agent: %s\nNTP server: %s", serverUrl, userAgent,
                    ConnectivityManager.getNtpServer(this));
        }
        String userAgent = Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_USER_AGENT);
        return String.format("HTTPS: %s\nHTTP: %s\nFallback 1: %s\nFallback 2: %s\nUser agent: %s\nNTP server: %s",
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_HTTPS_URL),
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_HTTP_URL),
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_FALLBACK_URL),
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS),
                userAgent != null ? userAgent : this.userAgent,
                ConnectivityManager.getNtpServer(this));
    }
}
