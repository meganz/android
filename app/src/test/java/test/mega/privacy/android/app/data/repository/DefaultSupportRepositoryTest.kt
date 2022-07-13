package test.mega.privacy.android.app.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.repository.DefaultSupportRepository
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.SupportRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
class DefaultSupportRepositoryTest {
    private lateinit var underTest: SupportRepository

    private val megaApiGateway = mock<MegaApiGateway>()

    @Before
    fun setUp() {
        underTest = DefaultSupportRepository(megaApi = megaApiGateway)
    }

    @Test
    fun `test submit ticket with a successful result`() = runTest {
        whenever(megaApiGateway.createSupportTicket(any(), any())).thenAnswer {
            (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                mock { on { errorCode }.thenReturn(MegaError.API_OK) }
            )
        }

        underTest.logTicket("Ticket content")

        verify(megaApiGateway).createSupportTicket(any(), any())
    }

    @Test(expected = MegaException::class)
    fun `test submit ticket with an error returns an exception`() = runTest {
        whenever(megaApiGateway.createSupportTicket(any(), any())).thenAnswer {
            (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                mock { on { errorCode }.thenReturn(MegaError.API_OK + 1) }
            )
        }

        underTest.logTicket("Ticket content")

        verify(megaApiGateway).createSupportTicket(any(), any())
    }

    @Test
    fun `test upload file successfully closes stream`() = runTest {
        whenever(megaApiGateway.startUploadForSupport(any(), any())).thenAnswer {
            (it.arguments[1] as OptionalMegaTransferListenerInterface).onTransferFinish(
                mock(),
                mock(),
                mock { on { errorCode }.thenReturn(MegaError.API_OK) }
            )
        }

        underTest.uploadFile(File("")).test {
            awaitComplete()
        }
    }

    @Test
    fun `test upload completing with an error returns the error`() = runTest {
        whenever(megaApiGateway.startUploadForSupport(any(), any())).thenAnswer {
            (it.arguments[1] as OptionalMegaTransferListenerInterface).onTransferFinish(
                mock(),
                mock(),
                mock { on { errorCode }.thenReturn(MegaError.API_OK + 1) }
            )
        }

        underTest.uploadFile(File("")).test {
            awaitError()
        }
    }

    @Test
    fun `test upload with temporary error does not return error`() = runTest {
        whenever(megaApiGateway.startUploadForSupport(any(), any())).thenAnswer {
            (it.arguments[1] as OptionalMegaTransferListenerInterface).onTransferTemporaryError(
                mock(),
                mock(),
                mock { on { errorCode }.thenReturn(MegaError.API_OK + 1) })
        }

        underTest.uploadFile(File("")).test(5) {}
    }

    @Test
    fun `test that log upload returns correct progress updates when notified on every ten percent`() =
        runTest {
            val listenerCaptor = argumentCaptor<OptionalMegaTransferListenerInterface>()

            underTest.uploadFile(File("")).test {
                verify(megaApiGateway).startUploadForSupport(
                    any(),
                    listenerCaptor.capture()
                )
                val listener = listenerCaptor.firstValue
                (0..10).forEach { i ->
                    listener.onTransferUpdate(
                        mock(),
                        mock {
                            on { transferredBytes }.thenReturn(i.toLong())
                            on { totalBytes }.thenReturn(10)
                        }
                    )
                    assertThat(awaitItem()).isEqualTo(i / 10f)
                }
            }
        }

    @Test
    fun `test that cancelling log upload cancels the transfer`() = runTest {
        val listenerCaptor = argumentCaptor<OptionalMegaTransferListenerInterface>()
        val transfer = mock<MegaTransfer>()
        underTest.uploadFile(File("")).test {
            verify(megaApiGateway).startUploadForSupport(any(), listenerCaptor.capture())
            val listener = listenerCaptor.firstValue
            listener.onTransferStart(mock(), transfer)
            cancelAndIgnoreRemainingEvents()
        }

        verify(megaApiGateway).cancelTransfer(transfer)
    }

}