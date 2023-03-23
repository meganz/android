package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.TransferEventMapper
import mega.privacy.android.data.mapper.TransferMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaTransfer
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [DefaultTransfersRepository]
 */
@ExperimentalCoroutinesApi
class DefaultTransfersRepositoryTest {
    private lateinit var underTest: DefaultTransfersRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val databaseHandler = mock<DatabaseHandler>()
    private val transferEventMapper = mock<TransferEventMapper>()
    private val cancelToken = mock<MegaCancelToken>()
    private val appEventGateway: AppEventGateway = mock()
    private val transferMapper: TransferMapper = mock()

    @Before
    fun setUp() {
        underTest = DefaultTransfersRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            dbH = databaseHandler,
            transferEventMapper = transferEventMapper,
            appEventGateway = appEventGateway,
            transferMapper = transferMapper
        )
    }

    private fun mockStartUpload() = megaApiGateway.startUpload(
        localPath = any(),
        parentNode = any(),
        fileName = anyOrNull(),
        modificationTime = any(),
        appData = anyOrNull(),
        isSourceTemporary = any(),
        shouldStartFirst = any(),
        cancelToken = anyOrNull(),
        listener = any(),
    )

    private fun startUploadFlow() =
        underTest.startUpload(
            localPath = "test local path",
            parentNode = mock(),
            fileName = "test filename",
            modificationTime = 123456789L,
            appData = null,
            isSourceTemporary = false,
            shouldStartFirst = false,
            cancelToken = cancelToken,
        )

    @Test
    fun `test that OnTransferStart is returned when the upload begins`() = runTest {
        whenever(mockStartUpload()).thenAnswer {
            (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferStart(
                api = mock(),
                transfer = mock(),
            )
        }
        startUploadFlow().test {
            assertThat(awaitItem()).isInstanceOf(GlobalTransfer.OnTransferStart::class.java)
        }
    }

    @Test
    fun `test that OnTransferFinished is returned when the upload is finished`() = runTest {
        whenever(mockStartUpload()).thenAnswer {
            (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferFinish(
                api = mock(),
                transfer = mock(),
                error = mock { on { errorCode }.thenReturn(MegaError.API_OK) },
            )
        }
        startUploadFlow().test {
            assertThat(awaitItem()).isInstanceOf(GlobalTransfer.OnTransferFinish::class.java)
        }
        verify(cancelToken).cancel()
        verify(megaApiGateway).removeTransferListener(any())
    }

    @Test
    fun `test that OnTransferUpdate is returned when the ongoing upload has been updated`() =
        runTest {
            whenever(mockStartUpload()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferUpdate(
                    api = mock(),
                    transfer = mock(),
                )
            }
            startUploadFlow().test {
                assertThat(awaitItem()).isInstanceOf(GlobalTransfer.OnTransferUpdate::class.java)
            }
        }

    @Test
    fun `test that OnTransferTemporaryError is returned when the upload experiences a temporary error`() =
        runTest {
            whenever(mockStartUpload()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferTemporaryError(
                    api = mock(),
                    transfer = mock(),
                    error = mock { on { errorCode }.thenReturn(MegaError.API_OK + 1) },
                )
            }
            startUploadFlow().test {
                assertThat(awaitItem()).isInstanceOf(GlobalTransfer.OnTransferTemporaryError::class.java)
            }
        }

    @Test
    fun `test that OnTransferData is returned when the upload data is being read`() = runTest {
        whenever(mockStartUpload()).thenAnswer {
            (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferData(
                api = mock(),
                transfer = mock(),
                buffer = byteArrayOf(),
            )
        }
        startUploadFlow().test {
            assertThat(awaitItem()).isInstanceOf(GlobalTransfer.OnTransferData::class.java)
        }
    }


    @Test
    fun `test that cancelTransferByTag returns success when MegaApi returns API_OK`() = runTest {
        val transferTag = 1000

        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        val megaRequest = mock<MegaRequest>()

        whenever(megaApiGateway.cancelTransferByTag(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError,
            )
        }

        underTest.cancelTransferByTag(transferTag)
    }

    @Test(expected = MegaException::class)
    fun `test that cancelTransferByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.cancelTransferByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.cancelTransferByTag(transferTag)
        }

    @Test
    fun `test that moveTransferToFirstByTag returns success when MegaApi returns API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToFirstByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferToFirstByTag(transferTag)
        }

    @Test(expected = MegaException::class)
    fun `test that moveTransferToFirstByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToFirstByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferToFirstByTag(transferTag)
        }

    @Test
    fun `test that moveTransferToLastByTag returns success when MegaApi returns API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToLastByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferToLastByTag(transferTag)
        }

    @Test(expected = MegaException::class)
    fun `test that moveTransferToLastByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToLastByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferToLastByTag(transferTag)
        }

    @Test
    fun `test that moveTransferBeforeByTag returns success when MegaApi returns API_OK`() =
        runTest {
            val transferTag = 1000
            val previousTransferTag = 1001

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferBeforeByTag(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferBeforeByTag(transferTag, previousTransferTag)
        }

    @Test(expected = MegaException::class)
    fun `test that moveTransferBeforeByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000
            val previousTransferTag = 1001

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferBeforeByTag(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferBeforeByTag(transferTag, previousTransferTag)
        }

    @Test
    fun `test that getTransferByTag return value when call api returns object`() = runTest {
        val transferTag = 1
        val megaTransfer = mock<MegaTransfer>()
        val transfer = mock<Transfer>()
        whenever(megaApiGateway.getTransfersByTag(transferTag)).thenReturn(megaTransfer)
        whenever(transferMapper(megaTransfer)).thenReturn(transfer)
        assertThat(underTest.getTransferByTag(transferTag)).isEqualTo(transfer)
    }

    @Test
    fun `test that getTransferByTag return null when call api returns null`() = runTest {
        val transferTag = 1
        whenever(megaApiGateway.getTransfersByTag(transferTag)).thenReturn(null)
        assertThat(underTest.getTransferByTag(transferTag)).isNull()
    }
}