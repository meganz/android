package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class FabButtonListener implements FloatingActionButton.OnClickListener{

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;

    public FabButtonListener(Context context){
        LogUtil.logDebug("FabButtonListener created");
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        LogUtil.logDebug("FabButtonListener");
        switch(v.getId()) {
            case R.id.floating_button: {
                LogUtil.logDebug("Floating Button click!");
                if(context instanceof ManagerActivityLollipop){
                    drawerItem = ((ManagerActivityLollipop)context).getDrawerItem();
                    switch (drawerItem){
                        case CLOUD_DRIVE:
                        case SEARCH:
                        case SHARED_ITEMS:{
                            LogUtil.logDebug("Cloud Drive SECTION");
                            if(!Util.isOnline(context)){
                                if(context instanceof ManagerActivityLollipop){
                                    ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                                }
                                return;
                            }
                            ((ManagerActivityLollipop)context).showUploadPanel();
                            break;
                        }
                        case CONTACTS:{
                            LogUtil.logDebug("Add contacts");
                            if(!Util.isOnline(context)){
                                if(context instanceof ManagerActivityLollipop){
                                    ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                                }
                                return;
                            }
                            ((ManagerActivityLollipop)context).chooseAddContactDialog(false);
                            break;
                        }
                        case CHAT:{
                            LogUtil.logDebug("Create new chat");
                            ((ManagerActivityLollipop)context).chooseAddContactDialog(true);
                            break;
                        }
                    }
                }
                break;
            }
            case R.id.floating_button_contact_file_list:{
                if(!Util.isOnline(context)){
                    if(context instanceof ContactFileListActivityLollipop){
                        ((ContactFileListActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
                    }
                    return;
                }
                if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop)context).showUploadPanel();
                }
                break;
            }
            case R.id.main_fab_chat: {
                LogUtil.logDebug("Main FAB chat click!");
                ((ManagerActivityLollipop)context).animateFABCollection();
                break;
            }
            case R.id.first_fab_chat: {
                LogUtil.logDebug("Create new chat");
                break;
            }
            case R.id.second_fab_chat: {
                LogUtil.logDebug("Second FAB chat click");
                break;
            }
            case R.id.third_fab_chat: {
                LogUtil.logDebug("Third FAB chat click");
                break;
            }
        }
    }
}
