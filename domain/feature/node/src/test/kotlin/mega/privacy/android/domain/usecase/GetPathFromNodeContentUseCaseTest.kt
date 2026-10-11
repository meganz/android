package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeContentUri
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPathFromNodeContentUseCaseTest {
    private lateinit var underTest: GetPathFromNodeContentUseCase

    @TempDir
    lateinit var temporaryFolder: File

    @BeforeAll
    fun setUp() {
        underTest = GetPathFromNodeContentUseCase(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that when url file is local then it returns path`() = runTest {
        val file = File(temporaryFolder.path, "somefile.txt")
        file.createNewFile()
        val localContentUri = NodeContentUri.LocalContentUri(file)
        val path = underTest(localContentUri)
        Truth.assertThat(path).isNull()
    }

    @Test
    fun `test that when url is remove then it returns path`() = runTest {
        val remoteContentUri = NodeContentUri.RemoteContentUri("http://127.0.0.1:4443/", false)
        val path = underTest(remoteContentUri)
        Truth.assertThat(path).isNull()
    }
}