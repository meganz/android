package mega.privacy.android.feature.sync.data.gateway

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.dao.UserPausedSyncsDao
import mega.privacy.android.data.database.entity.UserPausedSyncEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserPausedSyncGatewayImplTest {

    private lateinit var underTest: UserPausedSyncGatewayImpl
    private val userPausedSyncDao = mock<UserPausedSyncsDao>()

    @BeforeEach
    fun setUp() {
        underTest = UserPausedSyncGatewayImpl(userPausedSyncDao)
    }

    @Test
    fun `test that setUserPausedSync calls insertPausedSync on DAO`() = runTest {
        val syncId = 123L

        underTest.setUserPausedSync(syncId)

        verify(userPausedSyncDao).insertPausedSync(UserPausedSyncEntity(syncId))
    }

    @Test
    fun `test that getUserPausedSync calls getUserPausedSync on DAO and return its result`() =
        runTest {
            val syncId = 123L
            val userPausedSyncEntity = UserPausedSyncEntity(syncId)
            whenever(userPausedSyncDao.getUserPausedSync(syncId)).thenReturn(userPausedSyncEntity)

            val result = underTest.getUserPausedSync(syncId)

            verify(userPausedSyncDao).getUserPausedSync(syncId)
            Truth.assertThat(result).isEqualTo(userPausedSyncEntity)
        }

    @Test
    fun `test that deleteUserPausedSync calls deleteUserPausedSync on DAO`() = runTest {
        val syncId = 123L

        underTest.deleteUserPausedSync(syncId)

        verify(userPausedSyncDao).deleteUserPausedSync(syncId)
    }
}
