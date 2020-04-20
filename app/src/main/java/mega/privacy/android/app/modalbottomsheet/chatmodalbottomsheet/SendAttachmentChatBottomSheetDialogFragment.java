package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
        mainLinearLayout = contentView.findViewById(R.id.send_attachment_chat_bottom_sheet);
        items_layout = contentView.findViewById(R.id.send_attachment_chat_items_layout);

        TextView titleSlidingPanel = contentView.findViewById(R.id.send_attachment_chat_title_text);
        titleSlidingPanel.setText(getString(R.string.context_send));

        contentView.findViewById(R.id.send_attachment_chat_from_cloud_layout).setOnClickListener(this);
        contentView.findViewById(R.id.send_attachment_chat_from_filesystem_layout).setOnClickListener(this);
        contentView.findViewById(R.id.send_attachment_chat_contact_layout).setOnClickListener(this);
        contentView.findViewById(R.id.send_attachment_chat_location_layout).setOnClickListener(this);

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_attachment_chat_from_cloud_layout:
                ((ChatActivityLollipop) context).sendFromCloud();
                break;

            case R.id.send_attachment_chat_from_filesystem_layout:
                ((ChatActivityLollipop) context).sendFromFileSystem();
                break;

            case R.id.send_attachment_chat_contact_layout:
                ((ChatActivityLollipop) context).chooseContactsDialog();
                break;

            case R.id.send_attachment_chat_location_layout:
                ((ChatActivityLollipop) context).sendLocation();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }
}
