package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;
import mega.privacy.android.app.utils.Util;

public class FileExplorerPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    private Context context;

    public FileExplorerPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        log("getItem: "+position);
        switch (position){
            case 0: {
                return CloudDriveExplorerFragmentLollipop.newInstance();
            }
            case 1:{
                return IncomingSharesExplorerFragmentLollipop.newInstance();
            }
            case 2:{
                return ChatExplorerFragment.newInstance();
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
            case 2:{
                return context.getString(R.string.section_chat);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        if(Util.isChatEnabled()){
            return PAGE_COUNT;
        }
        else{
            return PAGE_COUNT-1;
        }
    }

    private static void log(String log) {
        Util.log("FileExplorerPagerAdapter", log);
    }
}
