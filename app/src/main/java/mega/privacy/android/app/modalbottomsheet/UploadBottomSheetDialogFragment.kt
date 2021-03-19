package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.core.view.isVisible
import mega.privacy.android.app.databinding.BottomSheetUploadBinding
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

class UploadBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private lateinit var listener: UploadBottomSheetDialogActionListener

    private lateinit var binding: BottomSheetUploadBinding

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        listener = context as UploadBottomSheetDialogActionListener

        binding = BottomSheetUploadBinding.inflate(layoutInflater)
        contentView = binding.root.rootView
        mainLinearLayout = binding.uploadBottomSheet
        items_layout = binding.itemsLayout

        if (context is ManagerActivityLollipop) {
            binding.newFolderLayout.isVisible = true
            binding.createFolderSeparator.isVisible = true
        }

        binding.uploadFromDeviceLayout.setOnClickListener {
            listener.uploadFromDevice()
            setStateBottomSheetBehaviorHidden()
        }

        binding.uploadFromSystemLayout.setOnClickListener {
            listener.uploadFromSystem()
            setStateBottomSheetBehaviorHidden()
        }

        binding.scanDocumentLayout.setOnClickListener {
            listener.scanDocument()
            setStateBottomSheetBehaviorHidden()
        }

        binding.takePictureLayout.setOnClickListener {
            listener.takePictureAndUpload()
            setStateBottomSheetBehaviorHidden()
        }

        binding.newFolderLayout.setOnClickListener {
            listener.showNewFolderDialog()
            setStateBottomSheetBehaviorHidden()
        }

        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }
}