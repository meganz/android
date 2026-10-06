package mega.privacy.android.data.facade

import android.content.Context
import android.os.Bundle
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RestoreCredentialFacadeTest {
    private lateinit var underTest: RestoreCredentialFacade
    private lateinit var context: Context

    private val credentialManager = mock<CredentialManager>()

    private val createRequestJson = """{"user":{"id":"dXNlcklk","name":"user"}}"""
    private val getRequestJson = """{"challenge":"Y2hhbGxlbmdl"}"""
    private val responseJson = """{"response":true}"""

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        underTest = RestoreCredentialFacade(
            context = context,
            credentialManager = credentialManager,
        )
    }

    @Test
    fun `test that createRestoreCredential sends a restore request with the cloud backup choice`() =
        runTest {
            whenever(credentialManager.createCredential(any(), any()))
                .thenReturn(CreateRestoreCredentialResponse(responseJson))

            val actual = underTest.createRestoreCredential(
                requestJson = createRequestJson,
                isCloudBackupEnabled = false,
            )

            assertThat(actual).isEqualTo(responseJson)
            argumentCaptor<CreateCredentialRequest> {
                verify(credentialManager).createCredential(eq(context), capture())
                val request = firstValue as CreateRestoreCredentialRequest
                assertThat(request.requestJson).isEqualTo(createRequestJson)
                assertThat(request.isCloudBackupEnabled).isFalse()
            }
        }

    @Test
    fun `test that getRestoreCredential sends only the restore option and returns the authentication response`() =
        runTest {
            val credential = mock<RestoreCredential> {
                on { authenticationResponseJson } doReturn responseJson
            }
            whenever(credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()))
                .thenReturn(GetCredentialResponse(credential))

            val actual = underTest.getRestoreCredential(getRequestJson)

            assertThat(actual).isEqualTo(responseJson)
            argumentCaptor<GetCredentialRequest> {
                verify(credentialManager).getCredential(eq(context), capture())
                val option = firstValue.credentialOptions.single() as GetRestoreCredentialOption
                assertThat(option.requestJson).isEqualTo(getRequestJson)
            }
        }

    @Test
    fun `test that getRestoreCredential throws when the credential is not a restore credential`() =
        runTest {
            whenever(credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()))
                .thenReturn(GetCredentialResponse(CustomCredential("other", Bundle())))

            val actual = runCatching { underTest.getRestoreCredential(getRequestJson) }

            assertThat(actual.exceptionOrNull()).isInstanceOf(GetCredentialUnknownException::class.java)
        }

    @Test
    fun `test that clearRestoreCredential clears only the restore credential`() = runTest {
        underTest.clearRestoreCredential()

        argumentCaptor<ClearCredentialStateRequest> {
            verify(credentialManager).clearCredentialState(capture())
            assertThat(firstValue.requestType)
                .isEqualTo(ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL)
        }
    }
}
