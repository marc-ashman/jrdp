package com.jrdp.client.android;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;
import android.widget.Scroller;

import com.jrdp.core.remote.rdp.KeyMap;
import com.jrdp.core.remote.rdp.RemoteDesktopApplication;

public class RemoteDesktopView extends AdvancedImageView
{
	private static final int CLICK_TIME_THRESHOLD = 200;
	private static final int CLICK_DISTANCE_THRESHOLD = 3;
	private static final int FLING_REFRESH_RATE = 30;
	
	private RemoteDesktopApplication app;
	private ScaleGestureDetector gestureDetector;
	private VelocityTracker velocityTracker;
	private Scroller cursorScroller;
	private Handler handler;
	private AndroidCanvas canvas;
	private boolean keepFlinging;
	private boolean ignoreRemainingTouchEvent = false;
	private float touchOriginX;
	private float touchOriginY;
	private long touchOriginTime;
	private float touchX;
	private float touchY;
	private int minimumFlingVelocity;
	private Thread uiThread;
	
	public RemoteDesktopView(Context context)
	{
		super(context);
		init(context);
	}
	
	public RemoteDesktopView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}
	
	public RemoteDesktopView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(context);
	}
	
	public void init(Context context)
	{
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
				zoomBy(ZOOM_TYPE_ADDITIVE, spanDelta);
				centerOnCursor();
				
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

		//TODO: this is a hack for the N1, fix it
		final int width = 1920;
		final int height = 1080;
		setLayoutParams(new RelativeLayout.LayoutParams(width, height));
		setMinimumHeight(height);
		setMinimumWidth(width);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
	}
	
	public void setRdpApplication(RemoteDesktopApplication app)
	{
		this.app = app;
	}
	
	public void setCanvas(AndroidCanvas canvas)
	{
		this.canvas = canvas;
		setImage(canvas, 1, 1);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if(changed)
			app.onScreenDimensionsChanged(right - left, bottom - top);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event)
	{
		return true;
	}
	
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
			final int cursorX = canvas.getCursorX();
			final int cursorY = canvas.getCursorY();
			final int width = canvas.getBitmap().getWidth();
			final int height = canvas.getBitmap().getHeight();
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
						cursorScroller.fling(canvas.getCursorX(), canvas.getCursorY(), -initialXVelocity, 
								-initialYVelocity, 0, canvas.getBitmap().getWidth(), 0, canvas.getBitmap().getHeight());
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
	
	public void centerOnCursor()
	{
		//Can be called by non-UI thread, need this as failsafe
		if(Thread.currentThread() != uiThread)
		{
			handler.postAtFrontOfQueue(new Runnable()
			{
				public void run()
				{
					centerOnCursor();
				}
			});
			return;
		}
		
		final int scrollX = getScrollX();
		final int scrollY = getScrollY();
		final int x = (int) ((canvas.getCursorX() * getZoomScale()) - (getWidth() / 2));
		final int y = (int) ((canvas.getCursorY() * getZoomScale()) - (getHeight() / 2));
		scrollTo(x, y);
		if(scrollX == getScrollX() && scrollY == getScrollY())
			invalidate();
	}
}