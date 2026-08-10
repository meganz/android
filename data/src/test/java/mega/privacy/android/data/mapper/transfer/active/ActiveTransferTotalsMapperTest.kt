package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroupImpl
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.getTransferGroup
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
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
import java.math.BigInteger

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
            val actual = underTest(transferType, entities)
            assertThat(actual.totalBytes).isEqualTo(expectedTotal)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns transferredBytes excluding folder transfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType).map {
                it.copy(transferredBytes = it.totalBytes / 2)
            }
            val expectedTransferredBytes =
                entities.filter { !it.isFolderTransfer }.sumOf {
                    if (it.isFinished) it.totalBytes else it.totalBytes / 2
                }
            val actual = underTest(transferType, entities)
            assertThat(actual.transferredBytes).isEqualTo(expectedTransferredBytes)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalTransfers`(transferType: TransferType) = runTest {
        val entities = createEntities(transferType)
        val expected = entities.size
        val actual = underTest(transferType, entities)
        assertThat(actual.totalTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalFinishedTransfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected = entities.count { it.isFinished }
            val actual = underTest(transferType, entities)
            assertThat(actual.totalFinishedTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper excludes folder transfers in totalFileTransfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected = entities.filter { !it.isFolderTransfer }.size
            val actual = underTest(transferType, entities)
            assertThat(actual.totalFileTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct pausedFileTransfers excluding folder transfers`(
        transferType: TransferType,
    ) = runTest {
        val entities = createEntities(transferType)
        val expected = entities.filter { !it.isFolderTransfer && it.isPaused }.size
        val actual = underTest(transferType, entities)
        assertThat(actual.pausedFileTransfers).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper excludes folder transfers in totalFinishedFileTransfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected = entities.filter { !it.isFolderTransfer }.count { it.isFinished }
            val actual = underTest(transferType, entities)
            assertThat(actual.totalFinishedFileTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct total completed file transfers`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType).map {
                it.copy(transferredBytes = if (it.uniqueId.rem(2) == 0L) it.totalBytes / 2 else it.totalBytes)
            }
            val subSetWithErrors =
                entities.filter { it.uniqueId.rem(2) == 0L } //set 50% of the transfers as completed 50% as not completed
            val expected = entities.filter { !it.isFolderTransfer }
                .count { it !in subSetWithErrors }
            val entitiesFinished = entities.map { it.copy(isFinished = true) }
            val actual = underTest(transferType, entitiesFinished)
            assertThat(actual.totalCompletedFileTransfers).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct total already transferred files`(transferType: TransferType) =
        runTest {
            val entities = createEntities(transferType)
            val expected =
                entities.filter { !it.isFolderTransfer }.count { it.isAlreadyTransferred }
            val actual = underTest(transferType, entities)
            assertThat(actual.totalAlreadyTransferredFiles).isEqualTo(expected)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns correct totalCancelled`(transferType: TransferType) = runTest {
        val entities = createEntities(transferType)
        val expected = entities.count { it.isCancelled }
        val actual = underTest(transferType, entities)
        assertThat(actual.totalCancelled).isEqualTo(expected)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns empty entity when empty list is mapped`(transferType: TransferType) =
        runTest {
            val expected = ActiveTransferTotals(transferType, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            assertThat(underTest(transferType, emptyList())).isEqualTo(expected)
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
                            selectedNames = fileTransfers.map { it.fileName },
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            pendingTransferNodeId = null,
                            transferredBytes = fileTransfers.sumOf {
                                if (it.isFinished) it.totalBytes else 0L
                            },
                        )
                    }
                }
            val actual = underTest(transferType, entities).actionGroups
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
                entity.copy(
                    appData = listOf(TransferAppData.TransferGroup(groupId.toLong())),
                    transferredBytes = if (entity.uniqueId.rem(2) == 0L) entity.totalBytes / 2 else entity.totalBytes
                )
            }
            val subSetWithErrors =
                entities.filter { it.uniqueId.rem(2) == 0L } //set 50% of the transfers as completed 50% as not completed
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
                            selectedNames = fileTransfers.map { it.fileName },
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            pendingTransferNodeId = null,
                            transferredBytes = fileTransfers.sumOf { it.totalBytes },
                        )
                    }
                }
            val actual = underTest(transferType, entitiesFinished).actionGroups
            assertThat(actual).containsExactlyElementsIn(expected)
        }

    // #2      : ActionGroup(groupId=1, totalFiles=3, finishedFiles=3, completedFiles=2, alreadyTransferred=0, destination=destination1, selectedNames=[File1.txt, File6.txt, File11.txt], singleTransferTag=null, startTime=1, pausedFiles=1, totalBytes=6144, transferredBytes=6144, pendingTransferNodeId=null, appData=[])
// #1      : ActionGroup(groupId=1, totalFiles=3, finishedFiles=3, completedFiles=0, alreadyTransferred=3, destination=destination1, selectedNames=[File1.txt, File6.txt, File11.txt], singleTransferTag=null, startTime=1, pausedFiles=1, totalBytes=6144, transferredBytes=6144, pendingTransferNodeId=null, appData=[])
    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper correctly maps completed transfers in groups `(transferType: TransferType) =
        runTest {
            val pendingTransferNodeIds = mutableMapOf<Int, PendingTransferNodeIdentifier>()
            val entities = createEntities(transferType).mapIndexed { index, entity ->
                val groupId = index.mod(5)
                val pendingTransferNodeId = pendingTransferNodeIds.getOrPut(groupId) {
                    mock<PendingTransferNodeIdentifier.CloudDriveNode>()
                }
                whenever(transferRepository.getActiveTransferGroupById(groupId)) doReturn ActiveTransferActionGroupImpl(
                    groupId = groupId,
                    transferType = transferType,
                    destination = "destination$groupId",
                    startTime = groupId.toLong(),
                    pendingTransferNodeId = pendingTransferNodeId,
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
                            completedFiles = fileTransfers.count { it.isFinished && it.transferredBytes == it.totalBytes },
                            alreadyTransferred = fileTransfers.count { it.isAlreadyTransferred },
                            destination = "destination$groupId",
                            selectedNames = fileTransfers.map { it.fileName },
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            pendingTransferNodeId = pendingTransferNodeIds[groupId],
                            transferredBytes = fileTransfers.sumOf {
                                if (it.isFinished) it.totalBytes else it.transferredBytes
                            },
                        )
                    }
                }
            val actual = underTest(transferType, entities).actionGroups
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
            listOf(
                ActiveTransferTotals.ActionGroup(
                    groupId = 0,
                    totalFiles = 0,
                    finishedFiles = 0,
                    completedFiles = 0,
                    alreadyTransferred = 0,
                    destination = "",
                    selectedNames = entities.map { it.fileName },
                    singleTransferTag = 0,
                    startTime = 0,
                    pausedFiles = 0,
                    totalBytes = 0,
                    pendingTransferNodeId = null,
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
                        selectedNames = listOf(entity.fileName),
                        singleTransferTag = entity.tag,
                        startTime = groupId.toLong(),
                        pausedFiles = 0,
                        totalBytes = 0,
                        pendingTransferNodeId = null,
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
                            selectedNames = listOf(activeTransfers.first().fileName),
                            singleTransferTag = fileTransfers.singleOrNull()?.tag,
                            startTime = groupId.toLong(),
                            pausedFiles = fileTransfers.count { it.isPaused },
                            totalBytes = fileTransfers.sumOf { it.totalBytes },
                            pendingTransferNodeId = null,
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
                    previousActionGroups = actionGroups
                ).actionGroups
            assertThat(actual).containsExactlyElementsIn(expected)
            verifyNoInteractions(transferRepository)
        }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper correctly maps selectedNames from previous group`(
        transferType: TransferType,
    ) = runTest {
        val groupId = 2354
        val entities = createEntities(transferType).map { entity ->
            entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
        }.filterNot { it.isFolderTransfer }
        val expectedNames = entities.map { it.fileName }
        val previousGroup = listOf(
            mock<ActiveTransferTotals.ActionGroup> {
                on { this.groupId } doReturn groupId
                on { this.selectedNames } doReturn expectedNames
                on { this.destination } doReturn "destination"
            }
        )

        val actual = underTest(transferType, entities, previousGroup).actionGroups

        assertThat(actual.firstOrNull()?.selectedNames).containsExactlyElementsIn(expectedNames)
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper correctly maps selectedNames from database`(
        transferType: TransferType,
    ) = runTest {
        val groupId = 2354
        val entities = createEntities(transferType).map { entity ->
            entity.copy(appData = listOf(TransferAppData.TransferGroup(groupId.toLong())))
        }.filterNot { it.isFolderTransfer }
        val expectedNames = entities.map { it.fileName }
        whenever(transferRepository.getActiveTransferGroupById(groupId)) doReturn ActiveTransferActionGroupImpl(
            groupId = groupId,
            transferType = transferType,
            destination = "destination",
            startTime = 2562,
            selectedNames = expectedNames,
        )

        val actual = underTest(transferType, entities).actionGroups

        assertThat(actual.firstOrNull()?.selectedNames).containsExactlyElementsIn(expectedNames)
    }


    private fun createEntities(transferType: TransferType) = (0..20).map { tag ->
        Transfer(
            uniqueId = tag.toLong(),
            tag = tag,
            transferType = transferType,
            totalBytes = 1024 * (tag.toLong() % 5 + 1),
            isFinished = tag.rem(5) == 0,
            isFolderTransfer = tag.rem(8) == 0,
            state = if (tag.rem(2) == 0) TransferState.STATE_PAUSED else TransferState.STATE_ACTIVE,
            appData = emptyList(),
            fileName = "File$tag.txt",
            localPath = "path/File$tag.txt",
            parentPath = "path",
            startTime = tag * 10L,
            nodeHandle = tag.toLong(),
            parentHandle = -1,
            speed = tag * 14L,
            isSyncTransfer = false,
            isBackupTransfer = false,
            isForeignOverQuota = false,
            isStreamingTransfer = false,
            transferredBytes = 0,
            folderTransferTag = if (tag.rem(8) == 0) tag else null,
            priority = BigInteger.ZERO,
            notificationNumber = 0L,
            stage = TransferStage.STAGE_TRANSFERRING_FILES
        )
    }
}