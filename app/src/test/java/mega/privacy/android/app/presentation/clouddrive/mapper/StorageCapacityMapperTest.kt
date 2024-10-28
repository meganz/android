package mega.privacy.android.app.presentation.clouddrive.mapper


import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.clouddrive.model.StorageOverQuotaCapacity
import mega.privacy.android.domain.entity.StorageState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StorageCapacityMapperTest {
    private lateinit var underTest: StorageCapacityMapper

    @BeforeEach
    fun setUp() {
        underTest = StorageCapacityMapper()
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(StorageState.Red, true, true, StorageOverQuotaCapacity.FULL),
        Arguments.of(StorageState.Red, false, true, StorageOverQuotaCapacity.DEFAULT),
        Arguments.of(StorageState.Red, true, false, StorageOverQuotaCapacity.FULL),
        Arguments.of(StorageState.Red, false, false, StorageOverQuotaCapacity.DEFAULT),
        Arguments.of(StorageState.Orange, true, true, StorageOverQuotaCapacity.ALMOST_FULL),
        Arguments.of(StorageState.Orange, false, true, StorageOverQuotaCapacity.ALMOST_FULL),
        Arguments.of(StorageState.Orange, true, false, StorageOverQuotaCapacity.DEFAULT),
        Arguments.of(StorageState.Orange, false, false, StorageOverQuotaCapacity.DEFAULT),
        Arguments.of(StorageState.Green, true, true, StorageOverQuotaCapacity.DEFAULT),
    )

    @ParameterizedTest(name = "test that if storageState is {0}, isFullStorageOverQuotaBannerEnabled is {1} and isAlmostFullStorageQuotaBannerEnabled is {2} then storage capacity is {3}")
    @MethodSource("provideParameters")
    fun `test that the storage state is successfully mapped into storage over quota capacity`(
        storageState: StorageState,
        isFullStorageOverQuotaBannerEnabled: Boolean,
        isAlmostFullStorageQuotaBannerEnabled: Boolean,
        expected: StorageOverQuotaCapacity
    ) {
        val actual = underTest(
            storageState,
            isFullStorageOverQuotaBannerEnabled,
            isAlmostFullStorageQuotaBannerEnabled
        )

        assertThat(actual).isEqualTo(expected)
    }
}