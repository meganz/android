package mega.privacy.android.domain.usecase.transfer.uploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.file.IsVideoFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetNodeCoordinatesUseCaseTest {

    private lateinit var underTest: SetNodeCoordinatesUseCase

    private lateinit var nodeRepository: NodeRepository
    private lateinit var isVideoFileUseCase: IsVideoFileUseCase
    private lateinit var isImageFileUseCase: IsImageFileUseCase
    private lateinit var getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase

    @BeforeAll
    fun setUp() {
        nodeRepository = mock()
        isVideoFileUseCase = mock()
        isImageFileUseCase = mock()
        getGPSCoordinatesUseCase = mock()
        underTest = SetNodeCoordinatesUseCase(
            nodeRepository = nodeRepository,
            isVideoFileUseCase = isVideoFileUseCase,
            isImageFileUseCase = isImageFileUseCase,
            getGPSCoordinatesUseCase = getGPSCoordinatesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository, isVideoFileUseCase, isImageFileUseCase, getGPSCoordinatesUseCase)
    }

    @ParameterizedTest(name = " is invoked = {0} when isVideoFile is {1} and isImageFile {2}")
    @MethodSource("provideParameters")
    fun `test that GetGPSCoordinatesUseCase`(
        isInvoked: Boolean,
        isVideoFile: Boolean,
        isImageFile: Boolean,
    ) = runTest {
        val path = "path"
        val nodeHandle = 1L
        val coordinates = mock<Pair<Float, Float>> {
            on { first }.thenReturn(123F)
            on { second }.thenReturn(6345F)
        }
        whenever(isVideoFileUseCase(path)).thenReturn(isVideoFile)
        whenever(isImageFileUseCase(path)).thenReturn(isImageFile)
        whenever(getGPSCoordinatesUseCase.invoke(path, isVideoFile)).thenReturn(coordinates)
        whenever(
            nodeRepository.setNodeCoordinates(
                NodeId(nodeHandle),
                coordinates.first.toDouble(),
                coordinates.second.toDouble()
            )
        ).thenReturn(Unit)
        underTest.invoke(path, nodeHandle)

        if (isInvoked) {
            verify(getGPSCoordinatesUseCase).invoke(path, isVideoFile)
        } else {
            verifyNoInteractions(getGPSCoordinatesUseCase)
        }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, false, false),
        Arguments.of(true, true, false),
        Arguments.of(true, false, true),
    )
}
