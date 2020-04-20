package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import static mega.privacy.android.app.utils.Constants.*;

public class MyAccountBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_my_account, null);
        mainLinearLayout = contentView.findViewById(R.id.my_account_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        LinearLayout optionChoosePicture = contentView.findViewById(R.id.my_account_choose_photo_layout);
        LinearLayout optionTakePicture = contentView.findViewById(R.id.my_account_take_photo_layout);
        LinearLayout optionRemovePicture = contentView.findViewById(R.id.my_account_delete_layout);
        LinearLayout optionQRcode = contentView.findViewById(R.id.my_account_my_QR_code);

        optionChoosePicture.setOnClickListener(this);
        optionTakePicture.setOnClickListener(this);
        optionRemovePicture.setOnClickListener(this);
        optionQRcode.setOnClickListener(this);

        optionRemovePicture.setVisibility(new AccountController(context).existsAvatar() ? View.VISIBLE : View.GONE);

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.my_account_choose_photo_layout:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("image/*");
                ((ManagerActivityLollipop) context).startActivityForResult(Intent.createChooser(intent, null), CHOOSE_PICTURE_PROFILE_CODE);
                break;

            case R.id.my_account_take_photo_layout:
                ((ManagerActivityLollipop) context).checkPermissions();
                break;

            case R.id.my_account_delete_layout:
                ((ManagerActivityLollipop) context).showConfirmationDeleteAvatar();
                break;

            case R.id.my_account_my_QR_code:
                ((ManagerActivityLollipop) context).checkBeforeOpeningQR();
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }
}
