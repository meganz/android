package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class ChatBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;
    private MegaChatListItem chat = null;
    private long chatId;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    private LinearLayout mainLinearLayout;
    private EmojiTextView titleNameContactChatPanel;
    private ImageView iconStateChatPanel;
    private TextView titleMailContactChatPanel;
    private RoundedImageView chatImageView;
    private TextView infoChatText;
    private LinearLayout optionInfoChat;
    private LinearLayout optionLeaveChat;
    private TextView optionLeaveText;
    private LinearLayout optionClearHistory;
    private LinearLayout optionMuteChat;
    private ImageView optionMuteChatIcon;
    private TextView optionMuteChatText;
    private LinearLayout optionArchiveChat;
    private TextView archiveChatText;
    private ImageView archiveChatIcon;

    private boolean notificationsEnabled;
    private ChatItemPreferences chatPrefs;

    private DisplayMetrics outMetrics;

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH;

    private int heightDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            logDebug("Handle of the chat: "+chatId);
        }
        else{
            logWarning("Bundle NULL");
            if(context instanceof ManagerActivityLollipop){
                chatId = ((ManagerActivityLollipop) context).selectedChatItemId;
            }
            else if(context instanceof ArchivedChatsActivity){
                chatId = ((ArchivedChatsActivity) context).selectedChatItemId;
            }
        }

        if(chatId!=-1){
            chat = megaChatApi.getChatListItem(chatId);
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }
    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.chat_item_bottom_sheet, null);

        mainLinearLayout = contentView.findViewById(R.id.chat_item_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        iconStateChatPanel = contentView.findViewById(R.id.chat_list_contact_state);

        iconStateChatPanel.setMaxWidth(scaleWidthPx(6,outMetrics));
        iconStateChatPanel.setMaxHeight(scaleHeightPx(6,outMetrics));

        titleNameContactChatPanel = contentView.findViewById(R.id.chat_list_chat_name_text);
        titleMailContactChatPanel = contentView.findViewById(R.id.chat_list_chat_mail_text);
        chatImageView = contentView.findViewById(R.id.sliding_chat_list_thumbnail);
        infoChatText = contentView.findViewById(R.id.chat_list_info_chat_text);
        optionInfoChat = contentView.findViewById(R.id.chat_list_info_chat_layout);
        optionLeaveChat = contentView.findViewById(R.id.chat_list_leave_chat_layout);
        optionLeaveText = contentView.findViewById(R.id.chat_list_leave_chat_text);
        optionClearHistory = contentView.findViewById(R.id.chat_list_clear_history_chat_layout);
        optionMuteChat = contentView.findViewById(R.id.chat_list_mute_chat_layout);
        optionMuteChatIcon = contentView.findViewById(R.id.chat_list_mute_chat_image);
        optionMuteChatText = contentView.findViewById(R.id.chat_list_mute_chat_text);
        optionArchiveChat = contentView.findViewById(R.id.chat_list_archive_chat_layout);
        archiveChatText = contentView.findViewById(R.id.chat_list_archive_chat_text);
        archiveChatIcon = contentView.findViewById(R.id.file_archive_chat_image);

        if(isScreenInPortrait(context)){
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        }else{
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        optionInfoChat.setOnClickListener(this);
        optionMuteChat.setOnClickListener(this);
        optionLeaveChat.setOnClickListener(this);
        optionClearHistory.setOnClickListener(this);
        optionArchiveChat.setOnClickListener(this);

        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);

        titleNameContactChatPanel.setText(chat.getTitle());

        if(chat.isPreview()){
            titleMailContactChatPanel.setText(getString(R.string.group_chat_label));
            iconStateChatPanel.setVisibility(View.GONE);
            addAvatarChatPanel(null, chat);

            infoChatText.setText(getString(R.string.group_chat_info_label));

            if(megaApi!=null && megaApi.getRootNode()!=null){
                optionInfoChat.setVisibility(View.VISIBLE);
                separatorInfo.setVisibility(View.VISIBLE);
            }
            else{
                optionInfoChat.setVisibility(View.GONE);
                separatorInfo.setVisibility(View.GONE);
            }

            optionMuteChat.setVisibility(View.GONE);
            optionLeaveChat.setVisibility(View.VISIBLE);
            optionLeaveText.setText("Remove preview");
            optionClearHistory.setVisibility(View.GONE);
            optionArchiveChat.setVisibility(View.GONE);
        }
        else {
            if(chat.isGroup()){
                titleMailContactChatPanel.setText(getString(R.string.group_chat_label));
                iconStateChatPanel.setVisibility(View.GONE);
                addAvatarChatPanel(null, chat);

                separatorInfo.setVisibility(View.GONE);
                optionInfoChat.setVisibility(View.GONE);

                if(chat.isActive()){
                    optionLeaveChat.setVisibility(View.VISIBLE);
                }
                else{
                    optionLeaveChat.setVisibility(View.GONE);
                }

                if((chat.getLastMessageType()== MegaChatMessage.TYPE_INVALID) || (chat.getLastMessageType()== MegaChatMessage.TYPE_TRUNCATE)){
                    optionClearHistory.setVisibility(View.GONE);
                }
                else{
                    if(chat.isActive() && chat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
                        optionClearHistory.setVisibility(View.VISIBLE);
                    }
                    else{
                        optionClearHistory.setVisibility(View.GONE);
                    }
                }
            }
            else{
                iconStateChatPanel.setVisibility(View.VISIBLE);

                long userHandle = chat.getPeerHandle();
                MegaUser contact = megaApi.getContact(MegaApiJava.userHandleToBase64(userHandle));

                if((chat.getLastMessageType()== MegaChatMessage.TYPE_INVALID) || (chat.getLastMessageType()== MegaChatMessage.TYPE_TRUNCATE)){
                    optionClearHistory.setVisibility(View.GONE);
                }
                else{
                    if(chat.isActive()){
                        optionClearHistory.setVisibility(View.VISIBLE);
                    }
                    else{
                        optionClearHistory.setVisibility(View.GONE);
                    }
                }

                if(contact!=null){
                    logDebug("User email: " + contact.getEmail());
                    titleMailContactChatPanel.setText(contact.getEmail());
                    addAvatarChatPanel(contact.getEmail(), chat);

                    if(contact.getVisibility()==MegaUser.VISIBILITY_VISIBLE){
                        optionInfoChat.setVisibility(View.VISIBLE);
                        infoChatText.setText(getString(R.string.contact_properties_activity));
                    }
                    else{
                        optionInfoChat.setVisibility(View.GONE);
                        optionClearHistory.setVisibility(View.GONE);
                    }
                }
                else{
                    optionInfoChat.setVisibility(View.GONE);
                    optionClearHistory.setVisibility(View.GONE);
                }

                optionLeaveChat.setVisibility(View.GONE);
                int state = megaChatApi.getUserOnlineStatus(userHandle);

                if(state == MegaChatApi.STATUS_ONLINE){
                    logDebug("This user is connected");
                    iconStateChatPanel.setVisibility(View.VISIBLE);
                    iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
                }
                else if(state == MegaChatApi.STATUS_AWAY){
                    logDebug("This user is away");
                    iconStateChatPanel.setVisibility(View.VISIBLE);
                    iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
                }
                else if(state == MegaChatApi.STATUS_BUSY){
                    logDebug("This user is busy");
                    iconStateChatPanel.setVisibility(View.VISIBLE);
                    iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
                }
                else if(state == MegaChatApi.STATUS_OFFLINE){
                    logDebug("This user is offline");
                    iconStateChatPanel.setVisibility(View.VISIBLE);
                    iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
                }
                else if(state == MegaChatApi.STATUS_INVALID){
                    logWarning("INVALID status: " + state);
                    iconStateChatPanel.setVisibility(View.GONE);
                }
                else{
                    logDebug("This user status is: " + state);
                    iconStateChatPanel.setVisibility(View.GONE);
                }
            }

            chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chat.getChatId()));
            if(chatPrefs!=null) {
                logDebug("Chat prefs exists!!!");
                notificationsEnabled = true;
                if (chatPrefs.getNotificationsEnabled() != null) {
                    notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
                }

                if (!notificationsEnabled) {
                    logDebug("Chat is MUTE");
                    optionMuteChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_unmute));
                    optionMuteChatText.setText(getString(R.string.general_unmute));
                }
            }
            else{
                MegaChatRoom chatRoom = megaChatApi.getChatRoomByUser(chat.getPeerHandle());
                if(chatRoom!=null){
                    titleMailContactChatPanel.setText(chatRoom.getPeerEmail(0));
                    addAvatarChatPanel(chatRoom.getPeerEmail(0), chat);
                }
            }

            if(chat.isArchived()){
                archiveChatText.setText(getString(R.string.unarchive_chat_option));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_unarchive));
                optionInfoChat.setVisibility(View.GONE);
                optionMuteChat.setVisibility(View.GONE);
                optionLeaveChat.setVisibility(View.GONE);
                optionClearHistory.setVisibility(View.GONE);
            }
            else{
                archiveChatText.setText(getString(R.string.archive_chat_option));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_archive));
            }
        }

        dialog.setContentView(contentView);

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
//        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//
//        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mBehavior.setPeekHeight((heightDisplay / 4) * 2);
//        }
//        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//            mBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
//        }

        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void addAvatarChatPanel(String contactMail, MegaChatListItem chat){
        int color;
        String name = null;
        if (chat.getTitle() != null && chat.getTitle().trim().length() > 0) {
            name = chat.getTitle();
        } else if (contactMail != null && contactMail.length() > 0) {
            name = contactMail;
        }

        if(chat.isGroup()){
            color = getSpecificColor(AVATAR_GROUP_CHAT_COLOR);
        }else{
            color = getColorAvatar(megaApi.getContact(contactMail));
        }

        chatImageView.setImageBitmap(getDefaultAvatar(color, name, AVATAR_SIZE, false));

        Bitmap bitmap = getImageAvatar(contactMail);
        if(bitmap != null){
            chatImageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.chat_list_info_chat_layout:{
                logDebug("Contact info");
                if(chat==null){
                    logWarning("Selected chat NULL");
                    return;
                }

                if(chat.isGroup()){
                    Intent i = new Intent(context, GroupChatInfoActivityLollipop.class);
//                i.putExtra("userEmail", selectedChatItem.getContacts().get(0).getMail());
//                i.putExtra("userFullName", ((ManagerActivityLollipop) context).getFullNameChat());
                    i.putExtra("handle", chat.getChatId());
                    context.startActivity(i);
                }
                else{
                    Intent i = new Intent(context, ContactInfoActivityLollipop.class);
//                i.putExtra("userEmail", selectedChatItem.getContacts().get(0).getMail());
//                i.putExtra("userFullName", ((ManagerActivityLollipop) context).getFullNameChat());
                    i.putExtra("handle", chat.getChatId());
                    context.startActivity(i);
                }

                dismissAllowingStateLoss();
                break;
            }
            case R.id.chat_list_leave_chat_layout:{
                logDebug("Click leave chat");
                if(chat==null){
                    logWarning("Selected chat NULL");
                    return;
                }
                logDebug("Leave chat - Chat ID: " + chat.getChatId());
                ((ManagerActivityLollipop)context).showConfirmationLeaveChat(chat);
                break;
            }
            case R.id.chat_list_clear_history_chat_layout:{
                logDebug("Click clear history chat");
                if(chat==null){
                    logWarning("Selected chat NULL");
                    return;
                }
                logDebug("Clear chat - Chat ID: " + chat.getChatId());
                ((ManagerActivityLollipop)context).showConfirmationClearChat(chat);

                break;
            }
            case R.id.chat_list_mute_chat_layout:{
                logDebug("Click mute chat");
                if(chatPrefs==null) {

                    if(notificationsEnabled){
                        chatPrefs = new ChatItemPreferences(Long.toString(chat.getChatId()), Boolean.toString(true), "");
                    }
                    else{
                        chatPrefs = new ChatItemPreferences(Long.toString(chat.getChatId()), Boolean.toString(false), "");
                    }

                    dbH.setChatItemPreferences(chatPrefs);
                }
                else{
                    ChatController chatC = new ChatController(context);
                    if(notificationsEnabled){
                        chatC.muteChat(chat);
                    }
                    else{
                        chatC.unmuteChat(chat);
                    }
                }

                ((ManagerActivityLollipop)context).showMuteIcon(chat);
                break;
            }
            case R.id.chat_list_archive_chat_layout:{
                if(chat==null){
                    logDebug("Selected chat NULL");
                    return;
                }

                ChatController chatC = new ChatController(context);
                chatC.archiveChat(chat);
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
    }
}
