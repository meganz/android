package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.PendingMessageSingle;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.utils.LogUtil.*;

public class PendingMessageBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView titleSlidingPanel;

    public LinearLayout optionRetryLayout;
    public LinearLayout optionDeleteLayout;
    ////

    DatabaseHandler dbH;

    DisplayMetrics outMetrics;

    MegaChatApiAndroid megaChatApi;
    MegaChatRoom selectedChat = null;

    AndroidMegaChatMessage selectedMessage = null;
    MegaChatMessage originalMsg = null;

    long chatId;
    long messageId;

    private int heightDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            messageId = savedInstanceState.getLong("messageId", -1);
            logDebug("Chat ID: " + chatId + "Message ID: " + messageId);
//            MegaChatMessage messageMega = megaChatApi.getManualSendingMessage(chatId, messageId);
//            if(messageMega!=null){
//                selectedMessage = new AndroidMegaChatMessage(messageMega);
//            }
            selectedChat = megaChatApi.getChatRoom(chatId);
        }
        else{
            logWarning("Bundle NULL");

            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;
            logDebug("Chat ID: " + chatId + "Message ID: " + messageId);

//            MegaChatMessage messageMega = megaChatApi.getManualSendingMessage(chatId, messageId);
//            log("Row of the MS message: "+messageId);
//            if(messageMega!=null){
//                selectedMessage = new AndroidMegaChatMessage(messageMega);
//            }
            selectedChat = megaChatApi.getChatRoom(chatId);
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

//        if(selectedMessage!=null){
//            log("selectedMessage content: "+selectedMessage.getMessage().getContent());
//            log("Temporal id of MS message: "+selectedMessage.getMessage().getTempId());
//        }
//        else{
//            log("Error the selectedMessage is NULL");
//            return;
//        }
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.msg_not_sent_bottom_sheet, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.msg_not_sent_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleSlidingPanel = (TextView)  contentView.findViewById(R.id.msg_not_sent_title_text);
        optionRetryLayout = (LinearLayout) contentView.findViewById(R.id.msg_not_sent_retry_layout);
        optionDeleteLayout = (LinearLayout) contentView.findViewById(R.id.msg_not_sent_delete_layout);
        optionDeleteLayout.setOnClickListener(this);

        LinearLayout separator = (LinearLayout) contentView.findViewById(R.id.separator);

        PendingMessageSingle pMsg = dbH.findPendingMessageById(messageId);
        if(pMsg!=null && pMsg.getState()==PendingMessageSingle.STATE_UPLOADING) {
            optionRetryLayout.setVisibility(View.GONE);
            optionRetryLayout.setOnClickListener(null);
            titleSlidingPanel.setText(getString(R.string.title_message_uploading_options));
            separator.setVisibility(View.GONE);
        }
        else{
            titleSlidingPanel.setText(getString(R.string.title_message_not_sent_options));
            if((selectedChat.getOwnPrivilege()==MegaChatRoom.PRIV_STANDARD)||(selectedChat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR)){
                optionRetryLayout.setVisibility(View.VISIBLE);
                optionRetryLayout.setOnClickListener(this);
                separator.setVisibility(View.VISIBLE);
            }
            else{
                optionRetryLayout.setVisibility(View.GONE);
                optionRetryLayout.setOnClickListener(null);
                separator.setVisibility(View.GONE);
            }
        }
//        if(selectedMessage!=null&&selectedChat!=null){
//            if(selectedMessage.getMessage().isEdited()){
//                log("Message edited : final id: "+selectedMessage.getMessage().getMsgId()+" temp id: "+selectedMessage.getMessage().getTempId());
//                originalMsg = megaChatApi.getMessage(selectedChat.getChatId(), selectedMessage.getMessage().getTempId());
//                if(originalMsg!=null){
//                    if(originalMsg.isEditable()){
//                        optionRetryLayout.setVisibility(View.VISIBLE);
//                        optionRetryLayout.setOnClickListener(this);
//                    }
//                    else{
//                        optionRetryLayout.setVisibility(View.GONE);
//                    }
//                }
//                else{
//                    log("Null recovering the original msg");
//                    optionRetryLayout.setVisibility(View.GONE);
//                }
//            }
//            else{
//                optionRetryLayout.setVisibility(View.VISIBLE);
//                optionRetryLayout.setOnClickListener(this);
//            }
//        }

        dialog.setContentView(contentView);

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch(v.getId()){

            case R.id.msg_not_sent_retry_layout: {
                logDebug("Retry option click");
//                if(selectedMessage!=null&&selectedChat!=null){
//                    log("selectedMessage content: "+selectedMessage.getMessage().getContent());
//
//                    ((ChatActivityLollipop) context).removeMsgNotSent();
//                    megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());
//
//                    if(selectedMessage.getMessage().isEdited()){
//                        log("Message is edited --> edit");
//                        if(originalMsg!=null){
//                            ((ChatActivityLollipop) context).editMessageMS(selectedMessage.getMessage().getContent(), originalMsg);
//                        }
//                    }
//                    else{
//                        log("Message NOT edited --> send");
//                        ((ChatActivityLollipop) context).sendMessage(selectedMessage.getMessage().getContent());
//                    }
//                }
//                else{
//                    log("onClick: Chat or message are NULL");
//                }

                ((ChatActivityLollipop) context).retryPendingMessage(messageId);
                break;
            }

            case R.id.msg_not_sent_delete_layout: {
                logDebug("Delete option click");
//                if(selectedMessage!=null&&selectedChat!=null){
//                    ((ChatActivityLollipop) context).removeMsgNotSent();
//                    megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());
//                }
//                else{
//                    log("onClick: Chat or message are NULL");
//                }

                ((ChatActivityLollipop) context).removePendingMsg(messageId);

                break;
            }
        }
//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
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
    }
}
