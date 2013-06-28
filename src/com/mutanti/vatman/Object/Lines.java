package com.mutanti.vatman.Object;

import java.util.ArrayList;
import java.util.Iterator;

public final class Lines extends Object implements Cloneable {
	private ArrayList<Line> m_lines;

	public Lines() {
		m_lines = new ArrayList<Line>();
	}

	public void add(Line line) {
		m_lines.add(line);
	}

	public int size() {
		return m_lines.size();
	}

	public Line get(int pos) {
		return m_lines.get(pos);
	}

	public void remove(int pos) {
		if (m_lines.size() > pos) {
			m_lines.remove(pos);
		}
	}

	public Iterator<Line> iterator() {
		Iterator<Line> iLines = m_lines.iterator();
		return iLines;
	}

	public Lines clone() {
		try {
			return (Lines) super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
