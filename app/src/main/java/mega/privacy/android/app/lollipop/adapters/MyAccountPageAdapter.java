package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyStorageFragmentLollipop;

import static mega.privacy.android.app.utils.LogUtil.*;

public class MyAccountPageAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 2;
    private Context context;

    public MyAccountPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        logDebug("Position: " + position);
        switch (position){
            case 0: {
                return MyAccountFragmentLollipop.newInstance();
            }
            case 1:{
                return MyStorageFragmentLollipop.newInstance();
            }
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position){
            case 0: {
                return context.getString(R.string.tab_my_account_general);
            }
            case 1:{
                return context.getString(R.string.tab_my_account_storage);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
