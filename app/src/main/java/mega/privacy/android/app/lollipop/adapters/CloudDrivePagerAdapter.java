package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;

import static mega.privacy.android.app.utils.LogUtil.*;

public class CloudDrivePagerAdapter extends FragmentStatePagerAdapter {

    final int PAGE_COUNT = 2;
    private Context context;

    public CloudDrivePagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        logDebug("Position: " + position);
        switch (position){
            case 0: {
                return FileBrowserFragmentLollipop.newInstance();
            }
            case 1:{
                return RubbishBinFragmentLollipop.newInstance();
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
        switch (position){
            case 0: {
                return context.getString(R.string.section_cloud_drive);
            }
            case 1:{
                return context.getString(R.string.section_rubbish_bin);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
