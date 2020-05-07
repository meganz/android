package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class FileContactsListBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaUser contact = null;
    private MegaShare share = null;
    private String nonContactEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String email = savedInstanceState.getString(EMAIL);
            if (email != null) {
                contact = megaApi.getContact(email);
                if (contact == null) {
                    nonContactEmail = email;
                }
            }
        } else {
            if (context instanceof FileContactListActivityLollipop) {
                share = ((FileContactListActivityLollipop) context).getSelectedShare();
                contact = ((FileContactListActivityLollipop) context).getSelectedContact();
            } else if (context instanceof FileInfoActivityLollipop) {
                share = ((FileInfoActivityLollipop) context).getSelectedShare();
                contact = ((FileInfoActivityLollipop) context).getSelectedContact();
            }

            if (contact == null) {
                nonContactEmail = share.getUser();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_file_contact_list, null);

        mainLinearLayout = contentView.findViewById(R.id.file_contact_list_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        EmojiTextView titleNameContactPanel = contentView.findViewById(R.id.file_contact_list_contact_name_text);
        TextView titleMailContactPanel = contentView.findViewById(R.id.file_contact_list_contact_mail_text);
        RoundedImageView contactImageView = contentView.findViewById(R.id.sliding_file_contact_list_thumbnail);

        LinearLayout optionChangePermissions = contentView.findViewById(R.id.file_contact_list_option_permissions_layout);
        LinearLayout optionDelete = contentView.findViewById(R.id.file_contact_list_option_delete_layout);
        LinearLayout optionInfo = contentView.findViewById(R.id.file_contact_list_option_info_layout);

        optionChangePermissions.setOnClickListener(this);
        optionDelete.setOnClickListener(this);
        optionInfo.setOnClickListener(this);

        titleNameContactPanel.setMaxWidthEmojis(scaleWidthPx(200, outMetrics));
        titleMailContactPanel.setMaxWidth(scaleWidthPx(200, outMetrics));

        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);

        String fullName = contact != null ? getMegaUserNameDB(contact) : nonContactEmail;

        if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
            optionInfo.setVisibility(View.VISIBLE);
            separatorInfo.setVisibility(View.VISIBLE);
        } else {
            optionInfo.setVisibility(View.GONE);
            separatorInfo.setVisibility(View.GONE);
        }

        titleNameContactPanel.setText(fullName);
        setImageAvatar(contact, contact != null ? contact.getEmail() : nonContactEmail, fullName, contactImageView);

        if (share != null) {
            int accessLevel = share.getAccess();
            switch (accessLevel) {
                case MegaShare.ACCESS_OWNER:
                case MegaShare.ACCESS_FULL:
                    titleMailContactPanel.setText(context.getString(R.string.file_properties_shared_folder_full_access));
                    break;

                case MegaShare.ACCESS_READ:
                    titleMailContactPanel.setText(context.getString(R.string.file_properties_shared_folder_read_only));
                    break;

                case MegaShare.ACCESS_READWRITE:
                    titleMailContactPanel.setText(context.getString(R.string.file_properties_shared_folder_read_write));
                    break;
            }

            if (share.isPending()) {
                titleMailContactPanel.append(" " + getString(R.string.pending_outshare_indicator));
            }
        } else {
            titleMailContactPanel.setText(contact.getEmail());
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    @Override
    public void onClick(View v) {
        if (contact == null) {
            logWarning("Selected contact NULL");
            return;
        }

        switch (v.getId()) {
            case R.id.file_contact_list_option_permissions_layout:
                if (context instanceof FileContactListActivityLollipop) {
                    ((FileContactListActivityLollipop) context).changePermissions();
                } else if (context instanceof FileInfoActivityLollipop) {
                    ((FileInfoActivityLollipop) context).changePermissions();
                }
                break;

            case R.id.file_contact_list_option_delete_layout:
                if (context instanceof FileContactListActivityLollipop) {
                    ((FileContactListActivityLollipop) context).removeFileContactShare();
                } else if (context instanceof FileInfoActivityLollipop) {
                    ((FileInfoActivityLollipop) context).removeFileContactShare();
                }
                break;

            case R.id.file_contact_list_option_info_layout:
                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra(NAME, share.getUser());
                context.startActivity(i);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String email = contact.getEmail();
        outState.putString(EMAIL, email);
    }
}
