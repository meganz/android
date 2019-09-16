package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;

public class ParticipantBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaChatRoom selectedChat;
    long chatId = -1;
    long participantHandle = -1;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView titleNameContactChatPanel;
    public ImageView stateIcon;
    public ImageView permissionsIcon;
    public TextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;
    public TextView contactInitialLetter;
    public LinearLayout contactLayout;
    public LinearLayout optionContactInfoChat;
    public LinearLayout optionStartConversationChat;
    public LinearLayout optionEditProfileChat;
    public LinearLayout optionLeaveChat;
    public LinearLayout optionChangePermissionsChat;
    public LinearLayout optionRemoveParticipantChat;
    public LinearLayout optionInvite;
    ////

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
            LogUtil.logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            LogUtil.logDebug("Handle of the chat: " + chatId);
            participantHandle = savedInstanceState.getLong("participantHandle", -1);
            LogUtil.logDebug("Handle of the participant: " + participantHandle);

            selectedChat = megaChatApi.getChatRoom(chatId);
        }
        else{
            LogUtil.logWarning("Bundle NULL");

            chatId = ((GroupChatInfoActivityLollipop) context).chatHandle;
            selectedChat = megaChatApi.getChatRoom(chatId);
            LogUtil.logDebug("Handle of the chat: " + chatId);

            participantHandle = ((GroupChatInfoActivityLollipop) context).selectedHandleParticipant;
            LogUtil.logDebug("Handle of the participant: " + participantHandle);
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
        titleNameContactChatPanel = (TextView) contentView.findViewById(R.id.group_participants_chat_name_text);
        stateIcon = (ImageView) contentView.findViewById(R.id.group_participants_state_circle);

        stateIcon.setVisibility(View.VISIBLE);

        stateIcon.setMaxWidth(Util.scaleWidthPx(6,outMetrics));
        stateIcon.setMaxHeight(Util.scaleHeightPx(6,outMetrics));

        permissionsIcon = (ImageView) contentView.findViewById(R.id.group_participant_list_permissions);

        titleMailContactChatPanel = (TextView) contentView.findViewById(R.id.group_participants_chat_mail_text);
        contactLayout = (LinearLayout) contentView.findViewById(R.id.group_participants_chat);
        contactImageView = (RoundedImageView) contentView.findViewById(R.id.sliding_group_participants_chat_list_thumbnail);
        contactInitialLetter = (TextView) contentView.findViewById(R.id.sliding_group_participants_chat_list_initial_letter);

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
        //////

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            LogUtil.logDebug("Landscape configuration");
            titleNameContactChatPanel.setMaxWidth(Util.scaleWidthPx(270, outMetrics));
            titleMailContactChatPanel.setMaxWidth(Util.scaleWidthPx(270, outMetrics));
        }
        else{
            titleNameContactChatPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
            titleMailContactChatPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        }

        if(selectedChat==null){
            LogUtil.logWarning("Error. Selected chat is NULL");
            return;
        }

        if(participantHandle==-1){
            LogUtil.logWarning("Error. Participant handle is -1");
            return;
        }

        int state = megaChatApi.getUserOnlineStatus(participantHandle);
        if(state == MegaChatApi.STATUS_ONLINE){
            LogUtil.logDebug("This user is connected");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
        }
        else if(state == MegaChatApi.STATUS_AWAY){
            LogUtil.logDebug("This user is away");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
        }
        else if(state == MegaChatApi.STATUS_BUSY){
            LogUtil.logDebug("This user is busy");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
        }
        else if(state == MegaChatApi.STATUS_OFFLINE){
            LogUtil.logDebug("This user is offline");
            stateIcon.setVisibility(View.VISIBLE);
            stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
        }
        else if(state == MegaChatApi.STATUS_INVALID){
            LogUtil.logWarning("INVALID status: " + state);
            stateIcon.setVisibility(View.GONE);
        }
        else{
            LogUtil.logDebug("This user status is: " + state);
            stateIcon.setVisibility(View.GONE);
        }

        if(participantHandle == megaApi.getMyUser().getHandle()){
            LogUtil.logDebug("Participant selected its me");
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
                    LogUtil.logDebug("Non contact");
                    optionContactInfoChat.setVisibility(View.GONE);
                    optionStartConversationChat.setVisibility(View.GONE);
                    optionInvite.setVisibility(View.VISIBLE);
                }
            }
            else{
                LogUtil.logDebug("Non contact");
                optionContactInfoChat.setVisibility(View.GONE);
                optionStartConversationChat.setVisibility(View.GONE);
                optionInvite.setVisibility(View.VISIBLE);
            }

            optionEditProfileChat.setVisibility(View.GONE);
            optionLeaveChat.setVisibility(View.GONE);

            LogUtil.logDebug("Other participant selected its me");
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

    public void addAvatarParticipantPanel(long handle, String email, String name){

        File avatar = null;
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
                    contactInitialLetter.setVisibility(View.GONE);
                    contactImageView.setImageBitmap(bitmap);
                    return;
                }
            }
        }

        LogUtil.logDebug("Set default avatar");
        ////DEfault AVATAR
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        String color = megaApi.getUserAvatarColor(userHandleEncoded);
        if(color!=null){
            LogUtil.logDebug("The color to set the avatar is " + color);
            p.setColor(Color.parseColor(color));
        }
        else{
            LogUtil.logDebug("Default color to the avatar");
            p.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        contactImageView.setImageBitmap(defaultAvatar);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float density  = getResources().getDisplayMetrics().density;

        if(name!=null){
            if(!(name.trim().isEmpty())){
                String firstLetter = name.charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                contactInitialLetter.setText(firstLetter);
                contactInitialLetter.setTextColor(Color.WHITE);
                contactInitialLetter.setVisibility(View.VISIBLE);
                contactInitialLetter.setTextSize(24);
            }
            else{
                contactInitialLetter.setVisibility(View.GONE);
            }
        }
        else{
            contactInitialLetter.setVisibility(View.GONE);
        }

        ////
    }

    @Override
    public void onClick(View v) {

        MegaChatRoom selectedChat = null;
        if(context instanceof GroupChatInfoActivityLollipop){
            selectedChat = ((GroupChatInfoActivityLollipop) context).getChat();
        }

        switch(v.getId()){

            case R.id.contact_info_group_participants_chat_layout: {
                LogUtil.logDebug("Contact info participants panel");
                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra("name", selectedChat.getPeerEmailByHandle(participantHandle));
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }

            case R.id.start_chat_group_participants_chat_layout: {
                LogUtil.logDebug("Start chat participants panel");
                ((GroupChatInfoActivityLollipop) context).startConversation(participantHandle);
                break;
            }

            case R.id.change_permissions_group_participants_chat_layout: {
                LogUtil.logDebug("Change permissions participants panel");
                ((GroupChatInfoActivityLollipop) context).showChangePermissionsDialog(participantHandle, selectedChat);
                break;
            }

            case R.id.remove_group_participants_chat_layout: {
                LogUtil.logDebug("Remove participants panel");
                ((GroupChatInfoActivityLollipop) context).showRemoveParticipantConfirmation(participantHandle, selectedChat);
                break;
            }

            case R.id.edit_profile_group_participants_chat_layout: {
                LogUtil.logDebug("Edit profile participants panel");
                Intent editProfile = new Intent(context, ManagerActivityLollipop.class);
                editProfile.setAction(Constants.ACTION_SHOW_MY_ACCOUNT);
                startActivity(editProfile);
                dismissAllowingStateLoss();
                break;
            }

            case R.id.leave_group_participants_chat_layout: {
                LogUtil.logDebug("Leave chat participants panel");
                ((GroupChatInfoActivityLollipop) context).showConfirmationLeaveChat(selectedChat);
                break;
            }

            case R.id.invite_group_participants_chat_layout: {
                LogUtil.logDebug("Invite contact participants panel");
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
        LogUtil.logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putLong("chatId", chatId);
        outState.putLong("participantHandle", participantHandle);
    }
}
