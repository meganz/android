package mega.privacy.android.data.mapper.offline

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.OfflineEntity
import mega.privacy.android.domain.entity.Offline
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class OfflineEntityMapperTest {
    private val encryptData: EncryptData = mock()
    private lateinit var underTest: OfflineEntityMapper

    @Before
    fun setUp() {
        underTest = OfflineEntityMapper(encryptData)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val model = Offline(
            id = 1,
            handle = "Some handle",
            name = "Rohit",
            path = "Mega/cloud",
            parentId = 1234,
            type = "File",
            origin = 0,
            handleIncoming = "NO",
        )
        val expected = OfflineEntity(
            encryptedHandle = "Some handle",
            encryptedName = "Rohit",
            encryptedPath = "Mega/cloud",
            parentId = 1234,
            encryptedType = "File",
            incoming = 0,
            encryptedIncomingHandle = "NO",
            lastModifiedTime = 0
        )
        whenever(encryptData(model.handle)).thenReturn(expected.encryptedHandle)
        whenever(encryptData(model.name)).thenReturn(expected.encryptedName)
        whenever(encryptData(model.path)).thenReturn(expected.encryptedPath)
        whenever(encryptData(model.type)).thenReturn(expected.encryptedType)
        whenever(encryptData(model.handleIncoming)).thenReturn(expected.encryptedIncomingHandle)
        Truth.assertThat(underTest(model)).isEqualTo(expected)
    }
}