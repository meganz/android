package mega.privacy.android.app.presentation.offline.offlinefileinfocompose

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineFileInfoComposeViewModelTest {

    private lateinit var underTest: OfflineFileInfoComposeViewModel
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getOfflineFileInformationByIdUseCase = mock<GetOfflineFileInformationByIdUseCase>()
    private val removeOfflineNodeUseCase = mock<RemoveOfflineNodeUseCase>()

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeEach
    fun initStubCommon() {
        runBlocking {
            stubCommon()
        }
    }

    private fun stubCommon() {
        val offlineFile = File(temporaryFolder, "OfflineFile.jpg")
        offlineFile.createNewFile()
        whenever(savedStateHandle.get<Long>(OfflineFileInfoComposeViewModel.NODE_HANDLE)) doReturn (1)
    }

    private fun initUnderTest() {
        underTest = OfflineFileInfoComposeViewModel(
            savedStateHandle = savedStateHandle,
            getOfflineFileInformationByIdUseCase = getOfflineFileInformationByIdUseCase,
            removeOfflineNodeUseCase = removeOfflineNodeUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        initUnderTest()
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.offlineFileInformation).isNull()
            assertThat(initial.isLoading).isTrue()
        }
    }

    @Test
    fun `test that removeOfflineNodeUseCase is invoked when removeFromOffline is called`() =
        runTest {
            initUnderTest()
            underTest.removeFromOffline()

            verify(removeOfflineNodeUseCase).invoke(NodeId(1))
        }

    @Test
    fun `test that error event is sent when node is null`() = runTest {
        whenever(getOfflineFileInformationByIdUseCase(NodeId(any()), any())) doReturn null

        initUnderTest()
        val event = underTest.uiState.value.errorEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        val content = (event as StateEventWithContentTriggered).content
        assertThat(content).isTrue()
    }

    @AfterEach
    fun resetMocks() {
        reset(
            savedStateHandle,
            getOfflineFileInformationByIdUseCase,
            removeOfflineNodeUseCase
        )
    }
}