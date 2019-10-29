package mega.privacy.android.app.listeners;

import java.util.ArrayList;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.utils.LogUtil.*;

public class GlobalListener implements MegaGlobalListenerInterface {

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        MegaApplication.getInstance().onUserAlertsUpdate(userAlerts);
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {
        if (nodeList == null) return;

        MegaApplication.getInstance().onNodesUpdate(nodeList);
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {
        logDebug("onAccountUpdate");

        MegaApplication.getInstance().onAccountUpdate();
    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
        if (requests == null) return;

        MegaApplication.getInstance().onContactRequestsUpdate(requests);
    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {
        logDebug("Event received: " + event.getText());

        switch (event.getType()) {
            case MegaEvent.EVENT_STORAGE:
                logDebug("EVENT_STORAGE: " + event.getNumber());

                int state = (int) event.getNumber();
                if (state == MegaApiJava.STORAGE_STATE_CHANGE) {
                    api.getAccountDetails(null);
                } else {
                    MegaApplication.getInstance().updateAccountDetails(state);
                }
                break;

            case MegaEvent.EVENT_ACCOUNT_BLOCKED:
                logDebug("EVENT_ACCOUNT_BLOCKED: " + event.getNumber());

                if (!(MegaApplication.getInstance().getApplicationContext() instanceof BaseActivity))
                    return;
                ((BaseActivity) MegaApplication.getInstance().getApplicationContext()).checkWhyAmIBlocked(event.getNumber(), event.getText());
                break;
        }
    }
}
