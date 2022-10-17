package io.github.muntashirakon.captiveportalcontroller;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

@TargetApi(Build.VERSION_CODES.N)
public class CPControllerTile extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        boolean controllerEnabled = ConnectivityManager.controllerEnabled(getApplicationContext());
        boolean detectionEnabled = ConnectivityManager.getOurCaptivePortalMode(getApplicationContext()) == ConnectivityManager.CAPTIVE_PORTAL_MODE_PROMPT;
        updateTile(controllerEnabled, detectionEnabled);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        boolean controllerEnabled = ConnectivityManager.controllerEnabled(getApplicationContext());
        boolean detectionEnabled = ConnectivityManager.getOurCaptivePortalMode(getApplicationContext()) == ConnectivityManager.CAPTIVE_PORTAL_MODE_PROMPT;
        updateTile(controllerEnabled, detectionEnabled);
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isLocked()) {
            // Device locked, do not do anything
            return;
        }
        if (ConnectivityManager.canWriteToGlobalSettings(getApplicationContext())) {
            // Permission present
            boolean controllerEnabled = ConnectivityManager.controllerEnabled(getApplicationContext());
            boolean detectionEnabled = !(ConnectivityManager.getOurCaptivePortalMode(getApplicationContext()) == ConnectivityManager.CAPTIVE_PORTAL_MODE_PROMPT);
            ConnectivityManager.setCaptivePortalMode(getApplicationContext(), detectionEnabled ? ConnectivityManager.CAPTIVE_PORTAL_MODE_PROMPT : ConnectivityManager.CAPTIVE_PORTAL_MODE_IGNORE);
            updateTile(controllerEnabled, detectionEnabled);
        } else {
            // Permission not present
            showDialog(Utils.getPermissionDialog(this));
            updateTile(false, true);
        }
    }

    public void updateTile(boolean controllerEnabled, boolean detectionEnabled) {
        Tile tile = getQsTile();
        int state;
        CharSequence subtitle;
        if (!controllerEnabled) {
            state = Tile.STATE_UNAVAILABLE;
            subtitle = null;
        } else {
            state = detectionEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
            subtitle = getString(detectionEnabled ? R.string.cp_enabled : R.string.cp_disabled);
        }
        tile.setState(state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(subtitle);
        }
        tile.updateTile();
    }
}
