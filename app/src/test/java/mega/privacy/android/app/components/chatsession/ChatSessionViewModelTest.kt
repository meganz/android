package mega.privacy.android.app.components.chatsession

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.CheckChatSessionUseCase
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatSessionViewModelTest {
    private lateinit var underTest: ChatSessionViewModel

    private val checkChatSessionUseCase: CheckChatSessionUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock {
        onBlocking { invoke(any()) }.thenReturn(false)
    }

    @BeforeAll
    fun setUp() {
        underTest = ChatSessionViewModel(
            checkChatSessionUseCase = checkChatSessionUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkChatSessionUseCase,
            getFeatureFlagValueUseCase,
        )
    }

    @Test
    fun `test that checkChatSession should call checkChatSessionUseCase`() =
        runTest {
            underTest.checkChatSession()

            verify(checkChatSessionUseCase).invoke()
        }


    @Test
    fun `test that successful checkChatSession updates sessionState to Valid`() = runTest {
        underTest.checkChatSession()
        underTest.state.test {
            assertThat(awaitItem().sessionState).isEqualTo(ChatSessionState.Valid)
        }
    }

    @Test
    fun `test that failed checkChatSession updates sessionState to Invalid`() = runTest {
        whenever(checkChatSessionUseCase()).thenAnswer {
            throw Exception("Call failed")
        }

        underTest.checkChatSession()

        underTest.state.test {
            assertThat(awaitItem().sessionState).isEqualTo(ChatSessionState.Invalid)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that optimistic parameter sets initial state correctly`(
        optimistic: Boolean,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher()) //we need async execution for this test
        setUp() // ensure initial status

        whenever(checkChatSessionUseCase()).thenAnswer {
            throw Exception("Call failed")
        }

        assertThat(underTest.state.value.sessionState).isEqualTo(ChatSessionState.Pending)
        underTest.checkChatSession(optimistic)

        underTest.state.test {
            if (optimistic) {
                assertThat(awaitItem().sessionState).isEqualTo(ChatSessionState.Valid)
            } else {
                assertThat(awaitItem().sessionState).isEqualTo(ChatSessionState.Pending)
            }
            assertThat(awaitItem().sessionState).isEqualTo(ChatSessionState.Invalid)
        }

        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that init block fetches single activity feature flag and updates state correctly`(
        isEnabled: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(isEnabled)

        val viewModel = ChatSessionViewModel(
            checkChatSessionUseCase = checkChatSessionUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase
        )

        viewModel.state.test {
            assertThat(awaitItem().isSingleActivityEnabled).isEqualTo(isEnabled)
        }

        verify(getFeatureFlagValueUseCase).invoke(AppFeatures.SingleActivity)
    }

}