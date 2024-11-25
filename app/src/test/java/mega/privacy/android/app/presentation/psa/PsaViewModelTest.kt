package mega.privacy.android.app.presentation.psa

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.psa.FetchPsaUseCase
import mega.privacy.android.domain.usecase.psa.MonitorPsaUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PsaViewModelTest {
    private lateinit var underTest: PsaViewModel

    private val monitorPsaUseCase = mock<MonitorPsaUseCase>()
    private val fetchPsaUseCase = mock<FetchPsaUseCase>()
    private val dismissPsaUseCase = mock<DismissPsaUseCase>()
    private val psaStateMapper = mock<PsaStateMapper>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorPsaUseCase,
            fetchPsaUseCase,
            dismissPsaUseCase,
            psaStateMapper,
        )

        monitorPsaUseCase.stub {
            on { invoke(any()) }.thenReturn(flow { awaitCancellation() })
        }

        psaStateMapper.stub {
            on { invoke(any()) }.thenAnswer {
                (it.arguments[0] as? Psa)?.id?.let { psaId ->
                    createStandardPsaState(
                        psaId
                    )
                } ?: PsaState.NoPsa
            }
        }

        fetchPsaUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(null)
        }
    }


    private fun initViewModel() {
        underTest = PsaViewModel(
            monitorPsaUseCase = monitorPsaUseCase,
            fetchPsaUseCase = fetchPsaUseCase,
            dismissPsaUseCase = dismissPsaUseCase,
            psaStateMapper = psaStateMapper,
            currentTimeProvider = { 0 }
        )
    }

    @Test
    fun `test that mapped values are returned`() = runTest {
        val expectedCount = 5
        val monitorFlow = MutableStateFlow(createPsa(99))
        monitorPsaUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(monitorFlow)
        }

        initViewModel()

        underTest.state.test {
            awaitItem()
            repeat(expectedCount) {
                monitorFlow.value = createPsa(it)
            }
            val events = cancelAndConsumeRemainingEvents()
            events.forEach {
                println(it)
            }
            assertThat(events.size).isEqualTo(expectedCount)
        }
    }


    @Test
    fun `test that dismiss use case is called when psa is marked as seen`() {
        initViewModel()

        underTest.markAsSeen(1)

        verifyBlocking(dismissPsaUseCase) { invoke(1) }
    }

    @Test
    fun `test that latest psa is fetched if previous psa is dismissed`() {
        initViewModel()
        underTest.markAsSeen(1)

        verifyBlocking(fetchPsaUseCase) { invoke(any()) }
    }

    @Test
    fun `test that new psa is set if another is fetched after dismissing the previous psa`() =
        runTest {
            monitorPsaUseCase.stub {
                onBlocking { invoke(any()) }.thenReturn(createPsa(1).asHotFlow())
            }
            val expectedId = 2
            fetchPsaUseCase.stub {
                onBlocking { invoke(any()) }.thenReturn(createPsa(expectedId))
            }

            initViewModel()

            underTest.state.test {
                awaitItem()
                underTest.markAsSeen(1)
                val state = awaitItem()
                assertThat((state as PsaState.StandardPsa).id).isEqualTo(expectedId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createStandardPsaState(id: Int) =
        PsaState.StandardPsa(
            id = id,
            title = "title",
            text = "text",
            imageUrl = "imageUrl",
            positiveText = "positiveText",
            positiveLink = "positiveLink"
        )

    private fun createPsa(id: Int) = Psa(
        id = id,
        title = "title",
        text = "text",
        imageUrl = "imageUrl",
        positiveText = "positiveText",
        positiveLink = "positiveLink",
        url = null,
    )
}