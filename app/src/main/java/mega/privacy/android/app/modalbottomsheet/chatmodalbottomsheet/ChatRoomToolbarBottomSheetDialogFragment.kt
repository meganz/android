package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetChatRoomToolbarBinding
import mega.privacy.android.app.interfaces.ChatRoomToolbarBottomSheetDialogActionListener
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.megachat.chatAdapters.FileStorageChatAdapter
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString

/**
 * Bottom Sheet Dialog which shows the chat options
 */
@AndroidEntryPoint
class ChatRoomToolbarBottomSheetDialogFragment : BottomSheetDialogFragment() {

    val viewModel: ChatRoomToolbarViewModel by viewModels()

    private lateinit var binding: BottomSheetChatRoomToolbarBinding

    private lateinit var listener: ChatRoomToolbarBottomSheetDialogActionListener

    private var isMultiselectMode = false

    private val filesAdapter by lazy {
        FileStorageChatAdapter(::onTakePictureClick, ::onClickItem, ::onLongClickItem)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), R.style.BottomSheetFragmentWithTransparentBackground)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
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
                binding.emptyGallery.isVisible = filesList.isNullOrEmpty()
                binding.list.isVisible = !filesList.isNullOrEmpty()
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
            viewModel.hasPermissionsGranted.collect { isGranted ->
                if (!isGranted) {
                    checkPermissionsDialog()
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
        val mLayoutManager: RecyclerView.LayoutManager =
                GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)

        binding.list.apply {
            clipToPadding = false
            setHasFixedSize(true)
            itemAnimator = Util.noChangeRecyclerViewItemAnimator()
            layoutManager = mLayoutManager
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
     * Show node permission dialog to ask for Node permissions.
     */
    private fun checkPermissionsDialog() {
        val chatActivity = requireActivity() as ChatActivity

        val hasStoragePermission = ContextCompat.checkSelfPermission(
                chatActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasStoragePermission) {
            ActivityCompat.requestPermissions(
                    chatActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.REQUEST_READ_STORAGE
            )
        } else {
            viewModel.updateReadStoragePermissions(true)
        }
    }

    private fun onTakePictureClick() {
        listener.onTakePictureOptionClicked()
        dismiss()
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