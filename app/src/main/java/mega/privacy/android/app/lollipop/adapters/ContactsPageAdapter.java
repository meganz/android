package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.ReceivedRequestsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SentRequestsFragmentLollipop;
import mega.privacy.android.app.utils.Util;

public class ContactsPageAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    private Context context;

    public ContactsPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        log("getItem: "+position);
        switch (position){
            case 0: {
                return ContactsFragmentLollipop.newInstance();
            }
            case 1:{
                return SentRequestsFragmentLollipop.newInstance();
            }
            case 2:{
                return ReceivedRequestsFragmentLollipop.newInstance();
            }
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position){
            case 0: {
                return context.getString(R.string.section_contacts).toLowerCase();
            }
            case 1:{
                return context.getString(R.string.tab_sent_requests).toLowerCase();
            }
            case 2:{
                return context.getString(R.string.tab_received_requests).toLowerCase();
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    private static void log(String log) {
        Util.log("ContactsPageAdapter", log);
    }
}
