package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager.widget.PagerAdapter;
import mega.privacy.android.app.lollipop.megachat.UserReactionsView;

public class InfoReactionPagerAdapter extends PagerAdapter {
    private int numTabs;
    private Context context;

    public InfoReactionPagerAdapter(Context context, int numTabs) {
        this.numTabs = numTabs;
        this.context = context;
    }

    @Override
    public int getCount() {
        return numTabs;
    }


    @Override public Object instantiateItem(final ViewGroup pager, final int position) {
        View newView ;
        newView = new UserReactionsView(pager.getContext()).init();

        pager.addView(newView);
        return newView;
    }

    @Override public void destroyItem(final ViewGroup pager, final int position, final Object view) {
        pager.removeView((View) view);
    }

    @Override public boolean isViewFromObject(final View view, final Object object) {
        return view.equals(object);
    }
}
