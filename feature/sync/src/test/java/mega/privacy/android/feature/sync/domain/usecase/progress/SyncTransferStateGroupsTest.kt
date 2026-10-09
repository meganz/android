package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.TransferState
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SyncTransferStateGroupsTest {

    @ParameterizedTest
    @EnumSource(names = ["STATE_ACTIVE", "STATE_RETRYING", "STATE_COMPLETING"])
    fun `test that isSlotHolding returns true for a state that holds a slot`(state: TransferState) {
        assertThat(state.isSlotHolding).isTrue()
        assertThat(state.isTerminal).isFalse()
    }

    @ParameterizedTest
    @EnumSource(names = ["STATE_COMPLETED", "STATE_FAILED", "STATE_CANCELLED"])
    fun `test that isTerminal returns true for a finished state`(state: TransferState) {
        assertThat(state.isTerminal).isTrue()
        assertThat(state.isSlotHolding).isFalse()
    }

    @ParameterizedTest
    @EnumSource(
        mode = EnumSource.Mode.EXCLUDE,
        names = [
            "STATE_ACTIVE",
            "STATE_RETRYING",
            "STATE_COMPLETING",
            "STATE_COMPLETED",
            "STATE_FAILED",
            "STATE_CANCELLED",
        ],
    )
    fun `test that isSlotHolding and isTerminal return false for every other state`(state: TransferState) {
        assertThat(state.isSlotHolding).isFalse()
        assertThat(state.isTerminal).isFalse()
    }
}
