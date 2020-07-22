package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;


public class ContactsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaContactAdapter contact = null;
    private ContactController cC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cC = new ContactController(context);

        if (savedInstanceState != null) {
            String email = savedInstanceState.getString(EMAIL);
            if (email != null) {
                MegaUser megaUser = megaApi.getContact(email);
                String fullName = getMegaUserNameDB(megaUser);
                if (fullName == null) {
                    fullName = megaUser.getEmail();
                }
                contact = new MegaContactAdapter(getContactDB(megaUser.getHandle()), megaUser, fullName);
            }
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
        LinearLayout optionStartConversation = contentView.findViewById(R.id.contact_list_option_start_conversation_layout);
        LinearLayout optionSendFile = contentView.findViewById(R.id.contact_list_option_send_file_layout);
        LinearLayout optionSendContact = contentView.findViewById(R.id.contact_list_option_send_contact_layout);
        LinearLayout optionShareFolder = contentView.findViewById(R.id.contact_list_option_share_layout);
        LinearLayout optionRemove = contentView.findViewById(R.id.contact_list_option_remove_layout);
        ImageView contactStateIcon = contentView.findViewById(R.id.contact_list_drawable_state);

        if (isScreenInPortrait(context)) {
            titleNameContactPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        } else {
            titleNameContactPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
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
        setContactStatus(contactStatus, contactStateIcon, titleMailContactPanel);
        setContactLastGreen(requireContext(), contactStatus, contact.getLastGreen(), titleMailContactPanel);

        if (isTextEmpty(titleMailContactPanel.getText().toString())) {
            titleMailContactPanel.setVisibility(View.GONE);
        }

        setImageAvatar(contact.getMegaUser(), contact.getMegaUser().getEmail(), contact.getFullName(), contactImageView);

        optionStartConversation.setVisibility(View.VISIBLE);
        optionStartConversation.setOnClickListener(this);

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
                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra(NAME, contact.getMegaUser().getEmail());
                context.startActivity(i);
                break;

            case R.id.contact_list_option_start_conversation_layout:
                ((ManagerActivityLollipop) context).startOneToOneChat(contact.getMegaUser());
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
                ((ManagerActivityLollipop) context).showConfirmationRemoveContact(contact.getMegaUser());
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
}
