package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.usecase.GetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.node.GetNestedParentFoldersUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDefaultDownloadPathForNodeUseCaseTest {
    private lateinit var underTest: GetDefaultDownloadPathForNodeUseCase

    private val getStorageDownloadLocationUseCase: GetStorageDownloadLocationUseCase = mock()
    private val getNestedParentFoldersUseCase: GetNestedParentFoldersUseCase = mock()
    private val isNodeInCloudDriveUseCase: IsNodeInCloudDriveUseCase = mock()

    private val parent = mock<FolderNode>()
    private val grandParent = mock<FolderNode>()
    private val greatGrandParent = mock<FolderNode>()

    @BeforeAll
    fun setup() {
        underTest = GetDefaultDownloadPathForNodeUseCase(
            getStorageDownloadLocationUseCase,
            getNestedParentFoldersUseCase,
            isNodeInCloudDriveUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getStorageDownloadLocationUseCase,
            getNestedParentFoldersUseCase,
            isNodeInCloudDriveUseCase,
            parent,
            grandParent,
            greatGrandParent,
        )
    }


    @Test
    fun `test that invoke returns the full path when node is in cloud drive`() = runTest {
        whenever(isNodeInCloudDriveUseCase(any())).thenReturn(true)
        stubNodes()
        stubDeviceFolder()
        whenever(getNestedParentFoldersUseCase(any()))
            .thenReturn(listOf(greatGrandParent, grandParent))
        val actual = underTest(parent)
        Truth.assertThat(actual)
            .isEqualTo(DEVICE_PATH + File.separator + GREAT_GRAND_PARENT_NAME + File.separator + GRAND_PARENT_NAME + File.separator)
    }

    @Test
    fun `test that invoke returns the download root path when node is not in cloud drive`() =
        runTest {
            whenever(isNodeInCloudDriveUseCase(any())).thenReturn(false)
            stubDeviceFolder()
            val actual = underTest(parent)
            Truth.assertThat(actual)
                .isEqualTo(DEVICE_PATH)
        }

    private fun stubNodes() {
        whenever(parent.name).thenReturn(PARENT_NAME)
        whenever(grandParent.name).thenReturn(GRAND_PARENT_NAME)
        whenever(greatGrandParent.name).thenReturn(GREAT_GRAND_PARENT_NAME)
    }

    private suspend fun stubDeviceFolder() {
        whenever(getStorageDownloadLocationUseCase()).thenReturn(DEVICE_PATH)
    }

    companion object {
        private const val DEVICE_PATH = "sdcard/download/MEGA download"
        private const val PARENT_NAME = "parent"
        private const val GRAND_PARENT_NAME = "grand parent"
        private const val GREAT_GRAND_PARENT_NAME = "great grand parent"
    }
}