package mega.privacy.android.app.modalbottomsheet;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;

import java.io.File;

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

        switch(v.getId()){

            case R.id.upload_from_device_layout:{
                log("click upload from device");
                listener.uploadFromDevice();
                dismissAllowingStateLoss();
                break;
            }
            case R.id.upload_from_system_layout:{
                log("click upload from_system");
                final File[] fs = context.getExternalFilesDirs(null);
                //has SD card
                if (fs.length > 1) {
                    Dialog localCameraDialog;
                    String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
                    AlertDialog.Builder b=new AlertDialog.Builder(context);

                    b.setTitle(getResources().getString(R.string.upload_to_filesystem_from));
                    b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which){
                                case 0:{
                                    pickFileFromFileSystem(false);
                                    break;
                                }
                                case 1: {
                                    if (fs[1] != null) {
                                        pickFileFromFileSystem(true);
                                    } else {
                                        pickFileFromFileSystem(false);
                                    }
                                    break;
                                }
                            }
                        }
                    });
                    b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    localCameraDialog = b.create();
                    localCameraDialog.show();
                } else {
                    pickFileFromFileSystem(false);
                }
                break;
            }
            case R.id.scan_document_layout:{
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
