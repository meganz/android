package mega.privacy.android.app.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.file.StorageId
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.id
import mega.privacy.android.app.FileDocument
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.main.adapters.FileStorageAdapter
import mega.privacy.android.app.main.adapters.FileStorageAdapter.CenterSmoothScroller
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.createViewFolderIntent
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import timber.log.Timber
import java.io.File
import java.util.Collections
import java.util.Stack

/**
 * Activity to browse local files and folders on the device
 */
class FileStorageActivity : PasscodeActivity(), Scrollable {

    private var mode: Mode? = null
    private var path: File? = null
    private var root: File? = null
    private var highlightFilePath: String? = null
    private lateinit var lastPositionStack: Stack<Int>
    private lateinit var viewContainer: RelativeLayout
    private var contentText: TextView? = null
    private var listView: RecyclerView? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var emptyImageView: ImageView? = null
    private var emptyTextView: TextView? = null
    private var pickFolderType: PickFolderType? = null
    private var prompt: String? = null
    private var url: String? = null
    private var size: Long = 0
    private var documentHashes: LongArray? = null
    private var serializedNodes: ArrayList<String>? = null
    private var adapter: FileStorageAdapter? = null
    private var toolbarView: Toolbar? = null

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_filestorage)
        this.consumeInsetsWithToolbar(
            WindowInsetsCompat.Type.systemBars(),
            findViewById(R.id.toolbar_filestorage)
        )
        viewContainer = findViewById(R.id.file_storage_container)
        contentText = findViewById(R.id.file_storage_content_text)
        listView = findViewById(R.id.file_storage_list_view)
        emptyImageView = findViewById(R.id.file_storage_empty_image)
        emptyTextView = findViewById(R.id.file_storage_empty_text)
        toolbarView = findViewById(R.id.toolbar_filestorage)
        listView = findViewById(R.id.file_storage_list_view)

        lastPositionStack = Stack()
        prompt = intent.getStringExtra(EXTRA_PROMPT)
        prompt?.let { showSnackbar(viewContainer, it) }

        setPickFolderType(intent.getStringExtra(PICK_FOLDER_TYPE))

        if (intent.getBooleanExtra(EXTRA_SAVE_RECOVERY_KEY, false)) {
            createRKFile()
            return
        } else if (pickFolderType == PickFolderType.CAMERA_UPLOADS_FOLDER) {
            openPickCUFolderFromSystem()
            return
        } else if (pickFolderType == PickFolderType.DOWNLOAD_FOLDER) {
            if (Util.isAndroid11OrUpper()) {
                path = FileUtil.buildDefaultDownloadDir(this)
                path?.mkdirs()
                finishPickFolder()
                return
            }
            openPickDownloadFolderFromSystem()
            return
        }
        if (!hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermission(
                this,
                Constants.REQUEST_WRITE_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        setSupportActionBar(toolbarView)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        mode = Mode.getFromIntent(intent)
        if (mode == Mode.PICK_FOLDER) {
            intent.extras?.let {
                documentHashes = it.getLongArray(EXTRA_DOCUMENT_HASHES)
                url = it.getString(EXTRA_URL)
                it.getLong(EXTRA_SIZE).let { sizeValue ->
                    size = sizeValue
                }
            }
            serializedNodes = intent.getStringArrayListExtra(EXTRA_SERIALIZED_NODES)
        } else if (mode == Mode.BROWSE_FILES) {
            supportActionBar?.title =
                getString(R.string.browse_files_label)
        }

        if (savedInstanceState?.containsKey(PATH) == true) {
            savedInstanceState.getString(PATH)?.let {
                path = File(it)
            }
        }

        emptyImageView?.setImageResource(mega.privacy.android.icon.pack.R.drawable.ic_empty_folder_glass)
        var textToShow = getString(R.string.file_browser_empty_folder_new)
        try {
            textToShow = textToShow.replace(
                "[A]", ("<font color=\'"
                        + getColorHexString(
                    this,
                    R.color.grey_900_grey_100
                ) + "\'>")
            )
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace(
                "[B]", ("<font color=\'"
                        + getColorHexString(
                    this,
                    R.color.grey_300_grey_600
                ) + "\'>")
            )
            textToShow = textToShow.replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting text")
        }
        emptyTextView?.text = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)

        listView?.addItemDecoration(SimpleDividerItemDecoration(this))
        mLayoutManager = LinearLayoutManager(this)
        listView?.setLayoutManager(mLayoutManager)
        listView?.setItemAnimator(Util.noChangeRecyclerViewItemAnimator())
        listView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
        if (adapter == null) {
            adapter = FileStorageAdapter(this, mode)
            listView?.setAdapter(adapter)
        }

        if (mode == Mode.BROWSE_FILES) {
            intent.extras?.let {
                val extraPath = it.getString(EXTRA_PATH)
                if (!extraPath.isNullOrEmpty()) {
                    path = File(extraPath)
                    root = path
                    highlightFilePath = extraPath + File.separator + it.getString(EXTRA_FILE_NAME)
                }
            }
            checkPath()
        }
    }

    /**
     * onCreateOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_filestorage_menu, menu)
        openInFileManagerMenuItem =
            menu.findItem(R.id.action_open_in_file_manager)
        if (mode == Mode.BROWSE_FILES) {
            checkMenuVisibility()
        }
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        // Handle presses on the action bar items
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_open_in_file_manager -> {
                try {
                    val intent = this.createViewFolderIntent(Uri.fromFile(path))
                    if (intent != null) {
                        startActivity(intent)
                        return true
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
                showSnackbar(
                    viewContainer,
                    resources.getString(R.string.filestorage_snackbar_file_manager_is_not_available)
                )
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }


    /**
     * Launches the System picker to create the Recovery Key file in the chosen path.
     */
    private fun createRKFile() {
        val defaultDownloadDir = FileUtil.buildDefaultDownloadDir(
            this
        )
        defaultDownloadDir.mkdirs()
        val initialUri = Uri.parse(defaultDownloadDir.absolutePath)

        try {
            startActivityForResult(
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(Constants.TYPE_TEXT_PLAIN)
                    .putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
                    .putExtra(
                        Intent.EXTRA_TITLE, FileUtil.getRecoveryKeyFileName(
                            this
                        )
                    ), REQUEST_SAVE_RK
            )
        } catch (e: Exception) {
            Timber.d("Can not handle action Intent.ACTION_CREATE_DOCUMENT")
        }
    }

    /**
     * On Android 11 and upper we cannot show our app picker, we must use the system one.
     * So opens the system file picker in order to give the option to chose a CU or MU local folder.
     */
    private fun openPickCUFolderFromSystem() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), REQUEST_PICK_CU_FOLDER
            )
        } catch (e: ActivityNotFoundException) {
            showOpenDocumentWarningAndFinish(e)
        }
    }

    /**
     * Opens the file picker in order to allow the user choose a default download location.
     */
    private fun openPickDownloadFolderFromSystem() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
                REQUEST_PICK_DOWNLOAD_FOLDER
            )
        } catch (e: ActivityNotFoundException) {
            showOpenDocumentWarningAndFinish(e)
        }
    }

    /**
     * Shows a warning when there is no app to pick folders and finishes.
     *
     * @param e The caught exception.
     */
    private fun showOpenDocumentWarningAndFinish(e: ActivityNotFoundException) {
        Timber.e(e, "Error launching ACTION_OPEN_DOCUMENT_TREE.")
        showSnackbar(
            viewContainer,
            getString(R.string.general_warning_no_picker)
        )
        Handler().postDelayed({ this.finish() }, Constants.LONG_SNACKBAR_DURATION)
    }

    /**
     * Sets the type of pick folder action.
     *
     * @param pickFolderString the type of pick folder action.
     */
    private fun setPickFolderType(pickFolderString: String?) {
        if (TextUtil.isTextEmpty(pickFolderString)) {
            pickFolderType = PickFolderType.NONE_ONLY_DOWNLOAD
        } else if (pickFolderString == PickFolderType.CAMERA_UPLOADS_FOLDER.folderType) {
            pickFolderType = PickFolderType.CAMERA_UPLOADS_FOLDER
        } else if (pickFolderString == PickFolderType.DOWNLOAD_FOLDER.folderType) {
            pickFolderType = PickFolderType.DOWNLOAD_FOLDER
        }
    }

    /**
     * Shows the empty view or the list view depending on if there are items in the adapter.
     * Hides both if the root view with Internal storage and External storage is shown.
     */
    private fun showEmptyState() {
        if ((adapter?.itemCount ?: 0) > 0) {
            listView?.visibility = View.VISIBLE
            emptyImageView?.visibility = View.GONE
            emptyTextView?.visibility = View.GONE
        } else {
            listView?.visibility = View.GONE
            emptyImageView?.visibility = View.VISIBLE
            emptyTextView?.visibility = View.VISIBLE
        }
    }

    /**
     * Changes the path shown in the screen or finish the activity if the current one is not valid.
     */
    private fun checkPath() {
        path?.let {
            changeFolder(it)
        } ?: run {
            Timber.e("Current path is not valid (null)")
            Util.showErrorAlertDialog(
                getString(R.string.error_io_problem),
                true, this
            )
        }
    }

    /**
     * onConfigurationChanged
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        emptyImageView?.setImageResource(mega.privacy.android.icon.pack.R.drawable.ic_empty_folder_glass)
    }

    /**
     * onSaveInstanceState
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        path?.let { outState.putString(PATH, it.absolutePath) }
        super.onSaveInstanceState(outState)
    }

    /*
     * Open new folder
     * @param newPath New folder path
     */
    @SuppressLint("NewApi")
    private fun changeFolder(newPath: File) {
        setFiles(newPath)
        path = newPath
        contentText?.text = path?.absolutePath
        checkMenuVisibility()
    }

    private fun checkMenuVisibility() {
        openInFileManagerMenuItem?.isVisible =
            mode == Mode.BROWSE_FILES && !isInCacheDirectory(path)
    }

    /*
     * Check if the folder is in the cache directory by matching last two directories
     * e.g. mega.privacy.android.app/cache
     */
    private fun isInCacheDirectory(folderPath: File?): Boolean {
        if (folderPath == null) return false
        var cacheDir = externalCacheDir
        if (cacheDir == null) cacheDir = getCacheDir()
        if (cacheDir == null) return false
        val cacheDirParent = cacheDir.parentFile ?: return false
        return folderPath
            .absolutePath
            .contains(cacheDirParent.name + "/" + cacheDir.name)
    }

    /*
     * Update file list for new folder
     */
    private fun setFiles(path: File?) {
        val documents: MutableList<FileDocument> = ArrayList()
        if (path == null || !path.canRead()) {
            Util.showErrorAlertDialog(
                getString(R.string.error_io_problem),
                true, this
            )
            return
        }

        val files = path.listFiles()
        var highlightFilePosition = -1
        var isHighlightFileFound = false

        if (files != null) {
            for (file in files) {
                val isHighlighted =
                    highlightFilePath != null && highlightFilePath == file.absolutePath
                val document = FileDocument(file, isHighlighted)
                if (document.isHidden) {
                    continue
                }
                documents.add(document)
                if (isHighlighted) {
                    isHighlightFileFound = true
                }
            }

            Collections.sort(documents, CustomComparator())

            if (isHighlightFileFound) {
                for (i in documents.indices) {
                    if (documents[i].isHighlighted) {
                        highlightFilePosition = i
                        break
                    }
                }
            }
        }

        adapter?.setFiles(documents)

        if (highlightFilePosition != -1) {
            val smoothScroller: RecyclerView.SmoothScroller = CenterSmoothScroller(
                listView?.context
            )
            smoothScroller.targetPosition = highlightFilePosition
            mLayoutManager?.startSmoothScroll(smoothScroller)
        }

        showEmptyState()
    }

    /**
     * Sort the files and folder
     *//*
     * Comparator to sort the files
     */
    inner class CustomComparator : Comparator<FileDocument> {
        /**
         * Compare two files
         */
        override fun compare(o1: FileDocument, o2: FileDocument): Int {
            if (o1.isFolder != o2.isFolder) {
                return if (o1.isFolder) -1 else 1
            }
            return o1.name.compareTo(o2.name, ignoreCase = true)
        }
    }

    private fun finishPickFolder() {
        if (path?.absolutePath?.isEmpty() == true) {
            Timber.e("The new Local Folder is invalid")
        } else {
            Timber.d("Successfully selected the new Local Folder")
        }
        val intent = Intent()
        intent.putExtra(EXTRA_PATH, path?.absolutePath)
        intent.putExtra(EXTRA_IS_FOLDER_IN_SD_CARD, isFolderInSDCard)
        intent.putExtra(EXTRA_DOCUMENT_HASHES, documentHashes)
        intent.putStringArrayListExtra(EXTRA_SERIALIZED_NODES, serializedNodes)
        intent.putExtra(EXTRA_URL, url)
        intent.putExtra(EXTRA_SIZE, size)
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * ItemClick from adapter
     */
    fun itemClick(position: Int) {
        Timber.d("Position: %s", position)
        val document = adapter?.getDocumentAt(position) ?: return
        if (document.isFolder) {
            if (!document.file.canRead()) {
                return
            }
            lastPositionStack.push(mLayoutManager?.findFirstCompletelyVisibleItemPosition())
            changeFolder(document.file)
        } else if (mode == Mode.BROWSE_FILES) {
            val file = adapter?.getItem(position)?.file
            if (file != null && FileUtil.isFileAvailable(file)) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uri =
                    FileProvider.getUriForFile(this, Constants.AUTHORITY_STRING_FILE_PROVIDER, file)
                if (uri != null) {
                    intent.setDataAndType(uri, typeForName(file.name).type)
                } else {
                    Timber.w("The file cannot be opened, uri is null")
                    return
                }
                if (MegaApiUtils.isIntentAvailable(this, intent)) {
                    startActivity(intent)
                }
            } else {
                showSnackbar(
                    viewContainer,
                    getString(R.string.corrupt_video_dialog_text)
                )
            }
        }
    }

    /**
     * onBackPressed
     */
    override fun onBackPressed() {
        val psaWebBrowser = psaWebBrowser
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return
        retryConnectionsAndSignalPresence()
        // Finish activity if at the root
        if (path == root) {
            super.onBackPressed()
        } else {
            // Go one level higher otherwise
            path?.parentFile?.let { changeFolder(it) }
            var lastVisiblePosition = 0
            if (lastPositionStack.isNotEmpty()) {
                lastVisiblePosition = lastPositionStack.pop()
            }
            if (lastVisiblePosition >= 0) {
                mLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
            }
        }
    }

    /**
     * onActivityResult
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (resultCode != RESULT_OK || intent == null) {
            // If resultCode is not Activity.RESULT_OK, means cancelled, so only finish.
            Timber.d("Result code: %s", resultCode)
            finish()
            return
        }

        val uri = intent.data
        val isFolderInPrimaryStorage: Boolean

        when (requestCode) {
            REQUEST_SAVE_RK -> {
                setResult(RESULT_OK, Intent().setData(uri))
                finish()
            }

            REQUEST_PICK_CU_FOLDER -> {
                Timber.d("Folder picked from system picker")
                contentResolver.takePersistableUriPermission(
                    uri ?: return,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                isFolderInPrimaryStorage = setPathAndCheckIfIsPrimary(uri)
                isFolderInSDCard = !isFolderInPrimaryStorage
                finishPickFolder()
            }

            REQUEST_PICK_DOWNLOAD_FOLDER -> {
                contentResolver.takePersistableUriPermission(
                    uri ?: return,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                isFolderInPrimaryStorage = setPathAndCheckIfIsPrimary(uri)

                if (!isFolderInPrimaryStorage) {
                    dbH.sdCardUri = uri.toString()
                }

                finishPickFolder()
            }
        }
    }

    /**
     * Sets as path the picked folder and checks if the chosen path is in the primary storage.
     *
     * @param uri The Uri to set as path.
     * @return True if the chosen path is in the primary storage, false otherwise.
     */
    private fun setPathAndCheckIfIsPrimary(uri: Uri): Boolean {
        val documentFile = DocumentFile.fromTreeUri(this, uri)
        if (documentFile == null) {
            Timber.e("DocumentFile is null")
            finish()
            return true
        }

        path = File(documentFile.getAbsolutePath(this))
        val documentId = documentFile.id

        return documentId == StorageId.PRIMARY || documentId.contains(StorageId.PRIMARY)
    }

    /**
     * Change the elevation of the ActionBar
     */
    private fun changeActionBarElevation(withElevation: Boolean) {
        changeStatusBarColorForElevation(this, withElevation)
        val elevation = resources.getDimension(R.dimen.toolbar_elevation)
        toolbarView?.elevation = if (withElevation) elevation else 0f
    }

    /**
     * checkScroll
     */
    override fun checkScroll() {
        listView?.let { listView ->
            changeActionBarElevation(listView.canScrollVertically(Constants.SCROLLING_UP_DIRECTION))
        }
    }

    /**
     * PickFolderType
     *
     * @param folderType The type of folder to pick
     */
    enum class PickFolderType(val folderType: String) {
        /**
         * Used to select a new Primary or Secondary Folder
         */
        CAMERA_UPLOADS_FOLDER("CAMERA_UPLOADS_FOLDER"),

        /**
         * Used to select a new Download Folder
         */
        DOWNLOAD_FOLDER("DOWNLOAD_FOLDER"),

        /**
         * Used to select other folder
         */
        NONE_ONLY_DOWNLOAD("NONE_ONLY_DOWNLOAD")
    }

    private var isFolderInSDCard = false
    private var openInFileManagerMenuItem: MenuItem? = null

    /**
     * Pick modes
     * @param action The action to perform
     */
    enum class Mode(val action: String) {
        /**
         * Select single folder
         */
        PICK_FOLDER("ACTION_PICK_FOLDER"),

        /**
         * Browse files
         */
        BROWSE_FILES("ACTION_BROWSE_FILES");

        companion object {
            /**
             * Get the mode from the intent
             */
            fun getFromIntent(intent: Intent): Mode {
                return if (intent.action == BROWSE_FILES.action) {
                    BROWSE_FILES
                } else {
                    PICK_FOLDER
                }
            }
        }
    }

    companion object {
        private const val PATH = "PATH"
        const val PICK_FOLDER_TYPE: String = "PICK_FOLDER_TYPE"
        private const val REQUEST_SAVE_RK = 1122
        private const val REQUEST_PICK_CU_FOLDER = 1133
        private const val REQUEST_PICK_DOWNLOAD_FOLDER = 1144
        const val EXTRA_URL: String = "fileurl"
        const val EXTRA_SIZE: String = "filesize"
        const val EXTRA_SERIALIZED_NODES: String = "serialized_nodes"
        const val EXTRA_DOCUMENT_HASHES: String = "document_hash"
        const val EXTRA_SAVE_RECOVERY_KEY: String = "save_recovery_key"
        const val EXTRA_PATH: String = "filepath"
        const val EXTRA_FILE_NAME: String = "filename"
        const val EXTRA_IS_FOLDER_IN_SD_CARD: String = "is_folder_in_sd_card"
        const val EXTRA_PROMPT: String = "prompt"
    }
}
