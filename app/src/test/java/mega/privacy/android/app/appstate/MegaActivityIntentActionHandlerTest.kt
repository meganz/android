package mega.privacy.android.app.appstate

import android.app.Activity
import android.content.ClipData
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
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.GetCurrentConnectivityStateUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.DeepLinksDialogNavKey
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import mega.privacy.android.navigation.destination.ShareFilesToMegaNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.doReturn
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
            activity = mock<Activity>(),
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
    fun `test that handleAction emits ShareFilesToMegaNavKey with single uri when action is ACTION_SEND with non-text type`() =
        runTest {
            val uriString = "content://test"
            val uri = mock<Uri> {
                on { toString() } doReturn uriString
            }
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            whenever(intent.type).thenReturn("image/png")
            whenever(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)).thenReturn(uri)

            underTest.handleAction(intent, {})

            verify(navigationEventQueue)
                .emit(ShareFilesToMegaNavKey(listOf(UriPath(uriString))))
        }

    @Test
    fun `test that handleAction emits ShareFilesToMegaNavKey with multiple uris when action is ACTION_SEND_MULTIPLE`() =
        runTest {
            val firstUri = mock<Uri> { on { toString() } doReturn "content://first" }
            val secondUri = mock<Uri> { on { toString() } doReturn "content://second" }
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND_MULTIPLE)
            whenever(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))
                .thenReturn(arrayListOf(firstUri, secondUri))

            underTest.handleAction(intent, {})

            verify(navigationEventQueue).emit(
                ShareFilesToMegaNavKey(
                    listOf(UriPath("content://first"), UriPath("content://second"))
                )
            )
        }

    @Test
    fun `test that handleAction does not emit when action is ACTION_SEND with no extra stream`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            whenever(intent.type).thenReturn("image/png")

            underTest.handleAction(intent, {})

            verify(navigationEventQueue, never()).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
        }

    @Test
    fun `test that handleAction does not emit when action is ACTION_SEND_MULTIPLE with empty extra stream`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND_MULTIPLE)
            whenever(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM))
                .thenReturn(arrayListOf())

            underTest.handleAction(intent, {})

            verify(navigationEventQueue, never()).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
        }

    @Test
    fun `test that handleAction emits ShareTextToMegaNavKey when action is ACTION_SEND with text plain type and EXTRA_TEXT`() =
        runTest {
            val sharedText = "shared text"
            val subject = "subject"
            val email = "user@example.com"
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            whenever(intent.type).thenReturn(Constants.TYPE_TEXT_PLAIN)
            whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(sharedText)
            whenever(intent.getStringExtra(Intent.EXTRA_SUBJECT)).thenReturn(subject)
            whenever(intent.getStringExtra(Intent.EXTRA_EMAIL)).thenReturn(email)

            underTest.handleAction(intent, {})

            verify(navigationEventQueue).emit(
                ShareTextToMegaNavKey(text = sharedText, subject = subject, email = email)
            )
        }

    @Test
    fun `test that handleAction emits ShareTextToMegaNavKey from clipData when EXTRA_TEXT is missing`() =
        runTest {
            val sharedText = "clipboard text"
            val item = mock<ClipData.Item> {
                on { text } doReturn sharedText
            }
            val clipData = mock<ClipData> {
                on { itemCount } doReturn 1
                on { getItemAt(0) } doReturn item
            }
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            whenever(intent.type).thenReturn(Constants.TYPE_TEXT_PLAIN)
            whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(null)
            whenever(intent.clipData).thenReturn(clipData)

            underTest.handleAction(intent, {})

            verify(navigationEventQueue).emit(
                ShareTextToMegaNavKey(text = sharedText, subject = null, email = null)
            )
        }

    @Test
    fun `test that handleAction does not emit when action is ACTION_SEND with text plain type but no text payload`() =
        runTest {
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_SEND)
            whenever(intent.type).thenReturn(Constants.TYPE_TEXT_PLAIN)
            whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(null)
            whenever(intent.clipData).thenReturn(null)

            underTest.handleAction(intent, {})

            verify(navigationEventQueue, never()).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(contentUriString)).thenReturn("file.pdf")

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
            )

            verify(navigationEventQueue).emit(
                navKey = check {
                    assertThat(it).isEqualTo(
                        PdfViewerNavKey(
                            contentUri = contentUriString,
                            isLocalContent = true,
                            isExternalFile = true,
                            title = "file.pdf",
                        )
                    )
                },
                priority = any(),
                navOptions = anyOrNull(),
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(httpsUriString)).thenReturn("sample.pdf")

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
            )

            verify(navigationEventQueue).emit(
                navKey = check {
                    assertThat(it).isEqualTo(
                        PdfViewerNavKey(
                            contentUri = httpsUriString,
                            isLocalContent = false,
                            isExternalFile = true,
                            title = "sample.pdf",
                        )
                    )
                },
                priority = any(),
                navOptions = anyOrNull(),
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(httpUriString)).thenReturn("sample.pdf")

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
            )

            verify(navigationEventQueue).emit(
                navKey = check {
                    assertThat(it).isEqualTo(
                        PdfViewerNavKey(
                            contentUri = httpUriString,
                            isLocalContent = false,
                            isExternalFile = true,
                            title = "sample.pdf",
                        )
                    )
                },
                priority = any(),
                navOptions = anyOrNull(),
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(false)
            var legacyLaunched = false

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(fileUriString)).thenReturn("document.pdf")

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
            )

            verify(navigationEventQueue).emit(
                navKey = check {
                    assertThat(it).isEqualTo(
                        PdfViewerNavKey(
                            contentUri = fileUriString,
                            isLocalContent = true,
                            isExternalFile = true,
                            title = "document.pdf",
                        )
                    )
                },
                priority = any(),
                navOptions = anyOrNull(),
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(false)
            var legacyLaunched = false

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(true)
            // Resolver returns base name without extension; production adds ".pdf" via FileUtil.addPdfFileExtension
            whenever(getFileNameFromStringUriUseCase(contentUriString)).thenReturn("file")

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
            )

            verify(navigationEventQueue).emit(
                navKey = check {
                    assertThat(it).isEqualTo(
                        PdfViewerNavKey(
                            contentUri = contentUriString,
                            isLocalContent = true,
                            isExternalFile = true,
                            title = "file.pdf",
                        )
                    )
                },
                priority = any(),
                navOptions = anyOrNull(),
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

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                launchLegacyPdfViewer = { legacyLaunched = true },
            )

            assertThat(legacyLaunched).isFalse()
            verify(navigationEventQueue, never()).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
        }

    @Test
    fun `test that handleAction falls back to legacy viewer when feature flag throws exception`() =
        runTest {
            val uri = mock<Uri>()
            val intent = mock<Intent>()
            whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
            whenever(intent.type).thenReturn("application/pdf")
            whenever(intent.data).thenReturn(uri)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI))
                .thenThrow(RuntimeException("Feature flag service unavailable"))
            var legacyLaunched = false

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
                launchLegacyPdfViewer = { legacyLaunched = true },
            )

            assertThat(legacyLaunched).isTrue()
            verify(navigationEventQueue, never()).emit(
                navKey = any(),
                priority = any(),
                navOptions = anyOrNull(),
            )
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
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(httpsUriString)).thenReturn(null)

            underTest.handleAction(
                intent = intent,
                refreshSession = {},
            )

            verify(navigationEventQueue).emit(
                navKey = check {
                    assertThat(it).isEqualTo(
                        PdfViewerNavKey(
                            contentUri = httpsUriString,
                            isLocalContent = false,
                            isExternalFile = true,
                            title = "sample.pdf",
                        )
                    )
                },
                priority = any(),
                navOptions = anyOrNull(),
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
            )

            verify(getFeatureFlagValueUseCase, never()).invoke(any())
        }
}
