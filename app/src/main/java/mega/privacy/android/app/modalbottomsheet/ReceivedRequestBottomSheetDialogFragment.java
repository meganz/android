package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import nz.mega.sdk.MegaContactRequest;

import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class ReceivedRequestBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaContactRequest request;
    private ContactController cC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            long handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
            request = megaApi.getContactRequestByHandle(handle);
        } else if (context instanceof ManagerActivityLollipop) {
            request = ((ManagerActivityLollipop) context).getSelectedRequest();
        }

        cC = new ContactController(context);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        if (request == null) {
            logWarning("Request NULL");
            return;
        }

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_received_request, null);
        mainLinearLayout = contentView.findViewById(R.id.received_request_item_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        TextView titleNameContactChatPanel = contentView.findViewById(R.id.received_request_list_contact_name_text);
        TextView titleMailContactChatPanel = contentView.findViewById(R.id.received_request_list_contact_mail_text);
        RoundedImageView contactImageView = contentView.findViewById(R.id.sliding_received_request_list_thumbnail);
        contactImageView.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_PRIMARY_COLOR), request.getTargetEmail(), AVATAR_SIZE, true));

        titleNameContactChatPanel.setMaxWidth(scaleWidthPx(200, outMetrics));
        titleMailContactChatPanel.setMaxWidth(scaleWidthPx(200, outMetrics));

        contentView.findViewById(R.id.contact_list_option_accept_layout).setOnClickListener(this);
        contentView.findViewById(R.id.contact_list_option_ignore_layout).setOnClickListener(this);
        contentView.findViewById(R.id.contact_list_option_decline_layout).setOnClickListener(this);

        titleNameContactChatPanel.setText(request.getSourceEmail());
        titleMailContactChatPanel.setText(String.format("%s", DateUtils.getRelativeTimeSpanString(request.getCreationTime() * 1000)));

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    @Override
    public void onClick(View v) {
        if (request == null) {
            logWarning("Selected request NULL");
            return;
        }

        switch (v.getId()) {
            case R.id.contact_list_option_accept_layout:
                cC.acceptInvitationContact(request);
                break;

            case R.id.contact_list_option_ignore_layout:
                cC.ignoreInvitationContact(request);
                break;

            case R.id.contact_list_option_decline_layout:
                cC.declineInvitationContact(request);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        long handle = request.getHandle();
        outState.putLong(HANDLE, handle);
    }
}
