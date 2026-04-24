package mega.privacy.android.app.appstate

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.deeplinks.ExternalPdfDeepLinkHandler
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOption
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.GetCurrentConnectivityStateUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.DeepLinksDialogNavKey
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import mega.privacy.android.navigation.destination.ShareToMegaNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaActivityIntentActionHandlerTest {

    private lateinit var underTest: MegaActivityIntentActionHandler

    private val navigationEventQueue = mock<NavigationEventQueue>()
    private val navigationResultManager = mock<NavigationResultManager>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val getCurrentConnectivityStateUseCase = mock<GetCurrentConnectivityStateUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getFileNameFromStringUriUseCase = mock<GetFileNameFromStringUriUseCase>()

    private val externalPdfDeepLinkHandler = ExternalPdfDeepLinkHandler(
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        getFileNameFromStringUriUseCase = getFileNameFromStringUriUseCase,
    )

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }

    @BeforeAll
    fun setUp() {
        underTest = MegaActivityIntentActionHandler(
            navigationEventQueue = navigationEventQueue,
            navigationResultManager = navigationResultManager,
            snackbarEventQueue = snackbarEventQueue,
            getCurrentConnectivityStateUseCase = getCurrentConnectivityStateUseCase,
            externalPdfDeepLinkHandler = externalPdfDeepLinkHandler,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            navigationEventQueue,
            navigationResultManager,
            snackbarEventQueue,
            getCurrentConnectivityStateUseCase,
            getFeatureFlagValueUseCase,
            getFileNameFromStringUriUseCase,
        )
    }

    @Test
    fun `test that handleAction calls refreshSession with ManualRefresh when action is ACTION_REFRESH`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_REFRESH)
            var refreshedWith: RefreshEvent? = null

            underTest.handleAction(intent, { refreshedWith = it }, { null })

            assertThat(refreshedWith).isEqualTo(RefreshEvent.ManualRefresh)
        }

    @Test
    fun `test that handleAction calls refreshSession with ChangeEnvironment when action is ACTION_REFRESH_API_SERVER`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_REFRESH_API_SERVER)
            var refreshedWith: RefreshEvent? = null

            underTest.handleAction(intent, { refreshedWith = it }, { null })

            assertThat(refreshedWith).isEqualTo(RefreshEvent.ChangeEnvironment)
        }

    @Test
    fun `test that handleAction emits DeepLinksDialogNavKey when action is ACTION_DEEP_LINKS`() =
        runTest {
            val intent = mock<Intent>()
            val deepLink = "https://mega.nz/test"
            whenever(intent.action).thenReturn(MegaActivity.ACTION_DEEP_LINKS)
            whenever(intent.dataString).thenReturn(deepLink)

            underTest.handleAction(intent, {}, { null })

            verify(navigationEventQueue).emit(DeepLinksDialogNavKey(deepLink))
        }

    @Test
    fun `test that handleAction calls refreshSession with SdkReload when action is SdkReload`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(RefreshEvent.SdkReload.name)
            var refreshedWith: RefreshEvent? = null

            underTest.handleAction(intent, { refreshedWith = it }, { null })

            assertThat(refreshedWith).isEqualTo(RefreshEvent.SdkReload)
        }

    @Test
    fun `test that handleAction emits ShareToMegaNavKey when action is ACTION_SEND`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            val shareUris = listOf(UriPath("content://test"))

            underTest.handleAction(intent, {}, { shareUris })

            verify(navigationEventQueue).emit(ShareToMegaNavKey(shareUris))
        }

    @Test
    fun `test that handleAction navigates to upload when shortcut upload and connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_UPLOAD)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Connected(isOnWifi = true))

            underTest.handleAction(intent, {}, { null })

            verify(navigationResultManager).returnResult(
                HomeFabOptionsBottomSheetNavKey.KEY,
                HomeFabOption.UploadFiles
            )
        }

    @Test
    fun `test that handleAction shows snackbar when shortcut upload and not connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_UPLOAD)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Disconnected)

            underTest.handleAction(intent, {}, { null })

            verify(snackbarEventQueue).queueMessage(R.string.error_server_connection_problem)
            verify(navigationResultManager, never()).returnResult(any(), any<HomeFabOption>())
        }

    @Test
    fun `test that handleAction navigates to scan document when shortcut scan document and connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_SCAN_DOCUMENT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Connected(isOnWifi = true))

            underTest.handleAction(intent, {}, { null })

            verify(navigationResultManager).returnResult(
                HomeFabOptionsBottomSheetNavKey.KEY,
                HomeFabOption.ScanDocument
            )
        }

    @Test
    fun `test that handleAction shows snackbar when shortcut scan document and not connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_SCAN_DOCUMENT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Disconnected)

            underTest.handleAction(intent, {}, { null })

            verify(snackbarEventQueue).queueMessage(R.string.error_server_connection_problem)
            verify(navigationResultManager, never()).returnResult(any(), any<HomeFabOption>())
        }

    @Test
    fun `test that handleAction navigates to chat when shortcut chat and connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_CHAT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Connected(isOnWifi = true))

            underTest.handleAction(intent, {}, { null })

            verify(navigationEventQueue).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
        }

    @Test
    fun `test that handleAction shows snackbar when shortcut chat and not connected`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Constants.ACTION_SHORTCUT_CHAT)
            whenever(getCurrentConnectivityStateUseCase()).thenReturn(ConnectivityState.Disconnected)

            underTest.handleAction(intent, {}, { null })

            verify(snackbarEventQueue).queueMessage(R.string.error_server_connection_problem)
            verify(navigationEventQueue, never()).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
        }

    @Test
    fun `test that handleAction invokes navigateToComposePdfViewer when action is ACTION_VIEW with pdf MIME type and compose flag is enabled`() =
        runTest {
            val contentUriString = "content://com.example/file.pdf"
            val uri = mock<Uri>()
            whenever(uri.scheme).thenReturn("content")
            whenever(uri.toString()).thenReturn(contentUriString)
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(contentUriString)).thenReturn("file.pdf")
            var receivedNavKey: PdfViewerNavKey? = null

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = contentUriString,
                    isLocalContent = true,
                    isExternalFile = true,
                    title = "file.pdf",
                )
            )
        }

    @Test
    fun `test that handleAction invokes navigateToComposePdfViewer with isLocalContent false when action is ACTION_VIEW with https URI and compose flag is enabled`() =
        runTest {
            val httpsUriString = "https://www.w3.org/sample.pdf"
            val uri = mock<Uri>()
            whenever(uri.scheme).thenReturn("https")
            whenever(uri.toString()).thenReturn(httpsUriString)
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(httpsUriString)).thenReturn("sample.pdf")
            var receivedNavKey: PdfViewerNavKey? = null

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = httpsUriString,
                    isLocalContent = false,
                    isExternalFile = true,
                    title = "sample.pdf",
                )
            )
        }

    @Test
    fun `test that handleAction invokes navigateToComposePdfViewer with isLocalContent false when action is ACTION_VIEW with http URI and compose flag is enabled`() =
        runTest {
            val httpUriString = "http://example.com/sample.pdf"
            val uri = mock<Uri>()
            whenever(uri.scheme).thenReturn("http")
            whenever(uri.toString()).thenReturn(httpUriString)
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(httpUriString)).thenReturn("sample.pdf")
            var receivedNavKey: PdfViewerNavKey? = null

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = httpUriString,
                    isLocalContent = false,
                    isExternalFile = true,
                    title = "sample.pdf",
                )
            )
        }

    @Test
    fun `test that handleAction calls launchLegacyPdfViewer when action is ACTION_VIEW with pdf MIME type and compose flag is disabled`() =
        runTest {
            val uri = mock<Uri>()
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(false)
            var legacyLaunched = false

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                launchLegacyPdfViewer = { legacyLaunched = true },
            )

            assertThat(legacyLaunched).isTrue()
        }

    @Test
    fun `test that handleAction invokes navigateToComposePdfViewer when action is ACTION_VIEW with pdf path extension and compose flag is enabled`() =
        runTest {
            val fileUriString = "file:///storage/emulated/0/Download/document.pdf"
            val uri = mock<Uri>()
            whenever(uri.scheme).thenReturn("file")
            whenever(uri.path).thenReturn("/storage/emulated/0/Download/document.pdf")
            whenever(uri.toString()).thenReturn(fileUriString)
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn(null)
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(fileUriString)).thenReturn("document.pdf")
            var receivedNavKey: PdfViewerNavKey? = null

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = fileUriString,
                    isLocalContent = true,
                    isExternalFile = true,
                    title = "document.pdf",
                )
            )
        }

    @Test
    fun `test that handleAction calls launchLegacyPdfViewer when action is ACTION_VIEW with pdf path extension and compose flag is disabled`() =
        runTest {
            val uri = mock<Uri>()
            whenever(uri.path).thenReturn("/storage/emulated/0/Download/document.pdf")
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn(null)
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(false)
            var legacyLaunched = false

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                launchLegacyPdfViewer = { legacyLaunched = true },
            )

            assertThat(legacyLaunched).isTrue()
        }

    @Test
    fun `test that handleAction invokes navigateToComposePdfViewer with title appended with pdf extension when compose flag is enabled`() =
        runTest {
            val contentUriString = "content://com.example/file.pdf"
            val uri = mock<Uri>()
            whenever(uri.scheme).thenReturn("content")
            whenever(uri.toString()).thenReturn(contentUriString)
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(true)
            // Resolver returns base name without extension; production adds ".pdf" via FileUtil.addPdfFileExtension
            whenever(getFileNameFromStringUriUseCase(contentUriString)).thenReturn("file")
            var receivedNavKey: PdfViewerNavKey? = null

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = contentUriString,
                    isLocalContent = true,
                    isExternalFile = true,
                    title = "file.pdf",
                )
            )
        }

    @Test
    fun `test that handleAction does not open pdf viewer when intent data is null`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(null)
            var legacyLaunched = false
            var composeCalled = false

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                launchLegacyPdfViewer = { legacyLaunched = true },
                navigateToComposePdfViewer = { composeCalled = true },
            )

            assertThat(legacyLaunched).isFalse()
            assertThat(composeCalled).isFalse()
        }

    @Test
    fun `test that handleAction falls back to legacy viewer when feature flag throws exception`() =
        runTest {
            val uri = mock<Uri>()
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI))
                .thenThrow(RuntimeException("Feature flag service unavailable"))
            var legacyLaunched = false
            var composeCalled = false

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                launchLegacyPdfViewer = { legacyLaunched = true },
                navigateToComposePdfViewer = { composeCalled = true },
            )

            assertThat(legacyLaunched).isTrue()
            assertThat(composeCalled).isFalse()
        }

    @Test
    fun `test that handleAction uses last path segment as title when getFileNameFromStringUri returns null`() =
        runTest {
            val httpsUriString = "https://www.w3.org/sample.pdf"
            val uri = mock<Uri>()
            whenever(uri.scheme).thenReturn("https")
            whenever(uri.toString()).thenReturn(httpsUriString)
            whenever(uri.lastPathSegment).thenReturn("sample.pdf")
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(AppFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(httpsUriString)).thenReturn(null)
            var receivedNavKey: PdfViewerNavKey? = null

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = httpsUriString,
                    isLocalContent = false,
                    isExternalFile = true,
                    title = "sample.pdf",
                )
            )
        }

    @Test
    fun `test that handleAction does not handle as pdf when action is not ACTION_VIEW`() =
        runTest {
            val uri = mock<Uri>()
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                getShareUris = { null },
            )

            verify(getFeatureFlagValueUseCase, never()).invoke(any())
        }
}
