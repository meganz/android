package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
		TextView  categoryTitle = (TextView)super.onCreateView(parent);
//		categoryTitle.setBackgroundColor(Color.WHITE);
//		categoryTitle.setTextColor(context.getResources().getColor(R.color.pressed_mega));

		return categoryTitle;
	}

}
