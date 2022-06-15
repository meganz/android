package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.WebViewActivity;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.main.FileContactListActivity;
import mega.privacy.android.app.main.FileInfoActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.VersionsFileActivity;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.utils.AlertDialogUtil;
import mega.privacy.android.app.utils.MegaNodeUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.ViewUtils;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.main.ManagerActivity.INCOMING_TAB;
import static mega.privacy.android.app.main.ManagerActivity.LINKS_TAB;
import static mega.privacy.android.app.main.ManagerActivity.OUTGOING_TAB;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.ACTION_BACKUP_SHARE_FOLDER;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.BACKUP_NONE;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class NodeOptionsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {
    /** The "modes" are defined to allow the client to specify the dialog style more flexibly.
    At the same time, compatible with old code. For which mode corresponds to which dialog style,
     please refer to the code */
    /** No definite mode, map the drawerItem to a specific mode */
    public static final int DEFAULT_MODE = 0;
    /** For Cloud Drive */
    public static final int CLOUD_DRIVE_MODE = 1;
    /** For Rubbish Bin */
    public static final int RUBBISH_BIN_MODE = 2;
    /** For Inbox */
    public static final int INBOX_MODE = 3;
    /** For Shared items */
    public static final int SHARED_ITEMS_MODE = 4;
    /** For Search */
    public static final int SEARCH_MODE = 5;
    /** For Recents */
    public static final int RECENTS_MODE = 6;
    /** For Favourites of HomePage tab */
    public static final int FAVOURITES_IN_TAB_MODE = 7;
    /** For Favourites */
    public static final int FAVOURITES_MODE = 8;

    private static final String SAVED_STATE_KEY_MODE = "MODE";

    private static final String CANNOT_OPEN_FILE_SHOWN = "CANNOT_OPEN_FILE_SHOWN";

    private int mMode;

    private MegaNode node = null;
    private NodeController nC;

    private TextView nodeInfo;

    private DrawerItem drawerItem;

    private AlertDialog cannotOpenFileDialog;

    public NodeOptionsBottomSheetDialogFragment(int mode) {
        if (mode >= DEFAULT_MODE && mode <= FAVOURITES_MODE) {
            mMode = mode;
        }
    }

    public NodeOptionsBottomSheetDialogFragment() {
        mMode = DEFAULT_MODE;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_item, null);
        itemsLayout = contentView.findViewById(R.id.items_layout_bottom_sheet_node);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
            if (requireActivity() instanceof ManagerActivity) {
                drawerItem = ((ManagerActivity) requireActivity()).getDrawerItem();
            }
            mMode = savedInstanceState.getInt(SAVED_STATE_KEY_MODE, DEFAULT_MODE);
        } else {
            if (requireActivity() instanceof ManagerActivity) {
                node = ((ManagerActivity) requireActivity()).getSelectedNode();
                drawerItem = ((ManagerActivity) requireActivity()).getDrawerItem();
            }
        }

        nC = new NodeController(requireActivity());

        if (megaApi.isInRubbish(node)) {
            mMode = RUBBISH_BIN_MODE;
        } else if (nC.nodeComesFromIncoming(node)) {
            mMode = SHARED_ITEMS_MODE;
        }

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ImageView nodeThumb = contentView.findViewById(R.id.node_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.node_name_text);

        nodeInfo = contentView.findViewById(R.id.node_info_text);
        ImageView nodeVersionsIcon = contentView.findViewById(R.id.node_info_versions_icon);
        ImageView permissionsIcon = contentView.findViewById(R.id.permissions_icon);

        LinearLayout optionEdit = contentView.findViewById(R.id.edit_file_option);

        TextView optionInfo = contentView.findViewById(R.id.properties_option);
        // option Versions
        LinearLayout optionVersionsLayout = contentView.findViewById(R.id.option_versions_layout);
        TextView versions = contentView.findViewById(R.id.versions);
//      optionFavourite
        TextView optionFavourite = contentView.findViewById(R.id.favorite_option);
//      optionLabel
        LinearLayout optionLabel = contentView.findViewById(R.id.option_label_layout);
        TextView optionLabelCurrent = contentView.findViewById(R.id.option_label_current);
//      counterSave
        TextView optionDownload = contentView.findViewById(R.id.download_option);
        LinearLayout optionOffline = contentView.findViewById(R.id.option_offline_layout);
        SwitchMaterial offlineSwitch = contentView.findViewById(R.id.file_properties_switch);
//      counterShares
        TextView optionLink = contentView.findViewById(R.id.link_option);
        TextView optionRemoveLink = contentView.findViewById(R.id.remove_link_option);
        TextView optionShare = contentView.findViewById(R.id.share_option);
        TextView optionShareFolder = contentView.findViewById(R.id.share_folder_option);
        TextView optionClearShares = contentView.findViewById(R.id.clear_share_option);
        TextView optionSendChat = contentView.findViewById(R.id.send_chat_option);
//      counterModify
        TextView optionRename = contentView.findViewById(R.id.rename_option);
        TextView optionMove = contentView.findViewById(R.id.move_option);
        TextView optionCopy = contentView.findViewById(R.id.copy_option);
        TextView optionRestoreFromRubbish = contentView.findViewById(R.id.restore_option);
//      counterOpen
        TextView optionOpenFolder = contentView.findViewById(R.id.open_folder_option);
        TextView optionOpenWith = contentView.findViewById(R.id.open_with_option);
//      counterRemove
        TextView optionLeaveShares = contentView.findViewById(R.id.leave_share_option);
        TextView optionRubbishBin = contentView.findViewById(R.id.rubbish_bin_option);
        TextView optionRemove = contentView.findViewById(R.id.remove_option);
//      backup
        RelativeLayout optionMoveBackup = contentView.findViewById(R.id.option_backup_move_layout);
        TextView optionCopyBackup = contentView.findViewById(R.id.backup_copy_option);
        RelativeLayout optionRubbishBinBackup = contentView.findViewById(R.id.option_backup_rubbish_bin_layout);

        optionEdit.setOnClickListener(this);
        optionLabel.setOnClickListener(this);
        optionFavourite.setOnClickListener(this);
        optionDownload.setOnClickListener(this);
        optionOffline.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionLink.setOnClickListener(this);
        optionRemoveLink.setOnClickListener(this);
        optionShare.setOnClickListener(this);
        optionShareFolder.setOnClickListener(this);
        optionClearShares.setOnClickListener(this);
        optionLeaveShares.setOnClickListener(this);
        optionRename.setOnClickListener(this);
        optionSendChat.setOnClickListener(this);
        optionMove.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionRubbishBin.setOnClickListener(this);
        optionRestoreFromRubbish.setOnClickListener(this);
        optionRemove.setOnClickListener(this);
        optionOpenFolder.setOnClickListener(this);
        optionOpenWith.setOnClickListener(this);
        optionMoveBackup.setOnClickListener(this);
        optionCopyBackup.setOnClickListener(this);
        optionRubbishBinBackup.setOnClickListener(this);
        optionVersionsLayout.setOnClickListener(this);

        TextView viewInFolder = contentView.findViewById(R.id.view_in_folder_option);
        if (mMode == RECENTS_MODE || mMode == FAVOURITES_IN_TAB_MODE) {
            viewInFolder.setVisibility(View.VISIBLE);
            viewInFolder.setOnClickListener(this);
        } else {
            viewInFolder.setVisibility(View.GONE);
            viewInFolder.setOnClickListener(null);
        }

        int counterOpen = 2;
        int counterSave = 2;
        int counterShares = 6;
        int counterModify = 4;

        LinearLayout separatorOpen = contentView.findViewById(R.id.separator_open_options);
        LinearLayout separatorDownload = contentView.findViewById(R.id.separator_download_options);
        LinearLayout separatorShares = contentView.findViewById(R.id.separator_share_options);
        LinearLayout separatorModify = contentView.findViewById(R.id.separator_modify_options);

        if (!isScreenInPortrait(requireContext())) {
            logDebug("Landscape configuration");
            nodeName.setMaxWidth(scaleWidthPx(275, getResources().getDisplayMetrics()));
            nodeInfo.setMaxWidth(scaleWidthPx(275, getResources().getDisplayMetrics()));
        } else {
            nodeName.setMaxWidth(scaleWidthPx(210, getResources().getDisplayMetrics()));
            nodeInfo.setMaxWidth(scaleWidthPx(210, getResources().getDisplayMetrics()));
        }

        if (node == null) return;

        boolean isTakenDown = node.isTakenDown();
        int accessLevel = megaApi.getAccess(node);

        if (node.isFile() && !isTakenDown) {
            optionOpenWith.setVisibility(View.VISIBLE);
        } else {
            counterOpen--;
            optionOpenWith.setVisibility(View.GONE);
        }

        if (isOnline(requireContext())) {
            nodeName.setText(node.getName());
            if (node.isFolder()) {
                optionVersionsLayout.setVisibility(View.GONE);
                nodeInfo.setText(getMegaNodeFolderInfo(node));
                nodeVersionsIcon.setVisibility(View.GONE);

                nodeThumb.setImageResource(getFolderIcon(node, drawerItem));

                if (isEmptyFolder(node)) {
                    counterSave--;
                    optionOffline.setVisibility(View.GONE);
                }

                counterShares--;
                optionSendChat.setVisibility(View.GONE);
            } else {
                if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())
                        && accessLevel >= MegaShare.ACCESS_READWRITE) {
                    optionEdit.setVisibility(View.VISIBLE);
                }

                nodeInfo.setText(getFileInfo(node));

                if (megaApi.hasVersions(node)) {
                    nodeVersionsIcon.setVisibility(View.VISIBLE);
                    optionVersionsLayout.setVisibility(View.VISIBLE);
                    versions.setText(String.valueOf(megaApi.getNumVersions(node)));
                } else {
                    nodeVersionsIcon.setVisibility(View.GONE);
                    optionVersionsLayout.setVisibility(View.GONE);
                }

                setNodeThumbnail(requireContext(), node, nodeThumb);

                if (isTakenDown)  {
                    counterShares--;
                    optionSendChat.setVisibility(View.GONE);
                } else {
                    optionSendChat.setVisibility(View.VISIBLE);
                }
            }
        }

        if (isTakenDown) {
            contentView.findViewById(R.id.dispute_option).setVisibility(View.VISIBLE);
            contentView.findViewById(R.id.dispute_option).setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), WebViewActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setData(Uri.parse(DISPUTE_URL)));

                dismiss();
            });

            counterSave--;
            optionDownload.setVisibility(View.GONE);
            if (ViewUtils.isVisible(optionOffline)) {
                counterSave--;
                optionOffline.setVisibility(View.GONE);
            }
        } else {
            offlineSwitch.setChecked(availableOffline(requireContext(), node));
        }

        optionLabel.setVisibility(isTakenDown ? View.GONE : View.VISIBLE);
        optionFavourite.setVisibility(isTakenDown ? View.GONE : View.VISIBLE);

        if (accessLevel != MegaShare.ACCESS_OWNER || isTakenDown) {
            counterShares--;
            optionShare.setVisibility(View.GONE);
        }

        if (node.isFolder()) {
            if (isTakenDown) {
                counterShares--;
                optionShareFolder.setVisibility(View.GONE);
                counterShares--;
                optionClearShares.setVisibility(View.GONE);
            } else {
                optionShareFolder.setVisibility(View.VISIBLE);

                if (isOutShare(node)) {
                    optionShareFolder.setText(R.string.manage_share);
                    optionClearShares.setVisibility(View.VISIBLE);
                } else {
                    optionShareFolder.setText(R.string.context_share_folder);
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                }
            }
        } else {
            counterShares--;
            optionShareFolder.setVisibility(View.GONE);
            counterShares--;
            optionClearShares.setVisibility(View.GONE);
        }

        if (isTakenDown) {
            counterShares--;
            optionLink.setVisibility(View.GONE);
            counterShares--;
            optionRemoveLink.setVisibility(View.GONE);
            counterModify--;
            optionCopy.setVisibility(View.GONE);
        } else {
            optionLink.setVisibility(View.VISIBLE);

            if (node.isExported()) {
                //Node has public link
                optionLink.setText(R.string.edit_link_option);
                optionRemoveLink.setVisibility(View.VISIBLE);
            } else {
                optionLink.setText(StringResourcesUtils.getQuantityString(R.plurals.get_links, 1));
                counterShares--;
                optionRemoveLink.setVisibility(View.GONE);
            }
        }

        if (mMode == DEFAULT_MODE) {
            mapDrawerItemToMode(drawerItem);
        }

        switch (mMode) {
            case CLOUD_DRIVE_MODE:
            case INBOX_MODE:
            case SEARCH_MODE:
                logDebug("show Cloud bottom sheet");

                // Check if sub folder of "My Backup"
                ArrayList<Long> handleList = new ArrayList<>();
                handleList.add(node.getHandle());
                int nodeType = checkBackupNodeTypeInList(megaApi, handleList);
                if (nodeType != BACKUP_NONE) {
                    counterModify--;
                    optionRename.setVisibility(View.GONE);
                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    if (ViewUtils.isVisible(optionCopy)) {
                        counterModify--;
                        optionCopy.setVisibility(View.GONE);
                    }
                    optionRubbishBin.setVisibility(View.GONE);

                    optionMoveBackup.setVisibility(View.VISIBLE);
                    optionCopyBackup.setVisibility(isTakenDown ? View.GONE : View.VISIBLE);
                    optionRubbishBinBackup.setVisibility(View.VISIBLE);
                }

                optionRemove.setVisibility(View.GONE);
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);
                break;

            case RUBBISH_BIN_MODE:
                logDebug("show Rubbish bottom sheet");

                optionEdit.setVisibility(View.GONE);

                long restoreHandle = node.getRestoreHandle();
                MegaNode restoreNode = megaApi.getNodeByHandle(restoreHandle);
                if (restoreHandle == INVALID_HANDLE || !megaApi.isInRubbish(node)
                        || restoreNode == null || megaApi.isInRubbish(restoreNode)) {
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);
                }

                optionLabel.setVisibility(View.GONE);
                optionFavourite.setVisibility(View.GONE);

                if (ViewUtils.isVisible(optionOpenWith)) {
                    counterOpen--;
                    optionOpenWith.setVisibility(View.GONE);
                }
                counterModify--;
                optionMove.setVisibility(View.GONE);
                counterModify--;
                optionRename.setVisibility(View.GONE);
                if (ViewUtils.isVisible(optionCopy)) {
                    counterModify--;
                    optionCopy.setVisibility(View.GONE);
                }
                if (ViewUtils.isVisible(optionClearShares)) {
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                }
                optionLeaveShares.setVisibility(View.GONE);
                optionRubbishBin.setVisibility(View.GONE);

                if (ViewUtils.isVisible(optionShare)) {
                    counterShares--;
                    optionShare.setVisibility(View.GONE);
                }
                if (ViewUtils.isVisible(optionShareFolder)) {
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                }
                if (ViewUtils.isVisible(optionLink)) {
                    counterShares--;
                    optionLink.setVisibility(View.GONE);
                }
                if (ViewUtils.isVisible(optionRemoveLink)) {
                    counterShares--;
                    optionRemoveLink.setVisibility(View.GONE);
                }
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                if (ViewUtils.isVisible(optionDownload)) {
                    counterSave--;
                    optionDownload.setVisibility(View.GONE);
                }
                if (ViewUtils.isVisible(optionOffline)) {
                    counterSave--;
                    optionOffline.setVisibility(View.GONE);
                }
                if (ViewUtils.isVisible(optionSendChat)) {
                    counterShares--;
                    optionSendChat.setVisibility(View.GONE);
                }
                break;

            case SHARED_ITEMS_MODE:
                int tabSelected = ((ManagerActivity) requireActivity()).getTabItemShares();
                if (tabSelected == 0 || nC.nodeComesFromIncoming(node)) {
                    logDebug("showOptionsPanelIncoming");

                    optionRemove.setVisibility(View.GONE);
                    if (ViewUtils.isVisible(optionShareFolder)) {
                        counterShares--;
                        optionShareFolder.setVisibility(View.GONE);
                    }

                    if (ViewUtils.isVisible(optionClearShares)) {
                        counterShares--;
                        optionClearShares.setVisibility(View.GONE);
                    }

                    int dBT = nC.getIncomingLevel(node);
                    logDebug("DeepTree value:" + dBT);

                    if (dBT > FIRST_NAVIGATION_LEVEL) {
                        optionLeaveShares.setVisibility(View.GONE);
                    } else {
                        //Show the owner of the shared folder
                        showOwnerSharedFolder();
                        optionLeaveShares.setVisibility(isTakenDown ? View.GONE : View.VISIBLE);
                        permissionsIcon.setVisibility(View.VISIBLE);

                        switch (accessLevel) {
                            case MegaShare.ACCESS_FULL:
                                logDebug("LEVEL 0 - access FULL");
                                permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                                break;

                            case MegaShare.ACCESS_READ:
                                logDebug("LEVEL 0 - access read");
                                permissionsIcon.setImageResource(R.drawable.ic_shared_read);
                                break;

                            case MegaShare.ACCESS_READWRITE:
                                logDebug("LEVEL 0 - readwrite");
                                permissionsIcon.setImageResource(R.drawable.ic_shared_read_write);
                                break;
                        }
                    }

                    if (ViewUtils.isVisible(optionLink)) {
                        counterShares--;
                        optionLink.setVisibility(View.GONE);
                    }

                    if (ViewUtils.isVisible(optionRemoveLink)) {
                        counterShares--;
                        optionRemoveLink.setVisibility(View.GONE);
                    }

                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL:
                            logDebug("access FULL");
                            if (dBT <= FIRST_NAVIGATION_LEVEL) {
                                optionRubbishBin.setVisibility(View.GONE);
                                counterModify--;
                                optionMove.setVisibility(View.GONE);
                            }

                            break;

                        case MegaShare.ACCESS_READ:
                            logDebug("access read");
                            optionLabel.setVisibility(View.GONE);
                            optionFavourite.setVisibility(View.GONE);
                            counterModify--;
                            optionRename.setVisibility(View.GONE);
                            counterModify--;
                            optionMove.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            break;

                        case MegaShare.ACCESS_READWRITE:
                            logDebug("readwrite");
                            optionLabel.setVisibility(View.GONE);
                            optionFavourite.setVisibility(View.GONE);
                            counterModify--;
                            optionRename.setVisibility(View.GONE);
                            counterModify--;
                            optionMove.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            break;
                    }
                } else if (tabSelected == 1) {
                    logDebug("showOptionsPanelOutgoing");

                    if (!isTakenDown && ((ManagerActivity) requireActivity()).getDeepBrowserTreeOutgoing() == FIRST_NAVIGATION_LEVEL
                            && ViewUtils.isVisible(optionClearShares)) {
                        //Show the number of contacts who shared the folder
                        ArrayList<MegaShare> sl = megaApi.getOutShares(node);
                        if (sl != null) {
                            if (sl.size() != 0) {
                                nodeInfo.setText(getQuantityString(R.plurals.general_num_shared_with,
                                        sl.size(), sl.size()));
                            }
                        }
                    } else if (ViewUtils.isVisible(optionClearShares)) {
                        counterShares--;
                        optionClearShares.setVisibility(View.GONE);
                    }

                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    optionRemove.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                } else if (tabSelected == 2) {
                    if (!isTakenDown && node.isShared()) {
                        optionClearShares.setVisibility(View.VISIBLE);
                    } else if (ViewUtils.isVisible(optionClearShares)) {
                        counterShares--;
                        optionClearShares.setVisibility(View.GONE);
                    }

                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    optionRemove.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                }

                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                break;

            case RECENTS_MODE:
            case FAVOURITES_IN_TAB_MODE:
            case FAVOURITES_MODE:
                if (ViewUtils.isVisible(optionShareFolder)) {
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                }
                if (ViewUtils.isVisible(optionClearShares)) {
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                }

                optionRemove.setVisibility(View.GONE);
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                switch (accessLevel) {
                    case MegaShare.ACCESS_READWRITE:
                    case MegaShare.ACCESS_READ:
                    case MegaShare.ACCESS_UNKNOWN:
                        optionLabel.setVisibility(View.GONE);
                        optionFavourite.setVisibility(View.GONE);
                        counterModify--;
                        optionRename.setVisibility(View.GONE);
                        counterModify--;
                        optionMove.setVisibility(View.GONE);
                        optionRubbishBin.setVisibility(View.GONE);
                        if (ViewUtils.isVisible(optionLink)) {
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                        }
                        if (ViewUtils.isVisible(optionRemoveLink)) {
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                        }
                        break;
                }
                break;
        }

        separatorOpen.setVisibility(counterOpen <= 0 ? View.GONE : View.VISIBLE);
        separatorDownload.setVisibility(counterSave <= 0 ? View.GONE : View.VISIBLE);
        separatorShares.setVisibility(counterShares <= 0 ? View.GONE : View.VISIBLE);
        separatorModify.setVisibility(counterModify <= 0 ? View.GONE : View.VISIBLE);

        offlineSwitch.setOnCheckedChangeListener((v, isChecked) -> onClick(v));

        optionFavourite.setText(node.isFavourite() ? R.string.file_properties_unfavourite : R.string.file_properties_favourite);
        optionFavourite.setCompoundDrawablesWithIntrinsicBounds(node.isFavourite()
                        ? R.drawable.ic_remove_favourite
                        : R.drawable.ic_add_favourite,
                0, 0, 0);

        if (node.getLabel() != MegaNode.NODE_LBL_UNKNOWN) {
            int color = ResourcesCompat.getColor(getResources(), getNodeLabelColor(node.getLabel()), null);
            Drawable drawable = MegaNodeUtil.getNodeLabelDrawable(node.getLabel(), getResources());
            optionLabelCurrent.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null);
            optionLabelCurrent.setText(MegaNodeUtil.getNodeLabelText(node.getLabel()));
            optionLabelCurrent.setTextColor(color);
            optionLabelCurrent.setVisibility(View.VISIBLE);
        } else {
            optionLabelCurrent.setVisibility(View.GONE);
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean(CANNOT_OPEN_FILE_SHOWN, false)) {
            cannotOpenFileDialog = showCannotOpenFileDialog(this, requireActivity(), node, ((ManagerActivity) requireActivity())::saveNodeByTap);
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        AlertDialogUtil.dismissAlertDialogIfExists(cannotOpenFileDialog);
        super.onDestroyView();
    }

    private void showOwnerSharedFolder() {
        ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
        for (int j = 0; j < sharesIncoming.size(); j++) {
            MegaShare mS = sharesIncoming.get(j);
            if (mS.getNodeHandle() == node.getHandle()) {
                MegaUser user = megaApi.getContact(mS.getUser());
                if (user != null) {
                    nodeInfo.setText(getMegaUserNameDB(user));
                } else {
                    nodeInfo.setText(mS.getUser());
                }
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (node == null) {
            logWarning("The selected node is NULL");
            return;
        }

        ArrayList<Long> handleList = new ArrayList<>();
        handleList.add(node.getHandle());

        Intent i;
        int nodeType;

        switch (v.getId()) {
            case R.id.download_option:
                ((ManagerActivity) requireActivity()).saveNodesToDevice(
                        Collections.singletonList(node), false, false, false, false);
                break;

            case R.id.favorite_option:
                megaApi.setNodeFavourite(node, !node.isFavourite());
                break;

            case R.id.option_label_layout:
                ((ManagerActivity) requireActivity()).showNodeLabelsPanel(node);
                break;

            case R.id.file_properties_switch:
            case R.id.option_offline_layout:
                if (availableOffline(requireContext(), node)) {
                    MegaOffline mOffDelete = dbH.findByHandle(node.getHandle());
                    removeFromOffline(mOffDelete);
                    Util.showSnackbar(
                            getActivity(), getResources().getString(R.string.file_removed_offline));
                } else {
                    saveForOffline();
                }
                break;

            case R.id.properties_option:
                i = new Intent(requireContext(), FileInfoActivity.class);
                i.putExtra(HANDLE, node.getHandle());

                if (drawerItem == DrawerItem.SHARED_ITEMS) {
                    if (((ManagerActivity) requireActivity()).getTabItemShares() == 0) {
                        i.putExtra("from", FROM_INCOMING_SHARES);
                        i.putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL,
                                ((ManagerActivity) requireActivity()).getDeepBrowserTreeIncoming() <= FIRST_NAVIGATION_LEVEL);
                    } else if (((ManagerActivity) requireActivity()).getTabItemShares() == 1) {
                        i.putExtra("adapterType", OUTGOING_SHARES_ADAPTER);
                    }
                } else if (drawerItem == DrawerItem.INBOX) {
                    if (((ManagerActivity) requireActivity()).getTabItemShares() == 0) {
                        i.putExtra("from", FROM_INBOX);
                    }
                } else if (drawerItem == DrawerItem.SEARCH) {
                    if (nC.nodeComesFromIncoming(node)) {
                        i.putExtra("from", FROM_INCOMING_SHARES);
                        int dBT = nC.getIncomingLevel(node);
                        i.putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL, dBT <= FIRST_NAVIGATION_LEVEL);
                    }
                }
                i.putExtra(NAME, node.getName());

                startActivityForResult(i, REQUEST_CODE_FILE_INFO);
                dismissAllowingStateLoss();
                break;

            case R.id.link_option:
                ((ManagerActivity) requireActivity()).showGetLinkActivity(node.getHandle());
                break;

            case R.id.view_in_folder_option:
                ((ManagerActivity) requireActivity()).viewNodeInFolder(node);
                break;

            case R.id.remove_link_option:
                ((ManagerActivity) requireActivity()).showConfirmationRemovePublicLink(node);
                break;

            case R.id.share_folder_option:
                nodeType = checkBackupNodeTypeByHandle(megaApi, node);
                if (nodeType != BACKUP_NONE) {
                    ((ManagerActivity) requireActivity()).showWarningDialogOfShare(node, nodeType, ACTION_BACKUP_SHARE_FOLDER);
                } else {
                    if (isOutShare(node)) {
                        i = new Intent(requireContext(), FileContactListActivity.class);
                        i.putExtra(NAME, node.getHandle());
                        startActivity(i);
                    } else {
                        nC.selectContactToShareFolder(node);
                    }
                }
                dismissAllowingStateLoss();
                break;

            case R.id.clear_share_option:
                ArrayList<MegaShare> shareList = megaApi.getOutShares(node);
                ((ManagerActivity) requireActivity()).showConfirmationRemoveAllSharingContacts(shareList, node);
                break;

            case R.id.leave_share_option:
                showConfirmationLeaveIncomingShare(requireActivity(),
                        (SnackbarShower) requireActivity(), node);
                break;

            case R.id.send_chat_option:
                ((ManagerActivity) requireActivity()).attachNodeToChats(node);
                dismissAllowingStateLoss();
                break;

            case R.id.rename_option:
                ((ManagerActivity) requireActivity()).showRenameDialog(node);

                break;

            case R.id.move_option:
                nC.chooseLocationToMoveNodes(handleList);
                dismissAllowingStateLoss();
                break;

            case R.id.option_backup_move_layout:
                ((ManagerActivity) requireActivity()).moveBackupNode(handleList);
                dismissAllowingStateLoss();
                break;

            case R.id.copy_option:
            case R.id.backup_copy_option:
                nC.chooseLocationToCopyNodes(handleList);
                dismissAllowingStateLoss();
                break;

            case R.id.rubbish_bin_option:
            case R.id.remove_option:
            case R.id.option_backup_rubbish_bin_layout:
                ((ManagerActivity) requireActivity()).askConfirmationMoveToRubbish(handleList);
                break;

            case R.id.open_folder_option:
                nC.openFolderFromSearch(node.getHandle());
                dismissAllowingStateLoss();
                break;

            case R.id.open_with_option:
                cannotOpenFileDialog = openWith(this, requireActivity(), node, ((ManagerActivity) requireActivity())::saveNodeByTap);
                return;

            case R.id.restore_option:
                List<MegaNode> nodes = new ArrayList<>();
                nodes.add(node);
                ((ManagerActivity) requireActivity()).restoreFromRubbish(nodes);
                break;

            case R.id.share_option:
                shareNode(requireActivity(), node);
                break;

            case R.id.edit_file_option:
                manageEditTextFileIntent(requireContext(), node, getAdapterType());
                break;
            case R.id.option_versions_layout:
                Intent version = new Intent(getActivity(), VersionsFileActivity.class);
                version.putExtra("handle", node.getHandle());
                requireActivity().startActivityForResult(version, REQUEST_CODE_DELETE_VERSIONS_HISTORY);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    private void refreshView() {
        switch (drawerItem) {
            case CLOUD_DRIVE:
            case RUBBISH_BIN:
                ((ManagerActivity) requireActivity()).onNodesCloudDriveUpdate();
                break;

            case INBOX:
                ((ManagerActivity) requireActivity()).onNodesInboxUpdate();
                break;

            case SHARED_ITEMS:
                ((ManagerActivity) requireActivity()).onNodesSharedUpdate();
                break;

            case SEARCH:
                ((ManagerActivity) requireActivity()).onNodesSearchUpdate();
                break;

            case HOMEPAGE:
                LiveEventBus.get(EVENT_NODES_CHANGE).post(false);
                break;
        }
    }

    private void removeFromOffline(MegaOffline mOffDelete) {
        removeOffline(mOffDelete, dbH, requireContext());
        refreshView();
    }

    private void saveForOffline() {
        int adapterType;

        switch (drawerItem) {
            case INBOX:
                adapterType = FROM_INBOX;
                break;

            case SHARED_ITEMS:
                if (((ManagerActivity) requireActivity()).getTabItemShares() == 0) {
                    adapterType = FROM_INCOMING_SHARES;
                    break;
                }

            default:
                adapterType = FROM_OTHERS;
        }

        File offlineParent = getOfflineParentFile(requireActivity(), adapterType, node, megaApi);

        if (isFileAvailable(offlineParent)) {
            File offlineFile = new File(offlineParent, node.getName());

            if (isFileAvailable(offlineFile)) {
                if (isFileDownloadedLatest(offlineFile, node)
                        && offlineFile.length() == node.getSize()) {
                    // if the file matches to the latest on the cloud, do nothing
                    return;
                } else {
                    // if the file does not match the latest on the cloud, delete the old file offline database record
                    String parentName = getOfflineParentFileName(requireContext(), node).getAbsolutePath() + File.separator;
                    MegaOffline mOffDelete = dbH.findbyPathAndName(parentName, node.getName());
                    removeFromOffline(mOffDelete);
                }
            }
        }

        // Save the new file to offline
        saveOffline(offlineParent, node, requireActivity());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        outState.putLong(HANDLE, handle);
        outState.putInt(SAVED_STATE_KEY_MODE, mMode);
        outState.putBoolean(CANNOT_OPEN_FILE_SHOWN, AlertDialogUtil.isAlertDialogShown(cannotOpenFileDialog));
    }

    private void mapDrawerItemToMode(DrawerItem drawerItem) {
        switch (drawerItem) {
            case CLOUD_DRIVE:
                mMode = CLOUD_DRIVE_MODE;
                break;
            case RUBBISH_BIN:
                mMode = RUBBISH_BIN_MODE;
                break;
            case INBOX:
                mMode = INBOX_MODE;
                break;
            case SHARED_ITEMS:
                mMode = SHARED_ITEMS_MODE;
                break;
            case SEARCH:
                mMode = SEARCH_MODE;
                break;
        }
    }

    private int getAdapterType() {
        switch (mMode) {
            case CLOUD_DRIVE_MODE:
                return FILE_BROWSER_ADAPTER;
            case RUBBISH_BIN_MODE:
                return RUBBISH_BIN_ADAPTER;
            case INBOX_MODE:
                return INBOX_ADAPTER;
            case SHARED_ITEMS_MODE:
                switch (((ManagerActivity) requireActivity()).getTabItemShares()) {
                    case INCOMING_TAB:
                        return INCOMING_SHARES_ADAPTER;
                    case OUTGOING_TAB:
                        return OUTGOING_SHARES_ADAPTER;
                    case LINKS_TAB:
                        return LINKS_ADAPTER;
                }
            case SEARCH_MODE:
                return SEARCH_ADAPTER;
            case RECENTS_MODE:
                return RECENTS_ADAPTER;
            case FAVOURITES_IN_TAB_MODE:
            case FAVOURITES_MODE:
                return FAVOURITES_ADAPTER;
            default:
                return INVALID_VALUE;
        }
    }
}
