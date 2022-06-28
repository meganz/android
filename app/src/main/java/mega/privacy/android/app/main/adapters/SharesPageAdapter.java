package mega.privacy.android.app.main.adapters;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.presentation.shares.links.LinksFragment;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesFragment;
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesFragment;
import timber.log.Timber;

public class SharesPageAdapter extends FragmentStatePagerAdapter {

    final int PAGE_COUNT = 3;
    private Context context;

    public SharesPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        Timber.d("Position: %s", position);
        switch (position) {
            case 0:
                IncomingSharesFragment isF = (IncomingSharesFragment) ((ManagerActivity) context).getSupportFragmentManager().findFragmentByTag(ManagerActivity.FragmentTag.INCOMING_SHARES.getTag());
                if (isF != null) {
                    return isF;
                }

                return IncomingSharesFragment.newInstance();

            case 1:
                OutgoingSharesFragment osF = (OutgoingSharesFragment) ((ManagerActivity) context).getSupportFragmentManager().findFragmentByTag(ManagerActivity.FragmentTag.OUTGOING_SHARES.getTag());
                if (osF != null) {
                    return osF;
                }

                return OutgoingSharesFragment.newInstance();

            case 2:
                LinksFragment lF = (LinksFragment) ((ManagerActivity) context).getSupportFragmentManager().findFragmentByTag(ManagerActivity.FragmentTag.LINKS.getTag());
                if (lF != null) {
                    return lF;
                }

                return LinksFragment.newInstance();
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
            case 0:
                return context.getString(R.string.tab_incoming_shares);

            case 1:
                return context.getString(R.string.tab_outgoing_shares);

            case 2:
                return context.getString(R.string.tab_links_shares);

        }

        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
