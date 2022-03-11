package mega.privacy.android.app.modalbottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.controllers.AccountController;
import mega.privacy.android.app.main.TwoFactorAuthenticationActivity;

public class RecoveryKeyBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(requireContext(), R.layout.bottom_sheet_recovery_key, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        contentView.findViewById(R.id.recovery_key_print_layout).setOnClickListener(this);
        contentView.findViewById(R.id.recovery_key_copytoclipboard_layout).setOnClickListener(this);
        contentView.findViewById(R.id.recovery_key_saveTo_fileSystem_layout).setOnClickListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.recovery_key_copytoclipboard_layout:
                new AccountController(requireActivity()).copyRkToClipboard();

                if (requireActivity() instanceof TwoFactorAuthenticationActivity) {
                    ((TwoFactorAuthenticationActivity) requireActivity()).finish();
                }
                break;

            case R.id.recovery_key_saveTo_fileSystem_layout:
                AccountController.saveRkToFileSystem(requireActivity());
                break;

            case R.id.recovery_key_print_layout:
                new AccountController(requireActivity()).printRK();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }
}
