package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferDataUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.IsThereAnyPendingUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsThereAnyChatUploadUseCaseTest {

    private lateinit var underTest: IsThereAnyChatUploadUseCase

    private lateinit var isThereAnyPendingUploadUseCase: IsThereAnyPendingUploadsUseCase
    private lateinit var getTransferDataUseCase: GetTransferDataUseCase
    private lateinit var getTransferByTagUseCase: GetTransferByTagUseCase

    private val chatTransfer = mock<Transfer> {
        on { transferType }.thenReturn(TransferType.CHAT_UPLOAD)
        on { appData }.thenReturn(emptyList())
    }
    private val nonChatTransfer = mock<Transfer> {
        on { transferType }.thenReturn(TransferType.GENERAL_UPLOAD)
    }
    private val transferData = mock<TransferData> {
        on { numUploads }.thenReturn(3)
        on { uploadTags }.thenReturn(listOf(1, 2, 3))
    }

    @BeforeAll
    fun setup() {
        isThereAnyPendingUploadUseCase = mock()
        getTransferDataUseCase = mock()
        getTransferByTagUseCase = mock()
        underTest = IsThereAnyChatUploadUseCase(
            isThereAnyPendingUploadsUseCase = isThereAnyPendingUploadUseCase,
            getTransferDataUseCase = getTransferDataUseCase,
            getTransferByTagUseCase = getTransferByTagUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(isThereAnyPendingUploadUseCase, getTransferDataUseCase, getTransferByTagUseCase)
    }

    @ParameterizedTest(name = " {0} if IsThereAnyPendingUploadsUseCase is {1} and iGetTransferDataUseCase is {2}")
    @MethodSource("provideParameters")
    fun `test that IsThereAnyChatUploadUseCase returns`(
        expectedResult: Boolean,
        isThereAnyPendingUploads: Boolean,
        transferData: TransferData?,
        transfer: Transfer?,
    ) = runTest {
        whenever(isThereAnyPendingUploadUseCase()).thenReturn(isThereAnyPendingUploads)
        whenever(getTransferDataUseCase()).thenReturn(transferData)
        if (isThereAnyPendingUploads) {
            transferData?.let {
                for (i in 0 until transferData.numUploads) {
                    whenever(getTransferByTagUseCase(transferData.uploadTags[i]))
                        .thenReturn(transfer)
                }
            }
        }

        Truth.assertThat(underTest.invoke()).isEqualTo(expectedResult)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, false, null, null),
        Arguments.of(false, false, transferData, null),
        Arguments.of(false, false, transferData, nonChatTransfer),
        Arguments.of(false, false, transferData, chatTransfer),
        Arguments.of(false, true, null, null),
        Arguments.of(false, true, transferData, null),
        Arguments.of(false, true, transferData, nonChatTransfer),
        Arguments.of(true, true, transferData, chatTransfer),
    )
}