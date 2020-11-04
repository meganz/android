package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.ChatPreferencesActivity;
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.DBUtil.resetAccountDetailsTimeStamp;
import static mega.privacy.android.app.utils.LogUtil.*;

public class SettingsListener extends BaseListener implements MegaGlobalListenerInterface, MegaChatListenerInterface {

    private static final int DAYS_USER_FREE = 30;
    private static final int DAYS_USER_PRO = 90;

    public SettingsListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        super.onRequestFinish(api, request, e);
        if (context == null)
            return;

        switch (request.getType()) {
            case MegaRequest.TYPE_GET_ATTR_USER:
                switch (request.getParamType()) {
                    case MegaApiJava.USER_ATTR_RICH_PREVIEWS:
                        if (context instanceof ChatPreferencesActivity) {
                            if (e.getErrorCode() == MegaError.API_ENOENT) {
                                logWarning("Attribute USER_ATTR_RICH_PREVIEWS not set");
                            }
                            if (request.getNumDetails() == 1) {
                                MegaApplication.setShowRichLinkWarning(request.getFlag());
                                MegaApplication.setCounterNotNowRichLinkWarning((int) request.getNumber());
                            } else if (request.getNumDetails() == 0) {
                                MegaApplication.setEnabledRichLinks(request.getFlag());
                                ((ChatPreferencesActivity) context).needUpdateRichLinks();
                            }
                        }
                        break;

                    case MegaApiJava.USER_ATTR_RUBBISH_TIME:
                        if (context instanceof FileManagementPreferencesActivity) {
                            if (e.getErrorCode() == MegaError.API_ENOENT) {
                                ((FileManagementPreferencesActivity) context).updateRBScheduler(MegaApplication.getInstance().getMyAccountInfo().getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE ?
                                        DAYS_USER_FREE : DAYS_USER_PRO);
                            } else {
                                ((FileManagementPreferencesActivity) context).updateRBScheduler(request.getNumber());
                            }
                        }
                        break;

                    case MegaApiJava.USER_ATTR_DISABLE_VERSIONS:
                        if (context instanceof FileManagementPreferencesActivity) {
                            MegaApplication.setDisableFileVersions(request.getFlag());
                            ((FileManagementPreferencesActivity) context).updateEnabledFileVersions();
                        }
                        break;
                }
                break;

            case MegaRequest.TYPE_SET_ATTR_USER:
                switch (request.getParamType()) {
                    case MegaApiJava.USER_ATTR_RICH_PREVIEWS:
                        if (context instanceof ChatPreferencesActivity && e.getErrorCode() != MegaError.API_OK) {
                            ((ChatPreferencesActivity) context).needUpdateRichLinks();
                        }
                        break;

                    case MegaApiJava.USER_ATTR_RUBBISH_TIME:
                        if (context instanceof FileManagementPreferencesActivity) {
                            if (e.getErrorCode() == MegaError.API_OK) {
                                ((FileManagementPreferencesActivity) context).updateRBScheduler(request.getNumber());
                            } else {
                                Util.showSnackbar(context, context.getString(R.string.error_general_nodes));
                            }
                        }
                        break;

                    case MegaApiJava.USER_ATTR_DISABLE_VERSIONS:
                        if (context instanceof FileManagementPreferencesActivity) {
                            MegaApplication.setDisableFileVersions(Boolean.parseBoolean(request.getText()));

                            if (e.getErrorCode() != MegaError.API_OK) {
                                logError("ERROR:USER_ATTR_DISABLE_VERSIONS");
                                ((FileManagementPreferencesActivity) context).updateEnabledFileVersions();
                            } else {
                                logDebug("File versioning attribute changed correctly");
                            }
                        }
                        break;
                }
                break;

            case MegaRequest.TYPE_CLEAN_RUBBISH_BIN:
                if (context instanceof FileManagementPreferencesActivity) {
                    if (e.getErrorCode() == MegaError.API_OK) {
                        Util.showSnackbar(context, context.getString(R.string.rubbish_bin_emptied));
                        resetAccountDetailsTimeStamp();
                        ((FileManagementPreferencesActivity) context).resetRubbishInfo();
                    } else {
                        Util.showSnackbar(context, context.getString(R.string.rubbish_bin_no_emptied));
                    }
                }
                break;

            case MegaRequest.TYPE_REMOVE_VERSIONS:
                if (e.getErrorCode() == MegaError.API_OK) {
                    Util.showSnackbar(context, context.getString(R.string.success_delete_versions));
                    MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_RESET_VERSION_INFO_SETTING));
                } else {
                    Util.showSnackbar(context, context.getString(R.string.error_delete_versions));
                }
                break;

            case MegaRequest.TYPE_FOLDER_INFO:
                MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_SET_VERSION_INFO_SETTING));
                break;
        }
    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {
    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {
    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
        if (config == null || context == null)
            return;

        if (context instanceof ChatPreferencesActivity) {
            if (!config.isPending()) {
                ((ChatPreferencesActivity) context).needUpdatePresence(false);
            }
        }
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {
    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
        if (context == null || users == null)
            return;

        for (int i = 0; i < users.size(); i++) {
            MegaUser user = users.get(i);
            if (user != null) {
                if (context instanceof ChatPreferencesActivity && user.isOwnChange() > 0 &&
                        user.hasChanged(MegaUser.CHANGE_TYPE_RICH_PREVIEWS)) {
                    api.shouldShowRichLinkWarning(this);
                    api.isRichPreviewsEnabled(this);
                }

                if (context instanceof FileManagementPreferencesActivity &&
                        user.isOwnChange() <= 0 && api.getMyUser() != null &&
                        user.getHandle() == api.getMyUser().getHandle()) {

                    if (user.hasChanged(MegaUser.CHANGE_TYPE_RUBBISH_TIME)) {
                        api.getRubbishBinAutopurgePeriod(this);
                    }
                    if (user.hasChanged(MegaUser.CHANGE_TYPE_DISABLE_VERSIONS)) {
                        api.getFileVersionsOption(this);
                    }
                }
            }
        }
    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {
    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {
    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {
    }
}
