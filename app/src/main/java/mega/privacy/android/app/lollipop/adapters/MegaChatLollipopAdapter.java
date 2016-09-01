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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.tempMegaChatClasses.Message;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class MegaChatLollipopAdapter extends RecyclerView.Adapter<MegaChatLollipopAdapter.ViewHolderMessageChatList> implements View.OnClickListener {

    Context context;
    int positionClicked;
    ArrayList<Message> messages;
    RecyclerView listFragment;
    MegaApiAndroid megaApi;
    boolean multipleSelect;
    int type;
    private SparseBooleanArray selectedItems;

    DisplayMetrics outMetrics;
    DatabaseHandler dbH = null;

    public MegaChatLollipopAdapter(Context _context, ArrayList<Message> _messages, RecyclerView _listView) {
        log("new adapter");
        this.context = _context;
        this.messages = _messages;
        this.positionClicked = -1;

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
        TextView timeText;
        RelativeLayout contentMessageLayout;
        TextView contentOwnMessageText;

    }
    ViewHolderMessageChatList holder;

    @Override
    public ViewHolderMessageChatList onCreateViewHolder(ViewGroup parent, int viewType) {
        log("onCReateViewHolder");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
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
        dateLayoutParams.setMargins(0, Util.scaleHeightPx(16, outMetrics), 0, Util.scaleHeightPx(16, outMetrics));
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

        holder.timeText = (TextView) v.findViewById(R.id.message_chat_time_text);
        holder.contentMessageLayout = (RelativeLayout) v.findViewById(R.id.content_own_message_layout);
        holder.contentOwnMessageText = (TextView) v.findViewById(R.id.content_own_message_text);
        //Margins
        RelativeLayout.LayoutParams ownMessageParams = (RelativeLayout.LayoutParams)holder.contentOwnMessageText.getLayoutParams();
        ownMessageParams.setMargins(Util.scaleWidthPx(11, outMetrics), 0, Util.scaleWidthPx(62, outMetrics), Util.scaleHeightPx(16, outMetrics));
        holder.contentOwnMessageText.setLayoutParams(ownMessageParams);

//
//        holder.layoutPendingMessages = (RelativeLayout) v.findViewById(R.id.recent_chat_list_unread_layout);
//        holder.circlePendingMessages = (RoundedImageView) v.findViewById(R.id.recent_chat_list_unread_circle);
//        holder.numberPendingMessages = (TextView) v.findViewById(R.id.recent_chat_list_unread_number);

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
            holder.dateLayout.setVisibility(View.VISIBLE);
            getDate(message, holder);
            holder.ownMessageLayout.setVisibility(View.VISIBLE);
            getTime(message, holder);
            holder.contentOwnMessageText.setText(message.getMessage());
        }
        else{
            log("Contact message!!");
        }
    }

    public void getDate(Message lastMessage, ViewHolderMessageChatList holder){
        java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, Locale.getDefault());
        Calendar cal = Util.calculateDateFromTimestamp(lastMessage.getDate());
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        holder.dateText.setText(formattedDate);
    }

    public void getTime(Message lastMessage, ViewHolderMessageChatList holder){
        java.text.DateFormat df = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        Calendar cal = Util.calculateDateFromTimestamp(lastMessage.getDate());
        TimeZone tz = cal.getTimeZone();
        df.setTimeZone(tz);
        Date date = cal.getTime();
        String formattedDate = df.format(date);
        holder.timeText.setText(formattedDate);
    }

    @Override
    public int getItemCount() {
        log("getItemCount: "+messages.size());
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
