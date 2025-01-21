package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
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
    private lateinit var getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase

    @BeforeAll
    fun setUp() {
        nodeRepository = mock()
        getGPSCoordinatesUseCase = mock()
        underTest = SetNodeCoordinatesUseCase(
            nodeRepository = nodeRepository,
            getGPSCoordinatesUseCase = getGPSCoordinatesUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository, getGPSCoordinatesUseCase)
    }

    @ParameterizedTest(name = " is invoked = {0} when file has coordinates is {1} and has geolocation app data is {2}")
    @MethodSource("provideParameters")
    fun `test that setNodeCoordinates`(
        isInvoked: Boolean,
        hasCoordinates: Boolean,
        hasAppData: Boolean,
    ) = runTest {
        val uriPath = UriPath("path")
        val nodeHandle = 1L
        val coordinates = Pair(123.0, 6345.0)
        val appData = if (hasAppData) {
            TransferAppData.Geolocation(coordinates.first, coordinates.second)
        } else null
        whenever(getGPSCoordinatesUseCase.invoke(uriPath, null))
            .thenReturn(if (hasCoordinates) coordinates else null)
        underTest.invoke(uriPath, nodeHandle, appData)

        if (isInvoked) {
            verify(nodeRepository).setNodeCoordinates(
                    NodeId(nodeHandle),
                    coordinates.first,
                    coordinates.second,
                )
        } else {
            verifyNoInteractions(nodeRepository)
        }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, false, false),
        Arguments.of(true, true, false),
        Arguments.of(true, false, true),
    )
}
