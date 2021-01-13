package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.WebViewActivity;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRecentActionBucket;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_DESTROY_ACTION_MODE;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;
import static nz.mega.sdk.MegaShare.ACCESS_FULL;

public class MegaNodeUtil {

    /**
     * The method to calculate how many nodes are folders in array list
     *
     * @param nodes the nodes to be calculated
     * @return how many nodes are folders in array list
     */
    public static int getNumberOfFolders(ArrayList<MegaNode> nodes) {

        int folderCount = 0;

        if (nodes == null) return folderCount;

        CopyOnWriteArrayList<MegaNode> safeList = new CopyOnWriteArrayList<>(nodes);

        for (MegaNode node : safeList) {
            if (node == null) {
                safeList.remove(node);
            } else if (node.isFolder()) {
                folderCount++;
            }
        }

        nodes = new ArrayList<>(safeList);
        return folderCount;
    }

    /**
     * @param node the detected node
     * @return whether the node is taken down
     */
    public static boolean isNodeTakenDown(MegaNode node) {
        return node != null && node.isTakenDown();
    }


    /**
     * If the node is taken down, and try to execute action against the node,
     * such as manage link, remove link, show the alert dialog
     *
     * @param node the detected node
     * @return whether show the dialog for the mega node or not
     */
    public static boolean showTakenDownNodeActionNotAvailableDialog(MegaNode node, Context context) {
        if (isNodeTakenDown(node)) {
            showSnackbar(context, context.getString(R.string.error_download_takendown_node));
            return true;
        } else {
            return false;
        }
    }

    /**
     * The static class to show taken down notice for mega node when the node is taken down and try to be opened in the preview
     */
    public static class NodeTakenDownAlertHandler {
        /**
         * alertTakenDown is the dialog to be shown. It resides inside this static class to prevent multiple definition within the activity class
         */
        private static AlertDialog alertTakenDown = null;

        /**
         * Shows a taken down alert.
         *
         * @param activity the activity is the page where dialog is shown
         */
        public static void showTakenDownAlert(final AppCompatActivity activity) {

            if (activity == null
                    || activity.isFinishing()
                    || (alertTakenDown != null && alertTakenDown.isShowing())) {
                return;
            }

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            dialogBuilder.setTitle(getString(R.string.general_not_available))
                    .setMessage(getString(R.string.error_download_takendown_node))
                    .setNegativeButton(R.string.general_dismiss, (dialog, i) -> activity.finish());

            alertTakenDown = dialogBuilder.create();
            alertTakenDown.setCancelable(false);
            alertTakenDown.show();
        }
    }

    /**
     * The static class to show taken down dialog for mega node when the node is taken down and be clicked in adapter
     */
    public static class NodeTakenDownDialogHandler {

        /**
         * The listener to handle button click events
         */
        public interface nodeTakenDownDialogListener {
            void onOpenClicked(int currentPosition, View view);

            void onDisputeClicked();

            void onCancelClicked();
        }

        /**
         * show dialog
         *
         * @param isFolder        the clicked node
         * @param view            the view in the adapter which triggers the click event
         * @param currentPosition the view position in adapter
         * @param listener        the listener to handle all clicking event
         * @param context         the context where adapter resides
         * @return the dialog object to be handled by adapter to be dismissed, in case of window leaking situation
         */
        public static AlertDialog showTakenDownDialog(boolean isFolder, final View view, final int currentPosition, nodeTakenDownDialogListener listener, Context context) {
            int alertMessageID = isFolder ? R.string.message_folder_takedown_pop_out_notification : R.string.message_file_takedown_pop_out_notification;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.dialog_three_vertical_buttons, null);
            builder.setView(v);

            TextView title = v.findViewById(R.id.dialog_title);
            TextView text = v.findViewById(R.id.dialog_text);

            Button openButton = v.findViewById(R.id.dialog_first_button);
            Button disputeButton = v.findViewById(R.id.dialog_second_button);
            Button cancelButton = v.findViewById(R.id.dialog_third_button);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.RIGHT;

            title.setText(R.string.general_error_word);
            text.setText(alertMessageID);
            openButton.setText(R.string.context_open_link);
            disputeButton.setText(R.string.dispute_takendown_file);
            cancelButton.setText(R.string.general_cancel);

            final AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            openButton.setOnClickListener(button -> {
                listener.onOpenClicked(currentPosition, view);
                dialog.dismiss();
            });

            disputeButton.setOnClickListener(button -> {
                listener.onDisputeClicked();
                Intent openTermsIntent = new Intent(context, WebViewActivity.class);
                openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                openTermsIntent.setData(Uri.parse(DISPUTE_URL));
                context.startActivity(openTermsIntent);
                dialog.dismiss();
            });

            cancelButton.setOnClickListener(button -> {
                listener.onCancelClicked();
                dialog.dismiss();
            });

            dialog.show();

            return dialog;
        }
    }

    /**
     * Gets the root parent folder of a node.
     *
     * @param node  MegaNode to get its root parent path
     * @return The path of the root parent of the node.
     */
    public static String getParentFolderPath(MegaNode node) {
        if (node != null) {
            MegaApplication app = MegaApplication.getInstance();
            MegaApiAndroid megaApi = app.getMegaApi();
            String path = megaApi.getNodePath(node);

            while (megaApi.getParentNode(node) != null) {
                node = megaApi.getParentNode(node);
            }

            if (node.getHandle() == megaApi.getRootNode().getHandle()) {
                return app.getString(R.string.section_cloud_drive) + path;
            } else if (node.getHandle() == megaApi.getRubbishNode().getHandle()) {
                return app.getString(R.string.section_rubbish_bin) + path.replace("bin" + SEPARATOR, "");
            } else if (node.isInShare()) {
                return app.getString(R.string.title_incoming_shares_explorer) + SEPARATOR + path.substring(path.indexOf(":") + 1);
            }
        }

        return "";
    }

    /**
     *
     * Shares a node.
     * If the node is a folder creates and/or shares the folder link.
     * If the node is a file and exists in local storage, shares the file. If not, creates and/or shares the file link.
     *
     * @param context   current Context.
     * @param node      node to share.
     */
    public static void shareNode(Context context, MegaNode node) {
        if (shouldContinueWithoutError(context, "sharing node", node)) {
            String path = getLocalFile(context, node.getName(), node.getSize());

            if (!isTextEmpty(path) && !node.isFolder()) {
                shareFile(context, new File(path));
            } else if (node.isExported()) {
                startShareIntent(context, new Intent(android.content.Intent.ACTION_SEND), node.getPublicLink());
            } else {
                MegaApplication.getInstance().getMegaApi().exportNode(node, new ExportListener(context, new Intent(android.content.Intent.ACTION_SEND)));
            }
        }
    }

    /**
     * Share multiple nodes out of MEGA app.
     *
     * If a folder is involved, we will share links of all nodes.
     *
     * Other apps can't handle the mixture of link and file, so if there is any file that is not
     * downloaded, we will share links of all files.
     *
     * @param context the context where nodes are shared
     * @param nodes nodes to share
     */
    public static void shareNodes(Context context, List<MegaNode> nodes) {
        if (!shouldContinueWithoutError(context, "sharing nodes", nodes)) {
            return;
        }
        List<File> downloadedFiles = new ArrayList<>();
        boolean allDownloadedFiles = true;
        for (MegaNode node : nodes) {
            String path = node.isFolder() ? null
                : getLocalFile(context, node.getName(), node.getSize());
            if (isTextEmpty(path)) {
                allDownloadedFiles = false;
                break;
            } else {
                downloadedFiles.add(new File(path));
            }
        }
        if (allDownloadedFiles) {
            shareFiles(context, downloadedFiles);
            return;
        }

        int notExportedNodes = 0;
        StringBuilder links = new StringBuilder();
        for (MegaNode node : nodes) {
            if (!node.isExported()) {
                notExportedNodes++;
            } else {
                links.append(node.getPublicLink())
                    .append("\n\n");
            }
        }
        if (notExportedNodes == 0) {
            startShareIntent(context, new Intent(android.content.Intent.ACTION_SEND),
                links.toString());
            return;
        }

        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        ExportListener exportListener = new ExportListener(context, notExportedNodes, links,
            new Intent(android.content.Intent.ACTION_SEND));
        for (MegaNode node : nodes) {
            if (!node.isExported()) {
                megaApi.exportNode(node, exportListener);
            }
        }
    }

    /**
     * Shares a link.
     *
     * @param context   current Context.
     * @param fileLink  link to share.
     */
    public static void shareLink(Context context, String fileLink) {
        startShareIntent(context, new Intent(android.content.Intent.ACTION_SEND), fileLink);
    }

    /**
     * Ends the creation of the share intent and starts it.
     *
     * @param context       current Context.
     * @param shareIntent   intent to start the share.
     * @param link          link of the node to share.
     */
    public static void startShareIntent (Context context, Intent shareIntent, String link) {
        shareIntent.setType(TYPE_TEXT_PLAIN);
        shareIntent.putExtra(Intent.EXTRA_TEXT, link);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param message   action being taken.
     * @param node      node involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    private static boolean shouldContinueWithoutError(Context context, String message, MegaNode node) {
        String error = "Error " + message + ". ";

        if (node == null) {
            logError(error + "Node == NULL");
            return false;
        } else if (!isOnline(context)) {
            logError(error + "No network connection");
            showSnackbar(context, context.getString(R.string.error_server_connection_problem));
            return false;
        }

        return true;
    }

    /**
     * Checks if there is any error before continues any action.
     *
     * @param context   current Context.
     * @param message   action being taken.
     * @param nodes      nodes involved in the action.
     * @return True if there is not any error, false otherwise.
     */
    private static boolean shouldContinueWithoutError(Context context, String message,
        List<MegaNode> nodes) {
        String error = "Error " + message + ". ";

        if (nodes == null || nodes.isEmpty()) {
            logError(error + "no nodes");
            return false;
        } else if (!isOnline(context)) {
            logError(error + "No network connection");
            showSnackbar(context, context.getString(R.string.error_server_connection_problem));
            return false;
        }

        return true;
    }

    /**
     * Checks if a MegaNode is the user attribute "My chat files"
     *
     * @param node MegaNode to check
     * @return True if the node is "My chat files" attribute, false otherwise
     */
    public static boolean isMyChatFilesFolder(MegaNode node) {
        MegaApplication megaApplication = MegaApplication.getInstance();

        return node != null && node.getHandle() != INVALID_HANDLE && !megaApplication.getMegaApi().isInRubbish(node)
                && existsMyChatFilesFolder() && node.getHandle() == megaApplication.getDbH().getMyChatFilesFolderHandle();
    }

    /**
     * Checks if the user attribute "My chat files" is saved in DB and exists
     *
     * @return True if the the user attribute "My chat files" is saved in the DB, false otherwise
     */
    public static boolean existsMyChatFilesFolder() {
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        MegaApiJava megaApi = MegaApplication.getInstance().getMegaApi();

        if (dbH != null && dbH.getMyChatFilesFolderHandle() != INVALID_HANDLE) {
            MegaNode myChatFilesFolder = megaApi.getNodeByHandle(dbH.getMyChatFilesFolderHandle());

            return myChatFilesFolder != null && myChatFilesFolder.getHandle() != INVALID_HANDLE && !megaApi.isInRubbish(myChatFilesFolder);
        }

        return false;
    }

    /**
     * Gets the node of the user attribute "My chat files" from the DB.
     *
     * Before call this method is neccesary to call existsMyChatFilesFolder() method
     *
     * @return "My chat files" folder node
     * @see MegaNodeUtil#existsMyChatFilesFolder()
     */
    public static MegaNode getMyChatFilesFolder() {
        return MegaApplication.getInstance().getMegaApi().getNodeByHandle(MegaApplication.getInstance().getDbH().getMyChatFilesFolderHandle());
    }

    /**
     * Checks if a node is "Camera Uploads" or "Media Uploads" folder.
     *
     * Note: The content of this method is temporary and will have to be modified when the PR of the CU user attribute be merged.
     *
     * @param n MegaNode to check
     * @return True if the node is "Camera Uploads" or "Media Uploads" folder, false otherwise
     */
    public static boolean isCameraUploads(MegaNode n) {
        String cameraSyncHandle = null;
        String secondaryMediaHandle = null;
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        MegaPreferences prefs = dbH.getPreferences();

        //Check if the item is the Camera Uploads folder
        if (prefs != null && prefs.getCamSyncHandle() != null) {
            cameraSyncHandle = prefs.getCamSyncHandle();
        }

        long handle = n.getHandle();

        if (cameraSyncHandle != null && !cameraSyncHandle.isEmpty()
                && handle == Long.parseLong(cameraSyncHandle) && !isNodeInRubbishOrDeleted(handle) ) {
            return true;
        }

        //Check if the item is the Media Uploads folder
        if (prefs != null && prefs.getMegaHandleSecondaryFolder() != null) {
            secondaryMediaHandle = prefs.getMegaHandleSecondaryFolder();
        }

        return secondaryMediaHandle != null && !secondaryMediaHandle.isEmpty()
                && handle == Long.parseLong(secondaryMediaHandle) && !isNodeInRubbishOrDeleted(handle);
    }

    /**
     * Checks if a node is  outgoing or a pending outgoing share.
     *
     * @param node MegaNode to check
     * @return True if the node is a outgoing or a pending outgoing share, false otherwise
     */
    public static boolean isOutShare(MegaNode node) {
        return node.isOutShare() || MegaApplication.getInstance().getMegaApi().isPendingShare(node);
    }

    /**
     * Gets the the icon that has to be displayed for a folder.
     *
     * @param node          MegaNode referencing the folder to check
     * @param drawerItem    indicates if the icon has to be shown in Outgoing shares section or any other
     * @return The icon of the folder to be displayed.
     */
    public static int getFolderIcon(MegaNode node, ManagerActivityLollipop.DrawerItem drawerItem) {
        if (node.isInShare()) {
            return R.drawable.ic_folder_incoming;
        } else if (isCameraUploads(node)) {
            if (drawerItem == ManagerActivityLollipop.DrawerItem.SHARED_ITEMS && isOutShare(node)) {
                return R.drawable.ic_folder_outgoing;
            } else {
                return R.drawable.ic_folder_camera_uploads_list;
            }
        } else if (isMyChatFilesFolder(node)) {
            if (drawerItem == ManagerActivityLollipop.DrawerItem.SHARED_ITEMS && isOutShare(node)) {
                return R.drawable.ic_folder_outgoing;
            } else {
                return R.drawable.ic_folder_chat_list;
            }
        } else if (isOutShare(node)) {
            return R.drawable.ic_folder_outgoing;
        } else {
            return R.drawable.ic_folder_list;
        }
    }

    /**
     * Gets the parent MegaNode of the highest level in tree of the node passed by param.
     *
     * @param node  MegaNode to check
     * @return The root parent MegaNode
     */
    public static MegaNode getRootParentNode(MegaNode node) {
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();

        while (megaApi.getParentNode(node) != null) {
            node = megaApi.getParentNode(node);
        }

        return node;
    }

    /**
     * Checks if it is on Links section and in root level.
     *
     * @param adapterType   current section
     * @param parentHandle  current parent handle
     * @return true if it is on Links section and it is in root level, false otherwise
     */
    public static boolean isInRootLinksLevel(int adapterType, long parentHandle) {
        return adapterType == LINKS_ADAPTER && parentHandle == INVALID_HANDLE;
    }

    /*
     * Checks if the Toolbar option "share" should be visible or not depending on the permissions of the MegaNode
     *
     * @param adapterType   view in which is required the check
     * @param isFolderLink  if true, the node comes from a folder link
     * @param handle        identifier of the MegaNode to check
     * @return True if the option "share" should be visible, false otherwise
     */
    public static boolean showShareOption(int adapterType, boolean isFolderLink, long handle) {
        if (isFolderLink) {
            return false;
        } else if (adapterType != OFFLINE_ADAPTER && adapterType != ZIP_ADAPTER && adapterType != FILE_LINK_ADAPTER) {
            MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
            MegaNode node = megaApi.getNodeByHandle(handle);

            return node != null && megaApi.getAccess(node) == MegaShare.ACCESS_OWNER;
        }

        return true;
    }

    /**
     * This method is to detect whether the node exist and in rubbish bean
     * @param handle node's handle to be detected
     * @return whether the node is in rubbish
     */
    public static boolean isNodeInRubbish(long handle){
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        MegaNode node =  megaApi.getNodeByHandle(handle);
        return node != null && megaApi.isInRubbish(node);
    }

    /**
     * This method is to detect whether the node has been deleted completely
     * or in rubbish bin
     * @param handle node's handle to be detected
     * @return whether the node is in rubbish
     */
    public static boolean isNodeInRubbishOrDeleted(long handle){
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        MegaNode node =  megaApi.getNodeByHandle(handle);
        return node == null || megaApi.isInRubbish(node);
    }

    /**
     * Gets the parent outgoing or incoming MegaNode folder of a node.
     *
     * @param node  MegaNode to get its parent
     * @return The outgoing or incoming parent folder.
     */
    public static MegaNode getOutgoingOrIncomingParent(MegaNode node) {
        if (isOutgoingOrIncomingFolder(node)) {
            return node;
        }

        MegaNode parentNode = node;
        MegaApiJava megaApi = MegaApplication.getInstance().getMegaApi();

        while (megaApi.getParentNode(parentNode) != null) {
            parentNode = megaApi.getParentNode(parentNode);

            if (isOutgoingOrIncomingFolder(parentNode)) {
                return parentNode;
            }
        }

        return null;
    }

    /**
     * Checks if a node is an outgoing or an incoming folder.
     *
     * @param node  MegaNode to check
     * @return  True if the node is an outgoing or incoming folder, false otherwise.
     */
    private static boolean isOutgoingOrIncomingFolder(MegaNode node) {
        return node.isOutShare() || node.isInShare();
    }

    /*
     * Check if all nodes can be moved to rubbish bin.
     *
     * @param nodes nodes to check
     * @return whether all nodes can be moved to rubbish bin
     */
    public static boolean canMoveToRubbish(List<MegaNode> nodes) {
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        for (MegaNode node : nodes) {
            if (megaApi.checkMove(node, megaApi.getRubbishNode()).getErrorCode()
                != MegaError.API_OK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if all nodes are file nodes.
     *
     * @param nodes nodes to check
     * @return whether all nodes are file nodes
     */
    public static boolean areAllFileNodes(List<MegaNode> nodes) {
        for (MegaNode node : nodes) {
            if (!node.isFile()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if all nodes have full access.
     *
     * @param nodes nodes to check
     * @return whether all nodes have full access
     */
    public static boolean allHaveFullAccess(List<MegaNode> nodes) {
        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        for (MegaNode node : nodes) {
            if (megaApi.checkAccess(node, ACCESS_FULL).getErrorCode() != MegaError.API_OK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shows a confirmation warning before leave an incoming share.
     *
     * @param context   current Context
     * @param n         incoming share to leave
     */
    public static void showConfirmationLeaveIncomingShare(Context context, MegaNode n) {
        showConfirmationLeaveIncomingShares(context, n, null);
    }

    /**
     * Shows a confirmation warning before leave some incoming shares.
     *
     * @param context       current Context
     * @param handleList    handles list of the incoming shares to leave
     */
    public static void showConfirmationLeaveIncomingShares (Context context, ArrayList<Long> handleList){
        showConfirmationLeaveIncomingShares(context, null, handleList);
    }

    /**
     * Shows a confirmation warning before leave one or more incoming shares.
     *
     * @param context       current Context
     * @param n             if only one incoming share to leave, its node, null otherwise
     * @param handleList    if mode than one incoming shares to leave, list of its handles, null otherwise
     */
    private static void showConfirmationLeaveIncomingShares (Context context, MegaNode n, ArrayList<Long> handleList) {
        boolean onlyOneIncomingShare = n != null && handleList == null;
        int numIncomingShares = onlyOneIncomingShare ? 1 : handleList.size();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setMessage(context.getResources().getQuantityString(R.plurals.confirmation_leave_share_folder, numIncomingShares))
                .setPositiveButton(R.string.general_leave, (dialog, which) -> {
                    if (onlyOneIncomingShare) {
                        new NodeController(context).leaveIncomingShare(n);
                    } else {
                        new NodeController(context).leaveMultipleIncomingShares(handleList);
                    }

                    MegaApplication.getInstance().sendBroadcast(new Intent(BROADCAST_ACTION_DESTROY_ACTION_MODE));
                })
                .setNegativeButton(R.string.general_cancel, null).show();
    }

    /**
     * Checks if a folder node is empty.
     * If a folder is empty means although contains more folders inside,
     * all of them don't contain any file.
     *
     * @param node  MegaNode to check.
     * @return  True if the folder is folder and is empty, false otherwise.
     */
    public static boolean isEmptyFolder(MegaNode node) {
        if (node == null || node.isFile()) {
            return false;
        }

        MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
        List<MegaNode> children = megaApi.getChildren(node);

        if (children != null && !children.isEmpty()) {
            for (MegaNode child : children) {
                if (child == null) {
                    continue;
                }

                if (child.isFile() || !isEmptyFolder(child)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets the tinted circle Drawable for the provided {@link MegaNode} Label
     *
     * @param nodeLabel     {@link MegaNode} Label
     * @param resources     Android resources
     * @return              Drawable
     */
    public static Drawable getNodeLabelDrawable(int nodeLabel, Resources resources) {
        Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_circle_label, null);
        drawable.setTint(ResourcesCompat.getColor(resources, getNodeLabelColor(nodeLabel), null));
        return drawable;
    }

    /**
     * Gets the String resource reference for the provided {@link MegaNode} Label
     *
     * @param nodeLabel     {@link MegaNode} Label
     * @return              String resource reference
     */
    @StringRes
    public static int getNodeLabelText(int nodeLabel) {
        switch (nodeLabel) {
            case MegaNode.NODE_LBL_RED:
                return R.string.label_red;
            case MegaNode.NODE_LBL_ORANGE:
                return R.string.label_orange;
            case MegaNode.NODE_LBL_YELLOW:
                return R.string.label_yellow;
            case MegaNode.NODE_LBL_GREEN:
                return R.string.label_green;
            case MegaNode.NODE_LBL_BLUE:
                return R.string.label_blue;
            case MegaNode.NODE_LBL_PURPLE:
                return R.string.label_purple;
            default:
                return R.string.label_grey;
        }
    }

    /**
     * Gets the Color resource reference for the provided {@link MegaNode} Label
     *
     * @param nodeLabel     {@link MegaNode} Label
     * @return              Color resource reference
     */
    @ColorRes
    public static int getNodeLabelColor(int nodeLabel) {
        switch (nodeLabel) {
            case MegaNode.NODE_LBL_RED:
                return R.color.salmon_400_salmon_300;
            case MegaNode.NODE_LBL_ORANGE:
                return R.color.orange_400_orange_300;
            case MegaNode.NODE_LBL_YELLOW:
                return R.color.yellow_600_yellow_300;
            case MegaNode.NODE_LBL_GREEN:
                return R.color.green_400_green_300;
            case MegaNode.NODE_LBL_BLUE:
                return R.color.blue_300_blue_200;
            case MegaNode.NODE_LBL_PURPLE:
                return R.color.purple_300_purple_200;
            default:
                return R.color.grey_300;
        }
    }
}
