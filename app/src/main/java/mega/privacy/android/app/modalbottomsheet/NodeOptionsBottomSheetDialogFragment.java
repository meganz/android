package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_FILE_INFO;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;

public class NodeOptionsBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;
    private MegaNode node = null;
    private NodeController nC;

    private BottomSheetBehavior mBehavior;

    private LinearLayout mainLinearLayout;
    private CoordinatorLayout coordinatorLayout;

    private ImageView nodeThumb;
    private TextView nodeName;
    private TextView nodeInfo;
    private ImageView nodeVersionsIcon;
    private RelativeLayout nodeIconLayout;
    private ImageView nodeIcon;
    private LinearLayout optionDownload;
    private LinearLayout optionOffline;
    private TextView optionOfflineText;
    private LinearLayout optionInfo;
    private TextView optionInfoText;
    private ImageView optionInfoImage;
    private LinearLayout optionLink;
    private TextView optionLinkText;
    private ImageView optionLinkImage;
    private LinearLayout optionRemoveLink;
    private LinearLayout optionShare;
    private TextView optionShareText;
    private LinearLayout optionClearShares;
    private LinearLayout optionLeaveShares;
    private LinearLayout optionSendChat;
    private LinearLayout optionRename;
    private LinearLayout optionMove;
    private LinearLayout optionCopy;
    private LinearLayout optionRubbishBin;
    private LinearLayout optionRemove;
    private LinearLayout optionRestoreFromRubbish;
    private LinearLayout optionOpenFolder;
    private LinearLayout optionOpenWith;

    private LinearLayout items_layout;
    private RelativeLayout node_head;

    private DisplayMetrics outMetrics;

    private ManagerActivityLollipop.DrawerItem drawerItem;
    private Bitmap thumb = null;

    private MegaApiAndroid megaApi;
    private DatabaseHandler dbH;

    private int height = -1;
    private boolean heightseted = false;
    private int heightReal = -1;
    private int heightDisplay;

    private View contentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");
            long handle = savedInstanceState.getLong("handle", -1);
            height = savedInstanceState.getInt("height", -1);
            log("Handle of the node: "+handle);
            node = megaApi.getNodeByHandle(handle);
            if(context instanceof ManagerActivityLollipop){
                drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
            }
        }
        else{
            log("Bundle NULL");
            if(context instanceof ManagerActivityLollipop){
                node = ((ManagerActivityLollipop) context).getSelectedNode();
                drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
            }
        }

        nC = new NodeController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);
        log("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_item, null);

        contentView.post(new Runnable() {
            @Override
            public void run() {
                heightReal = contentView.getHeight();
            }
        });

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.node_bottom_sheet);

        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout_bottom_sheet_node);
        node_head = (RelativeLayout) contentView.findViewById(R.id.node_title_layout);

        nodeThumb = (ImageView) contentView.findViewById(R.id.node_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.node_name_text);
        nodeInfo = (TextView) contentView.findViewById(R.id.node_info_text);
        nodeVersionsIcon = (ImageView) contentView.findViewById(R.id.node_info_versions_icon);
        nodeIconLayout = (RelativeLayout) contentView.findViewById(R.id.node_relative_layout_icon);
        nodeIcon = (ImageView) contentView.findViewById(R.id.node_icon);

        optionInfo = (LinearLayout) contentView.findViewById(R.id.option_properties_layout);
        optionInfoText = (TextView) contentView.findViewById(R.id.option_properties_text);
        optionInfoImage = (ImageView) contentView.findViewById(R.id.option_properties_image);
//      counterSave
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionOffline = (LinearLayout) contentView.findViewById(R.id.option_offline_layout);
        optionOfflineText = (TextView) contentView.findViewById(R.id.option_offline_text);
//      counterShares
        optionLink = (LinearLayout) contentView.findViewById(R.id.option_link_layout);
        optionLinkText = (TextView) contentView.findViewById(R.id.option_link_text);
        optionLinkImage = (ImageView) contentView.findViewById(R.id.option_link_image);
        optionRemoveLink = (LinearLayout) contentView.findViewById(R.id.option_remove_link_layout);
        optionShare = (LinearLayout) contentView.findViewById(R.id.option_share_layout);
        optionShareText = (TextView) contentView.findViewById(R.id.option_share_text);
        optionClearShares = (LinearLayout) contentView.findViewById(R.id.option_clear_share_layout);
        optionLeaveShares = (LinearLayout) contentView.findViewById(R.id.option_leave_share_layout);
        optionSendChat = (LinearLayout) contentView.findViewById(R.id.option_send_chat_layout);
//      counterModify
        optionRename = (LinearLayout) contentView.findViewById(R.id.option_rename_layout);
        optionMove = (LinearLayout) contentView.findViewById(R.id.option_move_layout);
        optionCopy = (LinearLayout) contentView.findViewById(R.id.option_copy_layout);
        optionRestoreFromRubbish = (LinearLayout) contentView.findViewById(R.id.option_restore_layout);
//      counterOpen
        optionOpenFolder = (LinearLayout) contentView.findViewById(R.id.option_open_folder_layout);
        optionOpenWith = (LinearLayout) contentView.findViewById(R.id.option_open_with_layout);
//      counterRemove
        optionRubbishBin = (LinearLayout) contentView.findViewById(R.id.option_rubbish_bin_layout);
        optionRemove = (LinearLayout) contentView.findViewById(R.id.option_remove_layout);

        optionDownload.setOnClickListener(this);
        optionOffline.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionLink.setOnClickListener(this);
        optionRemoveLink.setOnClickListener(this);
        optionShare.setOnClickListener(this);
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
        int counterRemove = 2;

        LinearLayout separatorDownload = (LinearLayout) contentView.findViewById(R.id.separator_download_options);
        LinearLayout separatorShares = (LinearLayout) contentView.findViewById(R.id.separator_share_options);
        LinearLayout separatorModify = (LinearLayout) contentView.findViewById(R.id.separator_modify_options);
        LinearLayout separatorOpen = (LinearLayout) contentView.findViewById(R.id.separator_open_options);

        nodeIconLayout.setVisibility(View.GONE);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            log("onCreate: Landscape configuration");
            nodeName.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
            nodeInfo.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
        } else {
            nodeName.setMaxWidth(Util.scaleWidthPx(210, outMetrics));
            nodeInfo.setMaxWidth(Util.scaleWidthPx(210, outMetrics));
        }

        if (node == null) return;

        if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isVideo() || MimeTypeList.typeForName(node.getName()).isAudio()
                || MimeTypeList.typeForName(node.getName()).isImage() || MimeTypeList.typeForName(node.getName()).isPdf()) {
            optionOpenWith.setVisibility(View.VISIBLE);
        } else {
            counterOpen--;
            optionOpenWith.setVisibility(View.GONE);
        }

        if (Util.isOnline(context)) {
            nodeName.setText(node.getName());

            if (node.isFolder()) {
                nodeInfo.setText(MegaApiUtils.getInfoFolder(node, context, megaApi));
                nodeVersionsIcon.setVisibility(View.GONE);

                if (node.isInShare()) {
                    nodeThumb.setImageResource(R.drawable.ic_folder_incoming);
                } else if (node.isOutShare() || megaApi.isPendingShare(node)) {
                    nodeThumb.setImageResource(R.drawable.ic_folder_outgoing);
                }
                else{
//                        nodeThumb.setImageResource(R.drawable.ic_folder_list);
                    if(((ManagerActivityLollipop) context).isCameraUploads(node)){
                        nodeThumb.setImageResource(R.drawable.ic_folder_image_list);
                    }else{
                        nodeThumb.setImageResource(R.drawable.ic_folder_list);
                    }
                }
                counterShares--;
                optionSendChat.setVisibility(View.GONE);
            } else {
                long nodeSize = node.getSize();
                nodeInfo.setText(Util.getSizeString(nodeSize));

                if(megaApi.hasVersions(node)){
                    nodeVersionsIcon.setVisibility(View.VISIBLE);
                }
                else{
                    nodeVersionsIcon.setVisibility(View.GONE);
                }

                if (node.hasThumbnail()) {
                    log("Node has thumbnail");
                    RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                    params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                    params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                    params1.setMargins(20, 0, 12, 0);
                    nodeThumb.setLayoutParams(params1);

                    thumb = ThumbnailUtils.getThumbnailFromCache(node);
                    if (thumb != null) {
                        nodeThumb.setImageBitmap(thumb);
                    } else {
                        thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                        if (thumb != null) {
                            nodeThumb.setImageBitmap(thumb);
                        } else {
                            nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        }
                    }
                } else {
                    nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                }
                if (Util.isChatEnabled()) {
                    optionSendChat.setVisibility(View.VISIBLE);
                }
                else {
                    counterShares--;
                    optionSendChat.setVisibility(View.GONE);
                }
            }
        }


        switch (drawerItem) {
            case CLOUD_DRIVE: {
                log("show Cloud bottom sheet");
                if (((ManagerActivityLollipop) context).isOnRecents()) {
                    optionInfoText.setText(R.string.general_file_info);
                    counterShares--;
                    optionShare.setVisibility(View.GONE);
                    nodeIconLayout.setVisibility(View.GONE);
                    counterShares--;
                    optionLink.setVisibility(View.GONE);
                    counterShares--;
                    optionRemoveLink.setVisibility(View.GONE);
                    counterShares--;
                    optionClearShares.setVisibility(View.GONE);
                    if (availableOffline(context, node)) {
                        optionOfflineText.setText(getString(R.string.context_delete_offline));
                    } else {
                        optionOfflineText.setText(getString(R.string.save_for_offline));
                    }

                    if (Util.isChatEnabled()) {
                        optionSendChat.setVisibility(View.VISIBLE);
                    } else {
                        counterShares--;
                        optionSendChat.setVisibility(View.GONE);
                    }

                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionLeaveShares.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);

                    int accessLevel = megaApi.getAccess(node);

                    switch (accessLevel) {
                        case MegaShare.ACCESS_READWRITE:
                        case MegaShare.ACCESS_READ:
                        case MegaShare.ACCESS_UNKNOWN: {
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
                        }
                        case MegaShare.ACCESS_FULL:
                        case MegaShare.ACCESS_OWNER: {
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
                    }
                    break;
                }

                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                    optionShare.setVisibility(View.VISIBLE);
                    if (node.isOutShare() || megaApi.isPendingShare(node)) {
                        optionShareText.setText(R.string.context_sharing_folder);
                    } else {
                        optionShareText.setText(R.string.context_share_folder);
                    }
                } else {
                    optionInfoText.setText(R.string.general_file_info);
                    counterShares--;
                    optionShare.setVisibility(View.GONE);
                }

                if (node.isExported()) {
                    //Node has public link
                    nodeIconLayout.setVisibility(View.VISIBLE);
                    nodeIcon.setImageResource(R.drawable.link_ic);

                    optionLinkText.setText(R.string.edit_link_option);
                    optionRemoveLink.setVisibility(View.VISIBLE);
                    if (node.isExpired()) {
                        log("Node exported but expired!!");
                    }
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

                if (availableOffline(context, node)) {
                    optionOfflineText.setText(getString(R.string.context_delete_offline));
                }
                else {
                    optionOfflineText.setText(getString(R.string.save_for_offline));
                }
                optionInfo.setVisibility(View.VISIBLE);
                optionRubbishBin.setVisibility(View.VISIBLE);
                optionLink.setVisibility(View.VISIBLE);

                optionRename.setVisibility(View.VISIBLE);
                optionMove.setVisibility(View.VISIBLE);
                optionCopy.setVisibility(View.VISIBLE);

                //Hide
                counterRemove--;
                optionRemove.setVisibility(View.GONE);
                counterShares--;
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);
                break;
            }
            case RUBBISH_BIN: {
                log("show Rubbish bottom sheet");
                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                } else {
                    optionInfoText.setText(R.string.general_file_info);
                }

                long restoreHandle = node.getRestoreHandle();
                if(restoreHandle!=-1){
                    MegaNode restoreNode = megaApi.getNodeByHandle(restoreHandle);
                    if((!megaApi.isInRubbish(node)) || restoreNode==null || megaApi.isInRubbish(restoreNode)){
                        counterModify--;
                        optionRestoreFromRubbish.setVisibility(View.GONE);
                    }
                    else{
                        optionRestoreFromRubbish.setVisibility(View.VISIBLE);
                    }
                }
                else{
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
                counterShares--;
                optionLeaveShares.setVisibility(View.GONE);
                counterRemove--;
                optionRubbishBin.setVisibility(View.GONE);
                counterShares--;
                optionShare.setVisibility(View.GONE);
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
            case INBOX: {

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
                        log("Node exported but expired!!");
                    }
                } else {
                    nodeIconLayout.setVisibility(View.GONE);
                    optionLinkText.setText(R.string.context_get_link_menu);
                    counterShares--;
                    optionRemoveLink.setVisibility(View.GONE);
                }

                optionDownload.setVisibility(View.VISIBLE);
                if (availableOffline(context, node)) {
                    optionOfflineText.setText(getString(R.string.context_delete_offline));
                }
                else {
                    optionOfflineText.setText(getString(R.string.save_for_offline));
                }
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
                counterShares--;
                optionLeaveShares.setVisibility(View.GONE);
                counterOpen--;
                optionOpenFolder.setVisibility(View.GONE);
                counterShares--;
                optionShare.setVisibility(View.GONE);
                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                break;
            }
            case SHARED_ITEMS: {

                int tabSelected = ((ManagerActivityLollipop) context).getTabItemShares();
                if (tabSelected == 0) {
                    log("showOptionsPanelIncoming");

                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        counterShares--;
                        optionSendChat.setVisibility(View.GONE);
                    } else {
                        optionInfoText.setText(R.string.general_file_info);
                        if (Util.isChatEnabled()) {
                            optionSendChat.setVisibility(View.VISIBLE);
                        }
                        else {
                            counterShares--;
                            optionSendChat.setVisibility(View.GONE);
                        }
                    }

                    nodeIconLayout.setVisibility(View.VISIBLE);

                    int accessLevel = megaApi.getAccess(node);
                    log("Node: " + node.getName() + " " + accessLevel);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                    optionDownload.setVisibility(View.VISIBLE);
                    if (availableOffline(context, node)) {
                        optionOfflineText.setText(getString(R.string.context_delete_offline));
                    }
                    else {
                        optionOfflineText.setText(getString(R.string.save_for_offline));
                    }
                    optionInfo.setVisibility(View.VISIBLE);
                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionShare.setVisibility(View.GONE);

                    int dBT = ((ManagerActivityLollipop) context).getDeepBrowserTreeIncoming();
                    log("DeepTree value:" + dBT);
                    if (dBT > 0) {
                        counterShares--;
                        optionLeaveShares.setVisibility(View.GONE);
                        nodeIconLayout.setVisibility(View.GONE);
                    } else {
                        //Show the owner of the shared folder
                        ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                        for (int j = 0; j < sharesIncoming.size(); j++) {
                            MegaShare mS = sharesIncoming.get(j);
                            if (mS.getNodeHandle() == node.getHandle()) {
                                MegaUser user = megaApi.getContact(mS.getUser());
                                if (user != null) {
                                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
                                    if (contactDB != null) {
                                        if (!contactDB.getName().equals("")) {
                                            nodeInfo.setText(contactDB.getName() + " " + contactDB.getLastName());
                                        } else {
                                            nodeInfo.setText(user.getEmail());
                                        }
                                    } else {
                                        log("The contactDB is null: ");
                                        nodeInfo.setText(user.getEmail());
                                    }
                                } else {
                                    nodeInfo.setText(mS.getUser());
                                }
                            }
                        }
                        optionLeaveShares.setVisibility(View.VISIBLE);

                        switch (accessLevel) {
                            case MegaShare.ACCESS_FULL: {
                                log("LEVEL 0 - access FULL");
                                nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                                break;
                            }
                            case MegaShare.ACCESS_READ: {
                                log("LEVEL 0 - access read");
                                nodeIcon.setImageResource(R.drawable.ic_shared_read);
                                break;
                            }
                            case MegaShare.ACCESS_READWRITE: {
                                log("LEVEL 0 - readwrite");
                                nodeIcon.setImageResource(R.drawable.ic_shared_read_write);
                            }
                        }
                    }

                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL: {
                            log("access FULL");
                            counterShares--;
                            optionLink.setVisibility(View.GONE);
                            counterShares--;
                            optionRemoveLink.setVisibility(View.GONE);
                            counterShares--;
                            optionClearShares.setVisibility(View.GONE);
                            optionRename.setVisibility(View.VISIBLE);
                            //optionMove.setVisibility(View.GONE);

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
                        }
                        case MegaShare.ACCESS_READ: {
                            log("access read");
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
                        case MegaShare.ACCESS_READWRITE: {
                            log("readwrite");
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
                    }
                } else if (tabSelected == 1) {
                    log("showOptionsPanelOutgoing");

                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        optionShare.setVisibility(View.VISIBLE);
                        optionShareText.setText(R.string.context_sharing_folder);
                    } else {
                        optionInfoText.setText(R.string.general_file_info);
                        counterShares--;
                        optionShare.setVisibility(View.GONE);
                    }

                    if (node.isExported()) {
                        //Node has public link
                        nodeIconLayout.setVisibility(View.VISIBLE);
                        nodeIcon.setImageResource(R.drawable.link_ic);
                        optionLinkText.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                        if (node.isExpired()) {
                            log("Node exported but expired!!");
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
                    if (availableOffline(context, node)) {
                        optionOfflineText.setText(getString(R.string.context_delete_offline));
                    }
                    else {
                        optionOfflineText.setText(getString(R.string.save_for_offline));
                    }
                    optionInfo.setVisibility(View.VISIBLE);
                    optionRename.setVisibility(View.VISIBLE);
                    optionMove.setVisibility(View.VISIBLE);
                    optionCopy.setVisibility(View.VISIBLE);
                    optionRubbishBin.setVisibility(View.VISIBLE);

                    //Hide
                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionLeaveShares.setVisibility(View.GONE);
                    counterOpen--;
                    optionOpenFolder.setVisibility(View.GONE);
                }

                counterModify--;
                optionRestoreFromRubbish.setVisibility(View.GONE);

                break;
            }
            case SEARCH: {
                if (node.isFolder()) {
                    optionInfoText.setText(R.string.general_folder_info);
                    optionShare.setVisibility(View.VISIBLE);

                } else {
                    optionInfoText.setText(R.string.general_file_info);
                    counterShares--;
                    optionShare.setVisibility(View.GONE);
                }

                int dBT = nC.getIncomingLevel(node);
                if (nC.nodeComesFromIncoming(node)) {
                    log("dBT: "+dBT);
                    if (node.isFolder()) {
                        optionInfoText.setText(R.string.general_folder_info);
                        counterShares--;
                        optionSendChat.setVisibility(View.GONE);
                    } else {
                        optionInfoText.setText(R.string.general_file_info);
                        if (Util.isChatEnabled()) {
                            optionSendChat.setVisibility(View.VISIBLE);
                        }
                        else {
                            counterShares--;
                            optionSendChat.setVisibility(View.GONE);
                        }
                    }

                    nodeIconLayout.setVisibility(View.VISIBLE);

                    int accessLevel = megaApi.getAccess(node);
                    log("Node: " + node.getName() + " " + accessLevel);
                    optionDownload.setVisibility(View.VISIBLE);
                    if (availableOffline(context, node)) {
                        optionOfflineText.setText(getString(R.string.context_delete_offline));
                    }
                    else {
                        optionOfflineText.setText(getString(R.string.save_for_offline));
                    }
                    optionInfo.setVisibility(View.VISIBLE);
                    counterRemove--;
                    optionRemove.setVisibility(View.GONE);
                    counterShares--;
                    optionShare.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);

                    log("DeepTree value:" + dBT);
                    if (dBT > 0) {
                        counterShares--;
                        optionLeaveShares.setVisibility(View.GONE);
                        nodeIconLayout.setVisibility(View.GONE);
                    } else {
                        //Show the owner of the shared folder
                        ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                        for (int j = 0; j < sharesIncoming.size(); j++) {
                            MegaShare mS = sharesIncoming.get(j);
                            if (mS.getNodeHandle() == node.getHandle()) {
                                MegaUser user = megaApi.getContact(mS.getUser());
                                if (user != null) {
                                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
                                    if (contactDB != null) {
                                        if (!contactDB.getName().equals("")) {
                                            nodeInfo.setText(contactDB.getName() + " " + contactDB.getLastName());
                                        } else {
                                            nodeInfo.setText(user.getEmail());
                                        }
                                    } else {
                                        log("The contactDB is null: ");
                                        nodeInfo.setText(user.getEmail());
                                    }
                                } else {
                                    nodeInfo.setText(mS.getUser());
                                }
                            }
                        }
                        optionLeaveShares.setVisibility(View.VISIBLE);

                        switch (accessLevel) {
                            case MegaShare.ACCESS_FULL: {
                                log("LEVEL 0 - access FULL");
                                nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess);
                                break;
                            }
                            case MegaShare.ACCESS_READ: {
                                log("LEVEL 0 - access read");
                                nodeIcon.setImageResource(R.drawable.ic_shared_read);
                                break;
                            }
                            case MegaShare.ACCESS_READWRITE: {
                                log("LEVEL 0 - readwrite");
                                nodeIcon.setImageResource(R.drawable.ic_shared_read_write);
                            }
                        }
                    }

                    switch (accessLevel) {
                        case MegaShare.ACCESS_FULL: {
                            log("access FULL");
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
                        }
                        case MegaShare.ACCESS_READ: {
                            log("access read");
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
                        case MegaShare.ACCESS_READWRITE: {
                            log("readwrite");
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
                    }
                }
                else {
                    if (node.isExported()) {
                        //Node has public link
                        nodeIconLayout.setVisibility(View.VISIBLE);
                        nodeIcon.setImageResource(R.drawable.link_ic);
                        optionLinkText.setText(R.string.edit_link_option);
                        optionRemoveLink.setVisibility(View.VISIBLE);
                        if (node.isExpired()) {
                            log("Node exported but expired!!");
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
                    if (availableOffline(context, node)) {
                        optionOfflineText.setText(getString(R.string.context_delete_offline));
                    }
                    else {
                        optionOfflineText.setText(getString(R.string.save_for_offline));
                    }
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
                    counterShares--;
                    optionLeaveShares.setVisibility(View.GONE);
                    counterModify--;
                    optionRestoreFromRubbish.setVisibility(View.GONE);
                }
                break;
            }
        }

        if (counterSave <= 0){
            separatorDownload.setVisibility(View.GONE);
        }
        else {
            separatorDownload.setVisibility(View.VISIBLE);
        }
        if (counterShares <= 0){
            separatorShares.setVisibility(View.GONE);
        }
        else {
            separatorShares.setVisibility(View.VISIBLE);
        }
        if (counterModify <= 0){
            separatorModify.setVisibility(View.GONE);
        }
        else {
            separatorModify.setVisibility(View.VISIBLE);
        }
        if (counterOpen <= 0 || counterRemove <= 0) {
            separatorOpen.setVisibility(View.GONE);
        }
        else {
            separatorOpen.setVisibility(View.VISIBLE);
        }

        dialog.setContentView(contentView);

        mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
//            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

//            final NodeOptionsBottomSheetDialogFragment thisclass = this;
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

//            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                mBehavior.setPeekHeight((heightDisplay / 2) * 2);
//            }
//            else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//                mBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
//            }

        mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                    if(slideOffset> 0 && !heightseted){
//                        log("HeightReal is "+ heightReal);
//                        if(context instanceof CustomHeight){
//                            height = ((CustomHeight) context).getHeightToPanel(thisclass);
//                        }
//                        log("Height is "+height);
//                        if(height != -1 && heightReal != -1){
//                            heightseted = true;
//                            int numSons = 0;
//                            int num = items_layout.getChildCount();
//                            for(int i=0; i<num; i++){
//                                View v = items_layout.getChildAt(i);
//                                if(v.getVisibility() == View.VISIBLE){
//                                    numSons++;
//                                }
//                            }
//                            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && numSons > 3){
//
//                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
//                                params.height = height;
//                                bottomSheet.setLayoutParams(params);
//                            }
//                            else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && numSons > 9){
//                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
//                                params.height = height;
//                                bottomSheet.setLayoutParams(params);
//                            }
//                            if(heightReal > height){
//                                ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
//                                params.height = height;
//                                bottomSheet.setLayoutParams(params);
//                            }
//                        }
//                    }

                if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                    ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                    if (getActivity() != null && getActivity().findViewById(R.id.toolbar) != null) {
                        int tBHeight = getActivity().findViewById(R.id.toolbar).getHeight();
                        Rect rectangle = new Rect();
                        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                        int windowHeight = rectangle.bottom;
                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8, context.getResources().getDisplayMetrics());
                        int maxHeight = windowHeight - tBHeight - rectangle.top - padding;

                        log("bottomSheet.height: "+mainLinearLayout.getHeight()+" maxHeight: "+maxHeight);
                        if (mainLinearLayout.getHeight() > maxHeight) {
                            params.height = maxHeight;
                            bottomSheet.setLayoutParams(params);
                        }
                    }
                }
            }
        });
    }

    //    private int getBottomSheetMaximumHeight() {
//        // get toolbar height
//        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
//        int toolbarHeight = toolbar.getHeight();
//
//        //get status bar height
//        Rect rectangle = new Rect();
//        Window window = getActivity().getWindow();
//        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
//        int windowHeight = rectangle.bottom;
//
//        // material design recommended bottomsheet padding from actionbar
//        final int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8, getContext().getResources().getDisplayMetrics());
//
//        // maximum height of the bottomsheet
////        return windowHeight - toolbarHeight - rectangle.top - padding;
//        return toolbarHeight + rectangle.top + padding;
//
//    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_download_layout:{
                log("Download option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                nC.prepareForDownload(handleList, false);
                break;
            }
            case R.id.option_offline_layout: {
                if (node==null) {
                    log("The selected node is NULL");
                    return;
                }
                if (availableOffline(context, node)) {
                    MegaOffline mOffDelete = dbH.findByHandle(node.getHandle());
                    removeFromOffline(mOffDelete);
                }
                else {
                    saveForOffline();
                }
                break;
            }
            case R.id.option_properties_layout:{
                log("Properties option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FileInfoActivityLollipop.class);
                i.putExtra("handle", node.getHandle());

                if(drawerItem== ManagerActivityLollipop.DrawerItem.SHARED_ITEMS){
                    if(((ManagerActivityLollipop) context).getTabItemShares()==0){
                        i.putExtra("from", Constants.FROM_INCOMING_SHARES);
                        int dBT = ((ManagerActivityLollipop) context).getDeepBrowserTreeIncoming();
                        if(dBT<=0){
                            log("First LEVEL is true: "+dBT);
                            i.putExtra("firstLevel", true);
                        }
                        else{
                            log("First LEVEL is false: "+dBT);
                            i.putExtra("firstLevel", false);
                        }
                    }
                }
                else if(drawerItem== ManagerActivityLollipop.DrawerItem.INBOX){
                    if(((ManagerActivityLollipop) context).getTabItemShares()==0){
                        i.putExtra("from", Constants.FROM_INBOX);
                    }
                }
                else if (drawerItem == ManagerActivityLollipop.DrawerItem.SEARCH
                        || (context instanceof ManagerActivityLollipop && ((ManagerActivityLollipop) context).isOnRecents())) {
                    if (nC.nodeComesFromIncoming(node)){
                        i.putExtra("from", Constants.FROM_INCOMING_SHARES);
                        int dBT = nC.getIncomingLevel(node);
                        if(dBT<=0){
                            i.putExtra("firstLevel", true);
                        }
                        else{
                            i.putExtra("firstLevel", false);
                        }
                    }
                }

                if (node.isFolder()) {
                    if (node.isInShare()){
                        i.putExtra("imageId", R.drawable.ic_folder_incoming);
                    }
                    else if (node.isOutShare()||megaApi.isPendingShare(node)){
                        i.putExtra("imageId", R.drawable.ic_folder_outgoing);
                    }
                    else{
                        i.putExtra("imageId", R.drawable.ic_folder);
                    }
                }
                else {
                    i.putExtra("imageId", MimeTypeThumbnail.typeForName(node.getName()).getIconResourceId());
                }
                i.putExtra("name", node.getName());

                ((ManagerActivityLollipop)context).startActivityForResult(i, REQUEST_CODE_FILE_INFO);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_link_layout:{
                log("Public link option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showGetLinkActivity(node.getHandle());
                break;
            }
            case R.id.option_remove_link_layout:{
                log("REMOVE public link option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showConfirmationRemovePublicLink(node);
                break;
            }
            case R.id.option_share_layout:{
                log("Share option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                if(node.isOutShare()||megaApi.isPendingShare(node)){
                    Intent i = new Intent(context, FileContactListActivityLollipop.class);
                    i.putExtra("name", node.getHandle());
                    context.startActivity(i);
                    dismissAllowingStateLoss();
                }
                else{
                    nC.selectContactToShareFolder(node);
                    dismissAllowingStateLoss();
                }

                break;
            }
            case R.id.option_clear_share_layout:{
                log("Clear shares");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<MegaShare> shareList = megaApi.getOutShares(node);
                ((ManagerActivityLollipop) context).showConfirmationRemoveAllSharingContacts(shareList, node);
                break;
            }
            case R.id.option_leave_share_layout:{
                log("Leave share option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showConfirmationLeaveIncomingShare(node);
                break;
            }
            case R.id.option_send_chat_layout:{
                log("Send chat option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.checkIfNodeIsMineAndSelectChatsToSendNode(node);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rename_layout:{
                log("Rename option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).showRenameDialog(node, node.getName());

                break;
            }
            case R.id.option_move_layout:{
                log("Move option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                nC.chooseLocationToMoveNodes(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_copy_layout:{
                log("Copy option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                nC.chooseLocationToCopyNodes(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rubbish_bin_layout:{
                log("Move to rubbish option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;
            }
            case R.id.option_remove_layout:{
                log("Remove option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ManagerActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
                break;
            }
            case R.id.option_open_folder_layout:{
                log("Open folder option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                nC.openFolderFromSearch(node.getHandle());
                dismissAllowingStateLoss();
                break;
            }

            case R.id.option_open_with_layout:{
                log("Open with");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                UtilsModalBottomSheet.openWith(megaApi, context, node);
                break;
            }
            case R.id.option_restore_layout:{
                log("Restore option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ManagerActivityLollipop) context).restoreFromRubbish(node);

                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    void refreshView () {
        switch (drawerItem) {
            case CLOUD_DRIVE:
            case RUBBISH_BIN: {
                ((ManagerActivityLollipop) context).onNodesCloudDriveUpdate();
                break;
            }
            case INBOX: {
                ((ManagerActivityLollipop) context).onNodesInboxUpdate();
                break;
            }
            case SHARED_ITEMS: {
                ((ManagerActivityLollipop) context).onNodesSharedUpdate();
                break;
            }
            case SEARCH: {
                ((ManagerActivityLollipop) context).onNodesSearchUpdate();
                break;
            }
        }
    }

    void removeFromOffline (MegaOffline mOffDelete) {
        removeOffline(mOffDelete, dbH, context);
        refreshView ();
    }

    void saveForOffline () {
        int adapterType;

        switch (drawerItem) {
            case INBOX: {
                adapterType = Constants.FROM_INBOX;
                break;
            }
            case SHARED_ITEMS: {
                if (((ManagerActivityLollipop) context).getTabItemShares() == 0) {
                    adapterType = Constants.FROM_INCOMING_SHARES;
                    break;
                }
            }
            default: {
                adapterType = Constants.FROM_OTHERS;
            }
        }

        File offlineParent = getOfflineParentFile(context, adapterType, node, megaApi);

        if (isFileAvailable(offlineParent)) {
            File offlineFile = new File(offlineParent, node.getName());
            if (isFileAvailable(offlineFile)) {
                return;
            }
        }

        saveOffline(offlineParent, node, context, (ManagerActivityLollipop) context, megaApi);
    }

    @Override
    public void onAttach(Activity activity) {
        log("onAttach");
        super.onAttach(activity);
        this.context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        log("Handle of the node: "+handle);
        outState.putLong("handle", handle);
    }

    private static void log(String log) {
        Util.log("NodeOptionsBottomSheetDialogFragment", log);
    }

    public interface CustomHeight{
        int getHeightToPanel(BottomSheetDialogFragment dialog);
    }
}
