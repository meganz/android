package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.ContactUtil;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.AvatarUtil.setImageAvatar;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.CallUtil.startNewCall;
import static mega.privacy.android.app.utils.ChatUtil.setContactLastGreen;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.Constants.EMAIL;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT;
import static mega.privacy.android.app.utils.ContactUtil.getContactDB;
import static mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;


public class MeetingBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private ContactController cC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cC = new ContactController(context);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_meeting, null);
        mainLinearLayout = contentView.findViewById(R.id.meeting_bottom_sheet);
        items_layout = contentView.findViewById(R.id.meeting_item);
        ImageView startMeeting = contentView.findViewById(R.id.iv_start_meeting);
        ImageView joinMeeting = contentView.findViewById(R.id.iv_join_meeting);
        startMeeting.setOnClickListener(this);
        joinMeeting.setOnClickListener(this);
        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.iv_start_meeting:
//                ContactUtil.openContactInfoActivity(context, contact.getMegaUser().getEmail());
                break;

            case R.id.iv_join_meeting:
//                ((ManagerActivityLollipop) context).startOneToOneChat(contact.getMegaUser());
                break;


        }
        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}
