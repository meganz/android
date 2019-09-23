package mega.privacy.android.app.components;

import mega.privacy.android.app.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;


public class LinearLayoutCheckable extends LinearLayout implements Checkable {

	private CheckBox _checkbox;
	
	public LinearLayoutCheckable(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// find checked text view
		_checkbox = (CheckBox)findViewById(R.id.checkbox);
	}
	
	@Override
	public boolean isChecked() {
		return _checkbox != null ? _checkbox.isChecked() : false;
	}

	@Override
	public void setChecked(boolean checked) {
		if (_checkbox != null) {
			_checkbox.setChecked(checked);
		}
	}

	@Override
	public void toggle() {
		if (_checkbox != null) {
			_checkbox.toggle();
		}
	}
}
