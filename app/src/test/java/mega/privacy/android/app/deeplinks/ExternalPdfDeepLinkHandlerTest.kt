package mega.privacy.android.app.deeplinks

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalPdfDeepLinkHandlerTest {

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getFileNameFromStringUriUseCase = mock<GetFileNameFromStringUriUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()

    private lateinit var underTest: ExternalPdfDeepLinkHandler

    @BeforeEach
    fun setUp() {
        reset(
            getFeatureFlagValueUseCase,
            getFileNameFromStringUriUseCase,
            isConnectedToInternetUseCase,
        )
        underTest = ExternalPdfDeepLinkHandler(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getFileNameFromStringUriUseCase = getFileNameFromStringUriUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
        )
    }

    @Test
    fun `test that consume returns false when action is not ACTION_VIEW`() = runTest {
        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_SEND)
        }
        var callbackCalled = false

        val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
            intent = intent,
            launchLegacyPdfViewer = {},
            navigateToComposePdfViewer = { callbackCalled = true },
        )

        assertThat(consumed).isFalse()
        assertThat(callbackCalled).isFalse()
    }

    @Test
    fun `test that consume returns false when action is VIEW but not pdf`() = runTest {
        val uri = mock<Uri>()
        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_VIEW)
            on { type }.thenReturn("image/png")
            on { data }.thenReturn(uri)
        }
        whenever(uri.path).thenReturn("/pictures/photo.png")
        var callbackCalled = false

        val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
            intent = intent,
            launchLegacyPdfViewer = {},
            navigateToComposePdfViewer = { callbackCalled = true },
        )

        assertThat(consumed).isFalse()
        assertThat(callbackCalled).isFalse()
    }

    @Test
    fun `test that consume invokes navigateToComposePdfViewer with correct key when compose flag is on`() =
        runTest {
            val contentUriString = "content://authority/doc.pdf"
            val uri = mock<Uri> {
                on { scheme }.thenReturn("content")
                on { toString() }.thenReturn(contentUriString)
            }
            val intent = mock<Intent> {
                on { action }.thenReturn(Intent.ACTION_VIEW)
                on { type }.thenReturn("application/pdf")
                on { data }.thenReturn(uri)
            }
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(true)
            whenever(getFileNameFromStringUriUseCase(contentUriString)).thenReturn("doc.pdf")
            var receivedNavKey: PdfViewerNavKey? = null

            val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
                intent = intent,
                launchLegacyPdfViewer = {},
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(consumed).isTrue()
            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = contentUriString,
                    isLocalContent = true,
                    isExternalFile = true,
                    title = "doc.pdf",
                )
            )
        }

    @Test
    fun `test that consume invokes legacy launcher when compose flag is off`() = runTest {
        val uri = mock<Uri> {
            on { scheme }.thenReturn("content")
            on { toString() }.thenReturn("content://x/a.pdf")
        }
        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_VIEW)
            on { type }.thenReturn("application/pdf")
            on { data }.thenReturn(uri)
        }
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.PdfViewerComposeUI)).thenReturn(false)
        var legacyCalled = false
        var callbackCalled = false

        val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
            intent = intent,
            launchLegacyPdfViewer = { legacyCalled = true },
            navigateToComposePdfViewer = { callbackCalled = true },
        )

        assertThat(consumed).isTrue()
        assertThat(legacyCalled).isTrue()
        assertThat(callbackCalled).isFalse()
    }

    @Test
    fun `test that consume routes to compose viewer without checking feature flag when offline`() =
        runTest {
            val contentUriString = "content://authority/doc.pdf"
            val uri = mock<Uri> {
                on { scheme }.thenReturn("content")
                on { toString() }.thenReturn(contentUriString)
            }
            val intent = mock<Intent> {
                on { action }.thenReturn(Intent.ACTION_VIEW)
                on { type }.thenReturn("application/pdf")
                on { data }.thenReturn(uri)
            }
            whenever(isConnectedToInternetUseCase()).thenReturn(false)
            whenever(getFileNameFromStringUriUseCase(contentUriString)).thenReturn("doc.pdf")
            var legacyCalled = false
            var callbackCalled = false

            val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
                intent = intent,
                launchLegacyPdfViewer = { legacyCalled = true },
                navigateToComposePdfViewer = { callbackCalled = true },
            )

            assertThat(consumed).isTrue()
            assertThat(callbackCalled).isTrue()
            assertThat(legacyCalled).isFalse()
            verify(getFeatureFlagValueUseCase, never())(ApiFeatures.PdfViewerComposeUI)
        }
}
