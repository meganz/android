package mega.privacy.android.app.presentation.filelink

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication.Companion.isClosedChat
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityFileLinkBinding
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieDialogHandler
import mega.privacy.android.app.imageviewer.ImageViewerActivity.Companion.getIntentForSingleNode
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.DecryptAlertDialog
import mega.privacy.android.app.main.DecryptAlertDialog.DecryptDialogListener
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.clouddrive.FileLinkViewModel
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.transfers.TransfersManagementActivity
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException.ChildDoesNotExistsException
import mega.privacy.android.app.usecase.exception.MegaNodeException.ParentDoesNotExistException
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.ColorUtils.getColorForElevation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.shareLink
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.checkNotificationsPermission
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.math.abs

/**
 * FileLinkActivity
 *
 * @property checkNameCollisionUseCase
 * @property legacyCopyNodeUseCase
 * @property cookieDialogHandler
 */
@AndroidEntryPoint
class FileLinkActivity : TransfersManagementActivity(), MegaRequestListenerInterface,
    View.OnClickListener, DecryptDialogListener, SnackbarShower {

    @Inject
    lateinit var checkNameCollisionUseCase: CheckNameCollisionUseCase

    @Inject
    lateinit var legacyCopyNodeUseCase: LegacyCopyNodeUseCase

    @Inject
    lateinit var cookieDialogHandler: CookieDialogHandler

    private var url: String? = null
    private var statusDialog: AlertDialog? = null
    private var preview: Bitmap? = null
    private var document: MegaNode? = null
    private var decryptionIntroduced = false
    private var importClicked = false
    private var target: MegaNode? = null
    private var mKey: String? = null
    private var shareMenuItem: MenuItem? = null
    private var upArrow: Drawable? = null
    private var drawableShare: Drawable? = null

    private lateinit var binding: ActivityFileLinkBinding
    private val viewModel: FileLinkViewModel by viewModels()
    private var decryptAlertDialog: DecryptAlertDialog? = null

    private val nodeSaver = NodeSaver(
        this, this, this,
        showSaveToDeviceConfirmDialog(this)
    )

    @SuppressLint("CheckResult")
    private val selectImportFolderResult =
        ActivityResultCallback<ActivityResult> { activityResult ->
            val resultCode = activityResult.resultCode
            val intent = activityResult.data

            if (resultCode != RESULT_OK || intent == null) {
                return@ActivityResultCallback
            }

            if (!viewModel.isConnected) {
                try {
                    statusDialog?.dismiss()
                } catch (ex: Exception) {
                    Timber.e(ex)
                }
                showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem)
                )
                return@ActivityResultCallback
            }

            val toHandle = intent.getLongExtra("IMPORT_TO", 0)
            target = megaApi.getNodeByHandle(toHandle)
            statusDialog = createProgressDialog(this, getString(R.string.general_importing))
            statusDialog?.show()
            if (document == null) {
                importClicked = true
            } else {
                checkCollisionBeforeCopying()
            }
        }

    private val selectImportFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        selectImportFolderResult
    )

    public override fun onDestroy() {
        megaApi.removeRequestListener(this)
        decryptAlertDialog?.dismissAllowingStateLoss()
        nodeSaver.destroy()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate()")
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        intent?.let { url = it.dataString }
        if (dbH.credentials != null && (megaApi.rootNode == null)) {
            Timber.d("Refresh session - sdk or karere")
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            intent.data = Uri.parse(url)
            intent.action = Constants.ACTION_OPEN_FILE_LINK_ROOTNODES_NULL
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
            return
        }

        savedInstanceState?.let { nodeSaver.restoreState(savedInstanceState) }
        binding = ActivityFileLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            setSupportActionBar(toolbarFileLink)

            fileLinkInfoCollapseToolbar.apply {
                expandedTitleMarginBottom =
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        Util.scaleHeightPx(60, outMetrics)
                    } else
                        Util.scaleHeightPx(35, outMetrics)
                expandedTitleMarginStart =
                    resources.getDimension(R.dimen.bottom_sheet_item_divider_margin_start).toInt()
            }

            fileInfoImageLayout.visibility = View.GONE

            contentFileLink.buttonPreviewContent.apply {
                setOnClickListener(this@FileLinkActivity)
                isEnabled = false
                visibility = View.GONE
            }

            fileLinkButtonDownload.apply {
                setOnClickListener(this@FileLinkActivity)
                visibility = View.GONE
            }

            fileLinkButtonImport.apply {
                setOnClickListener(this@FileLinkActivity)
                visibility = View.GONE
            }
        }

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
        setTransfersWidgetLayout(findViewById(R.id.transfers_widget_layout))

        try {
            statusDialog?.dismiss()
        } catch (e: Exception) {
            Timber.e(e)
        }

        url?.let { importLink(it) } ?: Timber.w("url NULL")
        binding.fileLinkFragmentContainer.post { cookieDialogHandler.showDialogIfNeeded(this) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        cookieDialogHandler.showDialogIfNeeded(this, true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver.saveState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        menuInflater.inflate(R.menu.file_folder_link_action, menu)
        upArrow = ContextCompat.getDrawable(applicationContext, R.drawable.ic_arrow_back_white)
        upArrow = upArrow?.mutate()
        drawableShare =
            ContextCompat.getDrawable(applicationContext, R.drawable.ic_social_share_white)
        drawableShare = drawableShare!!.mutate()
        shareMenuItem = menu.findItem(R.id.share_link)
        trySetupCollapsingToolbar()
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected")
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.share_link -> shareLink(this, url)
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * setup collapsing toolbar
     */
    private fun trySetupCollapsingToolbar() {
        if (shareMenuItem == null || document == null) {
            return
        }
        val statusBarColor =
            getColorForElevation(this, resources.getDimension(R.dimen.toolbar_elevation))
        if (Util.isDarkMode(this)) {
            binding.fileLinkInfoCollapseToolbar.setContentScrimColor(statusBarColor)
        }
        if (preview != null) {
            binding.appBar.addOnOffsetChangedListener { appBarLayout: AppBarLayout, offset: Int ->
                if (offset == 0) {
                    // Expanded
                    setColorFilterWhite()
                } else {
                    if (offset < 0 && abs(offset) >= appBarLayout.totalScrollRange / 2) {
                        // Collapsed
                        setColorFilterBlack()
                    } else {
                        setColorFilterWhite()
                    }
                }
            }
            binding.fileLinkInfoCollapseToolbar.apply {
                setCollapsedTitleTextColor(
                    ContextCompat.getColor(
                        this@FileLinkActivity,
                        R.color.grey_087_white_087
                    )
                )
                setExpandedTitleColor(
                    ContextCompat.getColor(
                        this@FileLinkActivity,
                        R.color.white_alpha_087
                    )
                )
                setStatusBarScrimColor(statusBarColor)
            }
        } else {
            binding.fileLinkInfoCollapseToolbar.setStatusBarScrimColor(statusBarColor)
            setColorFilterBlack()
        }
    }

    /**
     * Set menu item color filter to black
     */
    private fun setColorFilterBlack() {
        val color = getColor(R.color.grey_087_white_087)
        upArrow?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        if (shareMenuItem != null) {
            drawableShare?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            shareMenuItem?.icon = drawableShare
        }
    }

    /**
     * Set menu item color filter to white
     */
    private fun setColorFilterWhite() {
        val color = getColor(R.color.white_alpha_087)
        upArrow?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        supportActionBar?.setHomeAsUpIndicator(upArrow)
        shareMenuItem?.apply {
            drawableShare?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            icon = drawableShare
        }
    }

    /**
     * Show dialog for getting decryption key
     */
    private fun askForDecryptionKeyDialog() {
        // execute any action after onPause fragment will throw
        // IllegalStateException: Can not perform this action after onSaveInstanceState
        // we do not need to store the dialog show or not, API will trigger again in case device rotation
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Timber.d("askForDecryptionKeyDialog")
            decryptAlertDialog = DecryptAlertDialog.Builder()
                .setTitle(getString(R.string.alert_decryption_key))
                .setPosText(R.string.general_decryp).setNegText(R.string.general_cancel)
                .setMessage(getString(R.string.message_decryption_key))
                .setErrorMessage(R.string.invalid_decryption_key).setKey(mKey)
                .build()
            decryptAlertDialog?.show(supportFragmentManager, TAG_DECRYPT)
        }
    }

    /**
     * Get combined url with key for fetching link content
     */
    private fun decrypt() {
        mKey?.let { key ->
            if (key.isEmpty()) return
            var urlWithKey = ""
            if (url?.contains("#!") == true) {
                // old folder link format
                urlWithKey = if (key.startsWith("!")) {
                    Timber.d("Decryption key with exclamation!")
                    url + key
                } else {
                    "$url!$key"
                }
            } else if (url?.contains(Constants.SEPARATOR + "file" + Constants.SEPARATOR) == true) {
                // new folder link format
                urlWithKey = if (key.startsWith("#")) {
                    Timber.d("Decryption key with hash!")
                    url + key
                } else {
                    "$url#$key"
                }
            }
            Timber.d("File link to import: $urlWithKey")
            decryptionIntroduced = true
            importLink(urlWithKey)
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        if (intent != null) {
            if (intent.action != null) {
                if (intent.action == Constants.ACTION_IMPORT_LINK_FETCH_NODES) {
                    importNode()
                }
                intent.action = null
            }
        }
    }

    /**
     * Get the public link content
     */
    private fun importLink(url: String) {
        if (!viewModel.isConnected) {
            showSnackbar(
                Constants.SNACKBAR_TYPE,
                getString(R.string.error_server_connection_problem)
            )
            return
        }
        if (this.isFinishing) return
        val temp: AlertDialog
        try {
            temp = createProgressDialog(this, getString(R.string.general_loading))
            temp.show()
        } catch (ex: Exception) {
            return
        }
        statusDialog = temp
        megaApi.getPublicNode(url, this)
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: ${request.requestString}")
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate: ${request.requestString}")
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: ${request.requestString} code: ${e.errorCode}")
        when (request.type) {
            MegaRequest.TYPE_GET_PUBLIC_NODE -> {
                try {
                    statusDialog?.dismiss()
                } catch (ex: Exception) {
                    Timber.e(ex)
                }

                when (e.errorCode) {
                    MegaError.API_OK -> {
                        request.publicMegaNode?.let { publicNode ->
                            document = publicNode
                            val handle = publicNode.handle
                            Timber.d("DOCUMENTNODEHANDLEPUBLIC: $handle")

                            if (handle != MegaApiJava.INVALID_HANDLE) {
                                dbH.setLastPublicHandle(handle)
                                dbH.setLastPublicHandleTimeStamp()
                            }

                            binding.apply {
                                fileLinkInfoCollapseToolbar.title = publicNode.name
                                fileLinkIcon.setImageResource(typeForName(publicNode.name).iconResourceId)
                                fileLinkIconLayout.visibility = View.VISIBLE
                                fileLinkButtonDownload.visibility = View.VISIBLE
                                fileLinkButtonImport.isVisible = dbH.credentials != null
                                contentFileLink.fileLinkSize.text =
                                    Util.getSizeString(publicNode.size, this@FileLinkActivity)

                                preview = PreviewUtils.getPreviewFromCache(publicNode)
                                if (preview != null) {
                                    PreviewUtils.previewCache.put(handle, preview)
                                    fileInfoToolbarImage.setImageBitmap(preview)
                                    fileInfoImageLayout.visibility = View.VISIBLE
                                    fileLinkIconLayout.visibility = View.GONE
                                    contentFileLink.buttonPreviewContent.visibility = View.VISIBLE
                                    contentFileLink.buttonPreviewContent.isEnabled = true
                                } else {
                                    preview =
                                        PreviewUtils.getPreviewFromFolder(
                                            document,
                                            this@FileLinkActivity
                                        )
                                    if (preview != null) {
                                        PreviewUtils.previewCache.put(handle, preview)
                                        fileInfoToolbarImage.setImageBitmap(preview)
                                        fileInfoImageLayout.visibility = View.VISIBLE
                                        fileLinkIconLayout.visibility = View.GONE
                                        contentFileLink.buttonPreviewContent.isVisible = true
                                        contentFileLink.buttonPreviewContent.isEnabled = true
                                    } else {
                                        if (publicNode.hasPreview()) {
                                            val previewFile = File(
                                                PreviewUtils.getPreviewFolder(this@FileLinkActivity),
                                                publicNode.base64Handle + ".jpg"
                                            )
                                            megaApi.getPreview(
                                                publicNode,
                                                previewFile.absolutePath,
                                                this@FileLinkActivity
                                            )
                                            contentFileLink.buttonPreviewContent.isVisible = true
                                        } else {
                                            val publicNodeType = typeForName(publicNode.name)
                                            if (publicNodeType.isVideoMimeType
                                                || publicNodeType.isAudio
                                                || publicNodeType.isPdf
                                                || publicNodeType.isOpenableTextFile(publicNode.size)
                                            ) {
                                                fileInfoImageLayout.visibility = View.GONE
                                                fileLinkIconLayout.visibility = View.VISIBLE
                                                contentFileLink.apply {
                                                    buttonPreviewContent.isVisible = true
                                                    buttonPreviewContent.isEnabled = true
                                                }
                                            } else {
                                                contentFileLink.apply {
                                                    buttonPreviewContent.isVisible = false
                                                    buttonPreviewContent.isEnabled = false
                                                }
                                                fileInfoImageLayout.visibility = View.GONE
                                                fileLinkIconLayout.visibility = View.VISIBLE
                                            }
                                        }
                                    }
                                }
                            }
                            trySetupCollapsingToolbar()
                            if (importClicked) {
                                checkCollisionBeforeCopying()
                            }

                        } ?: {
                            Timber.w("document     --> Intent to ManagerActivity")
                            val closedChat = isClosedChat
                            if (closedChat) {
                                val backIntent = Intent(this, ManagerActivity::class.java)
                                startActivity(backIntent)
                            }
                            finish()
                        }
                    }

                    else -> {
                        Timber.w("ERROR: ${e.errorCode}")
                        val dialogTitle: String
                        val dialogMessage: String
                        when (e.errorCode) {
                            MegaError.API_EBLOCKED -> {
                                dialogMessage =
                                    getString(R.string.file_link_unavaible_ToS_violation)
                                dialogTitle = getString(R.string.general_error_file_not_found)
                            }

                            MegaError.API_EARGS -> {
                                if (decryptionIntroduced) {
                                    Timber.w("Incorrect key, ask again!")
                                    decryptionIntroduced = false
                                    askForDecryptionKeyDialog()
                                    return
                                } else {
                                    // Invalid Link
                                    dialogTitle = getString(R.string.general_error_word)
                                    dialogMessage = getString(R.string.link_broken)
                                }
                            }

                            MegaError.API_ETOOMANY -> {
                                dialogMessage =
                                    getString(R.string.file_link_unavaible_delete_account)
                                dialogTitle = getString(R.string.general_error_file_not_found)
                            }

                            MegaError.API_EINCOMPLETE -> {
                                decryptionIntroduced = false
                                askForDecryptionKeyDialog()
                                return
                            }

                            else -> {
                                dialogTitle = getString(R.string.general_error_word)
                                dialogMessage = getString(R.string.general_error_file_not_found)
                            }
                        }

                        try {
                            val dialog = MaterialAlertDialogBuilder(this).apply {
                                title = dialogTitle
                                setMessage(dialogMessage)
                                setCancelable(false)
                                setPositiveButton(
                                    getString(android.R.string.ok)
                                ) { dialog: DialogInterface, _: Int ->
                                    dialog.dismiss()
                                    val closedChat = isClosedChat
                                    if (closedChat) {
                                        val backIntent = Intent(
                                            this@FileLinkActivity,
                                            ManagerActivity::class.java
                                        )
                                        startActivity(backIntent)
                                    }
                                    finish()
                                }
                            }.create()
                            dialog.show()
                        } catch (ex: Exception) {
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.general_error_file_not_found)
                            )
                        }
                    }
                }
            }

            MegaRequest.TYPE_GET_ATTR_FILE -> {
                if (e.errorCode == MegaError.API_OK) {
                    val previewDir = PreviewUtils.getPreviewFolder(this)
                    document?.let {
                        val preview = File(previewDir, it.base64Handle + ".jpg")
                        if (preview.exists()) {
                            if (preview.length() > 0) {
                                val bitmap = PreviewUtils.getBitmapForCache(preview, this)
                                PreviewUtils.previewCache.put(it.handle, bitmap)
                                this.preview = bitmap
                                trySetupCollapsingToolbar()
                                binding.apply {
                                    fileInfoToolbarImage.setImageBitmap(bitmap)
                                    contentFileLink.buttonPreviewContent.isEnabled = true
                                    fileInfoImageLayout.visibility = View.VISIBLE
                                    fileLinkIconLayout.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError: ${request.requestString}")
    }

    @SuppressLint("NonConstantResourceId")
    override fun onClick(v: View) {
        when (v.id) {
            R.id.file_link_button_download -> {
                checkNotificationsPermission(this)
                nodeSaver.saveNode(
                    document ?: return,
                    highPriority = false,
                    isFolderLink = false,
                    fromMediaViewer = false,
                    needSerialize = true
                )
            }

            R.id.file_link_button_import -> {
                importNode()
            }

            R.id.button_preview_content -> {
                showFile()
            }
        }
    }

    /**
     * Open folder selection for importing the node
     */
    private fun importNode() {
        val intent = Intent(this, FileExplorerActivity::class.java)
        intent.action = FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER
        selectImportFolderLauncher.launch(intent)
    }

    /**
     * Checks if there is any name collision before copying the node.
     */
    private fun checkCollisionBeforeCopying() {
        composite.add(
            checkNameCollisionUseCase.check(document, target, NameCollisionType.COPY)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { collision: NameCollision ->
                        val list = ArrayList<NameCollision>()
                        list.add(collision)
                        nameCollisionActivityContract?.launch(list)
                    }
                ) { throwable: Throwable? ->
                    if (throwable is ParentDoesNotExistException) {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.general_error),
                            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                        )
                    } else if (throwable is ChildDoesNotExistsException) {
                        copyNode()
                    }
                })
    }

    /**
     * Copies a node.
     */
    private fun copyNode() {
        composite.add(
            legacyCopyNodeUseCase.copy(document, target, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    startActivity(
                        Intent(this, ManagerActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
                ) { copyThrowable: Throwable? ->
                    if (!manageCopyMoveException(copyThrowable)) {
                        showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.context_no_copied))
                        startActivity(
                            Intent(this, ManagerActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                })
    }

    /**
     * Open selected file
     */
    private fun showFile() {
        Timber.d("showFile")
        document?.let { node ->
            val serializeString = node.serialize()
            val name = node.name
            val nameType = typeForName(name)

            when {
                nameType.isImage -> {
                    val intent = getIntentForSingleNode(this, url ?: return)
                    startActivity(intent)
                }

                nameType.isVideoMimeType || nameType.isAudio -> {
                    Timber.d("Is video")
                    val mimeType = nameType.type
                    Timber.d("NODE HANDLE: ${node.handle}, TYPE: $mimeType")
                    val mediaIntent: Intent
                    val internalIntent: Boolean
                    var opusFile = false
                    if (nameType.isVideoNotSupported || nameType.isAudioNotSupported) {
                        mediaIntent = Intent(Intent.ACTION_VIEW)
                        internalIntent = false
                        val s =
                            name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if (s.size > 1 && s[s.size - 1] == "opus") {
                            opusFile = true
                        }
                    } else {
                        Timber.d("setIntentToAudioVideoPlayer")
                        mediaIntent = Util.getMediaIntent(this, name)
                        mediaIntent.apply {
                            putExtra("adapterType", Constants.FILE_LINK_ADAPTER)
                            putExtra(Constants.INTENT_EXTRA_KEY_IS_PLAYLIST, false)
                            putExtra(Constants.EXTRA_SERIALIZE_STRING, serializeString)
                            putExtra(Constants.URL_FILE_LINK, url)
                        }
                        internalIntent = true
                    }
                    mediaIntent.putExtra("FILENAME", name)
                    if (megaApi.httpServerIsRunning() == 0) {
                        megaApi.httpServerStart()
                        mediaIntent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
                    } else {
                        Timber.w("ERROR: HTTP server already running")
                    }
                    val mi = ActivityManager.MemoryInfo()
                    val activityManager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    activityManager.getMemoryInfo(mi)
                    if (mi.totalMem > Constants.BUFFER_COMP) {
                        Timber.d("Total mem: ${mi.totalMem} allocate 32 MB")
                        megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                    } else {
                        Timber.d("Total mem: ${mi.totalMem} allocate 16 MB")
                        megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                    }
                    val url = megaApi.httpServerGetLocalLink(node)
                    if (url != null) {
                        val parsedUri = Uri.parse(url)
                        if (parsedUri != null) {
                            mediaIntent.setDataAndType(parsedUri, mimeType)
                        } else {
                            Timber.w("ERROR: HTTP server get local link")
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error)
                            )
                            return
                        }
                    } else {
                        Timber.w("ERROR: HTTP server get local link")
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.general_text_error)
                        )
                        return
                    }
                    mediaIntent.putExtra("HANDLE", node.handle)
                    if (opusFile) {
                        mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                    }
                    if (internalIntent) {
                        startActivity(mediaIntent)
                    } else {
                        Timber.d("External Intent")
                        if (MegaApiUtils.isIntentAvailable(this, mediaIntent)) {
                            startActivity(mediaIntent)
                        } else {
                            Timber.d("No Available Intent")
                            showSnackbar(Constants.SNACKBAR_TYPE, "NoApp available")
                        }
                    }
                }

                nameType.isPdf -> {
                    Timber.d("Is pdf")
                    val mimeType = nameType.type
                    Timber.d("NODE HANDLE: ${node.handle}, TYPE: $mimeType")
                    val pdfIntent = Intent(this, PdfViewerActivity::class.java)
                    pdfIntent.apply {
                        putExtra("adapterType", Constants.FILE_LINK_ADAPTER)
                        putExtra(Constants.EXTRA_SERIALIZE_STRING, serializeString)
                        putExtra("inside", true)
                        putExtra("FILENAME", name)
                        putExtra(Constants.URL_FILE_LINK, url)
                        putExtra("HANDLE", node.handle)
                    }
                    if (viewModel.isConnected) {
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart()
                            pdfIntent.putExtra(
                                Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                                true
                            )
                        } else {
                            Timber.w("ERROR: HTTP server already running")
                        }
                        val mi = ActivityManager.MemoryInfo()
                        val activityManager =
                            this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                        activityManager.getMemoryInfo(mi)
                        if (mi.totalMem > Constants.BUFFER_COMP) {
                            Timber.d("Total mem: ${mi.totalMem} allocate 32 MB")
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB)
                        } else {
                            Timber.d("Total mem: ${mi.totalMem} allocate 16 MB")
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB)
                        }
                        val url = megaApi.httpServerGetLocalLink(node)
                        if (url != null) {
                            val parsedUri = Uri.parse(url)
                            if (parsedUri != null) {
                                pdfIntent.setDataAndType(parsedUri, mimeType)
                            } else {
                                Timber.d("ERROR: HTTP server get local link")
                                showSnackbar(
                                    Constants.SNACKBAR_TYPE,
                                    getString(R.string.general_text_error)
                                )
                            }
                        } else {
                            Timber.d("ERROR: HTTP server get local link")
                            showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.general_text_error)
                            )
                        }
                    } else {
                        showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.error_server_connection_problem) + ". " + getString(
                                R.string.no_network_connection_on_play_file
                            )
                        )
                    }
                    if (MegaApiUtils.isIntentAvailable(this, pdfIntent)) {
                        startActivity(pdfIntent)
                    } else {
                        Timber.w("No Available Intent")
                    }
                }

                nameType.isOpenableTextFile(node.size) -> {
                    manageTextFileIntent(this, node, Constants.FILE_LINK_ADAPTER, url)
                }

                else -> Timber.w("none")
            }
        } ?: Timber.w("Public Node null")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (intent == null) {
            return
        }
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
        }
    }

    /**
     * Show snackbar
     */
    fun showSnackbar(type: Int, s: String?) {
        showSnackbar(type, binding.fileLinkFragmentContainer, s)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    override fun onDialogPositiveClick(key: String?) {
        mKey = key
        decrypt()
    }

    override fun onDialogNegativeClick() {
        finish()
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.fileLinkFragmentContainer, content, chatId)
    }

    companion object {
        private const val TAG_DECRYPT = "decrypt"
    }
}