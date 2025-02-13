package mega.privacy.android.app.components.session

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
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

    @Test
    fun `test that retry connections and signal presence is called`() = runTest {
        underTest.retryConnectionsAndSignalPresence()

        verify(retryConnectionsAndSignalPresenceUseCase).invoke()
    }
}