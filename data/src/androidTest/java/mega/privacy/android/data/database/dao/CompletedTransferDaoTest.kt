package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CompletedTransferDaoTest {
    private lateinit var completedTransferDao: CompletedTransferDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        completedTransferDao = db.completedTransferDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun test_that_getAll_returns_all_items() = runTest {
        val entities = (1..10).map {
            val entity = createCompletedTransferEntity()
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        completedTransferDao.getAllCompletedTransfers().first().forEachIndexed { i, actual ->
            assertThat(actual.fileName).isEqualTo(entities[i].fileName)
            assertThat(actual.type).isEqualTo(entities[i].type)
            assertThat(actual.state).isEqualTo(entities[i].state)
            assertThat(actual.size).isEqualTo(entities[i].size)
            assertThat(actual.handle).isEqualTo(entities[i].handle)
            assertThat(actual.path).isEqualTo(entities[i].path)
            assertThat(actual.isOffline).isEqualTo(entities[i].isOffline)
            assertThat(actual.timestamp).isEqualTo(entities[i].timestamp)
            assertThat(actual.error).isEqualTo(entities[i].error)
            assertThat(actual.originalPath).isEqualTo(entities[i].originalPath)
            assertThat(actual.parentHandle).isEqualTo(entities[i].parentHandle)
            assertThat(actual.appData).isEqualTo(entities[i].appData)
        }
    }

    @Test
    fun test_that_getById_returns_the_corresponding_item() = runTest {
        val entity = createCompletedTransferEntity()
        completedTransferDao.insertOrUpdateCompletedTransfer(entity)
        val id = completedTransferDao.getAllCompletedTransfers().first().first().id

        val actual = completedTransferDao.getCompletedTransferById(id ?: return@runTest)

        assertThat(actual?.fileName).isEqualTo(entity.fileName)
        assertThat(actual?.type).isEqualTo(entity.type)
        assertThat(actual?.state).isEqualTo(entity.state)
        assertThat(actual?.size).isEqualTo(entity.size)
        assertThat(actual?.handle).isEqualTo(entity.handle)
        assertThat(actual?.path).isEqualTo(entity.path)
        assertThat(actual?.isOffline).isEqualTo(entity.isOffline)
        assertThat(actual?.timestamp).isEqualTo(entity.timestamp)
        assertThat(actual?.error).isEqualTo(entity.error)
        assertThat(actual?.originalPath).isEqualTo(entity.originalPath)
        assertThat(actual?.parentHandle).isEqualTo(entity.parentHandle)
        assertThat(actual?.appData).isEqualTo(entity.appData)
    }

    @Test
    fun test_that_insertOrUpdate_insert_the_corresponding_item() = runTest {
        val entity = createCompletedTransferEntity()
        completedTransferDao.insertOrUpdateCompletedTransfer(entity)

        assertThat(completedTransferDao.getAllCompletedTransfers().first().size).isEqualTo(1)
    }

    @Test
    fun test_that_insertOrUpdateCompletedTransfers_insert_the_corresponding_items() = runTest {
        val expected = (1..10).map {
            createCompletedTransferEntity(
                fileName = "2023-03-24 00.13.20_$it.jpg",
            )
        }
        completedTransferDao.insertOrUpdateCompletedTransfers(expected)

        assertThat(
            completedTransferDao.getAllCompletedTransfers().first().map { it.copy(id = null) }
        ).isEqualTo(expected)
    }

    @Test
    fun test_that_insertOrUpdateCompletedTransfers_chunked_insert_the_corresponding_items() =
        runTest {
            val expected = (1..15).map {
                createCompletedTransferEntity(
                    fileName = "2023-03-24 00.13.20_$it.jpg",
                )
            }
            completedTransferDao.insertOrUpdateCompletedTransfers(
                expected,
                chunkSize = 10
            )

            assertThat(
                completedTransferDao.getAllCompletedTransfers().first().map { it.copy(id = null) }
            ).isEqualTo(expected)
        }

    @Test
    fun test_that_deleteAll_delete_all_items() = runTest {
        (1..10).map {
            val entity = createCompletedTransferEntity()
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        completedTransferDao.deleteAllCompletedTransfers()

        assertThat(completedTransferDao.getAllCompletedTransfers().first()).isEmpty()
    }

    @Test
    fun test_that_deleteById_delete_the_corresponding_item() = runTest {
        (1..10).map {
            val entity = createCompletedTransferEntity()
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        val id = completedTransferDao.getAllCompletedTransfers().first().first().id

        val entity = completedTransferDao.getCompletedTransferById(id ?: return@runTest)
        assertThat(entity).isNotNull()

        completedTransferDao.deleteCompletedTransferByIds(listOf(id))
        val actual = completedTransferDao.getCompletedTransferById(id)

        assertThat(actual).isNull()
    }

    @Test
    fun test_that_deleteCompletedTransfers_chunked_deletes_the_corresponding_items() =
        runTest {
            val entities = (1..15).map {
                createCompletedTransferEntity(
                    fileName = "2023-03-24 00.13.20_$it.jpg",
                )
            }

            completedTransferDao.insertOrUpdateCompletedTransfers(entities)
            val inserted = completedTransferDao.getAllCompletedTransfers().first()
            assertThat(inserted.size).isEqualTo(entities.size)
            completedTransferDao.deleteCompletedTransferByIds(
                inserted.map { it.id ?: 0 }, 10
            )
            val actual = completedTransferDao.getAllCompletedTransfers().first()
            assertThat(actual).isEmpty()
        }


    @Test
    fun test_that_getCount_returns_the_items_count() = runTest {
        val expected = 10
        (1..expected).map {
            val entity = createCompletedTransferEntity()
            completedTransferDao.insertOrUpdateCompletedTransfer(entity)
            entity
        }

        assertThat(completedTransferDao.getCompletedTransfersCount()).isEqualTo(expected)
    }

    @Test
    fun test_that_deleteCompletedTransfersByPath_deletes_the_corresponding_items() = runTest {
        val entity = createCompletedTransferEntity()
        completedTransferDao.deleteAllCompletedTransfers()
        completedTransferDao.insertOrUpdateCompletedTransfer(entity)
        completedTransferDao.deleteCompletedTransfersByPath(entity.path)
        assertThat(completedTransferDao.getCompletedTransfersCount()).isEqualTo(0)
    }

    @Test
    fun test_that_deleteOldCompletedTransfersByState_only_deletes_transfers_of_specified_state() =
        runTest {
            // Create transfers with different states
            val state1Transfers = (1..5).map {
                createCompletedTransferEntity(
                    fileName = "state1_file_$it.jpg",
                    timeStamp = it.toLong(),
                    state = 1
                )
            }
            val state2Transfers = (1..5).map {
                createCompletedTransferEntity(
                    fileName = "state2_file_$it.jpg",
                    timeStamp = it.toLong(),
                    state = 2
                )
            }

            val limit = 3
            val expectedState1Count = limit
            val expectedState2Count = state2Transfers.size
            val expectedTotalCount = expectedState1Count + expectedState2Count

            // Insert all transfers
            completedTransferDao.insertOrUpdateCompletedTransfers(state1Transfers + state2Transfers)

            // Verify initial count
            assertThat(completedTransferDao.getCompletedTransfersCount()).isEqualTo(state1Transfers.size + state2Transfers.size)

            // Delete old transfers for state 1 with limit
            completedTransferDao.deleteOldCompletedTransfersByState(
                transferState = 1,
                limit = limit
            )

            // Verify that only state 1 transfers were affected and only the oldest ones were deleted
            val remainingTransfers = completedTransferDao.getAllCompletedTransfers().first()
            assertThat(remainingTransfers.size).isEqualTo(expectedTotalCount)

            // Verify state 1 transfers: should have only the most recent ones up to the limit
            val remainingState1Transfers = remainingTransfers.filter { it.state == 1 }
            assertThat(remainingState1Transfers.size).isEqualTo(expectedState1Count)

            val expectedState1Timestamps = state1Transfers
                .sortedByDescending { it.timestamp }
                .take(limit)
                .map { it.timestamp }
            assertThat(remainingState1Transfers.map { it.timestamp }).containsExactlyElementsIn(
                expectedState1Timestamps
            )

            // Verify state 2 transfers: should remain unchanged
            val remainingState2Transfers = remainingTransfers.filter { it.state == 2 }
            assertThat(remainingState2Transfers.size).isEqualTo(expectedState2Count)
            assertThat(remainingState2Transfers.map { it.timestamp }).containsExactlyElementsIn(
                state2Transfers.map { it.timestamp })
        }

    @Test
    fun test_that_getCompletedTransfersByStateWithLimit_returns_correct_transfers_with_limit() =
        runTest {
            // Create transfers with different states and timestamps
            val state1Transfers = (1..10).map {
                createCompletedTransferEntity(
                    fileName = "state1_file_$it.jpg",
                    timeStamp = it.toLong(),
                    state = 1
                )
            }
            val state2Transfers = (1..8).map {
                createCompletedTransferEntity(
                    fileName = "state2_file_$it.jpg",
                    timeStamp = (it + 100).toLong(), // Different timestamp range
                    state = 2
                )
            }
            val state3Transfers = (1..6).map {
                createCompletedTransferEntity(
                    fileName = "state3_file_$it.jpg",
                    timeStamp = (it + 200).toLong(), // Different timestamp range
                    state = 3
                )
            }

            // Insert all transfers
            completedTransferDao.insertOrUpdateCompletedTransfers(state1Transfers + state2Transfers + state3Transfers)

            // Test query with multiple states and limit
            val states = listOf(1, 2)
            val limit = 5
            val result =
                completedTransferDao.getCompletedTransfersByStateWithLimit(states, limit).first()

            // Verify result
            assertThat(result.size).isEqualTo(limit)
            assertThat(result.all { it.state in states }).isTrue()
            val expectedTimestamps = (state1Transfers + state2Transfers)
                .sortedByDescending { it.timestamp }
                .take(limit)
                .map { it.timestamp }
            assertThat(result.map { it.timestamp }).containsExactlyElementsIn(expectedTimestamps)
            assertThat(result.any { it.state !in states }).isFalse()
        }

    private fun createCompletedTransferEntity(
        fileName: String = "2023-03-24 00.13.20_1.jpg",
        timeStamp: Long = 1684228012974L,
        state: Int = 6,
    ) =
        CompletedTransferEntity(
            fileName = fileName,
            type = 1,
            state = state,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            displayPath = null,
            isOffline = false,
            timestamp = timeStamp,
            error = "No error",
            errorCode = null,
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
            appData = "appData"
        )
}
