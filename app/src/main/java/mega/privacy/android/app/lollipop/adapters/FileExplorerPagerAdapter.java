package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class FileExplorerPagerAdapter extends FragmentStatePagerAdapter {

    private static final int PAGE_COUNT = 3;
    private Context context;
    private boolean chatFirst;
    private boolean tabRemoved;

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
        logDebug("Position: " + position);
        if(chatFirst){
            switch (position){
                case 0:{
                    return getChatFragment(0);
                }
                case 1: {
                    return getCloudFragment(1);
                }
                case 2:{
                    return getIncomingFragment(2);
                }
            }
        }
        else{
            switch (position){
                case 0: {
                    return getCloudFragment(0);
                }
                case 1:{
                    return getIncomingFragment(1);
                }
                case 2:{
                    return getChatFragment(2);
                }
            }
        }

        return null;
    }

    private Fragment getChatFragment (int tab) {
        ChatExplorerFragment ceF = (ChatExplorerFragment) ((FileExplorerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag(((FileExplorerActivityLollipop) context).getFragmentTag(R.id.explorer_tabs_pager, tab));
        if (ceF != null) {
            return ceF;
        }

        return ChatExplorerFragment.newInstance();
    }

    private Fragment getIncomingFragment (int tab) {
        IncomingSharesExplorerFragmentLollipop isF = (IncomingSharesExplorerFragmentLollipop) ((FileExplorerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag(((FileExplorerActivityLollipop) context).getFragmentTag(R.id.explorer_tabs_pager, tab));
        if (isF != null) {
            return isF;
        }
        return IncomingSharesExplorerFragmentLollipop.newInstance();
    }

    private Fragment getCloudFragment (int tab) {
        CloudDriveExplorerFragmentLollipop cdF = (CloudDriveExplorerFragmentLollipop) ((FileExplorerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag(((FileExplorerActivityLollipop) context).getFragmentTag(R.id.explorer_tabs_pager, tab));
        if (cdF != null) {
            return cdF;
        }
        return CloudDriveExplorerFragmentLollipop.newInstance();
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

    public void setTabRemoved (boolean tabRemoved) {
        this.tabRemoved = tabRemoved;
    }

    @Override
    public int getCount() {
        if(isChatEnabled() && !tabRemoved){
            return PAGE_COUNT;
        } else{
            return PAGE_COUNT-1;
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

}
