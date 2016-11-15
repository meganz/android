package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.WrapTextView;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<MegaChatLollipopAdapter.ViewHolderMessageChatList> {

    Context context;
    int positionClicked;
    ArrayList<MegaChatMessage> messages;
    ArrayList<Integer> infoToShow;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    boolean multipleSelect;
    int type;
    private SparseBooleanArray selectedItems;

    DisplayMetrics outMetrics;
    DatabaseHandler dbH = null;

    public MegaChatLollipopAdapter(Context _context, ArrayList<MegaChatMessage> _messages, ArrayList<Integer> infoToShow, RecyclerView _listView) {
        log("new adapter");
        this.context = _context;
        this.messages = _messages;
        this.positionClicked = -1;
        this.infoToShow = infoToShow;

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
    }

    /*private view holder class*/
    public static class ViewHolderMessageChatList extends RecyclerView.ViewHolder {
        public ViewHolderMessageChatList(View view) {
            super(view);
        }

        int currentPosition;
        long userHandle;
        String fullNameTitle;
        String firstNameText;
        String lastNameText;
        boolean firstNameReceived;
        boolean lastNameReceived;

        RelativeLayout itemLayout;

        RelativeLayout dateLayout;
        TextView dateText;
        RelativeLayout ownMessageLayout;
        RelativeLayout titleOwnMessage;
        TextView meText;
        TextView timeOwnText;
        RelativeLayout contentOwnMessageLayout;
        TextView contentOwnMessageText;

        RelativeLayout ownMultiselectionLayout;
        ImageView ownMultiselectionImageView;
        ImageView ownMultiselectionTickIcon;

        RelativeLayout contactMessageLayout;
        RelativeLayout titleContactMessage;
        TextView contactText;
        TextView timeContactText;
        RelativeLayout contentContactMessageLayout;
        TextView contentContactMessageText;

        RelativeLayout ownManagementMessage;
        TextView ownManagementMessageText;

        TextView contactManagementMessageText;
        RelativeLayout contactManagementMessage;

        RelativeLayout contactMultiselectionLayout;
        ImageView contactMultiselectionImageView;
        ImageView contactMultiselectionTickIcon;

        public long getUserHandle (){
            return userHandle;
        }

        public void setFirstNameReceived (){
            this.firstNameReceived=true;
        }
        public void setLastNameReceived (){
            this.lastNameReceived=true;
        }

        public void setFirstNameText(String firstNameText){
            log("First name: "+firstNameText);
            this.firstNameText = firstNameText;
        }

        public void setLastNameText(String lastNameText){
            log("Last name: "+lastNameText);
            this.lastNameText=lastNameText;
        }
        public int getCurrentPosition (){
            return currentPosition;
        }

        public void setNameNonContact(){
            log("setNameNonContact: "+currentPosition);
            if(firstNameReceived && lastNameReceived) {
                log("Name and First Name received!");
                firstNameReceived= false;
                lastNameReceived = false;

                if(firstNameText!=null){
                    if (firstNameText.trim().length() <= 0){
                        fullNameTitle = lastNameText;
                    }
                    else{
                        fullNameTitle = firstNameText + " " + lastNameText;
                    }

                    if(fullNameTitle!=null){
                        if (fullNameTitle.trim().length() > 0){
                            this.contactText.setText(fullNameTitle);
                        }
                    }
                }

            }
        }
    }
    ViewHolderMessageChatList holder;

    @Override
    public ViewHolderMessageChatList onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCreateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float density  = context.getResources().getDisplayMetrics().density;

        float scaleW = Util.getScaleW(outMetrics, density);
        float scaleH = Util.getScaleH(outMetrics, density);

        dbH = DatabaseHandler.getDbHandler(context);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_chat, parent, false);
        holder = new ViewHolderMessageChatList(v);
        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.message_chat_item_layout);
        holder.dateLayout = (RelativeLayout) v.findViewById(R.id.message_chat_date_layout);
        //Margins
        RelativeLayout.LayoutParams dateLayoutParams = (RelativeLayout.LayoutParams)holder.dateLayout.getLayoutParams();
        dateLayoutParams.setMargins(0, Util.scaleHeightPx(12, outMetrics), 0, Util.scaleHeightPx(12, outMetrics));
        holder.dateLayout.setLayoutParams(dateLayoutParams);

        holder.dateText = (TextView) v.findViewById(R.id.message_chat_date_text);

        //Own messages
        holder.ownMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_own_message_layout);
        holder.titleOwnMessage = (RelativeLayout) v.findViewById(R.id.title_own_message_layout);
        holder.meText = (TextView) v.findViewById(R.id.message_chat_me_text);
        //Margins
        RelativeLayout.LayoutParams meTextParams = (RelativeLayout.LayoutParams)holder.meText.getLayoutParams();
        meTextParams.setMargins(Util.scaleWidthPx(7, outMetrics), 0, Util.scaleWidthPx(37, outMetrics), 0);
        holder.meText.setLayoutParams(meTextParams);

        holder.timeOwnText = (TextView) v.findViewById(R.id.message_chat_time_text);
        holder.contentOwnMessageLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_layout);
        //Margins
        RelativeLayout.LayoutParams ownLayoutParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageLayout.getLayoutParams();
        ownLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(16, outMetrics));
        holder.contentOwnMessageLayout.setLayoutParams(ownLayoutParams);

        holder.contentOwnMessageText = (WrapTextView) v.findViewById(R.id.content_own_message_text);
        //Margins
        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageText.getLayoutParams();
        ownMessageParams.setMargins(Util.scaleWidthPx(43, outMetrics), 0, Util.scaleWidthPx(62, outMetrics), 0);
        holder.contentOwnMessageText.setLayoutParams(ownMessageParams);

        holder.ownManagementMessage = (RelativeLayout) v.findViewById(R.id.own_deleted_message_layout);
        //Margins
        RelativeLayout.LayoutParams ownDeleteParams = (RelativeLayout.LayoutParams)holder.ownManagementMessage.getLayoutParams();
        ownDeleteParams.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(15, outMetrics));
        holder.ownManagementMessage.setLayoutParams(ownDeleteParams);

        holder.ownManagementMessageText = (TextView) v.findViewById(R.id.own_deleted_message_text);

        holder.ownMultiselectionLayout = (RelativeLayout) v.findViewById(R.id.own_multiselection_layout);
        //Margins
        RelativeLayout.LayoutParams ownMultiselectionParams = (RelativeLayout.LayoutParams)holder.ownMultiselectionLayout.getLayoutParams();
        ownMultiselectionParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
        holder.ownMultiselectionLayout.setLayoutParams(ownMultiselectionParams);

        holder.ownMultiselectionImageView = (ImageView) v.findViewById(R.id.own_multiselection_image_view);
        holder.ownMultiselectionTickIcon = (ImageView) v.findViewById(R.id.own_multiselection_tick_icon);

        //Contact messages////////////////////////////////////////
        holder.contactMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_contact_message_layout);
        holder.titleContactMessage = (RelativeLayout) v.findViewById(R.id.title_contact_message_layout);
        holder.contactText = (TextView) v.findViewById(R.id.message_chat_contact_text);
        //Margins
        RelativeLayout.LayoutParams contactTextParams = (RelativeLayout.LayoutParams)holder.contactText.getLayoutParams();
        contactTextParams.setMargins(Util.scaleWidthPx(37, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
        holder.contactText.setLayoutParams(contactTextParams);

        holder.timeContactText = (TextView) v.findViewById(R.id.contact_message_chat_time_text);
        holder.contentContactMessageLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_layout);
        //Margins
        RelativeLayout.LayoutParams contactLayoutParams = (RelativeLayout.LayoutParams)holder.contentContactMessageLayout.getLayoutParams();
        contactLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(16, outMetrics));
        holder.contentContactMessageLayout.setLayoutParams(contactLayoutParams);

        holder.contentContactMessageText = (WrapTextView) v.findViewById(R.id.content_contact_message_text);
        //Margins
        RelativeLayout.LayoutParams contactMessageParams = (RelativeLayout.LayoutParams)holder.contentContactMessageText.getLayoutParams();
        contactMessageParams.setMargins(Util.scaleWidthPx(62, outMetrics), 0, Util.scaleWidthPx(62, outMetrics), 0);
        holder.contentContactMessageText.setLayoutParams(contactMessageParams);

        holder.contactManagementMessage = (RelativeLayout) v.findViewById(R.id.contact_deleted_message_layout);
        //Margins
        RelativeLayout.LayoutParams contactDeleteParams = (RelativeLayout.LayoutParams)holder.contactManagementMessage.getLayoutParams();
        contactDeleteParams.setMargins(0, Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(15, outMetrics));
        holder.contactManagementMessage.setLayoutParams(contactDeleteParams);

        holder.contactManagementMessageText = (TextView) v.findViewById(R.id.contact_deleted_message_text);

        holder.contactMultiselectionLayout = (RelativeLayout) v.findViewById(R.id.contact_multiselection_layout);
        //Margins
        RelativeLayout.LayoutParams contactMultiselectionParams = (RelativeLayout.LayoutParams)holder.contactMultiselectionLayout.getLayoutParams();
        contactMultiselectionParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
        holder.contactMultiselectionLayout.setLayoutParams(contactMultiselectionParams);

        holder.contactMultiselectionImageView = (ImageView) v.findViewById(R.id.contact_multiselection_image_view);
        holder.contactMultiselectionTickIcon = (ImageView) v.findViewById(R.id.contact_multiselection_tick_icon);

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderMessageChatList holder, int position) {
        log("onBindViewHolder: "+position);
        holder.currentPosition = position;

        MegaChatMessage message = messages.get(position);
        holder.userHandle = message.getUserHandle();

//        String myMail = ((ChatActivityLollipop) context).getMyMail();

        if(message.getType()==MegaChatMessage.TYPE_ALTER_PARTICIPANTS){
            log("ALTER PARTICIPANT MESSAGE!!");

            if(message.getUserHandleOfAction()==megaApi.getMyUser().getHandle()){
                log("me alter participant");

                if(infoToShow!=null){
                    switch (infoToShow.get(position)){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            holder.dateLayout.setVisibility(View.VISIBLE);
                            holder.dateText.setText(TimeChatUtils.formatDate(message, TimeChatUtils.DATE_SHORT_FORMAT));
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                holder.ownMessageLayout.setVisibility(View.VISIBLE);
                holder.contactMessageLayout.setVisibility(View.GONE);

                int privilege = message.getPrivilege();
                log("Privilege of me: "+privilege);
                String textToShow = "";
                String fullNameAction = getParticipantFullName(message.getUserHandle());
                if(fullNameAction!=null){
                    if(fullNameAction.trim().length()<=0){
                        fullNameAction = "Participant left";
//                                holder.contactText.setText("Participant left");
//
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                                megaChatApi.getUserFirstname(holder.userHandle, listener);
//                                megaChatApi.getUserLastname(holder.userHandle, listener);

                    }
                }
                else{
                    fullNameAction = "Participant left";
//                            holder.contactText.setText("Participant left");

//                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                            megaChatApi.getUserFirstname(holder.userHandle, listener);
//                            megaChatApi.getUserLastname(holder.userHandle, listener);
                }
                if(privilege!=MegaChatRoom.PRIV_RM){
                    log("I was added");
                    textToShow = String.format(context.getString(R.string.message_add_participant), context.getString(R.string.chat_I_text), fullNameAction);
                }
                else{
                    log("I was removed or left");
                    if(message.getUserHandle()==message.getUserHandleOfAction()){
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

                holder.ownManagementMessageText.setText(result);

                holder.contentOwnMessageLayout.setVisibility(View.GONE);

                holder.ownManagementMessageText.setGravity(Gravity.RIGHT);
                holder.ownManagementMessage.setVisibility(View.VISIBLE);
                //Margins
                RelativeLayout.LayoutParams ownDeleteParams = (RelativeLayout.LayoutParams)holder.ownManagementMessage.getLayoutParams();
                ownDeleteParams.addRule(RelativeLayout.ALIGN_RIGHT);
                ownDeleteParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(15, outMetrics));
                holder.ownManagementMessage.setLayoutParams(ownDeleteParams);
            }
            else{
                log("CONTACT Message type ALTER PARTICIPANTS");

                int privilege = message.getPrivilege();
                log("Privilege of the user: "+privilege);

                if(infoToShow!=null){
                    switch (infoToShow.get(position)){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            holder.dateLayout.setVisibility(View.VISIBLE);
                            holder.dateText.setText(TimeChatUtils.formatDate(message, TimeChatUtils.DATE_SHORT_FORMAT));
                            holder.titleContactMessage.setVisibility(View.VISIBLE);
                            holder.timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleContactMessage.setVisibility(View.VISIBLE);
                            holder.timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                holder.ownMessageLayout.setVisibility(View.GONE);
                holder.contactMessageLayout.setVisibility(View.VISIBLE);

                holder.fullNameTitle = getParticipantFullName(message.getUserHandleOfAction());

                if(holder.fullNameTitle!=null){
                    if(holder.fullNameTitle.trim().length()<=0){
                        holder.contactText.setText("Participant left");
                        holder.fullNameTitle = "Participant left";
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, false);
                        megaChatApi.getUserFirstname(message.getUserHandleOfAction(), listener);
                        megaChatApi.getUserLastname(message.getUserHandleOfAction(), listener);
                    }
                    else{
                        holder.contactText.setText(holder.fullNameTitle);
                    }
                }
                else{
                    holder.contactText.setText("Participant left");
                    holder.fullNameTitle = "Participant left";

                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, false);
                    megaChatApi.getUserFirstname(message.getUserHandleOfAction(), listener);
                    megaChatApi.getUserLastname(message.getUserHandleOfAction(), listener);
                }

                String textToShow = "";
                if(privilege!=MegaChatRoom.PRIV_RM){
                    log("Participant was added");
                    if(message.getUserHandle()==megaApi.getMyUser().getHandle()){
                        log("By me");
                        textToShow = String.format(context.getString(R.string.message_add_participant), holder.fullNameTitle, context.getString(R.string.chat_me_text));
                    }
                    else{
//                        textToShow = String.format(context.getString(R.string.message_add_participant), message.getUserHandleOfAction()+"");
                        log("By other");
                        String fullNameAction = getParticipantFullName(message.getUserHandle());

                        if(fullNameAction!=null){
                            if(fullNameAction.trim().length()<=0){
                                fullNameAction = "Participant left";
//                                holder.contactText.setText("Participant left");
//
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                                megaChatApi.getUserFirstname(holder.userHandle, listener);
//                                megaChatApi.getUserLastname(holder.userHandle, listener);
                            }
                        }
                        else{
                            fullNameAction = "Participant left";
//                            holder.contactText.setText("Participant left");

//                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                            megaChatApi.getUserFirstname(holder.userHandle, listener);
//                            megaChatApi.getUserLastname(holder.userHandle, listener);
                        }
                        textToShow = String.format(context.getString(R.string.message_add_participant), holder.fullNameTitle, fullNameAction);

                    }
                }//END participant was added
                else{
                    log("Participant was removed or left");
                    if(message.getUserHandle()==megaApi.getMyUser().getHandle()){
                        textToShow = String.format(context.getString(R.string.message_remove_participant), holder.fullNameTitle, context.getString(R.string.chat_me_text));
                    }
                    else{

                        if(message.getUserHandle()==message.getUserHandleOfAction()){
                            log("The participant left the chat");

                            textToShow = String.format(context.getString(R.string.message_participant_left_group_chat), holder.fullNameTitle);

                        }
                        else{
                            log("The participant was removed");
                            String fullNameAction = getParticipantFullName(message.getUserHandle());

                            if(fullNameAction!=null){
                                if(fullNameAction.trim().length()<=0){
                                    fullNameAction = "Participant left";
//                                holder.contactText.setText("Participant left");
//
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                                megaChatApi.getUserFirstname(holder.userHandle, listener);
//                                megaChatApi.getUserLastname(holder.userHandle, listener);
                                }
                            }
                            else{
                                fullNameAction = "Participant left";
//                            holder.contactText.setText("Participant left");

//                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                            megaChatApi.getUserFirstname(holder.userHandle, listener);
//                            megaChatApi.getUserLastname(holder.userHandle, listener);

                            }
                            textToShow = String.format(context.getString(R.string.message_remove_participant), holder.fullNameTitle, fullNameAction);
                        }
//                        textToShow = String.format(context.getString(R.string.message_remove_participant), message.getUserHandleOfAction()+"");
                    }
                } //END participant removed

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                holder.contactManagementMessageText.setText(result);

                holder.contentContactMessageLayout.setVisibility(View.GONE);

                holder.contactManagementMessageText.setGravity(Gravity.LEFT);

                holder.contactManagementMessage.setVisibility(View.VISIBLE);
                //Margins
                RelativeLayout.LayoutParams contactDeleteParams = (RelativeLayout.LayoutParams)holder.contactManagementMessage.getLayoutParams();
                contactDeleteParams.addRule(RelativeLayout.ALIGN_LEFT);
                contactDeleteParams.setMargins(Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(15, outMetrics));
                holder.contactManagementMessage.setLayoutParams(contactDeleteParams);
            } //END CONTACT MANAGEMENT MESSAGE
        }
        else if(message.getType()==MegaChatMessage.TYPE_PRIV_CHANGE){
            log("PRIVILEGE CHANGE message");
            if(message.getUserHandleOfAction()==megaApi.getMyUser().getHandle()){
                log("my privilege change");
                int privilege = message.getPrivilege();
                log("Privilege of the user: "+privilege);

                if(infoToShow!=null){
                    switch (infoToShow.get(position)){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            holder.dateLayout.setVisibility(View.VISIBLE);
                            holder.dateText.setText(TimeChatUtils.formatDate(message, TimeChatUtils.DATE_SHORT_FORMAT));
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                holder.ownMessageLayout.setVisibility(View.VISIBLE);
                holder.contactMessageLayout.setVisibility(View.GONE);

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

                String fullNameAction = getParticipantFullName(message.getUserHandle());

                if(fullNameAction!=null){
                    if(fullNameAction.trim().length()<=0){
                        fullNameAction = "Participant left";
//                                holder.contactText.setText("Participant left");
//
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                                megaChatApi.getUserFirstname(holder.userHandle, listener);
//                                megaChatApi.getUserLastname(holder.userHandle, listener);
                    }
                }
                else{
                    fullNameAction = "Participant left";
//                            holder.contactText.setText("Participant left");

//                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                            megaChatApi.getUserFirstname(holder.userHandle, listener);
//                            megaChatApi.getUserLastname(holder.userHandle, listener);
                }
                String textToShow = String.format(context.getString(R.string.message_permissions_changed), privilegeString, fullNameAction);
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                holder.ownManagementMessageText.setText(result);

                holder.contentOwnMessageLayout.setVisibility(View.GONE);
                holder.ownManagementMessage.setVisibility(View.VISIBLE);

                holder.ownManagementMessageText.setGravity(Gravity.RIGHT);
                //Margins
                RelativeLayout.LayoutParams ownDeleteParams = (RelativeLayout.LayoutParams)holder.ownManagementMessage.getLayoutParams();
                ownDeleteParams.addRule(RelativeLayout.ALIGN_RIGHT);
                ownDeleteParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(15, outMetrics));
                holder.ownManagementMessage.setLayoutParams(ownDeleteParams);
            }
            else{
                log("Participant privilege change!");
                log("Message type PRIVILEGE CHANGE: "+message.getContent());

                if(infoToShow!=null){
                    switch (infoToShow.get(position)){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            holder.dateLayout.setVisibility(View.VISIBLE);
                            holder.dateText.setText(TimeChatUtils.formatDate(message, TimeChatUtils.DATE_SHORT_FORMAT));
                            holder.titleContactMessage.setVisibility(View.VISIBLE);
                            holder.timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleContactMessage.setVisibility(View.VISIBLE);
                            holder.timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                holder.ownMessageLayout.setVisibility(View.GONE);
                holder.contactMessageLayout.setVisibility(View.VISIBLE);

                holder.fullNameTitle = getParticipantFullName(message.getUserHandleOfAction());

                if(holder.fullNameTitle!=null){
                    if(holder.fullNameTitle.trim().length()<=0){
                        holder.contactText.setText("Participant left");
                        holder.fullNameTitle = "Participant left";
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, false);
                        megaChatApi.getUserFirstname(message.getUserHandleOfAction(), listener);
                        megaChatApi.getUserLastname(message.getUserHandleOfAction(), listener);
                    }
                    else{
                        holder.contactText.setText(holder.fullNameTitle);
                    }
                }
                else{
                    holder.contactText.setText("Participant left");
                    holder.fullNameTitle = "Participant left";
                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, false);
                    megaChatApi.getUserFirstname(message.getUserHandleOfAction(), listener);
                    megaChatApi.getUserLastname(message.getUserHandleOfAction(), listener);
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
                if(message.getUserHandle()==megaApi.getMyUser().getHandle()){
                    textToShow = String.format(context.getString(R.string.message_permissions_changed), privilegeString, context.getString(R.string.chat_me_text));
                    log("By me");
                }
                else{
                    log("By other");
                    String fullNameAction = getParticipantFullName(message.getUserHandle());

                    if(fullNameAction!=null){
                        if(fullNameAction.trim().length()<=0){
                            fullNameAction = "Participant left";
//                                holder.contactText.setText("Participant left");
//
//                                ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                                megaChatApi.getUserFirstname(holder.userHandle, listener);
//                                megaChatApi.getUserLastname(holder.userHandle, listener);
                        }
                    }
                    else{
                        fullNameAction = "Participant left";
//                            holder.contactText.setText("Participant left");

//                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this);
//                            megaChatApi.getUserFirstname(holder.userHandle, listener);
//                            megaChatApi.getUserLastname(holder.userHandle, listener);
                    }

                    textToShow = String.format(context.getString(R.string.message_permissions_changed), privilegeString, fullNameAction);
                }

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                holder.contactManagementMessageText.setText(result);

                holder.contentContactMessageLayout.setVisibility(View.GONE);
                holder.contactManagementMessageText.setGravity(Gravity.LEFT);
                holder.contactManagementMessage.setVisibility(View.VISIBLE);
                //Margins
                RelativeLayout.LayoutParams contactDeleteParams = (RelativeLayout.LayoutParams)holder.contactManagementMessage.getLayoutParams();
                contactDeleteParams.addRule(RelativeLayout.ALIGN_LEFT);
                contactDeleteParams.setMargins(Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(15, outMetrics));
                holder.contactManagementMessage.setLayoutParams(contactDeleteParams);
            }
        }
        else{
            //OTHER TYPE OF MESSAGES
            if(message.getUserHandle()==megaApi.getMyUser().getHandle()) {
                log("MY message!!:");
                log("MY message handle!!: "+message.getMsgId());
                if (!multipleSelect) {
                    holder.ownMultiselectionLayout.setVisibility(View.GONE);
//            holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
                    if (positionClicked != -1){
                        if (positionClicked == position){
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.file_list_selected_row));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
//            holder.imageButtonThreeDots.setVisibility(View.GONE);
                    holder.ownMultiselectionLayout.setVisibility(View.VISIBLE);
                    if(this.isItemChecked(position)){
                        log("Selected: "+message.getContent());
                        holder.ownMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                        holder.ownMultiselectionTickIcon.setVisibility(View.VISIBLE);
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.file_list_selected_row));
                    }
                    else{
                        log("NOT selected");
                        holder.ownMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                        holder.ownMultiselectionTickIcon.setVisibility(View.GONE);
                        holder.contentOwnMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

                if(infoToShow!=null){
                    switch (infoToShow.get(position)){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            holder.dateLayout.setVisibility(View.VISIBLE);
                            holder.dateText.setText(TimeChatUtils.formatDate(message, TimeChatUtils.DATE_SHORT_FORMAT));
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.VISIBLE);
                            holder.timeOwnText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleOwnMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }

                holder.ownMessageLayout.setVisibility(View.VISIBLE);
                holder.contactMessageLayout.setVisibility(View.GONE);

                if(message.getType()==MegaChatMessage.TYPE_NORMAL){
                    log("Message type NORMAL: "+message.getMsgId());

                    String messageContent = message.getContent();

                    if(message.isEdited()){
                        log("Message is edited");
                        Spannable content = new SpannableString(messageContent);
                        content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        holder.contentOwnMessageText.setText(content+" ");

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        holder.contentOwnMessageText.append(edited);
                        holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                        holder.ownManagementMessage.setVisibility(View.GONE);
                    }
                    else if(message.isDeleted()){
                        log("Message is deleted");
                        holder.contentOwnMessageLayout.setVisibility(View.GONE);
                        holder.ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                        holder.ownManagementMessageText.setText(context.getString(R.string.text_deleted_message));
                        holder.ownManagementMessage.setVisibility(View.VISIBLE);
                    }
                    else{
                        int status = message.getStatus();
                        if((status==MegaChatMessage.STATUS_SERVER_REJECTED)||(status==MegaChatMessage.STATUS_SENDING_MANUAL)){
                            log("Show triangle retry!");
                            holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                        }
                        else if(status==MegaChatMessage.STATUS_SENDING){
                            log("Sending message...");
                            holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.mail_my_account));
                        }
                        else{
                            holder.contentOwnMessageText.setTextColor(ContextCompat.getColor(context, R.color.name_my_account));
                        }
                        holder.contentOwnMessageText.setText(messageContent);
                        holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                        holder.ownManagementMessage.setVisibility(View.GONE);
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_TRUNCATE){
                    log("Message type TRUNCATE");
                    holder.contentOwnMessageLayout.setVisibility(View.GONE);
                    holder.ownManagementMessageText.setText(context.getString(R.string.text_cleared_history));
                    holder.ownManagementMessage.setVisibility(View.VISIBLE);
                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
                    log("Message type TITLE CHANGE: "+message.getContent());

                    holder.contentOwnMessageLayout.setVisibility(View.GONE);

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.change_title_messages), context.getString(R.string.chat_I_text), messageContent);

                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }

                    holder.ownManagementMessageText.setText(result);
                    holder.ownManagementMessageText.setGravity(Gravity.RIGHT);

                    holder.ownManagementMessage.setVisibility(View.VISIBLE);
                    //Margins
                    RelativeLayout.LayoutParams ownDeleteParams = (RelativeLayout.LayoutParams)holder.ownManagementMessage.getLayoutParams();
                    ownDeleteParams.addRule(RelativeLayout.ALIGN_RIGHT);
                    ownDeleteParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(15, outMetrics));
                    holder.ownManagementMessage.setLayoutParams(ownDeleteParams);
                }
                else{
                    log("Type message: "+message.getType());
//                log("Content: "+message.getContent());
                }
            }
            else{
                log("Contact message!!");
                long userHandle = message.getUserHandle();

                if(((ChatActivityLollipop) context).isGroup()){

                    holder.fullNameTitle = getParticipantFullName(userHandle);

                    if(holder.fullNameTitle!=null){
                        if(holder.fullNameTitle.trim().length()<=0){
                            holder.contactText.setText("Participant left");
                            holder.fullNameTitle = "Participant left";
                            ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, true);
                            megaChatApi.getUserFirstname(holder.userHandle, listener);
                            megaChatApi.getUserLastname(holder.userHandle, listener);
                        }
                        else{
                            holder.contactText.setText(holder.fullNameTitle);
                        }
                    }
                    else{
                        holder.contactText.setText("Participant left");
                        holder.fullNameTitle = "Participant left";
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, true);
                        megaChatApi.getUserFirstname(holder.userHandle, listener);
                        megaChatApi.getUserLastname(holder.userHandle, listener);
                    }
                }
                else{
                    holder.fullNameTitle = ("Hide name");
                    holder.contactText.setText("Hide name");
                }

                if (!multipleSelect) {
//            holder.imageButtonThreeDots.setVisibility(View.VISIBLE);
                    holder.contactMultiselectionLayout.setVisibility(View.GONE);
                    if (positionClicked != -1){
                        if (positionClicked == position){
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.file_list_selected_row));
                            listFragment.smoothScrollToPosition(positionClicked);
                        }
                        else{
                            holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                        }
                    }
                    else{
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                } else {
                    log("Multiselect ON");
//            holder.imageButtonThreeDots.setVisibility(View.GONE);
                    holder.contactMultiselectionLayout.setVisibility(View.VISIBLE);
                    if(this.isItemChecked(position)){
                        log("Selected: "+message.getContent());
                        holder.contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_filled));
                        holder.contactMultiselectionTickIcon.setVisibility(View.VISIBLE);
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.file_list_selected_row));
                    }
                    else{
                        log("NOT selected");
                        holder.contactMultiselectionImageView.setImageDrawable(context.getDrawable(R.drawable.message_multiselection_empty));
                        holder.contactMultiselectionTickIcon.setVisibility(View.GONE);
                        holder.contentContactMessageLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                    }
                }

                if(infoToShow!=null){
                    switch (infoToShow.get(position)){
                        case Constants.CHAT_ADAPTER_SHOW_ALL:{
                            holder.dateLayout.setVisibility(View.VISIBLE);
                            holder.dateText.setText(TimeChatUtils.formatDate(message, TimeChatUtils.DATE_SHORT_FORMAT));
                            holder.titleContactMessage.setVisibility(View.VISIBLE);
                            holder.timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_TIME:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleContactMessage.setVisibility(View.VISIBLE);
                            holder.timeContactText.setText(TimeChatUtils.formatTime(message));
                            break;
                        }
                        case Constants.CHAT_ADAPTER_SHOW_NOTHING:{
                            holder.dateLayout.setVisibility(View.GONE);
                            holder.titleContactMessage.setVisibility(View.GONE);
                            break;
                        }
                    }
                }
                holder.ownMessageLayout.setVisibility(View.GONE);
                holder.contactMessageLayout.setVisibility(View.VISIBLE);

                if(message.getType()==MegaChatMessage.TYPE_NORMAL){

                    Spannable name = new SpannableString(holder.fullNameTitle+"\n");
//                    name.setSpan(new RelativeSizeSpan(0.85f), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    name.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    name.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    holder.contentContactMessageText.setText(name);
//                    String messageContent = ;

                    if(message.isEdited()){
                        log("Message is edited");
                        Spannable content = new SpannableString(message.getContent());
                        content.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        holder.contentContactMessageText.append(content+" ");
//                    holder.contentContactMessageText.setText(content);

                        Spannable edited = new SpannableString(context.getString(R.string.edited_message_text));
                        edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        holder.contentContactMessageText.append(edited);
                        holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
                        holder.contactManagementMessage.setVisibility(View.GONE);
                    }
                    else if(message.isDeleted()){
                        log("Message is deleted");
                        holder.contentContactMessageLayout.setVisibility(View.GONE);
                        holder.ownManagementMessageText.setTextColor(ContextCompat.getColor(context, R.color.accentColor));
                        holder.contactManagementMessageText.setText(context.getString(R.string.text_deleted_message));
                        holder.contactManagementMessage.setVisibility(View.VISIBLE);
                    }
                    else{
                        holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
                        holder.contactManagementMessage.setVisibility(View.GONE);
                        holder.contentContactMessageText.append(message.getContent());
                    }
                }
                else if(message.getType()==MegaChatMessage.TYPE_TRUNCATE){
                    log("Message type TRUNCATE");
                    message.getUserHandleOfAction();
                    //Check if me
                    holder.contentContactMessageLayout.setVisibility(View.GONE);
                    holder.contactManagementMessageText.setText(context.getString(R.string.text_cleared_history));
                    holder.contactManagementMessage.setVisibility(View.VISIBLE);
                }
                else if(message.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
                    log("Message type CHANGE TITLE "+message.getContent());

                    holder.contentContactMessageLayout.setVisibility(View.GONE);

                    String messageContent = message.getContent();

                    String textToShow = String.format(context.getString(R.string.change_title_messages), holder.fullNameTitle, messageContent);

                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }

                    holder.contactManagementMessageText.setText(result);
                    holder.contactManagementMessageText.setGravity(Gravity.LEFT);

                    holder.contactManagementMessage.setVisibility(View.VISIBLE);
                    //Margins
                    RelativeLayout.LayoutParams contactDeleteParams = (RelativeLayout.LayoutParams)holder.contactManagementMessage.getLayoutParams();
                    contactDeleteParams.addRule(RelativeLayout.ALIGN_LEFT);
                    contactDeleteParams.setMargins(Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(64, outMetrics), Util.scaleHeightPx(15, outMetrics));
                    holder.contactManagementMessage.setLayoutParams(contactDeleteParams);
                }
                else{
                    log("Type message: "+message.getType());
                    log("Content: "+message.getContent());
                }
            }
        }



        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(-14, outMetrics), Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
//        holder.contentOwnMessageText.setLayoutParams(ownMessageParams);
    }

    public String getParticipantFullName(long userHandle){
        String participantFirstName = ((ChatActivityLollipop) context).getParticipantFirstName(userHandle);
        String participantLastName = ((ChatActivityLollipop) context).getParticipantLastName(userHandle);

        if(participantFirstName==null){
            participantFirstName="";
        }
        if(participantLastName == null){
            participantLastName="";
        }

        if (participantFirstName.trim().length() <= 0){
            log("Participant1: "+participantFirstName);
            return participantLastName;
        }
        else{
            log("Participant2: "+participantLastName);
            return participantFirstName + " " + participantLastName;
        }
    }

    @Override
    public int getItemCount() {
        return infoToShow.size();
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
        ViewHolderMessageChatList view = (ViewHolderMessageChatList) listFragment.findViewHolderForLayoutPosition(pos);

        if (selectedItems.get(pos, false)) {
            log("delete pos: "+pos);
            selectedItems.delete(pos);
            if(view!=null){
                log("Start animation: "+view.meText.getText());
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                MegaChatMessage message = messages.get(pos);
//                String myMail = ((ChatActivityLollipop) context).getMyMail();
                if(message.getUserHandle()==megaApi.getMyUser().getHandle()) {
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
                log("Start animation: "+view.meText.getText());
                Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
                MegaChatMessage message = messages.get(pos);
                String myMail = ((ChatActivityLollipop) context).getMyMail();
                if(message.getUserHandle()==megaApi.getMyUser().getHandle()) {
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
    public MegaChatMessage getMessageAt(int position) {
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
    public ArrayList<MegaChatMessage> getSelectedMessages() {
        ArrayList<MegaChatMessage> messages = new ArrayList<MegaChatMessage>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {
                MegaChatMessage m = getMessageAt(selectedItems.keyAt(i));
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


    public void setPositionClicked(int p){
        log("setPositionClicked: "+p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setMessages (ArrayList<MegaChatMessage> messages, ArrayList<Integer> infoToShow){
        this.messages = messages;
        this.infoToShow = infoToShow;
        notifyDataSetChanged();
    }

    public void modifyMessage(ArrayList<MegaChatMessage> messages, ArrayList<Integer> infoToShow, int position){
        this.messages = messages;
        this.infoToShow = infoToShow;
        if(messages.get(position).isDeleted()){
            log("Deleted the position message");
        }
        notifyItemChanged(position);
    }

    public void appendMessage(ArrayList<MegaChatMessage> messages, ArrayList<Integer> infoToShow){
        this.messages = messages;
        this.infoToShow = infoToShow;
        notifyItemInserted(messages.size() - 1);
    }

    public void loadPreviousMessage(ArrayList<MegaChatMessage> messages, ArrayList<Integer> infoToShow){
        this.messages = messages;
        this.infoToShow = infoToShow;
        notifyItemInserted(0);
    }

    public void loadPreviousMessages(ArrayList<MegaChatMessage> messages, ArrayList<Integer> infoToShow, int counter){
        log("loadPreviousMessages: "+counter);
        this.messages = messages;
        this.infoToShow = infoToShow;
        notifyItemRangeInserted(0, counter);
    }

    private static void log(String log) {
        Util.log("MegaChatLollipopAdapter", log);
    }
}
