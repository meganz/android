package mega.privacy.android.app.listeners;

import android.content.Intent;

import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder;
import mega.privacy.android.app.service.iar.RatingHandlerImpl;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;
import timber.log.Timber;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_AVATAR_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_MY_BACKUPS_FOLDER_CHANGED;
import static mega.privacy.android.app.constants.EventConstants.EVENT_USER_VISIBILITY_CHANGE;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.Constants.ACTION_STORAGE_STATE_CHANGED;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS;
import static mega.privacy.android.app.utils.Constants.EVENT_NOTIFICATION_COUNT_CHANGE;
import static mega.privacy.android.app.utils.Constants.EXTRA_STORAGE_STATE;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
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

            String myUserHandle = api.getMyUserHandle();
            boolean isMyChange = myUserHandle != null && myUserHandle.equals(MegaApiJava.userHandleToBase64(user.getHandle()));

            if(user.getChanges() == 0 && !isMyChange){
                LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long.class).post(user.getHandle());
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_PUSH_SETTINGS) && isMyChange) {
                MegaApplication.getPushNotificationSettingManagement().updateMegaPushNotificationSetting();
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_MY_CHAT_FILES_FOLDER) && isMyChange) {
                api.getMyChatFilesFolder(new GetAttrUserListener(megaApplication, true));
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_CAMERA_UPLOADS_FOLDER) && isMyChange) {
                //user has change CU attribute, need to update local ones
                Timber.d("Get CameraUpload attribute when change on other client.");
                api.getUserAttribute(USER_ATTR_CAMERA_UPLOADS_FOLDER, new GetCameraUploadAttributeListener(megaApplication));
                break;
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS) && isMyChange) {
                api.shouldShowRichLinkWarning(new GetAttrUserListener(megaApplication));
                api.isRichPreviewsEnabled(new GetAttrUserListener(megaApplication));
                break;
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_RUBBISH_TIME) && isMyChange) {
                api.getRubbishBinAutopurgePeriod(new GetAttrUserListener(megaApplication));
                break;
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_DISABLE_VERSIONS) && isMyChange) {
                api.getFileVersionsOption(new GetAttrUserListener(megaApplication));
                break;
            }

            // Receive the avatar change, send the event
            if (user.hasChanged(MegaUser.CHANGE_TYPE_AVATAR) && user.isOwnChange() == 0){
                LiveEventBus.get(EVENT_MEETING_AVATAR_CHANGE, Long.class).post(user.getHandle());
            }

            if (user.hasChanged(MegaUser.CHANGE_TYPE_MY_BACKUPS_FOLDER) && isMyChange) {
                //user has change backup attribute, need to update local ones
                logDebug("MyBackup + Get backup attribute when change on other client.");
                LiveEventBus.get(EVENT_MY_BACKUPS_FOLDER_CHANGED, Boolean.class).post(true);
                break;
            }
        }
    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        megaApplication.updateAppBadge();

        notifyNotificationCountChange(api);
    }

    private void notifyNotificationCountChange(MegaApiJava api) {
        ArrayList<MegaContactRequest> incomingContactRequests = api.getIncomingContactRequests();
        LiveEventBus.get(EVENT_NOTIFICATION_COUNT_CHANGE, Integer.class).post(api.getNumUnreadUserAlerts()
                + (incomingContactRequests == null ? 0 : incomingContactRequests.size()));
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {
        if (nodeList == null) return;

        for (int i = 0; i < nodeList.size(); i++) {
            MegaNode n = nodeList.get(i);
            if (n.isInShare() && n.hasChanged(MegaNode.CHANGE_TYPE_INSHARE)) {
                megaApplication.showSharedFolderNotification(n);
            } else if (n.hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK) && n.getPublicLink() != null) {
                // when activated share, will show rating if it matches the condition
                new RatingHandlerImpl(megaApplication.getApplicationContext()).showRatingBaseOnSharing();
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
        megaApplication.sendBroadcast(intent);

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
        notifyNotificationCountChange(api);

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

                    new RatingHandlerImpl(megaApplication.getApplicationContext()).showRatingBaseOnContacts();
                }

                if(cr.getStatus() == MegaContactRequest.STATUS_ACCEPTED){
                    LiveEventBus.get(EVENT_USER_VISIBILITY_CHANGE, Long.class).post(cr.getHandle());
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
                } else if (state == MegaApiJava.STORAGE_STATE_PAYWALL) {
                    megaApplication.setStorageState(state);
                    showOverDiskQuotaPaywallWarning();
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

                megaApplication.sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED)
                        .putExtra(EVENT_NUMBER, event.getNumber())
                        .putExtra(EVENT_TEXT, event.getText()));
                break;

            case MegaEvent.EVENT_BUSINESS_STATUS:
                megaApplication.sendBroadcastUpdateAccountDetails();

                break;

            case MegaEvent.EVENT_MISC_FLAGS_READY:
                megaApplication.checkEnabledCookies();

                break;
        }
    }
}
