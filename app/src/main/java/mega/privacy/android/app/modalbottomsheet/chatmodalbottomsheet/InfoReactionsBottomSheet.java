package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import biz.laenger.android.vpbs.BottomSheetUtils;
import biz.laenger.android.vpbs.ViewPagerBottomSheetDialogFragment;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.reaction.UserReactionListView;
import mega.privacy.android.app.components.twemoji.EmojiImageView;
import mega.privacy.android.app.components.twemoji.EmojiRange;
import mega.privacy.android.app.components.twemoji.EmojiUtils;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.InfoReactionPagerAdapter;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaStringList;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class InfoReactionsBottomSheet extends ViewPagerBottomSheetDialogFragment implements ViewPager.OnPageChangeListener {
    private Context context;
    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private DisplayMetrics outMetrics;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private LinearLayout infoReactionsTab;
    private int reactionTabLastSelectedIndex = INVALID_POSITION;
    private ArrayList<RelativeLayout> reactionTabs = null;
    private RelativeLayout separator;
    private InfoReactionPagerAdapter reactionsPageAdapter = null;
    private ViewPager infoReactionsPager;
    private String reactionSelected;
    private String REACTION_SELECTED = "REACTION_SELECTED";
    private ArrayList<String> list;

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

        if (!(context instanceof ChatActivityLollipop))
            return;

        if (savedInstanceState != null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong(CHAT_ID, -1);
            messageId = savedInstanceState.getLong(MESSAGE_ID, -1);
            reactionSelected = savedInstanceState.getString(REACTION_SELECTED);

            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }

        } else {
            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;

            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        }
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_info_reactions, null);
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
        dialog.setContentView(contentView);
    }

    private void addReaction(int position, String reaction){
        reactionTabs.add(position, inflateButton(context, reaction, infoReactionsTab));
        reactionTabs.get(position).setOnClickListener(new ReactionsTabsClickListener(infoReactionsPager, position));
    }

    private void removeReaction(int position, String reaction){
        if (reactionTabs.get(position).isSelected()) {
            reactionTabs.get(position).setSelected(false);
            infoReactionsPager.setCurrentItem(0);
            onPageSelected(0);
        }

        reactionTabs.get(position).removeAllViews();
        reactionTabs.remove(position);
    }
    private RelativeLayout inflateButton(final Context context, String reaction, final ViewGroup parent) {
        final RelativeLayout button = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.reaction_view, parent, false);
        final EmojiImageView reactionImage = button.findViewById(R.id.reaction_image);
        List<EmojiRange> emojis = EmojiUtils.emojis(reaction);
        reactionImage.setEmoji(emojis.get(0).emoji, true);
        parent.addView(button);
        return button;
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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(final int i) {
        if (reactionTabLastSelectedIndex != i) {
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
                lp.leftMargin = px2dp(9,outMetrics);
                lp.rightMargin = px2dp(9,outMetrics);
                reactionTabs.get(i).addView(separator, lp);
            }
            reactionTabLastSelectedIndex = i;
            reactionTabs.get(i).getParent().requestChildFocus(reactionTabs.get(i), reactionTabs.get(i));
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    public void changeInReactionReceived(long messageId, long chatId, String reaction, int count) {
        if (this.messageId != messageId || this.chatId != chatId)
            return;

        if (count == 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(reaction)) {
                    removeReaction(i, reaction);
                    list.remove(i);
                    if (reactionsPageAdapter != null) {
                        removeView(reactionsPageAdapter.getView(i));
//                        reactionsPageAdapter.notifyDataSetChanged();
                    } else {
                        reactionsPageAdapter = new InfoReactionPagerAdapter(context, list, messageId, chatId);
                    }

                    for(int j =0; j<reactionTabs.size(); j++ ){
                        if(reactionTabs.get(j).isSelected()){
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
            //Found the reaction:
            int position = -1;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(reaction)) {
                   position = i;
                    break;
                }
            }
            if(position == -1){
                addReaction(reactionTabs.size() , reaction);
                list.add(reaction);

                if (reactionsPageAdapter != null) {
                    addView(reaction);
                } else {
                    reactionsPageAdapter = new InfoReactionPagerAdapter(context, list, messageId, chatId);
                }
            }else{
                if (reactionsPageAdapter != null) {
                    reactionsPageAdapter.updatePage(position, reaction);
                } else {
                    reactionsPageAdapter = new InfoReactionPagerAdapter(context, list, messageId, chatId);
                }
            }

        }

    }

    /**
     * Aadd a view to the ViewPager.
     * @param reaction
     */
    public void addView (String reaction) {
        View newPage = new UserReactionListView(context).init(reaction, messageId, chatId);
        reactionsPageAdapter.addView(newPage);
        reactionsPageAdapter.notifyDataSetChanged();
    }

    /**
     * Remove a view from the ViewPager
     * @param defunctPage
     */
    public void removeView (View defunctPage) {
        reactionsPageAdapter.removeView (infoReactionsPager, defunctPage);
    }

    /**
     * Get the currently displayed page
     * @return
     */
    public View getCurrentPage () {
        return reactionsPageAdapter.getView (infoReactionsPager.getCurrentItem());
    }

    /**
     * Set the currently displayed page
     * @param pageToShow
     */
    public void setCurrentPage (View pageToShow) {
        infoReactionsPager.setCurrentItem (reactionsPageAdapter.getItemPosition (pageToShow), true);
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
}
