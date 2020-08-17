package mega.privacy.android.app.components.twemoji.reaction;

import android.view.View;
import androidx.viewpager.widget.ViewPager;

public class ViewPagerUtils  {

    public static View getCurrentView(ViewPager viewPager) {
        for (int i = 0; i < viewPager.getChildCount(); i++) {
            final View child = viewPager.getChildAt(i);
            final ViewPager.LayoutParams layoutParams = (ViewPager.LayoutParams) child.getLayoutParams();
            if (!layoutParams.isDecor) {
                return child;
            }
        }
        return null;
    }
}