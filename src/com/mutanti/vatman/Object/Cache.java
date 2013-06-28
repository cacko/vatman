package com.mutanti.vatman.Object;

import java.util.ArrayList;

import android.util.SparseArray;

public class Cache {

	private static Cache sInstance = null;

	private SparseArray<Long> mCacheTimestamps;
	private SparseArray<ArrayList<ScheduleItem>> mCache;

	private Cache() {
		mCacheTimestamps = new SparseArray<Long>();
		mCache = new SparseArray<ArrayList<ScheduleItem>>();
	}

	public static Cache getInstance() {
		if (sInstance == null) {
			sInstance = new Cache();
		}
		return sInstance;
	}

	public ArrayList<ScheduleItem> get(int stop) {
		ArrayList<ScheduleItem> result = null;
		if(mCacheTimestamps.indexOfKey(stop) > -1) {
			long cacheTimestamp = mCacheTimestamps.get(stop);
			if((cacheTimestamp + 1000 * 60 * 2) > System.currentTimeMillis()) {
				result = mCache.get(stop);
			} else {
				mCacheTimestamps.delete(stop);
				mCache.delete(stop);
			}
		}
		return result;
	}

	public void put(int stop, ArrayList<ScheduleItem> schedule) {
		mCacheTimestamps.delete(stop);
		mCacheTimestamps.put(stop, System.currentTimeMillis());
		mCache.delete(stop);
		mCache.put(stop, schedule);
	}

}
