package mega.privacy.android.app.deeplinks

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.navigation.destination.PdfViewerNavKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalPdfDeepLinkHandlerTest {

    private val getFileNameFromStringUriUseCase = mock<GetFileNameFromStringUriUseCase>()

    private lateinit var underTest: ExternalPdfDeepLinkHandler

    @BeforeEach
    fun setUp() {
        reset(getFileNameFromStringUriUseCase)
        underTest = ExternalPdfDeepLinkHandler(
            getFileNameFromStringUriUseCase = getFileNameFromStringUriUseCase,
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
            navigateToComposePdfViewer = { callbackCalled = true },
        )

        assertThat(consumed).isFalse()
        assertThat(callbackCalled).isFalse()
    }

    @Test
    fun `test that consume returns false when action is VIEW pdf but data is null`() = runTest {
        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_VIEW)
            on { type }.thenReturn("application/pdf")
            on { data }.thenReturn(null)
        }
        var callbackCalled = false

        val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
            intent = intent,
            navigateToComposePdfViewer = { callbackCalled = true },
        )

        assertThat(consumed).isFalse()
        assertThat(callbackCalled).isFalse()
    }

    @Test
    fun `test that consume invokes navigateToComposePdfViewer with correct key for local content`() =
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
            whenever(getFileNameFromStringUriUseCase(contentUriString)).thenReturn("doc.pdf")
            var receivedNavKey: PdfViewerNavKey? = null

            val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
                intent = intent,
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
    fun `test that consume invokes navigateToComposePdfViewer with isLocalContent false for http uri`() =
        runTest {
            val httpsUriString = "https://www.w3.org/sample.pdf"
            val uri = mock<Uri> {
                on { scheme }.thenReturn("https")
                on { toString() }.thenReturn(httpsUriString)
            }
            val intent = mock<Intent> {
                on { action }.thenReturn(Intent.ACTION_VIEW)
                on { type }.thenReturn("application/pdf")
                on { data }.thenReturn(uri)
            }
            whenever(getFileNameFromStringUriUseCase(httpsUriString)).thenReturn("sample.pdf")
            var receivedNavKey: PdfViewerNavKey? = null

            val consumed = underTest.consumeExternalActionViewPdfIfApplicable(
                intent = intent,
                navigateToComposePdfViewer = { receivedNavKey = it },
            )

            assertThat(consumed).isTrue()
            assertThat(receivedNavKey).isEqualTo(
                PdfViewerNavKey(
                    contentUri = httpsUriString,
                    isLocalContent = false,
                    isExternalFile = true,
                    title = "sample.pdf",
                )
            )
        }
}
