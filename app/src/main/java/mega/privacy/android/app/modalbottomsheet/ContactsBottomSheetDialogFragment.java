package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;


public class ContactsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaContactAdapter contact = null;
    private ContactController cC;

    private EmojiTextView titleNameContactPanel;
    private TextView titleMailContactPanel;
    private RoundedImageView contactImageView;
    private LinearLayout optionInfoContact;
    private LinearLayout optionStartConversation;
    private LinearLayout optionSendFile;
    private LinearLayout optionSendContact;
    private LinearLayout optionShareFolder;
    private LinearLayout optionRemove;
    private ImageView contactStateIcon;

    private String fullName = "";

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

        titleNameContactPanel = contentView.findViewById(R.id.contact_list_contact_name_text);
        titleMailContactPanel = contentView.findViewById(R.id.contact_list_contact_mail_text);
        contactImageView = contentView.findViewById(R.id.sliding_contact_list_thumbnail);
        optionInfoContact = contentView.findViewById(R.id.contact_list_info_contact_layout);
        optionStartConversation = contentView.findViewById(R.id.contact_list_option_start_conversation_layout);
        optionSendFile = contentView.findViewById(R.id.contact_list_option_send_file_layout);
        optionSendContact = contentView.findViewById(R.id.contact_list_option_send_contact_layout);
        optionShareFolder = contentView.findViewById(R.id.contact_list_option_share_layout);
        optionRemove = contentView.findViewById(R.id.contact_list_option_remove_layout);
        contactStateIcon = contentView.findViewById(R.id.contact_list_drawable_state);

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

        fullName = contact.getFullName();
        titleNameContactPanel.setText(fullName);

        ArrayList<MegaNode> sharedNodes = megaApi.getInShares(contact.getMegaUser());
        String sharedNodesDescription = getSubtitleDescription(sharedNodes);
        titleMailContactPanel.setText(sharedNodesDescription);

        addAvatarContactPanel(contact);

        optionStartConversation.setVisibility(View.VISIBLE);
        optionStartConversation.setOnClickListener(this);

        contactStateIcon.setVisibility(View.VISIBLE);
        if (megaChatApi != null) {
            int userStatus = megaChatApi.getUserOnlineStatus(contact.getMegaUser().getHandle());
            contactStateIcon.setVisibility(View.VISIBLE);

            switch (userStatus) {
                case MegaChatApi.STATUS_ONLINE:
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_online));
                    break;

                case MegaChatApi.STATUS_AWAY:
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_away));
                    break;

                case MegaChatApi.STATUS_BUSY:
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_busy));
                    break;

                case MegaChatApi.STATUS_OFFLINE:
                    contactStateIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.circle_status_contact_offline));
                    break;

                case MegaChatApi.STATUS_INVALID:
                default:
                    contactStateIcon.setVisibility(View.GONE);
            }
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true);
    }

    public void addAvatarContactPanel(MegaContactAdapter contact) {
        /*Default Avatar*/
        contactImageView.setImageBitmap(getDefaultAvatar(getColorAvatar(contact.getMegaUser()), contact.getFullName(), AVATAR_SIZE, true));

        /*Avatar*/
        String contactMail = contact.getMegaUser().getEmail();
        File avatar = buildAvatarFile(getActivity(), contactMail + ".jpg");
        Bitmap bitmap;

        if (isFileAvailable(avatar) && avatar.length() > 0) {
            BitmapFactory.Options bOpts = new BitmapFactory.Options();
            bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
            if (bitmap == null) {
                avatar.delete();
            } else {
                contactImageView.setImageBitmap(bitmap);
                return;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (contact == null) {
            logWarning("Selected contact NULL");
            return;
        }

        List<MegaUser> user = new ArrayList<>();
        user.add(contact.getMegaUser());

        switch (v.getId()) {
            case R.id.contact_list_info_contact_layout:
                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra("name", contact.getMegaUser().getEmail());
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
