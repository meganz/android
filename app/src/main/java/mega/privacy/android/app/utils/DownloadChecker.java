package mega.privacy.android.app.utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.DownloadableActivity;
import mega.privacy.android.app.utils.download.ChatDownloadInfo;
import mega.privacy.android.app.utils.download.DownloadInfo;
import mega.privacy.android.app.utils.download.DownloadLinkInfo;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;

public class DownloadChecker extends SelectDownloadLocationDialog {

    private String downloadRoot;

    private SDCardOperator sdCardOperator;

    private String downloadPath;

    private DownloadableActivity downloadable;

    private boolean isSDCardPath;

    public DownloadChecker(Context context, String downloadPath, From from) {
        super(context, from);
        if(context instanceof DownloadableActivity) {
            this.downloadable = (DownloadableActivity) context;
        }
        this.downloadPath = downloadPath;
        isSDCardPath = SDCardOperator.isSDCardPath(downloadPath);
    }

    public String getDownloadRoot() {
        return downloadRoot;
    }

    public SDCardOperator getSdCardOperator() {
        return sdCardOperator;
    }

    private void requestInternalFolder(String prompt) {
        switch (from) {
            case NORMAL:
                nodeController.requestLocalFolder(downloadInfo, null, prompt);
                break;
            case FILE_LINK:
                nodeController.intentPickFolder(document, url, null);
                break;
            case FOLDER_LINK:
                folderLinkActivity.toSelectFolder(hashes, size, null, null);
                break;
            case CHAT:
                chatController.requestLocalFolder(size, serializedNodes, null);
                break;
        }
    }

    private void showSelectDownloadLocationDialog() {
        switch (from) {
            case NORMAL:
                nodeController.showSelectDownloadLocationDialog(downloadInfo);
                break;
            case FILE_LINK:
                nodeController.showSelectDownloadLocationDialog(document, url);
                break;
            case FOLDER_LINK:
                folderLinkActivity.showSelectDownloadLocationDialog(hashes, size);
                break;
            case CHAT:
                chatController.showSelectDownloadLocationDialog(nodeList, size);
                break;
        }
    }

    public boolean check() {
        try {
            sdCardOperator = new SDCardOperator(context);
        } catch (SDCardOperator.SDCardException e) {
            e.printStackTrace();
            logError("Initialize SDCardOperator failed", e);
            // user uninstall the sd card. but default download location is still on the sd card
            if (isSDCardPath) {
                logDebug("select new path as download location.");
                requestInternalFolder(context.getString(R.string.no_external_SD_card_detected));
                return false;
            }
        }
        if (sdCardOperator != null && isSDCardPath) {
            //user has installed another sd card.
            if (sdCardOperator.isNewSDCardPath(downloadPath)) {
                logDebug("new sd card, check permission.");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showSelectDownloadLocationDialog();
                    }
                }, 1500);
                Toast.makeText(context, R.string.old_sdcard_unavailable, Toast.LENGTH_LONG).show();
                return false;
            }
            if (!sdCardOperator.canWriteWithFile(downloadPath)) {
                downloadRoot = sdCardOperator.getDownloadRoot();
                try {
                    sdCardOperator.initDocumentFileRoot(dbH.getSDCardUri());
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    logError("SDCardOperator initDocumentFileRoot failed requestSDCardPermission", e);
                    //don't have permission with sd card root. need to request.
                    String sdRoot = sdCardOperator.getSDCardRoot();
                    switch (from) {
                        case NORMAL:
                            downloadable.setDownloadInfo(downloadInfo);
                            break;
                        case FILE_LINK:
                            downloadable.setLinkInfo(new DownloadLinkInfo(document, url));
                            break;
                        case FOLDER_LINK:
                            downloadable.setDownloadInfo(new DownloadInfo(false, size, hashes));
                            break;
                        case CHAT:
                            downloadable.setChatDownloadInfo(new ChatDownloadInfo(size, serializedNodes, nodeList));
                            break;
                    }
                    //request SD card write permission.
                    SDCardOperator.requestSDCardPermission(sdRoot, context, downloadable);
                    return false;
                }
            }
        }
        return true;
    }
}
