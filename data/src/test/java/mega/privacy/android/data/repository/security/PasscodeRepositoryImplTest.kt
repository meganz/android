package mega.privacy.android.data.repository.security

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.security.PasscodeStoreGateway
import mega.privacy.android.data.mapper.security.PasscodeTimeoutMapper
import mega.privacy.android.data.mapper.security.PasscodeTypeMapper
import mega.privacy.android.data.mapper.security.PasscodeTypeStringMapper
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PasscodeRepositoryImplTest {
    private lateinit var underTest: PasscodeRepository

    private val passcodeStoreGateway = mock<PasscodeStoreGateway>()
    private val passcodeTimeoutMapper = mock<PasscodeTimeoutMapper>()
    private val passcodeTypeMapper = mock<PasscodeTypeMapper>()
    private val passcodeTypeStringMapper = mock<PasscodeTypeStringMapper>()
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = UnconfinedTestDispatcher(scheduler)
    private val lockStateFlow = MutableSharedFlow<Boolean?>()

    @BeforeEach
    internal fun setUp() {
        whenever(passcodeStoreGateway.monitorLockState()).thenReturn(lockStateFlow)
        underTest = PasscodeRepositoryImpl(
            ioDispatcher = dispatcher,
            passcodeStoreGateway = passcodeStoreGateway,
            passcodeTimeoutMapper = passcodeTimeoutMapper,
            passcodeTypeMapper = passcodeTypeMapper,
            passcodeTypeStringMapper = passcodeTypeStringMapper,
            applicationScope = CoroutineScope(dispatcher)
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

    @Test
    internal fun `test that monitorFailedAttempts returns the value from the store`() =
        runTest(dispatcher) {
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
        runTest(dispatcher) {
            val expected = 8
            underTest.setFailedAttempts(expected)

            verifyBlocking(passcodeStoreGateway) { setFailedAttempts(expected) }
        }

    @Test
    internal fun `test that setPasscode sets the value on the store`() =
        runTest(dispatcher) {
            val expected = "new passcode"
            underTest.setPasscode(expected)

            verifyBlocking(passcodeStoreGateway) { setPasscode(expected) }
        }

    @Test
    internal fun `test that getPasscode returns the value from the store`() =
        runTest(dispatcher) {
            val expected = "a passcode"
            passcodeStoreGateway.stub {
                onBlocking { getPasscode() }.thenReturn(expected)
            }
            val actual = underTest.getPasscode()

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    internal fun `test that setLocked sets the value on the store`() = runTest(dispatcher) {
        val expected = true
        underTest.setLocked(expected)

        verifyBlocking(passcodeStoreGateway) { setLockedState(expected) }
    }

    @Test
    internal fun `test that monitorLockState returns the value from the store`() =
        runTest(dispatcher) {
            lockStateFlow.emit(false)
            underTest.monitorLockState().test {
                assertThat(awaitItem()).isEqualTo(false)
            }
            lockStateFlow.emit(true)
            underTest.monitorLockState().test {
                assertThat(awaitItem()).isEqualTo(true)
            }
        }

    @Test
    internal fun `test that set last paused time sets the value on the store`() =
        runTest(dispatcher) {
            val expected = 54L
            underTest.setLastPausedTime(expected)

            verifyBlocking(passcodeStoreGateway) { setLastBackgroundTime(expected) }
        }

    @Test
    internal fun `test that get last paused time returns the value from the store`() =
        runTest(dispatcher) {
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
        runTest(dispatcher) {
            val expected = true
            underTest.setPasscodeEnabled(expected)

            verifyBlocking(passcodeStoreGateway) { setPasscodeEnabledState(expected) }
        }

    @Test
    internal fun `test that monitor passcode enabled returns the value from the store`() =
        runTest(dispatcher) {
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
        runTest(dispatcher) {
            val expected = 123L
            underTest.setPasscodeTimeOut(PasscodeTimeout.TimeSpan(expected))

            verifyBlocking(passcodeStoreGateway) { setPasscodeTimeout(expected) }
        }

    @Test
    internal fun `test that set passcode timeout with null sets the value on the store to null`() =
        runTest(dispatcher) {
            underTest.setPasscodeTimeOut(null)

            verifyBlocking(passcodeStoreGateway) { setPasscodeTimeout(null) }
        }

    @Test
    internal fun `test that monitor passcode timeout returns the value from the mapper`() =
        runTest(dispatcher) {
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
        runTest(dispatcher) {
            val expected = "expected"
            passcodeTypeStringMapper.stub {
                on { invoke(any()) }.thenReturn(expected)
            }

            underTest.setPasscodeType(PasscodeType.Password)

            verifyBlocking(passcodeStoreGateway) { setPasscodeType(expected) }
        }

    @Test
    internal fun `test that set passcode type sets use biometric to true on the store if the type is biometric`() =
        runTest(dispatcher) {
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
        runTest(dispatcher) {
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
        runTest(dispatcher) {
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
    internal fun `test that biometric value is passed to mapper when determining type`() =
        runTest(dispatcher) {
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
    internal fun `test that null passcode type sets biometrics to false`() = runTest(dispatcher) {
        underTest.setPasscodeType(null)

        verify(passcodeStoreGateway).setBiometricsEnabled(false)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that the configuration change status is successfully set`(
        isConfigurationChanged: Boolean,
    ) = runTest {
        underTest.setConfigurationChangedStatus(isConfigurationChanged)

        verify(passcodeStoreGateway).setConfigurationChangedStatus(isConfigurationChanged = isConfigurationChanged)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that the correct configuration change status is emitted`(
        isConfigurationChanged: Boolean,
    ) = runTest {
        whenever(passcodeStoreGateway.monitorConfigurationChangedStatus()) doReturn flowOf(
            isConfigurationChanged
        )

        underTest.monitorConfigurationChangedStatus().test {
            assertThat(expectMostRecentItem()).isEqualTo(isConfigurationChanged)
        }
    }
}
