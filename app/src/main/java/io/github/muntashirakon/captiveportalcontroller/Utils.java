package io.github.muntashirakon.captiveportalcontroller;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

public final class Utils {
    public static final String ACTION_CP_MODE_CHANGED = BuildConfig.APPLICATION_ID + ".action.CP_MODE_CHANGED";

    public static AlertDialog getPermissionDialog(Context context) {
        View view = View.inflate(context, R.layout.dialog_no_permission, null);
        TextView tv = view.findViewById(android.R.id.text1);
        tv.setText(String.format("pm grant %s %s", BuildConfig.APPLICATION_ID, Manifest.permission.WRITE_SECURE_SETTINGS));
        tv.setKeyListener(null);
        tv.setSelectAllOnFocus(true);
        tv.requestFocus();
        return new AlertDialog.Builder(context)
                .setView(view)
                .setNegativeButton(android.R.string.ok, null)
                .create();
    }
}
