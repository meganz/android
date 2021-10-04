package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;

public class SendAttachmentChatBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.send_attatchment_chat_bottom_sheet, null);
        itemsLayout = contentView.findViewById(R.id.send_attachment_chat_items_layout);

        contentView.findViewById(R.id.send_attachment_chat_from_cloud_layout).setOnClickListener(this);
        contentView.findViewById(R.id.send_attachment_chat_from_filesystem_layout).setOnClickListener(this);
        contentView.findViewById(R.id.send_attachment_chat_contact_layout).setOnClickListener(this);
        contentView.findViewById(R.id.send_attachment_chat_location_layout).setOnClickListener(this);

        dialog.setContentView(contentView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_attachment_chat_from_cloud_layout:
                ((ChatActivityLollipop) requireActivity()).sendFromCloud();
                break;

            case R.id.send_attachment_chat_from_filesystem_layout:
                ((ChatActivityLollipop) requireActivity()).sendFromFileSystem();
                break;

            case R.id.send_attachment_chat_contact_layout:
                ((ChatActivityLollipop) requireActivity()).chooseContactsDialog();
                break;

            case R.id.send_attachment_chat_location_layout:
                ((ChatActivityLollipop) requireActivity()).sendLocation();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }
}
