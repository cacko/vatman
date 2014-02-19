package com.mutanti.vatman.Adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mutanti.vatman.Object.Favourite;
import com.mutanti.vatman.Object.VatmanLocation;
import com.mutanti.vatman.Providers.Db;
import com.mutanti.vatman.R;
import com.mutanti.vatman.VatmanPreferences;

import java.util.ArrayList;

public class FavouritesAdapter extends ArrayAdapter<Favourite> {

    public static final int MODE_LINES = 2;
    public static final int MODE_ROUTES = 1;
    public static final int MODE_LINES_ROUTES = 3;
    private ArrayList<Favourite> mItems;
    private Context mContext;
    private SharedPreferences mPreferences;
    private int mViewMode;

    public FavouritesAdapter(Context context, int textViewResourceId,
                             ArrayList<Favourite> objects) {
        super(context, textViewResourceId, objects);
        mItems = objects;
        mContext = context;

        mPreferences = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        mViewMode = Integer.valueOf(mPreferences.getString(
                VatmanPreferences.KEY_PREF_VIEW_MODE, "1"));
    }

    @Override
    public int getCount() {
        return mItems.size();

    }

    public int getViewMode() {
        return mViewMode;
    }

    public Favourite get(int position) {
        return mItems.get(position);
    }

    public void setData(ArrayList<Favourite> data) {
        if (data == null) {
            clear();
            mItems = new ArrayList<Favourite>();
        } else {
            mItems = data;
        }
        notifyDataSetChanged();
    }

    private void updateDirection(Favourite o, ImageView favouriteDirection) {
        if (mPreferences.getBoolean(VatmanPreferences.KEY_PREF_ORIENTATION,
                true)) {
            final float oldDirection = o.getDirection();
            o.updateDirection();
            final float direction = o.getDirection();
            if (oldDirection != direction) {

                float fromAngle = oldDirection;
                float toAngle = direction;

                if (Math.abs(oldDirection - direction) > 180) {
                    if (fromAngle > toAngle) {
                        fromAngle -= 360;
                    } else {
                        toAngle -= 360;
                    }
                }

                Animation a = new RotateAnimation(fromAngle, toAngle,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                a.setRepeatCount(-1);
                a.setDuration(500);
                favouriteDirection.startAnimation(a);
            }
        }
    }

    public void updateDirections(ListView list) {
        int size = list.getChildCount();
        if (size < 1)
            return;
        for (int idx = 0; idx < size; idx++) {
            View v = (View) list.getChildAt(idx);
            ImageView favouriteDirection = (ImageView) v
                    .findViewById(R.id.favourite_direction);
            if (favouriteDirection != null) {
                updateDirection(mItems.get(idx), favouriteDirection);
            }
        }
    }

    public void hideOrientation(ListView list) {
        int size = list.getChildCount();
        if (size < 1)
            return;
        for (int idx = 0; idx < size; idx++) {
            View v = (View) list.getChildAt(idx);
            hideView(v.findViewById(R.id.favourite_direction_container));
        }
    }

    private void hideView(View view) {
        if (view != null && view.getVisibility() != View.GONE) {
            view.setVisibility(View.GONE);
        }
    }

    private void showView(View view) {
        if (view != null && view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void switchViewMode(int mode, ListView list) {
        switch (mode) {
            case MODE_LINES:
                switchToLinesMode(list);
                break;
            case MODE_ROUTES:
                switchToRoutesMode(list);
                break;
            case MODE_LINES_ROUTES:
                switchToCombinedMode(list);
                break;
        }
    }

    private void switchToCombinedMode(ListView list) {
        if (mViewMode == MODE_LINES_ROUTES) {
            return;
        }
        mViewMode = MODE_LINES_ROUTES;
        setDescriptions(list);
    }

    private void switchToLinesMode(ListView list) {
        if (mViewMode == MODE_LINES) {
            return;
        }
        mViewMode = MODE_LINES;
        setDescriptions(list);
    }

    private void switchToRoutesMode(ListView list) {
        if (mViewMode == MODE_ROUTES) {
            return;
        }
        mViewMode = MODE_ROUTES;
        setDescriptions(list);
    }

    private void setDescriptions(ListView list) {
        int size = list.getChildCount();
        if (size < 1)
            return;
        for (int idx = 0; idx < size; idx++) {
            View v = list.getChildAt(idx);
            mItems.get(idx).setDescription(
                    (TextView) v.findViewById(R.id.favourite_description),
                    mViewMode);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.favourite_item, null);
        }
        final Favourite o = mItems.get(position);
        if (o != null) {
            TextView favouriteStop = (TextView) v
                    .findViewById(R.id.favourite_stop);
            if (favouriteStop != null) {
                favouriteStop.setText(o.getName());
            }
            TextView favouriteDescription = (TextView) v
                    .findViewById(R.id.favourite_description);
            TextView favouriteDistance = (TextView) v
                    .findViewById(R.id.favourite_distance);
            ImageView favouriteDirection = (ImageView) v
                    .findViewById(R.id.favourite_direction);
            o.setDescription(favouriteDescription, mViewMode);

            final String distance = o.getDistanceString();
            if (distance == null
                    || !mPreferences.getBoolean(
                    VatmanPreferences.KEY_PREF_ORIENTATION, true)) {
                hideView(v.findViewById(R.id.favourite_direction_container));
            } else {
                favouriteDistance.setText(distance);
                showView(v.findViewById(R.id.favourite_direction_container));
                updateDirection(o, favouriteDirection);
            }

            ImageView favouriteStar = (ImageView) v
                    .findViewById(R.id.favourite_star);
            if (o.isFavourite()) {
                favouriteStar.setImageDrawable(mContext.getResources()
                        .getDrawable(android.R.drawable.btn_star_big_on));
            } else {
                favouriteStar.setImageDrawable(mContext.getResources()
                        .getDrawable(android.R.drawable.btn_star_big_off));
            }
            favouriteStar.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Db db = Db.getInstance();
                    View parent = (View) v.getParent();
                    TextView stopView = (TextView) parent
                            .findViewById(R.id.favourite_stop);
                    String stopName = (String) stopView.getText();
                    int stop = Integer.valueOf(stopName.split(" ")[0]);
                    if (db.isFavourite(stop)) {
                        db.removeFavourite(stop);
                    } else {
                        db.addFavouriteStop(stop);
                    }
                }
            });
        }
        return v;
    }

    public void onNewUpdates(int[] updatedStops, VatmanLocation location) {
        if (updatedStops == null || updatedStops.length == 0) {
            return;
        }
        ArrayList<Integer> forUpdate = new ArrayList<Integer>();
        for (int code : updatedStops) {
            forUpdate.add(code);
        }
        Db db = Db.getInstance();
        boolean updateDataset = false;
        for (int idx = 0; idx < mItems.size(); idx++) {
            Favourite item = mItems.get(idx);
            int code = item.getCode();
            if (forUpdate.contains(code)) {
                item = db.getFavourite(code, location);
                mItems.set(idx, item);
                updateDataset = true;
            }
        }
        if (updateDataset) {
            notifyDataSetChanged();
        }
    }
}
