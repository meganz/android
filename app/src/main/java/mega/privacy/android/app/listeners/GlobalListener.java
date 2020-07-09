package mega.privacy.android.app.listeners;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER;

public class GlobalListener implements MegaGlobalListenerInterface {

    private MegaApplication megaApplication;
    private DatabaseHandler dbH;

    public GlobalListener() {
        megaApplication = MegaApplication.getInstance();
        dbH = megaApplication.getDbH();
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
        if (users == null || users.isEmpty()) return;

        for (MegaUser user : users) {
            if (user == null) {
                continue;
            }

            boolean isMyChange = api.getMyUserHandle().equals(MegaApiJava.userHandleToBase64(user.getHandle()));

            if (user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER) && isMyChange) {
                api.getMyChatFilesFolder(new GetAttrUserListener(megaApplication, true));
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_CAMERA_UPLOADS_FOLDER) && isMyChange) {
                //user has change CU attribute, need to update local ones
                api.getUserAttribute(USER_ATTR_CAMERA_UPLOADS_FOLDER, new GetAttrUserListener(megaApplication));
                break;
            }
        }
    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        megaApplication.updateAppBadge();
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {
        if (nodeList == null) return;

        for (int i = 0; i < nodeList.size(); i++) {
            MegaNode n = nodeList.get(i);
            if (n.isInShare() && n.hasChanged(MegaNode.CHANGE_TYPE_INSHARE)) {
                megaApplication.showSharedFolderNotification(n);
            }
        }
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {
        logDebug("onAccountUpdate");

        Intent intent = new Intent(BROADCAST_ACTION_INTENT_ON_ACCOUNT_UPDATE);
        intent.setAction(ACTION_ON_ACCOUNT_UPDATE);
        MegaApplication.getInstance().sendBroadcast(intent);

        api.getPaymentMethods(null);
        api.getAccountDetails(null);
        api.getPricing(null);
        api.creditCardQuerySubscriptions(null);
        dbH.resetExtendedAccountDetailsTimestamp();
    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
        if (requests == null) return;

        megaApplication.updateAppBadge();

        for (int i = 0; i < requests.size(); i++) {
            MegaContactRequest cr = requests.get(i);
            if (cr != null) {
                if ((cr.getStatus() == MegaContactRequest.STATUS_UNRESOLVED) && (!cr.isOutgoing())) {

                    ContactsAdvancedNotificationBuilder notificationBuilder;
                    notificationBuilder = ContactsAdvancedNotificationBuilder.newInstance(megaApplication, megaApplication.getMegaApi());

                    notificationBuilder.removeAllIncomingContactNotifications();
                    notificationBuilder.showIncomingContactRequestNotification();

                    logDebug("IPC: " + cr.getSourceEmail() + " cr.isOutgoing: " + cr.isOutgoing() + " cr.getStatus: " + cr.getStatus());
                } else if ((cr.getStatus() == MegaContactRequest.STATUS_ACCEPTED) && (cr.isOutgoing())) {

                    ContactsAdvancedNotificationBuilder notificationBuilder;
                    notificationBuilder = ContactsAdvancedNotificationBuilder.newInstance(megaApplication, megaApplication.getMegaApi());

                    notificationBuilder.showAcceptanceContactRequestNotification(cr.getTargetEmail());

                    logDebug("ACCEPT OPR: " + cr.getSourceEmail() + " cr.isOutgoing: " + cr.isOutgoing() + " cr.getStatus: " + cr.getStatus());
                }
            }
        }
    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {
        logDebug("Event received: " + event.getText());

        if (megaApplication == null) {
            megaApplication = MegaApplication.getInstance();
        }

        switch (event.getType()) {
            case MegaEvent.EVENT_STORAGE:
                logDebug("EVENT_STORAGE: " + event.getNumber());

                int state = (int) event.getNumber();
                if (state == MegaApiJava.STORAGE_STATE_CHANGE) {
                    api.getAccountDetails(null);
                } else {
                    megaApplication.setStorageState(state);

                    Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
                    intent.setAction(ACTION_STORAGE_STATE_CHANGED);
                    intent.putExtra(EXTRA_STORAGE_STATE, state);
                    megaApplication.sendBroadcast(intent);
                }
                break;

            case MegaEvent.EVENT_ACCOUNT_BLOCKED:
                logDebug("EVENT_ACCOUNT_BLOCKED: " + event.getNumber());

                Intent intent = new Intent(BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED);
                intent.setAction(ACTION_EVENT_ACCOUNT_BLOCKED);
                intent.putExtra(EVENT_NUMBER, event.getNumber());
                intent.putExtra(EVENT_TEXT, event.getText());
                megaApplication.sendBroadcast(intent);
                break;

            case MegaEvent.EVENT_BUSINESS_STATUS:
                megaApplication.updateBusinessStatus();

                break;
        }
    }
}
