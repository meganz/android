package mega.privacy.android.app.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.BottomSheetPhotoBinding
import mega.privacy.android.app.presentation.editProfile.EditProfileViewModel

class PhotoBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetPhotoBinding
    private val editProfileViewModel: EditProfileViewModel by activityViewModels()

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
            isVisible = editProfileViewModel.existsMyAvatar()

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