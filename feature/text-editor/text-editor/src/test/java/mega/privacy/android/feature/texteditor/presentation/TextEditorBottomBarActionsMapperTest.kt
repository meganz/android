package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorBottomBarActionsMapperTest {

    private val underTest = TextEditorBottomBarActionsMapper()

    @Test
    fun `test that View mode with owner and not exported returns Download GetLink Share Edit`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).hasSize(4)
        assertThat(result[0]).isEqualTo(TextEditorBottomBarAction.Download)
        assertThat(result[1]).isEqualTo(TextEditorBottomBarAction.GetLink)
        assertThat(result[2]).isEqualTo(TextEditorBottomBarAction.Share)
        assertThat(result[3]).isEqualTo(TextEditorBottomBarAction.Edit)
    }

    @Test
    fun `test that Edit mode returns empty list`() {
        assertThat(
            underTest(
                mode = TextEditorMode.Edit,
                accessPermission = AccessPermission.OWNER,
                isNodeExported = false,
                inExcludedAdapterForGetLinkAndEdit = false,
                showDownload = true,
                showShare = true,
                showSendToChat = false,
            ),
        ).isEmpty()
    }

    @Test
    fun `test that Create mode returns empty list`() {
        assertThat(
            underTest(
                mode = TextEditorMode.Create,
                accessPermission = AccessPermission.OWNER,
                isNodeExported = false,
                inExcludedAdapterForGetLinkAndEdit = false,
                showDownload = true,
                showShare = true,
                showSendToChat = false,
            ),
        ).isEmpty()
    }

    @Test
    fun `test that Get Link is hidden when access is not OWNER`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.READWRITE,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
        assertThat(result).contains(TextEditorBottomBarAction.Share)
        assertThat(result).contains(TextEditorBottomBarAction.Edit)
    }

    @Test
    fun `test that Get Link is hidden when node is exported`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = true,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
    }

    @Test
    fun `test that Get Link is hidden when in excluded adapter`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = true,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
    }

    @Test
    fun `test that Download is hidden when showDownload is false`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = false,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Download)
        assertThat(result).contains(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that Share is hidden when showShare is false`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = false,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that Edit is hidden when access is READ only`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.READ,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Edit)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
    }

    @Test
    fun `test that Edit is hidden when in excluded adapter`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = true,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Edit)
    }

    @Test
    fun `test that Edit is shown for FULL and READWRITE access`() {
        listOf(AccessPermission.FULL, AccessPermission.READWRITE).forEach { access ->
            val result = underTest(
                mode = TextEditorMode.View,
                accessPermission = access,
                isNodeExported = false,
                inExcludedAdapterForGetLinkAndEdit = false,
                showDownload = true,
                showShare = true,
                showSendToChat = false,
            )
            assertThat(result).contains(TextEditorBottomBarAction.Edit)
            assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        }
    }

    @Test
    fun `test that unknown access and export yields no Get Link`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = null,
            isNodeExported = null,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Edit)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
        assertThat(result).contains(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that Get Link is hidden when node export state is null`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = null,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
    }

    @Test
    fun `test that showDownload false with excluded adapter shows only Share`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = true,
            showDownload = false,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).containsExactly(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that actions are in designer order Download GetLink Share Edit`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = false,
        )
        assertThat(result).containsExactly(
            TextEditorBottomBarAction.Download,
            TextEditorBottomBarAction.GetLink,
            TextEditorBottomBarAction.Share,
            TextEditorBottomBarAction.Edit,
        )
    }

    @Test
    fun `test that SendToChat is before Edit when both shown`() {
        val result = underTest(
            mode = TextEditorMode.View,
            accessPermission = AccessPermission.OWNER,
            isNodeExported = false,
            inExcludedAdapterForGetLinkAndEdit = false,
            showDownload = true,
            showShare = true,
            showSendToChat = true,
        )
        assertThat(result).containsExactly(
            TextEditorBottomBarAction.Download,
            TextEditorBottomBarAction.GetLink,
            TextEditorBottomBarAction.Share,
            TextEditorBottomBarAction.SendToChat,
            TextEditorBottomBarAction.Edit,
        )
    }
}
