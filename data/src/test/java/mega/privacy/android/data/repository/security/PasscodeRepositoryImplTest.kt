package mega.privacy.android.data.repository.security

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
internal class PasscodeRepositoryImplTest {
    private lateinit var underTest: PasscodeRepository
    private val passcodeStoreGateway = mock<PasscodeStoreGateway>()

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            passcodeStoreGateway = passcodeStoreGateway,
        )
    }

    @Test
    internal fun `test that monitorFailedAttempts returns the value from the store`() = runTest {
        val expected = listOf(null, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        passcodeStoreGateway.stub {
            on { monitorFailedAttempts() }.thenReturn(expected.asFlow())
        }
        underTest.monitorFailedAttempts().test {
            val actual = cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<Int?>>()
                .map { it.value }

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    internal fun `test that setFailedAttempts sets the value on the store`() =
        runTest {
            val expected = 8
            underTest.setFailedAttempts(expected)

            verifyBlocking(passcodeStoreGateway) { setFailedAttempts(expected) }
        }

    @Test
    internal fun `test that setPasscode sets the value on the store`() =
        runTest {
            val expected = "new passcode"
            underTest.setPasscode(expected)

            verifyBlocking(passcodeStoreGateway) { setPasscode(expected) }
        }

    @Test
    internal fun `test that getPasscode returns the value from the store`() =
        runTest {
            val expected = "a passcode"
            passcodeStoreGateway.stub {
                onBlocking { getPasscode() }.thenReturn(expected)
            }
            val actual = underTest.getPasscode()

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    internal fun `test that setLocked sets the value on the store`() = runTest {
        val expected = true
        underTest.setLocked(expected)

        verifyBlocking(passcodeStoreGateway) { setLockedState(expected) }
    }

    @Test
    internal fun `test that monitorLockState returns the value from the store`() =
        runTest {
            val expected = listOf(false, true, null, false)
            passcodeStoreGateway.stub {
                on { monitorLockState() }.thenReturn(expected.asFlow())
            }
            underTest.monitorLockState().test {
                val actual = cancelAndConsumeRemainingEvents()
                    .filterIsInstance<Event.Item<Boolean?>>()
                    .map { it.value }
                assertThat(actual).isEqualTo(expected)
            }
        }
}