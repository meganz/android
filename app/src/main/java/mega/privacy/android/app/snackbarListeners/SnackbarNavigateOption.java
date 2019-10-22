package mega.privacy.android.app.snackbarListeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.utils.Constants;

public class SnackbarNavigateOption implements View.OnClickListener {

    Context context;
    long idChat;
    boolean isSentAsMessageSnackbar = false;

    public SnackbarNavigateOption(Context context) {
        this.context = context;
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
            if (isSentAsMessageSnackbar) {
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


        if (context instanceof FullScreenImageViewerLollipop) {
            ((FullScreenImageViewerLollipop) context).startActivity(intent);
            ((FullScreenImageViewerLollipop) context).finish();
        } else if (context instanceof PdfViewerActivityLollipop) {
            ((PdfViewerActivityLollipop) context).startActivity(intent);
            ((PdfViewerActivityLollipop) context).finish();
        } else if (context instanceof AudioVideoPlayerLollipop) {
            ((AudioVideoPlayerLollipop) context).startActivity(intent);
            ((AudioVideoPlayerLollipop) context).finish();
        } else if (context instanceof FolderLinkActivityLollipop) {
            ((FolderLinkActivityLollipop) context).startActivity(intent);
        } else if (context instanceof FileLinkActivityLollipop) {
            ((FileLinkActivityLollipop) context).startActivity(intent);
        } else if (context instanceof FileInfoActivityLollipop) {
            ((FileInfoActivityLollipop) context).startActivity(intent);
            ((FileInfoActivityLollipop) context).finish();
        } else if (context instanceof ContactFileListActivityLollipop) {
            ((ContactFileListActivityLollipop) context).startActivity(intent);
            ((ContactFileListActivityLollipop) context).finish();
        } else if (context instanceof ChatActivityLollipop) {
            ((ChatActivityLollipop) context).startActivity(intent);
            ((ChatActivityLollipop) context).finish();
        } else if (context instanceof ChatFullScreenImageViewer) {
            ((ChatFullScreenImageViewer) context).startActivity(intent);
            ((ChatFullScreenImageViewer) context).finish();
        } else if (context instanceof ContactInfoActivityLollipop) {
            ((ContactInfoActivityLollipop) context).startActivity(intent);
            ((ContactInfoActivityLollipop) context).finish();
        }
    }
}
