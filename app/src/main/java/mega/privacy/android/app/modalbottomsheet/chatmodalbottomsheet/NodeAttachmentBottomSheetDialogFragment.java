package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class NodeAttachmentBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaNode node = null;
    MegaNodeList nodeList;
    MegaChatMessage messageMega;
    AndroidMegaChatMessage message = null;
    long chatId;
    long messageId;
    long handle=-1;
    ChatController chatC;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    LinearLayout mainLinearLayout;
    CoordinatorLayout coordinatorLayout;

    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    RelativeLayout nodeIconLayout;
    ImageView nodeIcon;
    LinearLayout optionView;
    TextView optionViewText;
    LinearLayout optionDownload;
    LinearLayout optionImport;
    LinearLayout optionSaveOffline;
    LinearLayout optionRemove;
    LinearLayout optionForwardLayout;

    DisplayMetrics outMetrics;

    static ManagerActivityLollipop.DrawerItem drawerItem = null;
    Bitmap thumb = null;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;

    private int heightDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logDebug("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            messageId = savedInstanceState.getLong("messageId", -1);
            logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
            handle = savedInstanceState.getLong("handle", -1);
        }
        else{
            logWarning("Bundle NULL");

            if(context instanceof ChatActivityLollipop){
                chatId = ((ChatActivityLollipop) context).idChat;
                messageId = ((ChatActivityLollipop) context).selectedMessageId;
            }
            else if(context instanceof NodeAttachmentHistoryActivity){
                chatId = ((NodeAttachmentHistoryActivity) context).chatId;
                messageId = ((NodeAttachmentHistoryActivity) context).selectedMessageId;
            }

            logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
        }

        messageMega = getMegaChatMessage(context, megaChatApi, chatId, messageId);
        if(messageMega!=null){
            message = new AndroidMegaChatMessage(messageMega);
        }

        chatC = new ChatController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);
        logDebug("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_attachment_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.node_attachment_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        nodeThumb = (ImageView) contentView.findViewById(R.id.node_attachment_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.node_attachment_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.node_attachment_info_text);
        nodeIconLayout = (RelativeLayout) contentView.findViewById(R.id.node_attachment_relative_layout_icon);
        nodeIcon = (ImageView) contentView.findViewById(R.id.node_attachment_icon);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionView = (LinearLayout) contentView.findViewById(R.id.option_view_layout);
        optionViewText = (TextView) contentView.findViewById(R.id.option_view_text);
        optionSaveOffline = (LinearLayout) contentView.findViewById(R.id.option_save_offline_layout);
        optionRemove = (LinearLayout) contentView.findViewById(R.id.option_remove_layout);
        optionForwardLayout = (LinearLayout) contentView.findViewById(R.id.option_forward_layout);

        LinearLayout separatorInfo = (LinearLayout) contentView.findViewById(R.id.separator_info);
        LinearLayout separatorRemove = (LinearLayout) contentView.findViewById(R.id.separator_remove);

        if (message == null || message.getMessage() == null) {
            return;
        }

        if(message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle() && messageMega.isDeletable()){
            logDebug("Message DELETABLE");
            optionRemove.setVisibility(View.VISIBLE);
        }
        else{
            logDebug("Message NOT DELETABLE");
            optionRemove.setVisibility(View.GONE);
        }

        optionImport = (LinearLayout) contentView.findViewById(R.id.option_import_layout);

        optionDownload.setOnClickListener(this);
        optionView.setOnClickListener(this);
        optionSaveOffline.setOnClickListener(this);
        optionRemove.setOnClickListener(this);
        optionImport.setOnClickListener(this);
        optionForwardLayout.setOnClickListener(this);

        if (chatC.isInAnonymousMode()) {
            optionSaveOffline.setVisibility(View.GONE);
            optionImport.setVisibility(View.GONE);
            optionForwardLayout.setVisibility(View.GONE);
        }

        nodeIconLayout.setVisibility(View.GONE);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            logDebug("Landscape configuration");
            nodeName.setMaxWidth(scaleWidthPx(275, outMetrics));
            nodeInfo.setMaxWidth(scaleWidthPx(275, outMetrics));
        }
        else{
            nodeName.setMaxWidth(scaleWidthPx(210, outMetrics));
            nodeInfo.setMaxWidth(scaleWidthPx(210, outMetrics));
        }

        nodeList = message.getMessage().getMegaNodeList();

        if(nodeList == null || nodeList.size() == 0){
            logWarning("Error: nodeList is NULL or empty");
            return;
        }

        if(handle == -1){
            node = nodeList.get(0);
        }
        else{
            node = getNodeByHandle(handle);
        }

        if (node == null) {
            logWarning("Error: node is NULL");
            return;
        }

        if(handle == -1){
            logDebug("Panel shown from ChatActivity");
            if(nodeList.size()==1){
                logDebug("One file included");
                showSingleNodeSelected();
            }
            else{
                logDebug("Several nodes in the message");
                optionView.setVisibility(View.VISIBLE);

                long totalSize = 0;
                int count = 0;
                for(int i=0; i<nodeList.size(); i++){
                    MegaNode temp = nodeList.get(i);
                    if(!(megaChatApi.isRevoked(chatId, temp.getHandle()))){
                        count++;
                        logDebug("Node Name: " + temp.getName());
                        totalSize = totalSize + temp.getSize();
                    }
                }
                nodeInfo.setText(getSizeString(totalSize));
                MegaNode node = nodeList.get(0);
                nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                if(count==1){
                    nodeName.setText(node.getName());
                }
                else{
                    nodeName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, count, count));
                }

                if(nodeList.size()==count){
                    optionViewText.setText(getString(R.string.general_view));
                }
                else{
                    optionViewText.setText(getString(R.string.general_view_with_revoke, nodeList.size()-count));
                }
            }
        }
        else{
            logDebug("Panel shown from NodeAttachmenntActivity - always one file selected");
            showSingleNodeSelected();
        }

        separatorInfo.setVisibility(optionView.getVisibility());

        if ((optionDownload.getVisibility() == View.GONE && optionImport.getVisibility() == View.GONE && optionForwardLayout.getVisibility() == View.GONE && optionSaveOffline.getVisibility() == View.GONE)
                || optionRemove.getVisibility() == View.GONE) {
            separatorRemove.setVisibility(View.GONE);
        }
        else {
            separatorRemove.setVisibility(View.VISIBLE);
        }

        dialog.setContentView(contentView);

        mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void showSingleNodeSelected() {
        if (node.hasThumbnail()) {
            logDebug("Node has thumbnail");
            RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
            params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
            params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
            params1.setMargins(20, 0, 12, 0);
            nodeThumb.setLayoutParams(params1);

            thumb = getThumbnailFromCache(node);
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
        }
        else {
            logDebug("Node has not thumbnail");
            nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
        }

        nodeName.setText(node.getName());

        long nodeSize = node.getSize();
        nodeInfo.setText(getSizeString(nodeSize));

        optionView.setVisibility(View.GONE);
    }

    public MegaNode getNodeByHandle(long handle){
        for(int i=0;i<nodeList.size();i++){
            MegaNode node = nodeList.get(i);
            if(node.getHandle()==handle){
                return node;
            }
        }
        return null;
    }

    @Override
    public void onClick(View v) {

        if (!isOnline(context)) {
            if(context instanceof ChatActivityLollipop){
                ((ChatActivityLollipop)context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
            }
        }
        else{
            switch(v.getId()){

                case R.id.option_forward_layout: {
                    chatC.prepareMessageToForward(messageId, chatId);
                    break;
                }
                case R.id.option_download_layout:{
                    logDebug("Download option");
                    if(node==null){
                        logWarning("The selected node is NULL");
                        return;
                    }

                    chatC.prepareForChatDownload(nodeList);

                    break;
                }
                case R.id.option_import_layout:{
                    logDebug("Import option");
                    if(node==null){
                        logWarning("The selected node is NULL");
                        return;
                    }

                    chatC.importNode(messageId, chatId);

                    break;
                }
                case R.id.option_save_offline_layout:{
                    logDebug("Save for offline option");
                    if(node==null){
                        logWarning("The selected node is NULL");
                        return;
                    }

                    if(message!=null){
                        ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
                        messages.add(message);
                        chatC.saveForOfflineWithAndroidMessages(messages, megaChatApi.getChatRoom(chatId));
                    }
                    else{
                        logWarning("Message is NULL");
                    }

                    break;
                }
                case R.id.option_remove_layout:{
                    logDebug("Remove option ");
                    if(node==null){
                        logWarning("The selected node is NULL");
                        return;
                    }

                    if(message!=null){
                        chatC.deleteMessage(message.getMessage(), chatId);
                    }
                    else{
                        logWarning("Message is NULL");
                    }

                    break;
                }
            }
        }

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onAttach(Activity activity) {
        logDebug("onAttach");
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
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putLong("chatId", chatId);
        outState.putLong("messageId", messageId);
        outState.putLong("handle", handle);
    }
}
