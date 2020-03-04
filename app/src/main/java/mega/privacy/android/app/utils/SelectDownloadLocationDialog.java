package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.settingsFragments.DownloadSettingsFragment;
import mega.privacy.android.app.lollipop.DownloadableActivity;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.download.ChatDownloadInfo;
import mega.privacy.android.app.utils.download.DownloadInfo;
import mega.privacy.android.app.utils.download.DownloadLinkInfo;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.LogUtil.*;

public class SelectDownloadLocationDialog {

    protected Context context;

    private String[] sdCardOptions;

    private String titleDefaultDownloadLocation, titleDownloadLocation;

    private AlertDialog.Builder dialogBuilder;

    private boolean isDefaultLocation;

    protected DownloadInfo downloadInfo;

    protected MegaNode document;

    protected String url;

    protected ArrayList<String> serializedNodes;

    protected ArrayList<MegaNode> nodeList;

    protected long[] hashes;

    protected long size;

    protected From from;

    protected DatabaseHandler dbH;

    public enum From {
        NORMAL, SETTINGS, FILE_LINK, FOLDER_LINK, CHAT
    }

    protected NodeController nodeController;

    protected DownloadSettingsFragment settingsFragment;

    protected ChatController chatController;

    protected FolderLinkActivityLollipop folderLinkActivity;

    public SelectDownloadLocationDialog(Context context, From from) {
        this.context = context;
        sdCardOptions = context.getResources().getStringArray(R.array.settings_storage_download_location_array);
        titleDefaultDownloadLocation = context.getResources().getString(R.string.settings_storage_download_location);
        titleDownloadLocation = context.getResources().getString(R.string.title_select_download_location);
        dialogBuilder = new AlertDialog.Builder(context);
        this.from = from;
        dbH = DatabaseHandler.getDbHandler(context);
    }

    public void setIsDefaultLocation(boolean isDefaultLocation) {
        this.isDefaultLocation = isDefaultLocation;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public void setDocument(MegaNode document) {
        this.document = document;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setNodeList(ArrayList<MegaNode> nodeList) {
        this.nodeList = nodeList;
    }

    public void setSerializedNodes(ArrayList<String> serializedNodes) {
        this.serializedNodes = serializedNodes;
    }

    public void setHashes(long[] hashes) {
        this.hashes = hashes;
    }

    public void setFolderLinkActivity(FolderLinkActivityLollipop folderLinkActivity) {
        this.folderLinkActivity = folderLinkActivity;
    }

    public void setNodeController(NodeController nodeController) {
        this.nodeController = nodeController;
    }

    public void setChatController(ChatController chatController) {
        this.chatController = chatController;
    }

    public void setSettingsFragment(DownloadSettingsFragment settingsFragment) {
        this.settingsFragment = settingsFragment;
    }

    private void selectLocalFolder(String sdRoot) {
        switch (from) {
            case NORMAL:
                nodeController.requestLocalFolder(downloadInfo, sdRoot, null);
                break;
            case SETTINGS:
                settingsFragment.toSelectFolder(sdRoot);
                break;
            case FILE_LINK:
                nodeController.intentPickFolder(document, url, sdRoot);
                break;
            case FOLDER_LINK:
                folderLinkActivity.toSelectFolder(hashes, size, sdRoot, null);
                break;
            case CHAT:
                chatController.requestLocalFolder(size, serializedNodes, sdRoot);
                break;
        }
    }

    private void initDialogBuilder() {
        setTitle();
        dialogBuilder.setNegativeButton(context.getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialogBuilder.setItems(sdCardOptions, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: {
                        selectLocalFolder(null);
                        break;
                    }
                    case 1: {
                        SDCardOperator sdCardOperator;
                        try {
                            sdCardOperator = new SDCardOperator(context);
                        } catch (SDCardOperator.SDCardException e) {
                            e.printStackTrace();
                            logError("Initialize SDCardOperator failed", e);
                            //sd card is available, choose internal storage location
                            selectLocalFolder(null);
                            return;
                        }
                        String sdCardRoot = sdCardOperator.getSDCardRoot();
                        //don't use DocumentFile
                        if (sdCardOperator.canWriteWithFile(sdCardRoot)) {
                            selectLocalFolder(sdCardRoot);
                        } else {
                            switch (from) {
                                case NORMAL:
                                    if (context instanceof DownloadableActivity) {
                                        ((DownloadableActivity) context).setDownloadInfo(downloadInfo);
                                    }
                                    break;
                                case FILE_LINK:
                                    if (context instanceof DownloadableActivity) {
                                        ((DownloadableActivity) context).setLinkInfo(new DownloadLinkInfo(document, url));
                                    }
                                    break;
                                case FOLDER_LINK:
                                    if (context instanceof DownloadableActivity) {
                                        ((DownloadableActivity) context).setDownloadInfo(new DownloadInfo(false, size, hashes));
                                    }
                                    break;
                                case CHAT:
                                    if (context instanceof DownloadableActivity) {
                                        ((DownloadableActivity) context).setChatDownloadInfo(new ChatDownloadInfo(size, serializedNodes, nodeList));
                                    }
                                    break;
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                try {
                                    sdCardOperator.initDocumentFileRoot(dbH.getSDCardUri());
                                    selectLocalFolder(sdCardRoot);
                                } catch (SDCardOperator.SDCardException e) {
                                    e.printStackTrace();
                                    logError("SDCardOperator initDocumentFileRoot failed, requestSDCardPermission", e);
                                    requestSDCardWritePermission(sdCardRoot);
                                }
                            } else {
                                requestSDCardWritePermission(sdCardRoot);
                            }
                        }
                        break;
                    }
                }
            }
        });
    }

    private void requestSDCardWritePermission(String sdCardRoot) {
        if(settingsFragment != null) {
            // from settings
            SDCardOperator.requestSDCardPermission(sdCardRoot, context, settingsFragment);
        } else {
            SDCardOperator.requestSDCardPermission(sdCardRoot, context, (Activity) context);
        }
    }

    private void setTitle() {
        if (isDefaultLocation) {
            dialogBuilder.setTitle(titleDefaultDownloadLocation);
        } else {
            dialogBuilder.setTitle(titleDownloadLocation);
        }
    }

    public void show() {
        initDialogBuilder();
        dialogBuilder.create().show();
    }
}
