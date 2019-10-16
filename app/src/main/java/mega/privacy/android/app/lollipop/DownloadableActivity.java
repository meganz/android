package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.provider.DocumentFile;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DownloadInfo;
import mega.privacy.android.app.utils.DownloadLinkInfo;
import mega.privacy.android.app.utils.FileUtil;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.Util;

import static mega.privacy.android.app.utils.LogUtil.*;


public class DownloadableActivity extends PinActivityLollipop {

    private DownloadInfo downloadInfo;

    private DownloadLinkInfo linkInfo;

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public void setLinkInfo(DownloadLinkInfo linkInfo) {
        this.linkInfo = linkInfo;
    }

    private Uri extractUri(Intent intent, int resultCode) {
        if (intent == null) {
            logWarning("extractUri: result intent is null");
            if (resultCode != Activity.RESULT_OK) {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.download_requires_permission), -1);
            } else {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.no_external_SD_card_detected), -1);
            }
            return null;
        }
        return intent.getData();
    }

    protected void onRequestSDCardWritePermission(Intent intent, int resultCode, NodeController nC) {
        Uri treeUri = extractUri(intent, resultCode);
        if (treeUri != null) {
            String uriString = treeUri.toString();
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir != null && pickedDir.canWrite()) {
                //save the sd card root uri string
                DatabaseHandler dbH = DatabaseHandler.getDbHandler(this);
                dbH.setSDCardUri(uriString);
                try {
                    SDCardOperator sdCardOperator = new SDCardOperator(this);
                    if (downloadInfo != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            nC.requestLocalFolder(downloadInfo, sdCardOperator.getSDCardRoot(), null);
                        } else {
                            String path = FileUtil.getFullPathFromTreeUri(treeUri, this);
                            dbH.setStorageDownloadLocation(path);
                            nC.checkSizeBeforeDownload(path, null, downloadInfo.getSize(), downloadInfo.getHashes(), downloadInfo.isHighPriority());
                        }
                    } else if (linkInfo != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            nC.intentPickFolder(linkInfo.getNode(), linkInfo.getUrl(), sdCardOperator.getSDCardRoot());
                        } else {
                            String path = FileUtil.getFullPathFromTreeUri(treeUri, this);
                            dbH.setStorageDownloadLocation(path);
                            nC.downloadTo(linkInfo.getNode(), path, linkInfo.getUrl());
                        }
                    }
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    logError("Initialize SDCardOperator failed", e);
                }
            }
        } else {
            logWarning("tree uri is null!");
        }
    }

    protected void onRequestSDCardWritePermission(Intent intent, int resultCode) {
        Uri treeUri = extractUri(intent, resultCode);
        if (treeUri != null) {
            String uriString = treeUri.toString();
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir != null && pickedDir.canWrite()) {
                //save the sd card root uri string
                DatabaseHandler dbH = DatabaseHandler.getDbHandler(this);
                dbH.setSDCardUri(uriString);
                try {
                    SDCardOperator sdCardOperator = new SDCardOperator(this);

                    NodeController controller = new NodeController(this);
                    String path = FileUtil.getFullPathFromTreeUri(treeUri, this);
                    //file link
                    if (linkInfo != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            controller.intentPickFolder(linkInfo.getNode(), linkInfo.getUrl(), sdCardOperator.getSDCardRoot());
                        } else {
                            dbH.setStorageDownloadLocation(path);
                            controller.downloadTo(linkInfo.getNode(), path, linkInfo.getUrl());
                        }
                    } else {
                        //folder link
                        if (this instanceof FolderLinkActivityLollipop) {
                            FolderLinkActivityLollipop activity = (FolderLinkActivityLollipop) this;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                activity.toSelectFolder(downloadInfo.getHashes(), downloadInfo.getSize(), sdCardOperator.getSDCardRoot(), null);
                            } else {
                                dbH.setStorageDownloadLocation(path);
                                activity.downloadTo(path, uriString, null, downloadInfo.getSize(), downloadInfo.getHashes());
                            }
                        }
                    }
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    logError(e.getMessage());
                }
            }
        } else {
            logWarning("tree uri is null!");
        }
    }
}
