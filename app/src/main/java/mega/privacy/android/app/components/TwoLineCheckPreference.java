package mega.privacy.android.app.components;

import android.content.Context;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoLineCheckPreference extends CheckBoxPreference {

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
	public void onBindViewHolder(PreferenceViewHolder holder) {
		super.onBindViewHolder(holder);

		TextView textView = (TextView) holder.findViewById(android.R.id.title);
		if (textView != null) {
			textView.setSingleLine(false);
		}
	}
}
