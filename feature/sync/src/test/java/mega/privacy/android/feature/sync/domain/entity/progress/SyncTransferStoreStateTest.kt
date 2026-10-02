package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SyncTransferStoreStateTest {

    @Test
    fun `test that NotSeeded is not seeded and holds nothing`() {
        val state = SyncTransferStoreState.NotSeeded

        assertThat(state.isSeeded).isFalse()
        assertThat(state.active).isEmpty()
        assertThat(state.overflow).isEmpty()
        assertThat(state.completed).isEmpty()
        assertThat(state.reseedRemoved).isEmpty()
        assertThat(state.reseedKeptCompleting).isEmpty()
        assertThat(state.counts).isEqualTo(SyncTransferCounts.Empty)
    }

    @Test
    fun `test that reset returns NotSeeded`() {
        assertThat(SyncTransferStore.reset()).isEqualTo(SyncTransferStoreState.NotSeeded)
    }
}
