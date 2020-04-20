package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity;

public class RecoveryKeyBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        AccountController aC = new AccountController(getContext());

        switch(v.getId()){
            case R.id.recovery_key_copytoclipboard_layout:
                if (getContext() instanceof TwoFactorAuthenticationActivity) {
                    ((TwoFactorAuthenticationActivity) getContext()).finish();
                }

                aC.copyRkToClipboard();
                break;

            case R.id.recovery_key_saveTo_fileSystem_layout:
                aC.saveRkToFileSystem();
                break;

            case R.id.recovery_key_print_layout:
                aC.printRK();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_recovery_key, null);
        mainLinearLayout = contentView.findViewById(R.id.recovery_key_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        contentView.findViewById(R.id.recovery_key_print_layout).setOnClickListener(this);
        contentView.findViewById(R.id.recovery_key_copytoclipboard_layout).setOnClickListener(this);
        contentView.findViewById(R.id.recovery_key_saveTo_fileSystem_layout).setOnClickListener(this);


        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }
}
