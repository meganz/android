package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;


import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;

public class UploadBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private UploadBottomSheetDialogActionListener listener;

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_upload, null);
        mainLinearLayout = contentView.findViewById(R.id.upload_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        LinearLayout optionFromDevice = contentView.findViewById(R.id.upload_from_device_layout);
        LinearLayout optionFromSystem = contentView.findViewById(R.id.upload_from_system_layout);
        LinearLayout optionTakePicture = contentView.findViewById(R.id.take_picture_layout);
        LinearLayout optionCreateFolder = contentView.findViewById(R.id.new_folder_layout);

        LinearLayout createFolderSeparator = contentView.findViewById(R.id.create_folder_separator);
        if (context instanceof ManagerActivityLollipop) {
            if (((ManagerActivityLollipop) context).isOnRecents()) {
                optionCreateFolder.setVisibility(View.GONE);
                createFolderSeparator.setVisibility(View.GONE);
            } else {
                optionCreateFolder.setVisibility(View.VISIBLE);
                createFolderSeparator.setVisibility(View.VISIBLE);
            }
        }

        optionFromDevice.setOnClickListener(this);
        optionFromSystem.setOnClickListener(this);
        optionTakePicture.setOnClickListener(this);
        optionCreateFolder.setOnClickListener(this);

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upload_from_device_layout:
                listener.uploadFromDevice();
                break;

            case R.id.upload_from_system_layout:
                listener.uploadFromSystem();
                break;

            case R.id.take_picture_layout:
                listener.takePictureAndUpload();
                break;

            case R.id.new_folder_layout:
                listener.showNewFolderDialog();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (UploadBottomSheetDialogActionListener) context;
    }
}
