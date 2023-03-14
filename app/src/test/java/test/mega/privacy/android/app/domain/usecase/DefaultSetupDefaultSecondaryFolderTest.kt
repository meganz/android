package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultSetupDefaultSecondaryFolder
import mega.privacy.android.app.domain.usecase.GetDefaultNodeHandle
import mega.privacy.android.app.domain.usecase.SetupDefaultSecondaryFolder
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.GetUploadFolderHandle
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultSetupDefaultSecondaryFolderTest {
    private lateinit var underTest: SetupDefaultSecondaryFolder
    private val invalidHandle = -1L
    private val validHandle = 69L
    private val folderName = "CU"

    private val cameraUploadRepository = mock<CameraUploadRepository> {
        onBlocking {
            getInvalidHandle()
        }.thenReturn(invalidHandle)
    }
    private val getUploadFolderHandle = mock<GetUploadFolderHandle>()
    private val getDefaultNodeHandle = mock<GetDefaultNodeHandle>()
    private val isNodeInRubbishOrDeleted = mock<IsNodeInRubbishOrDeleted>()
    private val setupSecondaryFolder = mock<SetupSecondaryFolder>()

    @Before
    fun setUp() {
        underTest = DefaultSetupDefaultSecondaryFolder(
            cameraUploadRepository = cameraUploadRepository,
            getUploadFolderHandle = getUploadFolderHandle,
            getDefaultNodeHandle = getDefaultNodeHandle,
            isNodeInRubbishOrDeleted = isNodeInRubbishOrDeleted,
            setupSecondaryFolder = setupSecondaryFolder
        )
    }

    @Test
    fun `test that if secondary folder handle is valid and default handle is invalid that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(validHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(false)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolder)
        }

    @Test
    fun `test that if secondary folder handle is valid and not deleted and default handle is valid, that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(validHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(false)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolder)
        }

    @Test
    fun `test that if secondary folder handle is invalid and deleted and default handle is invalid, that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(invalidHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(true)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolder)
        }

    @Test
    fun `test that if secondary folder handle is valid and deleted and default handle is invalid, that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(validHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(true)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolder)
        }

    @Test
    fun `test that if secondary folder handle is invalid and not deleted and default handle is invalid, that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(invalidHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(false)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolder)
        }

    @Test
    fun `test that if secondary folder handle is valid and deleted and default handle is valid that a new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(validHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(true)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verify(setupSecondaryFolder, times(1)).invoke(validHandle)
        }

    @Test
    fun `test that if secondary folder handle is invalid and not deleted and default handle is valid, that a new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(invalidHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(false)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verify(setupSecondaryFolder, times(1)).invoke(validHandle)
        }

    @Test
    fun `test that if secondary folder handle is invalid and deleted and default handle is valid, that a new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandle(isPrimary = false)).thenReturn(invalidHandle)
            whenever(isNodeInRubbishOrDeleted(any())).thenReturn(true)
            whenever(getDefaultNodeHandle(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verify(setupSecondaryFolder, times(1)).invoke(validHandle)
        }
}
