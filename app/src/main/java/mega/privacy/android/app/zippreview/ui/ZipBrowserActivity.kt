package mega.privacy.android.app.zippreview.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.components.ChatDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.databinding.ActivityZipBrowserBinding
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Constants.VIEWER_FROM_ZIP_BROWSER
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.getScreenHeight
import mega.privacy.android.app.zippreview.domain.FileType
import mega.privacy.android.app.zippreview.viewmodel.ZipBrowserViewModel
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.zip.ZipException
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Display the zip file content
 */
@AndroidEntryPoint
class ZipBrowserActivity : PasscodeActivity() {
    @Inject
    lateinit var crashReporter: CrashReporter

    private lateinit var zipBrowserBinding: ActivityZipBrowserBinding
    private var actionBar: ActionBar? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var zipAdapter: ZipListAdapter

    private lateinit var zipFullPath: String
    private lateinit var unzipRootPath: String

    private lateinit var unZipWaitingDialog: AlertDialog

    private val zipBrowserViewModel by viewModels<ZipBrowserViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
        initView()
        setupViewModel()
        zipAdapter = ZipListAdapter { zipInfoUIO, position ->
            zipBrowserViewModel.onZipFileClicked(zipInfoUIO, position)
        }
        recyclerView.adapter = zipAdapter
    }

    private fun initData() {
        intent.extras?.run {
            //Get the zip file path
            zipFullPath = getString(EXTRA_PATH_ZIP) ?: ""
            //Get the unzip root path for unpack zip file
            unzipRootPath = zipFullPath.split(".").first()
        }
    }

    private fun initView() {
        zipBrowserBinding = ActivityZipBrowserBinding.inflate(layoutInflater)
        setContentView(zipBrowserBinding.root)

        val toolbar = zipBrowserBinding.toolbar
        toolbar.also {
            it.visibility = View.VISIBLE
            setSupportActionBar(it)
        }
        actionBar = supportActionBar
        actionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = (StringResourcesUtils.getString(R.string.zip_browser_activity))
        }

        recyclerView = zipBrowserBinding.zipListViewBrowser.also {
            it.setPadding(0, 0, 0, recycleViewBottomPadding())
            it.clipToPadding = false
            it.addItemDecoration(ChatDividerItemDecoration(this, outMetrics))
            it.layoutManager = LinearLayoutManager(this)
            it.setHasFixedSize(true)
            it.itemAnimator = DefaultItemAnimator()
        }
    }

    private fun setupViewModel() {
        zipBrowserViewModel.apply {
            title.observe(this@ZipBrowserActivity) { title ->
                actionBar?.title = title
            }
            zipInfoList.observe(this@ZipBrowserActivity) { zipInfoList ->
                zipAdapter.submitList(zipInfoList)
            }
            showProgressDialog.observe(this@ZipBrowserActivity) { showProgressDialog ->
                if (showProgressDialog) {
                    showProgressDialog()
                } else if (unZipWaitingDialog.isShowing) {
                    unZipWaitingDialog.dismiss()
                }
            }
            showAlert.observe(this@ZipBrowserActivity) { isShowAlert ->
                if (isShowAlert) {
                    showAlert()
                }
            }
            openFile.observe(this@ZipBrowserActivity) { openFile ->
                openFile(openFile.second, openFile.first)
            }
            // Log the zip file path
            crashReporter.log("Path of ZipFile(setupViewModel) is $zipFullPath")
            //Open current zip file content
            viewModelInit(
                zipFullPath,
                unzipRootPath
            )
        }
    }

    override fun onBackPressed() {
        if (zipBrowserViewModel.backOnPress()) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("OnOptionsItemSelected")
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return true
    }

    /**
     * show snack bar
     * @param message the message is displayed on snack bar
     */
    private fun showSnackBar(message: String) {
        showSnackbar(zipBrowserBinding.zipLayout, message)
    }

    /**
     * legacy logic to get the recycle view bottom padding
     * @return bottom padding
     */
    private fun recycleViewBottomPadding(): Int {
        return (RATIO_RECYCLE_VIEW * getScreenHeight()).toInt() //Based on Eduardo's measurements
    }

    /**
     * Show progress dialog
     */
    private fun showProgressDialog() {
        if (!::unZipWaitingDialog.isInitialized) {
            unZipWaitingDialog = createProgressDialog(
                this@ZipBrowserActivity,
                getString(R.string.unzipping_process)
            )
            unZipWaitingDialog.show()
        }
    }

    /**
     * Show alert dialog when the file cannot be opened
     */
    private fun showAlert() {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setMessage(StringResourcesUtils.getString(R.string.error_fail_to_open_file_general))
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_ok), null)
            .show()
    }

    /**
     * Open file
     * @param zipInfoUIO ZipInfoUIO of file opened
     * @param position position of file opened
     */
    private fun openFile(zipInfoUIO: ZipInfoUIO, position: Int) {
        if (zipInfoUIO.fileType == FileType.ZIP) {
            zipFileOpen(zipInfoUIO)
        } else {
            val file = File("$unzipRootPath${File.separator}${zipInfoUIO.path}")
            MimeTypeList.typeForName(file.name).apply {
                when {
                    isImage ->
                        imageFileOpen(position, file)
                    isVideoReproducible || isAudio ->
                        mediaFileOpen(file, position)
                    isPdf ->
                        pdfFileOpen(file, position)
                    isOpenableTextFile(file.length()) -> {
                        startActivity(
                            Intent(
                                this@ZipBrowserActivity,
                                TextEditorActivity::class.java
                            ).putExtra(INTENT_EXTRA_KEY_FILE_NAME, file.name)
                                .putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
                                .putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
                        )
                    }
                    else -> otherFileOpen(file)
                }
            }
        }
    }

    /**
     * Open zip type file
     * @param zipInfoUIO ZipInfoUIO of file opened
     */
    private fun zipFileOpen(zipInfoUIO: ZipInfoUIO) {
        val intentZip = Intent(
            this@ZipBrowserActivity,
            ZipBrowserActivity::class.java
        )
        intentZip.putExtra(EXTRA_PATH_ZIP, "${unzipRootPath}${File.separator}${zipInfoUIO.path}")
        startActivity(intentZip)
    }

    /**
     * legacy logic to open image type file
     * @param position position of file opened
     * @param file file
     */
    private fun imageFileOpen(position: Int, file: File) {
        Timber.d("isImage")

        val intent = ImageViewerActivity.getIntentForFile(this, file.toUri(), true)
        DragToExitSupport.putThumbnailLocation(
            intent,
            recyclerView,
            position,
            VIEWER_FROM_ZIP_BROWSER,
            zipAdapter
        )
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /**
     * legacy logic to open media type file
     * @param position position of file opened
     * @param file file
     */
    private fun MimeTypeList.mediaFileOpen(file: File, position: Int) {
        Timber.d("Video file")
        val mediaIntent: Intent
        val internalIntent: Boolean
        var opusFile = false
        if (isVideoNotSupported || isAudioNotSupported) {
            mediaIntent = Intent(Intent.ACTION_VIEW)
            internalIntent = false
            val array = file.name.split("\\.")
            if (array.size > 1 && array.last() == "opus") {
                opusFile = true
            }
        } else {
            internalIntent = true
            mediaIntent = Util.getMediaIntent(this@ZipBrowserActivity, file.name)
        }
        mediaIntent.apply {
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, file.name)
            putExtra(INTENT_EXTRA_KEY_HANDLE, file.name.hashCode().toLong())
            putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
            putExtra(INTENT_EXTRA_KEY_POSITION, position)
            putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, -1L)
            putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, MegaApiJava.ORDER_DEFAULT_ASC)
            DragToExitSupport.putThumbnailLocation(
                mediaIntent,
                recyclerView,
                position,
                VIEWER_FROM_ZIP_BROWSER,
                zipAdapter
            )
            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        getExternalFilesDir(null)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && file.absolutePath.contains(path)) {
                mediaIntent.setDataAndType(
                    FileProvider.getUriForFile(
                        this@ZipBrowserActivity,
                        URI_FILE_PROVIDER,
                        file
                    ), type
                )
            } else {
                mediaIntent.setDataAndType(Uri.fromFile(file), type)
            }
        }
        if (opusFile) {
            mediaIntent.setDataAndType(mediaIntent.data, TYPE_AUDIO)
        }
        if (internalIntent) {
            startActivity(mediaIntent)
        } else {
            if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, mediaIntent)) {
                startActivity(mediaIntent)
            } else {
                showSnackBar(resources.getString(R.string.intent_not_available))

                val intentShare = Intent(Intent.ACTION_SEND)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intentShare.setDataAndType(
                        FileProvider.getUriForFile(
                            this@ZipBrowserActivity,
                            URI_FILE_PROVIDER,
                            file
                        ), type
                    )
                } else {
                    intentShare.setDataAndType(Uri.fromFile(file), type)
                }
                intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, intentShare)) {
                    Timber.d("Call to startActivity(intentShare)")
                    startActivity(intentShare)
                }
            }
        }
        overridePendingTransition(0, 0)
    }

    /**
     * legacy logic to open pdf type file
     * @param position position of file opened
     * @param file file
     */
    private fun MimeTypeList.pdfFileOpen(file: File, position: Int) {
        Timber.d("Pdf file")
        val pdfIntent =
            Intent(this@ZipBrowserActivity, PdfViewerActivity::class.java)
        pdfIntent.apply {
            putExtra(INTENT_EXTRA_KEY_INSIDE, true)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
            putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.absolutePath)
        }
        DragToExitSupport.putThumbnailLocation(
            pdfIntent,
            recyclerView,
            position,
            VIEWER_FROM_ZIP_BROWSER,
            zipAdapter
        )
        getExternalFilesDir(null)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && file.absolutePath.contains(path)) {
                pdfIntent.setDataAndType(
                    FileProvider.getUriForFile(
                        this@ZipBrowserActivity,
                        URI_FILE_PROVIDER,
                        file
                    ), type
                )
            } else {
                pdfIntent.setDataAndType(Uri.fromFile(file), type)
            }
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(pdfIntent)
            overridePendingTransition(0, 0)
        }
    }

    /**
     * legacy logic to open other type file
     * @param file file
     */
    private fun MimeTypeList.otherFileOpen(file: File) {
        Timber.d("NOT Image, video, audio or pdf")
        val viewIntent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            viewIntent.setDataAndType(
                FileProvider.getUriForFile(
                    this@ZipBrowserActivity,
                    URI_FILE_PROVIDER,
                    file
                ), type
            )
        } else {
            viewIntent.setDataAndType(Uri.fromFile(file), type)
        }
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, viewIntent)) {
            startActivity(viewIntent)
        } else {
            val intentShare = Intent(Intent.ACTION_SEND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intentShare.setDataAndType(
                    FileProvider.getUriForFile(
                        this@ZipBrowserActivity,
                        URI_FILE_PROVIDER,
                        file
                    ), type
                )
            } else {
                intentShare.setDataAndType(Uri.fromFile(file), type)
            }
            intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (MegaApiUtils.isIntentAvailable(this@ZipBrowserActivity, intentShare)) {
                Timber.d("Call to startActivity(intentShare)")
                startActivity(intentShare)
            }
            val toastMessage =
                "${getString(R.string.general_already_downloaded)}:${file.absolutePath}"
            Toast.makeText(this@ZipBrowserActivity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /**
         * Use for companion object injection
         */
        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CrashReporterEntryPoint {
            fun crashReporter(): CrashReporter
        }

        const val EXTRA_PATH_ZIP = "PATH_ZIP"
        const val EXTRA_HANDLE_ZIP = "HANDLE_ZIP"
        const val MEGA_DOWNLOADS = "MEGA Downloads/"

        const val URI_FILE_PROVIDER = "mega.privacy.android.app.providers.fileprovider"
        const val TYPE_AUDIO = "audio/*"

        const val RATIO_RECYCLE_VIEW = 85.0f / 548

        /**
         * Start ZipBrowserActivity and check the zip file if is error format
         * @param context Context
         * @param zipFilePath zip file full path
         */
        fun start(context: Context, zipFilePath: String) {
            if (zipFileFormatCheck(context, zipFilePath)) {
                context.startActivity(
                    Intent(context, ZipBrowserActivity::class.java).apply {
                        putExtra(EXTRA_PATH_ZIP, zipFilePath)
                    })
            } else {
                Util.showSnackbar(context, context.getString(R.string.message_zip_format_error))
            }
        }

        /**
         * check the zip file if is error format
         * @param context context
         * @param zipFilePath zip file full path
         */
        fun zipFileFormatCheck(context: Context, zipFilePath: String): Boolean {
            val hiltEntryPoint =
                EntryPointAccessors.fromApplication(context, CrashReporterEntryPoint::class.java)

            (context as Activity?)?.run {
                // Log the Activity name that opens ZipBrowserActivity
                hiltEntryPoint.crashReporter().log("Activity name is $localClassName")
            }
            // Log the zip file path
            hiltEntryPoint.crashReporter()
                .log("Path of ZipFile(zipFileFormatCheck) is $zipFilePath")
            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(zipFilePath)
            } catch (exception: ZipException) {

                zipFile?.close()
                return false
            }
            zipFile.close()
            return true
        }
    }
}
