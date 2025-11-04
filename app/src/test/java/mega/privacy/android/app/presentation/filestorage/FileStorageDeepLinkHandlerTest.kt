package mega.privacy.android.app.presentation.filestorage

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.filestorage.FileStorageDeepLinkHandler.Companion.PATH_QUERY_PARAM
import mega.privacy.android.app.presentation.filestorage.FileStorageDeepLinkHandler.Companion.getUriForFileStorageSection
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.contract.PendingIntentHandler
import mega.privacy.android.navigation.destination.LegacyFileExplorerNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileStorageDeepLinkHandlerTest {
    private lateinit var underTest: FileStorageDeepLinkHandler
    private val mockUriBuilderFactory = mock<() -> Uri.Builder>()

    @BeforeAll
    fun setup() {
        underTest = FileStorageDeepLinkHandler()
    }

    @BeforeEach
    fun cleanUp() {
        reset(mockUriBuilderFactory)
    }

    @Test
    fun `test URI is generated correctly`() {
        stubUriAndUriBuilder()
        val expected = "mega://fileStorage?path=$DESTINATION"
        val uri = getUriForFileStorageSection(
            destination = DESTINATION,
            highlightedFiles = emptyList(),
            mockUriBuilderFactory
        )
        val actual = uri.toString()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that URIs are parsed correctly`() {
        stubUriAndUriBuilder()
        val expected = LegacyFileExplorerNavKey(UriPath(DESTINATION))
        val uri = getUriForFileStorageSection(DESTINATION, emptyList(), mockUriBuilderFactory)
        val actual = underTest.getNavKeysFromUri(uri)

        assertThat(actual).containsExactly(expected)
    }

    private fun stubUriAndUriBuilder(
        destination: String = DESTINATION,
        scheme: String = PendingIntentHandler.DEFAULT_SCHEME_FOR_PENDING_INTENTS,
        authority: String = FileStorageDeepLinkHandler.authority,
    ): Uri = mock<Uri> {
        on { this.scheme } doReturn scheme
        on { this.authority } doReturn authority
        on { getQueryParameter(PATH_QUERY_PARAM) } doReturn destination
        on { toString() } doReturn "$scheme://$authority?$PATH_QUERY_PARAM=$destination"
    }.also { mockUri ->
        val mockUriBuilder = mock<Uri.Builder>()
        whenever(mockUriBuilderFactory.invoke()) doReturn mockUriBuilder
        whenever(mockUriBuilder.scheme(mockUri.scheme)) doReturn mockUriBuilder
        whenever(mockUriBuilder.authority(mockUri.authority)) doReturn mockUriBuilder
        whenever(
            mockUriBuilder.appendQueryParameter(
                PATH_QUERY_PARAM,
                mockUri.getQueryParameter(PATH_QUERY_PARAM)
            )
        ) doReturn mockUriBuilder
        whenever(mockUriBuilder.build()) doReturn mockUri
    }
}

private const val DESTINATION =
    "content://com.android.externalstorage.documents/tree/primary%3ADocuments//test/text.txt"