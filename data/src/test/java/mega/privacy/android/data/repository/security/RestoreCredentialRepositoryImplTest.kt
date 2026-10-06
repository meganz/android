package mega.privacy.android.data.repository.security

import androidx.credentials.exceptions.ClearCredentialUnknownException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.SyntaxError
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.RestoreCredentialGateway
import mega.privacy.android.domain.entity.login.CreatedRestoreCredential
import mega.privacy.android.domain.exception.login.RestoreCredentialException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestoreCredentialRepositoryImplTest {
    private lateinit var underTest: RestoreCredentialRepositoryImpl

    private val restoreCredentialGateway = mock<RestoreCredentialGateway>()

    private val requestJson = """{"request":true}"""
    private val responseJson = """{"response":true}"""

    @BeforeAll
    fun setUp() {
        underTest = RestoreCredentialRepositoryImpl(
            restoreCredentialGateway = restoreCredentialGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(restoreCredentialGateway)
    }

    @Test
    fun `test that createRestoreCredential returns a cloud backed credential when cloud backup succeeds`() =
        runTest {
            whenever(
                restoreCredentialGateway.createRestoreCredential(
                    requestJson = requestJson,
                    isCloudBackupEnabled = true,
                )
            ).thenReturn(responseJson)

            val actual = underTest.createRestoreCredential(requestJson)

            assertThat(actual).isEqualTo(
                CreatedRestoreCredential(responseJson = responseJson, isBackedUpToCloud = true)
            )
        }

    @Test
    fun `test that createRestoreCredential retries on the device only when end to end encryption is unavailable`() =
        runTest {
            whenever(
                restoreCredentialGateway.createRestoreCredential(
                    requestJson = requestJson,
                    isCloudBackupEnabled = true,
                )
            ).thenAnswer { throw E2eeUnavailableException("Backup is off") }
            whenever(
                restoreCredentialGateway.createRestoreCredential(
                    requestJson = requestJson,
                    isCloudBackupEnabled = false,
                )
            ).thenReturn(responseJson)

            val actual = underTest.createRestoreCredential(requestJson)

            assertThat(actual).isEqualTo(
                CreatedRestoreCredential(responseJson = responseJson, isBackedUpToCloud = false)
            )
        }

    @Test
    fun `test that createRestoreCredential throws Failed when the device only retry also fails`() =
        runTest {
            whenever(restoreCredentialGateway.createRestoreCredential(any(), any()))
                .thenAnswer { throw E2eeUnavailableException("Backup is off") }

            assertThrows<RestoreCredentialException.Failed> {
                underTest.createRestoreCredential(requestJson)
            }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateExceptions")
    fun `test that createRestoreCredential maps the platform exception to the domain exception`(
        platformException: Throwable,
        expected: Class<out RestoreCredentialException>,
    ) = runTest {
        whenever(restoreCredentialGateway.createRestoreCredential(any(), any()))
            .thenAnswer { throw platformException }

        val actual = assertThrows<RestoreCredentialException> {
            underTest.createRestoreCredential(requestJson)
        }

        assertThat(actual).isInstanceOf(expected)
        assertThat(actual.cause).isSameInstanceAs(platformException)
    }

    private fun provideCreateExceptions(): Stream<Arguments> = Stream.of(
        Arguments.of(
            CreateRestoreCredentialDomException(SyntaxError(), "Invalid JSON"),
            RestoreCredentialException.InvalidRequest::class.java,
        ),
        Arguments.of(
            IllegalArgumentException("user.id must be defined in requestJson"),
            RestoreCredentialException.InvalidRequest::class.java,
        ),
        Arguments.of(
            CreateCredentialUnknownException("Unknown"),
            RestoreCredentialException.Failed::class.java,
        ),
    )

    @Test
    fun `test that getRestoreCredential returns the authentication response from the gateway`() =
        runTest {
            whenever(restoreCredentialGateway.getRestoreCredential(requestJson))
                .thenReturn(responseJson)

            assertThat(underTest.getRestoreCredential(requestJson)).isEqualTo(responseJson)
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetExceptions")
    fun `test that getRestoreCredential maps the platform exception to the domain exception`(
        platformException: Throwable,
        expected: Class<out RestoreCredentialException>,
    ) = runTest {
        whenever(restoreCredentialGateway.getRestoreCredential(any()))
            .thenAnswer { throw platformException }

        val actual = assertThrows<RestoreCredentialException> {
            underTest.getRestoreCredential(requestJson)
        }

        assertThat(actual).isInstanceOf(expected)
        assertThat(actual.cause).isSameInstanceAs(platformException)
    }

    private fun provideGetExceptions(): Stream<Arguments> = Stream.of(
        Arguments.of(
            NoCredentialException("No restore credential"),
            RestoreCredentialException.NoCredential::class.java,
        ),
        Arguments.of(
            IllegalArgumentException("requestJson must be a valid JSON"),
            RestoreCredentialException.InvalidRequest::class.java,
        ),
        Arguments.of(
            GetCredentialUnknownException("Unknown"),
            RestoreCredentialException.Failed::class.java,
        ),
    )

    @Test
    fun `test that clearRestoreCredential clears the credential in the gateway`() = runTest {
        underTest.clearRestoreCredential()

        verify(restoreCredentialGateway).clearRestoreCredential()
    }

    @Test
    fun `test that clearRestoreCredential throws Failed when the gateway fails`() = runTest {
        val platformException = ClearCredentialUnknownException("Unknown")
        whenever(restoreCredentialGateway.clearRestoreCredential())
            .thenAnswer { throw platformException }

        val actual = assertThrows<RestoreCredentialException.Failed> {
            underTest.clearRestoreCredential()
        }

        assertThat(actual.cause).isSameInstanceAs(platformException)
    }
}
