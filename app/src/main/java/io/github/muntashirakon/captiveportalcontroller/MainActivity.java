package io.github.muntashirakon.captiveportalcontroller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private AutoCompleteTextView captivePortalHttpsUrl;
    private AutoCompleteTextView captivePortalHttpUrl;
    private AutoCompleteTextView captivePortalFallbackUrl;
    private AutoCompleteTextView captivePortalOtherFallbackUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectivityManager.initPrefsIfNotAlready(this);
        ConnectivityManager.checkCaptivePortalMode(this);
        Switch enableSwitch = findViewById(android.R.id.toggle);
        enableSwitch.setChecked(ConnectivityManager.controllerEnabled(this));
        enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (ConnectivityManager.canWriteToGlobalSettings(this)) {
                    // Permission granted, enable controller
                    ConnectivityManager.setControllerEnabled(this, true);
                } else {
                    // Permission not granted, display warning
                    // No chance of race-condition here
                    enableSwitch.setChecked(false);
                    displayPermissionMessage();
                }
            } else {
                // Disable controller
                ConnectivityManager.setControllerEnabled(this, false);
            }
        });
        Spinner mode = findViewById(R.id.spinner);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mode.setAdapter(ArrayAdapter.createFromResource(this, R.array.cp_modes, android.R.layout.simple_list_item_1));
        } else {
            mode.setAdapter(ArrayAdapter.createFromResource(this, R.array.cp_modes_pre26, android.R.layout.simple_list_item_1));
        }
        mode.setSelection(ConnectivityManager.getOurCaptivePortalMode(this));
        mode.setOnItemSelectedListener(this);
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

        Button saveButton = findViewById(android.R.id.button1);
        saveButton.setOnClickListener(v -> {
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
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Set captive portal mode directly
        ConnectivityManager.setCaptivePortalMode(this, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void displayPermissionMessage() {
        View view = View.inflate(this, R.layout.dialog_no_permission, null);
        TextView tv = view.findViewById(android.R.id.text1);
        tv.setText(String.format("pm grant %s %s", BuildConfig.APPLICATION_ID, Manifest.permission.WRITE_SECURE_SETTINGS));
        tv.setKeyListener(null);
        tv.setSelectAllOnFocus(true);
        tv.requestFocus();
        new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton(android.R.string.ok, null)
                .show();
    }

    public CharSequence generateSummary() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_SERVER);
        }
        return String.format("HTTPS: %s\nHTTP: %s\nFallback 1: %s\nFallback 2: %s",
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_HTTPS_URL),
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_HTTP_URL),
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_FALLBACK_URL),
                Settings.Global.getString(getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_OTHER_FALLBACK_URLS));
    }
}
