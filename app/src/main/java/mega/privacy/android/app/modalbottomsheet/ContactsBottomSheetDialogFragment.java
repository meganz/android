package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.ContactUtil;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;


public class ContactsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private static final String EXTRA_EMAIL = "EXTRA_EMAIL";
    private MegaContactAdapter contact = null;
    private ContactController cC;

    public static ContactsBottomSheetDialogFragment newInstance(String userEmail) {
        ContactsBottomSheetDialogFragment fragment = new ContactsBottomSheetDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_EMAIL, userEmail);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cC = new ContactController(context);

        if (savedInstanceState != null) {
            String email = savedInstanceState.getString(EMAIL);
            getContactFromEmail(email);
        } else if (getArguments() != null && getArguments().containsKey(EXTRA_EMAIL)) {
            String email = getArguments().getString(EXTRA_EMAIL);
            getContactFromEmail(email);
        } else if (context instanceof ManagerActivityLollipop) {
            contact = ((ManagerActivityLollipop) context).getSelectedUser();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        if (contact == null) {
            logWarning("Contact NULL");
        }

        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_item, null);
        mainLinearLayout = contentView.findViewById(R.id.contact_item_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout_bottom_sheet_contact);

        EmojiTextView titleNameContactPanel = contentView.findViewById(R.id.contact_list_contact_name_text);
        MarqueeTextView titleMailContactPanel = contentView.findViewById(R.id.contact_list_contact_mail_text);
        RoundedImageView contactImageView = contentView.findViewById(R.id.sliding_contact_list_thumbnail);

        LinearLayout optionInfoContact = contentView.findViewById(R.id.contact_list_info_contact_layout);
        LinearLayout optionStartCall = contentView.findViewById(R.id.contact_list_option_call_layout);
        LinearLayout optionStartConversation = contentView.findViewById(R.id.contact_list_option_start_conversation_layout);
        LinearLayout optionSendFile = contentView.findViewById(R.id.contact_list_option_send_file_layout);
        LinearLayout optionSendContact = contentView.findViewById(R.id.contact_list_option_send_contact_layout);
        LinearLayout optionShareFolder = contentView.findViewById(R.id.contact_list_option_share_layout);
        LinearLayout optionRemove = contentView.findViewById(R.id.contact_list_option_remove_layout);
        ImageView contactStateIcon = contentView.findViewById(R.id.contact_list_drawable_state);

        if (isScreenInPortrait(context)) {
            titleNameContactPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactPanel.setMaxWidth(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        } else {
            titleNameContactPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactPanel.setMaxWidth(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        optionInfoContact.setOnClickListener(this);
        optionRemove.setOnClickListener(this);
        optionSendFile.setOnClickListener(this);
        optionSendContact.setOnClickListener(this);
        optionShareFolder.setOnClickListener(this);

        optionSendFile.setVisibility(View.VISIBLE);
        optionStartConversation.setVisibility(View.VISIBLE);
        optionSendContact.setVisibility(View.VISIBLE);

        titleNameContactPanel.setText(contact.getFullName());

        int contactStatus = megaChatApi.getUserOnlineStatus(contact.getMegaUser().getHandle());
        setContactStatus(contactStatus, contactStateIcon, titleMailContactPanel, StatusIconLocation.DRAWER);
        setContactLastGreen(requireContext(), contactStatus, contact.getLastGreen(), titleMailContactPanel);

        if (isTextEmpty(titleMailContactPanel.getText().toString())) {
            titleMailContactPanel.setVisibility(View.GONE);
        }

        setImageAvatar(contact.getMegaUser().getHandle(), contact.getMegaUser().getEmail(), contact.getFullName(), contactImageView);

        optionStartConversation.setVisibility(View.VISIBLE);
        optionStartConversation.setOnClickListener(this);
        optionStartCall.setVisibility(View.VISIBLE);
        optionStartCall.setOnClickListener(participatingInACall() ? null : this);

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true);
    }

    @Override
    public void onClick(View v) {

        if (app.getStorageState() == STORAGE_STATE_PAYWALL && v.getId() != R.id.contact_list_info_contact_layout) {
            showOverDiskQuotaPaywallWarning();
            setStateBottomSheetBehaviorHidden();
            return;
        }

        if (contact == null) {
            logWarning("Selected contact NULL");
            return;
        }

        List<MegaUser> user = new ArrayList<>();
        user.add(contact.getMegaUser());

        switch (v.getId()) {
            case R.id.contact_list_info_contact_layout:
                ContactUtil.openContactInfoActivity(context, contact.getMegaUser().getEmail());
                break;

            case R.id.contact_list_option_start_conversation_layout:
                startOneToOneChat(contact.getMegaUser());
                break;

            case R.id.contact_list_option_call_layout:
                startNewCall(getActivity(), (SnackbarShower) context, contact.getMegaUser());
                break;

            case R.id.contact_list_option_send_file_layout:
                cC.pickFileToSend(user);
                break;

            case R.id.contact_list_option_send_contact_layout:
                new ChatController(context).selectChatsToAttachContact(contact.getMegaUser());
                break;

            case R.id.contact_list_option_share_layout:
                cC.pickFolderToShare(user);
                break;

            case R.id.contact_list_option_remove_layout:
                showConfirmationRemoveContact(contact.getMegaUser());
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String email = contact.getMegaUser().getEmail();
        outState.putString(EMAIL, email);
    }

    private void getContactFromEmail(String email) {
        if (email != null) {
            MegaUser megaUser = megaApi.getContact(email);
            String fullName = getMegaUserNameDB(megaUser);
            if (fullName == null) {
                fullName = megaUser.getEmail();
            }
            contact = new MegaContactAdapter(getContactDB(megaUser.getHandle()), megaUser, fullName);
        }
    }

    private void startOneToOneChat(MegaUser user) {
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
        MegaChatPeerList peers = MegaChatPeerList.createInstance();

        if (chat == null) {
            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, null);
        } else {
            Intent intentOpenChat = new Intent(context, ChatActivityLollipop.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra(CHAT_ID, chat.getChatId());
            startActivity(intentOpenChat);
        }
    }

    private void showConfirmationRemoveContact(MegaUser megaUser) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(getResources().getQuantityString(R.plurals.title_confirmation_remove_contact, 1))
                .setMessage(getResources().getQuantityString(R.plurals.confirmation_remove_contact, 1))
                .setPositiveButton(R.string.general_remove, (dialog, which) -> cC.removeContact(megaUser))
                .setNegativeButton(R.string.general_cancel, null)
                .show();
    }
}
