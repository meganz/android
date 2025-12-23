package mega.privacy.mobile.home.presentation.recents.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.home.R
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsParentFolderNameMapperTest {

    private lateinit var underTest: RecentsParentFolderNameMapper

    @BeforeEach
    fun setUp() {
        underTest = RecentsParentFolderNameMapper()
    }

    @Test
    fun `test that Cloud Drive folder name is mapped to StringRes when decrypted`() {
        val bucket = createMockRecentActionBucket(
            parentFolderName = "Cloud Drive",
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result as LocalizedText.StringRes).resId)
            .isEqualTo(R.string.section_cloud_drive)
    }

    @Test
    fun `test that regular folder name is mapped to Literal when decrypted`() {
        val folderName = "My Folder"
        val bucket = createMockRecentActionBucket(
            parentFolderName = folderName,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result).isInstanceOf(LocalizedText.Literal::class.java)
        // Verify it's not a StringRes to ensure the correct path was taken
        assertThat(result).isNotInstanceOf(LocalizedText.StringRes::class.java)
    }

    @Test
    fun `test that folder name is mapped to undecrypted StringRes when not decrypted`() {
        val bucket = createMockRecentActionBucket(
            parentFolderName = "Any Folder Name",
            isNodeKeyDecrypted = false
        )

        val result = underTest(bucket)

        assertThat(result).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result as LocalizedText.StringRes).resId)
            .isEqualTo(R.string.shared_items_verify_credentials_undecrypted_folder)
    }

    @Test
    fun `test that Cloud Drive folder name is ignored when not decrypted`() {
        val bucket = createMockRecentActionBucket(
            parentFolderName = "Cloud Drive",
            isNodeKeyDecrypted = false
        )

        val result = underTest(bucket)

        // Even if folder name is "Cloud Drive", when not decrypted, it should return undecrypted string
        assertThat(result).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result as LocalizedText.StringRes).resId)
            .isEqualTo(R.string.shared_items_verify_credentials_undecrypted_folder)
    }

    @Test
    fun `test that empty folder name is mapped to Literal when decrypted`() {
        val bucket = createMockRecentActionBucket(
            parentFolderName = "",
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result).isInstanceOf(LocalizedText.Literal::class.java)
        // Verify it's not a StringRes to ensure the correct path was taken
        assertThat(result).isNotInstanceOf(LocalizedText.StringRes::class.java)
    }

    @Test
    fun `test that folder name with special characters is mapped correctly when decrypted`() {
        val folderName = "Folder & Files (2024)"
        val bucket = createMockRecentActionBucket(
            parentFolderName = folderName,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result).isInstanceOf(LocalizedText.Literal::class.java)
        // Verify it's not a StringRes to ensure the correct path was taken
        assertThat(result).isNotInstanceOf(LocalizedText.StringRes::class.java)
    }

    private fun createMockFileNode(
        name: String = "testFile.txt",
    ): TypedFileNode = mock {
        on { it.name }.thenReturn(name)
        val fileTypeInfo = TextFileTypeInfo("text/plain", "txt")
        on { it.type }.thenReturn(fileTypeInfo)
    }

    private fun createMockRecentActionBucket(
        parentFolderName: String = "TestFolder",
        isNodeKeyDecrypted: Boolean = true,
    ): RecentActionBucket = mock {
        val node = createMockFileNode()
        on { it.nodes }.thenReturn(listOf(node))
        on { it.parentFolderName }.thenReturn(parentFolderName)
        on { it.isNodeKeyDecrypted }.thenReturn(isNodeKeyDecrypted)
        on { it.dateTimestamp }.thenReturn(1234567890L)
    }
}

