package mega.privacy.android.app.main.controllers;

import static mega.privacy.android.app.listeners.ShareListener.REMOVE_SHARE_LISTENER;
import static mega.privacy.android.app.listeners.ShareListener.SHARE_LISTENER;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK;
import static mega.privacy.android.app.utils.Constants.CHAT_LINK;
import static mega.privacy.android.app.utils.Constants.CONTACT_LINK;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_BOTH;
import static mega.privacy.android.app.utils.Constants.ERROR_LINK;
import static mega.privacy.android.app.utils.Constants.FILE_LINK;
import static mega.privacy.android.app.utils.Constants.FOLDER_LINK;
import static mega.privacy.android.app.utils.Constants.MULTIPLE_COPY;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CONTACT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.MegaApiUtils.calculateDeepBrowserTreeIncoming;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE;
import static mega.privacy.android.app.utils.MegaNodeUtil.checkBackupNodeTypeByHandle;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.showSnackbar;
import static nz.mega.sdk.MegaShare.ACCESS_READ;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.featuretoggle.AppFeatures;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.listeners.CleanRubbishBinListener;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.RemoveVersionsListener;
import mega.privacy.android.app.listeners.ShareListener;
import mega.privacy.android.app.main.AddContactActivity;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.presentation.filelink.FileLinkActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.listeners.MultipleRequestListener;
import mega.privacy.android.app.main.megachat.AndroidMegaRichLinkMessage;
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity;
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity;
import mega.privacy.android.app.presentation.manager.model.SharesTab;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.domain.entity.Feature;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import timber.log.Timber;

public class NodeController {

    Context context;
    MegaApiAndroid megaApi;

    boolean isFolderLink = false;

    public NodeController(Context context) {
        Timber.d("NodeController created");
        this.context = context;
        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
    }

    public NodeController(Context context, boolean isFolderLink) {
        Timber.d("NodeController created");
        this.context = context;
        this.isFolderLink = isFolderLink;
        if (megaApi == null) {
            if (isFolderLink) {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApiFolder();
            } else {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
            }
        }
    }

    public void chooseLocationToCopyNodes(List<Long> handleList) {
        Timber.d("chooseLocationToCopyNodes");
        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i = 0; i < handleList.size(); i++) {
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        ((ManagerActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_COPY);
    }

    public void copyNodes(long[] copyHandles, long toHandle) {
        Timber.d("copyNodes");

        if (!isOnline(context)) {
            ((SnackbarShower) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(toHandle);
        if (parent != null) {
            MultipleRequestListener copyMultipleListener = null;
            if (copyHandles.length > 1) {
                Timber.d("Copy multiple files");
                copyMultipleListener = new MultipleRequestListener(MULTIPLE_COPY, context);
                for (int i = 0; i < copyHandles.length; i++) {
                    MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
                    if (cN != null) {
                        Timber.d("cN != null, i = %d of %d", i, copyHandles.length);
                        megaApi.copyNode(cN, parent, copyMultipleListener);
                    } else {
                        Timber.w("cN == null, i = %d of %d", i, copyHandles.length);
                    }
                }
            } else {
                Timber.d("Copy one file");
                MegaNode cN = megaApi.getNodeByHandle(copyHandles[0]);
                if (cN != null) {
                    Timber.d("cN != null");
                    megaApi.copyNode(cN, parent, (ManagerActivity) context);
                } else {
                    Timber.w("cN == null");
                    if (context instanceof ManagerActivity) {
                        ((ManagerActivity) context).copyError();
                    }
                }
            }
        }

    }

    public void chooseLocationToMoveNodes(List<Long> handleList) {
        Timber.d("chooseLocationToMoveNodes");
        Intent intent = new Intent(context, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i = 0; i < handleList.size(); i++) {
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        ((ManagerActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_MOVE);
    }

    public void checkIfNodesAreMine(List<MegaNode> nodes, ArrayList<MegaNode> ownerNodes, ArrayList<MegaNode> notOwnerNodes) {
        MegaNode currentNode;

        for (int i = 0; i < nodes.size(); i++) {
            currentNode = nodes.get(i);
            if (currentNode == null) continue;

            MegaNode nodeOwner = checkIfNodeIsMine(currentNode);

            if (nodeOwner != null) {
                ownerNodes.add(nodeOwner);
            } else {
                notOwnerNodes.add(currentNode);
            }
        }
    }

    public MegaNode checkIfNodeIsMine(MegaNode node) {
        long myUserHandle = megaApi.getMyUserHandleBinary();

        if (node.getOwner() == myUserHandle) {
            return node;
        }

        ArrayList<MegaNode> fNodes = megaApi.getNodesByFingerprint(node.getFingerprint());

        if (fNodes == null) return null;

        for (MegaNode n : fNodes) {
            if (n.getOwner() == myUserHandle) {
                return n;
            }
        }

        return null;
    }

    /**
     * Checks if the node is inside an incoming folder.
     *
     * @param node The node to check.
     * @return True if the node comes from an incoming folder, false otherwise.
     */
    public boolean nodeComesFromIncoming(MegaNode node) {
        return node != null
                && !megaApi.isInCloud(node)
                && !megaApi.isInRubbish(node)
                && !megaApi.isInInbox(node);
    }

    public MegaNode getParent(MegaNode node) {
        return MegaNodeUtil.getRootParentNode(megaApi, node);
    }

    public int getIncomingLevel(MegaNode node) {
        int dBT = 0;
        MegaNode parent = node;

        while (megaApi.getParentNode(parent) != null) {
            dBT++;
            parent = megaApi.getParentNode(parent);
        }

        return dBT;
    }

    public int importLink(String url, Set<Feature> enabledFeatureFlags) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (Exception e) {
            Timber.e(e, "Error decoding URL: %s", url);
        }

        url.replace(' ', '+');
        if (url.startsWith("mega://")) {
            url = url.replace("mega://", "https://mega.co.nz/");
        }

        Timber.d("url %s", url);

        // Download link
        if (AndroidMegaRichLinkMessage.isFileLink(url)) {
            Intent openFileIntent;
            if (enabledFeatureFlags.contains(AppFeatures.FileLinkCompose)) {
                openFileIntent = new Intent(context, FileLinkComposeActivity.class);
            } else {
                openFileIntent = new Intent(context, FileLinkActivity.class);
            }
            openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
            openFileIntent.setData(Uri.parse(url));
            ((ManagerActivity) context).startActivity(openFileIntent);
            return FILE_LINK;
        } else if (AndroidMegaRichLinkMessage.isFolderLink(url)) {
            Intent openFolderIntent = new Intent(context, FolderLinkComposeActivity.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
            openFolderIntent.setData(Uri.parse(url));
            context.startActivity(openFolderIntent);
            return FOLDER_LINK;
        } else if (AndroidMegaRichLinkMessage.isChatLink(url)) {
            return CHAT_LINK;
        } else if (AndroidMegaRichLinkMessage.isContactLink(url)) {
            return CONTACT_LINK;
        }

        Timber.w("wrong url");
        return ERROR_LINK;
    }

    //old getPublicLinkAndShareIt
    public void exportLink(MegaNode document) {
        Timber.d("exportLink");
        if (!isOnline(context)) {
            showSnackbar(context, context.getString(R.string.error_server_connection_problem));
        } else if (context instanceof MegaRequestListenerInterface) {
            megaApi.exportNode(document, ((MegaRequestListenerInterface) context));
        }
    }

    public void exportLinkTimestamp(MegaNode document, int timestamp) {
        Timber.d("exportLinkTimestamp: %s", timestamp);
        if (!isOnline(context)) {
            showSnackbar(context, context.getString(R.string.error_server_connection_problem));
        } else if (context instanceof MegaRequestListenerInterface) {
            megaApi.exportNode(document, timestamp, ((MegaRequestListenerInterface) context));
        }
    }

    public void removeLink(MegaNode document, ExportListener exportListener) {
        megaApi.disableExport(document, exportListener);
    }

    public void removeLinks(ArrayList<MegaNode> nodes) {
        if (!isOnline(context)) {
            ((SnackbarShower) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        ExportListener exportListener = new ExportListener(context, nodes.size());

        for (MegaNode node : nodes) {
            removeLink(node, exportListener);
        }
    }


    public void selectContactToShareFolders(ArrayList<Long> handleList) {
        Timber.d("shareFolders ArrayListLong");


        if (!isOnline(context)) {
            ((SnackbarShower) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        Intent intent = new Intent();
        intent.setClass(context, AddContactActivity.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);

        long[] handles = new long[handleList.size()];
        int j = 0;
        for (int i = 0; i < handleList.size(); i++) {
            handles[j] = handleList.get(i);
            j++;
        }
        intent.putExtra(AddContactActivity.EXTRA_NODE_HANDLE, handles);
        //Multiselect=1 (multiple folders)
        intent.putExtra("MULTISELECT", 1);
        ((ManagerActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
    }

    public void selectContactToShareFolder(MegaNode node) {
        Timber.d("shareFolder");

        Intent intent = new Intent();
        intent.setClass(context, AddContactActivity.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);
        //Multiselect=0
        intent.putExtra("MULTISELECT", 0);
        intent.putExtra(AddContactActivity.EXTRA_NODE_HANDLE, node.getHandle());
        ((ManagerActivity) context).startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
    }

    public void openFolderFromSearch(long folderHandle) {
        Timber.d("openFolderFromSearch: %s", folderHandle);
        ((ManagerActivity) context).openFolderRefresh = true;
        boolean firstNavigationLevel = true;
        int access = -1;
        DrawerItem drawerItem = DrawerItem.CLOUD_DRIVE;
        if (folderHandle != -1) {
            MegaNode parentIntentN = megaApi.getParentNode(megaApi.getNodeByHandle(folderHandle));
            if (parentIntentN != null) {
                Timber.d("Check the parent node: %s handle: %d", parentIntentN.getName(), parentIntentN.getHandle());
                access = megaApi.getAccess(parentIntentN);
                switch (access) {
                    case MegaShare.ACCESS_OWNER:
                    case MegaShare.ACCESS_UNKNOWN: {
                        //Not incoming folder, check if Cloud or Rubbish tab
                        if (parentIntentN.getHandle() == megaApi.getRootNode().getHandle()) {
                            drawerItem = DrawerItem.CLOUD_DRIVE;
                            Timber.d("Navigate to TAB CLOUD first level%s", parentIntentN.getName());
                            firstNavigationLevel = true;
                            ((ManagerActivity) context).setParentHandleBrowser(parentIntentN.getHandle());
                        } else if (parentIntentN.getHandle() == megaApi.getRubbishNode().getHandle()) {
                            drawerItem = DrawerItem.RUBBISH_BIN;
                            Timber.d("Navigate to TAB RUBBISH first level%s", parentIntentN.getName());
                            firstNavigationLevel = true;
                            ((ManagerActivity) context).setParentHandleRubbish(parentIntentN.getHandle());
                        } else if (parentIntentN.getHandle() == megaApi.getInboxNode().getHandle()) {
                            Timber.d("Navigate to INBOX first level%s", parentIntentN.getName());
                            firstNavigationLevel = true;
                            ((ManagerActivity) context).setParentHandleInbox(parentIntentN.getHandle());
                            drawerItem = DrawerItem.INBOX;
                        } else {
                            int parent = checkParentNodeToOpenFolder(parentIntentN.getHandle());
                            Timber.d("The parent result is: %s", parent);

                            switch (parent) {
                                case 0: {
                                    //ROOT NODE
                                    drawerItem = DrawerItem.CLOUD_DRIVE;
                                    Timber.d("Navigate to TAB CLOUD with parentHandle");
                                    ((ManagerActivity) context).setParentHandleBrowser(parentIntentN.getHandle());
                                    firstNavigationLevel = false;
                                    break;
                                }
                                case 1: {
                                    Timber.d("Navigate to TAB RUBBISH");
                                    drawerItem = DrawerItem.RUBBISH_BIN;
                                    ((ManagerActivity) context).setParentHandleRubbish(parentIntentN.getHandle());
                                    firstNavigationLevel = false;
                                    break;
                                }
                                case 2: {
                                    Timber.d("Navigate to INBOX WITH parentHandle");
                                    drawerItem = DrawerItem.INBOX;
                                    ((ManagerActivity) context).setParentHandleInbox(parentIntentN.getHandle());
                                    firstNavigationLevel = false;
                                    break;
                                }
                                case -1: {
                                    drawerItem = DrawerItem.CLOUD_DRIVE;
                                    Timber.d("Navigate to TAB CLOUD general");
                                    ((ManagerActivity) context).setParentHandleBrowser(-1);
                                    firstNavigationLevel = true;
                                    break;
                                }
                            }
                        }
                        break;
                    }

                    case MegaShare.ACCESS_READ:
                    case MegaShare.ACCESS_READWRITE:
                    case MegaShare.ACCESS_FULL: {
                        Timber.d("GO to INCOMING TAB: %s", parentIntentN.getName());
                        drawerItem = DrawerItem.SHARED_ITEMS;
                        if (parentIntentN.getHandle() == -1) {
                            Timber.d("Level 0 of Incoming");
                            ((ManagerActivity) context).setDeepBrowserTreeIncoming(0, -1L);
                            firstNavigationLevel = true;
                        } else {
                            firstNavigationLevel = false;
                            int deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(parentIntentN, context);
                            ((ManagerActivity) context).setDeepBrowserTreeIncoming(deepBrowserTreeIncoming, parentIntentN.getHandle());
                            Timber.d("After calculating deepBrowserTreeIncoming: %s", deepBrowserTreeIncoming);
                        }
                        ((ManagerActivity) context).setTabItemShares(SharesTab.Companion.fromPosition(0));
                        break;
                    }
                    default: {
                        Timber.d("DEFAULT: The intent set the parentHandleBrowser to %s", parentIntentN.getHandle());
                        ((ManagerActivity) context).setParentHandleBrowser(parentIntentN.getHandle());
                        drawerItem = DrawerItem.CLOUD_DRIVE;
                        firstNavigationLevel = true;
                        break;
                    }
                }
            } else {
                Timber.w("Parent is already NULL");

                drawerItem = DrawerItem.SHARED_ITEMS;
                ((ManagerActivity) context).setDeepBrowserTreeIncoming(0, -1L);
                firstNavigationLevel = true;
                ((ManagerActivity) context).setTabItemShares(SharesTab.Companion.fromPosition(0));
            }
            ((ManagerActivity) context).setFirstNavigationLevel(firstNavigationLevel);
            ((ManagerActivity) context).setDrawerItem(drawerItem);
            ((ManagerActivity) context).selectDrawerItem(drawerItem);
        }
    }

    public int checkParentNodeToOpenFolder(long folderHandle) {
        Timber.d("Folder handle: %s", folderHandle);
        MegaNode folderNode = megaApi.getNodeByHandle(folderHandle);
        MegaNode parentNode = megaApi.getParentNode(folderNode);
        if (parentNode != null) {
            Timber.d("Parent handle: %s", parentNode.getHandle());
            if (parentNode.getHandle() == megaApi.getRootNode().getHandle()) {
                Timber.d("The parent is the ROOT");
                return 0;
            } else if (parentNode.getHandle() == megaApi.getRubbishNode().getHandle()) {
                Timber.d("The parent is the RUBBISH");
                return 1;
            } else if (parentNode.getHandle() == megaApi.getInboxNode().getHandle()) {
                Timber.d("The parent is the INBOX");
                return 2;
            } else if (parentNode.getHandle() == -1) {
                Timber.w("The parent is -1");
                return -1;
            } else {
                int result = checkParentNodeToOpenFolder(parentNode.getHandle());
                Timber.d("Call returns %s", result);
                switch (result) {
                    case -1:
                        return -1;
                    case 0:
                        return 0;
                    case 1:
                        return 1;
                    case 2:
                        return 2;
                }
            }
        }
        return -1;
    }

    public void removeShares(ArrayList<MegaShare> listShares, MegaNode node) {
        if (listShares == null || listShares.isEmpty()) return;

        ShareListener shareListener = new ShareListener(context, REMOVE_SHARE_LISTENER, listShares.size());

        for (MegaShare share : listShares) {
            String email = share.getUser();
            if (email != null) {
                removeShare(shareListener, node, email);
            }
        }
    }

    public void removeSeveralFolderShares(List<MegaNode> nodes) {
        ArrayList<MegaShare> totalShares = new ArrayList<>();

        for (MegaNode node : nodes) {
            ArrayList<MegaShare> shares = megaApi.getOutShares(node);
            if (shares != null && !shares.isEmpty()) {
                totalShares.addAll(shares);
            }
        }

        ShareListener shareListener = new ShareListener(context, REMOVE_SHARE_LISTENER, totalShares.size());

        for (MegaShare megaShare : totalShares) {
            MegaNode node = megaApi.getNodeByHandle(megaShare.getNodeHandle());
            String email = megaShare.getUser();
            if (node != null && email != null) {
                removeShare(shareListener, node, email);
            }
        }
    }

    public void removeShare(ShareListener shareListener, MegaNode node, String email) {
        megaApi.share(node, email, MegaShare.ACCESS_UNKNOWN, shareListener);
    }

    public void shareFolder(MegaNode node, ArrayList<String> selectedContacts, int permissions) {
        if (!isOnline(context)) {
            ((SnackbarShower) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        if (selectedContacts == null || selectedContacts.isEmpty()) return;

        ShareListener shareListener = new ShareListener(context, SHARE_LISTENER, selectedContacts.size());

        for (int i = 0; i < selectedContacts.size(); i++) {
            shareFolder(node, selectedContacts.get(i), permissions, shareListener);
        }
    }

    public void shareFolders(long[] nodeHandles, ArrayList<String> contactsData, int permissions) {

        if (!isOnline(context)) {
            ((SnackbarShower) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        if (nodeHandles == null || nodeHandles.length == 0) return;

        for (long nodeHandle : nodeHandles) {
            shareFolder(megaApi.getNodeByHandle(nodeHandle), contactsData, permissions);
        }
    }

    public void shareFolder(MegaNode node, String email, int permissions, ShareListener shareListener) {
        if (node == null || email == null) return;

        int nodeType = checkBackupNodeTypeByHandle(megaApi, node);
        Timber.d("MyBackup + shareFolders nodeType = %s", nodeType);

        if (nodeType == BACKUP_NONE) {
            megaApi.share(node, email, permissions, shareListener);
        } else {
            megaApi.share(node, email, ACCESS_READ, shareListener);
        }

    }

    public void cleanRubbishBin() {
        Timber.d("cleanRubbishBin");
        megaApi.cleanRubbishBin(new CleanRubbishBinListener(context));
    }

    public void clearAllVersions() {
        Timber.d("clearAllVersions");
        megaApi.removeVersions(new RemoveVersionsListener(context));
    }
}
