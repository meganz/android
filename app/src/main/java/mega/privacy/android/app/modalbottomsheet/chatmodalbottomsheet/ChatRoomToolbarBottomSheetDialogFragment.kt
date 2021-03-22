package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.AsyncTask
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.bottom_sheet_sort_by.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetChatRoomToolbarBinding
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatFileStorageAdapter
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.Util
import java.lang.ref.WeakReference
import java.util.*

class ChatRoomToolbarBottomSheetDialogFragment() : BaseBottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetChatRoomToolbarBinding
    private var downloadLocationDefaultPath: String? = null
    private var mPhotoUris: ArrayList<String>? = null
    private var adapter: MegaChatFileStorageAdapter? = null
    var imagesPath = ArrayList<String>()


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
                showGallery()
            }
        }

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
            //chatActivity.sendFromFileSystem()
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

        contentView = binding.root
        mainLinearLayout = binding.root.linear_layout
        items_layout = binding.root.linear_layout


        binding.recyclerViewGallery.setClipToPadding(false)
        binding.recyclerViewGallery.setHasFixedSize(true)
        binding.recyclerViewGallery.setItemAnimator(Util.noChangeRecyclerViewItemAnimator())

        mPhotoUris = ArrayList<String>()

        val mLayoutManager: RecyclerView.LayoutManager =
            GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)

        (mLayoutManager as GridLayoutManager).spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return adapter!!.getSpanSizeOfPosition(position)
            }
        }

        if (adapter == null) {
            adapter =
                MegaChatFileStorageAdapter(context, this, recyclerView, aB, mPhotoUris, dimImages)
            adapter!!.setHasStableIds(true)
        } else {
            adapter!!.setDimensionPhotos(dimImages)
            setNodes(mPhotoUris!!)
        }

        adapter!!.isMultipleSelect = false
        binding.recyclerViewGallery.setLayoutManager(mLayoutManager)
        binding.recyclerViewGallery.setAdapter(adapter)

        FetchPhotosTask(this).execute()
        checkAdapterItems(false)

        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }

    fun showGallery() {
//        fileStorageF = ChatFileStorageFragment.newInstance()
//        getSupportFragmentManager().beginTransaction()
//            .replace(R.id.fragment_container_file_storage, fileStorageF, "fileStorageF")
//            .commitNowAllowingStateLoss()

    }

    fun setNodes(photosUrisReceived: ArrayList<String>) {
        this.mPhotoUris = photosUrisReceived

        adapter!!.setNodes(mPhotoUris)
        checkAdapterItems(true)
    }

    public fun checkAdapterItems(fileLoaded: Boolean) {
        if (adapter!!.itemCount == 0) {
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
    fun updatePhotosUris(photoUris: List<String>?){
        mPhotoUris!!.clear()
        if (photoUris != null && photoUris.size > 0) {
            mPhotoUris!!.addAll(photoUris)
        }
    }


    fun createImagesPath(path: String?) {
        imagesPath.add(path)
    }

    class FetchPhotosTask(context: ChatRoomToolbarBottomSheetDialogFragment) : AsyncTask<Void, Void, String>() {
        private var mContextWeakReference: WeakReference<ChatRoomToolbarBottomSheetDialogFragment>? = WeakReference(
            context
        )

        override fun doInBackground(vararg params: Void?): List<String>? {
            val context: ChatRoomToolbarBottomSheetDialogFragment? = mContextWeakReference!!.get()
            if (context != null) {
                //get photos from gallery
                val projection = arrayOf(
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID
                )
                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val orderBy = MediaStore.Images.Media._ID + " DESC"
                var cursor: Cursor? = null
                try {
                    cursor = context.activity!!
                        .contentResolver.query(uri, projection, "", null, orderBy)
                    if (cursor != null) {
                        val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        val photoUris: MutableList<String> = ArrayList(cursor.count)
                        while (cursor.moveToNext()) {
                            photoUris.add("file://" + cursor.getString(dataColumn))
                            context.createImagesPath(cursor.getString(dataColumn))
                        }
                        return photoUris
                    }
                } catch (ex: Exception) {
                    LogUtil.logError("Exception is thrown", ex)
                } finally {
                    cursor?.close()
                }
            }
            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun onPostExecute(photoUris: List<String>?) {
            val context: ChatRoomToolbarBottomSheetDialogFragment? = mContextWeakReference!!.get()
            if (context != null) {
                context.updatePhotosUris(photoUris)
                context.checkAdapterItems(true)
            }
        }
    }

}