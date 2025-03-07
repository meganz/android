package mega.privacy.android.app.presentation.filestorage

import mega.privacy.android.shared.resources.R as sharedR
import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.file.StorageId
import com.anggrayudi.storage.file.id
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.extensions.consumeInsetsWithToolbar
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.main.adapters.FileStorageAdapter
import mega.privacy.android.app.main.adapters.FileStorageAdapter.CenterSmoothScroller
import mega.privacy.android.app.presentation.filestorage.model.FileStorageUiState
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
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.file.FileStorageType
import mega.privacy.android.domain.entity.uri.UriPath
import timber.log.Timber
import java.util.Stack

/**
 * Activity to browse local files and folders on the device
 */
@AndroidEntryPoint
class FileStorageActivity : PasscodeActivity(), Scrollable {

    private val viewModel: FileStorageViewModel by viewModels()
    private var mode: Mode? = null
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
    private var loading: View? = null

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
        emptyImageView = findViewById(R.id.file_storage_empty_image)
        emptyTextView = findViewById(R.id.file_storage_empty_text)
        toolbarView = findViewById(R.id.toolbar_filestorage)
        listView = findViewById(R.id.file_storage_list_view)
        loading = findViewById(R.id.loading)

        lastPositionStack = Stack()
        prompt = intent.getStringExtra(EXTRA_PROMPT)
        prompt?.let { showSnackbar(viewContainer, it) }

        setPickFolderType(intent.getStringExtra(PICK_FOLDER_TYPE))

        if (intent.getBooleanExtra(EXTRA_SAVE_RECOVERY_KEY, false)) {
            createRKFile()
            return
        } else if (pickFolderType == PickFolderType.CAMERA_UPLOADS_FOLDER) {
            openPickCUFolderFromSystem()
            observeEvents()
            return
        } else if (pickFolderType == PickFolderType.DOWNLOAD_FOLDER) {
            openPickDownloadFolderFromSystem()
            observeEvents()
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
        }

        if (savedInstanceState?.containsKey(PATH) == true) {
            savedInstanceState.getString(PATH)?.let {
                viewModel.setRootPath(UriPath(it))
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
                    viewModel.setRootPath(
                        uriPath = UriPath(extraPath),
                        updateStorageType = mode == Mode.BROWSE_FILES,
                        highlightFileName = it.getString(EXTRA_FILE_NAME)
                    )
                }
            }
        }
        observeUiState()
        observeEvents()
    }

    private fun observeUiState() {
        collectFlow(viewModel.uiState.distinctUntilChangedBy { (it as? FileStorageUiState.Loaded)?.children }) {
            when (it) {
                is FileStorageUiState.Loaded -> {
                    checkMenuVisibility(it.currentFolder?.uriPath)
                    contentText?.text = it.currentFolderPath
                    adapter?.setFiles(it.children)
                    showEmptyState()
                    it.getHighlightFilePosition()?.let { highlightFilePosition ->
                        Handler(Looper.getMainLooper()).post {
                            val smoothScroller = CenterSmoothScroller(listView?.context)
                            smoothScroller.targetPosition = highlightFilePosition
                            mLayoutManager?.startSmoothScroll(smoothScroller)
                        }
                    }
                }

                FileStorageUiState.Loading -> {
                    showLoadingState()
                }
            }
        }
        collectFlow(viewModel.uiLoadedState.map { it.storageType }.distinctUntilChanged()) {
            if (mode == Mode.BROWSE_FILES) {
                supportActionBar?.title = when (it) {
                    is FileStorageType.Internal -> it.deviceModel
                    is FileStorageType.SdCard -> getString(sharedR.string.general_sd_card)
                    is FileStorageType.Unknown -> getString(R.string.browse_files_label)
                }
            }
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.uiLoadedState.map { it.folderPickedEvent }.distinctUntilChanged()) {
            (it as? StateEventWithContentTriggered)?.content?.let { uriPath ->
                viewModel.consumeFolderPickedEvent()
                finishPickFolder(uriPath)
            }
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
            checkMenuVisibility(null)
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
                lifecycleScope.launch {
                    viewFolderIntent()?.let {
                        startActivity(it)
                    } ?: run {
                        showSnackbar(
                            viewContainer,
                            resources.getString(R.string.filestorage_snackbar_file_manager_is_not_available)
                        )
                    }
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private suspend fun viewFolderIntent(): Intent? =
        runCatching {
            viewModel.getCurrentPathContentUri()?.let {
                createViewFolderIntent(it.toUri(), viewModel.getCurrentFilePath())
            }
        }.onFailure { e ->
            Timber.e(e)
        }.getOrNull()


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
            Timber.d(e, "Can not handle action Intent.ACTION_CREATE_DOCUMENT")
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
        loading?.apply {
            animate().cancel()
            visibility = View.GONE
        }
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

    private fun showLoadingState() {
        //this should probably change to skimmer effect once migrated to compose
        listView?.visibility = View.GONE
        emptyImageView?.visibility = View.GONE
        emptyTextView?.visibility = View.GONE
        loading?.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(500)
                .start()
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
        viewModel.getCurrentUriPath()?.let { outState.putString(PATH, it.value) }
        super.onSaveInstanceState(outState)
    }

    /*
     * Open new folder
     * @param newPath New folder path
     */
    @SuppressLint("NewApi")
    private fun changeFolder(newPath: UriPath) {
        viewModel.goToChild(uriPath = newPath)
        checkMenuVisibility(newPath)
    }

    private fun checkMenuVisibility(currentPath: UriPath?) {
        lifecycleScope.launch {
            openInFileManagerMenuItem?.isVisible =
                mode == Mode.BROWSE_FILES
                        && !viewModel.isInCacheDirectory()
                        && viewFolderIntent() != null
        }
    }

    private fun finishPickFolder(uriPath: UriPath) {
        val intent = Intent()
        intent.putExtra(EXTRA_PATH, uriPath.value)
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
            if (document.canRead) {
                document.isHidden
                lastPositionStack.push(mLayoutManager?.findFirstCompletelyVisibleItemPosition())
            }
            changeFolder(document.uriPath)
        } else if (mode == Mode.BROWSE_FILES) {
            val documentFile = adapter?.getItem(position)
            if (documentFile != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val uri = documentFile.uriPath.toUri()
                intent.setDataAndType(uri, typeForName(documentFile.name).type)
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
        retryConnectionsAndSignalPresence()
        // Go one level higher if not at root, otherwise finish
        if (viewModel.goToParent()) {

            var lastVisiblePosition = 0
            if (lastPositionStack.isNotEmpty()) {
                lastVisiblePosition = lastPositionStack.pop()
            }
            if (lastVisiblePosition >= 0) {
                mLayoutManager?.scrollToPositionWithOffset(lastVisiblePosition, 0)
            }
        } else {
            super.onBackPressed()
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
                viewModel.folderPicked(uri.toString())
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

                viewModel.folderPicked(uri.toString())
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

        viewModel.setRootPath(UriPath(uri.toString()))
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
