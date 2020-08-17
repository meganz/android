package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ArchivedChatsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class ChatBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private ChatController chatC;
    private MegaChatListItem chat = null;
    private long chatId;

    private boolean notificationsEnabled;
    private ChatItemPreferences chatPrefs;

    private RoundedImageView chatImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
        } else {
            if (context instanceof ManagerActivityLollipop) {
                chatId = ((ManagerActivityLollipop) context).selectedChatItemId;
            } else if (context instanceof ArchivedChatsActivity) {
                chatId = ((ArchivedChatsActivity) context).selectedChatItemId;
            }
        }

        if (chatId != INVALID_HANDLE) {
            chat = megaChatApi.getChatListItem(chatId);
        }

        chatC = new ChatController(context);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.chat_item_bottom_sheet, null);
        mainLinearLayout = contentView.findViewById(R.id.chat_item_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        ImageView iconStateChatPanel = contentView.findViewById(R.id.chat_list_contact_state);
        iconStateChatPanel.setMaxWidth(scaleWidthPx(6, outMetrics));
        iconStateChatPanel.setMaxHeight(scaleHeightPx(6, outMetrics));

        EmojiTextView titleNameContactChatPanel = contentView.findViewById(R.id.chat_list_chat_name_text);
        TextView titleMailContactChatPanel = contentView.findViewById(R.id.chat_list_chat_mail_text);
        chatImageView = contentView.findViewById(R.id.sliding_chat_list_thumbnail);
        TextView infoChatText = contentView.findViewById(R.id.chat_list_info_chat_text);
        LinearLayout optionInfoChat = contentView.findViewById(R.id.chat_list_info_chat_layout);
        LinearLayout optionLeaveChat = contentView.findViewById(R.id.chat_list_leave_chat_layout);
        TextView optionLeaveText = contentView.findViewById(R.id.chat_list_leave_chat_text);
        LinearLayout optionClearHistory = contentView.findViewById(R.id.chat_list_clear_history_chat_layout);
        LinearLayout optionMuteChat = contentView.findViewById(R.id.chat_list_mute_chat_layout);
        ImageView optionMuteChatIcon = contentView.findViewById(R.id.chat_list_mute_chat_image);
        TextView optionMuteChatText = contentView.findViewById(R.id.chat_list_mute_chat_text);
        LinearLayout optionArchiveChat = contentView.findViewById(R.id.chat_list_archive_chat_layout);
        TextView archiveChatText = contentView.findViewById(R.id.chat_list_archive_chat_text);
        ImageView archiveChatIcon = contentView.findViewById(R.id.file_archive_chat_image);

        if (isScreenInPortrait(context)) {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        } else {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        optionInfoChat.setOnClickListener(this);
        optionMuteChat.setOnClickListener(this);
        optionLeaveChat.setOnClickListener(this);
        optionClearHistory.setOnClickListener(this);
        optionArchiveChat.setOnClickListener(this);

        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);

        titleNameContactChatPanel.setText(getTitleChat(chat));

        if (chat.isPreview()) {
            titleMailContactChatPanel.setText(getString(R.string.group_chat_label));
            iconStateChatPanel.setVisibility(View.GONE);
            addAvatarChatPanel(null, chat);

            infoChatText.setText(getString(R.string.group_chat_info_label));

            if (megaApi != null && megaApi.getRootNode() != null) {
                optionInfoChat.setVisibility(View.VISIBLE);
                separatorInfo.setVisibility(View.VISIBLE);
            } else {
                optionInfoChat.setVisibility(View.GONE);
                separatorInfo.setVisibility(View.GONE);
            }

            optionMuteChat.setVisibility(View.GONE);
            optionLeaveChat.setVisibility(View.VISIBLE);
            optionLeaveText.setText("Remove preview");
            optionClearHistory.setVisibility(View.GONE);
            optionArchiveChat.setVisibility(View.GONE);
        } else {
            if (chat.isGroup()) {
                titleMailContactChatPanel.setText(getString(R.string.group_chat_label));
                iconStateChatPanel.setVisibility(View.GONE);
                addAvatarChatPanel(null, chat);

                infoChatText.setText(getString(R.string.group_chat_info_label));
                optionInfoChat.setVisibility(View.VISIBLE);

                if (chat.isActive()) {
                    optionLeaveChat.setVisibility(View.VISIBLE);
                } else {
                    optionLeaveChat.setVisibility(View.GONE);
                }

                if ((chat.getLastMessageType() == MegaChatMessage.TYPE_INVALID) || (chat.getLastMessageType() == MegaChatMessage.TYPE_TRUNCATE)) {
                    optionClearHistory.setVisibility(View.GONE);
                } else if (chat.isActive() && chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR) {
                    optionClearHistory.setVisibility(View.VISIBLE);
                } else {
                    optionClearHistory.setVisibility(View.GONE);
                }
            } else {
                iconStateChatPanel.setVisibility(View.VISIBLE);

                long userHandle = chat.getPeerHandle();
                MegaUser contact = megaApi.getContact(MegaApiJava.userHandleToBase64(userHandle));

                if ((chat.getLastMessageType() == MegaChatMessage.TYPE_INVALID) || (chat.getLastMessageType() == MegaChatMessage.TYPE_TRUNCATE)) {
                    optionClearHistory.setVisibility(View.GONE);
                } else if (chat.isActive()) {
                    optionClearHistory.setVisibility(View.VISIBLE);
                } else {
                    optionClearHistory.setVisibility(View.GONE);
                }

                if (contact != null) {
                    titleMailContactChatPanel.setText(contact.getEmail());
                    addAvatarChatPanel(contact.getEmail(), chat);

                    if (contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                        optionInfoChat.setVisibility(View.VISIBLE);
                        infoChatText.setText(getString(R.string.contact_properties_activity));
                    } else {
                        optionInfoChat.setVisibility(View.GONE);
                        optionClearHistory.setVisibility(View.GONE);
                    }
                } else {
                    optionInfoChat.setVisibility(View.GONE);
                    optionClearHistory.setVisibility(View.GONE);
                }

                optionLeaveChat.setVisibility(View.GONE);
                setContactStatus(megaChatApi.getUserOnlineStatus(userHandle), iconStateChatPanel);
            }

            chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chat.getChatId()));
            if (chatPrefs != null) {
                notificationsEnabled = true;
                if (chatPrefs.getNotificationsEnabled() != null) {
                    notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
                }

                if (!notificationsEnabled) {
                    optionMuteChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_unmute));
                    optionMuteChatText.setText(getString(R.string.general_unmute));
                }
            } else {
                MegaChatRoom chatRoom = megaChatApi.getChatRoomByUser(chat.getPeerHandle());
                if (chatRoom != null) {
                    String email = chatC.getParticipantEmail(chatRoom.getPeerHandle(0));
                    titleMailContactChatPanel.setText(email);
                    addAvatarChatPanel(email, chat);
                }
            }

            if (chat.isArchived()) {
                archiveChatText.setText(getString(R.string.unarchive_chat_option));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_unarchive));
                optionInfoChat.setVisibility(View.GONE);
                optionMuteChat.setVisibility(View.GONE);
                optionLeaveChat.setVisibility(View.GONE);
                optionClearHistory.setVisibility(View.GONE);
            } else {
                archiveChatText.setText(getString(R.string.archive_chat_option));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_b_archive));
            }
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    private void addAvatarChatPanel(String contactMail, MegaChatListItem chat) {
        Bitmap bitmap = getAvatarBitmap(contactMail);

        if (bitmap != null) {
            chatImageView.setImageBitmap(bitmap);
        } else {
            int color;
            String name = null;

            if (!isTextEmpty(getTitleChat(chat))) {
                name = getTitleChat(chat);
            } else if (!isTextEmpty(contactMail)) {
                name = contactMail;
            }

            if (chat.isGroup()) {
                color = getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR);
            } else {
                color = getColorAvatar(megaApi.getContact(contactMail));
            }

            chatImageView.setImageBitmap(getDefaultAvatar(color, name, AVATAR_SIZE, false));
        }
    }

    @Override
    public void onClick(View v) {
        if (chat == null) {
            logDebug("Selected chat NULL");
            return;
        }

        switch (v.getId()) {
            case R.id.chat_list_info_chat_layout:
                if (chat.isGroup()) {
                    Intent i = new Intent(context, GroupChatInfoActivityLollipop.class);
                    i.putExtra(HANDLE, chat.getChatId());
                    context.startActivity(i);
                } else {
                    Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                    i.putExtra(HANDLE, chat.getChatId());
                    context.startActivity(i);
                }

                break;

            case R.id.chat_list_leave_chat_layout:
                logDebug("Leave chat - Chat ID: " + chat.getChatId());
                ((ManagerActivityLollipop) context).showConfirmationLeaveChat(chat);
                break;

            case R.id.chat_list_clear_history_chat_layout:
                logDebug("Clear chat - Chat ID: " + chat.getChatId());
                ((ManagerActivityLollipop) context).showConfirmationClearChat(chat);
                break;

            case R.id.chat_list_mute_chat_layout:
                if (chatPrefs == null) {
                    chatPrefs = new ChatItemPreferences(Long.toString(chat.getChatId()), Boolean.toString(notificationsEnabled), "");
                    dbH.setChatItemPreferences(chatPrefs);
                } else if (notificationsEnabled) {
                    chatC.muteChat(chat);
                } else {
                    chatC.unmuteChat(chat);
                }

                ((ManagerActivityLollipop) context).showMuteIcon(chat);
                break;

            case R.id.chat_list_archive_chat_layout:
                chatC.archiveChat(chat);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
    }
}
