package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiKeyboard;
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ChatActivity;
import nz.mega.sdk.MegaChatMessage;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class ReactionsBottomSheet extends BottomSheetDialogFragment {

    private final static int HEIGHT_REACTIONS_KEYBOARD = 250;

    private AndroidMegaChatMessage message;
    private long chatId;
    private long messageId;
    private RelativeLayout mainLayout;
    private EmojiKeyboard reactionsKeyboard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(requireActivity() instanceof ChatActivity))
            return;

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, MEGACHAT_INVALID_HANDLE);
        } else {
            chatId = ((ChatActivity) requireActivity()).idChat;
            messageId = ((ChatActivity) requireActivity()).selectedMessageId;
        }

        MegaChatMessage messageMega = MegaApplication.getInstance().getMegaChatApi().getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_reaction, null);
        mainLayout = contentView.findViewById(R.id.bottom_sheet);
        reactionsKeyboard = contentView.findViewById(R.id.reaction_keyboard);
        reactionsKeyboard.initReaction(dp2px(HEIGHT_REACTIONS_KEYBOARD));
        reactionsKeyboard.setOnEmojiSelectedListener(emoji -> {
            addReactionInMsg(requireActivity(), chatId, message.getMessage().getMsgId(), emoji, true);
            closeDialog();
        });

        dialog.setContentView(contentView);
        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) mainLayout.getParent());
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
