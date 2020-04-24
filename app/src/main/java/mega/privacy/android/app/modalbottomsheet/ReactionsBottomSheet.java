package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.ReactionsKeyboard;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ReactionsBottomSheet extends BottomSheetDialogFragment{

    private final static int HEIGHT_REACTIONS_KEYBOARD = 250;

    private Context context;
    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private View contentView;
    private BottomSheetBehavior mBehavior;
    private RelativeLayout mainLayout;
    private ReactionsKeyboard reactionsKeyboard;
    private DisplayMetrics outMetrics;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if(!(context instanceof ChatActivityLollipop))
            return;

        if (savedInstanceState != null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong(CHAT_ID, -1);
            messageId = savedInstanceState.getLong(MESSAGE_ID, -1);

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

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_reaction, null);
        mainLayout = contentView.findViewById(R.id.bottom_sheet);
        reactionsKeyboard = contentView.findViewById(R.id.reaction_keyboard);
        reactionsKeyboard.init(px2dp(HEIGHT_REACTIONS_KEYBOARD, outMetrics));
        reactionsKeyboard.setOnEmojiSelectedListener(emoji -> {
            addReactionInMsg(context, chatId, message.getMessage(), emoji, true);
            closeDialog();
        });

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
    }

    private void closeDialog(){
        if(reactionsKeyboard != null){
            reactionsKeyboard.persistReactionList();
        }
        dismissAllowingStateLoss();
    }
}
