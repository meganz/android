package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.MimeTypeList.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static nz.mega.sdk.MegaTransfer.*;

public class ManageTransferBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {
    private ManagerActivityLollipop managerActivity;

    private AndroidCompletedTransfer transfer;
    private long handle;

    private BottomSheetBehavior mBehavior;

    private ImageView thumbnail;
    private ImageView type;
    private ImageView stateIcon;
    private TextView name;
    private TextView location;
    private LinearLayout viewInFolderOption;
    private LinearLayout getLinkOption;
    private LinearLayout clearOption;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            transfer = managerActivity.getSelectedTransfer();
        } else {

        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_manage_transfer, null);

        if (transfer == null) return;

        managerActivity = (ManagerActivityLollipop) getActivity();

        thumbnail = contentView.findViewById(R.id.manage_transfer_thumbnail);
        type = contentView.findViewById(R.id.manage_transfer_small_icon);
        stateIcon = contentView.findViewById(R.id.manage_transfer_completed_image);
        name = contentView.findViewById(R.id.manage_transfer_filename);
        location = contentView.findViewById(R.id.manage_transfer_location);

        viewInFolderOption = contentView.findViewById(R.id.option_view_layout);
        viewInFolderOption.setOnClickListener(this);
        getLinkOption = contentView.findViewById(R.id.option_get_link_layout);
        getLinkOption.setOnClickListener(this);
        clearOption = contentView.findViewById(R.id.option_clear_layout);
        clearOption.setOnClickListener(this);

        name.setText(transfer.getFileName());

        if (transfer.getType() == TYPE_DOWNLOAD) {
            type.setImageResource(R.drawable.ic_download_transfers);
            getLinkOption.setVisibility(View.GONE);
            contentView.findViewById(R.id.separator_get_link).setVisibility(View.GONE);
        } else if (transfer.getType() == TYPE_UPLOAD) {
            type.setImageResource(R.drawable.ic_upload_transfers);
        }

        switch (transfer.getState()) {
            case STATE_COMPLETED:
                location.setText(transfer.getPath());
                stateIcon.setImageResource(R.drawable.ic_complete_transfer);
                break;

            default:
                location.setText(getActivity().getResources().getString(R.string.transfer_unknown));
                stateIcon.setImageResource(R.drawable.ic_queue);
                break;
        }

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
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getContext().getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getContext().getResources().getDisplayMetrics());
                params1.setMargins(54, 0, 18, 0);
                thumbnail.setLayoutParams(params1);

                RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) type.getLayoutParams();
                params2.setMargins(0, -12, -12, 0);
                type.setLayoutParams(params2);
                thumbnail.setImageBitmap(thumb);
            }
        }

        dialog.setContentView(contentView);

        mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.option_view_layout:
                managerActivity.openTransferLocation(transfer);
                break;

            case R.id.option_get_link_layout:
                managerActivity.showGetLinkActivity(handle);
                break;

            case R.id.option_clear_layout:
                managerActivity.removeTransfer(transfer);
                break;
        }

        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        managerActivity = (ManagerActivityLollipop) getActivity();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
