package mega.privacy.android.app.components.session

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.RetryConnectionsAndSignalPresenceUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SessionViewModelTest {
    private lateinit var underTest: SessionViewModel

    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val retryConnectionsAndSignalPresenceUseCase: RetryConnectionsAndSignalPresenceUseCase =
        mock()

    @BeforeAll
    fun setUp() {
        underTest = SessionViewModel(
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            retryConnectionsAndSignalPresenceUseCase = retryConnectionsAndSignalPresenceUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            rootNodeExistsUseCase,
            retryConnectionsAndSignalPresenceUseCase
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that checkSdkSession updates state correctly`(
        rootNodeExists: Boolean,
    ) = runTest {
        whenever(rootNodeExistsUseCase()).thenReturn(rootNodeExists)
        underTest.checkSdkSession()
        underTest.state.test {
            assertThat(awaitItem().isRootNodeExists).isEqualTo(rootNodeExists)
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

        whenever(rootNodeExistsUseCase()).thenReturn(false)

        assertThat(underTest.state.value.isRootNodeExists).isNull()
        underTest.checkSdkSession(optimistic)

        underTest.state.test {
            if (optimistic) {
                assertThat(awaitItem().isRootNodeExists).isEqualTo(true)
            } else {
                assertThat(awaitItem().isRootNodeExists).isNull()
            }
            assertThat(awaitItem().isRootNodeExists).isEqualTo(false)
        }

        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that retry connections and signal presence is called`() = runTest {
        underTest.retryConnectionsAndSignalPresence()

        verify(retryConnectionsAndSignalPresenceUseCase).invoke()
    }
}