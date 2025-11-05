package mega.privacy.android.app.presentation.transfers.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.navigation.TransferDeepLinkHandler.Companion.TAB_QUERY_PARAM
import mega.privacy.android.app.presentation.transfers.navigation.TransferDeepLinkHandler.Companion.getUriForTransfersSection
import mega.privacy.android.navigation.contract.PendingIntentHandler
import mega.privacy.android.navigation.destination.TransfersNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferDeepLinkHandlerTest {
    private lateinit var underTest: TransferDeepLinkHandler
    private val mockUriBuilderFactory = mock<() -> Uri.Builder>()

    @BeforeAll
    fun setup() {
        underTest = TransferDeepLinkHandler()
    }

    @BeforeEach
    fun cleanUp() {
        reset(mockUriBuilderFactory)
    }

    @Test
    fun `test URI is generated correctly when there are no tab parameter`() {
        stubUriAndUriBuilder()
        val expected = "mega://trans"
        val uri = getUriForTransfersSection(uriBuilderFactory = mockUriBuilderFactory)
        val actual = uri.toString()

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(TransfersNavKey.Tab::class)
    fun `test that URIs are generated correctly when there's a tab parameter`(
        tab: TransfersNavKey.Tab?,
    ) {
        stubUriAndUriBuilder(tabParam = tab?.toString())
        val expected = "mega://trans?tab=$tab"
        val uri = getUriForTransfersSection(tab, mockUriBuilderFactory)
        val actual = uri.toString()

        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(TransfersNavKey.Tab::class)
    @NullSource
    fun `test that URIs are parsed correctly`(
        tab: TransfersNavKey.Tab?,
    ) = runTest {
        stubUriAndUriBuilder(tabParam = tab?.toString())
        val expected = TransfersNavKey(tab)
        val uri = getUriForTransfersSection(tab, mockUriBuilderFactory)
        val actual = underTest.getNavKeysFromUri(uri)

        assertThat(actual).containsExactly(expected)
    }

    private fun stubUriAndUriBuilder(
        tabParam: String? = null,
        scheme: String = PendingIntentHandler.DEFAULT_SCHEME_FOR_PENDING_INTENTS,
        authority: String = TransferDeepLinkHandler.authority,
    ): Uri = mock<Uri> {
        on { this.scheme } doReturn scheme
        on { this.authority } doReturn authority
        tabParam?.let {
            on { getQueryParameter(TAB_QUERY_PARAM) } doReturn tabParam
        }
        on { toString() } doReturn "$scheme://$authority" +
                (tabParam?.let { "?$TAB_QUERY_PARAM=$tabParam" } ?: "")
    }.also { mockUri ->
        val mockUriBuilder = mock<Uri.Builder>()
        whenever(mockUriBuilderFactory.invoke()) doReturn mockUriBuilder
        whenever(mockUriBuilder.scheme(mockUri.scheme)) doReturn mockUriBuilder
        whenever(mockUriBuilder.authority(mockUri.authority)) doReturn mockUriBuilder
        whenever(
            mockUriBuilder.appendQueryParameter(
                TAB_QUERY_PARAM,
                mockUri.getQueryParameter(TAB_QUERY_PARAM)
            )
        ) doReturn mockUriBuilder
        whenever(mockUriBuilder.build()) doReturn mockUri
    }
}