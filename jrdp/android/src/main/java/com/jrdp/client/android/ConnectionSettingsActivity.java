package com.jrdp.client.android;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.jrdp.core.util.Logger;

public class ConnectionSettingsActivity extends Activity
{
	public static final int ARGUMENTS = 0;
	private LinearLayout minimized;
	private LinearLayout maximized;
	private EditText server;
	private EditText domain;
	private EditText port;
	private EditText username;
	private EditText password;
	private Button connect;
	private Spinner resolution;
	private Spinner encryption;
	private Spinner colorDepth;
	private Storage storage;
	
	private boolean isNew = false;
	private String connectionId;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		storage = new Storage(this);
		
		setContentView(R.layout.connection_settings);
		connect = (Button) findViewById(R.id.connect_button);
		minimized = (LinearLayout) findViewById(R.id.minimized_layout);
		maximized = (LinearLayout) findViewById(R.id.maximized_layout);
		server = (EditText) findViewById(R.id.server_field);
		port = (EditText) findViewById(R.id.port_field);
		domain = (EditText) findViewById(R.id.domain_field);
		username = (EditText) findViewById(R.id.username_field);
		password = (EditText) findViewById(R.id.password_field);
		resolution = (Spinner) findViewById(R.id.reolution_spinner);
		encryption = (Spinner) findViewById(R.id.encryption_spinner);
		colorDepth = (Spinner) findViewById(R.id.color_depth_spinner);
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, 
				R.array.resolution_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		resolution.setAdapter(adapter);
		
		adapter = ArrayAdapter.createFromResource(this, R.array.encryption_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		encryption.setAdapter(adapter);
		
		adapter = ArrayAdapter.createFromResource(this, R.array.color_depth_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		colorDepth.setAdapter(adapter);
		
		OnClickListener minimize = new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				minimized.setVisibility(View.VISIBLE);
				maximized.setVisibility(View.GONE);
			}
		};
		OnClickListener maximize = new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				minimized.setVisibility(View.GONE);
				maximized.setVisibility(View.VISIBLE);
			}
		};
		
		findViewById(R.id.minimizedImage).setOnClickListener(maximize);
		findViewById(R.id.minimizedText).setOnClickListener(maximize);
		findViewById(R.id.maximizedImage).setOnClickListener(minimize);
		findViewById(R.id.maximizedText).setOnClickListener(minimize);
		
		connect.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(validateForms(true))
				{
					save();
					connect();
				}
			}
		});
		
		Uri data = this.getIntent().getData();
		if(data != null)
		{
			connectionId = data.getQuery();
			Cursor cursor = storage.getData(Storage.TABLE_CONNECTION_INFO, null, Storage.ROW_ID + "=" + connectionId, null, null);
			//TODO: assert cursor != null
			if(cursor.moveToFirst())
			{
				server.setText(cursor.getString(cursor.getColumnIndex(Storage.ROW_IP)));
				port.setText(String.valueOf(cursor.getInt(cursor.getColumnIndex(Storage.ROW_PORT))));
				username.setText(cursor.getString(cursor.getColumnIndex(Storage.ROW_USERNAME)));
				password.setText(cursor.getString(cursor.getColumnIndex(Storage.ROW_PASSWORD)));
				username.setText(cursor.getString(cursor.getColumnIndex(Storage.ROW_USERNAME)));
				domain.setText(cursor.getString(cursor.getColumnIndex(Storage.ROW_DOMAIN)));
				resolution.setSelection(cursor.getInt(cursor.getColumnIndex(Storage.ROW_RESOLUTION_INDEX)));
				encryption.setSelection(cursor.getInt(cursor.getColumnIndex(Storage.ROW_ENCRYPTION_LEVEL)));
				colorDepth.setSelection(cursor.getInt(cursor.getColumnIndex(Storage.ROW_COLOR)));
			}
			else
			{
				//TODO: error: database corruption or wrong primary key... something went very wrong
			}
		}
		else
			isNew = true;
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onPause()
	{
		if(hasContent(server) && hasContent(port))
			save();
		super.onPause();
	}
	
	private void connect()
	{
		Uri.Builder uri = new Uri.Builder();
		uri.scheme("content");
		uri.path("connectionid");
		uri.query(connectionId + "");

		Intent intent = new Intent(ConnectionSettingsActivity.this, RemoteDesktopActivity.class);
		intent.setData(uri.build());
		this.startActivity(intent);
	}
	
	private void save()
	{
		ContentValues settings = new ContentValues();
		settings.put(Storage.ROW_NICKNAME, "Nickname Placeholder");
		settings.put(Storage.ROW_IP, server.getText().toString());
		settings.put(Storage.ROW_PORT, port.getText().toString());
		settings.put(Storage.ROW_USERNAME, username.getText().toString());
		settings.put(Storage.ROW_PASSWORD, password.getText().toString());
		settings.put(Storage.ROW_DOMAIN, domain.getText().toString());
		settings.put(Storage.ROW_ENCRYPTION_LEVEL, encryption.getSelectedItemPosition());
		settings.put(Storage.ROW_PERFORMANCE_FLAGS, 1);
		settings.put(Storage.ROW_RESOLUTION_INDEX, resolution.getSelectedItemPosition());
		settings.put(Storage.ROW_COLOR, colorDepth.getSelectedItemPosition());
		settings.put(Storage.ROW_INPUT_TYPE, 2);
		//TODO: implement ordering of connection list
		settings.put(Storage.ROW_LIST_ORDER, 1);
		if(isNew)
		{
			connectionId = String.valueOf(storage.insert(Storage.TABLE_CONNECTION_INFO, settings));
			isNew = false;
		}
		else
			storage.updateData(Storage.TABLE_CONNECTION_INFO, settings, Storage.ROW_ID + "=" + connectionId);
		Logger.log("saved new connection under id: " + connectionId);
	}
	
	private boolean validateForms(boolean showPrompts)
	{
		TextView serverRequired = (TextView) findViewById(R.id.server_required);
		TextView portRequired = (TextView) findViewById(R.id.port_required);
		boolean serverInvalid = false;
		boolean portInvalid = false;

		if(hasContent(server) == false)
		{
			serverInvalid = true;
			serverRequired.setVisibility(View.VISIBLE);
		}
		else
		{
			serverRequired.setVisibility(View.INVISIBLE);
		}
		if(hasContent(port) == false)
		{
			portInvalid = true;
			portRequired.setVisibility(View.VISIBLE);
		}
		else
		{
			portRequired.setVisibility(View.INVISIBLE);
		}

		if((portInvalid || serverInvalid) && showPrompts)
		{
			if(portInvalid && serverInvalid)
				showInvalidFieldPrompt(R.string.invalid_field_title, R.string.invalid_server_and_port);
			else if(portInvalid)
				showInvalidFieldPrompt(R.string.invalid_field_title, R.string.invalid_port);
			else if(serverInvalid)
				showInvalidFieldPrompt(R.string.invalid_field_title, R.string.invalid_server);
			return false;
		}
		else if(portInvalid || serverInvalid)
		{
			return false;
		}
		try
		{
			Integer.parseInt(port.getText().toString());
		}
		catch(NumberFormatException e)
		{
			if(showPrompts)
			{
				showInvalidFieldPrompt(R.string.error, R.string.invalid_port_format);
			}
			return false;
		}
		return true;
	}
	
	private boolean hasContent(EditText field)
	{
		if(field.getText().toString().equals(""))
			return false;
		return true;
	}
	
	private void showInvalidFieldPrompt(int resTitle, int resMessage)
	{
		Builder builder = new Builder(this);
		builder.setTitle(resTitle);
		builder.setMessage(resMessage);
		builder.setPositiveButton(R.string.ok, new Dialog.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
	}
}
