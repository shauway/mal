package com.github.shauway.mal.permission;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class PermissionManagerActivity extends AppCompatActivity {
    private static final String TAG = PermissionManagerActivity.class.getSimpleName();
    private static final int REQUEST_MANUAL_GRANT_PERMISSION = 1;
    private PermissionManager permissionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.permission_manager_activity_title_name);
        setContentView(R.layout.activity_permission_manager);
        permissionManager = PermissionManager.threadLocal.get();
        if (permissionManager != null) {
            permissionManager.requestPermission(this);
        } else { // 系统回收了PermissionManager对象
            setResult(PermissionManager.RESULT_RETRY);
            finish();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionManager.handlePermissionsResult(this, permissions, grantResults);
    }

    public void onBtnRequestPermissionsClicked(View view) {
        permissionManager.requestPermission(this);
    }

    public void onBtnQuitClicked(View view) {
        finish();
    }

    public void onBtnManualGrantPermissionClicked(View view) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_MANUAL_GRANT_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MANUAL_GRANT_PERMISSION) {
            permissionManager.requestPermission(this);
        }
    }
}
