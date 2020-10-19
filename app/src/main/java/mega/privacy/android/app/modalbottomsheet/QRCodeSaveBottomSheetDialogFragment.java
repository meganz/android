package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.view.View;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.lollipop.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

public class QRCodeSaveBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.qr_code_saveTo_cloud_layout:
                saveToCloudDrive();
                break;

            case R.id.qr_code_saveTo_fileSystem_layout:
                saveToFileSystem();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    private void saveToCloudDrive() {
        MegaNode parentNode = megaApi.getRootNode();
        String myEmail = megaApi.getMyUser().getEmail();
        File qrFile = buildQrFile(getActivity(), myEmail + QR_IMAGE_FILE_NAME);

        if (isFileAvailable(qrFile)) {
            if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                showOverDiskQuotaPaywallWarning();
                return;
            }

            ShareInfo info = ShareInfo.infoFromFile(qrFile);
            Intent intent = new Intent(getActivity().getApplicationContext(), UploadService.class);
            intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
            intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
            intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
            intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
            intent.putExtra("qrfile", true);
            getActivity().startService(intent);
            ((QRCodeActivity) getActivity()).showSnackbar(null, getString(R.string.save_qr_cloud_drive, qrFile.getName()));
        } else {
            ((QRCodeActivity) getActivity()).showSnackbar(null, getString(R.string.error_upload_qr));
        }
    }

    private void saveToFileSystem() {
        Intent intent = new Intent(getActivity(), FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        ((QRCodeActivity) getActivity()).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_qr_code, null);
        mainLinearLayout = contentView.findViewById(R.id.qr_code_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        contentView.findViewById(R.id.qr_code_saveTo_cloud_layout).setOnClickListener(this);
        contentView.findViewById(R.id.qr_code_saveTo_fileSystem_layout).setOnClickListener(this);

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }
}
