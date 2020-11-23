package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

public class NodeOptionsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node = null;
    private NodeController nC;

    private TextView nodeInfo;

    private ManagerActivityLollipop.DrawerItem drawerItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            node = megaApi.getNodeByHandle(handle);
            if (context instanceof ManagerActivityLollipop) {
                drawerItem = ManagerActivityLollipop.getDrawerItem();
            }
        } else {
            if (context instanceof ManagerActivityLollipop) {
                node = ((ManagerActivityLollipop) context).getSelectedNode();
                drawerItem = ManagerActivityLollipop.getDrawerItem();
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

        LinearLayout optionInfo = contentView.findViewById(R.id.option_properties_layout);
        TextView optionInfoText = contentView.findViewById(R.id.option_properties_text);
//      counterSave
        LinearLayout optionDownload = contentView.findViewById(R.id.option_download_layout);
        LinearLayout optionOffline = contentView.findViewById(R.id.option_offline_layout);
        SwitchMaterial offlineSwitch = contentView.findViewById(R.id.file_properties_switch);
//      counterShares
        LinearLayout optionLink = contentView.findViewById(R.id.option_link_layout);
        TextView optionLinkText = contentView.findViewById(R.id.option_link_text);
        LinearLayout optionRemoveLink = contentView.findViewById(R.id.option_remove_link_layout);
        LinearLayout optionShare = contentView.findViewById(R.id.option_share_layout);
        LinearLayout optionShareFolder = contentView.findViewById(R.id.option_share_folder_layout);
        TextView optionShareFolderText = contentView.findViewById(R.id.option_share_folder_text);
        LinearLayout optionClearShares = contentView.findViewById(R.id.option_clear_share_layout);
        LinearLayout optionSendChat = contentView.findViewById(R.id.option_send_chat_layout);
//      counterModify
        LinearLayout optionRename = contentView.findViewById(R.id.option_rename_layout);
        LinearLayout optionMove = contentView.findViewById(R.id.option_move_layout);
        LinearLayout optionCopy = contentView.findViewById(R.id.option_copy_layout);
        LinearLayout optionRestoreFromRubbish = contentView.findViewById(R.id.option_restore_layout);
//      counterOpen
        LinearLayout optionOpenFolder = contentView.findViewById(R.id.option_open_folder_layout);
        LinearLayout optionOpenWith = contentView.findViewById(R.id.option_open_with_layout);
//      counterRemove
        LinearLayout optionLeaveShares = contentView.findViewById(R.id.option_leave_share_layout);
        LinearLayout optionRubbishBin = contentView.findViewById(R.id.option_rubbish_bin_layout);
        LinearLayout optionRemove = contentView.findViewById(R.id.option_remove_layout);

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

        int counterSave = 2;
        int counterShares = 6;
        int counterModify = 4;
        int counterOpen = 2;
        int counterRemove = 3;

        LinearLayout separatorDownload = contentView.findViewById(R.id.separator_download_options);
        LinearLayout separatorShares = contentView.findViewById(R.id.separator_share_options);
        LinearLayout separatorModify = contentView.findViewById(R.id.separator_modify_options);
        LinearLayout separatorOpen = contentView.findViewById(R.id.separator_open_options);

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
                nodeInfo.setText(getInfoFolder(node, context, megaApi));
                nodeVersionsIcon.setVisibility(View.GONE);

                nodeThumb.setImageResource(getFolderIcon(node, drawerItem));

                if (isEmptyFolder(node)) {
                    counterSave--;
                    optionOffline.setVisibility(View.GONE);
                }

                counterShares--;
                optionSendChat.setVisibility(View.GONE);
            } else {
                long nodeSize = node.getSize();
                nodeInfo.setText(String.format("%s . %s", getSizeString(nodeSize), formatLongDateTime(node.getModificationTime())));

                if (megaApi.hasVersions(node)) {
                    nodeVersionsIcon.setVisibility(View.VISIBLE);
                } else {
                    nodeVersionsIcon.setVisibility(View.GONE);
                }

                if (node.hasThumbnail()) {
                    logDebug("Node has thumbnail");
                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                    params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                    params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                    params1.setMargins(20, 0, 12, 0);
                    nodeThumb.setLayoutParams(params1);

                    Bitmap thumb = getThumbnailFromCache(node);
                    if (thumb != null) {
                        nodeThumb.setImageBitmap(thumb);
                    } else {
                        thumb = getThumbnailFromFolder(node, context);
                        if (thumb != null) {
                            nodeThumb.setImageBitmap(thumb);
                        } else {
                            nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        }
                    }
                } else {
                    nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                }

                optionSendChat.setVisibility(View.VISIBLE);
            }
        }

        if (megaApi.getAccess(node) != MegaShare.ACCESS_OWNER) {
            counterShares--;
            optionShare.setVisibility(View.GONE);
        }

        switch (drawerItem) {
            case CLOUD_DRIVE:
                logDebug("show Cloud bottom sheet");
                if (((ManagerActivityLollipop) context).isOnRecents()) {
                    optionInfoText.setText(R.string.general_file_info);
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

                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterRemove--;
                    optionLeaveShares.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);

                    int accessLevel = megaApi.getAccess(node);
                    switch (accessLevel) {
                        case MegaShare.ACCESS_READWRITE:
                        case MegaShare.ACCESS_READ:
                        case MegaShare.ACCESS_UNKNOWN:
                            counterModify--;
                            optionRename.setVisibility(View.GONE);
                            counterModify--;
                            optionMove.setVisibility(View.GONE);
                            counterRemove--;
                            optionRubbishBin.setVisibility(View.GONE);
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            nodeIconLayout.setVisibility(View.GONE);
                            optionLinkText.setText(R.string.context_get_link_menu);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            break;

                        case MegaShare.ACCESS_FULL:
                        case MegaShare.ACCESS_OWNER:
                            optionLink.setVisibility(View.VISIBLE);
                            if (node.isExported()) {
                                nodeIconLayout.setVisibility(View.VISIBLE);
                                nodeIcon.setImageResource(R.drawable.link_ic);
                                optionLinkText.setText(R.string.edit_link_option);
                                optionRemoveLink.setVisibility(View.VISIBLE);
                            } else {
                                nodeIconLayout.setVisibility(View.GONE);
                                optionLinkText.setText(R.string.context_get_link_menu);
                                counterShares--;
                                optionRemoveLink.setVisibility(View.GONE);
                            }
                            break;
                    }
                    break;
                }

                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                    optionShareFolder.setVisibility(View.VISIBLE);
                    if (isOutShare(node)) {
                        optionShareFolderText.setText(R.string.manage_share);
                        optionClearShares.setVisibility(View.VISIBLE);
                    } else {
                        optionShareFolderText.setText(R.string.context_share_folder);
                        counterShares--;
                        optionClearShares.setVisibility(View.GONE);
                    }
                } else {
                    optionInfoText.setText(R.string.general_file_info);
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                }

                if (node.isExported()) {
                    //Node has public link
                    nodeIconLayout.setVisibility(View.VISIBLE);
                    nodeIcon.setImageResource(R.drawable.link_ic);

                    optionLinkText.setText(R.string.edit_link_option);
                    optionRemoveLink.setVisibility(View.VISIBLE);
                    if (node.isExpired()) {
                        logDebug("Node exported but expired!!");
                    }
                } else {
                    nodeIconLayout.setVisibility(View.GONE);
                    optionLinkText.setText(R.string.context_get_link_menu);
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

                //Hide
                counterRemove--;
                optionRemove.setVisibility(View.GONE);
                counterRemove--;
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);
                break;

            case RUBBISH_BIN:
                logDebug("show Rubbish bottom sheet");
                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                } else {
                    optionInfoText.setText(R.string.general_file_info);
                }

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

                optionMove.setVisibility(View.VISIBLE);
                optionRemove.setVisibility(View.VISIBLE);
                optionInfo.setVisibility(View.VISIBLE);
                optionRename.setVisibility(View.VISIBLE);
                optionCopy.setVisibility(View.VISIBLE);

                //Hide
                counterShares--;
                optionClearShares.setVisibility(View.GONE);
                counterRemove--;
                optionLeaveShares.setVisibility(View.GONE);
                counterRemove--;
                optionRubbishBin.setVisibility(View.GONE);
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

            case INBOX:

                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);

                } else {
                    optionInfoText.setText(R.string.general_file_info);
                }

                if (node.isExported()) {
                    //Node has public link
                    nodeIconLayout.setVisibility(View.VISIBLE);
                    nodeIcon.setImageResource(R.drawable.link_ic);
                    optionLinkText.setText(R.string.edit_link_option);
                    optionRemoveLink.setVisibility(View.VISIBLE);
                    if (node.isExpired()) {
                        logDebug("Node exported but expired!!");
                    }
                } else {
                    nodeIconLayout.setVisibility(View.GONE);
                    optionLinkText.setText(R.string.context_get_link_menu);
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
                counterRemove--;
                optionRemove.setVisibility(View.GONE);
                counterRemove--;
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterShares--;
                optionShareFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                break;

            case SHARED_ITEMS:

                int tabSelected = ((ManagerActivityLollipop) context).getTabItemShares();
                if (tabSelected == 0) {
                    logDebug("showOptionsPanelIncoming");

                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        counterShares--;
                        optionSendChat.setVisibility(View.GONE);
                    } else {
                        optionInfoText.setText(R.string.general_file_info);
                        optionSendChat.setVisibility(View.VISIBLE);
                    }

                    nodeIconLayout.setVisibility(View.GONE);

                    int accessLevel = megaApi.getAccess(node);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                    optionDownload.setVisibility(View.VISIBLE);
                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);

                    int dBT = ((ManagerActivityLollipop) context).getDeepBrowserTreeIncoming();
                    logDebug("DeepTree value:" + dBT);

                    if (dBT > 0) {
                        counterRemove--;
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
                                counterRemove--;
                                optionRubbishBin.setVisibility(View.GONE);
                                counterModify--;
                                optionMove.setVisibility(View.GONE);

                            }

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
                            counterRemove--;
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
                            counterRemove--;
                            optionRubbishBin.setVisibility(View.GONE);
                            break;
                    }
                } else if (tabSelected == 1) {
                    logDebug("showOptionsPanelOutgoing");

                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        optionShareFolder.setVisibility(View.VISIBLE);
                        optionShareFolderText.setText(R.string.manage_share);
                    } else {
                        optionInfoText.setText(R.string.general_file_info);
                        counterShares--;
                        optionShareFolder.setVisibility(View.GONE);
                    }

                    if (node.isExported()) {
                        //Node has public link
                        nodeIconLayout.setVisibility(View.VISIBLE);
                        nodeIcon.setImageResource(R.drawable.link_ic);
                        optionLinkText.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                        if (node.isExpired()) {
                            logDebug("Node exported but expired!!");
                        }
                    } else {
                        nodeIconLayout.setVisibility(View.GONE);
                        optionLinkText.setText(R.string.context_get_link_menu);
                        counterShares--;
                        optionRemoveLink.setVisibility(View.GONE);
                    }

                    if (((ManagerActivityLollipop) context).getDeepBrowserTreeOutgoing() == 0) {
                        optionClearShares.setVisibility(View.VISIBLE);

                        //Show the number of contacts who shared the folder
                        ArrayList<MegaShare> sl = megaApi.getOutShares(node);
                        if (sl != null) {
                            if (sl.size() != 0) {
                                nodeInfo.setText(context.getResources().getString(R.string.file_properties_shared_folder_select_contact) + " " + sl.size() + " " + context.getResources().getQuantityString(R.plurals.general_num_users, sl.size()));
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
                    optionMove.setVisibility(View.VISIBLE);
                    optionCopy.setVisibility(View.VISIBLE);
                    optionRubbishBin.setVisibility(View.VISIBLE);

                    //Hide
                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterRemove--;
                    optionLeaveShares.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                } else if (tabSelected == 2) {
                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        optionShareFolder.setVisibility(View.VISIBLE);
                        if (isOutShare(node)) {
                            optionShareFolderText.setText(R.string.manage_share);
                        } else {
                            optionShareFolderText.setText(R.string.context_share_folder);
                        }
                    } else {
                        optionInfoText.setText(R.string.general_file_info);
                        counterShares--;
                        optionShareFolder.setVisibility(View.GONE);
                    }

                    if (node.isExported()) {
                        //Node has public link
                        if (((ManagerActivityLollipop) context).getDeepBrowserTreeLinks() > 0) {
                            nodeIconLayout.setVisibility(View.VISIBLE);
                            nodeIcon.setImageResource(R.drawable.link_ic);
                        } else {
                            nodeIconLayout.setVisibility(View.GONE);
                        }

                        optionLinkText.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                    } else {
                        nodeIconLayout.setVisibility(View.GONE);
                        optionLinkText.setText(R.string.context_get_link_menu);
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
                    optionMove.setVisibility(View.VISIBLE);
                    optionCopy.setVisibility(View.VISIBLE);

                    //Hide
                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterRemove--;
                    optionLeaveShares.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                }

                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                break;

            case SEARCH:
                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                    optionShareFolder.setVisibility(View.VISIBLE);

                } else {
                    optionInfoText.setText(R.string.general_file_info);
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                }

                int dBT = nC.getIncomingLevel(node);
                if (nC.nodeComesFromIncoming(node)) {
                    logDebug("dBT: " + dBT);
                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        counterShares--;
                        optionSendChat.setVisibility(View.GONE);
                    } else {
                        optionInfoText.setText(R.string.general_file_info);
                        optionSendChat.setVisibility(View.VISIBLE);
                    }

                    nodeIconLayout.setVisibility(View.VISIBLE);

                    int accessLevel = megaApi.getAccess(node);
                    logDebug("Node: " + node.getName() + " " + accessLevel);
                    optionDownload.setVisibility(View.VISIBLE);
                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionShareFolder.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);

                    logDebug("DeepTree value:" + dBT);
                    if (dBT > 0) {
                        counterRemove--;
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
                                counterRemove--;
                                optionRubbishBin.setVisibility(View.GONE);
                                counterModify--;
                                optionMove.setVisibility(View.GONE);

                            }

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
                            counterRemove--;
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
                            counterRemove--;
                            optionRubbishBin.setVisibility(View.GONE);
                            break;
                    }
                } else {
                    if (node.isExported()) {
                        //Node has public link
                        nodeIconLayout.setVisibility(View.VISIBLE);
                        nodeIcon.setImageResource(R.drawable.link_ic);
                        optionLinkText.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                        if (node.isExpired()) {
                            logDebug("Node exported but expired!!");
                        }
                    } else {
                        nodeIconLayout.setVisibility(View.GONE);
                        optionLinkText.setText(R.string.context_get_link_menu);
                        counterShares--;
                        optionRemoveLink.setVisibility(View.GONE);
                    }
                    MegaNode parent = nC.getParent(node);
                    if (parent.getHandle() != megaApi.getRubbishNode().getHandle()) {
                        optionRubbishBin.setVisibility(View.VISIBLE);
                        counterRemove--;
                        optionRemove.setVisibility(View.GONE);
                    } else {
                        counterRemove--;
                        optionRubbishBin.setVisibility(View.GONE);
                        optionRemove.setVisibility(View.VISIBLE);
                    }

                    optionDownload.setVisibility(View.VISIBLE);

                    offlineSwitch.setChecked(availableOffline(context, node));
                    optionInfo.setVisibility(View.VISIBLE);
                    optionLink.setVisibility(View.VISIBLE);
                    optionRename.setVisibility(View.VISIBLE);
                    optionOpenFolder.setVisibility(View.VISIBLE);

                    //Hide
                    counterModify--;
                    optionMove.setVisibility(View.GONE);
                    counterModify--;
                    optionCopy.setVisibility(View.GONE);
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                    counterRemove--;
                    optionLeaveShares.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);
                }
                break;
        }

        separatorDownload.setVisibility(counterSave <= 0 ? View.GONE : View.VISIBLE);
        separatorShares.setVisibility(counterShares <= 0 ? View.GONE : View.VISIBLE);
        separatorModify.setVisibility(counterModify <= 0 ? View.GONE : View.VISIBLE);
        separatorOpen.setVisibility(counterOpen <= 0 || counterRemove <= 0 ? View.GONE : View.VISIBLE);

        offlineSwitch.setOnCheckedChangeListener((view, isChecked) -> onClick(view));

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
            case R.id.option_download_layout:
                nC.prepareForDownload(handleList, false);
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

            case R.id.option_properties_layout:
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
                } else if (drawerItem == ManagerActivityLollipop.DrawerItem.SEARCH
                        || (context instanceof ManagerActivityLollipop && ((ManagerActivityLollipop) context).isOnRecents())) {
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

            case R.id.option_link_layout:
                ((ManagerActivityLollipop) context).showGetLinkActivity(node.getHandle());
                break;

            case R.id.option_remove_link_layout:
                ((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(node);
                break;

            case R.id.option_share_folder_layout:
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

            case R.id.option_clear_share_layout:
                ArrayList<MegaShare> shareList = megaApi.getOutShares(node);
                ((ManagerActivityLollipop) context).showConfirmationRemoveAllSharingContacts(shareList, node);
                break;

            case R.id.option_leave_share_layout:
                showConfirmationLeaveIncomingShare(context, node);
                break;

            case R.id.option_send_chat_layout:
                if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                    showOverDiskQuotaPaywallWarning();
                    break;
                }
                nC.checkIfNodeIsMineAndSelectChatsToSendNode(node);
                dismissAllowingStateLoss();
                break;

            case R.id.option_rename_layout:
                ((ManagerActivityLollipop) context).showRenameDialog(node, node.getName());

                break;

            case R.id.option_move_layout:
                nC.chooseLocationToMoveNodes(handleList);
                dismissAllowingStateLoss();
                break;

            case R.id.option_copy_layout:
                nC.chooseLocationToCopyNodes(handleList);
                dismissAllowingStateLoss();
                break;

            case R.id.option_rubbish_bin_layout:
            case R.id.option_remove_layout:
                ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;

            case R.id.option_open_folder_layout:
                nC.openFolderFromSearch(node.getHandle());
                dismissAllowingStateLoss();
                break;

            case R.id.option_open_with_layout:
                openWith(node);
                break;

            case R.id.option_restore_layout:
                ((ManagerActivityLollipop) context).restoreFromRubbish(node);
                break;

            case R.id.option_share_layout:
                shareNode(context, node);
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
    }
}
