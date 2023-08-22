package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PausedTransferEventMapperTest {
    private lateinit var underTest: PausedTransferEventMapper

    @BeforeAll
    fun setup() {
        underTest = PausedTransferEventMapper()
    }

    @ParameterizedTest
    @MethodSource("provideNotFinishEvents")
    fun `test that event is mapped to null when event is not a finish event`(
        event: RequestEvent,
    ) = runTest {
        assertThat(underTest(event, mock())).isNull()
    }

    @Test
    fun `test that event is mapped to null when is a finish event with error`() = runTest {
        assertThat(
            underTest(mockRequestEvent(errorCode = MegaError.API_OK + 1), mock())
        ).isNull()
    }

    @Test
    fun `test that event is mapped to null when is a finish event is not from pause request`() =
        runTest {
            assertThat(
                underTest(mockRequestEvent(type = MegaRequest.TYPE_PAUSE_TRANSFER + 1), mock())
            ).isNull()
        }

    @Test
    fun `test that event is mapped to null when transfer is null`() = runTest {
        val getNullTransfer: suspend (Int) -> Transfer? = { null }
        assertThat(
            underTest(mockRequestEvent(), getNullTransfer)
        ).isNull()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that event is mapped correctly`(
        paused: Boolean,
    ) = runTest {
        val transfer = mock<Transfer>()
        val getTransfer: suspend (Int) -> Transfer? = { transfer }
        val expected = TransferEvent.TransferPaused(transfer, paused)
        assertThat(
            underTest(mockRequestEvent(paused = paused), getTransfer)
        ).isEqualTo(expected)
    }

    private fun provideNotFinishEvents() = listOf(
        RequestEvent.OnRequestUpdate(mock()),
        RequestEvent.OnRequestStart(mock()),
        RequestEvent.OnRequestTemporaryError(mock(), mock())
    )

    private fun mockRequestEvent(
        errorCode: Int = MegaError.API_OK,
        type: Int = MegaRequest.TYPE_PAUSE_TRANSFER,
        paused: Boolean = true,
    ): RequestEvent {
        val error = mock<MegaError>()
        val request = mock<MegaRequest>()
        whenever(error.errorCode).thenReturn(errorCode)
        whenever(request.type).thenReturn(type)
        whenever(request.flag).thenReturn(paused)
        return RequestEvent.OnRequestFinish(request, error)
    }
}