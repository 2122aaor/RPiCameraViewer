package ca.frozen.rpicameraviewer.activities;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

import ca.frozen.rpicameraviewer.App;
import ca.frozen.rpicameraviewer.classes.Camera;
import ca.frozen.rpicameraviewer.classes.CameraAdapter;
import ca.frozen.rpicameraviewer.classes.Network;
import ca.frozen.rpicameraviewer.classes.Utils;
import ca.frozen.rpicameraviewer.R;

public class MainActivity extends AppCompatActivity
{
	// local constants
	private final static String TAG = "MainActivity";

	// instance variables
	private CameraAdapter adapter;
	private ScannerFragment scannerFragment;

	//******************************************************************************
	// onCreate
	//******************************************************************************
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// set the view
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// create the toolbar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// load the settings, networks and cameras
		Utils.loadData();

		// set the list adapter
		ListView listView = (ListView)findViewById(R.id.cameras);
		adapter = new CameraAdapter();
		adapter.refresh();
		listView.setAdapter(adapter);
		registerForContextMenu(listView);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adaptr, View view, int position, long id)
			{
				startVideoActivity(adapter.getCameras().get(position));
			}
		});

		// create the add button
		FloatingActionButton addCameraButton = (FloatingActionButton) findViewById(R.id.add_camera);
		addCameraButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Camera camera = new Camera();
				camera.name = Utils.getNextCameraName();
				startCameraActivity(camera);
			}
		});

		// do a scan if there are no cameras
		if (savedInstanceState == null && adapter.getCameras().size() == 0)
		{
			Handler handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					startScanner();
				}
			}, 500);
		}
	}

	//******************************************************************************
	// onResume
	//******************************************************************************
	@Override
	public void onResume()
	{
		super.onResume();
		Utils.reloadData();
		adapter.refresh();
	}

	//******************************************************************************
	// onSaveInstanceState
	//******************************************************************************
	@Override
	protected void onSaveInstanceState(Bundle state)
	{
		state.putInt("dummy", 1);
		super.onSaveInstanceState(state);
	}

	//******************************************************************************
	// onCreateOptionsMenu
	//******************************************************************************
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	//******************************************************************************
	// onOptionsItemSelected
	//******************************************************************************
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        int id = item.getItemId();

		// scan for cameras
        if (id == R.id.action_scan)
        {
			startScanner();
            return true;
        }

		// delete all the cameras
		else if (id == R.id.action_delete_all)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage(R.string.ok_to_delete_all_cameras);
			alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if (Utils.getSettings().showAllCameras)
					{
						Utils.getCameras().clear();
						Utils.getNetworks().clear();
					}
					else
					{
						Network network = null;
						List<Camera> allCameras = Utils.getCameras();
						for (Camera camera : adapter.getCameras())
						{
							allCameras.remove(camera);
							network = camera.network;
						}
						if (network != null)
						{
							Utils.getNetworks().remove(network);
						}
					}
					updateCameras();
					dialog.dismiss();
				}
			});
			alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			});

			alert.show();
		}

		// display the list of networks
		else if (id == R.id.action_networks)
		{
			Intent intent = new Intent(this, NetworksActivity.class);
			startActivity(intent);
			return true;
		}

		// edit the settings
		else if (id == R.id.action_settings)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}

		// display the help information
		else if (id == R.id.action_help)
		{
			Intent intent = new Intent(this, HelpActivity.class);
			startActivity(intent);
			return true;
		}

		// display the about information
        else if (id == R.id.action_about)
        {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
	}

	//******************************************************************************
	// onCreateContextMenu
	//******************************************************************************
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		if (v.getId() == R.id.cameras)
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu_main_list, menu);
		}
	}

	//******************************************************************************
	// onContextItemSelected
	//******************************************************************************
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		final Camera camera;
		AdapterView.AdapterContextMenuInfo info;

		switch (item.getItemId())
		{
			// edit the selected camera
			case R.id.action_edit:
				info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				camera = adapter.getCameras().get(info.position);
				startCameraActivity(camera);
				return true;

			// edit the selected camera's network
			case R.id.action_edit_network:
				info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				camera = adapter.getCameras().get(info.position);
				startNetworkActivity(camera.network);
				return true;

			// prompt the user to delete the selected camera
			case R.id.action_delete:
				info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
				camera = adapter.getCameras().get(info.position);
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setMessage(R.string.ok_to_delete_camera);
				alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Utils.getCameras().remove(camera);
						updateCameras();
						dialog.dismiss();
					}
				});
				alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});

				alert.show();
				return true;

			// call the super
			default:
				return super.onContextItemSelected(item);
		}
	}

	//******************************************************************************
	// startScanner
	//******************************************************************************
	private void startScanner()
	{
		FragmentManager fm = getFragmentManager();
		scannerFragment = new ScannerFragment();
		scannerFragment.show(fm, "Scanner");
	}

	//******************************************************************************
	// startCameraActivity
	//******************************************************************************
	private void startCameraActivity(Camera camera)
	{
		Intent intent = new Intent(App.getContext(), CameraActivity.class);
		intent.putExtra(CameraActivity.CAMERA, camera);
		startActivity(intent);
	}

	//******************************************************************************
	// startNetworkActivity
	//******************************************************************************
	private void startNetworkActivity(Network network)
	{
		Intent intent = new Intent(App.getContext(), NetworkActivity.class);
		intent.putExtra(NetworkActivity.NETWORK, network);
		startActivity(intent);
	}

	//******************************************************************************
	// startVideoActivity
	//******************************************************************************
	private void startVideoActivity(Camera camera)
	{
		Intent intent = new Intent(App.getContext(), VideoActivity.class);
		intent.putExtra(VideoActivity.CAMERA, camera);
		startActivity(intent);
	}

	//******************************************************************************
	// updateCameras
	//******************************************************************************
	public void updateCameras()
	{
		Collections.sort(Utils.getCameras());
		Utils.saveData();
		adapter.refresh();
	}
}
