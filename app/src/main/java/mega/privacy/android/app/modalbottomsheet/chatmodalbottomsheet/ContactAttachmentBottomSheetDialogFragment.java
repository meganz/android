package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import static mega.privacy.android.app.utils.AvatarUtil.setImageAvatar;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.getUserStatus;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.EMAIL;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.ContactAttachmentActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import mega.privacy.android.app.utils.ContactUtil;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

public class ContactAttachmentBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private AndroidMegaChatMessage message;
    private long chatId;
    private long messageId;
    private String email;
    private ChatController chatC;
    private int position;

    public EmojiTextView titleNameContactChatPanel;
    public ImageView stateIcon;
    public EmojiTextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_attachment_item, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_HANDLE);
            email = savedInstanceState.getString(EMAIL);

            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        } else {
            chatId = ((ContactAttachmentActivity) requireActivity()).chatId;
            messageId = ((ContactAttachmentActivity) requireActivity()).messageId;
            email = ((ContactAttachmentActivity) requireActivity()).selectedEmail;
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        Timber.d("Chat ID: %d, Message ID: %d", chatId, messageId);
        chatC = new ChatController(requireActivity());

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (message == null) {
            Timber.e("Message is null");
            return;
        }

        LinearLayout titleContact = contentView.findViewById(R.id.contact_attachment_chat_title_layout);
        View separatorTitleContact = contentView.findViewById(R.id.contact_title_separator);

        titleNameContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_name_text);
        stateIcon = contentView.findViewById(R.id.contact_attachment_state_circle);
        stateIcon.setMaxWidth(scaleWidthPx(6, getResources().getDisplayMetrics()));
        stateIcon.setMaxHeight(scaleHeightPx(6, getResources().getDisplayMetrics()));

        titleMailContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_mail_text);
        contactImageView = contentView.findViewById(R.id.contact_attachment_thumbnail);

        LinearLayout optionView = contentView.findViewById(R.id.option_view_layout);
        LinearLayout optionInfo = contentView.findViewById(R.id.option_info_layout);
        LinearLayout optionStartConversation = contentView.findViewById(R.id.option_start_conversation_layout);
        LinearLayout optionInvite = contentView.findViewById(R.id.option_invite_layout);

        optionView.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionStartConversation.setOnClickListener(this);
        optionInvite.setOnClickListener(this);

        View separatorInfo = contentView.findViewById(R.id.separator_info);

        if (isScreenInPortrait(requireContext())) {
            titleNameContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT));
            titleMailContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT));
        } else {
            titleNameContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND));
            titleMailContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND));
        }

        titleContact.setVisibility(View.VISIBLE);
        separatorTitleContact.setVisibility(View.VISIBLE);

        long userCount = message.getMessage().getUsersCount();
        if (userCount == 1) {
            Timber.d("One contact attached");
            optionView.setVisibility(View.GONE);
            optionInfo.setVisibility(View.VISIBLE);

            long userHandle = message.getMessage().getUserHandle(0);
            setContactStatus(getUserStatus(userHandle), stateIcon, StatusIconLocation.DRAWER);

            if (userHandle != megaChatApi.getMyUserHandle()) {

                String userName = message.getMessage().getUserName(0);
                titleNameContactChatPanel.setText(userName);

                String userEmail = message.getMessage().getUserEmail(0);
                titleMailContactChatPanel.setText(userEmail);
                MegaUser contact = megaApi.getContact(userEmail);

                if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    optionInfo.setVisibility(View.VISIBLE);

                    //Check if the contact is the same that the one is chatting
                    MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                    if (!chatRoom.isGroup()) {
                        long contactHandle = message.getMessage().getUserHandle(0);
                        long messageContactHandle = chatRoom.getPeerHandle(0);
                        if (contactHandle == messageContactHandle) {
                            optionStartConversation.setVisibility(View.GONE);
                        } else {
                            optionStartConversation.setVisibility(View.VISIBLE);
                        }
                    } else {
                        optionStartConversation.setVisibility(View.VISIBLE);
                    }
                    optionInvite.setVisibility(View.GONE);
                    userHandle = contact.getHandle();
                } else {
                    optionInfo.setVisibility(View.GONE);
                    optionStartConversation.setVisibility(View.GONE);
                    optionInvite.setVisibility(View.VISIBLE);
                }
                setImageAvatar(userHandle, userEmail, userName, contactImageView);
            }
        } else {
            MegaUser contact;
            if (email == null) {
                optionView.setVisibility(View.VISIBLE);
                optionInfo.setVisibility(View.GONE);

                stateIcon.setVisibility(View.GONE);

                StringBuilder name = new StringBuilder();
                name.append(message.getMessage().getUserName(0));
                for (int i = 1; i < userCount; i++) {
                    name.append(", ").append(message.getMessage().getUserName(i));
                }

                optionInvite.setVisibility(View.GONE);

                for (int i = 1; i < userCount; i++) {
                    String userEmail = message.getMessage().getUserEmail(i);
                    contact = megaApi.getContact(userEmail);

                    if (contact != null && contact.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
                        optionStartConversation.setVisibility(View.GONE);
                        optionInvite.setVisibility(View.VISIBLE);
                        break;
                    }
                }

                titleMailContactChatPanel.setText(name);

                String email = getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int) userCount, userCount);
                titleNameContactChatPanel.setText(email);

                setImageAvatar(INVALID_HANDLE, null, userCount + "", contactImageView);
            } else {
                optionView.setVisibility(View.GONE);

                stateIcon.setVisibility(View.VISIBLE);

                position = getPositionByMail(email);
                if (position == -1) {
                    Timber.w("Error - position -1");
                    return;
                }

                optionStartConversation.setVisibility(View.VISIBLE);
                optionInvite.setVisibility(View.GONE);

                String email = message.getMessage().getUserEmail(position);
                titleMailContactChatPanel.setText(email);

                long userHandle = message.getMessage().getUserHandle(position);
                String name = message.getMessage().getUserName(position);
                if (isTextEmpty(name)) {
                    name = chatC.getParticipantFullName(userHandle);
                    if (name.trim().isEmpty()) {
                        name = email;
                    }
                }

                titleNameContactChatPanel.setText(name);

                contact = megaApi.getContact(email);
                if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    optionInfo.setVisibility(View.VISIBLE);
                    //Check if the contact is the same that the one is chatting
                    MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                    if (!chatRoom.isGroup() && chatRoom.getPeerHandle(0) == contact.getHandle()) {
                        optionStartConversation.setVisibility(View.GONE);
                    } else {
                        optionStartConversation.setVisibility(View.VISIBLE);
                    }

                    optionInvite.setVisibility(View.GONE);
                } else {
                    optionInfo.setVisibility(View.GONE);
                    optionStartConversation.setVisibility(View.GONE);
                    optionInvite.setVisibility(View.VISIBLE);
                }

                setImageAvatar(userHandle, email, name, contactImageView);
            }
        }

        separatorInfo.setVisibility((optionView.getVisibility() == View.VISIBLE ||
                optionInfo.getVisibility() == View.VISIBLE) && (optionStartConversation.getVisibility() == View.VISIBLE ||
                optionInvite.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if (message == null) {
            Timber.w("Error. The message is NULL");
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        long numUsers = message.getMessage().getUsersCount();

        int id = v.getId();
        if (id == R.id.option_info_layout) {
            if (!isOnline(requireContext())) {
                ((ChatActivity) requireActivity()).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                return;
            }

            long contactHandle = MEGACHAT_INVALID_HANDLE;
            String contactEmail = null;
            if (requireActivity() instanceof ChatActivity) {
                contactEmail = message.getMessage().getUserEmail(0);
                contactHandle = message.getMessage().getUserHandle(0);
            } else if (position != -1) {
                contactEmail = message.getMessage().getUserEmail(position);
                contactHandle = message.getMessage().getUserHandle(position);
            } else {
                Timber.w("Error - position -1");
            }

            if (contactHandle != MEGACHAT_INVALID_HANDLE) {
                MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                boolean isChatRoomOpen = chatRoom != null && !chatRoom.isGroup() && contactHandle == chatRoom.getPeerHandle(0);
                ContactUtil.openContactInfoActivity(requireActivity(), contactEmail, isChatRoomOpen);
            }
        } else if (id == R.id.option_view_layout) {
            Timber.d("View option");
            ContactUtil.openContactAttachmentActivity(requireActivity(), chatId, messageId);
        } else if (id == R.id.option_invite_layout) {
            if (!isOnline(requireContext())) {
                ((ChatActivity) requireActivity()).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                return;
            }

            ContactController cC = new ContactController(requireActivity());
            ArrayList<String> contactEmails;

            if (requireActivity() instanceof ChatActivity) {
                if (numUsers == 1) {
                    cC.inviteContact(message.getMessage().getUserEmail(0));
                } else {
                    Timber.d("Num users to invite: %s", numUsers);
                    contactEmails = new ArrayList<>();

                    for (int j = 0; j < numUsers; j++) {
                        String userMail = message.getMessage().getUserEmail(j);
                        contactEmails.add(userMail);
                    }
                    cC.inviteMultipleContacts(contactEmails);
                }
            } else if (email != null) {
                cC.inviteContact(email);
            }
        } else if (id == R.id.option_start_conversation_layout) {
            if (requireActivity() instanceof ChatActivity) {
                if (numUsers == 1) {
                    ((ChatActivity) requireActivity()).startConversation(message.getMessage().getUserHandle(0));
                    dismissAllowingStateLoss();
                } else {
                    Timber.d("Num users to invite: %s", numUsers);
                    ArrayList<Long> contactHandles = new ArrayList<>();

                    for (int j = 0; j < numUsers; j++) {
                        long userHandle = message.getMessage().getUserHandle(j);
                        contactHandles.add(userHandle);
                    }
                    ((ChatActivity) requireActivity()).startGroupConversation(contactHandles);
                }
            } else {
                Timber.d("Instance of ContactAttachmentActivity");
                Timber.d("position: %s", position);
                long userHandle = message.getMessage().getUserHandle(position);
                ((ContactAttachmentActivity) requireActivity()).startConversation(userHandle);
            }
        }

        setStateBottomSheetBehaviorHidden();
    }

    private int getPositionByMail(String email) {
        long userCount = message.getMessage().getUsersCount();
        for (int i = 0; i < userCount; i++) {
            if (message.getMessage().getUserEmail(i).equals(email)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putString(EMAIL, email);
    }
}
