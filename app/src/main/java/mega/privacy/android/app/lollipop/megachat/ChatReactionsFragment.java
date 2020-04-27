package mega.privacy.android.app.lollipop.megachat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiImageView;
import mega.privacy.android.app.components.twemoji.RecentEmoji;
import mega.privacy.android.app.components.twemoji.RecentEmojiManager;
import mega.privacy.android.app.components.twemoji.emoji.Emoji;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatReactionsFragment extends RelativeLayout implements View.OnClickListener {

    private Context context;
    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private int positionMessage;

    private RelativeLayout firstReaction;
    private EmojiImageView firstEmoji;
    private RelativeLayout secondReaction;
    private EmojiImageView secondEmoji;
    private RelativeLayout thirdReaction;
    private EmojiImageView thirdEmoji;
    private RelativeLayout fourthReaction;
    private EmojiImageView fourthEmoji;
    private RelativeLayout fifthReaction;
    private EmojiImageView fifthEmoji;
    private RelativeLayout addReaction;

    private MegaChatApiAndroid megaChatApi;
    private RecentEmoji recentEmoji = null;
    private LayoutInflater inflater;

    public ChatReactionsFragment(Context context) {
        super(context);
        initView(context);
    }

    public ChatReactionsFragment(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ChatReactionsFragment(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void init(Context context, long chatId, long messageId, int positionMessage) {
        this.context = context;
        this.chatId = chatId;
        this.messageId = messageId;
        this.positionMessage = positionMessage;
        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        firstReaction.setOnClickListener(this);
        firstEmoji.setOnClickListener(this);
        secondReaction.setOnClickListener(this);
        secondEmoji.setOnClickListener(this);
        thirdReaction.setOnClickListener(this);
        thirdEmoji.setOnClickListener(this);
        fourthReaction.setOnClickListener(this);
        fourthEmoji.setOnClickListener(this);
        fifthReaction.setOnClickListener(this);
        fifthEmoji.setOnClickListener(this);
        addReaction.setOnClickListener(this);

        recentEmoji = new RecentEmojiManager(getContext(), TYPE_REACTION);
    }

    private void initView(Context context){
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.add_reaction_layout, null);
        firstReaction = view.findViewById(R.id.first_emoji_layout);
        firstEmoji = view.findViewById(R.id.first_emoji_image);
        secondReaction = view.findViewById(R.id.second_emoji_layout);
        secondEmoji = view.findViewById(R.id.second_emoji_image);
        thirdReaction = view.findViewById(R.id.third_emoji_layout);
        thirdEmoji = view.findViewById(R.id.third_emoji_image);
        fourthReaction = view.findViewById(R.id.fourth_emoji_layout);
        fourthEmoji = view.findViewById(R.id.fourth_emoji_image);
        fifthReaction = view.findViewById(R.id.fifth_emoji_layout);
        fifthEmoji = view.findViewById(R.id.fifth_emoji_image);
        addReaction = view.findViewById(R.id.icon_more_reactions);

        firstEmoji.setEmoji(new Emoji(0x1f642, R.drawable.emoji_twitter_1f642));
        secondEmoji.setEmoji(new Emoji(0x2639, R.drawable.emoji_twitter_2639));
        thirdEmoji.setEmoji(new Emoji(0x1f923, R.drawable.emoji_twitter_1f923));
        fourthEmoji.setEmoji(new Emoji(0x1f44d, R.drawable.emoji_twitter_1f44d));
        fifthEmoji.setEmoji(new Emoji(0x1f44f, R.drawable.emoji_twitter_1f44f));

        this.addView(view);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for(int i = 0 ; i < getChildCount() ; i++){
            getChildAt(i).layout(l, t, r, b);
        }
    }

    @Override
    public void onClick(View view) {
        if (message == null) {
            logWarning("The message is NULL");
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        switch(view.getId()){
            case R.id.first_emoji_layout:
            case R.id.first_emoji_image:
                addReaction(view.findViewById(R.id.first_emoji_image));
                break;

            case R.id.second_emoji_layout:
            case R.id.second_emoji_image:
                addReaction(view.findViewById(R.id.second_emoji_image));
                break;

            case R.id.third_emoji_layout:
            case R.id.third_emoji_image:
                addReaction(view.findViewById(R.id.third_emoji_image));
                break;

            case R.id.fourth_emoji_layout:
            case R.id.fourth_emoji_image:
                addReaction(view.findViewById(R.id.fourth_emoji_image));
                break;

            case R.id.fifth_emoji_layout:
            case R.id.fifth_emoji_image:
                addReaction(view.findViewById(R.id.fifth_emoji_image));
                break;

            case R.id.icon_more_reactions:
                ((ChatActivityLollipop) context).showReactionBottomSheet(messagesSelected.get(0), positionMessage);
                break;
        }
    }

    private void addReaction(EmojiImageView imageEmoji){
        if(recentEmoji != null){
            recentEmoji.addEmoji(imageEmoji.getEmoji());
        }

        addReactionInMsg(context, chatId, message.getMessage(),imageEmoji.getEmoji(),true);
        closeDialog();
    }

    private void closeDialog(){
        if(recentEmoji != null){
            recentEmoji.persist();
        }

        ((ChatActivityLollipop) context).hideBottomSheet();
    }
}
