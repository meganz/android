package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.StorageState
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_CHANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_GREEN
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_ORANGE
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_RED
import nz.mega.sdk.MegaApiJava.STORAGE_STATE_UNKNOWN
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageStateMapperTest {

    private lateinit var underTest: StorageStateMapper

    @BeforeAll
    fun setup() {
        underTest = StorageStateMapper()
    }

    @ParameterizedTest(name = "when storage state from SDK is {0}, StorageState is {1}")
    @MethodSource("provideStorageStateParameters")
    fun `test that storage state can be mapped correctly`(
        storageState: Int,
        expected: StorageState,
    ) {
        val actual = underTest(storageState)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideStorageStateParameters() = listOf(
        arrayOf(STORAGE_STATE_UNKNOWN, StorageState.Unknown),
        arrayOf(STORAGE_STATE_GREEN, StorageState.Green),
        arrayOf(STORAGE_STATE_ORANGE, StorageState.Orange),
        arrayOf(STORAGE_STATE_RED, StorageState.Red),
        arrayOf(STORAGE_STATE_CHANGE, StorageState.Change),
        arrayOf(STORAGE_STATE_PAYWALL, StorageState.PayWall),
        arrayOf(100, StorageState.Unknown),
    )
}