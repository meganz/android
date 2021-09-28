package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import androidx.core.view.isVisible
import mega.privacy.android.app.databinding.BottomSheetPhotoBinding
import mega.privacy.android.app.lollipop.controllers.AccountController

class PhotoBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val callback = requireActivity() as PhotoCallback
        val binding = BottomSheetPhotoBinding.inflate(layoutInflater, null, false)

        contentView = binding.root
        mainLinearLayout = binding.photoBottomSheet
        itemsLayout = binding.itemsLayout

        binding.capturePhotoAction.setOnClickListener {
            callback.capturePhoto()
            setStateBottomSheetBehaviorHidden()
        }

        binding.choosePhotoAction.setOnClickListener {
            callback.choosePhoto()
            setStateBottomSheetBehaviorHidden()
        }

        binding.deletePictureAction.apply {
            isVisible = AccountController(requireActivity()).existsAvatar()

            if (isVisible) {
                setOnClickListener {
                    callback.deletePhoto()
                    setStateBottomSheetBehaviorHidden()
                }

                binding.deleteSeparator.isVisible = true
            } else {
                binding.deleteSeparator.isVisible = false
            }
        }

        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW)
    }

    interface PhotoCallback {
        fun capturePhoto()
        fun choosePhoto()
        fun deletePhoto()
    }
}