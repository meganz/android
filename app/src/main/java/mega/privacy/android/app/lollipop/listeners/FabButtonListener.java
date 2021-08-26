package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.content.Intent;
import android.view.View;

import ash.TL;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.WeakAccountProtectionAlertActivity;
import mega.privacy.android.app.listeners.BaseListener;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

import androidx.annotation.NonNull;

public class FabButtonListener implements FloatingActionButton.OnClickListener{

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;

    public FabButtonListener(Context context){
        logDebug("FabButtonListener created");
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        logDebug("FabButtonListener");
        switch(v.getId()) {
            case R.id.floating_button: {
                logDebug("Floating Button click!");
                if(context instanceof ManagerActivityLollipop){
                    drawerItem = ((ManagerActivityLollipop)context).getDrawerItem();
                    switch (drawerItem){
                        case CLOUD_DRIVE:
                        case SEARCH:
                        case SHARED_ITEMS:{
                            logDebug("Cloud Drive SECTION");
                            if(!isOnline(context)){
                                if(context instanceof ManagerActivityLollipop){
                                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                                }
                                return;
                            }
                            ((ManagerActivityLollipop)context).showUploadPanel();
                            break;
                        }
                        case CONTACTS:{
                            logDebug("Add contacts");
                            if(!isOnline(context)){
                                if(context instanceof ManagerActivityLollipop){
                                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                                }
                                return;
                            }
                            ((ManagerActivityLollipop)context).chooseAddContactDialog(false);
                            break;
                        }
                        case CHAT:{
                            logDebug("Create new chat");
                            if (!Util.isFastDoubleClick()) {
                                ((ManagerActivityLollipop) context).chooseAddContactDialog(true);
                            }
                            break;
                        }
                    }
                }
                break;
            }
            case R.id.floating_button_contact_file_list:{
                if(!isOnline(context)){
                    if(context instanceof ContactFileListActivityLollipop){
                        ((ContactFileListActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
                    }
                    return;
                }
                if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop)context).showUploadPanel();
                }
                break;
            }
        }
    }
}
