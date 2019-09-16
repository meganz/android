package mega.privacy.android.app.lollipop.providers;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

public class ProviderPageAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 2;
    private Context context;

    public ProviderPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        LogUtil.logDebug("position: " + position);
        switch (position){
            case 0: {
                return CloudDriveProviderFragmentLollipop.newInstance();
            }
            case 1:{
                return IncomingSharesProviderFragmentLollipop.newInstance();
            }
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position){
            case 0: {
                return context.getString(R.string.section_cloud_drive);
            }
            case 1:{
                return context.getString(R.string.tab_incoming_shares);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
