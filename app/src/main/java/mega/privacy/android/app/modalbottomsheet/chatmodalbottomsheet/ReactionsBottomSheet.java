package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiKeyboard;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatMessage;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class ReactionsBottomSheet extends BaseBottomSheetDialogFragment {

    private final static int HEIGHT_REACTIONS_KEYBOARD = 250;

    private AndroidMegaChatMessage message;
    private long chatId;
    private long messageId;
    private RelativeLayout mainLayout;
    private EmojiKeyboard reactionsKeyboard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(context instanceof ChatActivityLollipop))
            return;

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, MEGACHAT_INVALID_HANDLE);
        } else {
            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_reaction, null);
        mainLayout = contentView.findViewById(R.id.bottom_sheet);
        reactionsKeyboard = contentView.findViewById(R.id.reaction_keyboard);
        reactionsKeyboard.initReaction(px2dp(HEIGHT_REACTIONS_KEYBOARD, outMetrics));
        reactionsKeyboard.setOnEmojiSelectedListener(emoji -> {
            addReactionInMsg(context, chatId, message.getMessage().getMsgId(), emoji, true);
            closeDialog();
        });

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLayout.getParent());
        mBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
    }

    private void closeDialog() {
        if (reactionsKeyboard != null) {
            reactionsKeyboard.persistReactionList();
        }
        dismissAllowingStateLoss();
    }
}
