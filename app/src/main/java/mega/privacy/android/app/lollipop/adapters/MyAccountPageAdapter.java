package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyStorageFragmentLollipop;

import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class MyAccountPageAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 2;
    private Context context;

    public MyAccountPageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;

        refreshAccountInfo();
    }

    @Override
    public Fragment getItem(int position) {
        logDebug("Position: " + position);
        switch (position){
            case 0: {
                return MyAccountFragmentLollipop.newInstance();
            }
            case 1:{
                return MyStorageFragmentLollipop.newInstance();
            }
        }
        return null;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position){
            case 0: {
                return context.getString(R.string.tab_my_account_general);
            }
            case 1:{
                return context.getString(R.string.tab_my_account_storage);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    private void refreshAccountInfo(){
        MegaApplication app = MegaApplication.getInstance();
        MyAccountInfo myAccountInfo = app.getMyAccountInfo();

        //Check if the call is recently
        logDebug("Check the last call to getAccountDetails");
        if(callToAccountDetails(context) || myAccountInfo.getUsedFormatted().trim().length() <= 0) {
            logDebug("megaApi.getAccountDetails SEND");
            app.askForAccountDetails();
        }
        logDebug("Check the last call to getExtendedAccountDetails");
        if(callToExtendedAccountDetails(context)){
            logDebug("megaApi.getExtendedAccountDetails SEND");
            app.askForExtendedAccountDetails();
        }
        logDebug("Check the last call to getPaymentMethods");
        if(callToPaymentMethods(context)){
            logDebug("megaApi.getPaymentMethods SEND");
            app.askForPaymentMethods();
        }
    }
}
