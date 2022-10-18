package io.github.muntashirakon.captiveportalcontroller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.widget.Toast;

public final class ConnectivityManager {
    /**
     * What to do when connecting a network that presents a captive portal.
     * Must be one of the CAPTIVE_PORTAL_MODE_* constants below.
     * <p>
     * The default for this setting is CAPTIVE_PORTAL_MODE_PROMPT.
     *
     * @since API 26
     */
    public static final String CAPTIVE_PORTAL_MODE = "captive_portal_mode";

    /**
     * Setting to turn off captive portal detection. Feature is enabled by
     * default and the setting needs to be set to 0 to disable it.
     *
     * @since API 21
     * @deprecated use CAPTIVE_PORTAL_MODE_IGNORE to disable captive portal detection
     */
    @Deprecated
    public static final String CAPTIVE_PORTAL_DETECTION_ENABLED = "captive_portal_detection_enabled";

    /**
     * The server used for captive portal detection upon a new connection. A
     * 204 response code from the server is used for validation.
     *
     * @since API 21
     */
    public static final String CAPTIVE_PORTAL_SERVER = "captive_portal_server";

    /**
     * Whether to use HTTPS for network validation. This is enabled by default and the setting
     * needs to be set to 0 to disable it. This setting is a misnomer because captive portals
     * don't actually use HTTPS, but it's consistent with the other settings.
     *
     * @since API 24
     */
    public static final String CAPTIVE_PORTAL_USE_HTTPS = "captive_portal_use_https";

    /**
     * The URL used for HTTPS captive portal detection upon a new connection.
     * A 204 response code from the server is used for validation.
     *
     * @since API 26
     */
    public static final String CAPTIVE_PORTAL_HTTPS_URL = "captive_portal_https_url";

    /**
     * The URL used for HTTP captive portal detection upon a new connection.
     * A 204 response code from the server is used for validation.
     *
     * @since API 26
     */
    public static final String CAPTIVE_PORTAL_HTTP_URL = "captive_portal_http_url";

    /**
     * The URL used for fallback HTTP captive portal detection when previous HTTP
     * and HTTPS captive portal detection attempts did not return a conclusive answer.
     *
     * @since API 26
     */
    public static final String CAPTIVE_PORTAL_FALLBACK_URL = "captive_portal_fallback_url";

    /**
     * A comma separated list of URLs used for captive portal detection in addition to the
     * fallback HTTP url associated with the CAPTIVE_PORTAL_FALLBACK_URL settings.
     *
     * @since API 26
     */
    public static final String CAPTIVE_PORTAL_OTHER_FALLBACK_URLS = "captive_portal_other_fallback_urls";

    /**
     * Don't attempt to detect captive portals.
     *
     * @since API 26
     */
    public static final int CAPTIVE_PORTAL_MODE_IGNORE = 0;

    /**
     * When detecting a captive portal, display a notification that
     * prompts the user to sign in.
     *
     * @since API 26
     */
    public static final int CAPTIVE_PORTAL_MODE_PROMPT = 1;

    /**
     * When detecting a captive portal, immediately disconnect from the
     * network and do not reconnect to that network in the future.
     *
     * @since API 26
     */
    public static final int CAPTIVE_PORTAL_MODE_AVOID = 2;

    /**
     * Which User-Agent string to use in the header of the captive portal detection probes.
     * The User-Agent field is unset when this setting has no value (HttpUrlConnection default).
     *
     * @since API 26
     */
    public static final String CAPTIVE_PORTAL_USER_AGENT = "captive_portal_user_agent";

    public static int getTheirCaptivePortalMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Settings.Global.getInt(context.getContentResolver(), CAPTIVE_PORTAL_MODE, CAPTIVE_PORTAL_MODE_PROMPT);
        } else {
            return Settings.Global.getInt(context.getContentResolver(), CAPTIVE_PORTAL_DETECTION_ENABLED, CAPTIVE_PORTAL_MODE_PROMPT);
        }
    }

    public static int getOurCaptivePortalMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        return prefs.getInt(CAPTIVE_PORTAL_MODE, 1);
    }

    public static boolean useHttps(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Settings.Global.getInt(context.getContentResolver(), CAPTIVE_PORTAL_USE_HTTPS, 1) == 1;
        }
        return false;
    }

    public static void initPrefsIfNotAlready(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (prefs.contains("controller_enabled")) {
            return;
        }
        String captivePortalHttpsUrl;
        String captivePortalHttpUrl;
        String captivePortalFallbackUrl;
        String captivePortalOtherFallbackUrls;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            captivePortalHttpsUrl = Settings.Global.getString(context.getContentResolver(), CAPTIVE_PORTAL_HTTPS_URL);
            captivePortalHttpUrl = Settings.Global.getString(context.getContentResolver(), CAPTIVE_PORTAL_HTTP_URL);
            captivePortalFallbackUrl = Settings.Global.getString(context.getContentResolver(), CAPTIVE_PORTAL_FALLBACK_URL);
            captivePortalOtherFallbackUrls = Settings.Global.getString(context.getContentResolver(), CAPTIVE_PORTAL_OTHER_FALLBACK_URLS);
        } else {
            boolean https = ConnectivityManager.useHttps(context);
            String serverHost = Settings.Global.getString(context.getContentResolver(), ConnectivityManager.CAPTIVE_PORTAL_SERVER);
            captivePortalHttpsUrl = null;
            captivePortalHttpUrl = String.format("%s://%s%s", https ? "https" : "http", serverHost, "/generate_204");
            captivePortalFallbackUrl = null;
            captivePortalOtherFallbackUrls = null;
        }
        // Store these values to the preference
        prefs.edit()
                .putString(CAPTIVE_PORTAL_HTTPS_URL, captivePortalHttpsUrl)
                .putString(CAPTIVE_PORTAL_HTTP_URL, captivePortalHttpUrl)
                .putString(CAPTIVE_PORTAL_FALLBACK_URL, captivePortalFallbackUrl)
                .putString(CAPTIVE_PORTAL_OTHER_FALLBACK_URLS, captivePortalOtherFallbackUrls)
                // Init controller
                .putBoolean("controller_enabled", false)
                .putInt(CAPTIVE_PORTAL_MODE, getTheirCaptivePortalMode(context))
                .apply();
    }

    public static boolean controllerEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("controller_enabled", false)) {
            return canWriteToGlobalSettings(context);
        }
        return false;
    }

    public static void setControllerEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("controller_enabled", enabled).apply();
        checkCaptivePortalMode(context);
    }

    public static void setCaptivePortalMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit().putInt(CAPTIVE_PORTAL_MODE, mode).apply();
        checkCaptivePortalMode(context);
    }

    public static void setCaptivePortalServers(Context context, String https, String http, String fallback, String otherFallback) {
        if (!canWriteToGlobalSettings(context) || !controllerEnabled(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Settings.Global.putString(context.getContentResolver(), CAPTIVE_PORTAL_HTTPS_URL, https);
            Settings.Global.putString(context.getContentResolver(), CAPTIVE_PORTAL_HTTP_URL, http);
            Settings.Global.putString(context.getContentResolver(), CAPTIVE_PORTAL_FALLBACK_URL, fallback);
            Settings.Global.putString(context.getContentResolver(), CAPTIVE_PORTAL_OTHER_FALLBACK_URLS, otherFallback);
        }
        String serverHost;
        if (http.startsWith("http://") || http.startsWith("https://")) {
            serverHost = Uri.parse(http).getHost();
        } else {
            serverHost = Uri.parse("http://" + http).getHost();
        }
        Settings.Global.putString(context.getContentResolver(), CAPTIVE_PORTAL_SERVER, serverHost);
        Toast.makeText(context, R.string.re_enable_networking, Toast.LENGTH_SHORT).show();
    }

    public static void setCaptivePortalUserAgent(Context context, String userAgentString) {
        if (!canWriteToGlobalSettings(context) || !controllerEnabled(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Settings.Global.putString(context.getContentResolver(), CAPTIVE_PORTAL_USER_AGENT, userAgentString);
            Toast.makeText(context, R.string.re_enable_networking, Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean canWriteToGlobalSettings(Context context) {
        return context.checkPermission(Manifest.permission.WRITE_SECURE_SETTINGS, Process.myPid(), Process.myUid())
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void checkCaptivePortalMode(Context context) {
        if (!canWriteToGlobalSettings(context) || !controllerEnabled(context)) {
            return;
        }
        int ourCaptivePortalMode = getOurCaptivePortalMode(context);
        int theirCaptivePortalMode = getTheirCaptivePortalMode(context);
        if (ourCaptivePortalMode != theirCaptivePortalMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Settings.Global.putInt(context.getContentResolver(), CAPTIVE_PORTAL_MODE, ourCaptivePortalMode);
            } else {
                Settings.Global.putInt(context.getContentResolver(), CAPTIVE_PORTAL_DETECTION_ENABLED,
                        ourCaptivePortalMode == 1 ? CAPTIVE_PORTAL_MODE_PROMPT : CAPTIVE_PORTAL_MODE_IGNORE);
            }
            context.sendBroadcast(new Intent(Utils.ACTION_CP_MODE_CHANGED));
            Toast.makeText(context, R.string.re_enable_networking, Toast.LENGTH_SHORT).show();
        }
    }
}
