package mega.privacy.android.app.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import mega.privacy.android.app.databinding.BottomSheetPhotoBinding
import mega.privacy.android.app.main.controllers.AccountController

class PhotoBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetPhotoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetPhotoBinding.inflate(layoutInflater, null, false)
        contentView = binding.root
        itemsLayout = binding.itemsLayout
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val callback = requireActivity() as PhotoCallback

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

        super.onViewCreated(view, savedInstanceState)
    }

    interface PhotoCallback {
        fun capturePhoto()
        fun choosePhoto()
        fun deletePhoto()
    }
}