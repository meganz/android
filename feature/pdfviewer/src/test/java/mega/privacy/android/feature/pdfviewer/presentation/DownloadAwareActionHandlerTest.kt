package mega.privacy.android.feature.pdfviewer.presentation

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.buildDownloadAwareActionHandler
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * Tests for [buildDownloadAwareActionHandler] — the wrapper that makes the PDF viewer's
 * Download action surface the "Downloading" snackbar while delegating every other action.
 */
class DownloadAwareActionHandlerTest {

    @Test
    fun `test that download action triggers onDownload and is not delegated`() {
        val node = mock<TypedNode>()
        var downloadedNode: TypedNode? = null
        val delegated = mutableListOf<Pair<MenuAction, TypedNode>>()
        val handler = buildDownloadAwareActionHandler(
            delegate = SingleNodeActionHandler { action, n -> delegated += action to n },
            onDownload = { downloadedNode = it },
        )

        handler(DownloadMenuAction(), node)

        assertThat(downloadedNode).isSameInstanceAs(node)
        assertThat(delegated).isEmpty()
    }

    @Test
    fun `test that a non-download action is delegated and does not trigger onDownload`() {
        val node = mock<TypedNode>()
        var onDownloadCalled = false
        val delegated = mutableListOf<Pair<MenuAction, TypedNode>>()
        val shareAction = ShareMenuAction()
        val handler = buildDownloadAwareActionHandler(
            delegate = SingleNodeActionHandler { action, n -> delegated += action to n },
            onDownload = { onDownloadCalled = true },
        )

        handler(shareAction, node)

        assertThat(onDownloadCalled).isFalse()
        assertThat(delegated).hasSize(1)
        assertThat(delegated.single().first).isSameInstanceAs(shareAction)
        assertThat(delegated.single().second).isSameInstanceAs(node)
    }
}
