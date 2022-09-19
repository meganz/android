package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.Manifest
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetChatRoomToolbarBinding
import mega.privacy.android.app.interfaces.ChatRoomToolbarBottomSheetDialogActionListener
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.megachat.chatAdapters.FileStorageChatAdapter
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.getAudioPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getImagePermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.getReadExternalStoragePermission
import mega.privacy.android.app.utils.permission.PermissionUtils.getVideoPermissionByVersion
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions

/**
 * Bottom Sheet Dialog which shows the chat options
 */
@AndroidEntryPoint
class ChatRoomToolbarBottomSheetDialogFragment : BottomSheetDialogFragment() {

    val viewModel: ChatRoomToolbarViewModel by viewModels()

    private lateinit var binding: BottomSheetChatRoomToolbarBinding

    private lateinit var listener: ChatRoomToolbarBottomSheetDialogActionListener

    private var isMultiselectMode = false
    private var hasCameraPermission = false
    private var hasStoragePermission = false

    private val filesAdapter by lazy {
        FileStorageChatAdapter(::onTakePictureClick,
            ::onClickItem,
            ::onLongClickItem,
            viewLifecycleOwner)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(),
            R.style.BottomSheetFragmentWithTransparentBackground).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        listener = requireActivity() as ChatRoomToolbarBottomSheetDialogActionListener
        binding = BottomSheetChatRoomToolbarBinding.inflate(layoutInflater, container, false)
        binding.textFile.text = getQuantityString(R.plurals.general_num_files, 1)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog ?: return
        BottomSheetBehavior.from(dialog.findViewById(R.id.design_bottom_sheet)).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupButtons()

        lifecycleScope.launchWhenStarted {
            viewModel.filesGallery.collect { filesList ->
                binding.emptyGallery.isVisible = filesList.isEmpty()
                binding.list.isVisible = filesList.isNotEmpty()
                filesAdapter.submitList(filesList)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.showSendImagesButton.collect { visibility ->
                isMultiselectMode = visibility
                binding.sendFilesButton.isVisible = visibility
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.hasReadStoragePermissionsGranted.collect { isGranted ->
                hasStoragePermission = isGranted
                if (!hasStoragePermission) {
                    viewModel.checkStoragePermission()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.hasCameraPermissionsGranted.collect { isGranted ->
                hasCameraPermission = isGranted
                if (!hasCameraPermission && hasStoragePermission) {
                    viewModel.checkCameraPermission()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.checkReadStoragePermissions.collect { shouldCheck ->
                if (shouldCheck) {
                    checkPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.checkCameraPermissions.collect { shouldCheck ->
                if (shouldCheck) {
                    checkPermissions(Manifest.permission.CAMERA)
                }
            }
        }

        setupView()
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Setup recycler view
     */
    private fun setupView() {
        binding.list.apply {
            clipToPadding = false
            setHasFixedSize(true)
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            adapter = filesAdapter
        }
    }

    /**
     * Setup option buttons of the toolbar.
     */
    private fun setupButtons() {
        binding.sendFilesButton.setOnClickListener {
            val list = viewModel.getSelectedFiles()
            if (list.isNotEmpty()) {
                listener.onSendFilesSelected(list)
                dismiss()
            }
        }

        binding.optionVoiceClip.setOnClickListener {
            listener.onRecordVoiceClipClicked()
            dismiss()
        }

        binding.optionFile.setOnClickListener {
            listener.onSendFileOptionClicked()
            dismiss()
        }

        binding.optionVoice.setOnClickListener {
            listener.onStartCallOptionClicked(false)
            dismiss()
        }

        binding.optionVideo.setOnClickListener {
            listener.onStartCallOptionClicked(true)
            dismiss()
        }

        binding.optionScan.setOnClickListener {
            listener.onScanDocumentOptionClicked()
            dismiss()
        }

        binding.optionGif.setOnClickListener {
            listener.onSendGIFOptionClicked()
            dismiss()
        }

        binding.optionLocation.setOnClickListener {
            listener.onSendLocationOptionClicked()
            dismiss()
        }

        binding.optionContact.setOnClickListener {
            listener.onSendContactOptionClicked()
            dismiss()
        }
    }

    /**
     * Check whether a permit needs to be applied for or whether it is already granted
     *
     * @param typePermission Type of permission: READ_EXTERNAL_STORAGE or CAMERA
     */
    private fun checkPermissions(typePermission: String) {
        val chatActivity = requireActivity() as ChatActivity
        val hasPermission =
            hasPermissions(chatActivity, typePermission)

        when (typePermission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                if (hasPermission) {
                    viewModel.updatePermissionsGranted(typePermission, hasPermission)
                } else {
                    val permissions = arrayOf(
                        getImagePermissionByVersion(),
                        getAudioPermissionByVersion(),
                        getVideoPermissionByVersion(),
                        getReadExternalStoragePermission()
                    )
                    ActivityCompat.requestPermissions(
                        chatActivity,
                        permissions,
                        Constants.REQUEST_READ_STORAGE
                    )
                }
            }
            Manifest.permission.CAMERA -> {
                if (hasPermission) {
                    viewModel.updatePermissionsGranted(typePermission, hasPermission)
                } else {
                    ActivityCompat.requestPermissions(
                        chatActivity,
                        arrayOf(typePermission),
                        Constants.REQUEST_CAMERA_SHOW_PREVIEW
                    )
                }
            }
        }
    }

    private fun onTakePictureClick() {
        if (hasCameraPermission) {
            listener.onTakePictureOptionClicked()
            dismiss()
        } else {
            viewModel.updateCheckCameraPermissions()
        }
    }

    private fun onClickItem(file: FileGalleryItem) {
        if (isMultiselectMode) {
            viewModel.longClickItem(file)
        } else {
            val files = ArrayList<FileGalleryItem>()
            files.add(file)
            listener.onSendFilesSelected(files)
            dismiss()
        }
    }

    private fun onLongClickItem(file: FileGalleryItem) {
        viewModel.longClickItem(file)
    }
}