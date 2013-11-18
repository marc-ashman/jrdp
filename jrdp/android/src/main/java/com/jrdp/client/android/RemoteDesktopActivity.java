package com.jrdp.client.android;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.inputmethodservice.Keyboard;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.jrdp.core.remote.rdp.Constants;
import com.jrdp.core.remote.rdp.NetworkManager;
import com.jrdp.core.remote.rdp.Rdp;
import com.jrdp.core.remote.rdp.RdpConnectionInfo;
import com.jrdp.core.remote.rdp.RemoteDesktopApplication;
import com.jrdp.core.util.Logger;

public class RemoteDesktopActivity extends Activity implements RemoteDesktopApplication, AdvancedImageView.OnZoomListener, OnEditorActionListener
{
	private static final int ZOOM_MAX_DEFAULT = 4;
	private static final int SEEK_BAR_MAX_COUNT = 1000;
	
	public static final String CONNECTION_INFO_SERVER = "serverKey";
	public static final String CONNECTION_INFO_PORT = "portKey";
	public static final String CONNECTION_INFO_USERNAME = "usernameKey";
	public static final String CONNECTION_INFO_PASSWORD = "passwordKey";
	public static final String CONNECTION_INFO_DOMAIN = "domainKey";
	public static final String CONNECTION_INFO_IP = "ipKey";
	public static final String CONNECTION_INFO_ENCRYPTION_LEVEL = "encryptionKey";
	public static final String CONNECTION_INFO_TIMEZONE_BIAS = "timezoneKey";
	public static final String CONNECTION_INFO_PERFORMANCE_FLAGS = "performanceKey";
	public static final String CONNECTION_INFO_WIDTH = "widthKey";
	public static final String CONNECTION_INFO_HEIGHT = "heightKey";
	public static final String CONNECTION_INFO_COLOR_DEPTH = "depthKey";
	public static final String CONNECTION_INFO_CONNECTION_TYPE = "connectionKey";
	public static final String CONNECTION_INFO_IS_FRENCH_LOCALE = "frenchKey";
	public static final String CONNECTION_INFO_INPUT_TYPE = "InputTypeKey";

	public static final int INPUT_TYPE_DIRECT = 0x00000001;
	public static final int INPUT_TYPE_INPUT_FIELD = 0x00000002;
	
	public static final String DEBUG_INSTRUCTIONS = "containsDebug";
	
	private Rdp rdp;
	
	private AndroidCanvas canvas;
	private RemoteDesktopView rdpView;
	private SeekBar zoomBar;
	private RelativeLayout zoomContent;
	private RelativeLayout inputContent;
	private Toast lastToast;
	private Button zoomHideButton;
	private Button originalScaleButton;
	private Button inputHideButton;
	private Button inputSendButton;
	private WindowsKeyboardView windowsKeyboard;
	private EditText inputField;
	
	private int remoteScreenWidth;
	private int remoteScreenHeight;
	private float zoomUnit;
	private int inputType;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		/**
		 * Get all necessary connection parameters
		 */
		Uri data = getIntent().getData();
		if(data == null)
		{
			//TODO: epic error
			String msg = "No data sent into intent for RDPActivity";
			Logger.log(Logger.ERROR, msg);
			throw new RuntimeException(msg);
		}
		String connectionId = data.getQuery();
		Storage storage = new Storage(this);
		Cursor cursor = storage.getData(Storage.TABLE_CONNECTION_INFO, null, Storage.ROW_ID + "=" + connectionId, null, null);
		if(cursor.moveToFirst() == false)
		{
			//TODO: epic error
			String msg = "No columns exists for given connection ID";
			Logger.log(Logger.ERROR, msg);
			throw new RuntimeException(msg);
		}
		
		String server = cursor.getString(cursor.getColumnIndex(Storage.ROW_IP));
		int port = cursor.getInt(cursor.getColumnIndex(Storage.ROW_PORT));
		String username = cursor.getString(cursor.getColumnIndex(Storage.ROW_USERNAME));
		String password = cursor.getString(cursor.getColumnIndex(Storage.ROW_PASSWORD));
		String domain = cursor.getString(cursor.getColumnIndex(Storage.ROW_DOMAIN));
		String ip = "192.168.0.1";
		int timezoneBias = -5;
		int performanceFlags = 0;
		byte connectionType = Constants.CONNECTION_TYPE_BROADBAND_LOW;
		boolean isFrench = false;
		inputType = INPUT_TYPE_DIRECT;
		
		Resources resources = getResources();
		int[] encryptionValues = resources.getIntArray(R.array.encryption_values);
		int[] resolutionWidthValues = resources.getIntArray(R.array.resolution_x_values);
		int[] resolutionHeightValues = resources.getIntArray(R.array.resolution_y_values);
		int[] colorDepthValues = resources.getIntArray(R.array.color_depth_values);
		int encryptionLevel = encryptionValues[cursor.getInt(cursor.getColumnIndex(Storage.ROW_ENCRYPTION_LEVEL))];
		remoteScreenWidth = resolutionWidthValues[cursor.getInt(cursor.getColumnIndex(Storage.ROW_RESOLUTION_INDEX))];
		remoteScreenHeight = resolutionHeightValues[cursor.getInt(cursor.getColumnIndex(Storage.ROW_RESOLUTION_INDEX))];
		int colorDepth = colorDepthValues[cursor.getInt(cursor.getColumnIndex(Storage.ROW_COLOR))];
		
		final RdpConnectionInfo info = new RdpConnectionInfo(username, password, domain, ip,
				timezoneBias, performanceFlags, (short) remoteScreenWidth, (short) remoteScreenHeight,
				(short) colorDepth,	connectionType, isFrench);
		
		setContentView(R.layout.rdp);
		
		zoomBar = (SeekBar) findViewById(R.id.zoomBar);
		zoomContent = (RelativeLayout) findViewById(R.id.zoomContent);
		zoomHideButton = (Button) findViewById(R.id.zoomHideButton);
		originalScaleButton = (Button) findViewById(R.id.originalScaleButton);
		windowsKeyboard = (WindowsKeyboardView) findViewById(R.id.keyboardView);
		inputContent = (RelativeLayout) findViewById(R.id.inputContent);
		inputField = (EditText) findViewById(R.id.editText);
		inputHideButton = (Button) findViewById(R.id.inputHideButton);
		inputSendButton = (Button) findViewById(R.id.inputSendButton);
		rdpView = (RemoteDesktopView) findViewById(R.id.remoteView);
		/**
		 * BitmapDrawable(Bitmap) was deprecated, and now seems to call 
		 * BitmapDrawable(Bitmap, getResources()). For some annoying reason, 
		 * BitmapDrawables are limited by the width and height given in the 
		 * resources, so we need to create a new copy of the app's resources 
		 * with modified width and height set to 1080p. 
		 */
		Configuration configuration = new Configuration();
		configuration.setToDefaults();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		metrics.widthPixels = 1920;
		metrics.heightPixels = 1050;
		Resources res = new Resources(this.getAssets(), metrics, configuration);
		
		windowsKeyboard.setKeyboard(new Keyboard(this, R.xml.windows_keyboard));
		windowsKeyboard.setOnKeyboardKeyPressedListener(new WindowsKeyboardView.OnKeyboardKeyPressedListener(){
			@Override
			public void onKeyPressed(int key)
			{
			}

			@Override
			public void onModifiersChanged(int modifierMask)
			{
			}

			@Override
			public void onHideRequested()
			{
				windowsKeyboard.setVisibility(View.GONE);
			}
		});
		inputField.setOnEditorActionListener(this);
		
		canvas = new AndroidCanvas(res, Bitmap.createBitmap(remoteScreenWidth, remoteScreenHeight, Config.RGB_565), new Handler(), this);
		
		rdpView.setCanvas(canvas);
		rdpView.setRdpApplication(this);
		rdpView.setOnZoomListener(this);
		
		
		zoomHideButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) {
				zoomContent.setVisibility(View.GONE);
			}
		});
		originalScaleButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				rdpView.zoomBy(RemoteDesktopView.ZOOM_TYPE_ABSOLUTE, 1);
				rdpView.centerOnCursor();
			}
		});
		inputHideButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				inputContent.setVisibility(View.GONE);
			}
		});
		inputSendButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				sendInputFieldText();
			}
		});
		zoomBar.setMax(SEEK_BAR_MAX_COUNT);
		zoomBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser)
			{
				if(fromUser)
				{
					final float max = rdpView.getMaximumZoomFactor();
					float zoom = (zoomUnit * progress) + rdpView.getMinimumZoomFactor();
					if(zoom > max)
						zoom = max;
					String text = "Current Zoom: x" + String.format("%.2f", zoom);
					if(lastToast == null)
						lastToast = Toast.makeText(RemoteDesktopActivity.this, text, 2);
					lastToast.setText(text);
					lastToast.show();
					rdpView.zoomBy(RemoteDesktopView.ZOOM_TYPE_ABSOLUTE, zoom);
					rdpView.centerOnCursor();
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
        });

		rdpView.requestFocus();
		rdp = new Rdp(new NetworkManager(server, port), info, canvas);
		new Thread(new Runnable(){
			public void run()
			{
				rdp.connect();
			}
		}).start();
	}
    
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);

		windowsKeyboard.setKeyboard(new Keyboard(this, R.xml.windows_keyboard));
    }
	
	private String getPassedString(Intent intent, String key, String defaultValue)
	{
		String value = intent.getStringExtra(key);
		if(value == null)
			return defaultValue;
		return value;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.remote_desktop_menu, menu);
		if((inputType & INPUT_TYPE_DIRECT) == 0)
		{
			MenuItem item = menu.findItem(R.id.menuShowKeyboard);
			item.setEnabled(false);
			item.setVisible(false);
		}
		if((inputType & INPUT_TYPE_INPUT_FIELD) == 0)
		{
			MenuItem item = menu.findItem(R.id.menuShowInputField);
			item.setEnabled(false);
			item.setVisible(false);
		}
		return true;
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	enableMenuItem(menu.findItem(R.id.menuZoom), zoomContent.getVisibility() == View.GONE);
    	enableMenuItem(menu.findItem(R.id.menuShowInputField), inputContent.getVisibility() == View.GONE);
    	enableMenuItem(menu.findItem(R.id.menuShowWinKeyboard), windowsKeyboard.getVisibility() == View.GONE);
        return true;
    }
    
    private void enableMenuItem(MenuItem item, boolean enabled)
    {
		item.setEnabled(enabled);
		item.setVisible(enabled);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.menuZoom:
        	zoomBar.setProgress(getSeekBarZoomProgress(rdpView.getZoomScale()));
        	zoomContent.setVisibility(View.VISIBLE);
            break;
        case R.id.menuDisconnect:
        	requestDisconnect(true);
        	break;
        case R.id.menuShowKeyboard:
        	InputMethodManager inputManager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        	inputManager.toggleSoftInput(0, 0);
        	break;
        case R.id.menuShowInputField:
        	inputContent.setVisibility(View.VISIBLE);
        	break;
        case R.id.menuShowWinKeyboard:
        	if(windowsKeyboard.getVisibility() == View.GONE)
        		windowsKeyboard.setVisibility(View.VISIBLE);
        	else
        		windowsKeyboard.setVisibility(View.GONE);
        	break;
        default:
        	return false;
        }
        return false;
    }
    
    private int getSeekBarZoomProgress(float zoom)
    {
    	return (int) ((zoom / zoomUnit) - (rdpView.getMinimumZoomFactor() / zoomUnit));
    }
    
    private void setZoomLimits(int width, int height)
    {
    	float minWidth = (float) width / (float) canvas.getBitmap().getWidth();
    	float minHeight = (float) height / (float) canvas.getBitmap().getHeight();
    	
    	float minZoom = Math.min(minWidth, minHeight);
    	float maxZoom = ZOOM_MAX_DEFAULT;
    	
    	rdpView.setZoomLimits(minZoom, maxZoom);
		zoomUnit = (maxZoom - minZoom) / SEEK_BAR_MAX_COUNT;
    }

	@Override
	public void onScreenDimensionsChanged(int width, int height)
	{
		setZoomLimits(width, height);
		rdpView.centerOnCursor();
	}

	@Override
	public void onRemoteDimensionsChanged(int width, int height)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCursorChanged(int deltaX, int deltaY)
	{
		rdp.moveMouse((short) deltaX, (short) deltaY);
	}

	@Override
	public void onCursorSet(int x, int y)
	{
		rdp.setMouse((short) x, (short) y);
	}

	@Override
	public void onCursorLeftClick()
	{
		rdp.clickMouseLeft();
	}

	@Override
	public void onSpecialKeyboardKey(short key)
	{
		rdp.sendSpecialKeyboardKey(key);
	}

	@Override
	public void onKeyboardKey(char key)
	{
		rdp.sendKeyboardKey(key);
	}

	@Override
	public void onRequestRemoteViewRefresh()
	{
		rdpView.centerOnCursor();
	}

	@Override
	public void requestDisconnect(boolean askUser)
	{
		if(askUser == false)
		{
			disconnect();
			return;
		}
		Builder dialog = new Builder(this);
		dialog.setTitle(R.string.disconnect);
		dialog.setIcon(R.drawable.dialog_info);
		dialog.setCancelable(true);
		dialog.setMessage(R.string.sure_to_disconnect);
		dialog.setOnCancelListener(new OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				dialog.dismiss();
			}
		});
		dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				disconnect();
			}
		});
		dialog.show();
	}

	@Override
	public void onZoom(float scale)
	{
		if(zoomContent.getVisibility() != View.GONE)
		{
			zoomBar.setProgress(getSeekBarZoomProgress(scale));
		}
	}
	
	private void disconnect()
	{
		rdp.disconnect();
		finish();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
			requestDisconnect(true);
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public boolean onEditorAction(TextView view, int actionId, KeyEvent event)
	{
		if(actionId == EditorInfo.IME_ACTION_SEND)
			sendInputFieldText();
		return true;
	}
	
	private void sendInputFieldText()
	{
		final String text = inputField.getText().toString();
		final int length = text.length();
		for(int i=0; i < length; i++)
		{
			onKeyboardKey(text.charAt(i));
		}
		inputField.setText("");
		inputField.clearFocus();
	}
}