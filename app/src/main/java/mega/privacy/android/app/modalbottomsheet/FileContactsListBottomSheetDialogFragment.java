package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
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
        } else if (requireActivity() instanceof FileContactListActivityLollipop) {
            share = ((FileContactListActivityLollipop) requireActivity()).getSelectedShare();
            contact = ((FileContactListActivityLollipop) requireActivity()).getSelectedContact();

            if (contact == null) {
                nonContactEmail = share.getUser();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        if (requireActivity() instanceof FileInfoActivityLollipop) {
            share = ((FileInfoActivityLollipop) requireActivity()).getSelectedShare();
            contact = ((FileInfoActivityLollipop) requireActivity()).getSelectedContact();

            if (contact == null) {
                nonContactEmail = share.getUser();
            }
        }

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_file_contact_list, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        EmojiTextView titleNameContactPanel = contentView.findViewById(R.id.file_contact_list_contact_name_text);
        TextView titleMailContactPanel = contentView.findViewById(R.id.file_contact_list_contact_mail_text);
        RoundedImageView contactImageView = contentView.findViewById(R.id.sliding_file_contact_list_thumbnail);

        LinearLayout optionChangePermissions = contentView.findViewById(R.id.file_contact_list_option_permissions_layout);
        LinearLayout optionDelete = contentView.findViewById(R.id.file_contact_list_option_delete_layout);
        LinearLayout optionInfo = contentView.findViewById(R.id.file_contact_list_option_info_layout);

        optionChangePermissions.setOnClickListener(this);
        optionDelete.setOnClickListener(this);
        optionInfo.setOnClickListener(this);

        titleNameContactPanel.setMaxWidthEmojis(scaleWidthPx(200, getResources().getDisplayMetrics()));
        titleMailContactPanel.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));

        View separatorInfo = contentView.findViewById(R.id.separator_info);

        String fullName = contact != null ? getMegaUserNameDB(contact) : nonContactEmail;

        if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
            optionInfo.setVisibility(View.VISIBLE);
            separatorInfo.setVisibility(View.VISIBLE);
        } else {
            optionInfo.setVisibility(View.GONE);
            separatorInfo.setVisibility(View.GONE);
        }

        titleNameContactPanel.setText(fullName);
        setImageAvatar(contact != null ? contact.getHandle() : INVALID_ID, contact != null ? contact.getEmail() : nonContactEmail, fullName, contactImageView);

        if (share != null) {
            int accessLevel = share.getAccess();
            switch (accessLevel) {
                case MegaShare.ACCESS_OWNER:
                case MegaShare.ACCESS_FULL:
                    titleMailContactPanel.setText(StringResourcesUtils.getString(R.string.file_properties_shared_folder_full_access));
                    break;

                case MegaShare.ACCESS_READ:
                    titleMailContactPanel.setText(StringResourcesUtils.getString(R.string.file_properties_shared_folder_read_only));
                    break;

                case MegaShare.ACCESS_READWRITE:
                    titleMailContactPanel.setText(StringResourcesUtils.getString(R.string.file_properties_shared_folder_read_write));
                    break;
            }

            if (share.isPending()) {
                titleMailContactPanel.append(" " + getString(R.string.pending_outshare_indicator));
            }
        } else {
            titleMailContactPanel.setText(contact != null ? contact.getEmail() : nonContactEmail);
        }

        dialog.setContentView(contentView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.file_contact_list_option_permissions_layout:
                if (requireActivity() instanceof FileContactListActivityLollipop) {
                    ((FileContactListActivityLollipop) requireActivity()).changePermissions();
                } else if (requireActivity() instanceof FileInfoActivityLollipop) {
                    ((FileInfoActivityLollipop) requireActivity()).changePermissions();
                }
                break;

            case R.id.file_contact_list_option_delete_layout:
                if (requireActivity() instanceof FileContactListActivityLollipop) {
                    ((FileContactListActivityLollipop) requireActivity()).removeFileContactShare();
                } else if (requireActivity() instanceof FileInfoActivityLollipop) {
                    ((FileInfoActivityLollipop) requireActivity()).removeFileContactShare();
                }
                break;

            case R.id.file_contact_list_option_info_layout:
                ContactUtil.openContactInfoActivity(requireActivity(), share.getUser());
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String email = contact == null ? nonContactEmail : contact.getEmail();
        outState.putString(EMAIL, email);
    }
}
