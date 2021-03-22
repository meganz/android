package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetUploadBinding
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener

class UploadBottomSheetDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {

    companion object {
        private const val UPLOAD_TYPE = "UPLOAD_TYPE"
        const val GENERAL_UPLOAD = 1
        const val DOCUMENTS_UPLOAD = 2

        @JvmStatic
        fun newInstance(uploadType: Int): UploadBottomSheetDialogFragment {
            val fragment = UploadBottomSheetDialogFragment()
            val args = Bundle()

            args.putInt(UPLOAD_TYPE, uploadType)
            fragment.arguments = args

            return fragment
        }
    }

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

        if (arguments?.getInt(UPLOAD_TYPE) == DOCUMENTS_UPLOAD) {
            binding.takePictureOption.isVisible = false
            binding.newFolderOption.isVisible = false
        }

        binding.uploadFromDeviceOption.setOnClickListener(this)
        binding.uploadFromSystemOption.setOnClickListener(this)
        binding.scanDocumentOption.setOnClickListener(this)
        binding.takePictureOption.setOnClickListener(this)
        binding.newFolderOption.setOnClickListener(this)
        binding.newTxtOption.setOnClickListener(this)

        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.upload_from_device_option -> listener.uploadFromDevice()
            R.id.upload_from_system_option -> listener.uploadFromSystem()
            R.id.scan_document_option -> listener.scanDocument()
            R.id.take_picture_option -> listener.takePictureAndUpload()
            R.id.new_folder_option -> listener.showNewFolderDialog()
            R.id.new_txt_option -> listener.createAndOpenNewTextFile(null)
        }

        setStateBottomSheetBehaviorHidden()
    }
}