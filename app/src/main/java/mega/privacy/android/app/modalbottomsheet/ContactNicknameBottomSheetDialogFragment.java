package mega.privacy.android.app.modalbottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;

import static mega.privacy.android.app.utils.Constants.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ContactNicknameBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private String nickname;
    private ContactInfoActivityLollipop contactInfoActivityLollipop = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_nickname, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        if (savedInstanceState != null) {
            nickname = savedInstanceState.getString(EXTRA_USER_NICKNAME, null);
        } else if (requireActivity() instanceof ContactInfoActivityLollipop) {
            contactInfoActivityLollipop = ((ContactInfoActivityLollipop) requireActivity());
            nickname = contactInfoActivityLollipop.getNickname();
        }

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        contentView.findViewById(R.id.edit_nickname_layout).setOnClickListener(this);
        contentView.findViewById(R.id.remove_nickname_layout).setOnClickListener(this);
        super.onViewCreated(view, savedInstanceState);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_USER_NICKNAME, nickname);
    }
}
