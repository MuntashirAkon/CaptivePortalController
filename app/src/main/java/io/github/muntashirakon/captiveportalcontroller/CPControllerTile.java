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
        updateTile(controllerEnabled);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        boolean controllerEnabled = ConnectivityManager.controllerEnabled(getApplicationContext());
        updateTile(controllerEnabled);
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
            boolean controllerEnabled = !ConnectivityManager.controllerEnabled(getApplicationContext());
            ConnectivityManager.setControllerEnabled(this, controllerEnabled, CPControllerTile.class.getName());
            updateTile(controllerEnabled);
        } else {
            // Permission not present
            showDialog(Utils.getPermissionDialog(this));
        }
    }

    public void updateTile(boolean controllerEnabled) {
        Tile tile = getQsTile();
        tile.setState(controllerEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(controllerEnabled ? R.string.cp_enabled : R.string.cp_disabled));
        }
        tile.updateTile();
    }
}
