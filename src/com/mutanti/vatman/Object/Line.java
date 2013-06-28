package com.mutanti.vatman.Object;

import com.mutanti.vatman.R;

public final class Line {
	private String m_lineNum;
	private String m_lineType;
	private String m_lineRoute;
	private static final String TYPE_TRAMVAI = "t0";
	private static final String TYPE_TROLLEY = "t2";
	private static final String TYPE_BUS = "t1";

	public Line(String lineNum, String lineType, String lineRoute) {
		m_lineNum = lineNum.trim();
		m_lineType = lineType.trim();
		m_lineRoute = lineRoute.trim();
	}

	public final String getNum() {
		return m_lineNum;
	}

	public final String getType() {
		return m_lineType;
	}
	
	public final String getRoute() {
		return m_lineRoute;
	}

	public final int getTypeNameId() {
		int nameId = 0;
		if (m_lineType.equalsIgnoreCase(TYPE_TRAMVAI)) {
			nameId = R.string.type_tramvai;
		} else if (m_lineType.equalsIgnoreCase(TYPE_TROLLEY)) {
			nameId = R.string.type_trolley;
		} else if (m_lineType.equalsIgnoreCase(TYPE_BUS)) {
			nameId = R.string.type_bus;
		}
		return nameId;
	}

	public final int getIconId() {
		int id = 0;
		if (m_lineType.equalsIgnoreCase(TYPE_TRAMVAI)) {
			id = R.drawable.tramvai;
		} else if (m_lineType.equalsIgnoreCase(TYPE_TROLLEY)) {
			id = R.drawable.trolley;
		} else if (m_lineType.equalsIgnoreCase(TYPE_BUS)) {
			id = R.drawable.bus_selected;
		}
		return id;
	}
}
