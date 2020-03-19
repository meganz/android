package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.ContactsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.ReceivedRequestsFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SentRequestsFragmentLollipop;

import static mega.privacy.android.app.utils.LogUtil.*;

public class ContactsPageAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 3;
    private Context context;

    public ContactsPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        logDebug("Position: " + position);
        switch (position){
            case 0: {
                ContactsFragmentLollipop cF = (ContactsFragmentLollipop) ((ManagerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag(ManagerActivityLollipop.FragmentTag.CONTACTS.getTag());
                if (cF != null) {
                    return cF;
                }
                else {
                    return ContactsFragmentLollipop.newInstance();
                }
            }
            case 1:{
                SentRequestsFragmentLollipop srF = (SentRequestsFragmentLollipop) ((ManagerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag(ManagerActivityLollipop.FragmentTag.SENT_REQUESTS.getTag());
                if (srF != null) {
                    return srF;
                }
                else {
                    return SentRequestsFragmentLollipop.newInstance();
                }
            }
            case 2:{
                ReceivedRequestsFragmentLollipop rrF = (ReceivedRequestsFragmentLollipop) ((ManagerActivityLollipop) context).getSupportFragmentManager().findFragmentByTag(ManagerActivityLollipop.FragmentTag.RECEIVED_REQUESTS.getTag());
                if (rrF != null) {
                    return rrF;
                }
                else {
                    return ReceivedRequestsFragmentLollipop.newInstance();
                }
            }
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position){
            case 0: {
                return context.getString(R.string.section_contacts);
            }
            case 1:{
                return context.getString(R.string.tab_sent_requests);
            }
            case 2:{
                return context.getString(R.string.tab_received_requests);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
