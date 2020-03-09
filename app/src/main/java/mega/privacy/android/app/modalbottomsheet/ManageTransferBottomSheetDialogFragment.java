package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;

public class ManageTransferBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private AndroidCompletedTransfer transfer;

    private ImageView thumbnail;
    private ImageView stateIcon;
    private LinearLayout viewInFolderOption;
    private LinearLayout getLinkOption;
    private LinearLayout clearOption;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            transfer = ((ManagerActivityLollipop) getActivity()).getSelectedTransfer();
        } else {

        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_manage_transfer, null);

        thumbnail = contentView.findViewById(R.id.manage_transfer_thumbnail);
        stateIcon = contentView.findViewById(R.id.manage_transfer_small_icon);
        viewInFolderOption = contentView.findViewById(R.id.manage_transfer_thumbnail);
        getLinkOption = contentView.findViewById(R.id.manage_transfer_thumbnail);
        clearOption = contentView.findViewById(R.id.manage_transfer_thumbnail);

        dialog.setContentView(contentView);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
