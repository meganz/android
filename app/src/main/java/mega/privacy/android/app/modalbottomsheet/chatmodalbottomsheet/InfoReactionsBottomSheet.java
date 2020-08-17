package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.reaction.*;
import mega.privacy.android.app.components.twemoji.EmojiImageView;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiUtils;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.InfoReactionPagerAdapter;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaStringList;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class InfoReactionsBottomSheet extends ViewPagerBottomSheetDialogFragment implements ViewPager.OnPageChangeListener {

    private static final int HEIGHT_HEADER = 60;
    private static final int HEIGHT_USERS = 56;
    private static final int MARGIN = 9;

    private Context context;
    private long chatId;
    private long messageId;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DisplayMetrics outMetrics;
    private LinearLayout infoReactionsTab;
    private int reactionTabLastSelectedIndex = INVALID_POSITION;
    private ArrayList<RelativeLayout> reactionTabs = null;
    private RelativeLayout separator;
    private InfoReactionPagerAdapter reactionsPageAdapter = null;
    private ViewPager infoReactionsPager;
    private String reactionSelected;
    private String REACTION_SELECTED = "REACTION_SELECTED";
    private ArrayList<String> list;
    private int halfHeightDisplay;

    public InfoReactionsBottomSheet() {
    }

    public InfoReactionsBottomSheet(Context context, String reactionSelected) {
        this.context = context;
        this.reactionSelected = reactionSelected;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, MEGACHAT_INVALID_HANDLE);
            reactionSelected = savedInstanceState.getString(REACTION_SELECTED);
        } else {
            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        outMetrics = MegaApplication.getInstance().getBaseContext().getResources().getDisplayMetrics();
        halfHeightDisplay = outMetrics.heightPixels / 2;
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_info_reactions, null);
        RelativeLayout generalLayout = contentView.findViewById(R.id.general_layout);
        infoReactionsTab = contentView.findViewById(R.id.info_reactions_tabs);
        infoReactionsPager = contentView.findViewById(R.id.info_reactions_pager);
        infoReactionsPager.addOnPageChangeListener(this);
        separator = new RelativeLayout(context);

        separator.setBackgroundColor(ContextCompat.getColor(context, R.color.accentColor));

        final MegaStringList listReactions = megaChatApi.getMessageReactions(chatId, messageId);
        list = getReactionsList(listReactions, false);
        reactionTabs = new ArrayList<>();
        int indexToStart = 0;
        for (int i = 0; i < list.size(); i++) {
            addReaction(i, list.get(i));
            if (reactionSelected.equals(list.get(i))) {
                indexToStart = i;
            }
        }

        if (reactionsPageAdapter == null) {
            reactionsPageAdapter = new InfoReactionPagerAdapter(context, list, messageId, chatId);
        }

        infoReactionsPager.setAdapter(reactionsPageAdapter);
        reactionsPageAdapter.notifyDataSetChanged();
        infoReactionsPager.setCurrentItem(indexToStart);
        onPageSelected(indexToStart);
        BottomSheetUtils.setupViewPager(infoReactionsPager);

        ViewGroup.LayoutParams params = generalLayout.getLayoutParams();
        int height = getHeight();
        if(height == 0)
            return;

        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = height;
        } else {
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        }
        generalLayout.setLayoutParams(params);

        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(contentView);
    }

    /**
     * Method for obtaining the height of the dialogue.
     *
     * @return Integer with the height.
     */
    private int getHeight() {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        ArrayList<Long> totalUsers = new ArrayList<>();
        for (String currentReaction : list) {
            MegaHandleList listUsers = MegaApplication.getInstance().getMegaChatApi().getReactionUsers(chatId, messageId, currentReaction);
            totalUsers.add(listUsers.size());
        }

        long numMaxUsers = 0;
        for (Long user : totalUsers) {
            if (user > numMaxUsers) {
                numMaxUsers = user;
            }
        }

        int numOptions = (int) numMaxUsers;
        int heightChild = px2dp(HEIGHT_USERS, outMetrics);
        int peekHeight = px2dp(HEIGHT_HEADER, outMetrics);

        int possibleHeight = peekHeight + (heightChild * numOptions);

        if (possibleHeight < halfHeightDisplay) {
            return possibleHeight;
        }
        int child = (heightChild / 3);
        return peekHeight + (heightChild * (numOptions - 1)) + (child * 2);
    }

    /**
     * Method for adding a reaction in the dialogue.
     *
     * @param position The reaction position.
     * @param reaction The reaction String.
     */
    private void addReaction(int position, String reaction) {
        reactionTabs.add(position, inflateButton(context, reaction, infoReactionsTab));
        reactionTabs.get(position).setOnClickListener(new ReactionsTabsClickListener(infoReactionsPager, position));
    }

    /**
     * Method for removing a reaction in the dialogue.
     *
     * @param position The reaction position.
     */
    private void removeReaction(int position) {
        if (reactionTabs.get(position).isSelected()) {
            reactionTabs.get(position).setSelected(false);
            infoReactionsPager.setCurrentItem(0);
            onPageSelected(0);
        }

        reactionTabs.get(position).removeAllViews();
        reactionTabs.remove(position);
    }

    /**
     * Method for adding the reaction buttons.
     *
     * @param context  Context of the Activity.
     * @param reaction The reaction.
     * @param parent   The view parent.
     * @return The button inflated.
     */
    private RelativeLayout inflateButton(final Context context, String reaction, final ViewGroup parent) {
        final RelativeLayout button = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.reaction_view, parent, false);
        final EmojiImageView reactionImage = button.findViewById(R.id.reaction_image);
        List<EmojiRange> emojis = EmojiUtils.emojis(reaction);
        reactionImage.setEmoji(emojis.get(0).emoji, true);
        parent.addView(button);
        return button;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

    @Override
    public void onPageSelected(final int i) {
        if(reactionTabLastSelectedIndex == i)
            return;

        if (reactionTabLastSelectedIndex >= 0 && reactionTabLastSelectedIndex < reactionTabs.size()) {
            reactionTabs.get(reactionTabLastSelectedIndex).setSelected(false);
            if (reactionTabs.get(reactionTabLastSelectedIndex).getChildCount() > 0) {
                reactionTabs.get(reactionTabLastSelectedIndex).removeView(separator);
            }
        }

        reactionTabs.get(i).setSelected(true);

        if (reactionTabs.get(i).getChildCount() == 1) {
            if (separator.getParent() != null) {
                ((ViewGroup) separator.getParent()).removeView(separator);
            }
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(px2dp(40, outMetrics), px2dp(2, outMetrics));
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.leftMargin = px2dp(MARGIN, outMetrics);
            lp.rightMargin = px2dp(MARGIN, outMetrics);
            reactionTabs.get(i).addView(separator, lp);
        }

        reactionTabLastSelectedIndex = i;
        reactionTabs.get(i).getParent().requestChildFocus(reactionTabs.get(i), reactionTabs.get(i));
    }

    @Override
    public void onPageScrollStateChanged(int state) { }

    /**
     * Method for controlling changes received in reactions.
     *
     * @param messageId The message ID.
     * @param chatId    The chat room ID.
     * @param reaction  The reaction.
     * @param count     Number of reactions in a message.
     */
    public void changeInReactionReceived(long messageId, long chatId, String reaction, int count) {
        if (this.messageId != messageId || this.chatId != chatId)
            return;

        if (count == 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(reaction)) {
                    removeReaction(i);
                    list.remove(i);
                    if (reactionsPageAdapter != null) {
                        removeView(reactionsPageAdapter.getView(i));
                    } else {
                        reactionsPageAdapter = new InfoReactionPagerAdapter(context, list, messageId, chatId);
                    }

                    for (int j = 0; j < reactionTabs.size(); j++) {
                        if (reactionTabs.get(j).isSelected()) {
                            infoReactionsPager.setCurrentItem(j);
                            onPageSelected(j);
                            break;
                        }
                    }
                    break;
                }
            }

            if (reactionTabs.size() == 0) {
                dismissAllowingStateLoss();
            }
        } else {
            int position = INVALID_POSITION;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(reaction)) {
                    position = i;
                    break;
                }
            }

            if (position == INVALID_POSITION) {
                addReaction(reactionTabs.size(), reaction);
                list.add(reaction);
                if (reactionsPageAdapter != null) {
                    addView(reaction);
                    return;
                }
            } else if (reactionsPageAdapter != null) {
                reactionsPageAdapter.updatePage(position, reaction);
                return;
            }

            reactionsPageAdapter = new InfoReactionPagerAdapter(context, list, messageId, chatId);
        }
    }

    /**
     * Add a view to the ViewPager.
     *
     * @param reaction The reaction.
     */
    public void addView(String reaction) {
        View newPage = new UserReactionListView(context).init(reaction, messageId, chatId);
        reactionsPageAdapter.addView(newPage);
        reactionsPageAdapter.notifyDataSetChanged();
    }

    /**
     * Remove a view from the ViewPager.
     *
     * @param defunctPage The pager.
     */
    public void removeView(View defunctPage) {
        reactionsPageAdapter.removeView(infoReactionsPager, defunctPage);
    }

    private static class ReactionsTabsClickListener implements View.OnClickListener {
        private final ViewPager reactionsPager;
        private final int position;

        ReactionsTabsClickListener(final ViewPager reactionsPager, final int position) {
            this.reactionsPager = reactionsPager;
            this.position = position;
        }

        @Override
        public void onClick(final View v) {
            reactionsPager.setCurrentItem(position);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putString(REACTION_SELECTED, reactionSelected);
    }
}
