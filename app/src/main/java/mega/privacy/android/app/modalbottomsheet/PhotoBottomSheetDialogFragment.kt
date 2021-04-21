package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import mega.privacy.android.app.databinding.BottomSheetPhotoBinding
import mega.privacy.android.app.lollipop.controllers.AccountController

class PhotoBottomSheetDialogFragment: BaseBottomSheetDialogFragment() {

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val binding = BottomSheetPhotoBinding.inflate(layoutInflater, null, false)
        contentView = binding.root
        mainLinearLayout = binding.photoBottomSheet
        items_layout = binding.itemsLayout

        binding.capturePhotoAction.setOnClickListener {
//                ((ManagerActivityLollipop) context).checkPermissions();
            setStateBottomSheetBehaviorHidden()
        }

        binding.choosePhotoAction.setOnClickListener {
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                intent.setType("image/*");
//                ((ManagerActivityLollipop) context).startActivityForResult(Intent.createChooser(intent, null), CHOOSE_PICTURE_PROFILE_CODE);
            setStateBottomSheetBehaviorHidden()
        }

        binding.deletePictureAction.apply {
            isVisible = AccountController(requireContext()).existsAvatar()

            if (isVisible) {
                setOnClickListener {
//                ((ManagerActivityLollipop) context).showConfirmationDeleteAvatar();
                    setStateBottomSheetBehaviorHidden()
                }

                binding.deleteSeparator.isVisible = true
            } else {
                binding.deleteSeparator.isVisible = false
            }
        }

        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }
}