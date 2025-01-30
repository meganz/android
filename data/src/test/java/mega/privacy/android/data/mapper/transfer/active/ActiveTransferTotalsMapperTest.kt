package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferGroupImpl
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.getTransferGroup
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.collections.component1
import kotlin.collections.component2

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferTotalsMapperTest {

    private lateinit var underTest: ActiveTransferTotalsMapper

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferTotalsMapper({ transferRepository })
    }

    @BeforeEach
    fun cleanup() {
        reset(transferRepository)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns totalBytes excluding folder transfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expectedTotal = entities.filter { !it.isFolderTransfer }.sumOf { it.totalBytes }
            val actual = underTest(transferType, entities, emptyMap())
            assertThat(actual.totalBytes).isEqualTo(expectedTotal)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns transferredBytes excluding folder transfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val transferredBytes = entities.associate { it.tag to it.totalBytes / 2 }
            val expectedTransferredBytes =
                entities.filter { !it.isFolderTransfer }.sumOf {
                    if (it.isFinished) it.totalBytes else it.totalBytes / 2
                }
            val actual = underTest(transferType, entities, transferredBytes)
            assertThat(actual.transferredBytes).isEqualTo(expectedTransferredBytes)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalTransfers`(transferType: TransferType) = runTest {
        val entities = createEntities(transferType)
        val expected = entities.size
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.totalTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalFinishedTransfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected = entities.count { it.isFinished }
            val actual = underTest(transferType, entities, emptyMap())
            assertThat(actual.totalFinishedTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper excludes folder transfers in totalFileTransfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected = entities.filter { !it.isFolderTransfer }.size
            val actual = underTest(transferType, entities, emptyMap())
            assertThat(actual.totalFileTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct pausedFileTransfers excluding folder transfers`(
        transferType: TransferType,
    ) = runTest {
        val entities = createEntities(transferType)
        val expected = entities.filter { !it.isFolderTransfer && it.isPaused }.size
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.pausedFileTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper excludes folder transfers in totalFinishedFileTransfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected = entities.filter { !it.isFolderTransfer }.count { it.isFinished }
            val actual = underTest(transferType, entities, emptyMap())
            assertThat(actual.totalFinishedFileTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct total completed file transfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val subSetWithErrors =
                entities.filter { it.tag.rem(2) == 0 } //set 50% of the transfers as completed 50% as not completed
            val transferredBytes =
                entities.associate { it.tag to if (it in subSetWithErrors) it.totalBytes / 2 else it.totalBytes }
            val expected = entities.filter { !it.isFolderTransfer }
                .count { it !in subSetWithErrors }
            val entitiesFinished = entities.map { it.copy(isFinished = true) }
            val actual = underTest(transferType, entitiesFinished, transferredBytes)
            assertThat(actual.totalCompletedFileTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct total already transferred files`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected =
                entities.filter { !it.isFolderTransfer }.count { it.isAlreadyTransferred }
            val actual = underTest(transferType, entities, emptyMap())
            assertThat(actual.totalAlreadyTransferredFiles).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalCancelled`(transferType: TransferType) = runTest {
        val entities = createEntities(transferType)
        val expected = entities.count { it.isCancelled }
        val actual = underTest(transferType, entities, emptyMap())
        assertThat(actual.totalCancelled).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns empty entity when empty list is mapped`(transferType: TransferType) =
        runTest {
            val expected = ActiveTransferTotals(transferType, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            assertThat(underTest(transferType, emptyList(), emptyMap())).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper correctly creates groups from app data`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType).mapIndexed { index, entity ->
                val groupId = index.mod(5)
                whenever(transferRepository.getActiveTransferGroupById(groupId)) doReturn ActiveTransferGroupImpl(
                    groupId,
                    transferType,
                    "destination$groupId"
                )
                entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
            }
            val expected = entities
                .groupBy { it.getTransferGroup()?.groupId }
                .mapNotNull { (key, activeTransfers) ->
                    key?.toInt()?.let { groupId ->
                        val fileTransfers = activeTransfers.filter { !it.isFolderTransfer }
                        ActiveTransferTotals.Group(
                            groupId = groupId,
                            totalFiles = fileTransfers.size,
                            finishedFiles = fileTransfers.count { it.isFinished },
                            destination = "destination$groupId",
                        )
                    }
                }
            val actual = underTest(transferType, entities, emptyMap()).groups
            assertThat(actual).containsExactlyElementsIn(expected)
        }

    private fun createEntities(transferType: TransferType) = (0..20).map { tag ->
        ActiveTransferEntity(
            tag = tag,
            transferType = transferType,
            totalBytes = 1024 * (tag.toLong() % 5 + 1),
            isFinished = tag.rem(5) == 0,
            isFolderTransfer = tag.rem(8) == 0,
            isPaused = false,
            isAlreadyTransferred = tag.rem(9) == 0,
            isCancelled = tag.rem(7) == 0,
            appData = emptyList(),
        )
    }
}