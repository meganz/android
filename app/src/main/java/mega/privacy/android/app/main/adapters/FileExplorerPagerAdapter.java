package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.StringResourcesUtils.getString;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.CloudDriveExplorerFragment;
import mega.privacy.android.app.main.IncomingSharesExplorerFragment;
import mega.privacy.android.app.main.megachat.ChatExplorerFragment;
import timber.log.Timber;

public class FileExplorerPagerAdapter extends FragmentStatePagerAdapter {

    private static final int PAGE_COUNT = 3;
    private boolean tabRemoved;

    private Fragment mChatFragment;
    private Fragment mCloudFragment;
    private Fragment mIncomingFragment;

    public FileExplorerPagerAdapter(FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        Timber.d("Position: %s", position);
        switch (position) {
            case 1:
                return getIncomingFragment();

            case 2:
                return getChatFragment();

            case 0:
            default:
                return getCloudFragment();
        }
    }

    private Fragment getChatFragment() {
        if (mChatFragment != null) {
            return mChatFragment;
        }

        return mChatFragment = ChatExplorerFragment.newInstance();
    }

    private Fragment getIncomingFragment() {
        if (mIncomingFragment != null) {
            return mIncomingFragment;
        }

        return mIncomingFragment = IncomingSharesExplorerFragment.newInstance();
    }

    private Fragment getCloudFragment() {
        if (mCloudFragment != null) {
            return mCloudFragment;
        }

        return mCloudFragment = CloudDriveExplorerFragment.newInstance();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 1:
                return getString(R.string.tab_incoming_shares);

            case 2:
                return getString(R.string.section_chat);

            case 0:
            default:
                return getString(R.string.section_cloud_drive);
        }
    }

    public void setTabRemoved(boolean tabRemoved) {
        this.tabRemoved = tabRemoved;
    }

    @Override
    public int getCount() {
        return !tabRemoved ? PAGE_COUNT : PAGE_COUNT - 1;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

}
