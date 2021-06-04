package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.MegaNodeUtil;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.INCOMING_TAB;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.LINKS_TAB;
import static mega.privacy.android.app.lollipop.ManagerActivityLollipop.OUTGOING_TAB;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class NodeOptionsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {
    /** The "modes" are defined to allow the client to specify the dialog style more flexibly.
    At the same time, compatible with old code. For which mode corresponds to which dialog style,
     please refer to the code */
    /** No definite mode, map the drawerItem to a specific mode */
    public static final int MODE0 = 0;
    /** For Cloud Drive */
    public static final int MODE1 = 1;
    /** For Rubbish Bin */
    public static final int MODE2 = 2;
    /** For Inbox */
    public static final int MODE3 = 3;
    /** For Shared items */
    public static final int MODE4 = 4;
    /** For Search */
    public static final int MODE5 = 5;
    /** For Recents */
    public static final int MODE6 = 6;

    private static final String SAVED_STATE_KEY_MODE = "MODE";

    private int mMode;

    private MegaNode node = null;
    private NodeController nC;

    private TextView nodeInfo;

    private ManagerActivityLollipop.DrawerItem drawerItem;

    public NodeOptionsBottomSheetDialogFragment(int mode) {
        if (mode >= MODE0 && mode <= MODE6) {
            mMode = mode;
        }
    }

    public NodeOptionsBottomSheetDialogFragment() {
        mMode = MODE0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
            if (context instanceof ManagerActivityLollipop) {
                drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
            }
            mMode = savedInstanceState.getInt(SAVED_STATE_KEY_MODE, MODE0);
        } else {
            if (context instanceof ManagerActivityLollipop) {
                node = ((ManagerActivityLollipop) context).getSelectedNode();
                drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
            }
        }

        nC = new NodeController(context);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_item, null);
        mainLinearLayout = contentView.findViewById(R.id.node_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout_bottom_sheet_node);

        ImageView nodeThumb = contentView.findViewById(R.id.node_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.node_name_text);
        nodeInfo = contentView.findViewById(R.id.node_info_text);
        ImageView nodeVersionsIcon = contentView.findViewById(R.id.node_info_versions_icon);
        RelativeLayout nodeIconLayout = contentView.findViewById(R.id.node_relative_layout_icon);
        ImageView nodeIcon = contentView.findViewById(R.id.node_icon);
        ImageView permissionsIcon = contentView.findViewById(R.id.permissions_icon);

        LinearLayout optionEdit = contentView.findViewById(R.id.edit_file_option);

        TextView optionInfo = contentView.findViewById(R.id.properties_option);
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

        TextView viewInFolder = contentView.findViewById(R.id.view_in_folder_option);
        if (mMode == MODE6) {
            viewInFolder.setVisibility(View.VISIBLE);
            viewInFolder.setOnClickListener(this);
        }

        int counterOpen = 2;
        int counterSave = 2;
        int counterShares = 6;
        int counterModify = 4;

        LinearLayout separatorOpen = contentView.findViewById(R.id.separator_open_options);
        LinearLayout separatorDownload = contentView.findViewById(R.id.separator_download_options);
        LinearLayout separatorShares = contentView.findViewById(R.id.separator_share_options);
        LinearLayout separatorModify = contentView.findViewById(R.id.separator_modify_options);

        nodeIconLayout.setVisibility(View.GONE);
        permissionsIcon.setVisibility(View.GONE);

        if (!isScreenInPortrait(context)) {
            logDebug("Landscape configuration");
            nodeName.setMaxWidth(scaleWidthPx(275, outMetrics));
            nodeInfo.setMaxWidth(scaleWidthPx(275, outMetrics));
        } else {
            nodeName.setMaxWidth(scaleWidthPx(210, outMetrics));
            nodeInfo.setMaxWidth(scaleWidthPx(210, outMetrics));
        }

        if (node == null) return;

        if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isVideo() || MimeTypeList.typeForName(node.getName()).isAudio()
                || MimeTypeList.typeForName(node.getName()).isImage() || MimeTypeList.typeForName(node.getName()).isPdf()) {
            optionOpenWith.setVisibility(View.VISIBLE);
        } else {
            counterOpen--;
            optionOpenWith.setVisibility(View.GONE);
        }

        if (isOnline(context)) {
            nodeName.setText(node.getName());

            if (node.isFolder()) {
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
                nodeInfo.setText(getFileInfo(node));

                if (megaApi.hasVersions(node)) {
                    nodeVersionsIcon.setVisibility(View.VISIBLE);
                } else {
                    nodeVersionsIcon.setVisibility(View.GONE);
                }

                setNodeThumbnail(context, node, nodeThumb);
                optionSendChat.setVisibility(View.VISIBLE);
            }
        }

        int accessLevel = megaApi.getAccess(node);

        if (accessLevel != MegaShare.ACCESS_OWNER) {
            counterShares--;
            optionShare.setVisibility(View.GONE);
        }

        if (mMode == MODE0) {
            mapDrawerItemToMode(drawerItem);
        }

        optionInfo.setText(R.string.general_info);

        switch (mMode) {
            case MODE1:
                logDebug("show Cloud bottom sheet");

                if (node.isFolder()) {
                    optionShareFolder.setVisibility(View.VISIBLE);
                    if (isOutShare(node)) {
                        optionShareFolder.setText(R.string.manage_share);
                        optionClearShares.setVisibility(View.VISIBLE);
                    } else {
                        optionShareFolder.setText(R.string.context_share_folder);
                        counterShares--;
                        optionClearShares.setVisibility(View.GONE);
                    }
                } else {
                    if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
                        optionEdit.setVisibility(View.VISIBLE);
                    }

                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                }

                if (node.isExported()) {
                    //Node has public link
                    optionLink.setText(R.string.edit_link_option);
                    optionRemoveLink.setVisibility(View.VISIBLE);
                } else {
                    optionLink.setText(R.string.context_get_link_menu);
                    counterShares--;
                    optionRemoveLink.setVisibility(View.GONE);
                }

                offlineSwitch.setChecked(availableOffline(context, node));
                optionInfo.setVisibility(View.VISIBLE);
                optionRubbishBin.setVisibility(View.VISIBLE);
                optionLink.setVisibility(View.VISIBLE);

                optionRename.setVisibility(View.VISIBLE);
                optionMove.setVisibility(View.VISIBLE);
                optionCopy.setVisibility(View.VISIBLE);

                optionLabel.setVisibility(View.VISIBLE);
                optionFavourite.setVisibility(View.VISIBLE);

                //Hide
                optionRemove.setVisibility(View.GONE);
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);
                break;

            case MODE2:
                logDebug("show Rubbish bottom sheet");

                long restoreHandle = node.getRestoreHandle();
                if (restoreHandle != INVALID_HANDLE) {
                    MegaNode restoreNode = megaApi.getNodeByHandle(restoreHandle);
                    if ((!megaApi.isInRubbish(node)) || restoreNode == null || megaApi.isInRubbish(restoreNode)) {
                        counterModify--;
                        optionRestoreFromRubbish.setVisibility(View.GONE);
                    } else {
                        optionRestoreFromRubbish.setVisibility(View.VISIBLE);
                    }
                } else {
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);
                }

                nodeIconLayout.setVisibility(View.GONE);

                optionRemove.setVisibility(View.VISIBLE);
                optionInfo.setVisibility(View.VISIBLE);

                //Hide
                counterOpen--;
                optionOpenWith.setVisibility(View.GONE);
                counterModify--;
                optionMove.setVisibility(View.GONE);
                counterModify--;
                optionRename.setVisibility(View.GONE);
                counterModify--;
                optionCopy.setVisibility(View.GONE);
                counterShares--;
                optionClearShares.setVisibility(View.GONE);
                optionLeaveShares.setVisibility(View.GONE);
                optionRubbishBin.setVisibility(View.GONE);
                counterShares--;
                optionShare.setVisibility(View.GONE);
                counterShares--;
                optionShareFolder.setVisibility(View.GONE);
                counterShares--;
                optionLink.setVisibility(View.GONE);
                counterShares--;
                optionRemoveLink.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterSave--;
                optionDownload.setVisibility(View.GONE);
                counterSave--;
                optionOffline.setVisibility(View.GONE);
                counterShares--;
                optionSendChat.setVisibility(View.GONE);
                break;

            case MODE3:

                if (!node.isFolder()) {
                    if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
                        optionEdit.setVisibility(View.VISIBLE);
                    }
                }

                if (node.isExported()) {
                    //Node has public link
                    optionLink.setText(R.string.edit_link_option);
                    optionRemoveLink.setVisibility(View.VISIBLE);
                } else {
                    optionLink.setText(R.string.context_get_link_menu);
                    counterShares--;
                    optionRemoveLink.setVisibility(View.GONE);
                }

                optionDownload.setVisibility(View.VISIBLE);

                offlineSwitch.setChecked(availableOffline(context, node));
                optionInfo.setVisibility(View.VISIBLE);
                optionRubbishBin.setVisibility(View.VISIBLE);
                optionLink.setVisibility(View.VISIBLE);

                optionRubbishBin.setVisibility(View.VISIBLE);
                optionRename.setVisibility(View.VISIBLE);
                optionMove.setVisibility(View.VISIBLE);
                optionCopy.setVisibility(View.VISIBLE);

                //Hide
                counterShares--;
                optionClearShares.setVisibility(View.GONE);
                optionRemove.setVisibility(View.GONE);
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterShares--;
                optionShareFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                break;

            case MODE4:

                int tabSelected = ((ManagerActivityLollipop) context).getTabItemShares();
                if (tabSelected == 0) {
                    logDebug("showOptionsPanelIncoming");

                    if (node.isFolder()) {
                        counterShares--;
                        optionSendChat.setVisibility(View.GONE);
                    } else {
                        if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())
                                && accessLevel >= MegaShare.ACCESS_READWRITE) {
                            optionEdit.setVisibility(View.VISIBLE);
                        }

                        optionSendChat.setVisibility(View.VISIBLE);
                    }

                    nodeIconLayout.setVisibility(View.GONE);

                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                    optionDownload.setVisibility(View.VISIBLE);
                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);

                    int dBT = ((ManagerActivityLollipop) context).getDeepBrowserTreeIncoming();
                    logDebug("DeepTree value:" + dBT);

                    if (dBT > 0) {
                        optionLeaveShares.setVisibility(View.GONE);
                    } else {
                        //Show the owner of the shared folder
                        showOwnerSharedFolder();
                        optionLeaveShares.setVisibility(View.VISIBLE);
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

                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL:
                            logDebug("access FULL");
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            counterShares--;
                            optionClearShares.setVisibility(View.GONE);
                            optionRename.setVisibility(View.VISIBLE);

                            if (dBT > 0) {
                                optionRubbishBin.setVisibility(View.VISIBLE);
                                optionMove.setVisibility(View.VISIBLE);

                            } else {
                                optionRubbishBin.setVisibility(View.GONE);
                                counterModify--;
                                optionMove.setVisibility(View.GONE);

                            }

                            optionLabel.setVisibility(View.VISIBLE);
                            optionFavourite.setVisibility(View.VISIBLE);

                            break;

                        case MegaShare.ACCESS_READ:
                            logDebug("access read");
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            counterModify--;
                            optionRename.setVisibility(View.GONE);
                            counterShares--;
                            optionClearShares.setVisibility(View.GONE);
                            counterModify--;
                            optionMove.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            break;

                        case MegaShare.ACCESS_READWRITE:
                            logDebug("readwrite");
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            counterModify--;
                            optionRename.setVisibility(View.GONE);
                            counterShares--;
                            optionClearShares.setVisibility(View.GONE);
                            counterModify--;
                            optionMove.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            break;
                    }
                } else if (tabSelected == 1) {
                    logDebug("showOptionsPanelOutgoing");

                    if (node.isFolder()) {
                        optionShareFolder.setVisibility(View.VISIBLE);
                        optionShareFolder.setText(R.string.manage_share);
                    } else {
                        if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
                            optionEdit.setVisibility(View.VISIBLE);
                        }

                        counterShares--;
                        optionShareFolder.setVisibility(View.GONE);
                    }

                    if (node.isExported()) {
                        //Node has public link
                        optionLink.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                    } else {
                        optionLink.setText(R.string.context_get_link_menu);
                        counterShares--;
                        optionRemoveLink.setVisibility(View.GONE);
                    }

                    if (((ManagerActivityLollipop) context).getDeepBrowserTreeOutgoing() == 0) {
                        optionClearShares.setVisibility(View.VISIBLE);

                        //Show the number of contacts who shared the folder
                        ArrayList<MegaShare> sl = megaApi.getOutShares(node);
                        if (sl != null) {
                            if (sl.size() != 0) {
                                nodeInfo.setText(getQuantityString(R.plurals.general_num_shared_with,
                                                sl.size(), sl.size()));
                            }
                        }
                    } else {
                        counterShares--;
                        optionClearShares.setVisibility(View.GONE);
                    }

                    optionDownload.setVisibility(View.VISIBLE);
                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    optionRename.setVisibility(View.VISIBLE);
                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    optionCopy.setVisibility(View.VISIBLE);
                    optionRubbishBin.setVisibility(View.VISIBLE);

                    optionLabel.setVisibility(View.VISIBLE);
                    optionFavourite.setVisibility(View.VISIBLE);

                    //Hide
                    optionRemove.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                } else if (tabSelected == 2) {
                    if (node.isFolder()) {
                        optionShareFolder.setVisibility(View.VISIBLE);
                        if (isOutShare(node)) {
                            optionShareFolder.setText(R.string.manage_share);
                        } else {
                            optionShareFolder.setText(R.string.context_share_folder);
                        }
                    } else {
                        if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
                            optionEdit.setVisibility(View.VISIBLE);
                        }

                        counterShares--;
                        optionShareFolder.setVisibility(View.GONE);
                    }

                    if (node.isExported()) {
                        //Node has public link
                        optionLink.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                    } else {
                        optionLink.setText(R.string.context_get_link_menu);
                        counterShares--;
                        optionRemoveLink.setVisibility(View.GONE);
                    }

                    if (node.isShared()) {
                        optionClearShares.setVisibility(View.VISIBLE);
                    } else {
                        counterShares--;
                        optionClearShares.setVisibility(View.GONE);
                    }

                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    optionRubbishBin.setVisibility(View.VISIBLE);
                    optionLink.setVisibility(View.VISIBLE);

                    optionRename.setVisibility(View.VISIBLE);
                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    optionCopy.setVisibility(View.VISIBLE);

                    optionLabel.setVisibility(View.VISIBLE);
                    optionFavourite.setVisibility(View.VISIBLE);

                    //Hide
                    optionRemove.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                }

                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                break;

            case MODE5:
                if (megaApi.isInRubbish(node)) {
                    MegaNode restoreNode = megaApi.getNodeByHandle(node.getRestoreHandle());

                    if (!megaApi.isInRubbish(node) || restoreNode == null || megaApi.isInRubbish(restoreNode)) {
                        counterModify--;
                        optionRestoreFromRubbish.setVisibility(View.GONE);
                    } else {
                        optionRestoreFromRubbish.setVisibility(View.VISIBLE);
                    }

                    nodeIconLayout.setVisibility(View.GONE);

                    optionRemove.setVisibility(View.VISIBLE);
                    optionInfo.setVisibility(View.VISIBLE);

                    //Hide
                    counterOpen--;
                    optionOpenWith.setVisibility(View.GONE);
                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    counterModify--;
                    optionRename.setVisibility(View.GONE);
                    counterModify--;
                    optionCopy.setVisibility(View.GONE);
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                    optionRubbishBin.setVisibility(View.GONE);
                    counterShares--;
                    optionShare.setVisibility(View.GONE);
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                    counterShares--;
                    optionLink.setVisibility(View.GONE);
                    counterShares--;
                    optionRemoveLink.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                    counterSave--;
                    optionDownload.setVisibility(View.GONE);
                    counterSave--;
                    optionOffline.setVisibility(View.GONE);
                    counterShares--;
                    optionSendChat.setVisibility(View.GONE);

                    break;
                }

                if (node.isFolder()) {
                    optionShareFolder.setVisibility(View.VISIBLE);
                } else {
                    if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())
                            && accessLevel >= MegaShare.ACCESS_READWRITE) {
                        optionEdit.setVisibility(View.VISIBLE);
                    }

                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                }

                int dBT = nC.getIncomingLevel(node);
                if (nC.nodeComesFromIncoming(node)) {
                    logDebug("dBT: " + dBT);
                    if (node.isFolder()) {
                        counterShares--;
                        optionSendChat.setVisibility(View.GONE);
                    } else {
                        optionSendChat.setVisibility(View.VISIBLE);
                    }

                    nodeIconLayout.setVisibility(View.VISIBLE);

                    logDebug("Node: " + node.getName() + " " + accessLevel);
                    optionDownload.setVisibility(View.VISIBLE);
                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);

                    logDebug("DeepTree value:" + dBT);
                    if (dBT > 0) {
                        optionLeaveShares.setVisibility(View.GONE);
                        nodeIconLayout.setVisibility(View.GONE);
                    } else {
                        //Show the owner of the shared folder
                        showOwnerSharedFolder();
                        optionLeaveShares.setVisibility(View.VISIBLE);

                        switch (accessLevel) {
                            case MegaShare.ACCESS_FULL:
                                logDebug("LEVEL 0 - access FULL");
                                nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                                break;

                            case MegaShare.ACCESS_READ:
                                logDebug("LEVEL 0 - access read");
                                nodeIcon.setImageResource(R.drawable.ic_shared_read);
                                break;

                            case MegaShare.ACCESS_READWRITE:
                                logDebug("LEVEL 0 - readwrite");
                                nodeIcon.setImageResource(R.drawable.ic_shared_read_write);
                                break;
                        }
                    }

                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL:
                            logDebug("access FULL");
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            counterShares--;
                            optionClearShares.setVisibility(View.GONE);
                            optionRename.setVisibility(View.VISIBLE);

                            if (dBT > 0) {
                                optionRubbishBin.setVisibility(View.VISIBLE);
                                optionMove.setVisibility(View.VISIBLE);

                            } else {
                                optionRubbishBin.setVisibility(View.GONE);
                                counterModify--;
                                optionMove.setVisibility(View.GONE);

                            }

                            optionLabel.setVisibility(View.VISIBLE);
                            optionFavourite.setVisibility(View.VISIBLE);

                            break;

                        case MegaShare.ACCESS_READ:
                            logDebug("access read");
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            counterModify--;
                            optionRename.setVisibility(View.GONE);
                            counterShares--;
                            optionClearShares.setVisibility(View.GONE);
                            counterModify--;
                            optionMove.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            break;

                        case MegaShare.ACCESS_READWRITE:
                            logDebug("readwrite");
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            counterModify--;
                            optionRename.setVisibility(View.GONE);
                            counterShares--;
                            optionClearShares.setVisibility(View.GONE);
                            counterModify--;
                            optionMove.setVisibility(View.GONE);
                            optionRubbishBin.setVisibility(View.GONE);
                            break;
                    }
                } else {
                    if (node.isExported()) {
                        //Node has public link
                        optionLink.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                    } else {
                        optionLink.setText(R.string.context_get_link_menu);
                        counterShares--;
                        optionRemoveLink.setVisibility(View.GONE);
                    }

                    MegaNode parent = nC.getParent(node);
                    if (parent.getHandle() != megaApi.getRubbishNode().getHandle()) {
                        optionRubbishBin.setVisibility(View.VISIBLE);
                        optionRemove.setVisibility(View.GONE);
                    } else {
                        optionRubbishBin.setVisibility(View.GONE);
                        optionRemove.setVisibility(View.VISIBLE);
                    }

                    optionDownload.setVisibility(View.VISIBLE);

                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    optionLink.setVisibility(View.VISIBLE);
                    optionRename.setVisibility(View.VISIBLE);
                    optionOpenFolder.setVisibility(View.VISIBLE);
                    optionLabel.setVisibility(View.VISIBLE);
                    optionFavourite.setVisibility(View.VISIBLE);

                    //Hide
                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    counterModify--;
                    optionCopy.setVisibility(View.GONE);
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                    optionLeaveShares.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);
                }
                break;
            case MODE6:
                if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())
                        && accessLevel >= MegaShare.ACCESS_READWRITE) {
                    optionEdit.setVisibility(View.VISIBLE);
                }

                counterShares--;
                optionShareFolder.setVisibility(View.GONE);
                nodeIconLayout.setVisibility(View.GONE);
                counterShares--;
                optionLink.setVisibility(View.GONE);
                counterShares--;
                optionRemoveLink.setVisibility(View.GONE);
                counterShares--;
                optionClearShares.setVisibility(View.GONE);
                offlineSwitch.setChecked(availableOffline(context, node));

                optionSendChat.setVisibility(View.VISIBLE);

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
                        counterModify--;
                        optionRename.setVisibility(View.GONE);
                        counterModify--;
                        optionMove.setVisibility(View.GONE);
                        optionRubbishBin.setVisibility(View.GONE);
                        counterShares--;
                        optionLink.setVisibility(View.GONE);
                        nodeIconLayout.setVisibility(View.GONE);
                        optionLink.setText(R.string.context_get_link_menu);
                        counterShares--;
                        optionRemoveLink.setVisibility(View.GONE);
                        break;

                    case MegaShare.ACCESS_FULL:
                    case MegaShare.ACCESS_OWNER:
                        optionLabel.setVisibility(View.VISIBLE);
                        optionFavourite.setVisibility(View.VISIBLE);
                        optionLink.setVisibility(View.VISIBLE);

                        if (node.isExported()) {
                            optionLink.setText(R.string.edit_link_option);
                            optionRemoveLink.setVisibility(View.VISIBLE);
                        } else {
                            optionLink.setText(R.string.context_get_link_menu);
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

        offlineSwitch.setOnCheckedChangeListener((view, isChecked) -> onClick(view));

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

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true);
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

    @Override
    public void onClick(View v) {
        if (node == null) {
            logWarning("The selected node is NULL");
            return;
        }

        ArrayList<Long> handleList = new ArrayList<>();
        handleList.add(node.getHandle());

        Intent i;

        switch (v.getId()) {
            case R.id.download_option:
                ((ManagerActivityLollipop) context).saveNodesToDevice(
                        Collections.singletonList(node), false, false, false, false);
                break;

            case R.id.favorite_option:
                megaApi.setNodeFavourite(node, !node.isFavourite());
                break;

            case R.id.option_label_layout:
                ((ManagerActivityLollipop) context).showNodeLabelsPanel(node);
                break;

            case R.id.file_properties_switch:
            case R.id.option_offline_layout:
                if (availableOffline(context, node)) {
                    MegaOffline mOffDelete = dbH.findByHandle(node.getHandle());
                    removeFromOffline(mOffDelete);
                } else {
                    saveForOffline();
                }
                break;

            case R.id.properties_option:
                i = new Intent(context, FileInfoActivityLollipop.class);
                i.putExtra(HANDLE, node.getHandle());

                if (drawerItem == ManagerActivityLollipop.DrawerItem.SHARED_ITEMS) {
                    if (((ManagerActivityLollipop) context).getTabItemShares() == 0) {
                        i.putExtra("from", FROM_INCOMING_SHARES);
                        i.putExtra("firstLevel", ((ManagerActivityLollipop) context).getDeepBrowserTreeIncoming() <= 0);
                    } else if (((ManagerActivityLollipop) context).getTabItemShares() == 1) {
                        i.putExtra("adapterType", OUTGOING_SHARES_ADAPTER);
                    }
                } else if (drawerItem == ManagerActivityLollipop.DrawerItem.INBOX) {
                    if (((ManagerActivityLollipop) context).getTabItemShares() == 0) {
                        i.putExtra("from", FROM_INBOX);
                    }
                } else if (drawerItem == ManagerActivityLollipop.DrawerItem.SEARCH) {
                    if (nC.nodeComesFromIncoming(node)) {
                        i.putExtra("from", FROM_INCOMING_SHARES);
                        int dBT = nC.getIncomingLevel(node);
                        if (dBT <= 0) {
                            i.putExtra("firstLevel", true);
                        } else {
                            i.putExtra("firstLevel", false);
                        }
                    }
                }
                i.putExtra(NAME, node.getName());

                ((ManagerActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_FILE_INFO);
                dismissAllowingStateLoss();
                break;

            case R.id.link_option:
                ((ManagerActivityLollipop) context).showGetLinkActivity(node.getHandle());
                break;

            case R.id.view_in_folder_option:
                ((ManagerActivityLollipop) context).viewNodeInFolder(node);
                break;

            case R.id.remove_link_option:
                ((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(node);
                break;

            case R.id.share_folder_option:
                if (isOutShare(node)) {
                    i = new Intent(context, FileContactListActivityLollipop.class);
                    i.putExtra(NAME, node.getHandle());
                    context.startActivity(i);
                    dismissAllowingStateLoss();
                } else {
                    nC.selectContactToShareFolder(node);
                    dismissAllowingStateLoss();
                }
                break;

            case R.id.clear_share_option:
                ArrayList<MegaShare> shareList = megaApi.getOutShares(node);
                ((ManagerActivityLollipop) context).showConfirmationRemoveAllSharingContacts(shareList, node);
                break;

            case R.id.leave_share_option:
                showConfirmationLeaveIncomingShare(requireActivity(),
                        (SnackbarShower) requireActivity(), node);
                break;

            case R.id.send_chat_option:
                ((ManagerActivityLollipop) context).attachNodeToChats(node);
                dismissAllowingStateLoss();
                break;

            case R.id.rename_option:
                ((ManagerActivityLollipop) context).showRenameDialog(node);

                break;

            case R.id.move_option:
                nC.chooseLocationToMoveNodes(handleList);
                dismissAllowingStateLoss();
                break;

            case R.id.copy_option:
                nC.chooseLocationToCopyNodes(handleList);
                dismissAllowingStateLoss();
                break;

            case R.id.rubbish_bin_option:
            case R.id.remove_option:
                ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;

            case R.id.open_folder_option:
                nC.openFolderFromSearch(node.getHandle());
                dismissAllowingStateLoss();
                break;

            case R.id.open_with_option:
                openWith(context, node);
                break;

            case R.id.restore_option:
                ((ManagerActivityLollipop) context).restoreFromRubbish(node);
                break;

            case R.id.share_option:
                shareNode(context, node);
                break;

            case R.id.edit_file_option:
                manageEditTextFileIntent(context, node, getAdapterType());
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    private void refreshView() {
        switch (drawerItem) {
            case CLOUD_DRIVE:
            case RUBBISH_BIN:
                ((ManagerActivityLollipop) context).onNodesCloudDriveUpdate();
                break;

            case INBOX:
                ((ManagerActivityLollipop) context).onNodesInboxUpdate();
                break;

            case SHARED_ITEMS:
                ((ManagerActivityLollipop) context).onNodesSharedUpdate();
                break;

            case SEARCH:
                ((ManagerActivityLollipop) context).onNodesSearchUpdate();
                break;

            case HOMEPAGE:
                LiveEventBus.get(EVENT_NODES_CHANGE).post(false);
                break;
        }
    }

    private void removeFromOffline(MegaOffline mOffDelete) {
        removeOffline(mOffDelete, dbH, context);
        refreshView();
    }

    private void saveForOffline() {
        int adapterType;

        switch (drawerItem) {
            case INBOX:
                adapterType = FROM_INBOX;
                break;

            case SHARED_ITEMS:
                if (((ManagerActivityLollipop) context).getTabItemShares() == 0) {
                    adapterType = FROM_INCOMING_SHARES;
                    break;
                }

            default:
                adapterType = FROM_OTHERS;
        }

        File offlineParent = getOfflineParentFile(context, adapterType, node, megaApi);

        if (isFileAvailable(offlineParent)) {
            File offlineFile = new File(offlineParent, node.getName());
            // if the file matches to the latest on the cloud, do nothing
            if (isFileAvailable(offlineFile)
                    && isFileDownloadedLatest(offlineFile, node)
                    && offlineFile.length() == node.getSize()) {
                return;
            } else {
                // if the file does not match the latest on the cloud, delete the old file offline database record
                String parentName = getOfflineParentFileName(context, node).getAbsolutePath() + File.separator;
                MegaOffline mOffDelete = dbH.findbyPathAndName(parentName, node.getName());
                removeFromOffline(mOffDelete);
            }
        }

        // Save the new file to offline
        saveOffline(offlineParent, node, context, (ManagerActivityLollipop) context);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        outState.putLong(HANDLE, handle);
        outState.putInt(SAVED_STATE_KEY_MODE, mMode);
    }

    private void mapDrawerItemToMode(ManagerActivityLollipop.DrawerItem drawerItem) {
        switch (drawerItem) {
            case CLOUD_DRIVE:
                mMode = MODE1;
                break;
            case RUBBISH_BIN:
                mMode = MODE2;
                break;
            case INBOX:
                mMode = MODE3;
                break;
            case SHARED_ITEMS:
                mMode = MODE4;
                break;
            case SEARCH:
                mMode = MODE5;
                break;
        }
    }

    private int getAdapterType() {
        switch (mMode) {
            case MODE1:
                return FILE_BROWSER_ADAPTER;

            case MODE2:
                return RUBBISH_BIN_ADAPTER;

            case MODE3:
                return INBOX_ADAPTER;

            case MODE4:
                switch (((ManagerActivityLollipop) context).getTabItemShares()) {
                    case INCOMING_TAB:
                        return INCOMING_SHARES_ADAPTER;
                    case OUTGOING_TAB:
                        return OUTGOING_SHARES_ADAPTER;
                    case LINKS_TAB:
                        return LINKS_ADAPTER;
                }

            case MODE5:
                return SEARCH_ADAPTER;

            case MODE6:
                return RECENTS_ADAPTER;

            default:
                return INVALID_VALUE;
        }
    }
}
