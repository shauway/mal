package com.github.shauway.mal.demo.barscan;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.github.shauway.mal.barscan.CaptureFragment;
import com.github.shauway.mal.R;
import com.google.zxing.Result;

public class BarscanDemoActivity extends AppCompatActivity implements CaptureFragment.OnFragmentInteractionListener {
    private CaptureFragment captureFragment;
    private ViewGroup viewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barscan_demo);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        captureFragment = (CaptureFragment) getSupportFragmentManager().findFragmentById(R.id.capture_fragment);
        viewGroup = findViewById(R.id.btn_container);

        if (viewGroup != null) {
            viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    viewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    captureFragment.setFramingRectCenterTopOffset(viewGroup.getHeight() / 2);
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (captureFragment.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDecodeSucceeded(Result result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(result.getText());
        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> captureFragment.restartPreviewAfterDelay(0L));
        builder.setOnCancelListener(dialog -> captureFragment.restartPreviewAfterDelay(0L));
        builder.show();
    }
}
