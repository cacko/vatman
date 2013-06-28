package com.mutanti.vatman;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.mutanti.vatman.VatmanPreferences.PrefsFragment;
import com.mutanti.vatman.Adapter.FavouritesAdapter;
import com.mutanti.vatman.Adapter.ScheduleAdapter;
import com.mutanti.vatman.Adapter.StopsAdapter;
import com.mutanti.vatman.Exception.VatmanException;
import com.mutanti.vatman.Object.Favourite;
import com.mutanti.vatman.Object.PhoneInfo;
import com.mutanti.vatman.Object.ScheduleItem;
import com.mutanti.vatman.Object.VatmanLocation;
import com.mutanti.vatman.Providers.Db;
import com.mutanti.vatman.Providers.InitialStopsProvider;
import com.mutanti.vatman.Providers.StopsProvider;
import com.mutanti.vatman.util.GeoUtil;

public final class Vatman extends Activity implements LocationListener,
		SensorEventListener {

	private static InputMethodManager sInputManager;
	public static final String TAG = "vatman";
	public static final Boolean AUTORIZATION_REQUIRED = false;
	private static final String URL_GENERATOR = "http://vatman.mutanti.com/xml";
	@SuppressWarnings("unused")
	private static final String URL_GENERATOR_TEST = "http://vatman.mutanti.com/test.xml?";
	private ListView mMainView;

	private ProgressDialog mProgress;
	private ProgressDialog mProgressIndef;
	private int mProgressValue;

	final Handler mHandler = new Handler(new VatmanCallback());

	private Handler mScheduleHandler = new Handler();
	private final int SCHEDULE_UPDATE_INTERVAL = 30000;

	private ImageView mViewModeIcon;
	private AutoCompleteTextView mAutocompleteText;
	private View mSearchContainer;
	private int mCurrentStop = 0;
	private ScheduleAdapter mScheduleAdapter;
	private FavouritesAdapter mFavouritesAdapter;

	public static final int OPERATION_INIT_STOPS = 100;
	public static final int OPERATION_UPDATE_SCHEDULE = 101;
	public static final int OPERATION_ADD_FAVOURITE = 102;
	public static final int OPERATION_REMOVE_FAVOURITE = 103;
	public static final int OPERATION_GET_COUNT = 104;
	public static final int OPERATION_NEW_UPDATE = 105;
	public static final int OPERATION_INIT_STOPS_VERSION_INFO = 106;
	public static final int OPERATION_REMOTE_UPDATE = 107;
	public static final int OPERATION_FILTERED = 108;

	public static final String BUNDLE_ARG_MESSAGE = "message";
	public static final String BUNDLE_ARG_UPDATED_STOPS = "updated_stops";
	public static final String BUNDLE_ARG_STOP_COUNT = "count";
	public static final String BUNDLE_ARG_STOP_CODE = "code";
	public static final String BUNDLE_ARG_STOP_NAME = "name";
	public static final String BUNDLE_ARG_STOP_ROUTE = "route";
	public static final String BUNDLE_ARG_STOP_LAT = "lat";
	public static final String BUNDLE_ARG_STOP_LON = "lon";
	public static final String BUNDLE_ARG_STOP_VERSION = "version";

	public static final int INTENT_LOGIN = 1;
	public static final int INTENT_ABOUT = 2;
	public static final int INTENT_PREFERENCES = 3;
	public static final int INTENT_MAP = 4;

	public static final int VIEW_MODE_STOPS = 1;
	public static final int VIEW_MODE_LINES = 2;

	private Fetcher mFetcher = null;

	private SparseArray<Intent> mIntents = new SparseArray<Intent>();

	final private static int REQUEST_LOGIN = 1;
	final private static int PREFERENCES_SAVED = 2;
	final private static int MAP_RESULT = 3;
	private SharedPreferences mPreferences;
	private String mUsername;
	private String mPassword;
	private long mDbVersion;
	private int mDbTableVersionStops;
	private static ConnectivityManager sConnection;
	private ArrayList<ScheduleItem> mSchedules;

	private Display mDisplay;
	private static int sOrientationCompensation;

	private LocationManager mLocationManager;
	private VatmanLocation mLocation = null;
	private double[] mLocationBoundingBox = null;
	private boolean mPermitLocationUpdates;

	private ArrayList<Favourite> mFavourites;

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mMagnetometer;

	private float[] mGravity;
	private float[] mGeomagnetic;

	private static double sHeading;
	private boolean mHeadingInitialized;

	private Db mDb;
	private StopsProvider mStopsProvider;
	private Favourite mCurrentFavourite;

	public static long TIME_DRIFT;

	private MenuItem mRefreshAction;
	private MenuItem mFavouriteActionOn;
	private MenuItem mFavouriteActionOff;
	private MenuItem mFilterAction;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		sConnection = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		mPreferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		mUsername = mPreferences.getString(VatmanPreferences.KEY_PREF_USERNAME,
				null);
		mPassword = mPreferences.getString(VatmanPreferences.KEY_PREF_PASSWORD,
				null);
		mDbVersion = mPreferences.getLong(
				VatmanPreferences.KEY_PREF_STOP_VERSION, 0);

		mDbTableVersionStops = mPreferences.getInt(
				VatmanPreferences.KEY_PREF_STOPS_TABLE_VERSION, 0);

		mProgressIndef = new ProgressDialog(Vatman.this);
		mProgressIndef.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressIndef.setIndeterminate(true);
		mProgressIndef.setCancelable(false);
		mProgressIndef.setTitle(getString(R.string.please_wait));

		boolean isNewDb = !(getApplicationContext().getDatabasePath(Db.DB_NAME)
				.exists());

		mDb = Db.initInstance(this.openOrCreateDatabase(Db.DB_NAME, 0, null),
				mHandler);
		InitialStopsProvider initStopsProvider = getInitialStopsProvider();
		final long stopsVersion = initStopsProvider.getVersion();
		final int stopsCount = initStopsProvider.getCount();
		initStopsProvider = null;
		if (Db.TABLE_VERSION_STOPS > mDbTableVersionStops) {
			isNewDb = true;
			mDb.initTableStops();
			saveTableVersion(VatmanPreferences.KEY_PREF_STOPS_TABLE_VERSION,
					Db.TABLE_VERSION_STOPS);
			mDbTableVersionStops = Db.TABLE_VERSION_STOPS;
		}
		if (isNewDb || stopsVersion > mDbVersion) {
			initDbData(stopsCount);
		}
		mStopsProvider = StopsProvider.getInstance(mDb);

		setContentView(R.layout.main);

		mAutocompleteText = (AutoCompleteTextView) findViewById(R.id.autocomplete_stop);
		mAutocompleteText.setThreshold(2);

		mFavouritesAdapter = getFavouritesAdapter();

		StopsAdapter myCursorAdapter = new StopsAdapter(this, mDb);
		mAutocompleteText.setAdapter(myCursorAdapter);

		mSearchContainer = (LinearLayout) findViewById(R.id.search_container);

		mMainView = (ListView) findViewById(R.id.main_view);
		mScheduleAdapter = new ScheduleAdapter(this, R.layout.schedule_item,
				new ArrayList<ScheduleItem>(), mHandler);
		mMainView.setAdapter(mFavouritesAdapter);

		mMainView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				FavouritesAdapter adapter = (FavouritesAdapter) parent
						.getAdapter();
				Favourite o = adapter.get(position);
				mCurrentFavourite = o;
				updateSchedule(o.getCode());
			}
		});

		mMainView.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				FavouritesAdapter adapter = (FavouritesAdapter) parent
						.getAdapter();
				mCurrentFavourite = adapter.get(position);
				getIntent(INTENT_MAP).putExtra(VatmanMap.BUNDLE_CURRENT_STOP,
						mCurrentFavourite);
				startActivityForResult(getIntent(INTENT_MAP), MAP_RESULT);
				return true;
			}
		});

		mAutocompleteText.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				AutoCompleteTextView atv = (AutoCompleteTextView) Vatman.this
						.findViewById(R.id.autocomplete_stop);
				sInputManager.hideSoftInputFromWindow(atv.getWindowToken(), 0);
				String selected = "" + atv.getText();
				String[] parts = selected.split(" ");
				final int code = Integer.valueOf(parts[0]);
				mCurrentFavourite = mDb.getFavourite(code, mLocation);
				updateSchedule(code);
			}
		});

		mViewModeIcon = (ImageView) findViewById(R.id.icon_view_mode);
		mViewModeIcon.setOnClickListener(mViewModeIconOnClick);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mDisplay = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		mPermitLocationUpdates = true;
		setOrientationCompensation();
		startUpdates();
	}

	private Intent getIntent(int idx) {
		if (mIntents.indexOfKey(idx) < 0) {
			Intent newIntent = null;
			switch (idx) {
			case INTENT_ABOUT:
				newIntent = new Intent(this, VatmanAbout.class);
				break;

			case INTENT_LOGIN:
				newIntent = new Intent(this, VatmanLogin.class);
				break;

			case INTENT_MAP:
				newIntent = new Intent(this, VatmanMap.class);
				break;

			case INTENT_PREFERENCES:
				newIntent = new Intent(this, VatmanPreferences.class);
				newIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
						PrefsFragment.class.getName());
				break;
			}
			if (newIntent != null) {
				mIntents.put(idx, newIntent);
			}
		}
		return mIntents.get(idx);
	}

	protected void startUpdates() {

		if (mPermitLocationUpdates
				&& mPreferences
						.getBoolean(VatmanPreferences.KEY_PREF_GPS, true)) {
			enableLocationUpdates();
			if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_ORIENTATION,
					true)) {
				enableOrientationUpdates();
			}
		} else {
			if (!mPreferences.getBoolean(VatmanPreferences.KEY_PREF_GPS, true)) {
				disableLocationUpdates();
				mLocation = null;
				mLocationBoundingBox = null;
				mFavouritesAdapter.setData(getStops());
			}
			disableOrientationUpdates();
		}
	}

	protected void stopUpdates() {
		disableLocationUpdates();
		disableOrientationUpdates();
	}

	protected void onPause() {
		stopUpdates();
		stopScheduleUpdater();
		super.onPause();
	}

	protected void onResume() {
		super.onResume();
		startUpdates();
		setViewModeIcon(mFavouritesAdapter.getViewMode());
		if (mMainView.getAdapter() instanceof ScheduleAdapter) {
			mScheduleAdapter.updateTimes(mMainView);
			startScheduleUpdater();
		}
	}

	private void enableLocationUpdates() {
		List<String> providers = mLocationManager.getProviders(true);
		if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 60000, 5, this);
		}
		if (providers.contains(LocationManager.GPS_PROVIDER)) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 60000, 5, this);
		}
	}

	private void disableLocationUpdates() {
		mLocationManager.removeUpdates(this);
	}

	private void enableOrientationUpdates() {
		mHeadingInitialized = false;
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, mMagnetometer,
				SensorManager.SENSOR_DELAY_UI);

	}

	private void disableOrientationUpdates() {
		mSensorManager.unregisterListener(this);
	}

	private InputStream getStopsInputStream() {
		return getResources().openRawResource(R.raw.stops);
	}

	private void setOrientationCompensation() {
		int orientation = mDisplay.getRotation();
		switch (orientation) {
		case Surface.ROTATION_0:
			sOrientationCompensation = 0;
			break;
		case Surface.ROTATION_180:
			sOrientationCompensation = 180;
			break;
		case Surface.ROTATION_270:
			sOrientationCompensation = -90;
			break;
		case Surface.ROTATION_90:
			sOrientationCompensation = 90;
			break;
		}
	}

	private void saveVersion(long version) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putLong(VatmanPreferences.KEY_PREF_STOP_VERSION, version);
		editor.commit();
		mDbVersion = version;
	}

	private void saveTableVersion(String key, int version) {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(key, version);
		editor.commit();
	}

	private void savePreferences() {
		SharedPreferences.Editor editor = mPreferences.edit();
		if (mUsername == null) {
			editor.remove(VatmanPreferences.KEY_PREF_USERNAME);
		} else {
			editor.putString(VatmanPreferences.KEY_PREF_USERNAME, mUsername);
		}
		if (mPassword == null) {
			editor.remove(VatmanPreferences.KEY_PREF_PASSWORD);
		} else {
			editor.putString(VatmanPreferences.KEY_PREF_PASSWORD, mPassword);
		}
		editor.commit();
	}

	private void setViewModeIcon(int viewMode) {
		Drawable icon = null;
		switch (viewMode) {
		case FavouritesAdapter.MODE_ROUTES:
			icon = getResources().getDrawable(R.drawable.stop);
			break;
		case FavouritesAdapter.MODE_LINES:
			icon = getResources().getDrawable(R.drawable.bus_mode);
			break;
		case FavouritesAdapter.MODE_LINES_ROUTES:
			icon = getResources().getDrawable(R.drawable.combined_mode);
			break;
		}
		if (mViewModeIcon != null && icon != null) {
			mViewModeIcon.setImageDrawable(icon);
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_LOGIN:
			if (resultCode == Activity.RESULT_OK) {
				Bundle results = data.getExtras();
				String username = results
						.getString(VatmanPreferences.KEY_PREF_USERNAME);
				String password = results
						.getString(VatmanPreferences.KEY_PREF_PASSWORD);
				boolean persist = results.getBoolean(VatmanLogin.ARG_PERSIST);
				mUsername = username;
				mPassword = password;
				if (persist) {
					savePreferences();
				}

				mFetcher.setCredentials(mUsername, mPassword);
				doUpdateSchedule();
			}
			break;
		case PREFERENCES_SAVED:
			onPrefencesUpdate();
			break;

		case MAP_RESULT:
			if (resultCode == Activity.RESULT_OK) {

				if (data != null) {
					Bundle bundle = data.getExtras();
					if (bundle != null) {
						mCurrentFavourite = bundle
								.getParcelable(VatmanMap.BUNDLE_CURRENT_STOP);
					}
				}
				if (mCurrentFavourite != null) {
					AutoCompleteTextView tv = (AutoCompleteTextView) findViewById(R.id.autocomplete_stop);
					tv.setThreshold(99999);
					tv.setText(mCurrentFavourite.getName());
					tv.setThreshold(2);
					updateSchedule(mCurrentFavourite.getCode());
				}
			}
			break;
		}
	}

	private void onPrefencesUpdate() {
		int viewMode = Integer.valueOf(mPreferences.getString(
				VatmanPreferences.KEY_PREF_VIEW_MODE, "1"));
		mFavouritesAdapter.switchViewMode(viewMode, mMainView);
		if (!mPreferences.getBoolean(VatmanPreferences.KEY_PREF_ORIENTATION,
				true)) {
			mFavouritesAdapter.hideOrientation(mMainView);
			disableOrientationUpdates();
		}
		if (!mPreferences.getBoolean(VatmanPreferences.KEY_PREF_GPS, true)) {
			disableLocationUpdates();
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		mRefreshAction = menu.findItem(R.id.action_refresh);
		mFavouriteActionOn = menu.findItem(R.id.action_favourite_on);
		mFavouriteActionOff = menu.findItem(R.id.action_favourite_off);
		mFilterAction = menu.findItem(R.id.action_filter_on);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mMainView.getAdapter() instanceof ScheduleAdapter) {
				resetSchedule();
				return true;
			}

		case R.id.options_quit:
			if (mProgressIndef != null) {
				mProgressIndef.dismiss();
			}
			if (mProgress != null) {
				mProgress.dismiss();
			}
			mDb.close();
			finish();
			return true;

		case R.id.options_about:
			startActivity(getIntent(INTENT_ABOUT));
			return true;

		case R.id.options_preferences:
			startActivityForResult(getIntent(INTENT_PREFERENCES),
					PREFERENCES_SAVED);
			return true;

		case R.id.options_map_mode:
			getIntent(INTENT_MAP).putExtra(VatmanMap.BUNDLE_CURRENT_STOP,
					mCurrentFavourite);
			startActivityForResult(getIntent(INTENT_MAP), MAP_RESULT);
			return true;

		case R.id.action_refresh:
			if (mMainView.getAdapter() instanceof ScheduleAdapter) {
				updateSchedule(mCurrentStop);
			}
			return true;

		case R.id.action_favourite_on:
			mDb.removeFavourite(mCurrentStop);
			showFavouriteAction(false);
			return true;

		case R.id.action_favourite_off:
			mDb.addFavouriteStop(mCurrentStop);
			showFavouriteAction(true);
			return true;
			
		case R.id.action_filter_on:
			mFilterAction.setVisible(false);
			mScheduleAdapter.unFilter();
			return true;
			
		}
		return false;
	}

	private OnClickListener mViewModeIconOnClick = new OnClickListener() {
		public void onClick(View v) {
			int viewMode = mFavouritesAdapter.getViewMode();
			switch (viewMode) {
			case FavouritesAdapter.MODE_LINES:
				viewMode = FavouritesAdapter.MODE_LINES_ROUTES;
				break;
			case FavouritesAdapter.MODE_ROUTES:
				viewMode = FavouritesAdapter.MODE_LINES;
				break;
			case FavouritesAdapter.MODE_LINES_ROUTES:
				viewMode = FavouritesAdapter.MODE_ROUTES;
				break;
			}
			if (mMainView.getAdapter() instanceof FavouritesAdapter) {
				setViewModeIcon(viewMode);
				mFavouritesAdapter.switchViewMode(viewMode, mMainView);
				SharedPreferences.Editor editor = mPreferences.edit();
				editor.putString(VatmanPreferences.KEY_PREF_VIEW_MODE, ""
						+ viewMode);
				editor.commit();
			}
		}
	};

	private void resetSchedule() {
		mCurrentStop = 0;
		mCurrentFavourite = null;
		mAutocompleteText.setText("");
		mAutocompleteText.dismissDropDown();
		if (mMainView.getAdapter() instanceof ScheduleAdapter) {
			stopScheduleUpdater();
			onFavouritesShow();
			mMainView.setAdapter(mFavouritesAdapter);
			mRefreshAction.setVisible(false);
			getActionBar().setDisplayHomeAsUpEnabled(false);
			getActionBar().setHomeButtonEnabled(false);
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mMainView.getAdapter() instanceof ScheduleAdapter) {
				resetSchedule();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void execFetcher() {
		if (mFetcher == null) {
			mFetcher = new Fetcher(
					URL_GENERATOR,
					getVersion(),
					mHandler,
					new PhoneInfo(
							(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)),
					null, null);
		}
		if (AUTORIZATION_REQUIRED) {
			if (mUsername == null || mPassword == null) {
				startActivityForResult(getIntent(INTENT_LOGIN), REQUEST_LOGIN);
			} else {
				mFetcher.setCredentials(mUsername, mPassword);
				doUpdateSchedule();
			}
		} else {
			doUpdateSchedule();
		}
	}

	private void doUpdateSchedule() {
		mProgressIndef.setMessage(getString(R.string.download));
		mProgressIndef.show();
		Thread t = new Thread() {
			public void run() {
				mSchedules = new ArrayList<ScheduleItem>();
				NetworkInfo activeNetwork = sConnection.getActiveNetworkInfo();
				if (activeNetwork != null
						&& activeNetwork.getState() == NetworkInfo.State.CONNECTED) {
					try {
						mSchedules = mFetcher.fetch(mCurrentStop, mDbVersion);
						if (mSchedules.size() < 1) {
							mHandler.sendEmptyMessage(VatmanException.SCHEDULE_IS_EMPTY);
						}
					} catch (ParserConfigurationException e) {
						mHandler.sendEmptyMessage(VatmanException.INVALID_XML_RESPONSE);
						e.printStackTrace();
					} catch (SAXException e) {
						mHandler.sendEmptyMessage(VatmanException.INVALID_XML_RESPONSE);
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						mHandler.sendEmptyMessage(VatmanException.SERVICE_NOT_AVAILABLE);
						e.printStackTrace();
					} catch (IOException e) {
						mHandler.sendEmptyMessage(VatmanException.CONNECTION_FAILED);
						e.printStackTrace();
					} catch (VatmanException e) {
						mHandler.sendEmptyMessage(e.getErrNo());
					}
				} else {
					mHandler.sendEmptyMessage(VatmanException.CONNECTION_FAILED);
				}
				mHandler.sendEmptyMessage(OPERATION_UPDATE_SCHEDULE);
			}
		};
		t.start();
	}

	public void updateSchedule(final int stop) {
		mCurrentStop = stop;
		if (stop < 1) {
			mScheduleAdapter.setData(null);
		} else {
			execFetcher();
		}
		getActionBar().setDisplayHomeAsUpEnabled(true);
		mRefreshAction.setVisible(true);
	}

	private InitialStopsProvider getInitialStopsProvider() {
		InitialStopsProvider initialStopsProvider = null;
		try {
			initialStopsProvider = new InitialStopsProvider(mHandler,
					getStopsInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return initialStopsProvider;
	}

	private void initDbData(int count) {
		if (mProgress == null) {
			mProgress = new ProgressDialog(Vatman.this);
			mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgress.setCancelable(false);
			mProgress.setIndeterminate(false);
			mProgress.setTitle(getString(R.string.initialize));

		}
		mProgress.setProgress(0);
		mProgress.setMax(count);
		mPermitLocationUpdates = false;
		mProgress.show();
		mDb.init();
		Thread t = new Thread() {
			public void run() {
				mProgressValue = 0;
				try {
					InitialStopsProvider provider = getInitialStopsProvider();
					provider.parse();
					provider = null;
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	private ArrayList<Favourite> getStops() {
		mFavourites = mStopsProvider.getStops(mLocation, mLocationBoundingBox);
		return mFavourites;
	}

	private FavouritesAdapter getFavouritesAdapter() {
		return new FavouritesAdapter(this, R.layout.favourite_item, getStops(), mHandler);
	}

	private class VatmanCallback implements Callback {

		private long currentVersion;

		public boolean handleMessage(Message msg) {
			int msgId = 0;
			String message = null;
			boolean toastIt = true;
			switch (msg.what) {
			case VatmanException.INVALID_RESPONSE:
				msgId = R.string.invalid_response;
				break;

			case VatmanException.SCHEDULE_IS_EMPTY:
				msgId = R.string.schedule_is_empty;
				break;

			case VatmanException.CONNECTION_FAILED:
				msgId = R.string.connection_failed;
				break;

			case VatmanException.INVALID_XML_RESPONSE:
				msgId = R.string.invalid_xml_response;
				break;

			case VatmanException.SERVICE_NOT_AVAILABLE:
				msgId = R.string.service_not_available;
				break;

			case VatmanException.ACCESS_DENIED:
				mUsername = null;
				mPassword = null;
				savePreferences();
				msgId = R.string.access_denied;
				break;

			case VatmanException.SERVER_MESSAGE:
				message = mFetcher.getErrorMessage();
				break;

			case OPERATION_ADD_FAVOURITE:
			case OPERATION_REMOVE_FAVOURITE:
				mFavouritesAdapter.setData(getStops());
				toastIt = false;
				Toast.makeText(Vatman.this,
						msg.getData().getString(BUNDLE_ARG_MESSAGE),
						Toast.LENGTH_SHORT).show();
				break;

			case OPERATION_NEW_UPDATE:
				toastIt = false;
				final long newVersion = Long.valueOf(msg.getData().getString(
						BUNDLE_ARG_MESSAGE));
				final int[] updatedStops = msg.getData().getIntArray(
						BUNDLE_ARG_UPDATED_STOPS);
				mFavouritesAdapter.onNewUpdates(updatedStops, mLocation);
				saveVersion(newVersion);
				break;

			case OPERATION_INIT_STOPS_VERSION_INFO:
				toastIt = false;
				currentVersion = msg.getData().getLong(BUNDLE_ARG_STOP_VERSION);
				break;

			case OPERATION_INIT_STOPS:
				toastIt = false;
				mProgressValue++;
				mProgress.setProgress(mProgressValue);
				if (mProgressValue >= mProgress.getMax()) {
					mFavouritesAdapter = getFavouritesAdapter();
					mMainView.setAdapter(mFavouritesAdapter);
					saveVersion(currentVersion);
					mProgress.hide();
					mPermitLocationUpdates = true;
					enableLocationUpdates();
				}
				break;

			case OPERATION_UPDATE_SCHEDULE:
				toastIt = false;
				if (!(mMainView.getAdapter() instanceof ScheduleAdapter)) {
					mMainView.setAdapter(mScheduleAdapter);
				}
				mScheduleAdapter.setData(mSchedules);
				onScheduleShow();
				mProgressIndef.hide();
				startScheduleUpdater();
				break;

			case OPERATION_REMOTE_UPDATE:
				return false;
				
			case OPERATION_FILTERED:
				mFilterAction.setVisible(true);
				return false;

			}
			if (toastIt) {
				String toastMessage = (message == null) ? getString(msgId)
						: message;
				Toast.makeText(Vatman.this, toastMessage, Toast.LENGTH_SHORT)
						.show();
			}
			return false;
		}
	}

	private void showFavouriteAction(boolean isOn) {
		if (isOn) {
			mFavouriteActionOn.setVisible(true);
			mFavouriteActionOff.setVisible(false);
		} else {
			mFavouriteActionOn.setVisible(false);
			mFavouriteActionOff.setVisible(true);
		}
	}

	private void onScheduleShow() {
		mSearchContainer.setVisibility(View.GONE);
		showFavouriteAction(mCurrentFavourite.isFavourite());
		getActionBar().setSubtitle(mCurrentFavourite.getName());
		getWindow().setUiOptions(
				ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW, 0);
	}

	private void onFavouritesShow() {
		mFavouriteActionOn.setVisible(false);
		mFavouriteActionOff.setVisible(false);
		mFilterAction.setVisible(false);
		mSearchContainer.setVisibility(View.VISIBLE);
		getActionBar().setSubtitle(null);
	}

	private void startScheduleUpdater() {
		mScheduleHandler.removeCallbacks(mScheduleUpdateTask);
		mScheduleHandler.postDelayed(mScheduleUpdateTask,
				SCHEDULE_UPDATE_INTERVAL);
	}

	private void stopScheduleUpdater() {
		mScheduleHandler.removeCallbacks(mScheduleUpdateTask);
	}

	private Runnable mScheduleUpdateTask = new Runnable() {
		public void run() {
			mScheduleAdapter.updateTimes(mMainView);
			mScheduleHandler.postDelayed(this, SCHEDULE_UPDATE_INTERVAL);
		}
	};

	public void onLocationChanged(Location location) {
		mLocation = new VatmanLocation(location);
		mLocationBoundingBox = GeoUtil.getBoundingBox(location, 500);
		mFavouritesAdapter.setData(getStops());
	}

	public void onProviderDisabled(String provider) {

	}

	public void onProviderEnabled(String provider) {

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				double heading = Math.toDegrees(orientation[0]);
				if ((!mHeadingInitialized || Math.abs(heading - sHeading) > 1)
						&& mFavourites.size() > 0) {
					mHeadingInitialized = true;
					sHeading = heading;
					mFavouritesAdapter.updateDirections(mMainView);
				}
			}
		}
	}

	private String getVersion() {
		PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(),
					PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {

		}
		return pInfo.versionName + "b" + pInfo.versionCode;
	}

	public static double getHeading() {
		return sHeading + sOrientationCompensation;
	}

}