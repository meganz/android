package test.mega.privacy.android.app.presentation.fileinfo.model

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState.Companion.MAX_NUMBER_OF_CONTACTS_IN_LIST
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import nz.mega.sdk.MegaShare
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock

class FileInfoViewStateTest {

    private lateinit var underTest: FileInfoViewState

    @Test
    fun `test outSharesCoerceMax is returning all outShares if maximum is not surpassed`() {
        val outShares = List<MegaShare>(MAX_NUMBER_OF_CONTACTS_IN_LIST) { mock() }
        underTest = FileInfoViewState(outShares = outShares)
        Truth.assertThat(underTest.outSharesCoerceMax.size)
            .isEqualTo(MAX_NUMBER_OF_CONTACTS_IN_LIST)
    }

    @Test
    fun `test outSharesCoerceMax is returning no more than maximum out shares`() {
        val outShares = List<MegaShare>(MAX_NUMBER_OF_CONTACTS_IN_LIST + 1) { mock() }
        underTest = FileInfoViewState(outShares = outShares)
        Truth.assertThat(underTest.outSharesCoerceMax.size)
            .isEqualTo(MAX_NUMBER_OF_CONTACTS_IN_LIST)
    }

    @Test
    fun `test that when typed node is updated with a file node then available offline is updated to true`() {
        underTest = FileInfoViewState(isAvailableOfflineAvailable = false)
        val result = underTest.copyWithTypedNode(typedNode = mockFile())
        Truth.assertThat(result.isAvailableOfflineAvailable).isEqualTo(true)
    }

    @Test
    fun `test that when typed node is updated with a file node then size is updated to file size`() {
        underTest = FileInfoViewState()
        val result = underTest.copyWithTypedNode(typedNode = mockFile())
        Truth.assertThat(result.sizeInBytes).isEqualTo(FILE_SIZE)
    }

    @ParameterizedTest(name = "initial value: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that when typed node is updated with a folder node then available offline is not updated`(
        initialValue: Boolean,
    ) {
        underTest = FileInfoViewState(isAvailableOfflineAvailable = initialValue)
        val result = underTest.copyWithTypedNode(typedNode = mockFolder())
        Truth.assertThat(result.isAvailableOfflineAvailable).isEqualTo(initialValue)
    }

    @Test
    fun `test that when typed node is updated with a folder node then size is not updated`() {
        underTest = FileInfoViewState()
        val result = underTest.copyWithTypedNode(typedNode = mockFolder())
        Truth.assertThat(result.sizeInBytes).isEqualTo(0L)
    }

    @Test
    fun `test that when folder tree info is updated then size is updated to totalCurrentSizeInBytes plus sizeOfPreviousVersionsInBytes`() {
        underTest = FileInfoViewState()
        val result = underTest.copyWithFolderTreeInfo(mockFolderTreeInfo())
        Truth.assertThat(result.sizeInBytes).isEqualTo(CURRENT_SIZE + PREVIOUS_SIZE)
    }

    @ParameterizedTest(name = "folder empty: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that when folder tree info is updated then isAvailableOffline is updated to folder is empty or not`(
        empty: Boolean,
    ) {
        underTest = FileInfoViewState()
        val result = underTest.copyWithFolderTreeInfo(mockFolderTreeInfo(empty))
        Truth.assertThat(result.isAvailableOfflineAvailable).isEqualTo(!empty)
    }

    private fun mockFolder() = mock<TypedFolderNode> {
        on { name }.thenReturn("Node")
    }

    private fun mockFile() = mock<TypedFileNode> {
        on { name }.thenReturn("Node")
        on { size }.thenReturn(FILE_SIZE)
    }

    private fun mockFolderTreeInfo(empty: Boolean = true) = mock<FolderTreeInfo> {
        on { totalCurrentSizeInBytes }.thenReturn(CURRENT_SIZE)
        on { sizeOfPreviousVersionsInBytes }.thenReturn(PREVIOUS_SIZE)
        on { numberOfFiles }.thenReturn(if (empty) 0 else 1)
    }

    companion object {
        private const val FILE_SIZE = 123L
        private const val CURRENT_SIZE = 1234L
        private const val PREVIOUS_SIZE = 12345L
    }
}