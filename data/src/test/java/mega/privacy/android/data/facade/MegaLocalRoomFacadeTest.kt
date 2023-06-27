package mega.privacy.android.data.facade

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.mapper.contact.ContactEntityMapper
import mega.privacy.android.data.mapper.contact.ContactModelMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferModelMapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaLocalRoomFacadeTest {

    private lateinit var underTest: MegaLocalRoomFacade

    private val contactDao = mock<ContactDao>()
    private val contactEntityMapper = mock<ContactEntityMapper>()
    private val contactModelMapper = mock<ContactModelMapper>()
    private val completedTransferDao = mock<CompletedTransferDao>()
    private val completedTransferModelMapper = mock<CompletedTransferModelMapper>()
    private val encryptData = mock<EncryptData>()
    private val activeTransferDao = mock<ActiveTransferDao>()
    private val activeTransferMapper = mock<ActiveTransferMapper>()
    private val activeTransferEntityMapper = mock<ActiveTransferEntityMapper>()

    @BeforeAll
    fun setUp() {
        underTest = MegaLocalRoomFacade(
            contactDao,
            contactEntityMapper,
            contactModelMapper,
            completedTransferDao,
            activeTransferDao,
            completedTransferModelMapper,
            activeTransferMapper,
            activeTransferEntityMapper,
            encryptData,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            contactDao,
            contactEntityMapper,
            contactModelMapper,
            completedTransferDao,
            completedTransferModelMapper,
            encryptData,
            activeTransferDao,
            activeTransferMapper,
            activeTransferEntityMapper,
        )
    }

    @Test
    fun `test that getAllCompletedTransfers returns a list of completed transfers ordered by timestamp descendant`() =
        runTest {
            val completedTransferEntities = listOf<CompletedTransferEntity>(
                mock(), mock(), mock(),
            )
            val completedTransfers = listOf<CompletedTransfer>(
                mock { on { timestamp }.thenReturn(1684228012974) },
                mock { on { timestamp }.thenReturn(1684228012975) },
                mock { on { timestamp }.thenReturn(1684228012973) },
            )

            whenever(completedTransferDao.getAllCompletedTransfers())
                .thenReturn(flowOf(completedTransferEntities))
            completedTransferEntities.forEachIndexed { index, completedTransferEntity ->
                whenever(completedTransferModelMapper(completedTransferEntity)).thenReturn(
                    completedTransfers[index]
                )
            }

            val expected =
                listOf(completedTransfers[1], completedTransfers[0], completedTransfers[2])

            assertThat(underTest.getAllCompletedTransfers().single()).isEqualTo(expected)
        }


    @Test
    fun `test that getAllCompletedTransfers returns a list of completed transfers with size max elements`() =
        runTest {
            val expectedSize = 2
            val completedTransferEntities = listOf<CompletedTransferEntity>(
                mock(), mock(), mock(), mock()
            )

            whenever(completedTransferDao.getAllCompletedTransfers()).thenReturn(
                flowOf(completedTransferEntities)
            )
            completedTransferEntities.forEach { entity ->
                val completedTransfer = mock<CompletedTransfer> {
                    on { timestamp }.thenReturn(1684228012974)
                }
                whenever(completedTransferModelMapper(entity)).thenReturn(completedTransfer)
            }

            assertThat(underTest.getAllCompletedTransfers(expectedSize).single().size)
                .isEqualTo(expectedSize)
        }

    @Test
    fun `test that getAllCompletedTransfers returns all completed transfers if the size parameter is null`() =
        runTest {
            val completedTransferEntities = listOf<CompletedTransferEntity>(
                mock(), mock(), mock(), mock()
            )

            whenever(completedTransferDao.getAllCompletedTransfers()).thenReturn(
                flowOf(completedTransferEntities)
            )
            completedTransferEntities.forEach { entity ->
                val completedTransfer = mock<CompletedTransfer> {
                    on { timestamp }.thenReturn(1684228012974)
                }
                whenever(completedTransferModelMapper(entity)).thenReturn(completedTransfer)
            }

            assertThat(underTest.getAllCompletedTransfers().single().size)
                .isEqualTo(completedTransferEntities.size)
        }
}
