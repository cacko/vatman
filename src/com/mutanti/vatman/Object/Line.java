package com.mutanti.vatman.Object;

import com.mutanti.vatman.R;

public final class Line {
    private static final String TYPE_TRAMVAI = "t0";
    private static final String TYPE_TROLLEY = "t2";
    private static final String TYPE_BUS = "t1";
    private String m_lineNum;
    private String m_lineType;
    private String m_lineRoute;

    public Line(String lineNum, String lineType, String lineRoute) {
        m_lineNum = lineNum.trim();
        m_lineType = lineType.trim();
        m_lineRoute = lineRoute.trim();
    }

    public final String getNum() {
        return m_lineNum;
    }

    public final String getRoute() {
        return m_lineRoute;
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
