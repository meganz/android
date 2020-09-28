package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiUtils;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.components.twemoji.reaction.ReactionImageView;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.px2dp;

public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ViewHolderReaction> implements View.OnClickListener, View.OnLongClickListener {

    private MegaChatApiAndroid megaChatApi;
    private Context context;
    private long messageId;
    private long chatId;
    private ArrayList<String> listReactions;
    private AndroidMegaChatMessage megaMessage;
    private MegaChatRoom chatRoom;
    private RecyclerView recyclerViewFragment;
    private ViewHolderReactionGrid holderGrid = null;

    public ReactionAdapter(Context context, RecyclerView recyclerView, ArrayList<String> listReactions, long chatid, AndroidMegaChatMessage megaMessage) {
        this.context = context;
        this.recyclerViewFragment = recyclerView;
        this.listReactions = listReactions;
        this.chatId = chatid;
        this.megaMessage = megaMessage;
        this.messageId = megaMessage.getMessage().getMsgId();
        megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        chatRoom = megaChatApi.getChatRoom(chatId);
    }

    @Override
    public ReactionAdapter.ViewHolderReaction onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reaction, parent, false);

        holderGrid = new ViewHolderReactionGrid(v);

        holderGrid.generalLayout = v.findViewById(R.id.general_rl);
        holderGrid.moreReactionsLayout = v.findViewById(R.id.more_reactions_layout);
        holderGrid.moreReactionsLayout.setTag(holderGrid);
        holderGrid.itemReactionLayout = v.findViewById(R.id.item_reaction_layout);
        holderGrid.itemReactionLayout.setTag(holderGrid);
        holderGrid.itemNumUsersReaction = v.findViewById(R.id.item_number_users_reaction);
        holderGrid.itemEmojiReaction = v.findViewById(R.id.item_emoji_reaction);
        holderGrid.itemEmojiReactionText = v.findViewById(R.id.item_emoji_reaction_text);
        holderGrid.itemEmojiReactionText.setVisibility(View.GONE);
        holderGrid.moreReactionsLayout.setOnClickListener(this);
        holderGrid.itemReactionLayout.setOnClickListener(this);
        holderGrid.itemReactionLayout.setOnLongClickListener(chatRoom.isGroup() ? this : null);
        v.setTag(holderGrid);
        return holderGrid;
    }

    @Override
    public void onBindViewHolder(ViewHolderReaction holder, int position) {
        ViewHolderReactionGrid holderGrid2 = (ViewHolderReactionGrid) holder;
        onBindViewHolderGrid(holderGrid2, position);
    }

    private void onBindViewHolderGrid(final ViewHolderReactionGrid holder, final int position) {
        String reaction = getItemAtPosition(position);
        if (reaction == null) {
            holder.moreReactionsLayout.setVisibility(View.GONE);
            holder.itemReactionLayout.setVisibility(View.GONE);
            return;
        }

        if (reaction.equals(INVALID_REACTION)) {
            holder.moreReactionsLayout.setVisibility(View.VISIBLE);
            holder.itemReactionLayout.setVisibility(View.GONE);
            return;
        }

        holder.itemReactionLayout.setVisibility(View.VISIBLE);
        holder.moreReactionsLayout.setVisibility(View.GONE);

        List<EmojiRange> emojis = EmojiUtils.emojis(reaction);
        Emoji emoji = null;
        if(emojis != null && !emojis.isEmpty() && emojis.get(0) != null){
            emoji = emojis.get(0).emoji;
        }

        holder.reaction = reaction;
        int numUsers = megaChatApi.getMessageReactionCount(chatId, messageId, reaction);
        if(numUsers > 0){
            String text = numUsers + "";
            if (!holder.itemNumUsersReaction.getText().equals(text)) {
                holder.itemNumUsersReaction.setText(text);
            }
            Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
            DisplayMetrics outMetrics = new DisplayMetrics ();
            display.getMetrics(outMetrics);

            if(emoji == null){
                holder.itemEmojiReaction.setVisibility(View.GONE);
                holder.itemEmojiReactionText.setVisibility(View.VISIBLE);
                holder.itemEmojiReactionText.setText(reaction);
            }else{
                holder.itemEmojiReaction.setVisibility(View.VISIBLE);
                holder.itemEmojiReactionText.setVisibility(View.GONE);
                holder.itemEmojiReaction.setEmoji(emoji);
            }

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(holder.itemNumUsersReaction.getLayoutParams());
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.setMarginEnd(px2dp(8, outMetrics));
            params.addRule(RelativeLayout.END_OF, emoji == null ? R.id.item_emoji_reaction_text : R.id.item_emoji_reaction);
            holder.itemNumUsersReaction.setLayoutParams(params);
            holder.itemNumUsersReaction.setGravity(Gravity.CENTER_VERTICAL);

            boolean ownReaction = false;
            MegaHandleList handleList = megaChatApi.getReactionUsers(chatId, messageId, reaction);
            for (int i = 0; i < handleList.size(); i++) {
                if (handleList.get(i) == megaChatApi.getMyUserHandle()) {
                    ownReaction = true;
                    break;
                }
            }

            holder.itemNumUsersReaction.setTextColor(ContextCompat.getColor(context, ownReaction ? R.color.accentColor : R.color.mail_my_account));
            holder.itemReactionLayout.setBackground(ContextCompat.getDrawable(context, ownReaction ? R.drawable.own_reaction_added : R.drawable.contact_reaction_added));
        }else{
            holder.moreReactionsLayout.setVisibility(View.GONE);
            holder.itemReactionLayout.setVisibility(View.GONE);
        }
    }

    public class ViewHolderReaction extends RecyclerView.ViewHolder {
        RelativeLayout generalLayout;
        RelativeLayout moreReactionsLayout;
        RelativeLayout itemReactionLayout;
        ReactionImageView itemEmojiReaction;
        TextView itemEmojiReactionText;
        TextView itemNumUsersReaction;
        String reaction;

        public ViewHolderReaction(View itemView) {
            super(itemView);
        }
    }

    public class ViewHolderReactionGrid extends ViewHolderReaction {
        public ViewHolderReactionGrid(View v) {
            super(v);
        }
    }

    public RecyclerView getListFragment() {
        return recyclerViewFragment;
    }

    public void setListFragment(RecyclerView recyclerViewFragment) {
        this.recyclerViewFragment = recyclerViewFragment;
    }
    public ArrayList<String> getListReactions() {
        return listReactions;
    }

    private boolean isListReactionsEmpty() {
        return listReactions == null || listReactions.size() == 0;
    }

    private String getItemAtPosition(int pos) {
        return isListReactionsEmpty() || pos >= listReactions.size() ? null : listReactions.get(pos);
    }

    /**
     * Method to eliminate a reaction
     *
     * @param reaction The reaction.
     */
    public void removeItem(String reaction, long receivedChatId, long receivedMessageId) {
        if(isListReactionsEmpty() || receivedChatId != this.chatId || receivedMessageId != this.messageId){
            return;
        }

        for (String item : listReactions) {
            if (item.equals(reaction)) {
                int position = listReactions.indexOf(item);
                listReactions.remove(position);
                if (listReactions.size() == 1 && listReactions.get(0).equals(INVALID_REACTION)) {
                    listReactions.clear();
                    notifyDataSetChanged();
                } else {
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, listReactions.size());
                }
                break;
            }
        }
    }

    /**
     * Method for updating a reaction.
     *
     * @param reaction The reaction.
     */
    public void updateItem(String reaction, long receivedChatId, long receivedMessageId) {
        if(isListReactionsEmpty() || receivedChatId != this.chatId || receivedMessageId != this.messageId){
            return;
        }

        int position = INVALID_POSITION;
        for (String item : listReactions) {
            if (item.equals(reaction)) {
                position = listReactions.indexOf(item);
                break;
            }
        }
        if (position == INVALID_POSITION) {
            int positionToAdd = listReactions.size() - 1;
            listReactions.add(positionToAdd, reaction);
            notifyItemInserted(positionToAdd);
            notifyItemRangeChanged(positionToAdd, listReactions.size());
        } else {
            notifyItemChanged(position);
        }
    }

    public boolean isSameAdapter(long receivedChatId, long receivedMsgId) {
        return this.chatId == receivedChatId && this.messageId == receivedMsgId;
    }

    /**
     * Method for adding reactions.
     *
     * @param listReactions The reactions list.
     */
    public void setReactions(ArrayList<String> listReactions, long receivedChatId, long receivedMessageId) {
        if(receivedChatId != this.chatId || receivedMessageId != this.messageId){
            return;
        }

        this.listReactions = listReactions;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return isListReactionsEmpty() ? 0 : listReactions.size();
    }

    @Override
    public void onClick(View v) {
        ViewHolderReaction holder = (ViewHolderReaction) v.getTag();
        if (holder == null || holder.getAdapterPosition() < 0 || !shouldReactionBeClicked(megaChatApi.getChatRoom(chatId)))
            return;

        switch (v.getId()) {
            case R.id.more_reactions_layout:
                ((ChatActivityLollipop) context).openReactionBottomSheet(chatId, megaMessage);
                break;
            case R.id.item_reaction_layout:
                if (holder.itemEmojiReaction.getEmoji() == null) {
                    addReactionInMsg(context, chatId, megaMessage.getMessage().getMsgId(), holder.itemEmojiReactionText.getText().toString(), false);
                } else {
                    addReactionInMsg(context, chatId, megaMessage.getMessage().getMsgId(), holder.itemEmojiReaction.getEmoji(), false);
                }
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ViewHolderReaction holder = (ViewHolderReaction) v.getTag();
        if (holder == null)
            return true;

        int currentPosition = holder.getAdapterPosition();
        if (currentPosition < 0) {
            logWarning("Current position error - not valid value");
            return true;
        }

        switch (v.getId()) {
            case R.id.item_reaction_layout:
                ((ChatActivityLollipop) context).openInfoReactionBottomSheet(chatId, megaMessage, holder.reaction);
                break;
        }

        return true;
    }
}
