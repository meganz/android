package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaTransfer;

public class TransfersBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_transfers, null);
        mainLinearLayout = contentView.findViewById(R.id.transfers_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        ImageView iconPause = contentView.findViewById(R.id.transfers_option_pause);
        TextView textPause = contentView.findViewById(R.id.transfers_option_pause_text);

        if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
            iconPause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play));
            textPause.setText(getString(R.string.option_to_resume_transfers));
        }
        else{
            iconPause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pause));
            textPause.setText(getString(R.string.option_to_pause_transfers));
        }

        contentView.findViewById(R.id.transfers_manager_option_layout).setOnClickListener(this);
        contentView.findViewById(R.id.transfers_pause_layout).setOnClickListener(this);
        contentView.findViewById(R.id.transfers_clear_layout).setOnClickListener(this);
        contentView.findViewById(R.id.transfers_cancel_layout).setOnClickListener(this);

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.transfers_manager_option_layout:
                ((ManagerActivityLollipop) getActivity()).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.TRANSFERS);
                dismissAllowingStateLoss();
                break;

            case R.id.transfers_pause_layout:
                ((ManagerActivityLollipop) context).changeTransfersStatus();
                break;

            case R.id.transfers_clear_layout:
                ((ManagerActivityLollipop) context).showConfirmationClearCompletedTransfers();
                break;

            case R.id.transfers_cancel_layout:
                ((ManagerActivityLollipop) context).showConfirmationCancelAllTransfers();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }
}
