package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderSyncTransferSeedUseCaseTest {

    private lateinit var underTest: OrderSyncTransferSeedUseCase

    @BeforeAll
    fun setUp() {
        underTest = OrderSyncTransferSeedUseCase()
    }

    @Test
    fun `test that invoke drops terminal entries`() {
        val ordered = underTest(
            listOf(
                entry(1, TransferState.STATE_COMPLETED),
                entry(2, TransferState.STATE_FAILED),
                entry(3, TransferState.STATE_CANCELLED),
                entry(4, TransferState.STATE_QUEUED),
            ),
        )

        assertThat(ordered.map { it.tag }).containsExactly(4)
    }

    @Test
    fun `test that invoke puts slot holders first and orders each group by priority then tag`() {
        val ordered = underTest(
            listOf(
                entry(1, TransferState.STATE_QUEUED, priority = 5L),
                entry(2, TransferState.STATE_ACTIVE, priority = 9L),
                entry(3, TransferState.STATE_PAUSED, priority = 1L),
                entry(4, TransferState.STATE_RETRYING, priority = 9L),
                entry(5, TransferState.STATE_COMPLETING, priority = 3L),
                entry(6, TransferState.STATE_QUEUED, priority = 1L),
            ),
        )

        assertThat(ordered.map { it.tag }).containsExactly(5, 2, 4, 3, 6, 1).inOrder()
    }

    private fun entry(tag: Int, state: TransferState, priority: Long = tag.toLong()) = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = state,
        fileName = "f$tag.jpg",
        totalBytes = 100L,
        transferredBytes = 0L,
        speed = 0L,
        meanSpeed = 0L,
        priority = priority,
        notificationNumber = 1L,
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )
}
