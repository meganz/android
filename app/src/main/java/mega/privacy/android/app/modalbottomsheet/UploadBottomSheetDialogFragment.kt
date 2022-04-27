package mega.privacy.android.app.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetUploadBinding
import mega.privacy.android.app.interfaces.UploadBottomSheetDialogActionListener

class UploadBottomSheetDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {

    companion object {
        private const val UPLOAD_TYPE = "UPLOAD_TYPE"
        const val GENERAL_UPLOAD = 1
        const val DOCUMENTS_UPLOAD = 2
        const val HOMEPAGE_UPLOAD = 3

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetUploadBinding.inflate(layoutInflater)
        contentView = binding.root.rootView
        itemsLayout = binding.itemsLayout
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listener = requireActivity() as UploadBottomSheetDialogActionListener

        when (arguments?.getInt(UPLOAD_TYPE)) {
            DOCUMENTS_UPLOAD -> {
                binding.takePictureOption.isVisible = false
                binding.newFolderOption.isVisible = false
            }
            HOMEPAGE_UPLOAD -> {
                binding.newFolderOption.isVisible = false
            }
        }

        binding.uploadFiles.setOnClickListener(this)
        binding.uploadFolder.setOnClickListener(this)
        binding.scanDocumentOption.setOnClickListener(this)
        binding.takePictureOption.setOnClickListener(this)
        binding.newFolderOption.setOnClickListener(this)
        binding.newTxtOption.setOnClickListener(this)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.upload_files -> listener.uploadFiles()
            R.id.upload_folder -> listener.uploadFolder()
            R.id.scan_document_option -> listener.scanDocument()
            R.id.take_picture_option -> listener.takePictureAndUpload()
            R.id.new_folder_option -> listener.showNewFolderDialog()
            R.id.new_txt_option -> listener.showNewTextFileDialog(null)
        }

        setStateBottomSheetBehaviorHidden()
    }
}