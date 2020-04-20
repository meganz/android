package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;

public class AttachmentUploadBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_attachment_upload, null);
        mainLinearLayout = contentView.findViewById(R.id.attachment_upload_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        contentView.findViewById(R.id.attachment_upload_cloud_layout).setOnClickListener(this);
        contentView.findViewById(R.id.attachment_upload_contact_layout).setOnClickListener(this);
        contentView.findViewById(R.id.attachment_upload_photo_layout).setOnClickListener(this);

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.attachment_upload_cloud_layout:
                ((ChatActivityLollipop) context).attachFromCloud();
                break;

            case R.id.attachment_upload_contact_layout:
                ((ChatActivityLollipop) context).chooseContactsDialog();
                break;

            case R.id.attachment_upload_photo_layout:
                ((ChatActivityLollipop) context).attachPhotoVideo();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }
}
