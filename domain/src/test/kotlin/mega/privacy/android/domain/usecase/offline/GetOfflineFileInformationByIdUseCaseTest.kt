package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfflineFileInformationByIdUseCaseTest {
    private lateinit var underTest: GetOfflineFileInformationByIdUseCase

    private val getOfflineNodeInformationByNodeIdUseCase: GetOfflineNodeInformationByNodeIdUseCase = mock()
    private val getOfflineFileInformationUseCase: GetOfflineFileInformationUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetOfflineFileInformationByIdUseCase(
            getOfflineNodeInformationByNodeIdUseCase,
            getOfflineFileInformationUseCase
        )
    }

    @Test
    fun `test that invoke with valid NodeId should call both use cases`() = runTest {
        val nodeId = NodeId(11)
        val offlineNodeInformation = mock<OtherOfflineNodeInformation>()
        whenever(getOfflineNodeInformationByNodeIdUseCase.invoke(nodeId)) doReturn offlineNodeInformation

        underTest.invoke(nodeId)

        verify(getOfflineNodeInformationByNodeIdUseCase).invoke(nodeId)
        verify(getOfflineFileInformationUseCase).invoke(offlineNodeInformation)
    }

    @Test
    fun `test that getOfflineFileInformationUseCase is not called when GetOfflineNodeInformationByIdUseCase returns null`() =
        runTest {
            val nodeId = NodeId(11)
            whenever(getOfflineNodeInformationByNodeIdUseCase.invoke(nodeId)) doReturn null

            underTest.invoke(nodeId)

            verifyNoInteractions(getOfflineFileInformationUseCase)
        }

    @AfterEach
    fun reset() {
        reset(
            getOfflineNodeInformationByNodeIdUseCase,
            getOfflineFileInformationUseCase
        )
    }
}