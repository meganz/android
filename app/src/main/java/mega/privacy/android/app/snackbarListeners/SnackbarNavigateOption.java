package mega.privacy.android.app.snackbarListeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.contacts.ContactsActivity;
import mega.privacy.android.app.lollipop.ContactFileListActivity;
import mega.privacy.android.app.lollipop.ContactInfoActivity;
import mega.privacy.android.app.lollipop.FileInfoActivity;
import mega.privacy.android.app.lollipop.ManagerActivity;
import mega.privacy.android.app.lollipop.PdfViewerActivity;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.ChatActivity;
import mega.privacy.android.app.utils.Constants;
import static mega.privacy.android.app.utils.Constants.*;

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
            } else if (isSentAsMessageSnackbar) {
                ((ManagerActivity) context).moveToChatSection(idChat);
            } else {
                ((ManagerActivity) context).moveToSettingsSectionStorage();
            }

            return;
        }

        if(context instanceof ChatActivity){
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
        } else {
            intent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        context.startActivity(intent);

        if (context instanceof ImageViewerActivity) {
            ((ImageViewerActivity) context).finish();
        } else if (context instanceof PdfViewerActivity) {
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
