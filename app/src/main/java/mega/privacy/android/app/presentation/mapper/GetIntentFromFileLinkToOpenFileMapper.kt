package mega.privacy.android.app.presentation.mapper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.usecase.filelink.GetFileUrlByPublicLinkUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerSetMaxBufferSizeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Mapper to get intent to open file
 *
 * @property httpServerStart [MegaApiHttpServerStartUseCase] to start MegaApi Server
 * @property httpServerIsRunning [MegaApiHttpServerIsRunningUseCase] to check Mega Api Http server is running
 * @property httpServerSetMaxBufferSize [MegaApiHttpServerSetMaxBufferSizeUseCase] to get Api buffer size
 * @property getFileUrlByPublicLinkUseCase [GetFileUrlByPublicLinkUseCase] to get the local file url from public link
 */
class GetIntentFromFileLinkToOpenFileMapper @Inject constructor(
    private val httpServerStart: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunning: MegaApiHttpServerIsRunningUseCase,
    private val httpServerSetMaxBufferSize: MegaApiHttpServerSetMaxBufferSizeUseCase,
    private val getFileUrlByPublicLinkUseCase: GetFileUrlByPublicLinkUseCase,
) {

    /**
     * Invoke
     * @param activity Instance of activity
     *
     * @return Nullable Intent
     * @throws [UrlDownloadException] if failed to get Url from server
     */
    @Throws(UrlDownloadException::class)
    suspend operator fun invoke(
        activity: Activity,
        fileHandle: Long,
        fileName: String,
        fileSize: Long,
        serializedData: String?,
        url: String,
    ): Intent? {
        val nameType = MimeTypeList.typeForName(fileName)
        return when {
            nameType.isImage -> {
                ImageViewerActivity.getIntentForSingleNode(activity, url)
            }

            nameType.isVideoMimeType || nameType.isAudio -> {
                val mimeType = nameType.type
                var opusFile = false
                val mediaIntent =
                    if (nameType.isVideoNotSupported || nameType.isAudioNotSupported) {
                        val s = fileName.split("\\.".toRegex())
                        if (s.size > 1 && s[s.size - 1] == "opus") {
                            opusFile = true
                        }
                        Intent(Intent.ACTION_VIEW)
                    } else {
                        Util.getMediaIntent(activity, fileName)
                    }

                mediaIntent.apply {
                    putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FILE_LINK_ADAPTER)
                    putExtra(Constants.INTENT_EXTRA_KEY_IS_PLAYLIST, false)
                    putExtra(Constants.URL_FILE_LINK, url)
                    putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, fileHandle)
                    putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, fileName)
                    putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                }

                startHttpServer(mediaIntent)
                val path = getFileUrlByPublicLinkUseCase(url) ?: throw UrlDownloadException()
                mediaIntent.setDataAndType(Uri.parse(path), mimeType)

                if (opusFile) {
                    mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                }
                mediaIntent
            }

            nameType.isPdf -> {
                val mimeType = nameType.type
                val pdfIntent = Intent(activity, PdfViewerActivity::class.java)

                pdfIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK

                    putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FILE_LINK_ADAPTER)
                    putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, fileHandle)
                    putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, fileName)
                    putExtra(Constants.URL_FILE_LINK, url)
                    putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                    putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
                }
                startHttpServer(pdfIntent)
                val path = getFileUrlByPublicLinkUseCase(url) ?: throw UrlDownloadException()
                pdfIntent.setDataAndType(Uri.parse(path), mimeType)
                pdfIntent
            }

            nameType.isOpenableTextFile(fileSize) -> {
                Intent(activity, TextEditorActivity::class.java).apply {
                    putExtra(Constants.URL_FILE_LINK, url)
                    putExtra(Constants.EXTRA_SERIALIZE_STRING, serializedData)
                    putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
                    putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.FILE_LINK_ADAPTER)
                }
            }

            else -> {
                Timber.w("none")
                null
            }
        }
    }

    /**
     * Start the server if not started
     * also setMax buffer size based on available buffer size
     * @param intent [Intent]
     *
     * @return intent
     */
    private suspend fun startHttpServer(intent: Intent): Intent {
        if (httpServerIsRunning() == 0) {
            httpServerStart()
            intent.putExtra(Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }
        return intent
    }
}