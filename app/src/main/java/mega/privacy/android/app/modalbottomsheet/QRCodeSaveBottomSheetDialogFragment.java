package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.lollipop.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class QRCodeSaveBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private static int REQUEST_DOWNLOAD_FOLDER = 1000;

    public LinearLayout mainLinearLayout;
    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    public TextView titleText;
    public LinearLayout optionSaveToCloudDrive;
    public LinearLayout optionSaveToFileSystem;

    MegaApiAndroid megaApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");
        switch(v.getId()){

            case R.id.qr_code_saveTo_cloud_layout:{
                logDebug("Option save to Cloud Drive");
                saveToCloudDrive();
                break;
            }
            case R.id.qr_code_saveTo_fileSystem_layout:{
                logDebug("Option save to File System");
                saveToFileSystem();
                break;
            }
        }

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void saveToCloudDrive (){
        if (megaApi == null) {
            megaApi = ((MegaApplication) getActivity().getApplication()).getMegaApi();
        }
        MegaNode parentNode = megaApi.getRootNode();
        String myEmail = megaApi.getMyUser().getEmail();
        File qrFile = buildQrFile(getActivity(),myEmail + QR_IMAGE_FILE_NAME);

        if (isFileAvailable(qrFile)){
            ShareInfo info = ShareInfo.infoFromFile(qrFile);
            Intent intent = new Intent(getActivity().getApplicationContext(), UploadService.class);
            intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
            intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
            intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
            intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
            intent.putExtra("qrfile", true);
            getActivity().startService(intent);
            ((QRCodeActivity) getActivity()).showSnackbar(null, getString(R.string.save_qr_cloud_drive, qrFile.getName()));
        }
        else {
            ((QRCodeActivity) getActivity()).showSnackbar(null, getString(R.string.error_upload_qr));
        }
    }

    public void saveToFileSystem () {

        Intent intent = new Intent(getActivity(), FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        ((QRCodeActivity) getActivity()).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_qr_code, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.qr_code_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleText = (TextView) contentView.findViewById(R.id.qr_code_title_text);

        optionSaveToCloudDrive= (LinearLayout) contentView.findViewById(R.id.qr_code_saveTo_cloud_layout);
        optionSaveToFileSystem = (LinearLayout) contentView.findViewById(R.id.qr_code_saveTo_fileSystem_layout);

        optionSaveToCloudDrive.setOnClickListener(this);
        optionSaveToFileSystem.setOnClickListener(this);

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

        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, getContext(), 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
