package com.github.shauway.mal.barscan;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.github.shauway.barscan.R;
import com.github.shauway.mal.barscan.camera.CameraManager;
import com.google.zxing.Result;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CaptureFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * create an instance of this fragment.
 */
public class CaptureFragment extends Fragment implements SurfaceHolder.Callback {

    private static final String TAG = CaptureFragment.class.getSimpleName();

    private boolean vibrate; // 扫码成功后是否振动手机
    private boolean beep;   // 扫码成功后是否蜂鸣提示
    private int framingRectCenterTopOffset = 0; // 默认取景框在垂直方向上的偏移

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private OnFragmentInteractionListener mListener;

    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;


    public CaptureFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);


        View decor = window.getDecorView();
        int systemUiVisibility = decor.getSystemUiVisibility();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        systemUiVisibility |= flags;
        decor.setSystemUiVisibility(systemUiVisibility);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }


        hasSurface = false;
        inactivityTimer = new InactivityTimer(getActivity());
        beepManager = new BeepManager(getActivity(), this.beep, this.vibrate);
        ambientLightManager = new AmbientLightManager(getActivity());

    }

    @Override
    public void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = new FrameLayout(getContext());
        surfaceView = new SurfaceView(getContext());
        root.addView(surfaceView);
        if (this.viewfinderView != null) {
            root.addView(viewfinderView);
        }
        root.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return root;
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CaptureFragment);
        this.framingRectCenterTopOffset = ta.getDimensionPixelOffset(R.styleable.CaptureFragment_framing_rect_center_top_offset, 0);
        this.beep = ta.getBoolean(R.styleable.CaptureFragment_beep, true);
        this.vibrate = ta.getBoolean(R.styleable.CaptureFragment_vibrate, false);
        String viewfinderClass = ta.getString(R.styleable.CaptureFragment_viewfinder_name);
        if (viewfinderClass == null || viewfinderClass.trim().length() == 0) {
            this.viewfinderView = new DefaultViewfinderView(context);
        } else {
            try {
                Class c = Class.forName(viewfinderClass);
                Constructor<ViewfinderView> constructor = c.getDeclaredConstructor(Context.class);
                this.viewfinderView = constructor.newInstance(context);
            } catch (ClassNotFoundException | IllegalAccessException | java.lang.InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        ta.recycle();

    }


    @Override
    public void onResume() {
        super.onResume();

        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        } else {
            android.support.v7.app.ActionBar supportActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.hide();
            }
        }

        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        cameraManager = new CameraManager(getActivity().getApplication(), surfaceHolder, this.framingRectCenterTopOffset);
        viewfinderView.setCameraManager(cameraManager);

        handler = null;

        beepManager.updatePrefs(this.beep, this.vibrate);
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    public void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onDecodeSucceeded(Result result);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                turnOffTorch();
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                turnOnTorch();
                return true;
        }
        return false;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     */
    public void handleDecode(Result rawResult) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();
        mListener.onDecodeSucceeded(rawResult);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver();
            if (handler == null) {
                handler = new CaptureActivityHandler(this, null, null, cameraManager);
            }
        } catch (IOException ioe) {
            displayFrameworkBugMessageAndExit();
            throw new RuntimeException(ioe);
        } catch (RuntimeException e) {
            displayFrameworkBugMessageAndExit();
            throw e;
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, (dialog, which) -> getActivity().finish());
        builder.setOnCancelListener(dialog -> getActivity().finish());
        builder.show();
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void turnOnTorch() {
        cameraManager.setTorch(true);
    }

    public void turnOffTorch() {
        cameraManager.setTorch(false);
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public void setBeep(boolean beep) {
        this.beep = beep;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public void setFramingRectCenterTopOffset(int framingRectCenterTopOffset) {
        this.framingRectCenterTopOffset = framingRectCenterTopOffset;
        cameraManager.setFramingRectCenterTopOffset(this.framingRectCenterTopOffset);
    }
}
