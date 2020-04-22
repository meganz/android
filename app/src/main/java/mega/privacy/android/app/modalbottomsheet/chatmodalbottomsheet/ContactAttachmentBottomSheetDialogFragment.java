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

import java.io.File;
import java.util.ArrayList;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

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
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public EmojiTextView titleNameContactChatPanel;
    public ImageView stateIcon;
    public EmojiTextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;
    LinearLayout optionView;
    LinearLayout optionInfo;
    LinearLayout optionStartConversation;
    LinearLayout optionInvite;
    LinearLayout optionRemove;
    private LinearLayout optionSelect;

    DisplayMetrics outMetrics;

    static ManagerActivityLollipop.DrawerItem drawerItem = null;
    Bitmap thumb = null;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;

    private int heightDisplay;
    private int positionMessage;

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
            positionMessage = savedInstanceState.getInt(POSITION_SELECTED_MESSAGE, INVALID_POSITION);
            logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
            email = savedInstanceState.getString("email");
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if(messageMega!=null){
                message = new AndroidMegaChatMessage(messageMega);
            }
        }
        else{
            logWarning("Bundle NULL");

            if(context instanceof ChatActivityLollipop){
                chatId = ((ChatActivityLollipop) context).idChat;
                messageId = ((ChatActivityLollipop) context).selectedMessageId;
                positionMessage = ((ChatActivityLollipop) context).selectedPosition;
            }
            else{
                chatId = ((ContactAttachmentActivityLollipop) context).chatId;
                messageId = ((ContactAttachmentActivityLollipop) context).messageId;
                email = ((ContactAttachmentActivityLollipop) context).selectedEmail;
            }

            logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
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
        logDebug("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_attachment_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.contact_attachment_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleNameContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_name_text);
        stateIcon = (ImageView) contentView.findViewById(R.id.contact_attachment_state_circle);

        stateIcon.setVisibility(View.VISIBLE);

        stateIcon.setMaxWidth(scaleWidthPx(6,outMetrics));
        stateIcon.setMaxHeight(scaleHeightPx(6,outMetrics));

        titleMailContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_mail_text);
        contactImageView = (RoundedImageView) contentView.findViewById(R.id.contact_attachment_thumbnail);

        optionView = (LinearLayout) contentView.findViewById(R.id.option_view_layout);
        optionInfo = (LinearLayout) contentView.findViewById(R.id.option_info_layout);
        optionStartConversation = (LinearLayout) contentView.findViewById(R.id.option_start_conversation_layout);
        optionInvite = (LinearLayout) contentView.findViewById(R.id.option_invite_layout);
        optionRemove = (LinearLayout) contentView.findViewById(R.id.option_remove_layout);
        optionSelect = contentView.findViewById(R.id.option_select_layout);

        optionView.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionStartConversation.setOnClickListener(this);
        optionInvite.setOnClickListener(this);
        optionSelect.setOnClickListener(this);

        LinearLayout separatorInfo = (LinearLayout) contentView.findViewById(R.id.separator_info);

        optionRemove.setVisibility(View.GONE);

        if(isScreenInPortrait(context)){
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        }else{
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        if (message != null) {
            if(context instanceof ChatActivityLollipop) {
                optionSelect.setVisibility(View.VISIBLE);
            }else{
                optionSelect.setVisibility(View.GONE);
            }

            long userCount  = message.getMessage().getUsersCount();
            if(userCount==1){
                logDebug("One contact attached");

                optionView.setVisibility(View.GONE);
                optionInfo.setVisibility(View.VISIBLE);

                long userHandle = message.getMessage().getUserHandle(0);
                int state = megaChatApi.getUserOnlineStatus(userHandle);
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

                if(userHandle != megaChatApi.getMyUserHandle()){
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
                            logDebug("Non contact");
                            optionInfo.setVisibility(View.GONE);
                            optionStartConversation.setVisibility(View.GONE);
                            optionInvite.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        logDebug("Non contact");
                        optionInfo.setVisibility(View.GONE);
                        optionStartConversation.setVisibility(View.GONE);
                        optionInvite.setVisibility(View.VISIBLE);
                    }

                    addAvatarParticipantPanel(userHandle, userEmail, userName);
                }
            }
            else {
                logDebug("More than one contact attached");

                if(email==null){
                    logDebug("Panel shown from ChatActivity");
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
                                logDebug("Non contact");
                                optionStartConversation.setVisibility(View.GONE);
                                optionInvite.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                        else{
                            logDebug("Non contact");
                            optionStartConversation.setVisibility(View.GONE);
                            optionInvite.setVisibility(View.VISIBLE);
                            break;
                        }
                    }

                    logDebug("Names of attached contacts: " + name);
                    titleMailContactChatPanel.setText(name);

                    String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int)userCount, userCount);
                    titleNameContactChatPanel.setText(email);

                    addAvatarParticipantPanel(-1, null, userCount+"");
                }
                else{
                    logDebug("Panel shown from ContactAttachmentActivity - always one contact selected");
                    optionView.setVisibility(View.GONE);

                    stateIcon.setVisibility(View.VISIBLE);

                    position = getPositionByMail(email);
                    logDebug("Position selected: " + position);
                    if(position==-1){
                        logWarning("Error - position -1");
                        return;
                    }

                    optionStartConversation.setVisibility(View.VISIBLE);
                    optionInvite.setVisibility(View.GONE);

                    String email = message.getMessage().getUserEmail(position);
                    titleMailContactChatPanel.setText(email);

                    logDebug("Contact Email: " + email);

                    long userHandle = message.getMessage().getUserHandle(position);
                    logDebug("Contact Handle: " + userHandle);
                    String name = "";
                    name = chatC.getFullName(userHandle, chatId);
                    logDebug("Name before: " + name);
                    name = message.getMessage().getUserName(position);
                    if (name!=null){
                        logDebug("Name here: " + name);
                        if(name.trim().isEmpty()) {
                            name = chatC.getFullName(userHandle, chatId);
                            if(name.trim().isEmpty()) {
                                name = email;
                            }
                        }
                    }
                    else{
                        logWarning("Contact Name: is NULL... find more...");
                        name = chatC.getFullName(userHandle, chatId);
                        if(name.trim().isEmpty()) {
                            name = email;
                        }
                    }
                    logDebug("Contact Name: " + name);

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
                            logDebug("Non contact");
                            optionInfo.setVisibility(View.GONE);
                            optionStartConversation.setVisibility(View.GONE);
                            optionInvite.setVisibility(View.VISIBLE);
                        }
                    }
                    else{
                        logDebug("Non contact");
                        optionInfo.setVisibility(View.GONE);
                        optionStartConversation.setVisibility(View.GONE);
                        optionInvite.setVisibility(View.VISIBLE);
                    }

                    addAvatarParticipantPanel(userHandle, email, name);
                }
            }

            if (optionInfo.getVisibility() == View.GONE && optionView.getVisibility() == View.GONE){
                separatorInfo.setVisibility(View.GONE);
            }
            else {
                separatorInfo.setVisibility(View.VISIBLE);
            }

            dialog.setContentView(contentView);

            mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
            mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    public void addAvatarParticipantPanel(long handle, String email, String name){
        logDebug("handle: " + handle);
        /*Default Avatar*/
        contactImageView.setImageBitmap(getDefaultAvatar(getColorAvatar(handle), name, AVATAR_SIZE, true));

        if (handle != -1) {
            /*Avatar*/
            File avatar = buildAvatarFile(getActivity(),email + ".jpg");
            Bitmap bitmap = null;
            if (isFileAvailable(avatar)) {
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
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        if(message==null){
            logWarning("Error. The message is NULL");
            return;
        }

        switch(v.getId()){

            case R.id.option_info_layout:{
                logDebug("Info option");

                if (!isOnline(context)){
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
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
                        logWarning("Error - position -1");
                    }
                }

                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_view_layout:{
                logDebug("View option");
                Intent i = new Intent(context, ContactAttachmentActivityLollipop.class);
                i.putExtra("chatId", chatId);
                i.putExtra("messageId", messageId);
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_select_layout:
                if(context instanceof ChatActivityLollipop){
                    ((ChatActivityLollipop)context).activateActionModeWithItem(positionMessage);
                }
                dismissAllowingStateLoss();
                break;
            case R.id.option_invite_layout:{
                logDebug("Invite option");

                if (!isOnline(context)){
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
                }
                else{
                    ContactController cC = new ContactController(context);
                    ArrayList<String> contactEmails;
                    long numUsers = message.getMessage().getUsersCount();

                    if(context instanceof ChatActivityLollipop){
                        if(numUsers==1){
                            cC.inviteContact(message.getMessage().getUserEmail(0));
                        }
                        else{
                            logDebug("Num users to invite: " + numUsers);
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
                }

                break;
            }
            case R.id.option_start_conversation_layout:{
                logDebug("Start conversation option");

                long numUsers = message.getMessage().getUsersCount();
                if(context instanceof ChatActivityLollipop){
                    if(numUsers==1){
                        ((ChatActivityLollipop) context).startConversation(message.getMessage().getUserHandle(0));
                        dismissAllowingStateLoss();
                    }
                    else{
                        logDebug("Num users to invite: " + numUsers);
                        ArrayList<Long> contactHandles = new ArrayList<>();

                        for(int i=0;i<numUsers;i++){
                            long userHandle = message.getMessage().getUserHandle(i);
                            contactHandles.add(userHandle);
                        }
                        ((ChatActivityLollipop) context).startGroupConversation(contactHandles);
                    }
                }
                else{
                    logDebug("Instance of ContactAttachmentActivityLollipop");
                    logDebug("position: " + position);
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
        if (context instanceof ChatActivityLollipop) {
            outState.putLong(POSITION_SELECTED_MESSAGE, positionMessage);
        }
    }
}
