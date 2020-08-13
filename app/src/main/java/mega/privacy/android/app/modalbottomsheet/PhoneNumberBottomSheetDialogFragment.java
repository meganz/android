package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;

public class PhoneNumberBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MyAccountFragmentLollipop myAccountFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            myAccountFragment = (MyAccountFragmentLollipop) getActivity().getSupportFragmentManager().findFragmentByTag(ManagerActivityLollipop.FragmentTag.MY_ACCOUNT.getTag());
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_phonenumber, null);
        mainLinearLayout = contentView.findViewById(R.id.phonenumber_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);
        contentView.findViewById(R.id.modify_phonenumber_layout).setOnClickListener(this);
        contentView.findViewById(R.id.remove_phonenumber_layout).setOnClickListener(this);
        dialog.setContentView(contentView);

        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.modify_phonenumber_layout:
                myAccountFragment.showConfirmRemovePhoneNumberDialog(true);
                break;
            case R.id.remove_phonenumber_layout:
                myAccountFragment.showConfirmRemovePhoneNumberDialog(false);
                break;
        }
        setStateBottomSheetBehaviorHidden();
    }
}
