package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mega.privacy.android.app.R;

public class MEGAPreferenceCategory extends PreferenceCategory{

	Context context;
	
	public MEGAPreferenceCategory(Context context) {
		super(context);
		this.context = context;
	}
	
	public MEGAPreferenceCategory(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}
	
	public MEGAPreferenceCategory(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}
	
	@Override
	protected View onCreateView (ViewGroup parent){
		View  categoryTitle = (View)super.onCreateView(parent);
		categoryTitle.setBackgroundColor(ContextCompat.getColor(context, R.color.background_secondary));

		return categoryTitle;
	}

}
