package mega.privacy.android.data.mapper.sync

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.sync.SyncType
import nz.mega.sdk.MegaSync
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncTypeMapperTest {

    private lateinit var underTest: SyncTypeMapper

    @BeforeAll
    fun setup() {
        underTest = SyncTypeMapper()
    }

    @ParameterizedTest(name = " if received value is {0}")
    @MethodSource("provideParametersDirect")
    fun `test that sync type direct mapper returns correctly`(
        originValue: SyncType,
        resultValue: MegaSync.SyncType,
    ) {
        Truth.assertThat(underTest(originValue)).isEqualTo(resultValue)
    }

    @ParameterizedTest(name = " if received value is {0}")
    @MethodSource("provideParametersInverse")
    fun `test that sync type inverse mapper returns correctly`(
        originValue: MegaSync.SyncType,
        resultValue: SyncType,
    ) {
        Truth.assertThat(underTest(originValue)).isEqualTo(resultValue)
    }

    private fun provideParametersDirect(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncType.TYPE_TWOWAY, MegaSync.SyncType.TYPE_TWOWAY),
        Arguments.of(SyncType.TYPE_BACKUP, MegaSync.SyncType.TYPE_BACKUP),
        Arguments.of(SyncType.TYPE_UNKNOWN, MegaSync.SyncType.TYPE_UNKNOWN),
    )

    private fun provideParametersInverse(): Stream<Arguments> = Stream.of(
        Arguments.of(MegaSync.SyncType.TYPE_TWOWAY, SyncType.TYPE_TWOWAY),
        Arguments.of(MegaSync.SyncType.TYPE_BACKUP, SyncType.TYPE_BACKUP),
        Arguments.of(MegaSync.SyncType.TYPE_UNKNOWN, SyncType.TYPE_UNKNOWN),
    )
}