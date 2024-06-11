package mega.privacy.android.app.presentation.zipbrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.ZipImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.zipbrowser.view.ZipBrowserScreen
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EXTRA_PATH_ZIP
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.data.model.MimeTypeList.Companion.typeForName
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * The activity for the zip browser
 */
@AndroidEntryPoint
class ZipBrowserComposeActivity : PasscodeActivity() {
    /**
     * [GetThemeMode] injection
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ZipBrowserViewModel>()

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(
                initialValue = ThemeMode.System
            )
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                ZipBrowserScreen(viewModel = viewModel)
            }
        }

        collectFlow(viewModel.uiState.map { it.openedFile }
            .distinctUntilChanged()) { zipInfoUiEntity ->
            zipInfoUiEntity?.let {
                viewModel.clearOpenedFile()
                val zipFiePath =
                    "${viewModel.getUnzipRootPath()}${File.separator}${zipInfoUiEntity.path}"
                if (zipInfoUiEntity.zipEntryType == ZipEntryType.Zip) {
                    zipFileOpen(zipFiePath)
                } else {
                    val zipFile = File(zipFiePath)

                    typeForName(zipFile.name).apply {
                        when {
                            isImage -> openImageFile(zipFile)

                            isVideoMimeType || isAudio -> openMediaFile(zipFile)

                            isPdf -> openPdfFile(zipFile)

                            isOpenableTextFile(zipFile.length()) -> {
                                startActivity(
                                    Intent(
                                        this@ZipBrowserComposeActivity,
                                        TextEditorActivity::class.java
                                    ).apply {
                                        putExtra(INTENT_EXTRA_KEY_FILE_NAME, zipFile.name)
                                        putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
                                        putExtra(INTENT_EXTRA_KEY_PATH, zipFile.absolutePath)
                                    }
                                )
                            }

                            else -> openOtherTypeFile(zipFile)
                        }
                    }

                }
            }
        }
    }

    private fun zipFileOpen(zipFilePath: String) {
        if (zipFileFormatCheck(this, zipFilePath)) {
            startActivity(Intent(this, ZipBrowserComposeActivity::class.java).apply {
                putExtra(EXTRA_PATH_ZIP, zipFilePath)
            })
        } else {
            viewModel.updateShowSnackBar(true)
        }
    }

    private fun openImageFile(file: File) {
        Timber.d("isImage")
        lifecycleScope.launch {
            val intent = ImagePreviewActivity.createIntent(
                context = this@ZipBrowserComposeActivity,
                imageSource = ImagePreviewFetcherSource.ZIP,
                menuOptionsSource = ImagePreviewMenuSource.ZIP,
                anchorImageNodeId = NodeId(file.hashCode().toLong()),
                params = mapOf(ZipImageNodeFetcher.URI to "${file.toUri()}")
            )
            startActivity(intent)
        }
    }

    private fun MimeTypeList.openMediaFile(file: File) {
        val intent = getIntent(file)
        runCatching {
            FileProvider.getUriForFile(
                this@ZipBrowserComposeActivity,
                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                file
            )
        }.onFailure {
            Uri.fromFile(file)
        }.map { mediaFileUri ->
            intent.setDataAndType(mediaFileUri, type)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }

    private fun getIntent(
        file: File,
    ) = getMediaIntent(this, file.name).apply {
        putExtra(INTENT_EXTRA_KEY_FILE_NAME, file.name)
        putExtra(INTENT_EXTRA_KEY_HANDLE, file.name.hashCode().toLong())
        putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
        putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
        putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, -1L)
        putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.absolutePath)
        putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, SortOrder.ORDER_DEFAULT_ASC)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /**
     * Return if type is PDF
     */
    val MimeTypeList.isPdf
        get() = type.startsWith("application/pdf")

    /**
     * legacy logic to open pdf type file
     * @param file file
     */
    private fun MimeTypeList.openPdfFile(file: File) {
        Timber.d("Pdf file")
        val pdfIntent =
            Intent(this@ZipBrowserComposeActivity, PdfViewerActivity::class.java)
        pdfIntent.apply {
            putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, ZIP_ADAPTER)
            putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
            putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.absolutePath)
        }
        getExternalFilesDir(null)?.apply {
            pdfIntent.setDataAndType(
                FileProvider.getUriForFile(
                    this@ZipBrowserComposeActivity,
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                    file
                ), type
            )
            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(pdfIntent)
        }
    }

    /**
     * legacy logic to open other type file
     * @param file file
     */
    private fun MimeTypeList.openOtherTypeFile(file: File) {
        Timber.d("NOT Image, video, audio or pdf")
        val viewIntent = Intent(Intent.ACTION_VIEW)
        viewIntent.setDataAndType(
            FileProvider.getUriForFile(
                this@ZipBrowserComposeActivity,
                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                file
            ), type
        )
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (MegaApiUtils.isIntentAvailable(this@ZipBrowserComposeActivity, viewIntent)) {
            startActivity(viewIntent)
        } else {
            val intentShare = Intent(Intent.ACTION_SEND)
            intentShare.setDataAndType(
                FileProvider.getUriForFile(
                    this@ZipBrowserComposeActivity,
                    Constants.AUTHORITY_STRING_FILE_PROVIDER,
                    file
                ), type
            )
            intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (MegaApiUtils.isIntentAvailable(this@ZipBrowserComposeActivity, intentShare)) {
                Timber.d("Call to startActivity(intentShare)")
                startActivity(intentShare)
            }
            val toastMessage =
                "${getString(R.string.general_already_downloaded)}:${file.absolutePath}"
            Toast.makeText(this@ZipBrowserComposeActivity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /**
         * Use for companion object injection
         */
        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CrashReporterEntryPoint {
            /**
             * Get [CrashReporter]
             *
             * @return [CrashReporter] instance
             */
            fun crashReporter(): CrashReporter
        }

        /**
         * check the zip file if is error format
         * @param context context
         * @param zipFilePath zip file full path
         */
        fun zipFileFormatCheck(context: Context, zipFilePath: String): Boolean {
            val hiltEntryPoint =
                EntryPointAccessors.fromApplication(context, CrashReporterEntryPoint::class.java)

            (context as? Activity)?.run {
                // Log the Activity name that opens ZipBrowserActivity
                hiltEntryPoint.crashReporter().log("Activity name is $localClassName")
            }
            // Log the zip file path
            hiltEntryPoint.crashReporter()
                .log("Path of ZipFile(zipFileFormatCheck) is $zipFilePath")
            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(zipFilePath)
                // Try reading the Zip File with UTF-8 Charset
                zipFile.entries().toList()
            } catch (exception: Exception) {
                Timber.e(exception, "ZipFile")
                // Throws IllegalArgumentException (thrown when malformed) / ZipException (thrown when unsupported format)
                // If zip cannot be read with UTF-8 Charset, then switch to CP-437 (Default for Most Windows Zip Software)
                // i.e: 7-Zip, PeaZip, Winrar, Winzip
                try {
                    zipFile = ZipFile(zipFilePath, Charset.forName("Cp437"))
                    zipFile.entries().toList()
                } catch (e: Exception) {
                    Timber.e(exception, "ZipFile")
                    // Close the ZipFile if fallback also fails
                    zipFile?.close()
                    return false
                }
            } finally {
                zipFile?.close()
            }
            return true
        }
    }
}