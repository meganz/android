package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatReactionsFragment;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;


public class NodeAttachmentBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node;
    private MegaNodeList nodeList;
    private MegaChatMessage messageMega;
    private AndroidMegaChatMessage message;
    private long chatId;
    private long messageId;
    private long handle = INVALID_HANDLE;
    private ChatController chatC;
    private int positionMessage;
    private MegaChatRoom chatRoom;
    private ImageView nodeThumb;
    private TextView nodeName;
    private TextView nodeInfo;
    private LinearLayout optionView;

    private RelativeLayout titleLayout;
    private LinearLayout titleSeparator;
    private LinearLayout reactionsLayout;
    private ChatReactionsFragment reactionsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_HANDLE);
            handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            if((context instanceof ChatActivityLollipop)){
                positionMessage = savedInstanceState.getInt(POSITION_SELECTED_MESSAGE, INVALID_POSITION);
            }

        } else if (context instanceof ChatActivityLollipop) {
            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;
            positionMessage = ((ChatActivityLollipop) context).selectedPosition;

        } else if (context instanceof NodeAttachmentHistoryActivity) {
            chatId = ((NodeAttachmentHistoryActivity) context).chatId;
            messageId = ((NodeAttachmentHistoryActivity) context).selectedMessageId;
        }

        logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
        messageMega = getMegaChatMessage(context, megaChatApi, chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        chatRoom = megaChatApi.getChatRoom(chatId);
        chatC = new ChatController(context);
        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);


        if (message == null || message.getMessage() == null) {
            logWarning("Message is null");
            return;
        }

        nodeList = message.getMessage().getMegaNodeList();

        if (nodeList == null || nodeList.size() == 0) {
            logWarning("Error: nodeList is NULL or empty");
            return;
        }

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_attachment_item, null);
        mainLinearLayout = contentView.findViewById(R.id.node_attachment_bottom_sheet);
        reactionsLayout = contentView.findViewById(R.id.reactions_layout);
        reactionsFragment = contentView.findViewById(R.id.fragment_container_reactions);
        items_layout = contentView.findViewById(R.id.items_layout);

        nodeThumb = contentView.findViewById(R.id.node_attachment_thumbnail);
        nodeName = contentView.findViewById(R.id.node_attachment_name_text);
        nodeInfo = contentView.findViewById(R.id.node_attachment_info_text);
        RelativeLayout nodeIconLayout = contentView.findViewById(R.id.node_attachment_relative_layout_icon);
        LinearLayout optionOpenWith = contentView.findViewById(R.id.open_with_layout);
        LinearLayout forwardSeparator = contentView.findViewById(R.id.forward_separator);
        LinearLayout optionForward = contentView.findViewById(R.id.forward_layout);
        LinearLayout selectSeparator = contentView.findViewById(R.id.select_separator);
        LinearLayout optionSelect = contentView.findViewById(R.id.select_layout);
        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);
        optionView = contentView.findViewById(R.id.option_view_layout);
        TextView optionViewText = contentView.findViewById(R.id.option_view_text);
        LinearLayout optionDownload = contentView.findViewById(R.id.option_download_layout);
        LinearLayout optionImport = contentView.findViewById(R.id.option_import_layout);
        LinearLayout optionSaveOffline = contentView.findViewById(R.id.option_save_offline_layout);
        LinearLayout separatorRemove = contentView.findViewById(R.id.separator_remove);
        LinearLayout optionRemove = contentView.findViewById(R.id.option_remove_layout);

        optionRemove.setVisibility(message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle() && messageMega.isDeletable() ? View.VISIBLE : View.GONE);

        optionDownload.setOnClickListener(this);
        optionView.setOnClickListener(this);
        optionSaveOffline.setOnClickListener(this);
        optionRemove.setOnClickListener(this);
        optionImport.setOnClickListener(this);
        optionForward.setOnClickListener(this);
        optionOpenWith.setOnClickListener(this);
        optionSelect.setOnClickListener(this);

        if(context instanceof ChatActivityLollipop) {
            reactionsFragment.init(context, chatId, messageId, positionMessage);
        }

        if (chatC.isInAnonymousMode()) {
            optionSaveOffline.setVisibility(View.GONE);
            optionImport.setVisibility(View.GONE);
        }

        nodeIconLayout.setVisibility(View.GONE);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            nodeName.setMaxWidth(scaleWidthPx(275, outMetrics));
            nodeInfo.setMaxWidth(scaleWidthPx(275, outMetrics));
        } else {
            nodeName.setMaxWidth(scaleWidthPx(210, outMetrics));
            nodeInfo.setMaxWidth(scaleWidthPx(210, outMetrics));
        }

        if (handle == INVALID_HANDLE) {
            node = nodeList.get(0);
        } else {
            node = getNodeByHandle(handle);
        }

        if (node == null) {
            logWarning("Node is NULL");
            return;
        }

        if (context instanceof ChatActivityLollipop && chatRoom != null) {
            optionSelect.setVisibility(View.VISIBLE);
            if (chatC.isInAnonymousMode() ||
                    ((chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO) && !chatRoom.isPreview())) {
                optionForward.setVisibility(View.GONE);
                optionRemove.setVisibility(View.GONE);
            } else {
                if (!isOnline(context)) {
                    optionForward.setVisibility(View.GONE);
                } else {
                    optionForward.setVisibility(View.VISIBLE);
                }
                if (message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle() || !message.getMessage().isDeletable()) {
                    optionRemove.setVisibility(View.GONE);
                } else {
                    optionRemove.setVisibility(View.VISIBLE);
                }
            }

            if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isVideo() || MimeTypeList.typeForName(node.getName()).isAudio()
                    || MimeTypeList.typeForName(node.getName()).isImage() || MimeTypeList.typeForName(node.getName()).isPdf()) {
                optionOpenWith.setVisibility(View.VISIBLE);
            } else {
                optionOpenWith.setVisibility(View.GONE);
            }
        } else {
            optionSelect.setVisibility(View.GONE);
            optionForward.setVisibility(View.GONE);
            optionRemove.setVisibility(View.GONE);
            optionOpenWith.setVisibility(View.GONE);
        }

        if (handle == INVALID_HANDLE) {
            if (nodeList.size() == 1) {
                showSingleNodeSelected();
            } else {
                optionView.setVisibility(View.VISIBLE);

                long totalSize = 0;
                int count = 0;
                for (int i = 0; i < nodeList.size(); i++) {
                    MegaNode temp = nodeList.get(i);
                    if (!(megaChatApi.isRevoked(chatId, temp.getHandle()))) {
                        count++;
                        totalSize = totalSize + temp.getSize();
                    }
                }
                nodeInfo.setText(getSizeString(totalSize));
                MegaNode node = nodeList.get(0);
                nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                if (count == 1) {
                    nodeName.setText(node.getName());
                } else {
                    nodeName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, count, count));
                }

                if (nodeList.size() == count) {
                    optionViewText.setText(getString(R.string.general_view));
                } else {
                    optionViewText.setText(getString(R.string.general_view_with_revoke, nodeList.size() - count));
                }
            }
        } else {
            showSingleNodeSelected();
        }

        if(optionView.getVisibility() == View.VISIBLE || optionImport.getVisibility() == View.VISIBLE){
            separatorInfo.setVisibility(View.VISIBLE);
        }else{
            separatorRemove.setVisibility(View.GONE);
        }

        if ((optionDownload.getVisibility() == View.GONE && optionImport.getVisibility() == View.GONE && optionForward.getVisibility() == View.GONE && optionSaveOffline.getVisibility() == View.GONE)
                || optionRemove.getVisibility() == View.GONE) {
            separatorRemove.setVisibility(View.GONE);
        } else {
            separatorRemove.setVisibility(View.VISIBLE);
        }

        if (shouldReactionOptionsBeVisible(context, chatRoom, message)) {
            reactionsLayout.setVisibility(View.VISIBLE);
        } else {
            reactionsLayout.setVisibility(View.GONE);
        }

        if (optionOpenWith.getVisibility() == View.VISIBLE && optionForward.getVisibility() == View.VISIBLE) {
            forwardSeparator.setVisibility(View.VISIBLE);
        } else {
            forwardSeparator.setVisibility(View.GONE);
        }

        if (optionSelect.getVisibility() == View.VISIBLE && (optionForward.getVisibility() == View.VISIBLE || optionOpenWith.getVisibility() == View.VISIBLE)) {
            selectSeparator.setVisibility(View.VISIBLE);
        } else {
            selectSeparator.setVisibility(View.GONE);
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    private void showSingleNodeSelected() {
        if (node.hasThumbnail()) {
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

        nodeName.setText(node.getName());

        long nodeSize = node.getSize();
        nodeInfo.setText(getSizeString(nodeSize));

        optionView.setVisibility(View.GONE);
    }

    public MegaNode getNodeByHandle(long handle) {
        for (int i = 0; i < nodeList.size(); i++) {
            MegaNode node = nodeList.get(i);
            if (node.getHandle() == handle) {
                return node;
            }
        }
        return null;
    }

    @Override
    public void onClick(View v) {

        if (!isOnline(context)) {
            ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), INVALID_HANDLE);
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);

        switch (v.getId()) {
            case R.id.open_with_layout:
                if (node == null) {
                    logWarning("The selected node is NULL");
                    return;
                }
                openWith(node);
                break;

            case R.id.forward_layout:
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).forwardMessages(messagesSelected);
                }
                break;

            case R.id.select_layout:
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).activateActionModeWithItem(positionMessage);
                }
                break;

            case R.id.option_download_layout:
                logDebug("Download option");
                if (node == null) {
                    logWarning("The selected node is NULL");
                    return;
                }
                chatC.prepareForChatDownload(nodeList);
                break;

            case R.id.option_import_layout:
                if (node == null) {
                    logWarning("The selected node is NULL");
                    return;
                }
                chatC.importNode(messageId, chatId);
                break;

            case R.id.option_save_offline_layout:
                if (message != null) {
                    ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
                    messages.add(message);
                    chatC.saveForOfflineWithAndroidMessages(messages, megaChatApi.getChatRoom(chatId));
                } else {
                    logWarning("Message is NULL");
                }
                break;

            case R.id.option_remove_layout:
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).showConfirmationDeleteMessages(messagesSelected, chatRoom);
                }
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putLong(HANDLE, handle);
        if (context instanceof ChatActivityLollipop) {
            outState.putLong(POSITION_SELECTED_MESSAGE, positionMessage);
        }
    }
}
