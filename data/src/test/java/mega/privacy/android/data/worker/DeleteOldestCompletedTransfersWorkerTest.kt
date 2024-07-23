package mega.privacy.android.data.worker

import android.content.Context
import androidx.work.ProgressUpdater
import androidx.work.SystemClock
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkDatabase
import androidx.work.impl.utils.WorkForegroundUpdater
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteOldestCompletedTransfersWorkerTest {
    private lateinit var underTest: DeleteOldestCompletedTransfersWorker

    private val context = mock<Context>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val workProgressUpdater = mock<ProgressUpdater>()

    private lateinit var executor: Executor
    private lateinit var workExecutor: WorkManagerTaskExecutor
    private lateinit var workDatabase: WorkDatabase

    @BeforeAll
    fun init() {
        val ioDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(ioDispatcher)
        executor = Executors.newSingleThreadExecutor()
        workExecutor = WorkManagerTaskExecutor(executor)
        workDatabase =
            WorkDatabase.create(context, workExecutor.serialTaskExecutor, SystemClock(), true)

        underTest = DeleteOldestCompletedTransfersWorker(
            context,
            workerParams = WorkerParameters(
                UUID.randomUUID(),
                workDataOf(),
                emptyList(),
                WorkerParameters.RuntimeExtras(),
                1,
                1,
                executor,
                workExecutor,
                WorkerFactory.getDefaultWorkerFactory(),
                workProgressUpdater,
                WorkForegroundUpdater(
                    workDatabase,
                    { _, _ -> }, workExecutor
                )
            ),
            megaLocalRoomGateway
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            workProgressUpdater,
            megaLocalRoomGateway,
        )
    }

    @Test
    fun `test that migrate Legacy Completed Transfers is invoked when worker start work`() =
        runTest {
            underTest.doWork()
            verify(megaLocalRoomGateway).migrateLegacyCompletedTransfers()
        }

    @Test
    fun `test that delete Oldest Completed Transfers is invoked when worker start work`() =
        runTest {
            underTest.doWork()
            verify(megaLocalRoomGateway).deleteOldestCompletedTransfers()
        }
}