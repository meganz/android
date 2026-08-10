package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderLinkUiStateTest {

    @Test
    fun `test that isRootFolder returns true when both rootNode and currentFolderNode are null`() {
        val underTest = FolderLinkUiState(
            rootNode = null,
            currentFolderNode = null,
        )

        assertThat(underTest.isRootFolder).isTrue()
    }

    @Test
    fun `test that isRootFolder returns true when rootNode and currentFolderNode have same id`() {
        val node = mockFolderNode(id = 1L, name = "Folder")
        val underTest = FolderLinkUiState(
            rootNode = node,
            currentFolderNode = node,
        )

        assertThat(underTest.isRootFolder).isTrue()
    }

    @Test
    fun `test that isRootFolder returns false when rootNode and currentFolderNode have different ids`() {
        val underTest = FolderLinkUiState(
            rootNode = mockFolderNode(id = 1L),
            currentFolderNode = mockFolderNode(id = 2L),
        )

        assertThat(underTest.isRootFolder).isFalse()
    }

    @Test
    fun `test that isRootFolder returns false when rootNode is null and currentFolderNode is not`() {
        val underTest = FolderLinkUiState(
            rootNode = null,
            currentFolderNode = mockFolderNode(id = 1L),
        )

        assertThat(underTest.isRootFolder).isFalse()
    }

    @Test
    fun `test that isRootFolder returns false when rootNode is not null and currentFolderNode is null`() {
        val underTest = FolderLinkUiState(
            rootNode = mockFolderNode(id = 1L),
            currentFolderNode = null,
        )

        assertThat(underTest.isRootFolder).isFalse()
    }

    @Test
    fun `test that title returns Literal with folder name when currentFolderNode has a name`() {
        val underTest = FolderLinkUiState(
            rootNode = mockFolderNode(id = 1L, name = "Root"),
            currentFolderNode = mockFolderNode(id = 1L, name = "Root"),
        )

        assertThat(underTest.title).isEqualTo(LocalizedText.Literal("Root"))
    }

    @Test
    fun `test that title returns Literal with subfolder name when currentFolderNode is a subfolder`() {
        val underTest = FolderLinkUiState(
            rootNode = mockFolderNode(id = 1L, name = "Root"),
            currentFolderNode = mockFolderNode(id = 2L, name = "SubFolder"),
        )

        assertThat(underTest.title).isEqualTo(LocalizedText.Literal("SubFolder"))
    }

    @Test
    fun `test that title returns StringRes when currentFolderNode is null and isRootFolder is true`() {
        val underTest = FolderLinkUiState(
            rootNode = null,
            currentFolderNode = null,
        )

        assertThat(underTest.title).isEqualTo(
            LocalizedText.StringRes(sharedR.string.photos_empty_screen_brand_name_text)
        )
    }

    @Test
    fun `test that title returns empty Literal when currentFolderNode is null and isRootFolder is false`() {
        val underTest = FolderLinkUiState(
            rootNode = mockFolderNode(id = 1L),
            currentFolderNode = null,
        )

        assertThat(underTest.title).isEqualTo(LocalizedText.Literal(""))
    }

    @Test
    fun `test that title returns empty Literal when contentState is Expired`() {
        val underTest = FolderLinkUiState(
            contentState = FolderLinkContentState.Expired,
            rootNode = mockFolderNode(id = 1L, name = "Root"),
            currentFolderNode = mockFolderNode(id = 1L, name = "Root"),
        )

        assertThat(underTest.title).isEqualTo(LocalizedText.Literal(""))
    }

    @Test
    fun `test that title returns empty Literal when contentState is Unavailable`() {
        val underTest = FolderLinkUiState(
            contentState = FolderLinkContentState.Unavailable,
            rootNode = mockFolderNode(id = 1L, name = "Root"),
            currentFolderNode = mockFolderNode(id = 1L, name = "Root"),
        )

        assertThat(underTest.title).isEqualTo(LocalizedText.Literal(""))
    }

    @Test
    fun `test that subTitle returns StringRes when isRootFolder is true and contentState is Loaded`() {
        val underTest = FolderLinkUiState(
            contentState = FolderLinkContentState.Loaded,
            rootNode = null,
            currentFolderNode = null,
        )

        assertThat(underTest.subTitle).isEqualTo(
            LocalizedText.StringRes(sharedR.string.folder_link_subtitle)
        )
    }

    @Test
    fun `test that subTitle returns null when isRootFolder is false`() {
        val underTest = FolderLinkUiState(
            contentState = FolderLinkContentState.Loaded,
            rootNode = mockFolderNode(id = 1L),
            currentFolderNode = mockFolderNode(id = 2L),
        )

        assertThat(underTest.subTitle).isNull()
    }

    @Test
    fun `test that subTitle returns null when contentState is Expired`() {
        val underTest = FolderLinkUiState(
            contentState = FolderLinkContentState.Expired,
            rootNode = null,
            currentFolderNode = null,
        )

        assertThat(underTest.subTitle).isNull()
    }

    @Test
    fun `test that subTitle returns null when contentState is Unavailable`() {
        val underTest = FolderLinkUiState(
            contentState = FolderLinkContentState.Unavailable,
            rootNode = null,
            currentFolderNode = null,
        )

        assertThat(underTest.subTitle).isNull()
    }

    private fun mockFolderNode(
        id: Long = 1L,
        name: String = "folder",
    ): TypedFolderNode = mock {
        on { this.id } doReturn NodeId(id)
        on { this.name } doReturn name
    }
}
