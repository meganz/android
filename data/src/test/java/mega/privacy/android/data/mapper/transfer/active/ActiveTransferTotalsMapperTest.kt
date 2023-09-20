package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferTotalsMapperTest {

    private lateinit var underTest: ActiveTransferTotalsMapper

    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferTotalsMapper()
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns totalBytes excluding folder transfers`(transferType: TransferType) {
        val entities = createEntities(transferType)
        val expectedTotal = entities.filter { !it.isFolderTransfer }.sumOf { it.totalBytes }
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.totalBytes).isEqualTo(expectedTotal)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns transferredBytes excluding folder transfers`(transferType: TransferType) {
        val entities = createEntities(transferType)
        val transferredBytes = entities.associate { it.tag to it.totalBytes / 2 }
        val expectedTransferredBytes =
            entities.filter { !it.isFolderTransfer }.sumOf { it.totalBytes / 2 }
        val actual = underTest(transferType, entities, transferredBytes)
        assertThat(actual.transferredBytes).isEqualTo(expectedTransferredBytes)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalTransfers`(transferType: TransferType) {
        val entities = createEntities(transferType)
        val expected = entities.size
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.totalTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalFinishedTransfers`(transferType: TransferType) {
        val entities = createEntities(transferType)
        val expected = entities.count { it.isFinished }
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.totalFinishedTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper excludes folder transfers in totalFileTransfers`(transferType: TransferType) {
        val entities = createEntities(transferType)
        val expected = entities.filter { !it.isFolderTransfer }.size
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.totalFileTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct pausedFileTransfers excluding folder transfers`(
        transferType: TransferType,
    ) {
        val entities = createEntities(transferType)
        val expected = entities.filter { !it.isFolderTransfer && it.isPaused }.size
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.pausedFileTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper excludes folder transfers in totalFinishedFileTransfers`(transferType: TransferType) {
        val entities = createEntities(transferType)
        val expected = entities.filter { !it.isFolderTransfer }.count { it.isFinished }
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.totalFinishedFileTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns empty entity when empty list is mapped`(transferType: TransferType) {
        val expected = ActiveTransferTotals(transferType, 0, 0, 0, 0, 0, 0, 0)
        assertThat(underTest(transferType, emptyList(), emptyMap())).isEqualTo(expected)
    }

    private fun createEntities(transferType: TransferType) = (0..20).map { tag ->
        ActiveTransferEntity(
            tag = tag,
            transferType = transferType,
            totalBytes = 1024 * (tag.toLong() % 5 + 1),
            isFinished = tag.rem(5) == 0,
            isFolderTransfer = tag.rem(8) == 0,
            isPaused = false,
        )
    }
}