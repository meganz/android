package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.MimeTypeList.*;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaTransfer.*;

public class ManageTransferBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {
    private static final String TRANSFER_ID = "TRANSFER_ID";

    private ManagerActivityLollipop managerActivity;

    private AndroidCompletedTransfer transfer;
    private long handle;
    private long transferId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        managerActivity = (ManagerActivityLollipop) context;

        if (savedInstanceState == null) {
            transfer = managerActivity.getSelectedTransfer();
            transferId = transfer.getId();
        } else {
            transferId = savedInstanceState.getInt(TRANSFER_ID, -1);
            transfer = dbH.getcompletedTransfer(transferId);
        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_manage_transfer, null);
        mainLinearLayout = contentView.findViewById(R.id.manage_transfer_bottom_sheet);
        items_layout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

        if (transfer == null) return;

        managerActivity = (ManagerActivityLollipop) getActivity();

        ImageView thumbnail = contentView.findViewById(R.id.manage_transfer_thumbnail);
        ImageView type = contentView.findViewById(R.id.manage_transfer_small_icon);
        ImageView stateIcon = contentView.findViewById(R.id.manage_transfer_completed_image);
        TextView name = contentView.findViewById(R.id.manage_transfer_filename);
        TextView location = contentView.findViewById(R.id.manage_transfer_location);

        LinearLayout viewInFolderOption = contentView.findViewById(R.id.option_view_layout);
        viewInFolderOption.setOnClickListener(this);
        LinearLayout getLinkOption = contentView.findViewById(R.id.option_get_link_layout);
        getLinkOption.setOnClickListener(this);
        LinearLayout clearOption = contentView.findViewById(R.id.option_clear_layout);
        clearOption.setOnClickListener(this);
        LinearLayout retryOption = contentView.findViewById(R.id.option_retry_layout);
        retryOption.setOnClickListener(this);

        name.setText(transfer.getFileName());

        if (transfer.getType() == TYPE_DOWNLOAD) {
            type.setImageResource(R.drawable.ic_download_transfers);
            getLinkOption.setVisibility(View.GONE);
        } else if (transfer.getType() == TYPE_UPLOAD) {
            type.setImageResource(R.drawable.ic_upload_transfers);
        }

        location.setTextColor(ContextCompat.getColor(context, R.color.file_list_second_row));
        RelativeLayout.LayoutParams params =  (RelativeLayout.LayoutParams) stateIcon.getLayoutParams();
        params.rightMargin = dp2px(5, context.getResources().getDisplayMetrics());

        switch (transfer.getState()) {
            case STATE_COMPLETED:
                location.setText(transfer.getPath());
                stateIcon.setImageResource(R.drawable.ic_complete_transfer);
                retryOption.setVisibility(View.GONE);
                break;

            case STATE_FAILED:
                location.setTextColor(ContextCompat.getColor(context, R.color.expired_red));
                location.setText(String.format("%s: %s", context.getString(R.string.failed_label), transfer.getError()));
                params.rightMargin = 0;
                stateIcon.setImageBitmap(null);
                viewInFolderOption.setVisibility(View.GONE);
                getLinkOption.setVisibility(View.GONE);
                break;

            case STATE_CANCELLED:
                location.setText(R.string.transfer_cancelled);
                params.rightMargin = 0;
                stateIcon.setImageBitmap(null);
                viewInFolderOption.setVisibility(View.GONE);
                getLinkOption.setVisibility(View.GONE);
                break;

            default:
                location.setText(R.string.transfer_unknown);
                stateIcon.setImageResource(R.drawable.ic_queue);
                break;
        }

        if ((getLinkOption.getVisibility() == View.GONE && retryOption.getVisibility() == View.GONE) || viewInFolderOption.getVisibility() == View.GONE) {
            contentView.findViewById(R.id.separator_get_link).setVisibility(View.GONE);
        }

        stateIcon.setLayoutParams(params);

        thumbnail.setImageResource(typeForName(transfer.getFileName()).getIconResourceId());

        handle = Long.parseLong(transfer.getNodeHandle());

        if (typeForName(transfer.getFileName()).isImage() || typeForName(transfer.getFileName()).isVideo()) {
            Bitmap thumb = getThumbnailFromCache(handle);
            if (thumb == null) {
                MegaNode node = MegaApplication.getInstance().getMegaApi().getNodeByHandle(handle);
                thumb = getThumbnailFromFolder(node, getContext());
            }

            if (thumb != null) {
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) thumbnail.getLayoutParams();
                params1.height = params1.width = dp2px(36, outMetrics);
                params1.setMargins(54, 0, 18, 0);
                thumbnail.setLayoutParams(params1);

                RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) type.getLayoutParams();
                params2.setMargins(0, -12, -12, 0);
                type.setLayoutParams(params2);
                thumbnail.setImageBitmap(thumb);
            }
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.option_view_layout:
                managerActivity.openTransferLocation(transfer);
                break;

            case R.id.option_get_link_layout:
                if (!isOnline(context)) {
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                    break;
                }

                managerActivity.showGetLinkActivity(handle);
                break;

            case R.id.option_clear_layout:
                managerActivity.removeCompletedTransfer(transfer);
                break;

            case R.id.option_retry_layout:
                if (!isOnline(context)) {
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                    break;
                }

                managerActivity.retryTransfer(transfer);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(TRANSFER_ID, transferId);
    }
}
