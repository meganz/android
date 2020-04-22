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
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class GeneralChatMessageBottomSheet extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;
    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private int positionMessage;

    private View contentView;
    private BottomSheetBehavior mBehavior;
    private RelativeLayout mainLayout;
    private LinearLayout itemsLayout;
    private LinearLayout optionForward;
    private LinearLayout optionEdit;
    private LinearLayout optionCopy;
    private LinearLayout optionDelete;
    private LinearLayout optionSelect;

    private DisplayMetrics outMetrics;
    private int heightDisplay;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private ChatController chatC;
    private MegaChatRoom chatRoom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

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

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        heightDisplay = outMetrics.heightPixels;

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_general_chat_messages, null);
        mainLayout = contentView.findViewById(R.id.bottom_sheet);
        itemsLayout = contentView.findViewById(R.id.items_layout);
        optionForward = contentView.findViewById(R.id.forward_layout);
        optionEdit = contentView.findViewById(R.id.edit_layout);
        optionCopy = contentView.findViewById(R.id.copy_layout);
        optionDelete = contentView.findViewById(R.id.delete_layout);
        optionSelect = contentView.findViewById(R.id.option_select_layout);

        optionForward.setOnClickListener(this);
        optionEdit.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionDelete.setOnClickListener(this);
        optionSelect.setOnClickListener(this);

        if (message == null || chatRoom == null || ((ChatActivityLollipop) context).hasMessagesRemoved(message.getMessage()) || message.isUploading()) {
            optionForward.setVisibility(View.GONE);
            optionEdit.setVisibility(View.GONE);
            optionCopy.setVisibility(View.GONE);
            optionDelete.setVisibility(View.GONE);
            optionSelect.setVisibility(View.GONE);

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

                if (message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle()) {
                    optionDelete.setVisibility(View.GONE);
                } else {
                    optionDelete.setVisibility(View.VISIBLE);
                }
            }
        }

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(itemsLayout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
                dismissAllowingStateLoss();
                break;

            case R.id.option_select_layout:
                ((ChatActivityLollipop) context).activateActionModeWithItem(positionMessage);
                dismissAllowingStateLoss();
                break;

            case R.id.edit_layout:
                ((ChatActivityLollipop) context).editMessage(messagesSelected);
                dismissAllowingStateLoss();
                break;

            case R.id.copy_layout:
                ((ChatActivityLollipop) context).copyMessage(message);
                dismissAllowingStateLoss();
                break;

            case R.id.delete_layout:
                ((ChatActivityLollipop) context).showConfirmationDeleteMessages(messagesSelected, chatRoom);
                dismissAllowingStateLoss();
                break;
        }

        mBehavior = BottomSheetBehavior.from((View) mainLayout.getParent());
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
