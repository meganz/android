package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;

public class FileExplorerPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    private Context context;
    boolean chatFirst = false;

    public FileExplorerPagerAdapter(FragmentManager fm, Context context, boolean chatFirst) {
        super(fm);
        this.context = context;
        this.chatFirst = chatFirst;
    }

    public FileExplorerPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        this.chatFirst = false;
    }

    @Override
    public Fragment getItem(int position) {
        LogUtil.logDebug("Position: " + position);
        if(chatFirst){
            switch (position){
                case 0:{
                    return ChatExplorerFragment.newInstance();
                }
                case 1: {
                    return CloudDriveExplorerFragmentLollipop.newInstance();
                }
                case 2:{
                    return IncomingSharesExplorerFragmentLollipop.newInstance();
                }
            }
        }
        else{
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
        }

        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if(chatFirst){
            switch (position){
                case 0:{
                    return context.getString(R.string.section_chat).toLowerCase();
                }
                case 1: {
                    return context.getString(R.string.section_cloud_drive).toLowerCase();
                }
                case 2:{
                    return context.getString(R.string.tab_incoming_shares).toLowerCase();
                }
            }
        }
        else{
            switch (position){
                case 0: {
                    return context.getString(R.string.section_cloud_drive).toLowerCase();
                }
                case 1:{
                    return context.getString(R.string.tab_incoming_shares).toLowerCase();
                }
                case 2:{
                    return context.getString(R.string.section_chat).toLowerCase();
                }
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
}
