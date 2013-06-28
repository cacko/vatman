package com.mutanti.vatman.Adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mutanti.vatman.R;
import com.mutanti.vatman.Object.Line;
import com.mutanti.vatman.Object.Lines;

public final class LinesAdapter extends BaseAdapter {

	private Lines m_lines;
	private Context m_context;

	public LinesAdapter(Lines lines, Context context) {
		m_lines = lines;
		m_context = context;
	}

	public int getCount() {
		return m_lines.size();
	}

	public Line getItem(int position) {
		return m_lines.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		Line line = getItem(position);
		View v;
		LayoutInflater li = (LayoutInflater) m_context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		v = li.inflate(R.layout.line, null);
		TextView tv = (TextView) v.findViewById(R.id.line_name);
		tv.setText(line.getNum());
		ImageView iv = (ImageView) v.findViewById(R.id.line_icon);
		Drawable icon = m_context.getResources().getDrawable(line.getIconId());
		icon.setBounds(0, 0, icon.getIntrinsicWidth(),
				icon.getIntrinsicHeight());
		iv.setImageDrawable(icon);
		TextView tv2 = (TextView) v.findViewById(R.id.line_route);
		tv2.setText(line.getRoute());
		return v;
	}

}