/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.shauway.mal.barscan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import com.github.shauway.barscan.R;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DefaultViewfinderView extends ViewfinderView {

    private static final long ANIMATION_DELAY = 60L;

    private final Paint maskPaint;
    private final int maskColor;
    private final int laserColor;
    private final int frameBorderColor;
    private final Paint frameCornerPaint;

    private final int triAngleLength = dp2px(20);//每个角的点距离
    private final int triAngleWidth = dp2px(4); //每个角的点宽度
    private int laserTopOffset = 0;

    private Bitmap laser;
    private float laserScale = 1;
    private float laserSpeedE;

    public DefaultViewfinderView(Context context) {
        super(context);
        Resources resources = getResources();
        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskColor = resources.getColor(R.color.viewfinder_mask);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        maskPaint.setColor(maskColor);

        frameBorderColor = resources.getColor(R.color.viewfinder_frame_border);

        frameCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        frameCornerPaint.setColor(frameBorderColor);
        frameCornerPaint.setStrokeWidth(triAngleWidth);
        frameCornerPaint.setStyle(Paint.Style.STROKE);

        laser = BitmapFactory.decodeResource(resources, R.drawable.scan_laser);
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        maskPaint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, maskPaint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom, maskPaint);
        canvas.drawRect(frame.right, frame.top, width, frame.bottom, maskPaint);
        canvas.drawRect(0, frame.bottom, width, height, maskPaint);

        maskPaint.setColor(frameBorderColor);
        canvas.drawRect(frame.left, frame.top, frame.right, frame.top + 2, maskPaint);
        canvas.drawRect(frame.left, frame.bottom - 2, frame.right, frame.bottom, maskPaint);
        canvas.drawRect(frame.left, frame.top, frame.left + 2, frame.bottom, maskPaint);
        canvas.drawRect(frame.right - 2, frame.top, frame.right, frame.bottom, maskPaint);

        // 四个角落的三角
        Path leftTopPath = new Path();
        leftTopPath.moveTo(frame.left + triAngleLength, frame.top + triAngleWidth / 2);
        leftTopPath.lineTo(frame.left + triAngleWidth / 2, frame.top + triAngleWidth / 2);
        leftTopPath.lineTo(frame.left + triAngleWidth / 2, frame.top + triAngleLength);
        canvas.drawPath(leftTopPath, frameCornerPaint);

        Path rightTopPath = new Path();
        rightTopPath.moveTo(frame.right - triAngleLength, frame.top + triAngleWidth / 2);
        rightTopPath.lineTo(frame.right - triAngleWidth / 2, frame.top + triAngleWidth / 2);
        rightTopPath.lineTo(frame.right - triAngleWidth / 2, frame.top + triAngleLength);
        canvas.drawPath(rightTopPath, frameCornerPaint);

        Path leftBottomPath = new Path();
        leftBottomPath.moveTo(frame.left + triAngleWidth / 2, frame.bottom - triAngleLength);
        leftBottomPath.lineTo(frame.left + triAngleWidth / 2, frame.bottom - triAngleWidth / 2);
        leftBottomPath.lineTo(frame.left + triAngleLength, frame.bottom - triAngleWidth / 2);
        canvas.drawPath(leftBottomPath, frameCornerPaint);

        Path rightBottomPath = new Path();
        rightBottomPath.moveTo(frame.right - triAngleLength, frame.bottom - triAngleWidth / 2);
        rightBottomPath.lineTo(frame.right - triAngleWidth / 2, frame.bottom - triAngleWidth / 2);
        rightBottomPath.lineTo(frame.right - triAngleWidth / 2, frame.bottom - triAngleLength);
        canvas.drawPath(rightBottomPath, frameCornerPaint);

        maskPaint.setColor(laserColor);
        laserScale = frame.width() / 500f > frame.height() / 32f ? frame.height() / 32f : frame.width() / 500f;
        laserSpeedE = ANIMATION_DELAY * 1f / cameraManager.getFramingRect().height() * 1f * 2;
        if (laserTopOffset > frame.height() - laser.getHeight() * laserScale) {
            laserTopOffset = 0;
        }
        laserTopOffset = laserTopOffset % frame.height();
        int laserTop = laserTopOffset + frame.top;
        Rect laserDestRect = new Rect(frame.left, laserTop, frame.right, (int) (laserTop + laser.getHeight() * laserScale));
        canvas.drawBitmap(laser, null, laserDestRect, maskPaint);
        long delayMilliseconds = (long) (Math.abs(frame.height() / 2 - laserTopOffset) * laserSpeedE);
        postInvalidateDelayed(delayMilliseconds,
                frame.left, frame.top, frame.right, frame.bottom);
        laserTopOffset = laserTopOffset + 9;


    }

}
