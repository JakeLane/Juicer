package me.jakelane.juicer;

import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.VideoView;
import android.widget.ViewFlipper;

public class MainActivity extends Activity {
	private ViewFlipper logoAnimation;
	private FrameLayout frameVideoLogo;
	private VideoView logoVideo;
	private Camera cameraService;
	private Vibrator vibrateService;
	private BluetoothAdapter mBluetoothAdapter;
	private LocationManager localLocationManager;
	private LocationListener listenerLocationManager;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Vibrator service
		vibrateService = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		// GPS setup (set nothing to happen on updates)
		localLocationManager = (LocationManager) getSystemService("location");
		listenerLocationManager = new LocationListener() {
			public void onLocationChanged(Location paramAnonymousLocation) {}

			public void onProviderDisabled(String paramAnonymousString) {}

			public void onProviderEnabled(String paramAnonymousString) {}

			public void onStatusChanged(String paramAnonymousString, int paramAnonymousInt, Bundle paramAnonymousBundle) {}
		};
		// Video setup
		frameVideoLogo = (FrameLayout) findViewById(R.id.frameVideoLogo);
		logoVideo = (VideoView) findViewById(R.id.videoLogo);
		logoVideo.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.logo));
		logoVideo.setBackgroundColor(Color.WHITE);
		logoVideo.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.setLooping(true);
				logoVideo.setBackgroundColor(Color.TRANSPARENT);
			}
		});
		// Battery %
		registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.actionAbout:
				openAbout();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void openAbout() {
		// Open about activity on action button click
		Intent aboutIntent = new Intent(this, AboutActivity.class);
		startActivity(aboutIntent);
	}

	public void toggleDrainClicked(View view) {
		// Is the toggle on?
		boolean on = ((ToggleButton) view).isChecked();
		// Enable/Disable the drainer
		if (on) {
			// Do animation stuff
			logoAnimation = (ViewFlipper) findViewById(R.id.imageflipper);
			logoAnimation.setAutoStart(true);
			logoAnimation.setFlipInterval(150);
			// Video
			CheckBox checkboxVideo = (CheckBox) findViewById(R.id.checkBoxVideo);
			checkboxVideo.setEnabled(false);
			if (checkboxVideo.isChecked()) {
				// Make the animation invisible (in an inefficient way :D)
				logoAnimation.setVisibility(View.INVISIBLE);
				// Play the video
				logoVideo.start();
				// Make it visible
				frameVideoLogo.setVisibility(View.VISIBLE);
				logoVideo.resume();
			} else {
				// Start it
				logoAnimation.startFlipping();
			}
			// Start the timer
			((Chronometer) findViewById(R.id.drainTime)).setBase(SystemClock.elapsedRealtime());
			((Chronometer) findViewById(R.id.drainTime)).start();
			// Brightness
			CheckBox checkboxBrightness = (CheckBox) findViewById(R.id.checkBoxBrightness);
			checkboxBrightness.setEnabled(false);
			if (checkboxBrightness.isChecked()) {
				// Turn on always screen on
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				// Set brightness to 100%
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				lp.screenBrightness = 1f;
				getWindow().setAttributes(lp);

			} else {
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
			// GPS
			final CheckBox checkboxGPS = (CheckBox) findViewById(R.id.checkBoxGPS);
			checkboxGPS.setEnabled(false);
			if (checkboxGPS.isChecked()) {
				localLocationManager.requestLocationUpdates("gps", 0L, 0.0F, listenerLocationManager);
			}
			// Bluetooth
			CheckBox checkboxBluetooth = (CheckBox) findViewById(R.id.checkBoxBluetooth);
			checkboxBluetooth.setEnabled(false);
			if (checkboxBluetooth.isChecked()) {
				mBluetoothAdapter.enable();
				mBluetoothAdapter.startDiscovery();
			}
			// Vibrate
			CheckBox checkboxVibrate = (CheckBox) findViewById(R.id.checkBoxVibrate);
			checkboxVibrate.setEnabled(false);
			if (checkboxVibrate.isChecked()) {
				// Start without a delay
				long[] pattern = {0, 1000};
				// The '0' here means to repeat indefinitely
				vibrateService.vibrate(pattern, 0);
			}
			// LED
			CheckBox checkboxLED = (CheckBox) findViewById(R.id.checkBoxLED);
			checkboxLED.setEnabled(false);
			if (checkboxLED.isChecked()) {
				cameraService = Camera.open();
				Parameters p = cameraService.getParameters();
				p.setFlashMode(Parameters.FLASH_MODE_TORCH);
				cameraService.setParameters(p);
				cameraService.startPreview();
			}
		} else {
			// Stop the animation
			logoAnimation.stopFlipping();
			logoAnimation.setVisibility(View.VISIBLE);
			logoAnimation.setDisplayedChild(0);
			// Video
			CheckBox checkboxVideo = (CheckBox) findViewById(R.id.checkBoxVideo);
			checkboxVideo.setEnabled(true);
			// If the video is playing, stop it and make it invisible
			if (checkboxVideo.isChecked()) {
				logoVideo.stopPlayback();
				frameVideoLogo.setVisibility(View.INVISIBLE);
			}
			// Stop the timer
			((Chronometer) findViewById(R.id.drainTime)).stop();
			// Brightness
			CheckBox checkboxBrightness = (CheckBox) findViewById(R.id.checkBoxBrightness);
			checkboxBrightness.setEnabled(true);
			// Reset brightness
			WindowManager.LayoutParams lp = getWindow().getAttributes();
			lp.screenBrightness = -1f;
			getWindow().setAttributes(lp);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// GPS
			CheckBox checkboxGPS = (CheckBox) findViewById(R.id.checkBoxGPS);
			localLocationManager.removeUpdates(listenerLocationManager);
			checkboxGPS.setEnabled(true);
			// Bluetooth
			CheckBox checkboxBluetooth = (CheckBox) findViewById(R.id.checkBoxBluetooth);
			checkboxBluetooth.setEnabled(true);
			mBluetoothAdapter.cancelDiscovery();
			// Vibrate
			CheckBox checkboxVibrate = (CheckBox) findViewById(R.id.checkBoxVibrate);
			checkboxVibrate.setEnabled(true);
			vibrateService.cancel();
			// LED
			CheckBox checkboxLED = (CheckBox) findViewById(R.id.checkBoxLED);
			checkboxLED.setEnabled(true);
			if (checkboxLED.isChecked()) {
				Parameters p = cameraService.getParameters();
				p.setFlashMode(Parameters.FLASH_MODE_OFF);
				cameraService.setParameters(p);
				cameraService.stopPreview();
				cameraService.release();
				cameraService = null;
			}
		}
	}

	public void onCheckboxClicked(final View view) {
		// Is the view now checked?
		boolean checked = ((CheckBox) view).isChecked();

		// Check which check box was clicked
		switch (view.getId()) {
			case R.id.checkBoxVibrate:
				if (checked) {
					new AlertDialog.Builder(this).setTitle(R.string.warning).setMessage(R.string.vibrateWarning).setPositiveButton(
							android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									return;
								}
							}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							((CheckBox) view).setChecked(false);
						}
					}).show();
				}
				break;
			case R.id.checkBoxLED:
				if (checked && !getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
					new AlertDialog.Builder(this).setTitle(R.string.warning).setMessage(R.string.noFlashWarning).setPositiveButton(
							android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									return;
								}
							}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							((CheckBox) view).setChecked(false);
						}
					}).show();
				}
				break;
			case R.id.checkBoxBluetooth:
				mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				if (checked) {
					if (mBluetoothAdapter == null) {
						new AlertDialog.Builder(this).setTitle(R.string.warning).setMessage(R.string.noBluetooth).setPositiveButton(
								android.R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										((CheckBox) view).setChecked(false);
									}
								}).show();
					}
				}
				break;
			case R.id.checkBoxGPS:
				if (checked) {
					if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
						new AlertDialog.Builder(this).setTitle(R.string.warning).setMessage(R.string.noGPSWarning).setPositiveButton(
								android.R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										return;
									}
								}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								((CheckBox) view).setChecked(false);
								dialog.cancel();
							}
						}).show();
					}
					final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
					if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
							&& !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
						new AlertDialog.Builder(this).setTitle(R.string.warning).setMessage(R.string.gpsOff).setPositiveButton(android.R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
									}
								}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								((CheckBox) view).setChecked(false);
								dialog.cancel();
							}
						}).show();
					}
				}
				break;
		}
	}

	// Back Button press
	public void onBackPressed() {
		new AlertDialog.Builder(this).setMessage(R.string.backCloseButton).setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Kill LED if still on
						if (cameraService != null) {
							Parameters p = cameraService.getParameters();
							p.setFlashMode(Parameters.FLASH_MODE_OFF);
							cameraService.setParameters(p);
							cameraService.stopPreview();
							cameraService.release();
							cameraService = null;
						}
						// Kill the Vibrate service (just in case)
						if (vibrateService != null) {
							vibrateService.cancel();
						}
						finish();
					}
				}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		}).show();
	}

	public class bluetoothTask extends TimerTask {
		// Bluetooth task
		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}

	// Battery Indicator
	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent i) {
			int level = i.getIntExtra("level", 0);
			TextView tv = (TextView) findViewById(R.id.batteryPercent);
			tv.setText("Battery: " + Integer.toString(level) + "%");
		}

	};

	// Fix video on pause (temporary until I get around to things)
	@Override
	protected void onPause() {
		logoVideo.pause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		logoVideo.start();
		logoVideo.resume();
		super.onResume();
	}
}