/*
 * Copyright (C) 2013 JRDP Project
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

package com.jrdp.client.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import com.jrdp.core.remote.rdp.Cursor;
import com.jrdp.core.remote.rdp.KeyMap;
import com.jrdp.core.remote.rdp.RemoteDesktopApplication;

public class RemoteTextureSurface extends SurfaceView implements com.jrdp.core.remote.Canvas, SurfaceHolder.Callback {
    private static final int CLICK_TIME_THRESHOLD = 200;
    private static final int CLICK_DISTANCE_THRESHOLD = 3;
    private static final int FLING_REFRESH_RATE = 30;

    private RemoteDesktopApplication app;
    private Thread uiThread;
    private Scroller cursorScroller;
    private Handler handler;
    private ScaleGestureDetector gestureDetector;

    private final Paint defaultPaint = new Paint();

    private boolean haveSurface;
    SurfaceHolder surfaceHolder;

    private boolean ignoreRemainingTouchEvent = false;
    private int minimumFlingVelocity;
    private boolean keepFlinging;

    public RemoteTextureSurface(Context context) {
        super(context);

        init(context);
    }

    public RemoteTextureSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public RemoteTextureSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
        uiThread = Thread.currentThread();
        handler = new Handler();
        cursorScroller = new Scroller(context);
        minimumFlingVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        gestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener()
        {
            @Override
            public boolean onScale(ScaleGestureDetector detector)
            {
                if(detector.getCurrentSpan() < 10f)
                    return true;
                float difference = detector.getCurrentSpan() - detector.getPreviousSpan();
                if(difference < 1 && difference > -1)
                {
                    //Filter out very minor events, don't want the screen to be jittery when two fingers lay idle on screen
                    return true;
                }

                final float spanDelta = detector.getScaleFactor();
                //TODO: all that remains
//                zoomBy(ZOOM_TYPE_ADDITIVE, spanDelta);
//                centerOnCursor();

                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector)
            {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector)
            {
                ignoreRemainingTouchEvent = true;
            };
        });

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public synchronized void surfaceCreated(SurfaceHolder holder) {
        haveSurface = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public synchronized void surfaceDestroyed(SurfaceHolder holder) {
        haveSurface = false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
        if(changed)
            app.onScreenDimensionsChanged(right - left, bottom - top);
    }

    private float touchOriginX;
    private float touchOriginY;
    private long touchOriginTime;
    private float touchX;
    private float touchY;
    private VelocityTracker velocityTracker;

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        gestureDetector.onTouchEvent(event);
        if(gestureDetector.isInProgress())
            return true;

        short diffX, diffY;
        float x = event.getX();
        float y = event.getY();
        if(velocityTracker == null)
            velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if(!cursorScroller.isFinished())
                    cursorScroller.abortAnimation();

                touchOriginX = touchX = x;
                touchOriginY = touchY = y;
                touchOriginTime = event.getEventTime();
                break;
            case MotionEvent.ACTION_MOVE:
                if(ignoreRemainingTouchEvent)
                    return true;
                diffX = (short) (touchX - x);
                diffY = (short) (touchY - y);

                //make sure cursor doesn't go past bounds of canvas
                final int cursorX = getCursorX();
                final int cursorY = getCursorY();
                final int width = surfaceBitmap.getWidth();
                final int height = surfaceBitmap.getHeight();
                if(diffX + cursorX < 0)
                    diffX = (short) -cursorX;
                else if(diffX + cursorX > width)
                    diffX = (short) (width - cursorX);
                if(diffY + cursorY < 0)
                    diffY = (short) -cursorY;
                else if(diffY + cursorY > height)
                    diffY = (short) (height - cursorY);

                app.onCursorChanged(diffX, diffY);

                touchX = x;
                touchY = y;
                break;
            case MotionEvent.ACTION_UP:
                diffX = (short) (touchOriginX - x);
                diffY = (short) (touchOriginY - y);

                if(event.getEventTime() - touchOriginTime <= CLICK_TIME_THRESHOLD &&
                        Math.abs(diffX) <= CLICK_DISTANCE_THRESHOLD &&
                        Math.abs(diffY) <= CLICK_DISTANCE_THRESHOLD)
                {
                    app.onCursorLeftClick();
                }
                else
                {
                    velocityTracker.computeCurrentVelocity(1000);
                    if(!ignoreRemainingTouchEvent)
                    {
                        int initialXVelocity = (int) velocityTracker.getXVelocity();
                        int initialYVelocity = (int) velocityTracker.getYVelocity();
                        if (Math.abs(initialXVelocity) + Math.abs(initialYVelocity) > minimumFlingVelocity) {
                            cursorScroller.fling(getCursorX(), getCursorY(), -initialXVelocity,
                                    -initialYVelocity, 0, surfaceBitmap.getWidth(), 0, surfaceBitmap.getHeight());
                            keepFlinging = true;
                            handler.postDelayed(new Runnable(){
                                public void run()
                                {
                                    cursorScroller.computeScrollOffset();
                                    if(keepFlinging && !cursorScroller.isFinished())
                                    {
                                        app.onCursorSet(cursorScroller.getCurrX(), cursorScroller.getCurrY());
                                        handler.postDelayed(this, 1000 / FLING_REFRESH_RATE);
                                    }
                                }
                            }, 1);
                        }
                    }
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    ignoreRemainingTouchEvent = false;
                }
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                app.requestDisconnect(true);
                break;
            case KeyEvent.KEYCODE_DEL:
                app.onSpecialKeyboardKey(KeyMap.BACKSPACE);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                break;
            case KeyEvent.KEYCODE_ENTER:
                app.onSpecialKeyboardKey(KeyMap.ENTER);
                break;
            default:
                int unicode = event.getUnicodeChar();
                if(unicode == 0)
                    return false;
                else if((unicode & KeyCharacterMap.COMBINING_ACCENT) != 0)
                    ;//TODO: this
                else
                {
                    char character = (char) unicode;
                    app.onKeyboardKey(character);
                    break;
                }
                break;
        }
        return true;
    }

    ///////////////////////////////////////
    //SurfaceHolder.Callback implementation
    ///////////////////////////////////////

    private boolean showCursor = false;
    private Cursor cursor;
    private int cursorX;
    private int cursorY;

    Bitmap surfaceBitmap;

    public void setSurfaceDimensions(int width, int height) {
        surfaceBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    }

    private boolean canDraw() {
        if(surfaceBitmap == null || !haveSurface)
            return false;
        return true;
    }

    @Override
    public void setCanvasBottomUp(int[] img, int width, int height, int x, int y, int clippingWidth, int clippingHeight) {
        if(canDraw())
            return;

        surfaceBitmap.setPixels(img, 0, width, x, y, clippingWidth, clippingHeight);

        Rect dirty = new Rect(x, y, x + width, y + height);
        Canvas canvas = surfaceHolder.lockCanvas(dirty);
        if(canvas != null) {
            canvas.drawBitmap(img, 0, width, x, y, clippingWidth, clippingHeight, true, null);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void setCanvas(int[] img, int width, int height, int x, int y, int clippingWidth, int clippingHeight) {
        if(canDraw())
            return;

        surfaceBitmap.setPixels(img, width * (clippingHeight - 1), -width, x, y, clippingWidth, clippingHeight);

        Rect dirty = new Rect(x, y, x + width, y + height);
        Canvas canvas = surfaceHolder.lockCanvas(dirty);
        if(canvas != null) {
            canvas.drawBitmap(img, width * (clippingHeight - 1), -width, x, y, width, height, true, null);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void cursorPositionChanged(int x, int y) {
        if(canDraw())
            return;

        //remove old cursor
//        Rect dirty = new Rect(cursorX, cursorY, cursor.getWidth() + cursorX, cursor.getHeight() + cursorY);
//        Canvas canvas = surfaceHolder.lockCanvas(dirty);
//        if(canvas != null) {
//            final Bitmap noCursorBitmap = Bitmap.createBitmap(surfaceBitmap, cursorX, cursorY, cursor.getWidth(), cursor.getHeight());
//            int[] bitmapData = new int[noCursorBitmap.getWidth() * noCursorBitmap.getHeight()];
//            noCursorBitmap.getPixels(bitmapData, 0, noCursorBitmap.getWidth(), 0, 0, noCursorBitmap.getWidth(), noCursorBitmap.getHeight());
//            canvas.drawBitmap(bitmapData, 0, cursor.getWidth(), cursorX - cursor.getHotspotX(), cursorY - cursor.getHotspotY(), cursor.getWidth(), cursor.getHeight(), true, null);
//
//            //draw new cursor
//            cursorX = x;
//            cursorY = y;
//
//            dirty = new Rect(x, y, cursor.getWidth() + x, cursor.getHeight() + y);
//            canvas.drawBitmap(cursor.getCursorBitmap(), 0, cursor.getWidth(), cursorX - cursor.getHotspotX(), cursorY - cursor.getHotspotY(), cursor.getWidth(), cursor.getHeight(), true, null);
//
//            surfaceHolder.unlockCanvasAndPost(canvas);
//        }
        cursorX = x;
        cursorY = y;

        Canvas canvas = surfaceHolder.lockCanvas();
        if(canvas != null) {
            canvas.drawBitmap(surfaceBitmap, 0, 0, null);
            if(showCursor)
                canvas.drawBitmap(cursor.getCursorBitmap(), 0, cursor.getWidth(),
                        cursorX - cursor.getHotspotX(), cursorY - cursor.getHotspotY(),
                        cursor.getWidth(), cursor.getHeight(), true, null);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void redrawCursor() {
        cursorPositionChanged(cursorX, cursorY);
    }

    @Override
    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
        redrawCursor();
    }

    @Override
    public void hideCursor() {
        showCursor = false;
        redrawCursor();
    }

    @Override
    public void showCursor() {
        showCursor = false;
        redrawCursor();
    }

    @Override
    public int getCursorX() {
        return cursorX;
    }

    @Override
    public int getCursorY() {
        return cursorY;
    }
}
