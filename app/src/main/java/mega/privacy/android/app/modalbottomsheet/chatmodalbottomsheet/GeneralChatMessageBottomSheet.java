package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class GeneralChatMessageBottomSheet extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private int positionMessage;

    private ChatController chatC;
    private MegaChatRoom chatRoom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(context instanceof ChatActivityLollipop))
            return;

        if (savedInstanceState != null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_ID);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_ID);
            positionMessage = savedInstanceState.getInt(POSITION_SELECTED_MESSAGE, INVALID_POSITION);
        } else {
            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;
            positionMessage = ((ChatActivityLollipop) context).selectedPosition;
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        chatRoom = megaChatApi.getChatRoom(chatId);
        chatC = new ChatController(context);
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);


        contentView = View.inflate(getContext(), R.layout.bottom_sheet_general_chat_messages, null);
        items_layout = contentView.findViewById(R.id.items_layout);

        LinearLayout optionForward = contentView.findViewById(R.id.forward_layout);
        LinearLayout editSeparator = contentView.findViewById(R.id.edit_separator);
        LinearLayout optionEdit = contentView.findViewById(R.id.edit_layout);
        LinearLayout copySeparator = contentView.findViewById(R.id.copy_separator);
        LinearLayout optionCopy = contentView.findViewById(R.id.copy_layout);
        LinearLayout selectSeparator = contentView.findViewById(R.id.select_separator);
        LinearLayout optionSelect = contentView.findViewById(R.id.select_layout);
        LinearLayout deleteSeparator = contentView.findViewById(R.id.delete_separator);
        LinearLayout optionDelete = contentView.findViewById(R.id.delete_layout);

        optionForward.setOnClickListener(this);
        optionEdit.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionDelete.setOnClickListener(this);
        optionSelect.setOnClickListener(this);

        if (message == null || chatRoom == null || ((ChatActivityLollipop) context).hasMessagesRemoved(message.getMessage()) || message.isUploading()) {
            optionForward.setVisibility(View.GONE);
            editSeparator.setVisibility(View.GONE);
            optionEdit.setVisibility(View.GONE);
            copySeparator.setVisibility(View.GONE);
            optionCopy.setVisibility(View.GONE);
            selectSeparator.setVisibility(View.GONE);
            optionSelect.setVisibility(View.GONE);
            deleteSeparator.setVisibility(View.GONE);
            optionDelete.setVisibility(View.GONE);

        } else {
            int typeMessage = message.getMessage().getType();

            optionSelect.setVisibility(View.VISIBLE);

            if (typeMessage == MegaChatMessage.TYPE_NORMAL ||
                    (typeMessage == MegaChatMessage.TYPE_CONTAINS_META &&
                            message.getMessage().getContainsMeta() != null &&
                            message.getMessage().getContainsMeta().getType() != MegaChatContainsMeta.CONTAINS_META_INVALID &&
                            message.getMessage().getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW)) {
                optionCopy.setVisibility(View.VISIBLE);
            } else {
                optionCopy.setVisibility(View.GONE);
            }
            if (((chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO) && !chatRoom.isPreview())) {
                optionForward.setVisibility(View.GONE);
                optionEdit.setVisibility(View.GONE);
                optionDelete.setVisibility(View.GONE);
            } else {
                if (!isOnline(context) || chatC.isInAnonymousMode()) {
                    optionForward.setVisibility(View.GONE);
                } else {
                    optionForward.setVisibility(View.VISIBLE);
                }
                if (message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle() || !message.getMessage().isEditable()) {
                    optionEdit.setVisibility(View.GONE);
                } else {
                    if (typeMessage == MegaChatMessage.TYPE_NORMAL || typeMessage == MegaChatMessage.TYPE_CONTAINS_META) {
                        optionEdit.setVisibility(View.VISIBLE);
                    } else {
                        optionEdit.setVisibility(View.GONE);
                    }
                }

                if (message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle() || !message.getMessage().isDeletable()) {
                    optionDelete.setVisibility(View.GONE);
                } else {
                    optionDelete.setVisibility(View.VISIBLE);
                }
            }

            deleteSeparator.setVisibility(optionDelete.getVisibility());
            selectSeparator.setVisibility(optionSelect.getVisibility());
            editSeparator.setVisibility(optionEdit.getVisibility());
            if(optionEdit.getVisibility() == View.VISIBLE){
                copySeparator.setVisibility(View.GONE);
            }else{
                copySeparator.setVisibility(optionCopy.getVisibility());
            }
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    @Override
    public void onClick(View view) {
        if (message == null) {
            logWarning("The message is NULL");
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        switch (view.getId()) {
            case R.id.forward_layout:
                ((ChatActivityLollipop) context).forwardMessages(messagesSelected);
                break;

            case R.id.select_layout:
                ((ChatActivityLollipop) context).activateActionModeWithItem(positionMessage);
                break;

            case R.id.edit_layout:
                ((ChatActivityLollipop) context).editMessage(messagesSelected);
                break;

            case R.id.copy_layout:
                ((ChatActivityLollipop) context).copyMessage(message);
                break;

            case R.id.delete_layout:
                ((ChatActivityLollipop) context).showConfirmationDeleteMessages(messagesSelected, chatRoom);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putLong(POSITION_SELECTED_MESSAGE, positionMessage);
    }
}
