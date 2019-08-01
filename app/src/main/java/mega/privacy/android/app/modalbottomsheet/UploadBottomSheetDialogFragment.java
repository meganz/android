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


import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener;
import mega.privacy.android.app.utils.Util;

public class UploadBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;
    private UploadBottomSheetDialogActionListener listener;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    private LinearLayout mainLinearLayout;
    private LinearLayout optionFromDevice;
    private LinearLayout optionFromSystem;
    private LinearLayout optionScanDocument;
    private LinearLayout optionTakePicture;
    private LinearLayout optionCreateFolder;

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        int heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_upload, null);

        mainLinearLayout = contentView.findViewById(R.id.upload_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        optionFromDevice = contentView.findViewById(R.id.upload_from_device_layout);
        optionFromSystem = contentView.findViewById(R.id.upload_from_system_layout);
        optionScanDocument = contentView.findViewById(R.id.scan_document_layout);
        optionTakePicture = contentView.findViewById(R.id.take_picture_layout);
        optionCreateFolder = contentView.findViewById(R.id.new_folder_layout);

        optionFromDevice.setOnClickListener(this);
        optionFromSystem.setOnClickListener(this);
        optionScanDocument.setOnClickListener(this);
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
                log("click upload from device");
                listener.uploadFromDevice();
                dismissAllowingStateLoss();
                break;
            }
            case R.id.upload_from_system_layout: {
                log("click upload from_system");
                listener.uploadFromSystem();
                break;
            }
            case R.id.scan_document_layout: {
                break;
            }
            case R.id.take_picture_layout: {
                listener.takePictureAndUpload();
                break;
            }
            case R.id.new_folder_layout: {
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

    private static void log(String log) {
        Util.log("UploadBottomSheetDialogFragment", log);
    }
}
