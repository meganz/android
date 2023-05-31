package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import mega.privacy.android.domain.usecase.ResetCameraUploadTimelines
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [EstablishCameraUploadsSyncHandlesUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EstablishCameraUploadsSyncHandlesUseCaseTest {

    private lateinit var underTest: EstablishCameraUploadsSyncHandlesUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getCameraUploadsSyncHandlesUseCase = mock<GetCameraUploadsSyncHandlesUseCase>()
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()
    private val resetCameraUploadTimelines = mock<ResetCameraUploadTimelines>()
    private val setPrimarySyncHandle = mock<SetPrimarySyncHandle>()
    private val setSecondarySyncHandle = mock<SetSecondarySyncHandle>()

    @BeforeAll
    fun setUp() {
        underTest = EstablishCameraUploadsSyncHandlesUseCase(
            cameraUploadRepository = cameraUploadRepository,
            getCameraUploadsSyncHandlesUseCase = getCameraUploadsSyncHandlesUseCase,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
            resetCameraUploadTimelines = resetCameraUploadTimelines,
            setPrimarySyncHandle = setPrimarySyncHandle,
            setSecondarySyncHandle = setSecondarySyncHandle,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            getCameraUploadsSyncHandlesUseCase,
            isNodeInRubbishOrDeletedUseCase,
            resetCameraUploadTimelines,
            setPrimarySyncHandle,
            setSecondarySyncHandle,
        )
    }

    @ParameterizedTest(name = "primary deleted: {0}, secondary deleted: {1}")
    @MethodSource("provideParameters")
    fun `test that the sync handles are set on different conditions`(
        primaryHandleDeleted: Boolean,
        secondaryHandleDeleted: Boolean,
    ) = runTest {
        val testPair = Pair(10L, 20L)

        whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(testPair)
        whenever(isNodeInRubbishOrDeletedUseCase(testPair.first)).thenReturn(primaryHandleDeleted)
        whenever(isNodeInRubbishOrDeletedUseCase(testPair.second)).thenReturn(secondaryHandleDeleted)

        underTest()

        if (!primaryHandleDeleted) {
            verify(resetCameraUploadTimelines).invoke(
                handleInAttribute = testPair.first,
                isSecondary = false,
            )
            verify(setPrimarySyncHandle).invoke(testPair.first)
        }
        if (!secondaryHandleDeleted) {
            verify(resetCameraUploadTimelines).invoke(
                handleInAttribute = testPair.second,
                isSecondary = true,
            )
            verify(setSecondarySyncHandle).invoke(testPair.second)
        }
    }

    @Test
    fun `test that invalid sync handles are set when the api could not retrieve the sync handles`() =
        runTest {
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)

            underTest()

            verify(setPrimarySyncHandle).invoke(cameraUploadRepository.getInvalidHandle())
            verify(setSecondarySyncHandle).invoke(cameraUploadRepository.getInvalidHandle())
        }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments>? {
            return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false),
            )
        }
    }
}