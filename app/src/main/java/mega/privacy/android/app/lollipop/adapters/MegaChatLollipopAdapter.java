package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.tempMegaChatClasses.Message;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<MegaChatLollipopAdapter.ViewHolderMessageChatList> implements View.OnClickListener {

    Context context;
    int positionClicked;
    ArrayList<Message> messages;
    ArrayList<Integer> infoToShow;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
    boolean multipleSelect;
    int type;
    private SparseBooleanArray selectedItems;

    DisplayMetrics outMetrics;
    DatabaseHandler dbH = null;

    public MegaChatLollipopAdapter(Context _context, ArrayList<Message> _messages, ArrayList<Integer> infoToShow, RecyclerView _listView) {
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
        public ViewHolderMessageChatList(View arg0) {
            super(arg0);
        }

        int currentPosition;
        RelativeLayout dateLayout;
        TextView dateText;
        RelativeLayout ownMessageLayout;
        RelativeLayout titleOwnMessage;
        TextView meText;
        TextView timeOwnText;
        RelativeLayout contentOwnMessageLayout;
        TextView contentOwnMessageText;

        RelativeLayout contactMessageLayout;
        RelativeLayout titleContactMessage;
        TextView contactText;
        TextView timeContactText;
        RelativeLayout contentContactMessageLayout;
        TextView contentContactMessageText;

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
        holder.dateLayout = (RelativeLayout) v.findViewById(R.id.message_chat_date_layout);
        //Margins
        RelativeLayout.LayoutParams dateLayoutParams = (RelativeLayout.LayoutParams)holder.dateLayout.getLayoutParams();
        dateLayoutParams.setMargins(0, Util.scaleHeightPx(12, outMetrics), 0, Util.scaleHeightPx(12, outMetrics));
        holder.dateLayout.setLayoutParams(dateLayoutParams);

        holder.dateText = (TextView) v.findViewById(R.id.message_chat_date_text);

        holder.ownMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_own_message_layout);
        holder.ownMessageLayout.setOnClickListener(this);
        holder.titleOwnMessage = (RelativeLayout) v.findViewById(R.id.title_own_message_layout);
        holder.meText = (TextView) v.findViewById(R.id.message_chat_me_text);
        //Margins
        RelativeLayout.LayoutParams meTextParams = (RelativeLayout.LayoutParams)holder.meText.getLayoutParams();
        meTextParams.setMargins(Util.scaleWidthPx(7, outMetrics), 0, Util.scaleWidthPx(37, outMetrics), 0);
        holder.meText.setLayoutParams(meTextParams);

        holder.timeOwnText = (TextView) v.findViewById(R.id.message_chat_time_text);
        holder.contentOwnMessageLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_layout);
        holder.contentOwnMessageText = (TextView) v.findViewById(R.id.content_own_message_text);
        //Margins
        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageText.getLayoutParams();
        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), 0, Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
        holder.contentOwnMessageText.setLayoutParams(ownMessageParams);

        holder.contactMessageLayout = (RelativeLayout) v.findViewById(R.id.message_chat_contact_message_layout);
        holder.contactMessageLayout.setOnClickListener(this);
        holder.titleContactMessage = (RelativeLayout) v.findViewById(R.id.title_contact_message_layout);
        holder.contactText = (TextView) v.findViewById(R.id.message_chat_contact_text);
        holder.contactText.setText(((ChatActivityLollipop) context).getShortContactName());
        //Margins
        RelativeLayout.LayoutParams contactTextParams = (RelativeLayout.LayoutParams)holder.contactText.getLayoutParams();
        contactTextParams.setMargins(Util.scaleWidthPx(37, outMetrics), 0, Util.scaleWidthPx(7, outMetrics), 0);
        holder.contactText.setLayoutParams(contactTextParams);

        holder.timeContactText = (TextView) v.findViewById(R.id.contact_message_chat_time_text);
        holder.contentContactMessageLayout = (RelativeLayout) v.findViewById(R.id.content_contact_message_layout);
        holder.contentContactMessageText = (TextView) v.findViewById(R.id.content_contact_message_text);
        //Margins
        RelativeLayout.LayoutParams contactMessageParams = (RelativeLayout.LayoutParams)holder.contentContactMessageText.getLayoutParams();
        contactMessageParams.setMargins(Util.scaleWidthPx(62, outMetrics), 0, Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(16, outMetrics));
        holder.contentContactMessageText.setLayoutParams(contactMessageParams);

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderMessageChatList holder, int position) {
        log("onBindViewHolder");
        holder.currentPosition = position;
        Message message = messages.get(position);

        String myMail = ((ChatActivityLollipop) context).getMyMail();
        if(message.getUser().getMail().equals(myMail)) {
            log("MY message!!");

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
            holder.contentOwnMessageText.setText(message.getMessage());

            holder.contactMessageLayout.setVisibility(View.GONE);
        }
        else{
            log("Contact message!!");

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
            holder.contentContactMessageText.setText(message.getMessage());
        }

        //Check the next message to know the margin bottom the content message
        //        Message nextMessage = messages.get(position+1);
        //Margins
//        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageText.getLayoutParams();
//        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), Util.scaleHeightPx(-14, outMetrics), Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
//        holder.contentOwnMessageText.setLayoutParams(ownMessageParams);
    }
//
//
//
//    public int compareDate(Message lastMessage){
//
//        if(lastDate!=null){
//            Calendar cal = Util.calculateDateFromTimestamp(lastMessage.getDate());
//
//            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.DATE);
//
//            int result = tc.compare(cal, lastDate);
//            log("RESULTS: "+result);
//            return result;
//        }
//        else{
//            log("return -1");
//            return -1;
//        }
//    }
//
//    public int compareTime(Message lastMessage){
//
//        if(lastDate!=null){
//            Calendar cal = Util.calculateDateFromTimestamp(lastMessage.getDate());
//
//            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.TIME);
//
//            int result = tc.compare(cal, lastDate);
//            log("RESULTS: "+result);
//            return result;
//        }
//        else{
//            log("return -1");
//            return -1;
//        }
//    }
//
//    public void storeDate(Message lastMessage){
//        lastDate = Util.calculateDateFromTimestamp(lastMessage.getDate());
//    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public void onClick(View v) {

    }

    public void setPositionClicked(int p){
        log("setPositionClicked: "+p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    private static void log(String log) {
        Util.log("MegaChatLollipopAdapter", log);
    }
}
