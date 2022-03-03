package mega.privacy.android.app.modalbottomsheet;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.lollipop.FileStorageActivity;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.lollipop.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class QRCodeSaveBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_qr_code, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        contentView.findViewById(R.id.qr_code_saveTo_cloud_layout).setOnClickListener(this);
        contentView.findViewById(R.id.qr_code_saveTo_fileSystem_layout).setOnClickListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

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
            if (MegaApplication.getInstance().getStorageState() == STORAGE_STATE_PAYWALL) {
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
        Intent intent = new Intent(getActivity(), FileStorageActivity.class);
        intent.putExtra(FileStorageActivity.PICK_FOLDER_TYPE, FileStorageActivity.PickFolderType.DOWNLOAD_FOLDER.getFolderType());
        intent.setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction());
        ((QRCodeActivity) getActivity()).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
    }
}
