package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferGroupEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferGroupImpl
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferGroupEntityMapperTest {
    private lateinit var underTest: ActiveTransferGroupEntityMapper


    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferGroupEntityMapper()
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    internal fun `test that entity is mapped correctly`(
        transferType: TransferType,
    ) = runTest {
        val activeTransferGroup =
            ActiveTransferGroupImpl(GROUP_ID, transferType, DESTINATION, START_TIME)
        val expected =
            ActiveTransferGroupEntity(GROUP_ID, transferType, DESTINATION, START_TIME)
        val actual = underTest(activeTransferGroup)
        assertThat(expected).isEqualTo(actual)
    }

    private companion object {
        const val GROUP_ID = 34
        const val DESTINATION = "destination"
        const val START_TIME = 94837594L
    }
}