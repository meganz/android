package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.jeremyliao.liveeventbus.LiveEventBus;
import com.jeremyliao.liveeventbus.core.LiveEvent;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;

import static mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_REMOVE_PHONE_NUMBER_CONFIRMATION;

public class PhoneNumberBottomSheetDialogFragmentOld extends BaseBottomSheetDialogFragment {

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_phonenumber, null);
        mainLinearLayout = contentView.findViewById(R.id.phonenumber_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        contentView.findViewById(R.id.modify_phonenumber_layout).setOnClickListener(v -> {
            LiveEventBus.get(EVENT_SHOW_REMOVE_PHONE_NUMBER_CONFIRMATION, Boolean.class).post(true);
            setStateBottomSheetBehaviorHidden();
        });

        contentView.findViewById(R.id.remove_phonenumber_layout).setOnClickListener(v -> {
            LiveEventBus.get(EVENT_SHOW_REMOVE_PHONE_NUMBER_CONFIRMATION, Boolean.class).post(false);
            setStateBottomSheetBehaviorHidden();
        });

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }
}
