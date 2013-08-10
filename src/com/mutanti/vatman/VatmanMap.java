package com.mutanti.vatman;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Projection;
import com.mutanti.vatman.Object.Favourite;
import com.mutanti.vatman.Object.FavouriteStop;
import com.mutanti.vatman.Object.VatmanLocation;
import com.mutanti.vatman.Providers.Db;
import com.mutanti.vatman.Providers.StopsProvider;
import com.mutanti.vatman.util.GeoUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayGestureDetector;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.ZoomEvent;
import de.android1.overlaymanager.lazyload.LazyLoadCallback;
import de.android1.overlaymanager.lazyload.LazyLoadException;
import de.android1.overlaymanager.lazyload.LazyLoadListener;

public class VatmanMap extends MapActivity {

    static final String DEBUG_HOME_SIG = "94:-116:-18:42:70:-24:-127:74:-80:-24:-71:124:2:-82:57:-71:94:18:42:-120:";
    static final String DEBUG_WORK_SIG = "82:0:-89:-18:-82:83:99:110:-107:97:-94:18:78:67:24:-100:19:120:84:71:";
    static final String DEBUG_HOME_KEY = "0rspDmvFQWrIdHCPeQx2qSTqfdMsGEsrbDfF8Yw";
    static final String DEBUG_WORK_KEY = "0aHo-hqqAuOnpRD58RcSCYn9yGgaqjKa7R6menw";
    static final String RELEASE_KEY = "0rspDmvFQWrIvo5Y5op0VGCxNKH45EhkuFE-2kQ";
    static final String BUNDLE_CURRENT_STOP = "current_stop";
    static final int OPERATION_REPOSITION = 1;
    static final int OPERATION_MY_LOCATION_BUTTON = 2;
    static final int DEFAULT_ZOOM_LEVEL = 18;
    static final int DEFAULT_MIN_ZOOM_LEVEL = 17;
    static final VatmanLocation SOFIA = new VatmanLocation(42.697738, 23.322296);
    static String sMapsApiKey = null;
    private static SparseArray<Favourite> mStops = new SparseArray<Favourite>();
    final Handler mHandler = new Handler(new VatmanMapCallback());
    private MapView mMapView;
    private MyLocationOverlay mMyLocationOverlay;
    private StopsProvider mStopsProvider;
    private SharedPreferences mPreferences;
    private Drawable mMarker;
    private OverlayManager mOverlayManager;
    private VatmanLocation mPositionTo;
    private ManagedOverlay mManagedOverlay;
    private FrameLayout mMainLayout;
    private View mPopupView;
    private TextView mPopupText;
    private ImageView mButtonBack;
    private ImageView mButtonNext;
    private LinearLayout mMyLocationLayout;
    private ImageView mButtonMyLocation;
    private boolean mShowMarkers = true;
    private int mZoomLevel;
    private FavouriteStop mCurrentStop;
    private ManagedOverlayItem mFocusedItem;
    private OnClickListener mBackListener = new OnClickListener() {
        public void onClick(View v) {
            int allItemsCount = mManagedOverlay.size();
            if (allItemsCount > 1) {
                int lastIdx = mManagedOverlay.getLastFocusedIndex();
                int newIdx = (lastIdx == -1) ? 0 : lastIdx - 1;
                if (newIdx < 0) {
                    newIdx = allItemsCount - 1;
                }
                focusItem(newIdx);
            }
        }
    };
    private OnClickListener mNextListener = new OnClickListener() {
        public void onClick(View v) {
            int allItemsCount = mManagedOverlay.size();
            if (allItemsCount > 1) {
                int lastIdx = mManagedOverlay.getLastFocusedIndex();
                int newIdx = (lastIdx == -1) ? 0 : lastIdx + 1;
                if (!(allItemsCount > newIdx)) {
                    newIdx = 0;
                }
                focusItem(newIdx);
            }
        }
    };
    private OnClickListener mMyLocationListener = new OnClickListener() {
        public void onClick(View v) {
            GeoPoint myLocation = mMyLocationOverlay.getMyLocation();
            if (myLocation != null
                    && !(mMapView.getMapCenter().equals(myLocation))) {
                mMapView.getController().animateTo(myLocation, new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(VatmanMap.OPERATION_REPOSITION);
                    }
                });
            }
        }
    };

    public static String getMapsApiKey(Context context) {
        if (sMapsApiKey == null) {
            try {
                sMapsApiKey = RELEASE_KEY;
                Signature[] sigs = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(),
                                PackageManager.GET_SIGNATURES).signatures;
                for (int i = 0; i < sigs.length; i++) {
                    MessageDigest sha = MessageDigest.getInstance("SHA-1");
                    sha.update(sigs[i].toByteArray());
                    byte[] digest = sha.digest();
                    String str = "";
                    for (int di = 0; di < digest.length; di++) {
                        str += Byte.toString(digest[di]) + ":";
                    }
                    if (str.equals(DEBUG_WORK_SIG)) {
                        sMapsApiKey = DEBUG_WORK_KEY;
                        break;
                    } else if (str.equals(DEBUG_HOME_SIG)) {
                        sMapsApiKey = DEBUG_HOME_KEY;
                        break;
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return sMapsApiKey;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        mMarker = getResources().getDrawable(R.drawable.map_marker);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mCurrentStop = bundle.getParcelable(BUNDLE_CURRENT_STOP);
        }
        if (mCurrentStop != null) {
            mPositionTo = new VatmanLocation(mCurrentStop.getGeoPoint());
        }
        mPreferences = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        Db db = Db.getInstance();
        mStopsProvider = StopsProvider.getInstance(db);
        mZoomLevel = getLastZoomLevel();

        // do layout
        mMainLayout = new FrameLayout(this);
        mMapView = new MapView(this, getMapsApiKey(this));
        mMainLayout.addView(mMapView);
        setContentView(mMainLayout);

        mMapView.setBuiltInZoomControls(true);
        mMapView.getController().setZoom(mZoomLevel);
        mMapView.setClickable(true);
        mMapView.setEnabled(true);

        // do arrows layout
        LinearLayout mArrowLayout = new LinearLayout(this);
        mArrowLayout.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        mArrowLayout.setPadding(5, 5, 5, 5);
        View mArrowView = getLayoutInflater().inflate(R.layout.map_arrows,
                mArrowLayout, true);
        mButtonBack = (ImageView) mArrowView.findViewById(R.id.map_button_back);
        mButtonNext = (ImageView) mArrowView.findViewById(R.id.map_button_next);
        mButtonBack.setEnabled(false);
        mButtonNext.setEnabled(false);
        mButtonBack.setOnClickListener(mBackListener);
        mButtonNext.setOnClickListener(mNextListener);
        mMainLayout.addView(mArrowLayout);

        // do my location button
        mMyLocationLayout = new LinearLayout(this);
        mMyLocationLayout.setGravity(Gravity.RIGHT);
        mMyLocationLayout.setPadding(5, 5, 5, 5);
        View myLocationView = getLayoutInflater().inflate(
                R.layout.map_my_location, mMyLocationLayout, true);
        mButtonMyLocation = (ImageView) myLocationView
                .findViewById(R.id.map_button_my_location);
        mButtonMyLocation.setOnClickListener(mMyLocationListener);

        // init popup view
        mPopupView = getLayoutInflater().inflate(R.layout.map_popup, mMapView,
                false);
        mPopupText = (TextView) mPopupView.findViewById(R.id.map_popup_text);
        mPopupView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                goToSchedule();
            }
        });

        // overlay manager stuff
        mOverlayManager = new OverlayManager(getApplication(), mMapView);
        mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
        mManagedOverlay = mOverlayManager.createOverlay("lazyloadOverlay",
                mMarker);
        mManagedOverlay
                .setOnOverlayGestureListener(new ManagedOverlayGestureDetector.OnOverlayGestureListener() {

                    public boolean onDoubleTap(MotionEvent arg0,
                                               ManagedOverlay arg1, GeoPoint arg2,
                                               ManagedOverlayItem arg3) {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    public void onLongPress(MotionEvent arg0,
                                            ManagedOverlay arg1) {
                        // TODO Auto-generated method stub

                    }

                    public void onLongPressFinished(MotionEvent arg0,
                                                    ManagedOverlay arg1, GeoPoint arg2,
                                                    ManagedOverlayItem arg3) {
                        // TODO Auto-generated method stub

                    }

                    public boolean onScrolled(MotionEvent arg0,
                                              MotionEvent arg1, float arg2, float arg3,
                                              ManagedOverlay arg4) {
                        mHandler.sendEmptyMessage(VatmanMap.OPERATION_MY_LOCATION_BUTTON);
                        return true;
                    }

                    public boolean onSingleTap(MotionEvent arg0,
                                               ManagedOverlay arg1, GeoPoint arg2,
                                               ManagedOverlayItem item) {
                        if (item != null) {
                            mFocusedItem = item;
                            mCurrentStop = (FavouriteStop) mStops.get(item
                                    .hashCode());
                            showPopupFor(mCurrentStop);
                            return true;
                        } else {
                            if (mFocusedItem != null) {
                                mManagedOverlay.setFocus(mFocusedItem);
                                return true;
                            }
                        }
                        return false;
                    }

                    public boolean onZoom(ZoomEvent event, ManagedOverlay arg1) {
                        int zoomLevel = event.getZoomLevel();
                        mShowMarkers = (zoomLevel >= DEFAULT_ZOOM_LEVEL);
                        VatmanMap.this.mZoomLevel = zoomLevel;
                        return true;
                    }
                });
        createOverlayWithLazyLoading();
        initPosition();
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }

    private boolean isMyLocationOnScreen() {
        GeoPoint myLocation = mMyLocationOverlay.getMyLocation();
        if (myLocation != null) {
            Projection projection = mMapView.getProjection();
            Point myLocationPoint = projection.toPixels(myLocation, null);
            if (myLocationPoint.x < 0 || myLocationPoint.y < 0
                    || myLocationPoint.x > mMapView.getWidth()
                    || myLocationPoint.y > mMapView.getHeight()) {
                return false;
            }
        }
        return true;
    }

    private void updateMyLocationButtonVisibility() {
        mButtonMyLocation
                .setVisibility((isMyLocationOnScreen()) ? View.INVISIBLE
                        : View.VISIBLE);
    }

    private void focusItem(int idx) {
        ManagedOverlayItem item = mManagedOverlay.getItem(idx);
        if (item != null) {
            int itemHash = item.hashCode();
            if (mStops.indexOfKey(itemHash) > -1) {
                mCurrentStop = (FavouriteStop) mStops.get(itemHash);
                mFocusedItem = item;
                mManagedOverlay.setFocus(item);
                showPopupFor(mCurrentStop);
            }
        }
    }

    private void showPopupFor(FavouriteStop stop) {
        mMapView.removeView(mPopupView);
        MapView.LayoutParams mapParams = new MapView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, stop.getGeoPoint(), 0,
                -(mMarker.getMinimumHeight() / 2) - 5,
                MapView.LayoutParams.BOTTOM_CENTER);
        mPopupText.setText(stop.getName());
        mMapView.addView(mPopupView, mapParams);
    }

    private int getLastZoomLevel() {
        mZoomLevel = DEFAULT_ZOOM_LEVEL;
        if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_SAVE_ZOOM_LEVEL,
                false)) {
            mZoomLevel = mPreferences.getInt(
                    VatmanPreferences.KEY_PREF_LAST_ZOOM_LEVEL,
                    DEFAULT_ZOOM_LEVEL);
        }
        mShowMarkers = (mZoomLevel >= DEFAULT_MIN_ZOOM_LEVEL);
        return mZoomLevel;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdates();
        final Handler mHandlerResume = new Handler();
        mHandlerResume.postDelayed(new Runnable() {
            public void run() {
                double[] boundingBox = GeoUtil.getBoundingBox(mPositionTo,
                        mMapView.getLatitudeSpan(), mMapView.getLongitudeSpan());
                if (boundingBox == null) {
                    mHandlerResume.postDelayed(this, 100);
                } else {
                    mHandler.sendEmptyMessage(VatmanMap.OPERATION_REPOSITION);
                }
            }
        }, 100);
        addItems(new VatmanLocation(mMapView.getMapCenter()));
    }

    @Override
    protected void onPause() {
        if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_SAVE_ZOOM_LEVEL,
                false)) {
            Editor editor = mPreferences.edit();
            editor.putInt(VatmanPreferences.KEY_PREF_LAST_ZOOM_LEVEL,
                    mZoomLevel);
            editor.commit();
        }
        stopUpdates();
        super.onPause();
    }

    private void goToSchedule() {
        if (mCurrentStop != null) {
            Intent intent = new Intent();
            intent.putExtra(BUNDLE_CURRENT_STOP, mCurrentStop);
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void initPosition() {
        if (mPositionTo == null) {
            Location lastFix = mMyLocationOverlay.getLastFix();
            mPositionTo = (lastFix != null) ? new VatmanLocation(lastFix)
                    : SOFIA;
            goToPosition(mPositionTo);
            if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_GPS, true)) {
                mMyLocationOverlay.runOnFirstFix(new Runnable() {
                    public void run() {
                        mPositionTo = new VatmanLocation(mMyLocationOverlay
                                .getMyLocation());
                        goToPosition(mPositionTo);
                    }
                });
            }
        } else {
            goToPosition(mPositionTo);
            if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_GPS, true)) {
                mMyLocationOverlay.runOnFirstFix(new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(VatmanMap.OPERATION_MY_LOCATION_BUTTON);
                    }
                });
            }
        }
    }

    private void goToPosition(VatmanLocation location) {
        mMapView.getController().animateTo(location.getGeoPoint(),
                new Runnable() {
                    public void run() {
                        mHandler.sendEmptyMessage(VatmanMap.OPERATION_REPOSITION);
                    }
                });
    }

    private void setButtonsState() {
        if (mManagedOverlay.size() > 1) {
            mButtonBack.setEnabled(true);
            mButtonNext.setEnabled(true);
        } else {
            mButtonBack.setEnabled(false);
            mButtonNext.setEnabled(false);
        }
    }

    private void addItems(VatmanLocation position) {
        mManagedOverlay.addAll(getItems(GeoUtil.getBoundingBox(position,
                mMapView.getLatitudeSpan(), mMapView.getLongitudeSpan())));
        if (mFocusedItem != null) {
            mManagedOverlay.setFocus(mFocusedItem);
        }
        mOverlayManager.populate();
        setButtonsState();
    }

    private List<ManagedOverlayItem> getItems(double[] boundingBox) {
        List<ManagedOverlayItem> items = new LinkedList<ManagedOverlayItem>();
        if (mShowMarkers) {
            try {
                VatmanLocation location = null;
                try {
                    location = new VatmanLocation(
                            mMyLocationOverlay.getMyLocation());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ArrayList<Favourite> stops = mStopsProvider.getStops(location,
                        boundingBox, false);
                for (Favourite stop : stops) {
                    ManagedOverlayItem item = new ManagedOverlayItem(
                            stop.getGeoPoint(), "", "");
                    int itemHash = item.hashCode();
                    if (mCurrentStop != null
                            && stop.getCode() == mCurrentStop.getCode()) {
                        mFocusedItem = item;
                    }
                    if (mStops.indexOfKey(itemHash) < 0) {
                        mStops.put(itemHash, stop);
                    }
                    items.add(item);
                }
            } catch (Exception e) {
                try {
                    throw new LazyLoadException(e.getMessage());
                } catch (LazyLoadException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return items;
    }

    public void createOverlayWithLazyLoading() {

        mManagedOverlay.setLazyLoadCallback(new LazyLoadCallback() {
            public List<ManagedOverlayItem> lazyload(GeoPoint topLeft,
                                                     GeoPoint bottomRight, ManagedOverlay overlay)
                    throws LazyLoadException {
                mHandler.sendEmptyMessage(VatmanMap.OPERATION_MY_LOCATION_BUTTON);
                return getItems(GeoUtil.getBoundingBox(topLeft, bottomRight));
            }
        });

        mManagedOverlay.setLazyLoadListener(new LazyLoadListener() {

            public void onBegin(ManagedOverlay overlay) {
            }

            public void onSuccess(ManagedOverlay overlay) {
                mOverlayManager.populate();
                setButtonsState();
                overlay.setFocus(mFocusedItem);
            }

            public void onError(LazyLoadException exception,
                                ManagedOverlay overlay) {
            }
        });

    }

    private void enableLocationUpdates() {
        mMyLocationOverlay.enableMyLocation();
        mMapView.getOverlays().add(mMyLocationOverlay);
        mMainLayout.addView(mMyLocationLayout);
    }

    private void disableLocationUpdates() {
        mMyLocationOverlay.disableMyLocation();
        mMapView.getOverlays().remove(mMyLocationOverlay);
        mMainLayout.removeView(mMyLocationLayout);
    }

    private void enableOrientationUpdates() {
        // mMyLocationOverlay.enableCompass();
    }

    private void disableOrientationUpdates() {
        // mMyLocationOverlay.disableCompass();
    }

    protected void startUpdates() {
        if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_GPS, true)) {
            enableLocationUpdates();
            if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_ORIENTATION,
                    true)) {
                enableOrientationUpdates();
            }
        } else {
            if (!mPreferences.getBoolean(VatmanPreferences.KEY_PREF_GPS, true)) {
                disableLocationUpdates();
            }
            disableOrientationUpdates();
        }
    }

    protected void stopUpdates() {
        disableLocationUpdates();
        disableOrientationUpdates();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private class VatmanMapCallback implements Callback {
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case VatmanMap.OPERATION_MY_LOCATION_BUTTON:
                    updateMyLocationButtonVisibility();
                    break;
                case VatmanMap.OPERATION_REPOSITION:
                    updateMyLocationButtonVisibility();
                    addItems(new VatmanLocation(mMapView.getMapCenter()));
                    if (mCurrentStop != null) {
                        showPopupFor(mCurrentStop);
                    }
                    break;
            }
            return false;
        }
    }
}
