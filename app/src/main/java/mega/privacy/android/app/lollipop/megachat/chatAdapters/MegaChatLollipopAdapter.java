package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.WrapTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatAttachmentAvatarListener;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatHandleList;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static int LEFT_MARGIN_CONTACT_MSG_MANAGEMENT = 40;
    public static int RIGHT_MARGIN_CONTACT_MSG_MANAGEMENT = 68;

    public static int MULTISELECTION_LEFT_MARGIN_CONTACT_MSG_MANAGEMENT = 73;

    public static int LEFT_MARGIN_CONTACT_MSG_NORMAL = 73;

    Context context;
    int positionClicked;
    ArrayList<AndroidMegaChatMessage> messages;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    boolean multipleSelect;
    private SparseBooleanArray selectedItems;

    ChatController cC;

    long myUserHandle = -1;

    DisplayMetrics outMetrics;
    DatabaseHandler dbH = null;
    MegaChatRoom chatRoom;

    public MegaChatLollipopAdapter(Context _context, MegaChatRoom chatRoom, ArrayList<AndroidMegaChatMessage> _messages, RecyclerView _listView) {
        log("new adapter");
        this.context = _context;
        this.messages = _messages;
        this.positionClicked = -1;
        this.chatRoom = chatRoom;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        listFragment = _listView;

        if(messages!=null)
        {
            log("Number of messages: "+messages.size());
        }
        else{
            log("Number of messages: NULL");
        }

        myUserHandle = megaChatApi.getMyUserHandle();
        log("MegaChatLollipopAdapter: MyUserHandle: "+myUserHandle);
    }

    public static class ViewHolderMessageChat extends RecyclerView.ViewHolder {
        public ViewHolderMessageChat(View view) {
            super(view);
        }

        int currentPosition;
        long userHandle;
        String fullNameTitle;
        boolean nameRequested = false;
        boolean nameRequestedAction = false;

        RelativeLayout itemLayout;

        RelativeLayout dateLayout;
        TextView dateText;
        RelativeLayout ownMessageLayout;
        RelativeLayout titleOwnMessage;
//        TextView meText;
        TextView timeOwnText;
        RelativeLayout contentOwnMessageLayout;
        TextView contentOwnMessageText;

        ImageView contentOwnMessageThumbLand;
        ImageView contentOwnMessageThumbPort;
        RelativeLayout contentOwnMessageFileLayout;
        ImageView contentOwnMessageFileThumb;
        TextView contentOwnMessageFileName;
        TextView contentOwnMessageFileSize;

        RelativeLayout contentOwnMessageContactLayout;
        RelativeLayout contentOwnMessageContactLayoutAvatar;
        RoundedImageView contentOwnMessageContactThumb;
        TextView contentOwnMessageContactName;
        public TextView contentOwnMessageContactEmail;
        TextView contentOwnMessageContactInitialLetter;

        TextView retryAlert;
        ImageView triangleIcon;

        RelativeLayout ownMultiselectionLayout;
        ImageView ownMultiselectionImageView;
        ImageView ownMultiselectionTickIcon;

        RelativeLayout contactMessageLayout;
        RelativeLayout titleContactMessage;
//        TextView contactText;
        TextView timeContactText;
        RelativeLayout contentContactMessageLayout;
        TextView contentContactMessageText;

        ImageView contentContactMessageThumbLand;
        ImageView contentContactMessageThumbPort;
        RelativeLayout contentContactMessageFileLayout;
        ImageView contentContactMessageFileThumb;
        TextView contentContactMessageFileName;
        TextView contentContactMessageFileSize;
        TextView contentContactMessageFileSender;

        RelativeLayout contentContactMessageContactLayout;
        RelativeLayout contentContactMessageContactLayoutAvatar;
        RoundedImageView contentContactMessageContactThumb;
        TextView contentContactMessageContactName;
        public TextView contentContactMessageContactEmail;
        TextView contentContactMessageContactInitialLetter;
        TextView contentContactMessageContactSender;

        RelativeLayout ownManagementMessageLayout;
        TextView ownManagementMessageText;

        TextView contactManagementMessageText;
        RelativeLayout contactManagementMessageLayout;

        RelativeLayout contactMultiselectionLayout;
        ImageView contactMultiselectionImageView;
        ImageView contactMultiselectionTickIcon;

        RelativeLayout contactManagementMultiselectionLayout;
        ImageView contactManagementMultiselectionImageView;
        ImageView contactManagementMultiselectionTickIcon;

        RelativeLayout ownManagementMultiselectionLayout;
        ImageView ownManagementMultiselectionImageView;
        ImageView ownManagementMultiselectionTickIcon;

        public long getUserHandle (){
            return userHandle;
        }

        public int getCurrentPosition (){
            return currentPosition;
        }

        public void setMyImageView(Bitmap bitmap){
            contentOwnMessageContactThumb.setImageBitmap(bitmap);
            contentOwnMessageContactInitialLetter.setVisibility(View.GONE);
        }
        public void setContactImageView(Bitmap bitmap){
            contentContactMessageContactThumb.setImageBitmap(bitmap);
            contentContactMessageContactInitialLetter.setVisibility(View.GONE);
        }
    }
    ViewHolderMessageChat holder;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float density  = context.getResources().getDisplayMetrics().density;

        dbH = DatabaseHandler.getDbHandler(context);

        cC = new ChatController(context);

        View v = null;

        v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_chat, parent, false);
        holder = new ViewHolderMessageChat(v);
        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.message_chat_item_layout);
        holder.dateLayout = (RelativeLayout) v.findViewById(R.id.message_chat_date_layout);
        //Margins
        RelativeLayout.LayoutParams dateLayoutParams = (RelativeLayout.LayoutParams)holder.dateLayout.getLayoutParams();
        dateLayoutParams.setMargins(0, Util.scaleHeightPx(8, outMetrics), 0, Util.scaleHeightPx(8, outMetrics));
        holder.dateLayout.setLayoutParams(dateLayoutParams);

        holder.dateText = (TextView) v.findViewById(R.id.message_chat_date_text);

        //Own messages
        holder.ownMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_own_message_layout);
        holder.titleOwnMessage = (RelativeLayout) v.findViewById(R.id.title_own_message_layout);
//        holder.meText = (TextView) v.findViewById(R.id.message_chat_me_text);

        holder.timeOwnText = (TextView) v.findViewById(R.id.message_chat_time_text);
        //Margins
        RelativeLayout.LayoutParams timeOwnTextParams = (RelativeLayout.LayoutParams)holder.timeOwnText.getLayoutParams();
        timeOwnTextParams.setMargins(Util.scaleWidthPx(7, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
        holder.timeOwnText.setLayoutParams(timeOwnTextParams);

        holder.contentOwnMessageLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_layout);
        //Margins
        RelativeLayout.LayoutParams ownLayoutParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageLayout.getLayoutParams();
        ownLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(4, outMetrics));
        holder.contentOwnMessageLayout.setLayoutParams(ownLayoutParams);

        holder.contentOwnMessageText = (WrapTextView) v.findViewById(R.id.content_own_message_text);
        //Margins
        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageText.getLayoutParams();
        ownMessageParams.setMargins(Util.scaleWidthPx(43, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
        holder.contentOwnMessageText.setLayoutParams(ownMessageParams);

        holder.contentOwnMessageThumbLand = (ImageView)  v.findViewById(R.id.content_own_message_thumb_portrait);
        holder.contentOwnMessageThumbPort = (ImageView)  v.findViewById(R.id.content_own_message_thumb_landscape);

        holder.contentOwnMessageFileLayout = (RelativeLayout)  v.findViewById(R.id.content_own_message_file_layout);
        holder.contentOwnMessageFileThumb = (ImageView)  v.findViewById(R.id.content_own_message_file_thumb);
        holder.contentOwnMessageFileName = (TextView)  v.findViewById(R.id.content_own_message_file_name);
        holder.contentOwnMessageFileSize = (TextView)  v.findViewById(R.id.content_own_message_file_size);

        holder.contentOwnMessageContactLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_contact_layout);
        holder.contentOwnMessageContactLayoutAvatar = (RelativeLayout) v.findViewById(R.id.content_own_message_contact_layout_avatar);
        holder.contentOwnMessageContactThumb = (RoundedImageView) v.findViewById(R.id.content_own_message_contact_thumb);
        holder.contentOwnMessageContactName = (TextView)  v.findViewById(R.id.content_own_message_contact_name);
        holder.contentOwnMessageContactEmail = (TextView)  v.findViewById(R.id.content_own_message_contact_email);
        holder.contentOwnMessageContactInitialLetter = (TextView)  v.findViewById(R.id.content_own_message_contact_initial_letter);

        holder.retryAlert = (TextView) v.findViewById(R.id.not_sent_own_message_text);
        //Margins
        RelativeLayout.LayoutParams ownRetryAlertParams = (RelativeLayout.LayoutParams)holder.retryAlert.getLayoutParams();
        ownRetryAlertParams.setMargins(Util.scaleWidthPx(43, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
        holder.retryAlert.setLayoutParams(ownRetryAlertParams);

        holder.triangleIcon = (ImageView)  v.findViewById(R.id.own_triangle_icon);
        //Margins
        RelativeLayout.LayoutParams ownTriangleParams = (RelativeLayout.LayoutParams)holder.triangleIcon.getLayoutParams();
        ownTriangleParams.setMargins(Util.scaleWidthPx(43, outMetrics), 0, Util.scaleWidthPx(4, outMetrics), 0);
        holder.triangleIcon.setLayoutParams(ownTriangleParams);

        holder.ownManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.own_management_message_layout);
        //Margins
        RelativeLayout.LayoutParams ownManagementParams = (RelativeLayout.LayoutParams)holder.ownManagementMessageLayout.getLayoutParams();
        ownManagementParams.addRule(RelativeLayout.ALIGN_RIGHT);
        ownManagementParams.setMargins(0, 0, 0, Util.scaleHeightPx(4, outMetrics));
        holder.ownManagementMessageLayout.setLayoutParams(ownManagementParams);

        holder.ownManagementMessageText = (TextView) v.findViewById(R.id.own_management_message_text);
        RelativeLayout.LayoutParams ownManagementTextParams = (RelativeLayout.LayoutParams)holder.ownManagementMessageText.getLayoutParams();
        ownManagementTextParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(68, outMetrics), Util.scaleHeightPx(5, outMetrics));
        holder.ownManagementMessageText.setLayoutParams(ownManagementTextParams);

        holder.ownMultiselectionLayout = (RelativeLayout) v.findViewById(R.id.own_multiselection_layout);
        //Margins
        RelativeLayout.LayoutParams ownMultiselectionParams = (RelativeLayout.LayoutParams)holder.ownMultiselectionLayout.getLayoutParams();
        ownMultiselectionParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
        holder.ownMultiselectionLayout.setLayoutParams(ownMultiselectionParams);

        holder.ownMultiselectionImageView = (ImageView) v.findViewById(R.id.own_multiselection_image_view);
        holder.ownMultiselectionTickIcon = (ImageView) v.findViewById(R.id.own_multiselection_tick_icon);

        holder.ownManagementMultiselectionLayout = (RelativeLayout) v.findViewById(R.id.own_management_multiselection_layout);
        //Margins
        RelativeLayout.LayoutParams ownManagementMultiselectionParams = (RelativeLayout.LayoutParams)holder.ownManagementMultiselectionLayout.getLayoutParams();
        ownManagementMultiselectionParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
        holder.ownManagementMultiselectionLayout.setLayoutParams(ownManagementMultiselectionParams);

        holder.ownManagementMultiselectionImageView = (ImageView) v.findViewById(R.id.own_management_multiselection_image_view);
        holder.ownManagementMultiselectionTickIcon = (ImageView) v.findViewById(R.id.own_management_multiselection_tick_icon);

        //Contact messages////////////////////////////////////////
        holder.contactMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_contact_message_layout);
        holder.titleContactMessage = (RelativeLayout) v.findViewById(R.id.title_contact_message_layout);
//        holder.contactText = (TextView) v.findViewById(R.id.message_chat_contact_text);

        holder.timeContactText = (TextView) v.findViewById(R.id.contact_message_chat_time_text);
        //Margins
        RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)holder.timeContactText.getLayoutParams();
        timeContactTextParams.setMargins(Util.scaleWidthPx(75, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
        holder.timeContactText.setLayoutParams(timeContactTextParams);

        holder.contentContactMessageLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_layout);
        //Margins
        RelativeLayout.LayoutParams contactLayoutParams = (RelativeLayout.LayoutParams)holder.contentContactMessageLayout.getLayoutParams();
        contactLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(4, outMetrics));
        holder.contentContactMessageLayout.setLayoutParams(contactLayoutParams);

        holder.contentContactMessageText = (WrapTextView) v.findViewById(R.id.content_contact_message_text);
        //Margins
        RelativeLayout.LayoutParams contactMessageParams = (RelativeLayout.LayoutParams)holder.contentContactMessageText.getLayoutParams();
        contactMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
        holder.contentContactMessageText.setLayoutParams(contactMessageParams);

        holder.contentContactMessageThumbLand = (ImageView)  v.findViewById(R.id.content_contact_message_thumb_portrait);
        holder.contentContactMessageThumbPort = (ImageView)  v.findViewById(R.id.content_contact_message_thumb_landscape);

        holder.contentContactMessageFileLayout = (RelativeLayout)  v.findViewById(R.id.content_contact_message_file_layout);
        holder.contentContactMessageFileSender = (TextView)  v.findViewById(R.id.content_contact_message_file_sender);
        holder.contentContactMessageFileThumb = (ImageView)  v.findViewById(R.id.content_contact_message_file_thumb);
        holder.contentContactMessageFileName = (TextView)  v.findViewById(R.id.content_contact_message_file_name);
        holder.contentContactMessageFileSize = (TextView)  v.findViewById(R.id.content_contact_message_file_size);

        holder.contentContactMessageContactLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout);
        holder.contentContactMessageContactLayoutAvatar = (RelativeLayout) v.findViewById(R.id.content_contact_message_contact_layout_avatar);
        holder.contentContactMessageContactThumb = (RoundedImageView) v.findViewById(R.id.content_contact_message_contact_thumb);
        holder.contentContactMessageContactName = (TextView)  v.findViewById(R.id.content_contact_message_contact_name);
        holder.contentContactMessageContactEmail = (TextView)  v.findViewById(R.id.content_contact_message_contact_email);
        holder.contentContactMessageContactInitialLetter = (TextView)  v.findViewById(R.id.content_contact_message_contact_initial_letter);
        holder.contentContactMessageContactSender = (TextView)  v.findViewById(R.id.content_contact_message_contact_sender);

        holder.contactManagementMessageLayout = (RelativeLayout) v.findViewById(R.id.contact_management_message_layout);
        //Margins
        RelativeLayout.LayoutParams contactManagementParams = (RelativeLayout.LayoutParams)holder.contactManagementMessageLayout.getLayoutParams();
        contactManagementParams.addRule(RelativeLayout.ALIGN_LEFT);
        contactManagementParams.setMargins(0, 0, 0, Util.scaleHeightPx(4, outMetrics));
        holder.contactManagementMessageLayout.setLayoutParams(contactManagementParams);

        holder.contactManagementMessageText = (TextView) v.findViewById(R.id.contact_management_message_text);
        //Margins
        RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)holder.contactManagementMessageText.getLayoutParams();
        contactManagementTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(RIGHT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics));
        holder.contactManagementMessageText.setLayoutParams(contactManagementTextParams);

        holder.contactMultiselectionLayout = (RelativeLayout) v.findViewById(R.id.contact_multiselection_layout);
        //Margins
        RelativeLayout.LayoutParams contactMultiselectionParams = (RelativeLayout.LayoutParams)holder.contactMultiselectionLayout.getLayoutParams();
        contactMultiselectionParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
        holder.contactMultiselectionLayout.setLayoutParams(contactMultiselectionParams);

        holder.contactMultiselectionImageView = (ImageView) v.findViewById(R.id.contact_multiselection_image_view);
        holder.contactMultiselectionTickIcon = (ImageView) v.findViewById(R.id.contact_multiselection_tick_icon);

        holder.contactManagementMultiselectionLayout = (RelativeLayout) v.findViewById(R.id.contact_management_multiselection_layout);
        //Margins
        RelativeLayout.LayoutParams contactManagementMultiselectionParams = (RelativeLayout.LayoutParams)holder.contactManagementMultiselectionLayout.getLayoutParams();
        contactManagementMultiselectionParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
        holder.contactManagementMultiselectionLayout.setLayoutParams(contactManagementMultiselectionParams);

        holder.contactManagementMultiselectionImageView = (ImageView) v.findViewById(R.id.contact_management_multiselection_image_view);
        holder.contactManagementMultiselectionTickIcon = (ImageView) v.findViewById(R.id.contact_management_multiselection_tick_icon);

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        log("onBindViewHolder: " + position);

        ((ViewHolderMessageChat)holder).currentPosition = position;

        ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.GONE);
        ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.GONE);

        MegaChatMessage message = messages.get(position).getMessage();
        ((ViewHolderMessageChat)holder).userHandle = message.getUserHandle();

//        String myMail = ((ChatActivityLollipop) context).getMyMail();

        if(message.getType()==MegaChatMessage.TYPE_ALTER_PARTICIPANTS){
            log("ALTER PARTICIPANT MESSAGE!!");

            if(message.getHandleOfAction()==myUserHandle){
                log("me alter participant");

                if(messages.get(position).getInfoToShow()!=-1){
                    switch (messages.get(position).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.GONE);

                int privilege = message.getPrivilege();
                log("Privilege of me: "+privilege);
                String textToShow = "";
                String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                if(fullNameAction==null){
                    fullNameAction = "";
                }

                if(fullNameAction.trim().length()<=0){

                    log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    fullNameAction = "Unknown name";
                    if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                        log("3-Call for nonContactName: "+ message.getUserHandle());
                        ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                        megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                        megaChatApi.getUserLastname(message.getUserHandle(), listener);
                    }
                    else{
                        log("4-Name already asked and no name received: "+ message.getUserHandle());
                    }
                }

                if(privilege!=MegaChatRoom.PRIV_RM){
                    log("I was added");
                    textToShow = String.format(context.getString(R.string.message_add_participant), context.getString(R.string.chat_I_text), fullNameAction);
                }
                else{
                    log("I was removed or left");
                    if(message.getUserHandle()==message.getHandleOfAction()){
                        log("I left the chat");
                        textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), context.getString(R.string.chat_I_text));

                    }
                    else{
                        textToShow = String.format(context.getString(R.string.message_remove_participant), context.getString(R.string.chat_I_text), fullNameAction);
                    }
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(result);

                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);

                if (!multipleSelect) {
                    ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.GONE);
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.VISIBLE);
                    if(this.isItemChecked(position)){
                        log("Selected: "+message.getContent());
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                    }
                    else{
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }
            }
            else{
                log("CONTACT Message type ALTER PARTICIPANTS");

                int privilege = message.getPrivilege();
                log("Privilege of the user: "+privilege);

                if(messages.get(position).getInfoToShow()!=-1){
                    switch (messages.get(position).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);

                if (!multipleSelect) {
                    ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.GONE);

                    //Margins
                    RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                    contactManagementTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(RIGHT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics));
                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                    RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                    timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                    ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                    //Margins
                    RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                    timeContactTextParams.setMargins(Util.scaleWidthPx(MULTISELECTION_LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                    ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                    //Set more margins
                    RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                    contactManagementTextParams.setMargins(Util.scaleWidthPx(MULTISELECTION_LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(68, outMetrics), Util.scaleHeightPx(5, outMetrics));
                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                    if (this.isItemChecked(position)) {
                        log("Selected: " + message.getContent());
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                    } else {
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

                ((ViewHolderMessageChat)holder).fullNameTitle = cC.getFullName(message.getHandleOfAction(), chatRoom);


                if(((ViewHolderMessageChat)holder).fullNameTitle==null){
                    ((ViewHolderMessageChat)holder).fullNameTitle = "";
                }

                if(((ViewHolderMessageChat)holder).fullNameTitle.trim().length()<=0){

                    log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    ((ViewHolderMessageChat)holder).fullNameTitle = "Unknown name";
                    if(!(((ViewHolderMessageChat)holder).nameRequested)){
                        log("3-Call for nonContactName: "+ message.getUserHandle());

                        ((ViewHolderMessageChat)holder).nameRequested=true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getHandleOfAction());

                        megaChatApi.getUserFirstname(message.getHandleOfAction(), listener);
                        megaChatApi.getUserLastname(message.getHandleOfAction(), listener);
                    }
                    else{
                        log("4-Name already asked and no name received: "+ message.getUserHandle());
                    }
                }

                String textToShow = "";
                if(privilege!=MegaChatRoom.PRIV_RM){
                    log("Participant was added");
                    if(message.getUserHandle()==myUserHandle){
                        log("By me");
                        textToShow = String.format(context.getString(R.string.message_add_participant), ((ViewHolderMessageChat)holder).fullNameTitle, context.getString(R.string.chat_me_text));
                    }
                    else{
//                        textToShow = String.format(context.getString(R.string.message_add_participant), message.getHandleOfAction()+"");
                        log("By other");
                        String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                        if(fullNameAction==null){
                            fullNameAction = "";
                        }

                        if(fullNameAction.trim().length()<=0){

                            log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                            fullNameAction = "Unknown name";
                            if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                                log("3-Call for nonContactName: "+ message.getUserHandle());
                                ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                                megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                                megaChatApi.getUserLastname(message.getUserHandle(), listener);
                            }
                            else{
                                log("4-Name already asked and no name received: "+ message.getUserHandle());
                            }
                        }

                        textToShow = String.format(context.getString(R.string.message_add_participant), ((ViewHolderMessageChat)holder).fullNameTitle, fullNameAction);

                    }
                }//END participant was added
                else{
                    log("Participant was removed or left");
                    if(message.getUserHandle()==myUserHandle){
                        textToShow = String.format(context.getString(R.string.message_remove_participant), ((ViewHolderMessageChat)holder).fullNameTitle, context.getString(R.string.chat_me_text));
                    }
                    else{

                        if(message.getUserHandle()==message.getHandleOfAction()){
                            log("The participant left the chat");

                            textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), ((ViewHolderMessageChat)holder).fullNameTitle);

                        }
                        else{
                            log("The participant was removed");
                            String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                            if(fullNameAction==null){
                                fullNameAction = "";
                            }

                            if(fullNameAction.trim().length()<=0){

                                log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                                fullNameAction = "Unknown name";
                                if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                                    log("3-Call for nonContactName: "+ message.getUserHandle());
                                    ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                                    megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                                    megaChatApi.getUserLastname(message.getUserHandle(), listener);
                                }
                                else{
                                    log("4-Name already asked and no name received: "+ message.getUserHandle());
                                }
                            }

                            textToShow = String.format(context.getString(R.string.message_remove_participant), ((ViewHolderMessageChat)holder).fullNameTitle, fullNameAction);
                        }
//                        textToShow = String.format(context.getString(R.string.message_remove_participant), message.getHandleOfAction()+"");
                    }
                } //END participant removed

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);

            } //END CONTACT MANAGEMENT MESSAGE
        }
        else if(message.getType()==MegaChatMessage.TYPE_PRIV_CHANGE){
            log("PRIVILEGE CHANGE message");
            if(message.getHandleOfAction()==myUserHandle){
                log("a moderator change my privilege");
                int privilege = message.getPrivilege();
                log("Privilege of the user: "+privilege);

                if(messages.get(position).getInfoToShow()!=-1){
                    switch (messages.get(position).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.GONE);

                String privilegeString = "";
                if(privilege==MegaChatRoom.PRIV_MODERATOR){
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_STANDARD){
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_RO){
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                }
                else {
                    log("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";

                if(message.getUserHandle()==myUserHandle){
                    log("I changed my Own permission");
                    textToShow = String.format(context.getString(R.string.message_permissions_changed), context.getString(R.string.chat_I_text), privilegeString, context.getString(R.string.chat_me_text));
                }
                else{
                    log("I was change by someone");
                    String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                    if(fullNameAction==null){
                        fullNameAction = "";
                    }

                    if(fullNameAction.trim().length()<=0){

                        log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                        fullNameAction = "Unknown name";
                        if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                            log("3-Call for nonContactName: "+ message.getUserHandle());
                            ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                            megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                            megaChatApi.getUserLastname(message.getUserHandle(), listener);
                        }
                        else{
                            log("4-Name already asked and no name received: "+ message.getUserHandle());
                        }
                    }
                    textToShow = String.format(context.getString(R.string.message_permissions_changed), context.getString(R.string.chat_I_text), privilegeString, fullNameAction);
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(result);

                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);
                log("Visible own management message!");

                if (!multipleSelect) {
                    ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.GONE);

                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");

                    ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                    if (this.isItemChecked(position)) {
                        log("Selected: " + message.getContent());
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                    } else {
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }
            }
            else{
                log("Participant privilege change!");
                log("Message type PRIVILEGE CHANGE: "+message.getContent());

                if(messages.get(position).getInfoToShow()!=-1){
                    switch (messages.get(position).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.VISIBLE);

                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);

                if (!multipleSelect) {
                    ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.GONE);

                    //Margins
                    RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                    contactManagementTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(RIGHT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics));
                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                    RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                    timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                    ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                    if (positionClicked != -1){
                        if (positionClicked == position){
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                    //Margins
                    RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                    timeContactTextParams.setMargins(Util.scaleWidthPx(MULTISELECTION_LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                    ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                    //Set more margins
                    RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                    contactManagementTextParams.setMargins(Util.scaleWidthPx(MULTISELECTION_LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(68, outMetrics), Util.scaleHeightPx(5, outMetrics));
                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                    if (this.isItemChecked(position)) {
                        log("Selected: " + message.getContent());
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                    } else {
                        log("NOT selected");
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

                ((ViewHolderMessageChat)holder).fullNameTitle = cC.getFullName(message.getHandleOfAction(), chatRoom);


                if(((ViewHolderMessageChat)holder).fullNameTitle==null){
                    ((ViewHolderMessageChat)holder).fullNameTitle = "";
                }

                if(((ViewHolderMessageChat)holder).fullNameTitle.trim().length()<=0){

                    log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    ((ViewHolderMessageChat)holder).fullNameTitle = "Unknown name";
                    if(!(((ViewHolderMessageChat)holder).nameRequested)){
                        log("3-Call for nonContactName: "+ message.getUserHandle());

                        ((ViewHolderMessageChat)holder).nameRequested=true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getHandleOfAction());

                        megaChatApi.getUserFirstname(message.getHandleOfAction(), listener);
                        megaChatApi.getUserLastname(message.getHandleOfAction(), listener);
                    }
                    else{
                        log("4-Name already asked and no name received: "+ message.getUserHandle());
                    }
                }

                int privilege = message.getPrivilege();
                String privilegeString = "";
                if(privilege==MegaChatRoom.PRIV_MODERATOR){
                    privilegeString = context.getString(R.string.administrator_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_STANDARD){
                    privilegeString = context.getString(R.string.standard_permission_label_participants_panel);
                }
                else if(privilege==MegaChatRoom.PRIV_RO){
                    privilegeString = context.getString(R.string.observer_permission_label_participants_panel);
                }
                else {
                    log("Change to other");
                    privilegeString = "Unknow";
                }

                String textToShow = "";
                if(message.getUserHandle()==myUserHandle){
                    log("The privilege was change by me");
                    textToShow = String.format(context.getString(R.string.message_permissions_changed), ((ViewHolderMessageChat)holder).fullNameTitle, privilegeString, context.getString(R.string.chat_me_text));

                }
                else{
                    log("By other");
                    String fullNameAction = cC.getFullName(message.getUserHandle(), chatRoom);

                    if(fullNameAction==null){
                        fullNameAction = "";
                    }

                    if(fullNameAction.trim().length()<=0){

                        log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                        fullNameAction = "Unknown name";
                        if(!(((ViewHolderMessageChat)holder).nameRequestedAction)){
                            log("3-Call for nonContactName: "+ message.getUserHandle());
                            ((ViewHolderMessageChat)holder).nameRequestedAction=true;
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, message.getUserHandle());
                            megaChatApi.getUserFirstname(message.getUserHandle(), listener);
                            megaChatApi.getUserLastname(message.getUserHandle(), listener);
                        }
                        else{
                            log("4-Name already asked and no name received: "+ message.getUserHandle());
                        }
                    }

                    textToShow = String.format(context.getString(R.string.message_permissions_changed), ((ViewHolderMessageChat)holder).fullNameTitle, privilegeString, fullNameAction);
                }
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);
            }
        }
        else{
            //OTHER TYPE OF MESSAGES
            if(message.getUserHandle()==myUserHandle) {
                log("MY message!!");
                log("MY message handle!!: "+message.getMsgId());

                if(messages.get(position).getInfoToShow()!=-1){
                    switch (messages.get(position).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.VISIBLE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.GONE);

                if(message.getType()==MegaChatMessage.TYPE_NORMAL||message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                    log("Message type: "+message.getMsgId());

                    String messageContent = "";
                    if(message.getContent()!=null){
                        messageContent = message.getContent();
                    }

                    if(message.isEdited()){

                        log("Message is edited");
                        Spannable content = new SpannableString(messageContent);
                        int status = message.getStatus();
                        if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                            log("Show triangle retry!");
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.mail_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                        }
                        else if((status==MegaChatMessage.STATUS_SENDING)){
                            log("Status not received by server: "+message.getStatus());
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.mail_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        else{
                            log("Status: "+message.getStatus());
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                            content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(content+" ");

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        ((ViewHolderMessageChat)holder).contentOwnMessageText.append(edited);
                        ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.GONE);

                        if (!multipleSelect) {
                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(43, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);

                            ((ViewHolderMessageChat)holder).ownMultiselectionLayout.setVisibility(View.GONE);
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).ownMultiselectionLayout.setVisibility(View.VISIBLE);
                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);

                            if(this.isItemChecked(position)){
                                log("Selected: "+message.getContent());
                                ((ViewHolderMessageChat)holder).ownMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                                ((ViewHolderMessageChat)holder).ownMultiselectionTickIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).ownMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                                ((ViewHolderMessageChat)holder).ownMultiselectionTickIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                    else if(message.isDeleted()){
                        log("Message is deleted");

                        RelativeLayout.LayoutParams ownManagementParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).ownManagementMessageText.getLayoutParams();
                        ownManagementParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(68, outMetrics), Util.scaleHeightPx(5, outMetrics));
                        ((ViewHolderMessageChat)holder).ownManagementMessageText.setLayoutParams(ownManagementParams);

                        ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                        ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(context.getString(R.string.text_deleted_message));
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);

                        if (!multipleSelect) {
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.GONE);

                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                            if (this.isItemChecked(position)) {
                                log("Selected: " + message.getContent());
                                ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                                ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                            } else {
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                                ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                    else{

                        if(message.getType()==MegaChatMessage.TYPE_NORMAL){

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);

                            if(message.getContent()!=null){
                                messageContent = message.getContent();
                            }

                            int status = message.getStatus();

                            if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                                log("Show triangle retry!");
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                                ((ViewHolderMessageChat)holder).triangleIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
                            }
                            else if((status==MegaChatMessage.STATUS_SENDING)){
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                            }
                            else{
                                log("Status: "+message.getStatus());
                                ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                            }

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.VISIBLE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.VISIBLE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);

                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setLayoutParams(ownMessageParams);


                            MegaNodeList nodeList = message.getMegaNodeList();
                            if(nodeList != null){

                                if(nodeList.size()==1){
                                    MegaNode node = nodeList.get(0);
                                    log("Node Name: "+node.getName());
                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setText(node.getName());
                                    long nodeSize = node.getSize();
                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setText(Util.getSizeString(nodeSize));

                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                                }
                                else{
                                    long totalSize = 0;
                                    for(int i=0; i<nodeList.size(); i++){
                                        MegaNode temp = nodeList.get(i);
                                        log("Node Name: "+temp.getName());
                                        totalSize = totalSize + temp.getSize();
                                    }
                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setText(Util.getSizeString(totalSize));
                                    MegaNode node = nodeList.get(0);
                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                                    ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, nodeList.size(), nodeList.size()));
                                }
                            }
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT){
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            messageContent = "Attachment revoked";
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(messageContent);
                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentOwnMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setVisibility(View.VISIBLE);

                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentOwnMessageContactLayout.setLayoutParams(ownMessageParams);

                            long userCount  = message.getUsersCount();

                            if(userCount==1){
                                String name = "";
                                name = message.getUserName(0);
                                if(name.trim().isEmpty()){
                                    name = message.getUserEmail(0);
                                }
                                String email = message.getUserEmail(0);
                                log("Contact Name: "+name);

                                ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setText(name);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setText(email);

                                setUserAvatar(((ViewHolderMessageChat)holder), message);
                             }
                            else{
                                //Show default avatar with userCount
                                StringBuilder name = new StringBuilder("");
                                name.append(message.getUserName(0));
                                for(int i=1; i<userCount;i++){
                                    name.append(", "+message.getUserName(i));
                                }
                                log("Names of attached contacts: "+name);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactName.setText(name);

                                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int)userCount, userCount);
                                ((ViewHolderMessageChat)holder).contentOwnMessageContactEmail.setText(email);

                                createDefaultAvatar(((ViewHolderMessageChat)holder), null, email, true);
                            }
                        }

                        ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.GONE);

                        if (!multipleSelect) {

                            ((ViewHolderMessageChat)holder).ownMultiselectionLayout.setVisibility(View.GONE);

                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");

                            ((ViewHolderMessageChat)holder).ownMultiselectionLayout.setVisibility(View.VISIBLE);

                            if(this.isItemChecked(position)){
                                log("Selected: "+message.getContent());
                                ((ViewHolderMessageChat)holder).ownMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                                ((ViewHolderMessageChat)holder).ownMultiselectionTickIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).ownMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                                ((ViewHolderMessageChat)holder).ownMultiselectionTickIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_TRUNCATE){
                    log("Message type TRUNCATE");

                    RelativeLayout.LayoutParams ownManagementParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).ownManagementMessageText.getLayoutParams();
                    ownManagementParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(68, outMetrics), Util.scaleHeightPx(5, outMetrics));
                    ((ViewHolderMessageChat)holder).ownManagementMessageText.setLayoutParams(ownManagementParams);

                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                    ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(context.getString(R.string.history_cleared_message));
                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);

                    if (!multipleSelect) {
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.GONE);

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                        if (this.isItemChecked(position)) {
                            log("Selected: " + message.getContent());
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
                    log("Message type TITLE CHANGE: "+message.getContent());

                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.GONE);

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.change_title_messages), context.getString(R.string.chat_I_text), messageContent);

                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }

                    ((ViewHolderMessageChat)holder).ownManagementMessageText.setText(result);

                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.VISIBLE);

                    if (!multipleSelect) {
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.GONE);

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).ownManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                        if (this.isItemChecked(position)) {
                            log("Selected: " + message.getContent());
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                            ((ViewHolderMessageChat)holder).ownManagementMultiselectionTickIcon.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                }
                else{
                    log("Type message ERROR: "+message.getType());
                    ((ViewHolderMessageChat)holder).contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.tour_bar_red));

                    ((ViewHolderMessageChat)holder).contentOwnMessageText.setText(context.getString(R.string.error_message_unrecognizable));
                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).ownManagementMessageLayout.setVisibility(View.GONE);

                    //Margins
                    RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
                    ownMessageParams.setMargins(Util.scaleWidthPx(43, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                    ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);
                    ((ViewHolderMessageChat)holder).ownMultiselectionLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

//                log("Content: "+message.getContent());
                }
//                ((ViewHolderMessageChat)holder).retryAlert.setVisibility(View.VISIBLE);
            }
            else{
                long userHandle = message.getUserHandle();
                log("Contact message!!: "+userHandle);

                if(((ChatActivityLollipop) context).isGroup()){

                    ((ViewHolderMessageChat)holder).fullNameTitle = cC.getFullName(userHandle, chatRoom);

                    if(((ViewHolderMessageChat)holder).fullNameTitle==null){
                        ((ViewHolderMessageChat)holder).fullNameTitle = "";
                    }

                    if(((ViewHolderMessageChat)holder).fullNameTitle.trim().length()<=0){

                        log("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                        ((ViewHolderMessageChat)holder).fullNameTitle = "Unknown name";
                        if(!(((ViewHolderMessageChat)holder).nameRequested)){
                            log("3-Call for nonContactName: "+ message.getUserHandle());
                            ((ViewHolderMessageChat)holder).nameRequested=true;
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, ((ViewHolderMessageChat)holder), this, userHandle);
                            megaChatApi.getUserFirstname(userHandle, listener);
                            megaChatApi.getUserLastname(userHandle, listener);
                        }
                        else{
                            log("4-Name already asked and no name received: "+ message.getUserHandle());
                        }
                    }
                }
                else{
//                    ((ViewHolderMessageChat)holder).fullNameTitle = getContactFullName(((ViewHolderMessageChat)holder).userHandle);
                    ((ViewHolderMessageChat)holder).fullNameTitle = chatRoom.getTitle();
                }

                if(messages.get(position).getInfoToShow()!=-1){
                    switch (messages.get(position).getInfoToShow()){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).dateText.setText(TimeChatUtils.formatDate(message.getTimestamp(), TimeChatUtils.DATE_SHORT_FORMAT));
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            ((ViewHolderMessageChat)holder).dateLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                ((ViewHolderMessageChat)holder).ownMessageLayout.setVisibility(View.GONE);
                ((ViewHolderMessageChat)holder).contactMessageLayout.setVisibility(View.VISIBLE);

                if(message.getType()==MegaChatMessage.TYPE_NORMAL||message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT||message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                    if(((ChatActivityLollipop) context).isGroup()){

//                    name.setSpan(new RelativeSizeSpan(0.85f), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        String handleString = megaApi.userHandleToBase64(((ViewHolderMessageChat)holder).userHandle);
                        String color = megaApi.getUserAvatarColor(handleString);

                        if(message.getType()==MegaChatMessage.TYPE_NORMAL){
                            Spannable name = new SpannableString(((ViewHolderMessageChat)holder).fullNameTitle+"\n");
                            if(color!=null){
                                log("The color to set the avatar is "+color);
                                name.setSpan(new ForegroundColorSpan(Color.parseColor(color)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            else{
                                log("Default color to the avatar");
                                name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }

//                    name.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            ((ViewHolderMessageChat)holder).contentContactMessageText.setText(name);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.GONE);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.VISIBLE);
                            if(color!=null){
                                log("The color to set the avatar is "+color);
                                ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setTextColor(Color.parseColor(color));
                            }
                            else{
                                log("Default color to the avatar");
                                ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            }
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setText(((ViewHolderMessageChat)holder).fullNameTitle);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.VISIBLE);
                            if(color!=null){
                                log("The color to set the avatar is "+color);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setTextColor(Color.parseColor(color));
                            }
                            else{
                                log("Default color to the avatar");
                                ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            }
                            ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setText(((ViewHolderMessageChat)holder).fullNameTitle);
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).contentContactMessageFileSender.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contentContactMessageContactSender.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contentContactMessageText.setText("");
                    }

                    if(message.isEdited()){
                        log("Message is edited");
                        String messageContent = "";
                        if(message.getContent()!=null){
                            messageContent = message.getContent();
                        }

                        Spannable content = new SpannableString(messageContent);
                        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ((ViewHolderMessageChat)holder).contentContactMessageText.append(content+" ");
//                    ((ViewHolderMessageChat)holder).contentContactMessageText.setText(content);

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ((ViewHolderMessageChat)holder).contentContactMessageText.append(edited);
                        ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.GONE);

                        //Margins
                        RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                        timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                        ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                        if (!multipleSelect) {
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contactMultiselectionLayout.setVisibility(View.GONE);
                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contactMultiselectionLayout.setVisibility(View.VISIBLE);
                            if(this.isItemChecked(position)){
                                log("Selected: "+message.getContent());
                                ((ViewHolderMessageChat)holder).contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                                ((ViewHolderMessageChat)holder).contactMultiselectionTickIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                                ((ViewHolderMessageChat)holder).contactMultiselectionTickIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                    else if(message.isDeleted()){
                        log("Message is deleted");
                        ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);

                        //Margins
                        RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                        contactManagementTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(RIGHT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics));
                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                        RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                        timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                        ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                        if (!multipleSelect) {
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.GONE);

                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                            if (this.isItemChecked(position)) {
                                log("Selected: " + message.getContent());
                                ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                                ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                            } else {
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                                ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }

                        if(((ChatActivityLollipop) context).isGroup()){
                            String textToShow = String.format(context.getString(R.string.text_deleted_message_by), ((ViewHolderMessageChat)holder).fullNameTitle);
                            Spanned result = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                            } else {
                                result = Html.fromHtml(textToShow);
                            }
                            ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(context.getString(R.string.text_deleted_message));
                        }
                    }
                    else{
                        ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
                        ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.GONE);

                        String messageContent = "";
                        if(message.getType()==MegaChatMessage.TYPE_NORMAL){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);

                            if(message.getContent()!=null){
                                messageContent = message.getContent();
                            }
                            ((ViewHolderMessageChat)holder).contentContactMessageText.append(messageContent);

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));

                        }
                        else if(message.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.VISIBLE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.VISIBLE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);

                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageFileLayout.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setLayoutParams(ownMessageParams);

                            MegaNodeList nodeList = message.getMegaNodeList();
                            if(nodeList != null){
                                if(nodeList.size()==1){
                                    MegaNode node = nodeList.get(0);
                                    log("Node Name: "+node.getName());
                                    ((ViewHolderMessageChat)holder).contentContactMessageFileName.setText(node.getName());
                                    long nodeSize = node.getSize();
                                    ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setText(Util.getSizeString(nodeSize));

                                    ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                                }
                                else{
                                    long totalSize = 0;
                                    for(int i=0; i<nodeList.size(); i++){
                                        MegaNode temp = nodeList.get(i);
                                        log("Node Name: "+temp.getName());
                                        totalSize = totalSize + temp.getSize();
                                    }
                                    ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setText(Util.getSizeString(totalSize));
                                    MegaNode node = nodeList.get(0);
                                    ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                                    ((ViewHolderMessageChat)holder).contentContactMessageFileName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, nodeList.size(), nodeList.size()));
                                }
                            }
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.GONE);

                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageText.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentContactMessageText.setLayoutParams(ownMessageParams);

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                            messageContent = "Attachment revoked";
                            ((ViewHolderMessageChat)holder).contentContactMessageText.setText(messageContent);
                        }
                        else if(message.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){

                            ((ViewHolderMessageChat)holder).contentContactMessageText.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbLand.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageThumbPort.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageFileLayout.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileThumb.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileName.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contentContactMessageFileSize.setVisibility(View.GONE);

                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactThumb.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactName.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setVisibility(View.VISIBLE);

                            //Margins
                            RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentContactMessageContactLayout.getLayoutParams();
                            ownMessageParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(68, outMetrics), 0);
                            ((ViewHolderMessageChat)holder).contentContactMessageContactLayout.setLayoutParams(ownMessageParams);

                            long userCount  = message.getUsersCount();

                            if(userCount==1){
                                String name = "";
                                name = message.getUserName(0);
                                if(name.trim().isEmpty()){
                                    name = message.getUserEmail(0);
                                }
                                String email = message.getUserEmail(0);
                                log("Contact Name: "+name);

                                ((ViewHolderMessageChat)holder).contentContactMessageContactName.setText(name);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setText(email);

                                setUserAvatar(((ViewHolderMessageChat)holder), message);
                            }
                            else{
                                //Show default avatar with userCount
                                StringBuilder name = new StringBuilder("");
                                name.append(message.getUserName(0));
                                for(int i=1; i<userCount;i++){
                                    name.append(", "+message.getUserName(i));
                                }
                                log("Names of attached contacts: "+name);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactName.setText(name);

                                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int)userCount, userCount);
                                ((ViewHolderMessageChat)holder).contentContactMessageContactEmail.setText(email);

                                createDefaultAvatar(((ViewHolderMessageChat)holder), null, email, false);
                            }
                        }

                        //Margins
                        RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                        timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                        ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                        if (!multipleSelect) {
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contactMultiselectionLayout.setVisibility(View.GONE);
                            if (positionClicked != -1){
                                if (positionClicked == position){
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                    listFragment.smoothScrollToPosition(positionClicked);
                                }
                                else{
                                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                                }
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        } else {
                            log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contactMultiselectionLayout.setVisibility(View.VISIBLE);
                            if(this.isItemChecked(position)){
                                log("Selected: "+message.getContent());
                                ((ViewHolderMessageChat)holder).contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                                ((ViewHolderMessageChat)holder).contactMultiselectionTickIcon.setVisibility(View.VISIBLE);
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                            }
                            else{
                                log("NOT selected");
                                ((ViewHolderMessageChat)holder).contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                                ((ViewHolderMessageChat)holder).contactMultiselectionTickIcon.setVisibility(View.GONE);
                                ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_TRUNCATE){
                    log("Message type TRUNCATE");
                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);

                    //Margins
                    RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                    contactManagementTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(RIGHT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics));
                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                    RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                    timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                    ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                    if (!multipleSelect) {
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.GONE);

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                        if (this.isItemChecked(position)) {
                            log("Selected: " + message.getContent());
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                    if(((ChatActivityLollipop) context).isGroup()){
                        String textToShow = String.format(context.getString(R.string.history_cleared_by), ((ViewHolderMessageChat)holder).fullNameTitle);
                        Spanned result = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                        } else {
                            result = Html.fromHtml(textToShow);
                        }

                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);

                    }
                    else{
                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(context.getString(R.string.history_cleared_message));
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
                    log("Message type CHANGE TITLE "+message.getContent());

                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.GONE);

                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.VISIBLE);

                    if (!multipleSelect) {
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.GONE);

                        //Margins
                        RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                        contactManagementTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(RIGHT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics));
                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                        RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                        timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                        ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                        if (positionClicked != -1){
                            if (positionClicked == position){
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));
                                listFragment.smoothScrollToPosition(positionClicked);
                            }
                            else{
                                ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                            }
                        }
                        else{
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    } else {
                        log("Multiselect ON");
//            ((ViewHolderMessageChat)holder).imageButtonThreeDots.setVisibility(View.GONE);
                        ((ViewHolderMessageChat)holder).contactManagementMultiselectionLayout.setVisibility(View.VISIBLE);

                        //Margins
                        RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                        timeContactTextParams.setMargins(Util.scaleWidthPx(MULTISELECTION_LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                        ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                        //Set more margins
                        RelativeLayout.LayoutParams contactManagementTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contactManagementMessageText.getLayoutParams();
                        contactManagementTextParams.setMargins(Util.scaleWidthPx(MULTISELECTION_LEFT_MARGIN_CONTACT_MSG_MANAGEMENT, outMetrics), Util.scaleHeightPx(5, outMetrics), Util.scaleWidthPx(68, outMetrics), Util.scaleHeightPx(5, outMetrics));
                        ((ViewHolderMessageChat)holder).contactManagementMessageText.setLayoutParams(contactManagementTextParams);

                        if (this.isItemChecked(position)) {
                            log("Selected: " + message.getContent());
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.VISIBLE);
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_file_list_selected_row));

                        } else {
                            log("NOT selected");
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                            ((ViewHolderMessageChat)holder).contactManagementMultiselectionTickIcon.setVisibility(View.GONE);
                            ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.change_title_messages), ((ViewHolderMessageChat)holder).fullNameTitle, messageContent);

                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }

                    ((ViewHolderMessageChat)holder).contactManagementMessageText.setText(result);
                }
                else{
                    log("Type message ERROR: "+message.getType());

                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setVisibility(View.VISIBLE);
                    ((ViewHolderMessageChat)holder).contactManagementMessageLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageText.setTextColor(ContextCompat.getColor(context, R.color.tour_bar_red));
                    ((ViewHolderMessageChat)holder).contentContactMessageText.setText(context.getString(R.string.error_message_unrecognizable));

                    //Margins
                    RelativeLayout.LayoutParams timeContactTextParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).timeContactText.getLayoutParams();
                    timeContactTextParams.setMargins(Util.scaleWidthPx(LEFT_MARGIN_CONTACT_MSG_NORMAL, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
                    ((ViewHolderMessageChat)holder).timeContactText.setLayoutParams(timeContactTextParams);

                    ((ViewHolderMessageChat)holder).contactMultiselectionLayout.setVisibility(View.GONE);
                    ((ViewHolderMessageChat)holder).contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                }
            }
        }

        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)((ViewHolderMessageChat)holder).contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(-14, outMetrics), Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
//        ((ViewHolderMessageChat)holder).contentOwnMessageText.setLayoutParams(ownMessageParams);
    }

    public void setUserAvatar(ViewHolderMessageChat holder, MegaChatMessage message){
        log("setUserAvatar");

        String name = "";
        name = message.getUserName(0);
        if(name.trim().isEmpty()){
            name = message.getUserEmail(0);
        }
        String email = message.getUserEmail(0);
        log("Contact Name: "+name);
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(message.getUserHandle(0));

        ChatAttachmentAvatarListener listener;
        if(myUserHandle==message.getUserHandle()){
            createDefaultAvatar(holder, userHandleEncoded, name, true);
            listener = new ChatAttachmentAvatarListener(context, holder, this, true);
        }
        else{
            createDefaultAvatar(holder, userHandleEncoded, name, false);
            listener = new ChatAttachmentAvatarListener(context, holder, this, false);
        }

        File avatar = null;
        if (context.getExternalCacheDir() != null){
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), email + ".jpg");
        }
        else{
            avatar = new File(context.getCacheDir().getAbsolutePath(), email + ".jpg");
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

                    if(megaApi==null){
                        log("setUserAvatar: megaApi is Null in Offline mode");
                        return;
                    }

                    MegaUser contact = megaApi.getContact(email);

                    if(contact!=null){
                        if (context.getExternalCacheDir() != null){
                            megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
                        }
                        else{
                            megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
                        }
                    }
                    else{
                        log("Contact is NULL");
                    }
                }
                else{
                    if(myUserHandle==message.getUserHandle()){
                        holder.contentOwnMessageContactInitialLetter.setVisibility(View.GONE);
                        holder.contentOwnMessageContactThumb.setImageBitmap(bitmap);
                    }
                    else{
                        holder.contentContactMessageContactInitialLetter.setVisibility(View.GONE);
                        holder.contentContactMessageContactThumb.setImageBitmap(bitmap);
                    }
                }
            }
            else{
                if(megaApi==null){
                    log("setUserAvatar: megaApi is Null in Offline mode");
                    return;
                }

                MegaUser contact = megaApi.getContact(email);
                if(contact!=null){
                    if (context.getExternalCacheDir() != null){
                        megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
                    }
                    else{
                        megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
                    }
                }
                else{
                    log("Contact is NULL");
                }
            }
        }
        else{
            if(megaApi==null){
                log("setUserAvatar: megaApi is Null in Offline mode");
                return;
            }

            MegaUser contact = megaApi.getContact(email);
            if(contact!=null){
                if (context.getExternalCacheDir() != null){
                    megaApi.getUserAvatar(contact, context.getExternalCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
                }
                else{
                    megaApi.getUserAvatar(contact, context.getCacheDir().getAbsolutePath() + "/" + contact.getEmail() + ".jpg", listener);
                }
            }
            else{
                log("Contact is NULL");
            }
        }
    }

    public void createDefaultAvatar(ViewHolderMessageChat holder, String userHandle, String name, boolean isMyMsg){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);

        String color = megaApi.getUserAvatarColor(userHandle);
        if(userHandle!=null){
            if(color!=null){
                log("The color to set the avatar is "+color);
                p.setColor(Color.parseColor(color));
            }
            else{
                log("Default color to the avatar");
                p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
            }
        }
        else{
            p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
        }


        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        if(isMyMsg){
            holder.contentOwnMessageContactThumb.setImageBitmap(defaultAvatar);

            if (name != null){
                if (name.trim().length() > 0){
                    String firstLetter = name.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    holder.contentOwnMessageContactInitialLetter.setText(firstLetter);
                    holder.contentOwnMessageContactInitialLetter.setTextColor(Color.WHITE);
                    holder.contentOwnMessageContactInitialLetter.setVisibility(View.VISIBLE);
                }
            }
            holder.contentOwnMessageContactInitialLetter.setTextSize(24);
        }
        else{
            holder.contentContactMessageContactThumb.setImageBitmap(defaultAvatar);
            if (name != null){
                if (name.trim().length() > 0){
                    String firstLetter = name.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    holder.contentContactMessageContactInitialLetter.setText(firstLetter);
                    holder.contentContactMessageContactInitialLetter.setTextColor(Color.WHITE);
                    holder.contentContactMessageContactInitialLetter.setVisibility(View.VISIBLE);
                }
            }
            holder.contentContactMessageContactInitialLetter.setTextSize(24);
        }

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }


    public boolean isMultipleSelect() {
        log("isMultipleSelect");
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        log("setMultipleSelect");
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
            notifyDataSetChanged();
        }
        if(this.multipleSelect)
        {
            selectedItems = new SparseBooleanArray();
        }
    }

    public void toggleSelection(int pos) {
        log("toggleSelection");
        ViewHolderMessageChat view = (ViewHolderMessageChat) listFragment.findViewHolderForLayoutPosition(pos);

        if (selectedItems.get(pos, false)) {
            log("delete pos: "+pos);
            selectedItems.delete(pos);
            if(view!=null){
                log("Start animation");
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                MegaChatMessage message = messages.get(pos).getMessage();
//                String myMail = ((ChatActivityLollipop) context).getMyMail();
                if(message.getUserHandle()==myUserHandle) {
                    view.ownMultiselectionTickIcon.setVisibility(View.GONE);
                    view.contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                    view.ownMultiselectionLayout.startAnimation(flipAnimation);
                }
                else{
                    view.contactMultiselectionTickIcon.setVisibility(View.GONE);
                    view.contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                    view.contactMultiselectionLayout.startAnimation(flipAnimation);
                }
            }
        }
        else {
            log("PUT pos: "+pos);
            selectedItems.put(pos, true);
            if(view!=null){
                log("Start animation");
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                MegaChatMessage message = messages.get(pos).getMessage();
                String myMail = ((ChatActivityLollipop) context).getMyMail();
                if(message.getUserHandle()==myUserHandle) {
                    view.ownMultiselectionLayout.startAnimation(flipAnimation);
                }
                else{
                    view.contactMultiselectionLayout.startAnimation(flipAnimation);
                }
            }
        }
        notifyItemChanged(pos);
    }

    public void selectAll(){
        for (int i= 0; i<this.getItemCount();i++){
            if(!isItemChecked(i)){
                toggleSelection(i);
            }
        }
    }

    public void clearSelections() {
        log("clearSelection");
        if(selectedItems!=null){
            selectedItems.clear();
        }
//        notifyDataSetChanged();
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    /*
     * Get request at specified position
     */
    public AndroidMegaChatMessage getMessageAt(int position) {
        try {
            if (messages != null) {
                return messages.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    /*
     * Get list of all selected chats
     */
    public ArrayList<AndroidMegaChatMessage> getSelectedMessages() {
        ArrayList<AndroidMegaChatMessage> messages = new ArrayList<AndroidMegaChatMessage>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {

                AndroidMegaChatMessage m = getMessageAt(selectedItems.keyAt(i));
                if (m != null){
                    messages.add(m);
                }
            }
        }
        return messages;
    }

    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setPositionClicked(int p){
        log("setPositionClicked: "+p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setMessages (ArrayList<AndroidMegaChatMessage> messages){
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void modifyMessage(ArrayList<AndroidMegaChatMessage> messages, int position){
        this.messages = messages;

        if(messages.get(position).getMessage().isDeleted()){
            log("Deleted the position message");
        }
        notifyItemChanged(position);
    }

    public void appendMessage(ArrayList<AndroidMegaChatMessage> messages){
        this.messages = messages;
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessage(ArrayList<AndroidMegaChatMessage> messages, int position){
        this.messages = messages;
        notifyItemInserted(position);
    }

    public void removeMesage(int position, ArrayList<AndroidMegaChatMessage> messages){
        this.messages = messages;
        notifyItemRemoved(position);
    }

    public void loadPreviousMessage(ArrayList<AndroidMegaChatMessage> messages){
        this.messages = messages;
        notifyItemInserted(0);
    }

    public void loadPreviousMessages(ArrayList<AndroidMegaChatMessage> messages, int counter){
        log("loadPreviousMessages: "+counter);
        this.messages = messages;
        notifyItemRangeInserted(0, counter);
    }

    private static void log(String log) {
        Util.log("MegaChatLollipopAdapter", log);
    }
}
