package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
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
                                    isAlreadyDownloaded = isAlreadyDownloaded,
                                ),
                                ActiveTransferEntity(
                                    tag = TAG,
                                    transferType = transferType,
                                    totalBytes = TOTAL,
                                    isFinished = isFinished,
                                    isFolderTransfer = isFolder,
                                    isPaused = isPaused,
                                    isAlreadyDownloaded = isAlreadyDownloaded,
                                )
                            )
                        }
                    }
                }
            }
        }

    companion object {
        private const val TAG = 2
        private const val TOTAL = 1024L
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
        override val isAlreadyDownloaded: Boolean,
    ) : ActiveTransfer
}