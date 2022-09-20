package mega.privacy.android.app.modalbottomsheet;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import mega.privacy.android.app.databinding.BottomSheetManageTransferBinding;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.setThumbnail;
import static mega.privacy.android.app.utils.Constants.INVALID_ID;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaTransfer.*;

public class ManageTransferBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {
    private static final String TRANSFER_ID = "TRANSFER_ID";
    private static final int MARGIN_TRANSFER_TYPE_ICON_WITH_THUMBNAIL = -12;

    private ManagerActivity managerActivity;

    private AndroidCompletedTransfer transfer;
    private long handle;
    private long transferId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        BottomSheetManageTransferBinding binding = BottomSheetManageTransferBinding.inflate(getLayoutInflater());
        contentView = binding.getRoot();
        itemsLayout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

        managerActivity = (ManagerActivity) requireActivity();

        if (savedInstanceState == null) {
            transfer = managerActivity.getSelectedTransfer();
            transferId = transfer.getId();
        } else {
            transferId = savedInstanceState.getLong(TRANSFER_ID, INVALID_ID);
            transfer = dbH.getcompletedTransfer(transferId);
        }

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (transfer == null) return;

        managerActivity = (ManagerActivity) getActivity();

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

        location.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_054_white_054));
        RelativeLayout.LayoutParams params =  (RelativeLayout.LayoutParams) stateIcon.getLayoutParams();
        params.rightMargin = dp2px(5, getResources().getDisplayMetrics());

        switch (transfer.getState()) {
            case STATE_COMPLETED:
                location.setText(transfer.getPath());
                stateIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green_500_300), PorterDuff.Mode.SRC_IN);
                stateIcon.setImageResource(R.drawable.ic_transfers_completed);
                retryOption.setVisibility(View.GONE);
                break;

            case STATE_FAILED:
                location.setTextColor(ColorUtils.getThemeColor(requireContext(), R.attr.colorError));
                location.setText(String.format("%s: %s", StringResourcesUtils.getString(R.string.failed_label), transfer.getError()));
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
                stateIcon.clearColorFilter();
                stateIcon.setImageResource(R.drawable.ic_queue);
                break;
        }

        if ((getLinkOption.getVisibility() == View.GONE && retryOption.getVisibility() == View.GONE) || viewInFolderOption.getVisibility() == View.GONE) {
            contentView.findViewById(R.id.separator_get_link).setVisibility(View.GONE);
        }

        stateIcon.setLayoutParams(params);

        handle = Long.parseLong(transfer.getNodeHandle());

        Bitmap thumb = getThumbnailFromCache(handle);
        if (thumb == null) {
            MegaNode node = MegaApplication.getInstance().getMegaApi().getNodeByHandle(handle);
            thumb = getThumbnailFromFolder(node, getContext());
        }

        if (setThumbnail(requireContext(), thumb, thumbnail, transfer.getFileName())) {
            RelativeLayout.LayoutParams typeParams = (RelativeLayout.LayoutParams) type.getLayoutParams();
            typeParams.topMargin = typeParams.rightMargin = MARGIN_TRANSFER_TYPE_ICON_WITH_THUMBNAIL;
            type.setLayoutParams(typeParams);
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.option_view_layout:
                if (transfer.getType() == TYPE_UPLOAD && !isOnline(requireContext())) {
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                    break;
                }

                managerActivity.openTransferLocation(transfer);
                break;

            case R.id.option_get_link_layout:
                if (!isOnline(requireContext())) {
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                    break;
                }

                managerActivity.showGetLinkActivity(handle);
                break;

            case R.id.option_clear_layout:
                managerActivity.removeCompletedTransfer(transfer, true);
                break;

            case R.id.option_retry_layout:
                if (!isOnline(requireContext())) {
                    managerActivity.showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                    break;
                }

                managerActivity.retrySingleTransfer(transfer);
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
