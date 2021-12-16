package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.adapters.FileStorageAdapter
import mega.privacy.android.app.databinding.BottomSheetChatRoomToolbarBinding
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.lollipop.megachat.FileGalleryItem
import mega.privacy.android.app.lollipop.tasks.FetchDeviceGalleryTask
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import java.util.*

class ChatRoomToolbarBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetChatRoomToolbarBinding
    private var downloadLocationDefaultPath: String? = null
    private var mPhotoUris: ArrayList<FileGalleryItem>? = null
    private lateinit var adapter: FileStorageAdapter

    lateinit var mainLinearLayout :LinearLayout

    @SuppressLint("SetTextI18n", "RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        downloadLocationDefaultPath = FileUtil.getDownloadLocation()

        binding = BottomSheetChatRoomToolbarBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )
        val chatActivity = requireActivity() as ChatActivityLollipop

        contentView = binding.root
        mainLinearLayout = binding.linearLayout

        mPhotoUris = ArrayList<FileGalleryItem>()

        binding.optionGallery.setOnClickListener{
            chatActivity.sendFromFileSystem()
            setStateBottomSheetBehaviorHidden()
        }

        binding.optionFile.setOnClickListener{
            chatActivity.sendFromCloud()
            setStateBottomSheetBehaviorHidden()
        }

        binding.optionVoice.setOnClickListener{
            chatActivity.optionCall(false)
            setStateBottomSheetBehaviorHidden()
        }

        binding.optionVideo.setOnClickListener{
            chatActivity.optionCall(true)
            setStateBottomSheetBehaviorHidden()
        }

        binding.optionScan.setOnClickListener{
            setStateBottomSheetBehaviorHidden()
        }

        binding.optionGif.setOnClickListener{
            chatActivity.sendGif()
            setStateBottomSheetBehaviorHidden()
        }

        binding.optionLocation.setOnClickListener{
            chatActivity.sendLocation()
            setStateBottomSheetBehaviorHidden()
        }

        binding.optionContact.setOnClickListener{
            chatActivity.chooseContactsDialog()
            setStateBottomSheetBehaviorHidden()
        }

        setupListView()
        setupListAdapter()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                uploadGallery()
            }
        }

        dialog.setContentView(contentView)

        //mBehavior = BottomSheetBehavior.from(mainLinearLayout.parent as View)
        //mBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupListView() {
        val mLayoutManager: RecyclerView.LayoutManager =
            GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)

        binding.recyclerViewGallery.clipToPadding = false
        binding.recyclerViewGallery.setHasFixedSize(true)
        binding.recyclerViewGallery.setItemAnimator(Util.noChangeRecyclerViewItemAnimator())
        binding.recyclerViewGallery.setLayoutManager(mLayoutManager)
    }


    private fun setupListAdapter() {
        context?.let {
            adapter = FileStorageAdapter(it, mPhotoUris)

        }
        adapter.setHasStableIds(true)
        binding.recyclerViewGallery.adapter = adapter
    }

    fun uploadGallery() {
        setNodes(mPhotoUris!!)
        FetchDeviceGalleryTask(context).execute()
        checkAdapterItems(false)
    }

    fun setNodes(photosUrisReceived: ArrayList<FileGalleryItem>) {
        this.mPhotoUris = photosUrisReceived
        adapter.setNodes(mPhotoUris)
        checkAdapterItems(true)
    }

    private fun checkAdapterItems(fileLoaded: Boolean) {
        if (adapter.itemCount == 0) {
            binding.recyclerViewGallery.visibility = View.GONE
            if (fileLoaded) {
                binding.emptyGallery.setText(R.string.file_storage_empty_folder)
            }
            binding.emptyGallery.visibility = View.VISIBLE
            return
        }

        binding.recyclerViewGallery.visibility = View.VISIBLE
        binding.emptyGallery.visibility = View.GONE
    }

    fun updateFiles(files: List<FileGalleryItem>?){
        if (files!!.isNotEmpty()) {
            mPhotoUris!!.clear()
            mPhotoUris!!.addAll(files)
            adapter.notifyDataSetChanged()
        }

        checkAdapterItems(true)
    }
}