package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

public class ChatBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaChatListItem chat = null;
    long chatId;

    private BottomSheetBehavior mBehavior;

    public LinearLayout mainLinearLayout;
    public TextView titleNameContactChatPanel;
    public ImageView iconStateChatPanel;
    public TextView titleMailContactChatPanel;
    public RoundedImageView chatImageView;
    public TextView chatInitialLetter;
    public TextView infoChatText;
    public LinearLayout optionInfoChat;
    public LinearLayout optionLeaveChat;
    public LinearLayout optionClearHistory;
    public LinearLayout optionMuteChat;
    public ImageView optionMuteChatIcon;
    public TextView optionMuteChatText;


    boolean notificationsEnabled;
    ChatItemPreferences chatPrefs;

    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;

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
            log("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            log("Handle of the chat: "+chatId);
        }
        else{
            log("Bundle NULL");
            if(context instanceof ManagerActivityLollipop){
                chatId = ((ManagerActivityLollipop) context).selectedChatItemId;

            }
        }

        if(chatId!=-1){
            chat = megaChatApi.getChatListItem(chatId);
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.chat_item_bottom_sheet, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.chat_item_bottom_sheet);

        iconStateChatPanel = (ImageView) contentView.findViewById(R.id.chat_list_contact_state);

        iconStateChatPanel.setMaxWidth(Util.scaleWidthPx(6,outMetrics));
        iconStateChatPanel.setMaxHeight(Util.scaleHeightPx(6,outMetrics));

        titleNameContactChatPanel = (TextView) contentView.findViewById(R.id.chat_list_chat_name_text);
        titleMailContactChatPanel = (TextView) contentView.findViewById(R.id.chat_list_chat_mail_text);
        chatImageView = (RoundedImageView) contentView.findViewById(R.id.sliding_chat_list_thumbnail);
        chatInitialLetter = (TextView) contentView.findViewById(R.id.sliding_chat_list_initial_letter);
        infoChatText = (TextView) contentView.findViewById(R.id.chat_list_info_chat_text);
        optionInfoChat = (LinearLayout) contentView.findViewById(R.id.chat_list_info_chat_layout);
        optionLeaveChat= (LinearLayout) contentView.findViewById(R.id.chat_list_leave_chat_layout);
        optionClearHistory = (LinearLayout) contentView.findViewById(R.id.chat_list_clear_history_chat_layout);
        optionMuteChat = (LinearLayout) contentView.findViewById(R.id.chat_list_mute_chat_layout);
        optionMuteChatIcon = (ImageView) contentView.findViewById(R.id.chat_list_mute_chat_image);
        optionMuteChatText = (TextView) contentView.findViewById(R.id.chat_list_mute_chat_text);

        titleNameContactChatPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        titleMailContactChatPanel.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        optionInfoChat.setOnClickListener(this);
        optionMuteChat.setOnClickListener(this);
        optionLeaveChat.setOnClickListener(this);
        optionClearHistory.setOnClickListener(this);

        titleNameContactChatPanel.setText(chat.getTitle());

		if(chat.isGroup()){
			titleMailContactChatPanel.setText(getString(R.string.group_chat_label));
			iconStateChatPanel.setVisibility(View.GONE);
			addAvatarChatPanel(null, chat);
		}
		else{
			iconStateChatPanel.setVisibility(View.VISIBLE);

            long userHandle = chat.getPeerHandle();
            int state = megaChatApi.getUserOnlineStatus(userHandle);

            if(state == MegaChatApi.STATUS_ONLINE){
                log("This user is connected");
                iconStateChatPanel.setVisibility(View.VISIBLE);
                iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
            }
            else if(state == MegaChatApi.STATUS_AWAY){
                log("This user is away");
                iconStateChatPanel.setVisibility(View.VISIBLE);
                iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
            }
            else if(state == MegaChatApi.STATUS_BUSY){
                log("This user is busy");
                iconStateChatPanel.setVisibility(View.VISIBLE);
                iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
            }
            else if(state == MegaChatApi.STATUS_OFFLINE){
                log("This user is offline");
                iconStateChatPanel.setVisibility(View.VISIBLE);
                iconStateChatPanel.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
            }
            else if(state == MegaChatApi.STATUS_INVALID){
                log("INVALID status: "+state);
                iconStateChatPanel.setVisibility(View.GONE);
            }
            else{
                log("This user status is: "+state);
                iconStateChatPanel.setVisibility(View.GONE);
            }

			String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
			MegaUser user = megaApi.getContact(userHandleEncoded);
			if(user!=null){
				log("El email del user es: "+user.getEmail());
				titleMailContactChatPanel.setText(user.getEmail());
				addAvatarChatPanel(user.getEmail(), chat);
			}
			else{
				log("El user es NULL");
			}
		}

		if(chat.isGroup()){
			infoChatText.setText(getString(R.string.group_chat_info_label));

			optionLeaveChat.setVisibility(View.VISIBLE);

            if(chat.getLastMessageType()== MegaChatMessage.TYPE_INVALID){
                optionClearHistory.setVisibility(View.GONE);
            }
            else{
                if(chat.getOwnPrivilege()!= MegaChatRoom.PRIV_MODERATOR){
                    optionClearHistory.setVisibility(View.GONE);
                }
                else{
                    optionClearHistory.setVisibility(View.VISIBLE);
                }
            }

		}
		else{
			infoChatText.setText(getString(R.string.contact_properties_activity));
			optionLeaveChat.setVisibility(View.GONE);

            if(chat.getLastMessageType()== MegaChatMessage.TYPE_INVALID){
                optionClearHistory.setVisibility(View.GONE);
            }
            else{
                optionClearHistory.setVisibility(View.VISIBLE);
            }
		}

		chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chat.getChatId()));
		if(chatPrefs!=null) {
			log("Chat prefs exists!!!");
			notificationsEnabled = true;
			if (chatPrefs.getNotificationsEnabled() != null) {
				notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
			}

			if (!notificationsEnabled) {
				log("Chat is MUTE");
				optionMuteChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_unmute));
				optionMuteChatText.setText(getString(R.string.general_unmute));
			}
			else{
				log("Chat with notifications enabled!!");
				optionMuteChatText.setText(getString(R.string.general_mute));
				optionMuteChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mute));
			}
		}
		else{
			log("Chat prefs is NULL");
			optionMuteChatText.setText(getString(R.string.general_mute));
			optionMuteChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_mute));
		}

        dialog.setContentView(contentView);
    }

    public void addAvatarChatPanel(String contactMail, MegaChatListItem chat){

        File avatar = null;
        if (getActivity().getExternalCacheDir() != null){
            avatar = new File(getActivity().getExternalCacheDir().getAbsolutePath(), contactMail + ".jpg");
        }
        else{
            avatar = new File(getActivity().getCacheDir().getAbsolutePath(), contactMail + ".jpg");
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
                    chatInitialLetter.setVisibility(View.GONE);
                    chatImageView.setImageBitmap(bitmap);
                    return;
                }
            }
        }

        ////DEfault AVATAR
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        if(chat.isGroup()){
            p.setColor(getResources().getColor(R.color.divider_upgrade_account));
        }
        else{
            MegaUser contact = megaApi.getContact(contactMail);
            if (contact != null) {
                String color = megaApi.getUserAvatarColor(contact);
                if (color != null) {
                    log("The color to set the avatar is " + color);
                    p.setColor(Color.parseColor(color));
                } else {
                    log("Default color to the avatar");
                    p.setColor(getResources().getColor(R.color.lollipop_primary_color));
                }
            } else {
                log("Contact is NULL");
                p.setColor(getResources().getColor(R.color.lollipop_primary_color));
            }
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth() / 2;
        else
            radius = defaultAvatar.getHeight() / 2;

        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);
        chatImageView.setImageBitmap(defaultAvatar);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;

        boolean setInitialByMail = false;

        if (chat.getTitle() != null) {
            if (chat.getTitle().trim().length() > 0) {
                String firstLetter = chat.getTitle().charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                chatInitialLetter.setText(firstLetter);
                chatInitialLetter.setTextColor(Color.WHITE);
                chatInitialLetter.setVisibility(View.VISIBLE);
            } else {
                setInitialByMail = true;
            }
        } else {
            setInitialByMail = true;
        }
        if (setInitialByMail) {
            if (contactMail != null) {
                if (contactMail.length() > 0) {
                    String firstLetter = contactMail.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    chatInitialLetter.setText(firstLetter);
                    chatInitialLetter.setTextColor(Color.WHITE);
                    chatInitialLetter.setVisibility(View.VISIBLE);
                }
            }
        }
        chatInitialLetter.setTextSize(22);
        ////
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.chat_list_info_chat_layout:{
                log("click contact info");
                if(chat==null){
                    log("Selected chat NULL");
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
                log("click leave chat");
                if(chat==null){
                    log("Selected chat NULL");
                }
                log("Leave chat with: "+chat.getTitle());
                ((ManagerActivityLollipop)context).showConfirmationLeaveChat(chat);
                break;
            }
            case R.id.chat_list_clear_history_chat_layout:{
                log("click clear history chat");
                if(chat==null){
                    log("Selected chat NULL");
                }
                log("Clear chat with: "+chat.getTitle());
                ((ManagerActivityLollipop)context).showConfirmationClearChat(chat);

                break;
            }
            case R.id.chat_list_mute_chat_layout:{
                log("click mute chat");
                if(chatPrefs==null) {

                    if(notificationsEnabled){
                        chatPrefs = new ChatItemPreferences(Long.toString(chat.getChatId()), Boolean.toString(true), "", "");
                    }
                    else{
                        chatPrefs = new ChatItemPreferences(Long.toString(chat.getChatId()), Boolean.toString(false), "", "");
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
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putLong("chatId", chatId);
    }

    private static void log(String log) {
        Util.log("ChatBottomSheetDialogFragment", log);
    }
}
