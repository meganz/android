package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import mega.privacy.android.app.utils.Util;

public class CloudPageAdapter extends FragmentStatePagerAdapter {

    private final int CLOUD_TAB = 0;
    private final int RECENTS_TAB = 1;
    private final int PAGE_COUNT = 2;
    private Context context;

    public CloudPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        log("getItem: " + position);
        switch (position) {
            case CLOUD_TAB: {
                FileBrowserFragmentLollipop fbF = (FileBrowserFragmentLollipop) ((ManagerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag((ManagerActivityLollipop.FragmentTag.CLOUD_DRIVE.getTag()));
                if (fbF != null) {
                    return fbF;
                }

                return FileBrowserFragmentLollipop.newInstance();
            }
            case RECENTS_TAB: {
                RecentsFragment rF = (RecentsFragment) ((ManagerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag((ManagerActivityLollipop.FragmentTag.RECENTS.getTag()));
                if (rF != null) {
                    return rF;
                }

                return RecentsFragment.newInstance();
            }
        }
        return null;
    }

    @Override
    public int getItemPosition(Object obj) {
        return POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case CLOUD_TAB: {
                return context.getString(R.string.section_cloud_drive).toLowerCase();
            }
            case RECENTS_TAB: {
                return context.getString(R.string.section_recents).toLowerCase();
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    private static void log(String log) {
        Util.log("CloudPageAdapter", log);
    }
}
