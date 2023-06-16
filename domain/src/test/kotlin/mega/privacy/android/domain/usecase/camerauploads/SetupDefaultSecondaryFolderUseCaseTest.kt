package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupDefaultSecondaryFolderUseCaseTest {
    private lateinit var underTest: SetupDefaultSecondaryFolderUseCase
    private val invalidHandle = -1L
    private val validHandle = 69L
    private val folderName = "CU"

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getUploadFolderHandleUseCase = mock<GetUploadFolderHandleUseCase>()
    private val getDefaultNodeHandleUseCase = mock<GetDefaultNodeHandleUseCase>()
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()
    private val setupSecondaryFolderUseCase = mock<SetupSecondaryFolderUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetupDefaultSecondaryFolderUseCase(
            cameraUploadRepository = cameraUploadRepository,
            getUploadFolderHandleUseCase = getUploadFolderHandleUseCase,
            getDefaultNodeHandleUseCase = getDefaultNodeHandleUseCase,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
            setupSecondaryFolderUseCase = setupSecondaryFolderUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            getUploadFolderHandleUseCase,
            getDefaultNodeHandleUseCase,
            isNodeInRubbishOrDeletedUseCase,
            setupSecondaryFolderUseCase,
        )
    }

    @Test
    fun `test that if secondary folder handle is valid and default handle is invalid that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                validHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(false)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolderUseCase)
        }

    @Test
    fun `test that if secondary folder handle is valid and not deleted and default handle is valid, that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                validHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(false)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolderUseCase)
        }

    @Test
    fun `test that if secondary folder handle is invalid and deleted and default handle is invalid, that no new secondary folder gets setup`() =
        runTest {
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                invalidHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(true)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolderUseCase)
        }

    @Test
    fun `test that if secondary folder handle is valid and deleted and default handle is invalid, that no new secondary folder gets setup`() =
        runTest {
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                validHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(true)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolderUseCase)
        }

    @Test
    fun `test that if secondary folder handle is invalid and not deleted and default handle is invalid, that no new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                invalidHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(false)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(invalidHandle)
            underTest(folderName)
            verifyNoInteractions(setupSecondaryFolderUseCase)
        }

    @Test
    fun `test that if secondary folder handle is valid and deleted and default handle is valid that a new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                validHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(true)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verify(setupSecondaryFolderUseCase, times(1)).invoke(validHandle)
        }

    @Test
    fun `test that if secondary folder handle is invalid and not deleted and default handle is valid, that a new secondary folder gets setup`() =
        runTest {
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                invalidHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(false)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verify(setupSecondaryFolderUseCase, times(1)).invoke(validHandle)
        }

    @Test
    fun `test that if secondary folder handle is invalid and deleted and default handle is valid, that a new secondary folder gets setup`() =
        runTest {
            whenever(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)).thenReturn(
                invalidHandle
            )
            whenever(isNodeInRubbishOrDeletedUseCase(any())).thenReturn(true)
            whenever(getDefaultNodeHandleUseCase(folderName)).thenReturn(validHandle)
            underTest(folderName)
            verify(setupSecondaryFolderUseCase, times(1)).invoke(validHandle)
        }
}
