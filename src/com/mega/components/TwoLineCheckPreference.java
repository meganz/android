package com.mega.components;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class TwoLineCheckPreference extends CheckBoxPreference{

	public TwoLineCheckPreference(Context ctx, AttributeSet attrs, int defStyle) {
		super(ctx, attrs, defStyle);
    }
	
	public TwoLineCheckPreference(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
	}

	public TwoLineCheckPreference(Context ctx) {
		super(ctx);
	}
	
	@Override
	 protected void onBindView(View view) {
		super.onBindView(view);
		
		TextView textView = (TextView) view.findViewById(android.R.id.title);
		if (textView != null) {
			textView.setSingleLine(false);
		}
	}
}
