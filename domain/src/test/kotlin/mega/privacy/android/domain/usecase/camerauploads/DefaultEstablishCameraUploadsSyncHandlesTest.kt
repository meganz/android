package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.ResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [EstablishCameraUploadsSyncHandles]
 */
@ExperimentalCoroutinesApi
class DefaultEstablishCameraUploadsSyncHandlesTest {
    private lateinit var underTest: EstablishCameraUploadsSyncHandles

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getCameraUploadsSyncHandles = mock<GetCameraUploadsSyncHandles>()
    private val isNodeInRubbishOrDeleted = mock<IsNodeInRubbishOrDeleted>()
    private val resetCameraUploadTimelines = mock<ResetCameraUploadTimelines>()
    private val setPrimarySyncHandle = mock<SetPrimarySyncHandle>()
    private val setSecondarySyncHandle = mock<SetSecondarySyncHandle>()

    @Before
    fun setUp() {
        underTest = DefaultEstablishCameraUploadsSyncHandles(
            cameraUploadRepository = cameraUploadRepository,
            getCameraUploadsSyncHandles = getCameraUploadsSyncHandles,
            isNodeInRubbishOrDeleted = isNodeInRubbishOrDeleted,
            resetCameraUploadTimelines = resetCameraUploadTimelines,
            setPrimarySyncHandle = setPrimarySyncHandle,
            setSecondarySyncHandle = setSecondarySyncHandle,
        )
    }

    @Test
    fun `test that both sync handles are set when successfully retrieved from the api`() = runTest {
        val testPair = Pair(10L, 20L)

        whenever(getCameraUploadsSyncHandles()).thenReturn(testPair)
        whenever(isNodeInRubbishOrDeleted(testPair.first)).thenReturn(false)
        whenever(isNodeInRubbishOrDeleted(testPair.second)).thenReturn(false)

        underTest()

        verify(resetCameraUploadTimelines).invoke(
            handleInAttribute = testPair.first,
            isSecondary = false,
        )
        verify(resetCameraUploadTimelines).invoke(
            handleInAttribute = testPair.second,
            isSecondary = true,
        )
        verify(setPrimarySyncHandle).invoke(testPair.first)
        verify(setSecondarySyncHandle).invoke(testPair.second)
    }

    @Test
    fun `test that only the primary sync handle is set when successfully retrieved from the api`() =
        runTest {
            val testPair = Pair(10L, 20L)

            whenever(getCameraUploadsSyncHandles()).thenReturn(testPair)
            whenever(isNodeInRubbishOrDeleted(testPair.first)).thenReturn(false)
            whenever(isNodeInRubbishOrDeleted(testPair.second)).thenReturn(true)

            underTest()

            verify(resetCameraUploadTimelines).invoke(
                handleInAttribute = testPair.first,
                isSecondary = false,
            )
            verify(setPrimarySyncHandle).invoke(testPair.first)
        }

    @Test
    fun `test that only the secondary sync handle is set when successfully retrieved from the api`() =
        runTest {
            val testPair = Pair(10L, 20L)

            whenever(getCameraUploadsSyncHandles()).thenReturn(testPair)
            whenever(isNodeInRubbishOrDeleted(testPair.first)).thenReturn(true)
            whenever(isNodeInRubbishOrDeleted(testPair.second)).thenReturn(false)

            underTest()

            verify(resetCameraUploadTimelines).invoke(
                handleInAttribute = testPair.second,
                isSecondary = true,
            )
            verify(setSecondarySyncHandle).invoke(testPair.second)
        }

    @Test
    fun `test that no sync handles are set when successfully retrieved from the api`() = runTest {
        val testPair = Pair(10L, 20L)

        whenever(getCameraUploadsSyncHandles()).thenReturn(testPair)
        whenever(isNodeInRubbishOrDeleted(testPair.first)).thenReturn(true)
        whenever(isNodeInRubbishOrDeleted(testPair.second)).thenReturn(true)

        underTest()

        verifyNoInteractions(resetCameraUploadTimelines)
        verifyNoInteractions(setPrimarySyncHandle)
        verifyNoInteractions(setSecondarySyncHandle)
    }

    @Test
    fun `test that invalid sync handles are set when the api could not retrieve the sync handles`() =
        runTest {
            whenever(getCameraUploadsSyncHandles()).thenReturn(null)

            underTest()

            verify(setPrimarySyncHandle).invoke(cameraUploadRepository.getInvalidHandle())
            verify(setSecondarySyncHandle).invoke(cameraUploadRepository.getInvalidHandle())
        }
}