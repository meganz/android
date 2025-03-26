package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroupImpl
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.singleOrNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferTotalsMapperTest {

    private lateinit var underTest: ActiveTransferTotalsMapper

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferTotalsMapper { transferRepository }
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
            val transferredBytes = entities.associate { it.uniqueId to it.totalBytes / 2 }
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
                entities.filter { it.uniqueId.rem(2) == 0L } //set 50% of the transfers as completed 50% as not completed
            val transferredBytes =
                entities.associate { it.uniqueId to if (it in subSetWithErrors) it.totalBytes / 2 else it.totalBytes }
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
                whenever(transferRepository.getActiveTransferGroupById(groupId)) doReturn ActiveTransferActionGroupImpl(
                    groupId = groupId,
                    transferType = transferType,
                    destination = "destination$groupId",
                    startTime = groupId.toLong(),
                )
                entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
            }
            val transferredBytes = emptyMap<Long, Long>()
            val expected = entities
                .groupBy { it.getTransferGroup()?.groupId }
                .mapNotNull { (key, activeTransfers) ->
                    key?.toInt()?.let { groupId ->
                        val fileTransfers = activeTransfers.filter { !it.isFolderTransfer }
                        ActiveTransferTotals.ActionGroup(
                            groupId = groupId,
                            totalFiles = fileTransfers.size,
                            finishedFiles = fileTransfers.count { it.isFinished },
                            completedFiles = 0,
                            alreadyTransferred = fileTransfers.count { it.isAlreadyTransferred },
                            destination = "destination$groupId",
                            singleFileName = null,
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            transferredBytes = fileTransfers.sumOf {
                                if (it.isFinished) it.totalBytes else 0L
                            },
                        )
                    }
                }
            val actual = underTest(transferType, entities, transferredBytes).actionGroups
            assertThat(actual).containsExactlyElementsIn(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper correctly maps completed transfers in groups if all are finished`(
        transferType: TransferType,
    ) =
        runTest {
            val entities = createEntities(transferType).mapIndexed { index, entity ->
                val groupId = index.mod(5)
                whenever(transferRepository.getActiveTransferGroupById(groupId)) doReturn ActiveTransferActionGroupImpl(
                    groupId = groupId,
                    transferType = transferType,
                    destination = "destination$groupId",
                    startTime = groupId.toLong(),
                )
                entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
            }
            val subSetWithErrors =
                entities.filter { it.uniqueId.rem(2) == 0L } //set 50% of the transfers as completed 50% as not completed
            val transferredBytes =
                entities.associate { it.uniqueId to if (it in subSetWithErrors) it.totalBytes / 2 else it.totalBytes }
            val entitiesFinished = entities.map { it.copy(isFinished = true) }
            val expected = entities
                .groupBy { it.getTransferGroup()?.groupId }
                .mapNotNull { (key, activeTransfers) ->
                    key?.toInt()?.let { groupId ->
                        val fileTransfers = activeTransfers.filter { !it.isFolderTransfer }
                        val expectedCompleted = activeTransfers.filter { !it.isFolderTransfer }
                            .count { it !in subSetWithErrors }
                        ActiveTransferTotals.ActionGroup(
                            groupId = groupId,
                            totalFiles = fileTransfers.size,
                            finishedFiles = fileTransfers.size,
                            completedFiles = expectedCompleted,
                            alreadyTransferred = fileTransfers.count { it.isAlreadyTransferred },
                            destination = "destination$groupId",
                            singleFileName = null,
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            transferredBytes = fileTransfers.sumOf { it.totalBytes },
                        )
                    }
                }
            val actual = underTest(transferType, entitiesFinished, transferredBytes).actionGroups
            assertThat(actual).containsExactlyElementsIn(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper correctly maps completed transfers in groups `(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType).mapIndexed { index, entity ->
                val groupId = index.mod(5)
                whenever(transferRepository.getActiveTransferGroupById(groupId)) doReturn ActiveTransferActionGroupImpl(
                    groupId = groupId,
                    transferType = transferType,
                    destination = "destination$groupId",
                    startTime = groupId.toLong(),
                )
                entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
            }
            val subSetWithErrors =
                entities.filter { it.uniqueId.rem(2) == 0L } //set 50% of the transfers as completed 50% as not completed
            val transferredBytes =
                entities.associate { it.uniqueId to if (it in subSetWithErrors) it.totalBytes / 2 else it.totalBytes }
            val expected = entities
                .groupBy { it.getTransferGroup()?.groupId }
                .mapNotNull { (key, activeTransfers) ->
                    key?.toInt()?.let { groupId ->
                        val fileTransfers = activeTransfers.filter { !it.isFolderTransfer }
                        ActiveTransferTotals.ActionGroup(
                            groupId = groupId,
                            totalFiles = fileTransfers.size,
                            finishedFiles = fileTransfers.count { it.isFinished },
                            completedFiles = fileTransfers.count { it.isFinished && transferredBytes[it.uniqueId] == it.totalBytes },
                            alreadyTransferred = fileTransfers.count { it.isAlreadyTransferred },
                            destination = "destination$groupId",
                            singleFileName = null,
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            transferredBytes = fileTransfers.sumOf {
                                if (it.isFinished) it.totalBytes else transferredBytes[it.uniqueId]
                                    ?: 0L
                            },
                        )
                    }
                }
            val actual = underTest(transferType, entities, transferredBytes).actionGroups
            assertThat(actual).containsExactlyElementsIn(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that app data from individual transfers is added to transfer group without duplicates`(
        transferType: TransferType,
    ) = runTest {
        val groupAppData = TransferAppData.TransferGroup(0L)
        val appData = listOf(
            TransferAppData.PreviewDownload,
            TransferAppData.ChatUpload(4534L),
            TransferAppData.VoiceClip,
            TransferAppData.Geolocation(43.0, 2.4)
        )
        val entities = createEntities(transferType).mapIndexed { index, entity ->
            entity.copy(
                appData = appData.subList(index.mod(appData.size), appData.size) //random sub-set
                        + groupAppData
            )
        }
        val actual = underTest(
            transferType,
            entities,
            emptyMap(),
            listOf(
                ActiveTransferTotals.ActionGroup(
                    groupId = 0,
                    totalFiles = 0,
                    finishedFiles = 0,
                    completedFiles = 0,
                    alreadyTransferred = 0,
                    destination = "",
                    singleFileName = null,
                    singleTransferTag = 0,
                    startTime = 0,
                    pausedFiles = 0,
                    totalBytes = 0,
                    transferredBytes = 0,
                )
            )
        ).actionGroups.single().appData
        assertThat(actual).containsExactlyElementsIn(appData)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that groups are not fetched from repository if was in previous groups`(transferType: TransferType) =
        runTest {
            val actionGroups = mutableListOf<ActiveTransferTotals.ActionGroup>()
            val entities = createEntities(transferType).mapIndexed { index, entity ->
                val groupId = index.mod(5)
                actionGroups.add(
                    ActiveTransferTotals.ActionGroup(
                        groupId = groupId,
                        totalFiles = 0,
                        finishedFiles = 0,
                        completedFiles = 0,
                        alreadyTransferred = 0,
                        destination = "destination$groupId",
                        singleFileName = null,
                        singleTransferTag = entity.tag,
                        startTime = groupId.toLong(),
                        pausedFiles = 0,
                        totalBytes = 0,
                        transferredBytes = 0,
                    )
                )
                entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
            }
            val expected = entities
                .groupBy { it.getTransferGroup()?.groupId }
                .mapNotNull { (key, activeTransfers) ->
                    key?.toInt()?.let { groupId ->
                        val fileTransfers = activeTransfers.filter { !it.isFolderTransfer }
                        ActiveTransferTotals.ActionGroup(
                            groupId = groupId,
                            totalFiles = fileTransfers.size,
                            finishedFiles = fileTransfers.count { it.isFinished },
                            completedFiles = 0,
                            alreadyTransferred = fileTransfers.count { it.isAlreadyTransferred },
                            destination = "destination$groupId",
                            singleFileName = null,
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            transferredBytes = fileTransfers.sumOf {
                                //if it's finished always totalBytes as it can be cancelled or failed
                                if (it.isFinished) it.totalBytes else 0L
                            },
                        )
                    }
                }
            val actual =
                underTest(
                    transferType,
                    entities,
                    emptyMap(),
                    previousActionGroups = actionGroups
                ).actionGroups
            assertThat(actual).containsExactlyElementsIn(expected)
            verifyNoInteractions(transferRepository)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper correctly maps single file name if group contains only one file`(
        transferType: TransferType,
    ) = runTest {
        val entities = createEntities(transferType).mapIndexed { index, entity ->
            val groupId = index
            whenever(transferRepository.getActiveTransferGroupById(groupId)) doReturn ActiveTransferActionGroupImpl(
                groupId = groupId,
                transferType = transferType,
                destination = "destination$groupId",
                startTime = groupId.toLong(),
            )
            entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
        }.filterNot { it.isFolderTransfer }
        val transferredBytes = emptyMap<Long, Long>()
        val expected = entities
            .groupBy { it.getTransferGroup()?.groupId }
            .mapNotNull { (key, activeTransfer) ->
                key?.toInt()?.let { groupId ->
                    ActiveTransferTotals.ActionGroup(
                        groupId = groupId,
                        totalFiles = activeTransfer.size,
                        finishedFiles = activeTransfer.count { it.isFinished },
                        completedFiles = 0,
                        alreadyTransferred = activeTransfer.count { it.isAlreadyTransferred },
                        destination = "destination$groupId",
                        singleFileName = activeTransfer.singleOrNull()?.fileName,
                        singleTransferTag = activeTransfer.singleOrNull()?.tag,
                        startTime = groupId.toLong(),
                        pausedFiles = activeTransfer.count { it.isPaused },
                        totalBytes = activeTransfer.sumOf { it.totalBytes },
                        transferredBytes = activeTransfer.sumOf {
                            if (it.isFinished) it.totalBytes else 0L
                        },
                    )
                }
            }
        val actual = underTest(transferType, entities, transferredBytes).actionGroups
        assertThat(actual).containsExactlyElementsIn(expected)
    }


    private fun createEntities(transferType: TransferType) = (0..20).map { tag ->
        ActiveTransferEntity(
            uniqueId = tag.toLong(),
            tag = tag,
            transferType = transferType,
            totalBytes = 1024 * (tag.toLong() % 5 + 1),
            isFinished = tag.rem(5) == 0,
            isFolderTransfer = tag.rem(8) == 0,
            isPaused = tag.rem(2) == 0,
            isAlreadyTransferred = tag.rem(9) == 0,
            isCancelled = tag.rem(7) == 0,
            appData = emptyList(),
            fileName = "File$tag.txt"
        )
    }
}