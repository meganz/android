package mega.privacy.android.app.main.adapters;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.managerSections.CompletedTransfersFragment;
import mega.privacy.android.app.main.managerSections.TransfersFragment;
import timber.log.Timber;

public class TransfersPageAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 2;
    private Context context;

    public TransfersPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        Timber.d("Position: %s", position);
        switch (position) {
            case 0: {
                return TransfersFragment.newInstance();
            }
            case 1: {
                return CompletedTransfersFragment.newInstance();
            }
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 0: {
                return context.getString(R.string.title_tab_in_progress_transfers);
            }
            case 1: {
                return context.getString(R.string.title_tab_completed_transfers);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
