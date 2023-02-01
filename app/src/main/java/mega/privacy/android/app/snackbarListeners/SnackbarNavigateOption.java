package mega.privacy.android.app.snackbarListeners;

import static mega.privacy.android.app.utils.Constants.ACTION_SHOW_TRANSFERS;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.DISMISS_ACTION_SNACKBAR;
import static mega.privacy.android.app.utils.Constants.EXTRA_MOVE_TO_CHAT_SECTION;
import static mega.privacy.android.app.utils.Constants.INVITE_CONTACT_TYPE;
import static mega.privacy.android.app.utils.Constants.MUTE_NOTIFICATIONS_SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.Constants.OPENED_FROM_IMAGE_VIEWER;
import static mega.privacy.android.app.utils.Constants.RESUME_TRANSFERS_TYPE;
import static mega.privacy.android.app.utils.Constants.SENT_REQUESTS_TYPE;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.contacts.ContactsActivity;
import mega.privacy.android.app.main.ContactFileListActivity;
import mega.privacy.android.app.main.ContactInfoActivity;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.PdfViewerActivity;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.presentation.manager.model.TransfersTab;
import mega.privacy.android.app.utils.Constants;

public class SnackbarNavigateOption implements View.OnClickListener {

    Context context;
    long idChat;
    private String userEmail;
    private int type;
    boolean isSentAsMessageSnackbar = false;

    public SnackbarNavigateOption(Context context) {
        this.context = context;
    }

    public SnackbarNavigateOption(Context context, int type) {
        this.context = context;
        this.type = type;
    }

    public SnackbarNavigateOption(Context context, int type, String userEmail) {
        this.context = context;
        this.type = type;
        this.userEmail = userEmail;
    }

    public SnackbarNavigateOption(Context context, long idChat) {
        this.context = context;
        this.idChat = idChat;
        isSentAsMessageSnackbar = true;
    }

    @Override
    public void onClick(View v) {
        //Intent to Settings

        if (type == DISMISS_ACTION_SNACKBAR) {
            //Do nothing, only dismiss
            return;
        }

        if (context instanceof ManagerActivity) {
            if (type == MUTE_NOTIFICATIONS_SNACKBAR_TYPE) {
                MegaApplication.getPushNotificationSettingManagement().controlMuteNotifications(context, NOTIFICATIONS_ENABLED, null);
            } else if (type == RESUME_TRANSFERS_TYPE) {
                ((ManagerActivity) context).selectDrawerItem(DrawerItem.TRANSFERS);
            } else if (isSentAsMessageSnackbar) {
                ((ManagerActivity) context).moveToChatSection(idChat);
            } else {
                ((ManagerActivity) context).moveToSettingsSectionStorage();
            }
            return;
        }

        if (context instanceof ChatActivity) {
            switch (type) {
                case INVITE_CONTACT_TYPE:
                    new ContactController(context).inviteContact(userEmail);
                    return;

                case SENT_REQUESTS_TYPE:
                    ((ChatActivity) context).startActivity(ContactsActivity.getSentRequestsIntent(context));
                    return;
            }
        }

        Intent intent = new Intent(context, ManagerActivity.class);

        if (isSentAsMessageSnackbar) {
            intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(CHAT_ID, idChat);
            intent.putExtra(EXTRA_MOVE_TO_CHAT_SECTION, true);
        } else if (type == RESUME_TRANSFERS_TYPE) {
            intent.setAction(ACTION_SHOW_TRANSFERS);
            intent.putExtra(OPENED_FROM_IMAGE_VIEWER, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB);
        } else {
            intent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        context.startActivity(intent);

        if (context instanceof PdfViewerActivity) {
            ((PdfViewerActivity) context).finish();
        } else if (context instanceof FileInfoActivity) {
            ((FileInfoActivity) context).finish();
        } else if (context instanceof ContactFileListActivity) {
            ((ContactFileListActivity) context).finish();
        } else if (context instanceof ChatActivity) {
            ((ChatActivity) context).finish();
        } else if (context instanceof ContactInfoActivity) {
            ((ContactInfoActivity) context).finish();
        }
    }
}
