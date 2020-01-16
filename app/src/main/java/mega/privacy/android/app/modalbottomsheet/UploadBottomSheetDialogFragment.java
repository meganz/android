package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;

import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class UploadBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;
    private UploadBottomSheetDialogActionListener listener;
    private BottomSheetBehavior mBehavior;
    private LinearLayout mainLinearLayout;

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        int heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_upload, null);

        mainLinearLayout = contentView.findViewById(R.id.upload_bottom_sheet);
        LinearLayout items_layout = contentView.findViewById(R.id.items_layout);

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
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());

        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.upload_from_device_layout: {
                logDebug("click upload from device");
                listener.uploadFromDevice();
                dismissAllowingStateLoss();
                break;
            }
            case R.id.upload_from_system_layout: {
                logDebug("click upload from_system");
                listener.uploadFromSystem();
                break;
            }
            case R.id.scan_document_layout:{
                break;
            }

            case R.id.take_picture_layout: {
                logDebug("Click take picture");
                listener.takePictureAndUpload();
                break;
            }
            case R.id.new_folder_layout: {
                logDebug("Click create new folder");
                listener.showNewFolderDialog();
                break;
            }
        }

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
        listener = (UploadBottomSheetDialogActionListener) activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        listener = (UploadBottomSheetDialogActionListener) context;
    }
}
