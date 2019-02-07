package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class FabButtonListener implements FloatingActionButton.OnClickListener{

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;

    public FabButtonListener(Context context){
        log("FabButtonListener created");
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        log("onClick FabButtonListener");
        MegaApiAndroid megaApi = ((MegaApplication)((ManagerActivityLollipop)context).getApplication()).getMegaApi();
        megaApi.sendSMSVerificationCode("+642108294492",new MegaRequestListenerInterface() {
            @Override
            public void onRequestStart(MegaApiJava api,MegaRequest request) {
                Log.e("@#@","code: " + request.getRequestString() );
            }

            @Override
            public void onRequestUpdate(MegaApiJava api,MegaRequest request) {

            }

            @Override
            public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
                Log.e("@#@","code: " + e.getErrorCode() );
            }

            @Override
            public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {

            }
        },true);
//        switch(v.getId()) {
//            case R.id.floating_button: {
//                log("Floating Button click!");
//                if(context instanceof ManagerActivityLollipop){
//                    drawerItem = ((ManagerActivityLollipop)context).getDrawerItem();
//                    switch (drawerItem){
//                        case CLOUD_DRIVE:
//                        case SEARCH:
//                        case SHARED_ITEMS:{
//                            log("Cloud Drive SECTION");
//                            if(!Util.isOnline(context)){
//                                if(context instanceof ManagerActivityLollipop){
//                                    ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
//                                }
//                                return;
//                            }
//                            ((ManagerActivityLollipop)context).showUploadPanel();
//                            break;
//                        }
//                        case CONTACTS:{
//                            log("Add contacts");
//                            if(!Util.isOnline(context)){
//                                if(context instanceof ManagerActivityLollipop){
//                                    ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
//                                }
//                                return;
//                            }
//                            ((ManagerActivityLollipop)context).chooseAddContactDialog(false);
//                            break;
//                        }
//                        case CHAT:{
//                            log("Create new chat");
//                            ((ManagerActivityLollipop)context).chooseAddContactDialog(true);
//                            break;
//                        }
//                    }
//                }
//                break;
//            }
//            case R.id.floating_button_contact_file_list:{
//                if(!Util.isOnline(context)){
//                    if(context instanceof ContactFileListActivityLollipop){
//                        ((ContactFileListActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
//                    }
//                    return;
//                }
//                if(context instanceof ContactFileListActivityLollipop){
//                    ((ContactFileListActivityLollipop)context).showUploadPanel();
//                }
//                break;
//            }
//            case R.id.main_fab_chat: {
//                log("Main FAB chat click!");
//                ((ManagerActivityLollipop)context).animateFABCollection();
//                break;
//            }
//            case R.id.first_fab_chat: {
//                log("Create new chat");
//                break;
//            }
//            case R.id.second_fab_chat: {
//                log("Second FAB chat click");
//                break;
//            }
//            case R.id.third_fab_chat: {
//                log("Third FAB chat click");
//                break;
//            }
//        }
    }

    public static void log(String message) {
        Util.log("FabButtonListener", message);
    }
}
