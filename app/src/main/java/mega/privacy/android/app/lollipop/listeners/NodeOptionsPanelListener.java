package mega.privacy.android.app.lollipop.listeners;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class NodeOptionsPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    MegaApiAndroid megaApi;
    NodeController nC;

    public NodeOptionsPanelListener(Context context){
        log("NodeOptionsPanelListener created");
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        nC = new NodeController(context);
    }

    @Override
    public void onClick(View v) {
        log("onClick NodeOptionsPanelListener");
        MegaNode selectedNode = null;
        if(context instanceof ManagerActivityLollipop){
            selectedNode = ((ManagerActivityLollipop) context).getSelectedNode();
        }
        switch(v.getId()){
//            case R.id.offline_list_option_remove_layout:{
//                log("OFFLINE_list_out_options option");
//                ((OfflineActivityLollipop) context).hideOptionsPanel();
//                String pathNavigation = ((OfflineActivityLollipop) context).getPathNavigation();
//                MegaOffline mOff = ((OfflineActivityLollipop) context).getSelectedNode();
//                nC.deleteOffline(mOff, pathNavigation);
//                break;
//            }
//
//            case R.id.offline_list_out_options:{
//                log("OFFLINE_list_out_options option");
//                ((OfflineActivityLollipop) context).hideOptionsPanel();
//                break;
//            }
        }
    }

    public static void log(String message) {
        Util.log("NodeOptionsPanelListener", message);
    }
}
