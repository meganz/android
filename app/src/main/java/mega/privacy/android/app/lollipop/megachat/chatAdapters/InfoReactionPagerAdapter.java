package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import java.util.ArrayList;
import mega.privacy.android.app.components.reaction.*;

public class InfoReactionPagerAdapter extends PagerAdapter {
    private Context context;
    private ArrayList<View> views = new ArrayList<>();

    private long chatId;
    private long msgId;

    public InfoReactionPagerAdapter(Context context, ArrayList<String> listReactions, long msgId, long chatId) {
        this.context = context;
        this.msgId = msgId;
        this.chatId = chatId;
        fillList(listReactions);
    }

    private void fillList(ArrayList<String> listReactions) {
        for (int i = 0; i < listReactions.size(); i++) {
            String reaction = listReactions.get(i);
            View newView = new UserReactionListView(context).init(reaction, msgId, chatId);
            views.add(newView);
        }
    }

    @Override
    public int getCount() {
        return views.size();
    }

    @Override
    public Object instantiateItem(final ViewGroup pager, final int position) {
        View v = views.get (position);
        pager.addView (v);
        return v;
    }

    @Override
    public void destroyItem(final ViewGroup pager, final int position, final Object view) {
        pager.removeView(views.get(position));
    }

    @Override
    public boolean isViewFromObject(final View view, final Object object) {
        return view.equals(object);
    }

    @Override
    public int getItemPosition(Object object) {
        int index = views.indexOf(object);
        if (index == -1){
            return POSITION_NONE;
        }
        else {
            return index;
        }
    }

    /**
     * Used to add a new page.
     *
     * @param v The new view that will be added.
     * @return int The position of the new view
     */
    public int addView(View v) {
        views.add(v);
        return getItemPosition(v);
    }

    /**
     * Used to add a new page.
     *
     * @param v        The new view that will be added.
     * @param position int The position in the array.
     * @return int The position of the new view
     */
    public int addView(View v, int position) {
        views.add(position, v);
        return position;
    }

    /**
     * Remove a concrete view.
     *
     * @param pager Pager of the view
     * @param v     The new view that will be removed.
     * @return position of removed view.
     */
    public int removeView(ViewPager pager, View v) {
        int position =  views.indexOf(v);
        return removeView(pager, position);
    }

    /**
     * Remove a concrete view.
     *
     * @param pager
     * @param position
     * @return The position of removed view.
     */
    public int removeView(ViewPager pager, int position) {
        pager.setAdapter(null);
        views.remove(position);
        pager.setAdapter(this);
        return position;
    }

    /**
     * Obtain the "view" at "position".
     *
     * @param position of the view recovered.
     * @return the "view" at "position".
     */
    public View getView(int position) {
        return views.get(position);
    }

    public void updatePage(int position, String reaction){
        UserReactionListView view = (UserReactionListView)getView(position);
        view.updateUsers(reaction, msgId, chatId);
    }


}
