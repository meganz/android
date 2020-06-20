package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.CloudDriveExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.IncomingSharesExplorerFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerFragment;

import static mega.privacy.android.app.utils.LogUtil.*;

public class FileExplorerPagerAdapter extends FragmentStatePagerAdapter {

    private static final int PAGE_COUNT = 3;
    private Context context;
    private boolean chatFirst;
    private boolean tabRemoved;

    private Fragment mChatFragment;
    private Fragment mCloudFragment;
    private Fragment mIncomingFragment;

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
                    return getChatFragment();
                }
                case 1: {
                    return getCloudFragment();
                }
                case 2:{
                    return getIncomingFragment();
                }
            }
        }
        else{
            switch (position){
                case 0: {
                    return getCloudFragment();
                }
                case 1:{
                    return getIncomingFragment();
                }
                case 2:{
                    return getChatFragment();
                }
            }
        }

        return null;
    }

    private Fragment getChatFragment () {
        if (mChatFragment != null) {
            return mChatFragment;
        }

        return mChatFragment = ChatExplorerFragment.newInstance();
    }

    private Fragment getIncomingFragment () {
        if (mIncomingFragment != null) {
            return mIncomingFragment;
        }

        return mIncomingFragment = IncomingSharesExplorerFragmentLollipop.newInstance();
    }

    private Fragment getCloudFragment () {
        if (mCloudFragment != null) {
            return mCloudFragment;
        }

        return mCloudFragment = CloudDriveExplorerFragmentLollipop.newInstance();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        if(chatFirst){
            switch (position){
                case 0:{
                    return context.getString(R.string.section_chat);
                }
                case 1: {
                    return context.getString(R.string.section_cloud_drive);
                }
                case 2:{
                    return context.getString(R.string.tab_incoming_shares);
                }
            }
        }
        else{
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
        }

        return null;
    }

    public void setTabRemoved (boolean tabRemoved) {
        this.tabRemoved = tabRemoved;
    }

    @Override
    public int getCount() {
        if(!tabRemoved){
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
