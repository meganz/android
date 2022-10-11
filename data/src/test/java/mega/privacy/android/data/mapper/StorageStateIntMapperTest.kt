package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiJava
import org.junit.Test

class StorageStateIntMapperTest {

    private val expectedResults = mapOf(
        StorageState.Unknown to MegaApiJava.STORAGE_STATE_UNKNOWN,
        StorageState.Green to MegaApiJava.STORAGE_STATE_GREEN,
        StorageState.Orange to MegaApiJava.STORAGE_STATE_ORANGE,
        StorageState.Red to MegaApiJava.STORAGE_STATE_RED,
        StorageState.Change to MegaApiJava.STORAGE_STATE_CHANGE,
        StorageState.PayWall to MegaApiJava.STORAGE_STATE_PAYWALL
    )

    @Test
    fun `test that StorageState is mapped to Int correctly`() {
        StorageState.values().forEach { state ->
            assertThat(storageStateToInt((state))).isEqualTo(expectedResults[state])
        }
    }
}