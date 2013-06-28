package com.mutanti.vatman.Object;

import android.text.format.Time;

import com.mutanti.vatman.Vatman;

public final class ScheduleItem {
	private long m_time;
	private Lines m_lines;
	private Lines m_unFilteredItems;
	private final String TIMEZONE = "Europe/Athens";

	public ScheduleItem(String time, Lines lines) {
		m_time = Long.valueOf(time);
		m_lines = lines;
		m_unFilteredItems = null;
	}

	public long getTime() {
		return m_time;
	}

	public long getElapsedMinutes() {
		long millis = 0;

		Time now = new Time(TIMEZONE);
		now.set(System.currentTimeMillis());
		
		Time busTime = new Time(TIMEZONE);
		busTime.set(m_time * 1000);
		millis = busTime.toMillis(false) - now.toMillis(false) + Vatman.TIME_DRIFT;
		return millis;
	}

	public Lines getLines() {
		return m_lines;
	}
	
	public void unFilter() {
		if(m_unFilteredItems != null) {
			m_lines = m_unFilteredItems.clone();
			m_unFilteredItems = null;		
		}
	}
	
	public boolean filter(Line line) {
		if(line == null || m_unFilteredItems != null) {
			unFilter();
			return false;
		}
		int removed = 0;
		m_unFilteredItems = m_lines.clone();
		m_lines = new Lines();
		for(int idx = 0; idx < m_unFilteredItems.size(); idx++) {
			Line aLine = m_unFilteredItems.get(idx);
			if(line == null || aLine.getNum() == line.getNum()) {
				m_lines.add(aLine);
			} else {
				removed++;
			}
		}
		return (removed >= m_unFilteredItems.size());
	}
}
