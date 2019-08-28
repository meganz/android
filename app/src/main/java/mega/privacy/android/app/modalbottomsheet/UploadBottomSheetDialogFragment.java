package mega.privacy.android.app.modalbottomsheet;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.SDCardUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class UploadBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView title;
    public LinearLayout optionFromDevice;
    public LinearLayout optionFromSystem;
    public LinearLayout optionScanDocument;
    public LinearLayout optionTakePicture;
    public LinearLayout optionCreateFolder;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    MegaApiAndroid megaApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_upload, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.upload_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        optionFromDevice = (LinearLayout) contentView.findViewById(R.id.upload_from_device_layout);
        optionFromSystem = (LinearLayout) contentView.findViewById(R.id.upload_from_system_layout);
        optionScanDocument = (LinearLayout) contentView.findViewById(R.id.scan_document_layout);
        optionTakePicture = (LinearLayout) contentView.findViewById(R.id.take_picture_layout);
        optionCreateFolder = (LinearLayout) contentView.findViewById(R.id.new_folder_layout);

        title = (TextView) contentView.findViewById(R.id.contact_list_contact_name_text);

        optionFromDevice.setOnClickListener(this);
        optionFromSystem.setOnClickListener(this);
        optionScanDocument.setOnClickListener(this);
        optionTakePicture.setOnClickListener(this);
        optionCreateFolder.setOnClickListener(this);

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
//        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//
//        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mBehavior.setPeekHeight((heightDisplay / 4) * 2);
//        }
//        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//            mBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
//        }

        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.upload_from_device_layout:{
                log("click upload from device");

                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");

                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                }
                else if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                }

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
            case R.id.take_picture_layout:{
                ((ManagerActivityLollipop) context).fromTakePicture = Constants.TAKE_PICTURE_OPTION;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    if (!hasStoragePermission) {
                        ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                Constants.REQUEST_WRITE_STORAGE);
                    }

                    boolean hasCameraPermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
                    if (!hasCameraPermission) {
                        ActivityCompat.requestPermissions((ManagerActivityLollipop) context,
                                new String[]{Manifest.permission.CAMERA},
                                Constants.REQUEST_CAMERA);
                    }

                    if (hasStoragePermission && hasCameraPermission){
                        ((ManagerActivityLollipop) context).takePicture();
                    }
                }
                else{
                    ((ManagerActivityLollipop) context).takePicture();
                }
                break;
            }
            case R.id.new_folder_layout:{
                ((ManagerActivityLollipop) context).showNewFolderDialog();
                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void pickFileFromFileSystem(boolean fromSDCard) {
        Intent intent = new Intent();
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FILE.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS,false);
        intent.setClass(context,FileStorageActivityLollipop.class);
        if (fromSDCard) {
            File[] fs = context.getExternalFilesDirs(null);
            String sdRoot = SDCardUtils.getSDCardRoot(fs[1]);
            intent.putExtra(FileStorageActivityLollipop.EXTRA_SD_ROOT,sdRoot);
        }

        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop)context).startActivityForResult(intent,Constants.REQUEST_CODE_GET_LOCAL);
        } else if (context instanceof ContactFileListActivityLollipop) {
            ((ContactFileListActivityLollipop)context).startActivityForResult(intent,Constants.REQUEST_CODE_GET_LOCAL);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private static void log(String log) {
        Util.log("UploadBottomSheetDialogFragment", log);
    }
}
