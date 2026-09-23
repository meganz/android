package mega.privacy.android.app.presentation.pdfviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.extensions.isHttpScheme
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_APP
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_FOLDER_LINK
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.URL_FILE_LINK
import mega.privacy.android.app.utils.Constants.ZIP_ADAPTER
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity open the Compose PDF viewer. It renders the
 * [PdfViewerNavKey] destination via [LegacyActivityScaffold]; the entry itself is registered by
 * the injected [featureDestinations].
 *
 * Callers inside `MegaActivity` should navigate to [PdfViewerNavKey] directly. Legacy callers keep
 * the existing intent contract (data URI plus adapter type, handle, chat ids and link extras),
 * which is mapped onto a [PdfViewerNavKey] here.
 */
@AndroidEntryPoint
class PdfViewerActivity : AppCompatActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    @Inject
    lateinit var viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startKey = intent.toPdfViewerNavKey()
        if (startKey == null) {
            Timber.w("PdfViewerActivity launched without a PDF to open")
            finish()
            return
        }
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            LegacyActivityScaffold(
                container = { content ->
                    // File and folder links open without a session, so the shell must not require one.
                    MegaAppContainer(
                        themeMode = themeMode,
                        isSessionRequired = false,
                        finishOnSessionRefresh = false,
                        content = content,
                    )
                },
                initialKey = startKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                onEmptyBackStack = { if (!isFinishing) finish() },
            ) { _, _ -> }
        }
    }

    /**
     * Maps a legacy launch intent onto a [PdfViewerNavKey]. Returns null when the intent carries
     * no content to open.
     */
    private fun Intent.toPdfViewerNavKey(): PdfViewerNavKey? {
        val uri = data ?: return null
        val adapterType = getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INVALID_VALUE)
        val nodeSourceType = if (getBooleanExtra(INTENT_EXTRA_KEY_IS_FOLDER_LINK, false)) {
            NodeSourceType.FOLDER_LINK
        } else {
            legacyPdfNodeSourceType(adapterType, viewTypeToNodeSourceTypeMapper)
        }
        val isFromChat = nodeSourceType == NodeSourceType.CHAT
        val contentUri = if (nodeSourceType == NodeSourceType.OFFLINE) {
            // The Compose viewer resolves an offline file from its filesystem path.
            getStringExtra(INTENT_EXTRA_KEY_PATH) ?: uri.path ?: uri.toString()
        } else {
            uri.toString()
        }
        return PdfViewerNavKey(
            nodeHandle = getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE),
            contentUri = contentUri,
            isLocalContent = !uri.isHttpScheme(),
            shouldStopHttpServer = getBooleanExtra(
                ExtraConstant.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER,
                false,
            ),
            nodeSourceType = nodeSourceType,
            mimeType = type ?: DEFAULT_MIME_TYPE,
            chatId = getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE)
                .takeIf { isFromChat && it != INVALID_HANDLE },
            messageId = getLongExtra(INTENT_EXTRA_KEY_MSG_ID, INVALID_HANDLE)
                .takeIf { isFromChat && it != INVALID_HANDLE },
            title = getStringExtra(INTENT_EXTRA_KEY_FILE_NAME),
            isExternalFile = !getBooleanExtra(INTENT_EXTRA_KEY_INSIDE, false),
            isFromZip = adapterType == ZIP_ADAPTER,
            publicLinkUrl = getStringExtra(URL_FILE_LINK),
        )
    }

    companion object {
        private const val DEFAULT_MIME_TYPE = "application/pdf"

        /**
         * Builds an [Intent] that opens the PDF viewer for a cloud node. The caller sets the data
         * URI and MIME type of the content to display.
         *
         * @param nodeHandle Node handle
         * @param nodeSourceType Legacy adapter type of the screen the node was opened from
         */
        fun createIntent(
            context: Context,
            nodeHandle: Long,
            nodeSourceType: Int?,
        ): Intent = Intent(context, PdfViewerActivity::class.java).apply {
            putExtra(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(INTENT_EXTRA_KEY_INSIDE, true)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, nodeSourceType)
            putExtra(INTENT_EXTRA_KEY_APP, true)
        }
    }
}
