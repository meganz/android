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
        val expectedResults = mapOf(
            STORAGE_STATE_UNKNOWN to StorageState.Unknown,
            STORAGE_STATE_GREEN to StorageState.Green,
            STORAGE_STATE_ORANGE to StorageState.Orange,
            STORAGE_STATE_RED to StorageState.Red,
            STORAGE_STATE_CHANGE to StorageState.Change,
            STORAGE_STATE_PAYWALL to StorageState.PayWall,
            unknownState to StorageState.Unknown,
        )

        expectedResults.forEach { (key, value) ->
            val actual = toStorageState(key)
            assertThat(actual).isEqualTo(value)
        }
    }
}