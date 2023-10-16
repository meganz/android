package mega.privacy.android.data.mapper.offline

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.OfflineEntity
import mega.privacy.android.domain.entity.Offline
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class OfflineModelMapperTest {
    private val decryptData: DecryptData = mock()
    private lateinit var underTest: OfflineModelMapper

    @Before
    fun setUp() {
        underTest = OfflineModelMapper(decryptData)
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val model = OfflineEntity(
            encryptedHandle = "Some handle",
            encryptedName = "Rohit",
            encryptedPath = "Mega/cloud",
            parentId = 1234,
            encryptedType = "File",
            incoming = 0,
            encryptedIncomingHandle = "NO",
            lastModifiedTime = 0
        )
        val expected = Offline(
            id = -1,
            handle = "Some handle",
            name = "Rohit",
            path = "Mega/cloud",
            parentId = 1234,
            type = "File",
            origin = 0,
            handleIncoming = "NO",
        )

        whenever(decryptData(model.encryptedName)).thenReturn(expected.name)
        whenever(decryptData(model.encryptedHandle)).thenReturn(expected.handle)
        whenever(decryptData(model.encryptedPath)).thenReturn(expected.path)
        whenever(decryptData(model.encryptedType)).thenReturn(expected.type)
        whenever(decryptData(model.encryptedIncomingHandle)).thenReturn(expected.handleIncoming)
        Truth.assertThat(underTest(model)).isEqualTo(expected)
    }
}