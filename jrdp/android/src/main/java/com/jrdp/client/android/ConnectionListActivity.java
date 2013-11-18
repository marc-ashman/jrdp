package com.jrdp.client.android;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jrdp.core.util.Logger;

public class ConnectionListActivity extends Activity implements Storage.StorageListener
{
	private ListView list;
	private Button addNewConnectionButton;
	private Button quickConnect;
	private Storage storage;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Logger.setLogger(new AndroidLogger(AndroidLogger.LOG_LEVEL_DEBUG));
		storage = new Storage(this);
		
		setContentView(R.layout.connection_list);
		
		addNewConnectionButton = (Button) findViewById(R.id.addNewConnectionButton);
		quickConnect = (Button) findViewById(R.id.quickConnect);
		list = (ListView) findViewById(R.id.connectionListView);
		
		/////EXPERIMENTAL ANIMATION///////
		AnimationSet set = new AnimationSet(true);
		
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(50);
		set.addAnimation(animation);
		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(100);
		set.addAnimation(animation);
		
		LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
		list.setLayoutAnimation(controller);
		//////////////////////////////////
		
		addNewConnectionButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(ConnectionListActivity.this, ConnectionSettingsActivity.class);
				startActivity(intent);
			}
		});
		quickConnect.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(ConnectionListActivity.this, QuickConnectActivity.class);
				startActivity(intent);
			}
		});
		
		registerForContextMenu(list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startRdpSession((int) id);
            }
        });
		list.setAdapter(new CursorAdapter(this, null)
		{
			@Override
			public void bindView(View view, Context context, Cursor cursor)
			{
				TextView title = (TextView) view.findViewById(R.id.connection_list_item_title);
				TextView subTitle = (TextView) view.findViewById(R.id.connection_list_item_subtitle);
				String connectionNick = cursor.getString(cursor.getColumnIndex(Storage.ROW_NICKNAME));
				String ip = cursor.getString(cursor.getColumnIndex(Storage.ROW_IP));
				if(connectionNick == null || connectionNick.equals(""))
				{
					title.setText(cursor.getLong(cursor.getColumnIndex(Storage.ROW_ID)) + ": " + ip);
					subTitle.setEnabled(false);
					subTitle.setVisibility(View.GONE);
				}
				else
				{
					title.setText(cursor.getLong(cursor.getColumnIndex(Storage.ROW_ID)) + ": " + connectionNick);
					subTitle.setText(ip);
				}
			}

			@Override
			public View newView(Context context, Cursor cursor, ViewGroup parent)
			{
				return ConnectionListActivity.this.getLayoutInflater().inflate(R.layout.connection_list_item, null);
			}
		});
		refreshList();
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		storage.addListener(this);
		refreshList();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		storage.removeListener(this);
	}
	
	protected void refreshList()
	{
		Cursor cursor = storage.getData(Storage.TABLE_CONNECTION_INFO, 
				new String[] { Storage.ROW_ID, Storage.ROW_NICKNAME, Storage.ROW_IP }, 
				null, Storage.ROW_LIST_ORDER + " ASC", null);
		((CursorAdapter)list.getAdapter()).changeCursor(cursor);
		((CursorAdapter)list.getAdapter()).notifyDataSetChanged();
	}

    private void startRdpSession(int connectionId) {
        Uri.Builder uri = new Uri.Builder();
        uri.scheme("content");
        uri.path("connectionid");
        uri.query(connectionId + "");

        Intent intent = new Intent(this, RemoteDesktopActivity.class);
        intent.setData(uri.build());
        this.startActivity(intent);
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.connection_list_context_menu, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId())
		{
			case R.id.contextMenuConnect:
                startRdpSession((int) info.id);
				break;
			case R.id.contextMenuDeleteConnection:
				Builder dialog = new Builder(this);
				dialog.setPositiveButton(this.getResources().getString(R.string.yes), new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						storage.delete(Storage.TABLE_CONNECTION_INFO, Storage.ROW_ID + "=" + info.id);
						dialog.dismiss();
					}
				});
				dialog.setNegativeButton(this.getResources().getString(R.string.no), new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});
				dialog.setIcon(android.R.drawable.ic_dialog_alert);
				dialog.setCancelable(true);
				dialog.setTitle(R.string.confirm);
				dialog.setMessage(R.string.sure_to_delete_connection);
				dialog.show();
				break;
			case R.id.contextMenuEditConnection:
				Intent intent = new Intent(ConnectionListActivity.this, ConnectionSettingsActivity.class);
				Uri.Builder builder = new Uri.Builder();
				builder.scheme("content:").path("connections").query(String.valueOf(info.id));
				intent.setData(builder.build());
				startActivity(intent);
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void onStoredDataChangedListener(Storage storage)
	{
		refreshList();
	}
}
