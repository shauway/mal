package com.github.shauway.mal.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shauway
 */

public class PermissionManager {
    private static final String TAG = PermissionManager.class.getSimpleName();

    private Activity requestPermissionsActivity;

    private final List<String> dangerousPermissions;

    private HashMap<String, String> permissionRationaleMessage;

    private PermissionAllGrantedListener listener;

    protected static final ThreadLocal<PermissionManager> threadLocal = new ThreadLocal<>();

    /**
     * 由于用户手动为应用开启权限时，系统回收了PermissionManager实例，所以当用户返回到应用时需要重试权限申请流程
     */
    public static final int RESULT_RETRY = 16;

    /**
     * @param requestPermissionsActivity
     */
    public PermissionManager(@NonNull Activity requestPermissionsActivity) {
        this.requestPermissionsActivity = requestPermissionsActivity;
        permissionRationaleMessage = new HashMap<>();

        dangerousPermissions = new ArrayList<>();
        dangerousPermissions.add(Manifest.permission.READ_CALENDAR);
        dangerousPermissions.add(Manifest.permission.WRITE_CALENDAR);
        dangerousPermissions.add(Manifest.permission.CAMERA);
        dangerousPermissions.add(Manifest.permission.READ_CONTACTS);
        dangerousPermissions.add(Manifest.permission.WRITE_CONTACTS);
        dangerousPermissions.add(Manifest.permission.GET_ACCOUNTS);
        dangerousPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        dangerousPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        dangerousPermissions.add(Manifest.permission.RECORD_AUDIO);
        dangerousPermissions.add(Manifest.permission.READ_PHONE_STATE);
        dangerousPermissions.add(Manifest.permission.CALL_PHONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            dangerousPermissions.add(Manifest.permission.READ_CALL_LOG);
            dangerousPermissions.add(Manifest.permission.WRITE_CALL_LOG);
            dangerousPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        dangerousPermissions.add(Manifest.permission.ADD_VOICEMAIL);
        dangerousPermissions.add(Manifest.permission.USE_SIP);
        dangerousPermissions.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        dangerousPermissions.add(Manifest.permission.SEND_SMS);
        dangerousPermissions.add(Manifest.permission.RECEIVE_SMS);
        dangerousPermissions.add(Manifest.permission.READ_SMS);
        dangerousPermissions.add(Manifest.permission.RECEIVE_WAP_PUSH);
        dangerousPermissions.add(Manifest.permission.RECEIVE_MMS);
        dangerousPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            dangerousPermissions.add(Manifest.permission.READ_PHONE_NUMBERS);
            dangerousPermissions.add(Manifest.permission.ANSWER_PHONE_CALLS);
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT_WATCH) {
            dangerousPermissions.add(Manifest.permission.BODY_SENSORS);
        }
    }

    public PermissionManager need(@NonNull String permission, @NonNull String rationaleMessage) {
        if (dangerousPermissions.contains(permission) && ContextCompat.checkSelfPermission(requestPermissionsActivity, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionRationaleMessage.put(permission, rationaleMessage);
        }
        return this;
    }

    public void request(int requestCode) {
        request(requestCode, null);
    }

    public void request(int requestCode, PermissionAllGrantedListener listener) {
        this.listener = listener;
        if (isAllNeededPermissionsGranted()) {
            if (listener != null) {
                listener.onGranted(requestPermissionsActivity);
            }
            return;
        }
        threadLocal.set(this);
        Intent intent = new Intent(requestPermissionsActivity, PermissionManagerActivity.class);
        requestPermissionsActivity.startActivityForResult(intent, requestCode);
    }

    protected void requestPermission(@NonNull Activity permissionManagerActivity) {
        String[] ungrantedPermissions = getUngrantedPermissions();
        if (ungrantedPermissions != null && ungrantedPermissions.length > 0) {
            ActivityCompat.requestPermissions(permissionManagerActivity, ungrantedPermissions, 0);
        } else {
            permissionManagerActivity.setResult(Activity.RESULT_OK);
            permissionManagerActivity.finish();
            if (listener != null) {
                listener.onGranted(requestPermissionsActivity);
            }
        }
    }

    protected void handlePermissionsResult(@NonNull final Activity permissionManagerActivity, @NonNull final String permissions[], int grantResults[]) {
        ViewGroup view = permissionManagerActivity.findViewById(R.id.rationale_message_container);
        view.removeAllViews();
        permissionManagerActivity.findViewById(R.id.btn_request_permissions).setVisibility(View.VISIBLE);
        permissionManagerActivity.findViewById(R.id.btn_quit).setVisibility(View.VISIBLE);
        int unpromptCount = 0; // 用户勾选[不再询问]并拒绝授权的权限数
        for (int i = 0; i < permissions.length; i++) {
            // 拒绝授权的向用户展示申请该权限的理由
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                TextView tvRationalMessage = new TextView(permissionManagerActivity);
                tvRationalMessage.setText(this.permissionRationaleMessage.get(permissions[i]));
                view.addView(tvRationalMessage);
            }
            if (!ActivityCompat.shouldShowRequestPermissionRationale(permissionManagerActivity, permissions[i])) {
                unpromptCount++;
            }
        }
        // 如果用户拒绝了全部权限并且全部勾选了[不再提示]，由提示用户手动开启所需的权限
        if (unpromptCount > 0 && unpromptCount == permissions.length) {
            TextView tvManualGrantPermissionTip = new TextView(permissionManagerActivity);
            tvManualGrantPermissionTip.setText(R.string.msg_manual_grant_permissions);
            view.addView(tvManualGrantPermissionTip);
            permissionManagerActivity.findViewById(R.id.btn_request_permissions).setVisibility(View.GONE);
            permissionManagerActivity.findViewById(R.id.btn_manual_grant_permissions).setVisibility(View.VISIBLE);
        }
        // 如果所需权限都已授权，则结束该Activity，并向客户端Activity返回RESULT_OK结果
        if (isAllNeededPermissionsGranted()) {
            permissionManagerActivity.setResult(Activity.RESULT_OK);
            permissionManagerActivity.finish();
        }
    }

    public static boolean isAllPermissionsGranted(@NonNull Context context, @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllNeededPermissionsGranted() {
        for (Map.Entry<String, String> entry : permissionRationaleMessage.entrySet()) {
            if (ContextCompat.checkSelfPermission(requestPermissionsActivity, entry.getKey()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // 获取未授权的权限
    private String[] getUngrantedPermissions() {
        List<String> ungrantedPermissions = new ArrayList<>();
        for (String permission : permissionRationaleMessage.keySet()) {
            if (ContextCompat.checkSelfPermission(requestPermissionsActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                ungrantedPermissions.add(permission);
            }
        }
        String[] ss = new String[ungrantedPermissions.size()];
        ungrantedPermissions.toArray(ss);
        return ss;
    }

}
