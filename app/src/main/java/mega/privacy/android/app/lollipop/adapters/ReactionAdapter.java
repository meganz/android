package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
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
import mega.privacy.android.app.components.twemoji.ReactionImageView;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ReactionAdapter extends RecyclerView.Adapter<ReactionAdapter.ViewHolderReaction> implements View.OnClickListener, View.OnLongClickListener {

    private MegaChatApiAndroid megaChatApi;
    private Context context;
    private long messageId;
    private long chatId;
    private ArrayList<String> listReactions;
    private AndroidMegaChatMessage megaMessage;
    private MegaChatRoom chatRoom;

    public ReactionAdapter(Context context, long chatid, AndroidMegaChatMessage megaMessage, ArrayList<String> listReactions) {
        this.context = context;
        this.listReactions = listReactions;
        this.chatId = chatid;
        this.megaMessage = megaMessage;
        this.messageId = megaMessage.getMessage().getMsgId();
        megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        chatRoom = megaChatApi.getChatRoom(chatId);
        if (chatRoom == null)
            return;
    }

    @Override
    public ReactionAdapter.ViewHolderReaction onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reaction, parent, false);
        ViewHolderReaction holder = new ViewHolderReaction(v);
        holder.moreReactionsLayout = v.findViewById(R.id.more_reactions_layout);
        holder.moreReactionsLayout.setTag(holder);
        holder.itemReactionLayout = v.findViewById(R.id.item_reaction_layout);
        holder.itemReactionLayout.setTag(holder);
        holder.itemNumUsersReaction = v.findViewById(R.id.item_number_users_reaction);
        holder.itemEmojiReaction = v.findViewById(R.id.item_emoji_reaction);

        holder.moreReactionsLayout.setOnClickListener(this);
        holder.itemReactionLayout.setOnClickListener(this);

        if (chatRoom.isGroup()) {
            holder.itemReactionLayout.setOnLongClickListener(this);
        } else {
            holder.itemReactionLayout.setOnLongClickListener(null);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(ReactionAdapter.ViewHolderReaction holder, int position) {
        String reaction = getItemAtPosition(position);
        if (reaction == null) {
            return;
        }
        if (reaction.equals(INVALID_REACTION)) {
            /* Add more reactions icon visible*/
            holder.moreReactionsLayout.setVisibility(View.VISIBLE);
            holder.itemReactionLayout.setVisibility(View.GONE);
            return;
        }

        /* Specific reaction*/
        holder.itemReactionLayout.setVisibility(View.VISIBLE);
        holder.moreReactionsLayout.setVisibility(View.GONE);

        List<EmojiRange> emojis = EmojiUtils.emojis(reaction);
        holder.emojiReaction = emojis.get(0).emoji;
        holder.reaction = reaction;

        /*Number users*/
        int numUsers = megaChatApi.getMessageReactionCount(chatId, messageId, reaction);
        String text = numUsers + "";
        holder.itemNumUsersReaction.setText(text);

        /*Color background*/
        boolean ownReaction = false;
        MegaHandleList handleList = megaChatApi.getReactionUsers(chatId, messageId, reaction);
        for (int i = 0; i < handleList.size(); i++) {
            if (handleList.get(i) == megaChatApi.getMyUserHandle()) {
                ownReaction = true;
                break;
            }
        }

        if (ownReaction) {
            holder.itemNumUsersReaction.setTextColor(ContextCompat.getColor(context, R.color.stroke_own_reaction_added));
            holder.itemReactionLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.own_reaction_added));
        } else {
            holder.itemNumUsersReaction.setTextColor(ContextCompat.getColor(context, R.color.number_reactions_added));
            holder.itemReactionLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.contact_reaction_added));
        }

        /*Add emoji Reaction*/
        holder.itemEmojiReaction.setEmoji(holder.emojiReaction);
    }

    private String getItemAtPosition(int pos) {
        if (listReactions == null || listReactions.size() == 0 || pos >= listReactions.size())
            return null;

        return listReactions.get(pos);
    }

    /**
     * Method to eliminate a reaction
     *
     * @param reaction The reaction.
     */
    public void removeItem(String reaction) {
        if (listReactions == null || listReactions.size() == 0)
            return;

        for (String item : listReactions) {
            if (item.equals(reaction)) {
                int position = listReactions.indexOf(item);
                listReactions.remove(position);
                if (listReactions.size() == 1 && listReactions.get(0).equals(INVALID_REACTION)) {
                    listReactions.clear();
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
    public void updateItem(String reaction) {
        if (listReactions == null || listReactions.size() == 0)
            return;

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

    /**
     * Method for adding reactions.
     *
     * @param listReactions The reactions list.
     */
    public void setReactions(ArrayList<String> listReactions) {
        this.listReactions = listReactions;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (listReactions == null)
            return 0;

        return listReactions.size();
    }

    @Override
    public void onClick(View v) {
        ViewHolderReaction holder = (ViewHolderReaction) v.getTag();
        if (holder == null)
            return;

        int currentPosition = holder.getAdapterPosition();
        if (currentPosition < 0) {
            logWarning("Current position error - not valid value");
            return;
        }

        switch (v.getId()) {
            case R.id.more_reactions_layout:
                ((ChatActivityLollipop) context).openReactionBottomSheet(chatId, megaMessage);
                break;

            case R.id.item_reaction_layout:
                addReactionInMsg(context, chatId, megaMessage.getMessage(), holder.emojiReaction, false);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {

        ViewHolderReaction holder = (ViewHolderReaction) v.getTag();
        if (holder == null)
            return false;

        int currentPosition = holder.getAdapterPosition();
        if (currentPosition < 0) {
            logWarning("Current position error - not valid value");
            return false;
        }

        switch (v.getId()) {
            case R.id.item_reaction_layout:
                ((ChatActivityLollipop) context).openInfoReactionBottomSheet(chatId, megaMessage, holder.reaction);
                break;
        }

        return false;
    }

    public class ViewHolderReaction extends RecyclerView.ViewHolder {

        private RelativeLayout moreReactionsLayout;
        private RelativeLayout itemReactionLayout;
        private ReactionImageView itemEmojiReaction;
        private TextView itemNumUsersReaction;
        private Emoji emojiReaction;
        private String reaction;

        public ViewHolderReaction(View itemView) {
            super(itemView);
        }
    }
}
