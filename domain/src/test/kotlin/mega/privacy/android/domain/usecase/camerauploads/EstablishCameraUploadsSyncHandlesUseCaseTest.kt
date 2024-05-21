package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EstablishCameraUploadsSyncHandlesUseCaseTest {

    private lateinit var underTest: EstablishCameraUploadsSyncHandlesUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val getCameraUploadsSyncHandlesUseCase = mock<GetCameraUploadsSyncHandlesUseCase>()
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()
    private val setSetPrimaryNodeIdUseCase = mock<SetPrimaryNodeIdUseCase>()
    private val setSecondaryNodeIdUseCase = mock<SetSecondaryNodeIdUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = EstablishCameraUploadsSyncHandlesUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            getCameraUploadsSyncHandlesUseCase = getCameraUploadsSyncHandlesUseCase,
            isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
            setPrimaryNodeIdUseCase = setSetPrimaryNodeIdUseCase,
            setSecondaryNodeIdUseCase = setSecondaryNodeIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadsRepository,
            getCameraUploadsSyncHandlesUseCase,
            isNodeInRubbishOrDeletedUseCase,
            setSetPrimaryNodeIdUseCase,
            setSecondaryNodeIdUseCase,
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
            verify(setSetPrimaryNodeIdUseCase).invoke(NodeId(testPair.first))
        }
        if (!secondaryHandleDeleted) {
            verify(setSecondaryNodeIdUseCase).invoke(NodeId(testPair.second))
        }
    }

    @Test
    fun `test that invalid sync handles are set when the api could not retrieve the sync handles`() =
        runTest {
            whenever(getCameraUploadsSyncHandlesUseCase()).thenReturn(null)

            underTest()

            verify(setSetPrimaryNodeIdUseCase).invoke(NodeId(cameraUploadsRepository.getInvalidHandle()))
            verify(setSecondaryNodeIdUseCase).invoke(NodeId(cameraUploadsRepository.getInvalidHandle()))
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
