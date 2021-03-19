package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetUploadBinding
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener

class UploadBottomSheetDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {

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

        binding.uploadFromDeviceLayout.setOnClickListener(this)
        binding.uploadFromSystemLayout.setOnClickListener(this)
        binding.scanDocumentLayout.setOnClickListener(this)
        binding.takePictureLayout.setOnClickListener(this)
        binding.newFolderLayout.setOnClickListener(this)
        binding.newTxtLayout.setOnClickListener(this)

        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.upload_from_device_layout -> listener.uploadFromDevice()
            R.id.upload_from_system_layout -> listener.uploadFromSystem()
            R.id.scan_document -> listener.scanDocument()
            R.id.take_picture_layout -> listener.takePictureAndUpload()
            R.id.new_folder_layout -> listener.showNewFolderDialog()
            R.id.new_txt_layout -> listener.createAndOpenNewTextFile()
        }

        setStateBottomSheetBehaviorHidden()
    }
}