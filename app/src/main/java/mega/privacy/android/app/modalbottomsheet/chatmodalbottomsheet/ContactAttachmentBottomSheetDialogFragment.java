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
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;

public class ContactAttachmentBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaHandleList handleList;
    AndroidMegaChatMessage message = null;
    long chatId;
    long messageId;
    String email=null;
    NodeController nC;
    ChatController chatC;

    MegaUser contact;
    int position;

    private BottomSheetBehavior mBehavior;

    public LinearLayout mainLinearLayout;
    public TextView titleNameContactChatPanel;
    public ImageView stateIcon;
    public TextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;
    public TextView contactInitialLetter;
    LinearLayout optionView;
    LinearLayout optionInfo;
    LinearLayout optionStartConversation;
    LinearLayout optionInvite;
    LinearLayout optionRemove;

    DisplayMetrics outMetrics;

    static ManagerActivityLollipop.DrawerItem drawerItem = null;
    Bitmap thumb = null;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            log("Handle of the chat: "+chatId);
            messageId = savedInstanceState.getLong("messageId", -1);
            log("Handle of the message: "+messageId);
            email = savedInstanceState.getString("email");
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if(messageMega!=null){
                message = new AndroidMegaChatMessage(messageMega);
            }
        }
        else{
            log("Bundle NULL");

            if(context instanceof ChatActivityLollipop){
                chatId = ((ChatActivityLollipop) context).idChat;
                messageId = ((ChatActivityLollipop) context).selectedMessageId;
            }
            else{
                chatId = ((ContactAttachmentActivityLollipop) context).chatId;
                messageId = ((ContactAttachmentActivityLollipop) context).messageId;
                email = ((ContactAttachmentActivityLollipop) context).selectedEmail;
            }

            log("Id Chat and Message id: "+chatId+ "___"+messageId);
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if(messageMega!=null){
                message = new AndroidMegaChatMessage(messageMega);
            }
        }

        nC = new NodeController(context);
        chatC = new ChatController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);
        log("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_attachment_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.contact_attachment_bottom_sheet);

        titleNameContactChatPanel = (TextView) contentView.findViewById(R.id.contact_attachment_chat_name_text);
        stateIcon = (ImageView) contentView.findViewById(R.id.contact_attachment_state_circle);

        stateIcon.setVisibility(View.VISIBLE);

        stateIcon.setMaxWidth(Util.scaleWidthPx(6,outMetrics));
        stateIcon.setMaxHeight(Util.scaleHeightPx(6,outMetrics));

        titleMailContactChatPanel = (TextView) contentView.findViewById(R.id.contact_attachment_chat_mail_text);
        contactImageView = (RoundedImageView) contentView.findViewById(R.id.contact_attachment_thumbnail);
        contactInitialLetter = (TextView) contentView.findViewById(R.id.contact_attachment_initial_letter);

        optionView = (LinearLayout) contentView.findViewById(R.id.option_view_layout);
        optionInfo = (LinearLayout) contentView.findViewById(R.id.option_info_layout);
        optionStartConversation = (LinearLayout) contentView.findViewById(R.id.option_start_conversation_layout);
        optionInvite = (LinearLayout) contentView.findViewById(R.id.option_invite_layout);
        optionRemove = (LinearLayout) contentView.findViewById(R.id.option_remove_layout);

        optionView.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionStartConversation.setOnClickListener(this);
        optionInvite.setOnClickListener(this);

        optionRemove.setVisibility(View.GONE);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            log("onCreate: Landscape configuration");
            titleNameContactChatPanel.setMaxWidth(Util.scaleWidthPx(270, outMetrics));
            titleMailContactChatPanel.setMaxWidth(Util.scaleWidthPx(270, outMetrics));
        }
        else{
            titleNameContactChatPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
            titleMailContactChatPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        }

        if (message != null) {
            long userCount  = message.getMessage().getUsersCount();
            if(userCount==1){
                log("One contact attached");

                optionView.setVisibility(View.GONE);
                optionInfo.setVisibility(View.VISIBLE);

                long userHandle = message.getMessage().getUserHandle(0);
                int state = megaChatApi.getUserOnlineStatus(userHandle);
                if(state == MegaChatApi.STATUS_ONLINE){
                    log("This user is connected");
                    stateIcon.setVisibility(View.VISIBLE);
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
                }
                else if(state == MegaChatApi.STATUS_AWAY){
                    log("This user is away");
                    stateIcon.setVisibility(View.VISIBLE);
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
                }
                else if(state == MegaChatApi.STATUS_BUSY){
                    log("This user is busy");
                    stateIcon.setVisibility(View.VISIBLE);
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
                }
                else if(state == MegaChatApi.STATUS_OFFLINE){
                    log("This user is offline");
                    stateIcon.setVisibility(View.VISIBLE);
                    stateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
                }
                else if(state == MegaChatApi.STATUS_INVALID){
                    log("INVALID status: "+state);
                    stateIcon.setVisibility(View.GONE);
                }
                else{
                    log("This user status is: "+state);
                    stateIcon.setVisibility(View.GONE);
                }

                if(userHandle != megaApi.getMyUser().getHandle()){
//                    addAvatarParticipantPanel(userHandle, megaChatApi.getMyEmail());

                    String userName = message.getMessage().getUserName(0);
                    titleNameContactChatPanel.setText(userName);

                    String userEmail = message.getMessage().getUserEmail(0);
                    titleMailContactChatPanel.setText(userEmail);
                    MegaUser contact = megaApi.getContact(userEmail);

                    if(contact!=null) {
                        if (contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                            optionInfo.setVisibility(View.VISIBLE);

                            //Check if the contact is the same that the one is chatting
                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                            if(!chatRoom.isGroup()){
                                long contactHandle = message.getMessage().getUserHandle(0);
                                long messageContactHandle = chatRoom.getPeerHandle(0);
                                if(contactHandle==messageContactHandle){
                                    optionStartConversation.setVisibility(View.GONE);
                                }
                                else{
                                    optionStartConversation.setVisibility(View.VISIBLE);
                                }
                            }
                            else{
                                optionStartConversation.setVisibility(View.VISIBLE);
                            }

                            optionInvite.setVisibility(View.GONE);
                        }
                        else{
                            log("Non contact");
                            optionInfo.setVisibility(View.GONE);
                            optionStartConversation.setVisibility(View.GONE);
                            optionInvite.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        log("Non contact");
                        optionInfo.setVisibility(View.GONE);
                        optionStartConversation.setVisibility(View.GONE);
                        optionInvite.setVisibility(View.VISIBLE);
                    }

                    addAvatarParticipantPanel(userHandle, userEmail, userName);
                }
            }
            else {
                log("More than one contact attached");

                if(email==null){
                    log("Panel shown from ChatActivity");
                    optionView.setVisibility(View.VISIBLE);
                    optionInfo.setVisibility(View.GONE);

                    stateIcon.setVisibility(View.GONE);

                    StringBuilder name = new StringBuilder("");
                    name.append(message.getMessage().getUserName(0));
                    for(int i=1; i<userCount;i++){
                        name.append(", "+message.getMessage().getUserName(i));
                    }

                    optionInvite.setVisibility(View.GONE);

                    for(int i=1; i<userCount;i++){
                        String userEmail = message.getMessage().getUserEmail(i);
                        contact = megaApi.getContact(userEmail);

                        if(contact!=null) {
                            if (contact.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
                                log("Non contact");
                                optionStartConversation.setVisibility(View.GONE);
                                optionInvite.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                        else{
                            log("Non contact");
                            optionStartConversation.setVisibility(View.GONE);
                            optionInvite.setVisibility(View.VISIBLE);
                            break;
                        }
                    }

                    log("Names of attached contacts: "+name);
                    titleMailContactChatPanel.setText(name);

                    String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int)userCount, userCount);
                    titleNameContactChatPanel.setText(email);

                    addAvatarParticipantPanel(-1, null, userCount+"");
                }
                else{
                    log("Panel shown from ContactAttachmentActivity - always one contact selected");
                    optionView.setVisibility(View.GONE);

                    stateIcon.setVisibility(View.VISIBLE);

                    position = getPositionByMail(email);
                    log("Position selected: "+position);
                    if(position==-1){
                        log("Error - position -1");
                        return;
                    }

                    optionStartConversation.setVisibility(View.VISIBLE);
                    optionInvite.setVisibility(View.GONE);

                    String email = message.getMessage().getUserEmail(position);
                    titleMailContactChatPanel.setText(email);

                    log("Contact Email: " + email);

                    long userHandle = message.getMessage().getUserHandle(position);
                    log("Contact Handle: " + userHandle);
                    String name = "";
                    name = chatC.getFullName(userHandle, chatId);
                    log("name before: "+name);
                    name = message.getMessage().getUserName(position);
                    if (name!=null){
                        log("Name here: "+name);
                        if(name.trim().isEmpty()) {
                            name = chatC.getFullName(userHandle, chatId);
                            if(name.trim().isEmpty()) {
                                name = email;
                            }
                        }
                    }
                    else{
                        log("Contact Name: is NULL... find more...");
                        name = chatC.getFullName(userHandle, chatId);
                        if(name.trim().isEmpty()) {
                            name = email;
                        }
                    }
                    log("Contact Name: " + name);

                    titleNameContactChatPanel.setText(name);

                    contact = megaApi.getContact(email);

                    if(contact!=null) {
                        if (contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                            optionInfo.setVisibility(View.VISIBLE);
                            //Check if the contact is the same that the one is chatting
                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                            if(!chatRoom.isGroup()){
                                long messageContactHandle = chatRoom.getPeerHandle(0);
                                if(contact.getHandle()==messageContactHandle){
                                    optionStartConversation.setVisibility(View.GONE);
                                }
                                else{
                                    optionStartConversation.setVisibility(View.VISIBLE);
                                }
                            }
                            else{
                                optionStartConversation.setVisibility(View.VISIBLE);
                            }

                            optionInvite.setVisibility(View.GONE);
                        }
                        else{
                            log("Non contact");
                            optionInfo.setVisibility(View.GONE);
                            optionStartConversation.setVisibility(View.GONE);
                            optionInvite.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        log("Non contact");
                        optionInfo.setVisibility(View.GONE);
                        optionStartConversation.setVisibility(View.GONE);
                        optionInvite.setVisibility(View.VISIBLE);
                    }

                    addAvatarParticipantPanel(userHandle, email, name);
                }
            }
            dialog.setContentView(contentView);

            mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        }
    }

    public void addAvatarParticipantPanel(long handle, String email, String name){
        log("addAvatarParticipantPanel: "+handle);
        File avatar = null;

        if(handle!=-1){
            //Ask for avatar
            if (getActivity().getExternalCacheDir() != null){
                avatar = new File(getActivity().getExternalCacheDir().getAbsolutePath(), email + ".jpg");
            }
            else{
                avatar = new File(getActivity().getCacheDir().getAbsolutePath(), email + ".jpg");
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

            log("Set default avatar");
            ////DEfault AVATAR
            Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(defaultAvatar);
            Paint p = new Paint();
            p.setAntiAlias(true);
            String userHandleEncoded = MegaApiAndroid.userHandleToBase64(handle);
            String color = megaApi.getUserAvatarColor(userHandleEncoded);
            if(color!=null){
                log("The color to set the avatar is "+color);
                p.setColor(Color.parseColor(color));
            }
            else{
                log("Default color to the avatar");
                p.setColor(getResources().getColor(R.color.lollipop_primary_color));
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
        }
        else{
            log("Set default avatar HANDLE is Null");
            ////DEfault AVATAR
            Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(defaultAvatar);
            Paint p = new Paint();
            p.setAntiAlias(true);
            p.setColor(getResources().getColor(R.color.lollipop_primary_color));


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

            if(name!=null){
                contactInitialLetter.setText(name);
                contactInitialLetter.setTextColor(Color.WHITE);
                contactInitialLetter.setVisibility(View.VISIBLE);
                contactInitialLetter.setTextSize(24);
            }
            else{
                contactInitialLetter.setVisibility(View.GONE);
            }
        }

        ////
    }

    @Override
    public void onClick(View v) {
        log("onClick");

        if(message==null){
            log("Error. The message is NULL");
            return;
        }

        switch(v.getId()){

            case R.id.option_info_layout:{
                log("Info option");

                if (!Util.isOnline(context)){
                    ((ChatActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                    return;
                }

                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                if(context instanceof ChatActivityLollipop){
                    i.putExtra("name", message.getMessage().getUserEmail(0));
                }
                else{
                    if(position!=-1){
                        i.putExtra("name", message.getMessage().getUserEmail(position));
                    }
                    else{
                        log("Error - position -1");
                    }
                }

                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_view_layout:{
                log("View option");
                Intent i = new Intent(context, ContactAttachmentActivityLollipop.class);
                i.putExtra("chatId", chatId);
                i.putExtra("messageId", messageId);
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_invite_layout:{
                log("Invite option");

                if (!Util.isOnline(context)){
                    ((ChatActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                    return;
                }

                ContactController cC = new ContactController(context);
                ArrayList<String> contactEmails;
                long numUsers = message.getMessage().getUsersCount();

                if(context instanceof ChatActivityLollipop){
                    if(numUsers==1){
                        cC.inviteContact(message.getMessage().getUserEmail(0));
                    }
                    else{
                        log("Num users to invite: "+numUsers);
                        contactEmails = new ArrayList<>();

                        for(int i=0;i<numUsers;i++){
                            String userMail = message.getMessage().getUserEmail(i);
                            contactEmails.add(userMail);
                        }
                        cC.inviteMultipleContacts(contactEmails);
                    }
                }
                else{
                    if(email!=null){
                        cC.inviteContact(email);
                    }
                }

                break;
            }
            case R.id.option_start_conversation_layout:{
                log("Start conversation option");

                long numUsers = message.getMessage().getUsersCount();
                if(context instanceof ChatActivityLollipop){
                    if(numUsers==1){
                        ((ChatActivityLollipop) context).startConversation(message.getMessage().getUserHandle(0));
                        dismissAllowingStateLoss();
                    }
                    else{
                        log("Num users to invite: "+numUsers);
                        ArrayList<Long> contactHandles = new ArrayList<>();

                        for(int i=0;i<numUsers;i++){
                            long userHandle = message.getMessage().getUserHandle(i);
                            contactHandles.add(userHandle);
                        }
                        ((ChatActivityLollipop) context).startGroupConversation(contactHandles);
                    }
                }
                else{
                    log("instance of ContactAttachmentActivityLollipop");
                    log("position: "+position);
                    long userHandle = message.getMessage().getUserHandle(position);
                    ((ContactAttachmentActivityLollipop) context).startConversation(userHandle);
                    dismissAllowingStateLoss();
                }

                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public int getPositionByMail(String email){
        long userCount = message.getMessage().getUsersCount();
        for(int i=0;i<userCount;i++){
            if(message.getMessage().getUserEmail(i).equals(email)){
                return i;
            }
        }
        return -1;
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

        outState.putLong("chatId", chatId);
        outState.putLong("messageId", messageId);
    }

    private static void log(String log) {
        Util.log("ContactAttachmentBottomSheetDialogFragment", log);
    }
}
