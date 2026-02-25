package mega.privacy.android.app.presentation.psa

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.psa.mapper.PsaStateMapper
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
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
    private val dismissPsaUseCase = mock<DismissPsaUseCase>()
    private val psaStateMapper = mock<PsaStateMapper>()

    @BeforeEach
    fun setUp() {
        reset(
            monitorPsaUseCase,
            dismissPsaUseCase,
            psaStateMapper,
        )

        monitorPsaUseCase.stub {
            on { invoke() }.thenReturn(flow { awaitCancellation() })
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

    }

    private fun initViewModel() {
        underTest = PsaViewModel(
            monitorPsaUseCase = monitorPsaUseCase,
            dismissPsaUseCase = dismissPsaUseCase,
            psaStateMapper = psaStateMapper,
            setDisplayedPsaUseCase = mock(),
        )
    }

    @Test
    fun `test that mapped values are returned`() = runTest {
        val expectedCount = 5
        val monitorFlow = MutableStateFlow(createPsa(99))
        monitorPsaUseCase.stub {
            onBlocking { invoke() }.thenReturn(monitorFlow)
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