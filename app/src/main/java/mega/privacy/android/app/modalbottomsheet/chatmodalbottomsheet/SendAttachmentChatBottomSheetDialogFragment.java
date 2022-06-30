package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;

public class SendAttachmentChatBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.send_attatchment_chat_bottom_sheet, null);
        itemsLayout = contentView.findViewById(R.id.send_attachment_chat_items_layout);
        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        contentView.findViewById(R.id.send_attachment_chat_from_cloud_layout).setOnClickListener(this);
        contentView.findViewById(R.id.send_attachment_chat_from_filesystem_layout).setOnClickListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_attachment_chat_from_cloud_layout:
                ((ChatActivity) requireActivity()).sendFromCloud();
                break;

            case R.id.send_attachment_chat_from_filesystem_layout:
                ((ChatActivity) requireActivity()).sendFromFileSystem();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }
}
