package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
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

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.WrapTextView;
import mega.privacy.android.app.lollipop.ChatActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatMessage;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<MegaChatLollipopAdapter.ViewHolderMessageChatList> {

    Context context;
    int positionClicked;
    ArrayList<MegaChatMessage> messages;
    ArrayList<Integer> infoToShow;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
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
    public class ViewHolderMessageChatList extends RecyclerView.ViewHolder {
        public ViewHolderMessageChatList(View view) {
            super(view);
        }

        int currentPosition;

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

        RelativeLayout ownDeletedMessage;
        RelativeLayout contactDeletedMessage;

        RelativeLayout contactMultiselectionLayout;
        ImageView contactMultiselectionImageView;
        ImageView contactMultiselectionTickIcon;
    }
    ViewHolderMessageChatList holder;

    @Override
    public ViewHolderMessageChatList onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCReateViewHolder");

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

        holder.ownMultiselectionLayout = (RelativeLayout) v.findViewById(R.id.own_multiselection_layout);
        //Margins
        RelativeLayout.LayoutParams ownMultiselectionParams = (RelativeLayout.LayoutParams)holder.ownMultiselectionLayout.getLayoutParams();
        ownMultiselectionParams.setMargins(Util.scaleWidthPx(20, outMetrics), 0, 0, 0);
        holder.ownMultiselectionLayout.setLayoutParams(ownMultiselectionParams);

        holder.ownMultiselectionImageView = (ImageView) v.findViewById(R.id.own_multiselection_image_view);
        holder.ownMultiselectionTickIcon = (ImageView) v.findViewById(R.id.own_multiselection_tick_icon);

        holder.contactMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_contact_message_layout);
        holder.titleContactMessage = (RelativeLayout) v.findViewById(R.id.title_contact_message_layout);
        holder.contactText = (TextView) v.findViewById(R.id.message_chat_contact_text);
        holder.contactText.setText(((ChatActivityLollipop) context).getShortContactName());
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

        holder.ownDeletedMessage = (RelativeLayout) v.findViewById(R.id.own_deleted_message_layout);
        holder.contactDeletedMessage = (RelativeLayout) v.findViewById(R.id.contact_deleted_message_layout);

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
        log("onBindViewHolder");
        holder.currentPosition = position;
        MegaChatMessage message = messages.get(position);

//        String myMail = ((ChatActivityLollipop) context).getMyMail();
        if(message.getUserHandle()==megaApi.getMyUser().getHandle()) {
            log("MY message!!");
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
            String messageContent = message.getContent();

            if(message.isEdited()){
                log("Message is edited");
                Spannable content = new SpannableString(messageContent);
                content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.contentOwnMessageText.setText(messageContent);
                Spannable edited = new SpannableString(" (edited)");
                edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                holder.contentOwnMessageText.append(edited);
                holder.contentOwnMessageLayout.setVisibility(View.VISIBLE);
                holder.ownDeletedMessage.setVisibility(View.GONE);
            }
            else if(message.isDeleted()){
                log("Message is deleted");
                holder.contentOwnMessageLayout.setVisibility(View.GONE);
                holder.ownDeletedMessage.setVisibility(View.VISIBLE);
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
                holder.ownDeletedMessage.setVisibility(View.GONE);
            }
        }
        else{
            log("Contact message!!");

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

            String messageContent = message.getContent();
            if(message.isEdited()){
                log("Message is edited");
                Spannable content = new SpannableString(messageContent);
                content.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.name_my_account)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.contentContactMessageText.setText(messageContent);
                Spannable edited = new SpannableString(" (edited)");
                edited.setSpan(new RelativeSizeSpan(0.85f), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.accentColor)), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                edited.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, edited.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                holder.contentContactMessageText.append(edited);
                holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
                holder.contactDeletedMessage.setVisibility(View.GONE);
            }
            else if(message.isDeleted()){
                log("Message is deleted");
                holder.contentContactMessageLayout.setVisibility(View.GONE);
                holder.contactDeletedMessage.setVisibility(View.VISIBLE);
            }
            else{
                holder.contentContactMessageLayout.setVisibility(View.VISIBLE);
                holder.contactDeletedMessage.setVisibility(View.GONE);
                holder.contentContactMessageText.setText(message.getContent());
            }

        }

        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(-14, outMetrics), Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
//        holder.contentOwnMessageText.setLayoutParams(ownMessageParams);
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
    public List<MegaChatMessage> getSelectedMessages() {
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

    private static void log(String log) {
        Util.log("MegaChatLollipopAdapter", log);
    }
}
