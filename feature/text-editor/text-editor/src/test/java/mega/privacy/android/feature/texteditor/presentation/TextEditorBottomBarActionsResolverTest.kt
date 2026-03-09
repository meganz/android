package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorBottomBarActionsResolverTest {

    @Test
    fun `test that View mode with owner and not exported returns Download GetLink Share Edit`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            false,
            false,
            true,
            true,
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
            computeTextEditorBottomBarActions(
                TextEditorMode.Edit,
                AccessPermission.OWNER,
                false,
                false,
                true,
                true,
            ),
        ).isEmpty()
    }

    @Test
    fun `test that Create mode returns empty list`() {
        assertThat(
            computeTextEditorBottomBarActions(
                TextEditorMode.Create,
                AccessPermission.OWNER,
                false,
                false,
                true,
                true,
            ),
        ).isEmpty()
    }

    @Test
    fun `test that Get Link is hidden when access is not OWNER`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.READWRITE,
            false,
            false,
            true,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
        assertThat(result).contains(TextEditorBottomBarAction.Share)
        assertThat(result).contains(TextEditorBottomBarAction.Edit)
    }

    @Test
    fun `test that Get Link is hidden when node is exported`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            true,
            false,
            true,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
    }

    @Test
    fun `test that Get Link is hidden when in excluded adapter`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            false,
            true,
            true,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
    }

    @Test
    fun `test that Download is hidden when showDownload is false`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            false,
            false,
            false,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Download)
        assertThat(result).contains(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that Share is hidden when showShare is false`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            false,
            false,
            true,
            false,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that Edit is hidden when access is READ only`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.READ,
            false,
            false,
            true,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Edit)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
    }

    @Test
    fun `test that Edit is hidden when in excluded adapter`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            false,
            true,
            true,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Edit)
    }

    @Test
    fun `test that Edit is shown for FULL and READWRITE access`() {
        listOf(AccessPermission.FULL, AccessPermission.READWRITE).forEach { access ->
            val result = computeTextEditorBottomBarActions(
                TextEditorMode.View,
                access,
                false,
                false,
                true,
                true,
            )
            assertThat(result).contains(TextEditorBottomBarAction.Edit)
            assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        }
    }

    @Test
    fun `test that unknown access and export yields no Get Link`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            null,
            null,
            false,
            true,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        assertThat(result).doesNotContain(TextEditorBottomBarAction.Edit)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
        assertThat(result).contains(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that Get Link is hidden when node export state is null`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            null,
            false,
            true,
            true,
        )
        assertThat(result).doesNotContain(TextEditorBottomBarAction.GetLink)
        assertThat(result).contains(TextEditorBottomBarAction.Download)
    }

    @Test
    fun `test that showDownload false with excluded adapter shows only Share`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            false,
            true,
            false,
            true,
        )
        assertThat(result).containsExactly(TextEditorBottomBarAction.Share)
    }

    @Test
    fun `test that actions are in designer order when all four shown`() {
        val result = computeTextEditorBottomBarActions(
            TextEditorMode.View,
            AccessPermission.OWNER,
            false,
            false,
            true,
            true,
        )
        assertThat(result).containsExactly(
            TextEditorBottomBarAction.Download,
            TextEditorBottomBarAction.GetLink,
            TextEditorBottomBarAction.Share,
            TextEditorBottomBarAction.Edit,
        )
    }
}
