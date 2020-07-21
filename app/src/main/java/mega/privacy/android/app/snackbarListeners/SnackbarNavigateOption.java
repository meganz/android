package mega.privacy.android.app.snackbarListeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.utils.Constants;
import static mega.privacy.android.app.utils.Constants.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class SnackbarNavigateOption implements View.OnClickListener {

    Context context;
    long idChat;
    private int type;
    boolean isSentAsMessageSnackbar = false;

    public SnackbarNavigateOption(Context context) {
        this.context = context;
    }

    public SnackbarNavigateOption(Context context, int type) {
        this.context = context;
        this.type = type;
    }

    public SnackbarNavigateOption(Context context, long idChat) {
        this.context = context;
        this.idChat = idChat;
        isSentAsMessageSnackbar = true;
    }

    @Override
    public void onClick(View v) {
        //Intent to Settings

        if (context instanceof ManagerActivityLollipop) {
            if (type == MUTE_NOTIFICATIONS_SNACKBAR_TYPE) {
                MegaApplication.getPushNotificationSettingManagement().controlMuteNotifications(context, NOTIFICATIONS_ENABLED, MEGACHAT_INVALID_HANDLE);
            } else if (isSentAsMessageSnackbar) {
                ((ManagerActivityLollipop) context).moveToChatSection(idChat);
            } else {
                ((ManagerActivityLollipop) context).moveToSettingsSectionStorage();
            }

            return;
        }

        Intent intent = new Intent(context, ManagerActivityLollipop.class);

        if (isSentAsMessageSnackbar) {
            intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("CHAT_ID", idChat);
            intent.putExtra("moveToChatSection", true);
        } else {
            intent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        context.startActivity(intent);

        if (context instanceof FullScreenImageViewerLollipop) {
            ((FullScreenImageViewerLollipop) context).finish();
        } else if (context instanceof PdfViewerActivityLollipop) {
            ((PdfViewerActivityLollipop) context).finish();
        } else if (context instanceof AudioVideoPlayerLollipop) {
            ((AudioVideoPlayerLollipop) context).finish();
        } else if (context instanceof FileInfoActivityLollipop) {
            ((FileInfoActivityLollipop) context).finish();
        } else if (context instanceof ContactFileListActivityLollipop) {
            ((ContactFileListActivityLollipop) context).finish();
        } else if (context instanceof ChatActivityLollipop) {
            ((ChatActivityLollipop) context).finish();
        } else if (context instanceof ChatFullScreenImageViewer) {
            ((ChatFullScreenImageViewer) context).finish();
        } else if (context instanceof ContactInfoActivityLollipop) {
            ((ContactInfoActivityLollipop) context).finish();
        }
    }
}
