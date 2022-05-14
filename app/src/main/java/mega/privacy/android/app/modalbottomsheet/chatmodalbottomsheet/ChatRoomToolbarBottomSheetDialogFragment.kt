package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetChatRoomToolbarBinding
import mega.privacy.android.app.interfaces.ChatRoomToolbarBottomSheetDialogActionListener
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.LogUtil.logDebug

/**
 * Bottom Sheet Dialog which shows the chat options
 */
@AndroidEntryPoint
class ChatRoomToolbarBottomSheetDialogFragment : BottomSheetDialogFragment() {

    val viewModel: ChatRoomToolbarViewModel by activityViewModels()

    private lateinit var binding: BottomSheetChatRoomToolbarBinding

    private lateinit var listener: ChatRoomToolbarBottomSheetDialogActionListener

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

        lifecycleScope.launchWhenStarted {
            viewModel.gallery.collect {
                logDebug("Recovered gallery. Num items ${it.size}")
            }
        }

        setupButtons()
        checkPermissionsDialog()
        //setupListView()
        //setupListAdapter()

        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Setup option buttons of the toolbar.
     */
    private fun setupButtons() {
        binding.optionGallery.setOnClickListener {
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
            //uploadGallery()
        }

    }

    /*  private fun setupListView() {
          val mLayoutManager: RecyclerView.LayoutManager =
              GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)

          binding.recyclerViewGallery?.apply {
              clipToPadding = false
              setHasFixedSize(true)
              setItemAnimator(Util.noChangeRecyclerViewItemAnimator())
              setLayoutManager(mLayoutManager)
          }
      }*/

    /*  private fun setupListAdapter() {
          context?.let {
              //adapter = FileStorageAdapter(it)
          }
          adapter.setHasStableIds(true)
          binding.recyclerViewGallery.adapter = adapter
      }*/
}