package mega.privacy.android.feature.pdfviewer.presentation.components

import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.test.RecordingTimberTree
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class GetPdfUriTest {

    private val tree = RecordingTimberTree()

    @Before
    fun plantTree() {
        Timber.plant(tree)
    }

    @After
    fun uprootTree() {
        Timber.uproot(tree)
    }

    @Test
    fun `test that getPdfUri returns the parsed content uri for a remote source`() {
        val actual = getPdfUri(
            PdfViewerSource.FolderLink(
                nodeHandle = 4L,
                contentUri = STREAM_URI,
                isLocalContent = false,
            )
        )

        assertThat(actual).isEqualTo(Uri.parse(STREAM_URI))
    }

    @Test
    fun `test that getPdfUri does not log the content uri of remote sources`() {
        getPdfUri(
            PdfViewerSource.CloudNode(
                nodeHandle = 1L,
                contentUri = STREAM_URI,
                isLocalContent = false,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )
        )
        getPdfUri(
            PdfViewerSource.ChatAttachment(
                chatId = 1L,
                messageId = 2L,
                nodeHandle = 3L,
                contentUri = STREAM_URI,
                isLocalContent = false,
            )
        )
        getPdfUri(
            PdfViewerSource.FolderLink(
                nodeHandle = 4L,
                contentUri = STREAM_URI,
                isLocalContent = false,
            )
        )

        assertThat(tree.messages).isNotEmpty()
        assertThat(tree.messages.none { it.contains(KEY) }).isTrue()
        assertThat(tree.messages.none { it.contains(FILE_NAME) }).isTrue()
    }

    private companion object {
        const val KEY = "SeCrEtNoDeKeY_0123456789abcdefghijklmnopqrs"
        const val FILE_NAME = "secret-document.pdf"
        const val STREAM_URI = "http://127.0.0.1:4443/aBcDeFgH!$KEY!123/$FILE_NAME"
    }
}
