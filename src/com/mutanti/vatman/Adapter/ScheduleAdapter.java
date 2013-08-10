package com.mutanti.vatman.Adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mutanti.vatman.Object.Line;
import com.mutanti.vatman.Object.ScheduleItem;
import com.mutanti.vatman.R;
import com.mutanti.vatman.Vatman;
import com.mutanti.vatman.util.Layout;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public final class ScheduleAdapter extends ArrayAdapter<ScheduleItem> {

    private static final long NULL = -999;
    private ArrayList<ScheduleItem> mItems;
    private ArrayList<ScheduleItem> mUnFilteredItems;
    private Context mContent;
    private long m_previousElapsedTime;
    private Handler mHandler;

    public ScheduleAdapter(Context context, int textViewResourceId,
                           ArrayList<ScheduleItem> items, Handler callback) {
        super(context, textViewResourceId, items);
        mItems = items;
        mContent = context;
        m_previousElapsedTime = NULL;
        mHandler = callback;
        mUnFilteredItems = null;
    }

    @Override
    public int getCount() {
        return mItems.size();

    }

    private void updateTime(TextView tv, ScheduleItem item) {
        long elapsedTime = item.getElapsedMinutes();
        if (m_previousElapsedTime == elapsedTime) {
            elapsedTime++;
        }
        m_previousElapsedTime = elapsedTime;
        String tvText = "сега";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime);
        if (seconds > 30) {
            long minutes = (seconds < 60) ? 1 : TimeUnit.MILLISECONDS
                    .toMinutes(elapsedTime);
            long hours = TimeUnit.MILLISECONDS.toHours(elapsedTime);
            tvText = "~"
                    + ((minutes > 59) ? String.format("%d:%02d", hours, minutes
                    - (hours * 60)) : "" + minutes);
        }
        tv.setText(tvText);
    }

    public void updateTimes(ListView list) {
        boolean doUpdate = false;
        m_previousElapsedTime = NULL;
        int size = list.getChildCount();
        if (size < 1)
            return;
        for (int idx = 0; idx < size; idx++) {
            if (!(mItems.size() > idx)) {
                break;
            }
            View v = list.getChildAt(idx);
            TextView tv = (TextView) v.findViewById(R.id.schedule_item_time);
            if (tv != null) {
                ScheduleItem o = mItems.get(idx);
                long time = o.getElapsedMinutes();
                if (time < 0) {
                    mItems.remove(idx);
                    doUpdate = true;
                }
                if (!doUpdate) {
                    updateTime(tv, o);
                }
            }
        }
        if (doUpdate) {
            notifyDataSetChanged();
        }
    }

    public void setData(ArrayList<ScheduleItem> data) {
        mUnFilteredItems = null;
        if (data == null) {
            mItems = new ArrayList<ScheduleItem>();
        } else {
            mItems = data;
        }
        m_previousElapsedTime = NULL;
        notifyDataSetChanged();
    }

    @SuppressWarnings("unchecked")
    public void unFilter() {
        if (mUnFilteredItems != null) {
            mItems = (ArrayList<ScheduleItem>) mUnFilteredItems.clone();
            mUnFilteredItems = null;
            for (ScheduleItem item : mItems) {
                item.filter(null);
            }
            notifyDataSetChanged();
        }
    }

    @SuppressWarnings("unchecked")
    public void filter(Line line) {
        if (mUnFilteredItems != null) {
            unFilter();
        } else {
            mUnFilteredItems = (ArrayList<ScheduleItem>) mItems.clone();
            mItems = new ArrayList<ScheduleItem>();
            for (int idx = 0; idx < mUnFilteredItems.size(); idx++) {
                ScheduleItem si = mUnFilteredItems.get(idx);
                if (!si.filter(line)) {
                    mItems.add(si);
                }
            }
            notifyDataSetChanged();
            sendMessage(Vatman.OPERATION_FILTERED);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) mContent
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.schedule_item, null);
        }
        final ScheduleItem o = mItems.get(position);
        final Typeface tf = Typeface.createFromAsset(mContent.getAssets(),
                "fonts/HoneycombAndroidClock.ttf");
        TextView tv = (TextView) v.findViewById(R.id.schedule_item_time);
        tv.setTypeface(tf);
        if (o != null) {
            updateTime(tv, o);
            ListView bt = (ListView) v.findViewById(R.id.schedule_item_lines);

            if (bt != null) {
                bt.setAdapter(new LinesAdapter(o.getLines(), mContent));
                Layout.setListViewHeightBasedOnChildren(bt);
                bt.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View v,
                                            int position, long id) {

                        Line line = o.getLines().get(position);
                        filter(line);
                    }
                });
            }
        }
        return v;
    }

    private void sendMessage(int operation) {
        Message message = new Message();
        message.what = operation;
        mHandler.sendMessage(message);
    }
}