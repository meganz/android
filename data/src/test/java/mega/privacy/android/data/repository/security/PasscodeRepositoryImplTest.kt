package mega.privacy.android.data.repository.security

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import mega.privacy.android.data.mapper.security.PasscodeTimeoutMapper
import mega.privacy.android.data.mapper.security.PasscodeTypeMapper
import mega.privacy.android.data.mapper.security.PasscodeTypeStringMapper
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeRepositoryImplTest {
    private lateinit var underTest: PasscodeRepository

    private val passcodeStoreGateway = mock<PasscodeStoreGateway>()
    private val passcodeTimeoutMapper = mock<PasscodeTimeoutMapper>()
    private val passcodeTypeMapper = mock<PasscodeTypeMapper>()
    private val passcodeTypeStringMapper = mock<PasscodeTypeStringMapper>()

    @BeforeAll
    internal fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    internal fun setUp() {
        underTest = PasscodeRepositoryImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            passcodeStoreGateway = passcodeStoreGateway,
            passcodeTimeoutMapper = passcodeTimeoutMapper,
            passcodeTypeMapper = passcodeTypeMapper,
            passcodeTypeStringMapper = passcodeTypeStringMapper,
        )
    }

    @AfterEach
    internal fun cleanUp() {
        Mockito.reset(
            passcodeStoreGateway,
            passcodeTimeoutMapper,
            passcodeTypeMapper,
            passcodeTypeStringMapper,
        )

        Mockito.clearInvocations(
            passcodeStoreGateway,
            passcodeTimeoutMapper,
            passcodeTypeMapper,
            passcodeTypeStringMapper,
        )
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
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

    @Test
    internal fun `test that set last paused time sets the value on the store`() =
        runTest {
            val expected = 54L
            underTest.setLastPausedTime(expected)

            verifyBlocking(passcodeStoreGateway) { setLastBackgroundTime(expected) }
        }

    @Test
    internal fun `test that get last paused time returns the value from the store`() =
        runTest {
            val expected = 34L
            passcodeStoreGateway.stub {
                on { monitorLastBackgroundTime() }.thenReturn(
                    flow {
                        emit(expected)
                        awaitCancellation()
                    }
                )
            }
            val actual = underTest.getLastPausedTime()

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    internal fun `test that set passcode enabled sets the value on the store`() =
        runTest {
            val expected = true
            underTest.setPasscodeEnabled(expected)

            verifyBlocking(passcodeStoreGateway) { setPasscodeEnabledState(expected) }
        }

    @Test
    internal fun `test that monitor passcode enabled returns the value from the store`() =
        runTest {
            val expected = listOf(false, true, null, false)
            passcodeStoreGateway.stub {
                on { monitorPasscodeEnabledState() }.thenReturn(
                    expected.asFlow()
                )
            }
            underTest.monitorIsPasscodeEnabled().test {
                val actual = cancelAndConsumeRemainingEvents()
                    .filterIsInstance<Event.Item<Boolean?>>()
                    .map { it.value }
                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    internal fun `test that set passcode timeout with time span sets the value on the store to the millisecond value`() =
        runTest {
            val expected = 123L
            underTest.setPasscodeTimeOut(PasscodeTimeout.TimeSpan(expected))

            verifyBlocking(passcodeStoreGateway) { setPasscodeTimeout(expected) }
        }

    @Test
    internal fun `test that set passcode timeout with immediate sets the value on the store to 0`() =
        runTest {
            val expected = 0L
            underTest.setPasscodeTimeOut(PasscodeTimeout.Immediate)

            verifyBlocking(passcodeStoreGateway) { setPasscodeTimeout(expected) }
        }

    @Test
    internal fun `test that monitor passcode timeout returns the value from the mapper`() =
        runTest {
            val values = listOf(null, 1L, 2L, 3L, 4L)
            val expected = PasscodeTimeout.Immediate
            passcodeTimeoutMapper.stub {
                on { invoke(anyOrNull()) }.thenReturn(expected)
            }
            passcodeStoreGateway.stub {
                on { monitorPasscodeTimeOut() }.thenReturn(
                    values.asFlow()
                )
            }

            underTest.monitorPasscodeTimeOut().test {
                val actual = cancelAndConsumeRemainingEvents()
                    .filterIsInstance<Event.Item<PasscodeTimeout>>()
                    .map { it.value }
                actual.forEach {
                    assertThat(it).isEqualTo(expected)
                }
            }
        }

    @Test
    internal fun `test that set passcode type sets the value on the store to the value returned from the mapper`() =
        runTest {
            val expected = "expected"
            passcodeTypeStringMapper.stub {
                on { invoke(any()) }.thenReturn(expected)
            }

            underTest.setPasscodeType(PasscodeType.Password)

            verifyBlocking(passcodeStoreGateway) { setPasscodeType(expected) }
        }

    @Test
    internal fun `test that set passcode type sets use biometric to true on the store if the type is biometric`() =
        runTest {
            val expected = "expected"
            passcodeTypeStringMapper.stub {
                on { invoke(any()) }.thenReturn(expected)
            }

            underTest.setPasscodeType(PasscodeType.Biometric(PasscodeType.Pin(4)))

            verifyBlocking(passcodeStoreGateway) { setBiometricsEnabled(true) }
            verifyBlocking(passcodeStoreGateway) { setPasscodeType(expected) }
        }

    @Test
    internal fun `test that set passcode type sets use biometric to false on the store if the type is not biometric`() =
        runTest {
            val expected = "expected"
            passcodeTypeStringMapper.stub {
                on { invoke(any()) }.thenReturn(expected)
            }

            underTest.setPasscodeType(PasscodeType.Pin(4))

            verifyBlocking(passcodeStoreGateway) { setBiometricsEnabled(false) }
            verifyBlocking(passcodeStoreGateway) { setPasscodeType(expected) }
        }

    @Test
    internal fun `test that monitor passcode type returns the value from the mapper`() =
        runTest {
            val values = listOf(null, "4", "6", "alphanumeric")
            val expected = PasscodeTimeout.Immediate
            passcodeTypeMapper.stub {
                on { invoke(anyOrNull(), any()) }.thenReturn(PasscodeType.Password)
            }

            passcodeStoreGateway.stub {
                on { monitorPasscodeType() }.thenReturn(
                    values.asFlow()
                )
            }

            underTest.monitorPasscodeType().test {
                val actual = cancelAndConsumeRemainingEvents()
                    .filterIsInstance<Event.Item<PasscodeType>>()
                    .map { it.value }
                actual.forEach {
                    assertThat(it).isEqualTo(expected)
                }
            }
        }

    @Test
    internal fun `test that biometric value is passed to mapper when determining type`() = runTest {
        val value = "6"

        passcodeTypeMapper.stub {
            on { invoke(value, true) }.thenReturn(PasscodeType.Pin(6))
        }

        passcodeStoreGateway.stub {
            on { monitorPasscodeType() }.thenReturn(
                flow {
                    emit(value)
                    awaitCancellation()
                }
            )
            on { monitorBiometricEnabledState() }.thenReturn(
                flow {
                    emit(true)
                    awaitCancellation()
                }
            )
        }

        underTest.monitorPasscodeType().test {
            awaitItem()
        }

        verify(passcodeTypeMapper).invoke(value, true)
    }

    @Test
    internal fun `test that null passcode type sets biometrics to false`() = runTest {
        underTest.setPasscodeType(null)

        verify(passcodeStoreGateway).setBiometricsEnabled(false)
    }
}