package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;

import static mega.privacy.android.app.utils.Constants.*;

public class ContactNicknameBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {
    private String nickname;
    private ContactInfoActivityLollipop contactInfoActivityLollipop = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            nickname = savedInstanceState.getString(EXTRA_USER_NICKNAME, null);
        } else if (context instanceof ContactInfoActivityLollipop) {
            contactInfoActivityLollipop = ((ContactInfoActivityLollipop) context);
            nickname = contactInfoActivityLollipop.getNickname();
        }
    }

    @Override
    public void onClick(View v) {
        if (contactInfoActivityLollipop == null) return;
        switch (v.getId()) {
            case R.id.edit_nickname_layout:
                contactInfoActivityLollipop.showConfirmationSetNickname(nickname);
                break;

            case R.id.remove_nickname_layout:
                contactInfoActivityLollipop.addNickname(null, null);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_nickname, null);
        mainLinearLayout = contentView.findViewById(R.id.nickname_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);
        contentView.findViewById(R.id.edit_nickname_layout).setOnClickListener(this);
        contentView.findViewById(R.id.remove_nickname_layout).setOnClickListener(this);
        dialog.setContentView(contentView);

        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_USER_NICKNAME, nickname);
    }
}
