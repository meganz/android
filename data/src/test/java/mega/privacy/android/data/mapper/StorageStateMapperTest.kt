package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_CHANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_GREEN
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_ORANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_RED
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_UNKNOWN
import org.junit.Test

class StorageStateMapperTest {

    @Test
    fun `test that storage type can be mapped correctly`() {
        val unknownState = 100
        val expectedResults = HashMap<Int, StorageState>().apply {
            put(STORAGE_STATE_UNKNOWN, StorageState.Unknown)
            put(STORAGE_STATE_GREEN, StorageState.Green)
            put(STORAGE_STATE_ORANGE, StorageState.Orange)
            put(STORAGE_STATE_RED, StorageState.Red)
            put(STORAGE_STATE_CHANGE, StorageState.Change)
            put(STORAGE_STATE_PAYWALL, StorageState.PayWall)
            put(unknownState, StorageState.Unknown)
        }

        expectedResults.forEach { (key, value) ->
            val actual = toStorageState(key)
            assertThat(actual).isEqualTo(value)
        }
    }
}