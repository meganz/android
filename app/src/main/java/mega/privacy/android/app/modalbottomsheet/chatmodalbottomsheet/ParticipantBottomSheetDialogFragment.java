package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


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
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class ParticipantBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaChatRoom selectedChat;
    long chatId = -1;
    long participantHandle = -1;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public EmojiTextView titleNameContactChatPanel;
    public ImageView stateIcon;
    public ImageView permissionsIcon;
    public TextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;
    public LinearLayout contactLayout;
    public LinearLayout optionContactInfoChat;
    public LinearLayout optionStartConversationChat;
    public LinearLayout optionEditProfileChat;
    public LinearLayout optionLeaveChat;
    public LinearLayout optionChangePermissionsChat;
    public LinearLayout optionRemoveParticipantChat;
    public LinearLayout optionInvite;

    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi = null;

    private int heightDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            MegaApplication app = (MegaApplication) ((Activity)context).getApplication();
            megaChatApi = app.getMegaChatApi();
        }

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            logDebug("Handle of the chat: " + chatId);
            participantHandle = savedInstanceState.getLong("participantHandle", -1);
            logDebug("Handle of the participant: " + participantHandle);

            selectedChat = megaChatApi.getChatRoom(chatId);
        }
        else{
            logWarning("Bundle NULL");

            chatId = ((GroupChatInfoActivityLollipop) context).chatHandle;
            selectedChat = megaChatApi.getChatRoom(chatId);
            logDebug("Handle of the chat: " + chatId);

            participantHandle = ((GroupChatInfoActivityLollipop) context).selectedHandleParticipant;
            logDebug("Handle of the participant: " + participantHandle);
        }
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_group_participant, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.participant_item_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        //Sliding CHAT panel
        titleNameContactChatPanel = contentView.findViewById(R.id.group_participants_chat_name_text);
        stateIcon = (ImageView) contentView.findViewById(R.id.group_participants_state_circle);

        stateIcon.setVisibility(View.VISIBLE);

        stateIcon.setMaxWidth(scaleWidthPx(6,outMetrics));
        stateIcon.setMaxHeight(scaleHeightPx(6,outMetrics));

        permissionsIcon = (ImageView) contentView.findViewById(R.id.group_participant_list_permissions);

        titleMailContactChatPanel = (TextView) contentView.findViewById(R.id.group_participants_chat_mail_text);
        contactLayout = (LinearLayout) contentView.findViewById(R.id.group_participants_chat);
        contactImageView = contentView.findViewById(R.id.sliding_group_participants_chat_list_thumbnail);
        optionContactInfoChat = (LinearLayout) contentView.findViewById(R.id.contact_info_group_participants_chat_layout);
        optionStartConversationChat = (LinearLayout) contentView.findViewById(R.id.start_chat_group_participants_chat_layout);
        optionEditProfileChat = (LinearLayout) contentView.findViewById(R.id.edit_profile_group_participants_chat_layout);
        optionLeaveChat = (LinearLayout) contentView.findViewById(R.id.leave_group_participants_chat_layout);
        optionChangePermissionsChat = (LinearLayout) contentView.findViewById(R.id.change_permissions_group_participants_chat_layout);
        optionRemoveParticipantChat= (LinearLayout) contentView.findViewById(R.id.remove_group_participants_chat_layout);
        optionInvite = (LinearLayout) contentView.findViewById(R.id.invite_group_participants_chat_layout);

        optionChangePermissionsChat.setOnClickListener(this);
        optionRemoveParticipantChat.setOnClickListener(this);
        optionContactInfoChat.setOnClickListener(this);
        optionStartConversationChat.setOnClickListener(this);
        optionEditProfileChat.setOnClickListener(this);
        optionLeaveChat.setOnClickListener(this);
        optionInvite.setOnClickListener(this);

        LinearLayout separatorInfo = (LinearLayout) contentView.findViewById(R.id.separator_info);
        LinearLayout separatorOptions = (LinearLayout) contentView.findViewById(R.id.separator_options);
        LinearLayout separatorLeave = (LinearLayout) contentView.findViewById(R.id.separator_leave);

        if(isScreenInPortrait(context)){
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        }else{
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        if(selectedChat==null){
            logWarning("Error. Selected chat is NULL");
            return;
        }

        if(participantHandle==-1){
            logWarning("Error. Participant handle is -1");
            return;
        }

        int state = megaChatApi.getUserOnlineStatus(participantHandle);
        if(state == MegaChatApi.STATUS_ONLINE){
            logDebug("This user is connected");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
        }
        else if(state == MegaChatApi.STATUS_AWAY){
            logDebug("This user is away");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
        }
        else if(state == MegaChatApi.STATUS_BUSY){
            logDebug("This user is busy");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
        }
        else if(state == MegaChatApi.STATUS_OFFLINE){
            logDebug("This user is offline");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
        }
        else if(state == MegaChatApi.STATUS_INVALID){
            logWarning("INVALID status: " + state);
            stateIcon.setVisibility(View.GONE);
        }
        else{
            logDebug("This user status is: " + state);
            stateIcon.setVisibility(View.GONE);
        }

        if(participantHandle == megaApi.getMyUser().getHandle()){
            logDebug("Participant selected its me");
            ChatController chatC = new ChatController(context);
            String myFullName = chatC.getMyFullName();

            if (myFullName == null || myFullName.isEmpty() || myFullName.equals("")) {
                myFullName = megaChatApi.getMyEmail();
            }
            titleNameContactChatPanel.setText(myFullName);

            titleMailContactChatPanel.setText(megaChatApi.getMyEmail());

            int permission = selectedChat.getOwnPrivilege();

            if(permission== MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
            }
            else if(permission==MegaChatRoom.PRIV_MODERATOR){
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
            }
            else{
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
            }

            optionEditProfileChat.setVisibility(View.VISIBLE);
            if(permission<MegaChatRoom.PRIV_RO){
                optionLeaveChat.setVisibility(View.GONE);
            }
            else{
                optionLeaveChat.setVisibility(View.VISIBLE);
            }

            optionContactInfoChat.setVisibility(View.GONE);
            optionStartConversationChat.setVisibility(View.GONE);
            optionChangePermissionsChat.setVisibility(View.GONE);
            optionRemoveParticipantChat.setVisibility(View.GONE);

            optionInvite.setVisibility(View.GONE);

            addAvatarParticipantPanel(participantHandle, megaChatApi.getMyEmail(), myFullName);
        }
        else{

            String fullName = selectedChat.getPeerFullnameByHandle(participantHandle);
            if (fullName == null || fullName.isEmpty() || fullName.equals("")) {
                fullName = selectedChat.getPeerEmailByHandle(participantHandle);
            }

            titleNameContactChatPanel.setText(fullName);
            titleMailContactChatPanel.setText(selectedChat.getPeerEmailByHandle(participantHandle));

            int permission = selectedChat.getPeerPrivilegeByHandle(participantHandle);

            if(permission== MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
            }
            else if(permission==MegaChatRoom.PRIV_MODERATOR){
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
            }
            else{
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
            }

            MegaUser contact = megaApi.getContact(selectedChat.getPeerEmailByHandle(participantHandle));

            if(contact!=null) {
                if (contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    optionContactInfoChat.setVisibility(View.VISIBLE);
                    optionStartConversationChat.setVisibility(View.VISIBLE);
                    optionInvite.setVisibility(View.GONE);
                }
                else{
                    logDebug("Non contact");
                    optionContactInfoChat.setVisibility(View.GONE);
                    optionStartConversationChat.setVisibility(View.GONE);
                    optionInvite.setVisibility(View.VISIBLE);
                }
            }
            else{
                logDebug("Non contact");
                optionContactInfoChat.setVisibility(View.GONE);
                optionStartConversationChat.setVisibility(View.GONE);
                optionInvite.setVisibility(View.VISIBLE);
            }

            optionEditProfileChat.setVisibility(View.GONE);
            optionLeaveChat.setVisibility(View.GONE);

            logDebug("Other participant selected its me");
            if(selectedChat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
                optionChangePermissionsChat.setVisibility(View.VISIBLE);
                optionRemoveParticipantChat.setVisibility(View.VISIBLE);
            }
            else{
                optionChangePermissionsChat.setVisibility(View.GONE);
                optionRemoveParticipantChat.setVisibility(View.GONE);
            }

            addAvatarParticipantPanel(participantHandle, selectedChat.getPeerEmailByHandle(participantHandle), fullName);
        }

        if ((optionContactInfoChat.getVisibility() == View.GONE && optionEditProfileChat.getVisibility() == View.GONE)
                || (optionChangePermissionsChat.getVisibility() == View.GONE && optionStartConversationChat.getVisibility() == View.GONE && optionInvite.getVisibility() == View.GONE
                    && optionLeaveChat.getVisibility() == View.GONE && optionRemoveParticipantChat.getVisibility() == View.GONE)) {
            separatorInfo.setVisibility(View.GONE);
        }
        else {
            separatorInfo.setVisibility(View.VISIBLE);
        }
        if ((optionChangePermissionsChat.getVisibility() == View.GONE && optionStartConversationChat.getVisibility() == View.GONE && optionInvite.getVisibility() == View.GONE)
                || (optionLeaveChat.getVisibility() == View.GONE && optionRemoveParticipantChat.getVisibility() == View.GONE)){
            separatorOptions.setVisibility(View.GONE);
        }
        else {
            separatorOptions.setVisibility(View.VISIBLE);
        }
        if (optionLeaveChat.getVisibility() == View.GONE || optionRemoveParticipantChat.getVisibility() == View.GONE) {
            separatorLeave.setVisibility(View.GONE);
        }
        else {
            separatorLeave.setVisibility(View.VISIBLE);
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

    private void addAvatarParticipantPanel(long handle, String email, String name){

        File avatar = null;
        /*Default avatar*/
        contactImageView.setImageBitmap(getDefaultAvatar(context, getColorAvatar(context, megaApi, handle), name, AVATAR_SIZE, true));

        /*Avatar*/
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(handle);

        if(handle == megaChatApi.getMyUserHandle()){
            //Ask for my avatar
            avatar = buildAvatarFile(getActivity(),email + ".jpg");
        }
        else{

            if(email!=null){
                //Ask for avatar
                avatar = buildAvatarFile(getActivity(),email + ".jpg");
            }

            if(avatar!=null){
                if (!avatar.exists()){
                    avatar = buildAvatarFile(getActivity(),userHandleEncoded + ".jpg");
                }
            }
            else{
                avatar = buildAvatarFile(getActivity(),userHandleEncoded + ".jpg");
            }
        }

        Bitmap bitmap = null;
        if (avatar.exists()){
            if (avatar.length() > 0){
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();
                }
                else{
                    contactImageView.setImageBitmap(bitmap);
                    return;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {

        MegaChatRoom selectedChat = null;
        if(context instanceof GroupChatInfoActivityLollipop){
            selectedChat = ((GroupChatInfoActivityLollipop) context).getChat();
        }

        switch(v.getId()){

            case R.id.contact_info_group_participants_chat_layout: {
                logDebug("Contact info participants panel");
                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra("name", selectedChat.getPeerEmailByHandle(participantHandle));
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }

            case R.id.start_chat_group_participants_chat_layout: {
                logDebug("Start chat participants panel");
                ((GroupChatInfoActivityLollipop) context).startConversation(participantHandle);
                break;
            }

            case R.id.change_permissions_group_participants_chat_layout: {
                logDebug("Change permissions participants panel");
                ((GroupChatInfoActivityLollipop) context).showChangePermissionsDialog(participantHandle, selectedChat);
                break;
            }

            case R.id.remove_group_participants_chat_layout: {
                logDebug("Remove participants panel");
                ((GroupChatInfoActivityLollipop) context).showRemoveParticipantConfirmation(participantHandle, selectedChat);
                break;
            }

            case R.id.edit_profile_group_participants_chat_layout: {
                logDebug("Edit profile participants panel");
                Intent editProfile = new Intent(context, ManagerActivityLollipop.class);
                editProfile.setAction(ACTION_SHOW_MY_ACCOUNT);
                startActivity(editProfile);
                dismissAllowingStateLoss();
                break;
            }

            case R.id.leave_group_participants_chat_layout: {
                logDebug("Leave chat participants panel");
                ((GroupChatInfoActivityLollipop) context).showConfirmationLeaveChat(selectedChat);
                break;
            }

            case R.id.invite_group_participants_chat_layout: {
                logDebug("Invite contact participants panel");
                ((GroupChatInfoActivityLollipop) context).inviteContact(selectedChat.getPeerEmailByHandle(participantHandle));
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
        outState.putLong("participantHandle", participantHandle);
    }
}
