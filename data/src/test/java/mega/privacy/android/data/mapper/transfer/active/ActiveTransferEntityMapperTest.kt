package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferEntityMapperTest {
    private lateinit var underTest: ActiveTransferEntityMapper


    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferEntityMapper()
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @MethodSource("provideParameters")
    internal fun `test that mapper returns model correctly when invoke function`(
        model: ActiveTransfer,
        expected: ActiveTransferEntity,
    ) = runTest {
        Truth.assertThat(underTest(model)).isEqualTo(expected)
    }

    private fun provideParameters(): List<Arguments> =
        TransferType.entries.flatMap { transferType ->
            TransferState.entries.flatMap { transferState ->
                listOf(true, false).flatMap { isFinished ->
                    listOf(true, false).flatMap { isFolder ->
                        listOf(true, false).flatMap { isPaused ->
                            listOf(true, false).map { isAlreadyDownloaded ->
                                Arguments.of(
                                    ActiveTransferTestImpl(
                                        tag = TAG,
                                        transferType = transferType,
                                        totalBytes = TOTAL,
                                        isFinished = isFinished,
                                        isFolderTransfer = isFolder,
                                        isPaused = isPaused,
                                        isAlreadyTransferred = isAlreadyDownloaded,
                                        localPath = LOCAL_PATH,
                                        nodeHandle = NODE_HANDLE,
                                        state = transferState,
                                    ),
                                    ActiveTransferEntity(
                                        tag = TAG,
                                        transferType = transferType,
                                        totalBytes = TOTAL,
                                        isFinished = isFinished,
                                        isFolderTransfer = isFolder,
                                        isPaused = isPaused,
                                        isAlreadyTransferred = isAlreadyDownloaded,
                                        localPath = LOCAL_PATH,
                                        nodeHandle = NODE_HANDLE,
                                        state = transferState,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

    companion object {
        private const val TAG = 2
        private const val TOTAL = 1024L
        private const val LOCAL_PATH = "path/file.txt"
        private const val NODE_HANDLE = 1235345L
    }


    /**
     * Active transfer impl
     *
     * @constructor Create empty Active transfer impl
     */
    private data class ActiveTransferTestImpl(
        override val tag: Int,
        override val transferType: TransferType,
        override val totalBytes: Long,
        override val isFinished: Boolean,
        override val isFolderTransfer: Boolean,
        override val isPaused: Boolean,
        override val isAlreadyTransferred: Boolean,
        override val localPath: String,
        override val nodeHandle: Long,
        override val state: TransferState,
    ) : ActiveTransfer
}