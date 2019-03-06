package mega.privacy.android.app.snackbarListeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.utils.Constants;

public class SnackbarNavigateOption implements View.OnClickListener{

    Context context;

    public SnackbarNavigateOption(Context context) {

        this.context=context;
    }

    @Override
    public void onClick(View v) {
        //Intent to Settings

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop)context).moveToSettingsSectionStorage();
        }
        else if(context instanceof FullScreenImageViewerLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((FullScreenImageViewerLollipop)context).startActivity(settingIntent);
            ((FullScreenImageViewerLollipop)context).finish();
        }
        else if(context instanceof PdfViewerActivityLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((PdfViewerActivityLollipop)context).startActivity(settingIntent);
            ((PdfViewerActivityLollipop)context).finish();
        }
        else if(context instanceof AudioVideoPlayerLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((AudioVideoPlayerLollipop)context).startActivity(settingIntent);
            ((AudioVideoPlayerLollipop)context).finish();
        }
        else if(context instanceof FolderLinkActivityLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((FolderLinkActivityLollipop)context).startActivity(settingIntent);
        }
        else if(context instanceof FileLinkActivityLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((FileLinkActivityLollipop)context).startActivity(settingIntent);
        }
        else if(context instanceof FileInfoActivityLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((FileInfoActivityLollipop)context).startActivity(settingIntent);
            ((FileInfoActivityLollipop)context).finish();
        }
        else if(context instanceof ContactFileListActivityLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((ContactFileListActivityLollipop)context).startActivity(settingIntent);
            ((ContactFileListActivityLollipop)context).finish();
        }
        else if(context instanceof ChatActivityLollipop){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((ChatActivityLollipop)context).startActivity(settingIntent);
            ((ChatActivityLollipop)context).finish();
        }
        else if(context instanceof ChatFullScreenImageViewer){
            Intent settingIntent = new Intent(context, ManagerActivityLollipop.class);
            settingIntent.setAction(Constants.ACTION_SHOW_SETTINGS_STORAGE);
            settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ((ChatFullScreenImageViewer)context).startActivity(settingIntent);
            ((ChatFullScreenImageViewer)context).finish();
        }
    }
}
