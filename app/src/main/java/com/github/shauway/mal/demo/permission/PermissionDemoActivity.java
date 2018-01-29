package com.github.shauway.mal.demo.permission;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.github.shauway.mal.R;
import com.github.shauway.mal.permission.PermissionManager;

public class PermissionDemoActivity extends AppCompatActivity {
    private static final String TAG = PermissionDemoActivity.class.getSimpleName();
    private PermissionManager permissionManager;

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_demo);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getSupportActionBar().setTitle("系统权限Library");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        requestPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) { // 用户拒绝该Activity所需的权限
                finish();
            } else if (resultCode == PermissionManager.RESULT_RETRY) {
                requestPermissions();
            } else if (resultCode == RESULT_OK) {// 权限请求成功
                permissionManager = null;
            }
        }
    }

    private void requestPermissions() {
        if (permissionManager == null &&
                !PermissionManager.isAllPermissionsGranted(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_SMS, Manifest.permission.ACCESS_FINE_LOCATION})) {
            permissionManager = new PermissionManager(this);
            permissionManager.need(Manifest.permission.CAMERA, "需要相机权限")
                    .need(Manifest.permission.READ_SMS, "需要读取短信权限")
                    .need(Manifest.permission.ACCESS_FINE_LOCATION, "需要获取位置的权限")
                    .request(PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
