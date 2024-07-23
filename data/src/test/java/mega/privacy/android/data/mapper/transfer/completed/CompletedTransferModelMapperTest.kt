package mega.privacy.android.data.mapper.transfer.completed

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CompletedTransferModelMapperTest {
    private lateinit var underTest: CompletedTransferModelMapper

    @BeforeAll
    fun setup() {
        underTest = CompletedTransferModelMapper()
    }

    @Test
    fun `test that mapper returns model correctly when invoke function`() = runTest {
        val entity = CompletedTransferEntity(
            id = 0,
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = 1,
            state = 6,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            isOffline = false,
            timestamp = 1684228012974L,
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
            appData = "appData",
        )
        val expected = CompletedTransfer(
            id = 0,
            fileName = "2023-03-24 00.13.20_1.jpg",
            type = 1,
            state = 6,
            size = "3.57 MB",
            handle = 27169983390750L,
            path = "Cloud drive/Camera uploads",
            isOffline = false,
            timestamp = 1684228012974L,
            error = "No error",
            originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
            parentHandle = 11622336899311L,
            appData = "appData",
        )

        Truth.assertThat(underTest(entity)).isEqualTo(expected)
    }
}
