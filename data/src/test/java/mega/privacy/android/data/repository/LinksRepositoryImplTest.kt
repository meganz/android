package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LinksRepositoryImplTest {
    private lateinit var underTest: LinksRepositoryImpl
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val megaApiGateway: MegaApiGateway = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest =
            LinksRepositoryImpl(
                megaApiGateway = megaApiGateway,
                ioDispatcher = UnconfinedTestDispatcher()
            )
    }

    @Test
    fun `test that decryptPasswordProtectedLink returns correctly when call decryptPasswordProtectedLink successfully`() =
        runTest {
            val password = "password"
            val decryptedLink = "https://mega.co.nz/abc"
            val encryptedLink = "https://mega.co.nz/abc/encrypted"
            val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
            val request = mock<MegaRequest> { on { text }.thenReturn(decryptedLink) }

            whenever(
                megaApiGateway.decryptPasswordProtectedLink(
                    eq(encryptedLink),
                    eq(password),
                    any()
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    request,
                    megaError
                )
            }

            assertThat(underTest.decryptPasswordProtectedLink(encryptedLink, password))
                .isEqualTo(decryptedLink)
        }

    @Test
    fun `test that decryptPasswordProtectedLink throw exception when call decryptPasswordProtectedLink returns failed`() =
        runTest {
            val megaError =
                mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }
            val password = "password"
            val encryptedLink = "https://mega.co.nz/abc/encrypted"

            whenever(
                megaApiGateway.decryptPasswordProtectedLink(
                    eq(encryptedLink),
                    eq(password),
                    any()
                )
            ).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError
                )
            }

            assertThrows<MegaException> {
                underTest.decryptPasswordProtectedLink(
                    encryptedLink,
                    password
                )
            }
        }
}

