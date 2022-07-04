package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetChildrenNode
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetChildrenNodeTest {

    private lateinit var underTest: GetChildrenNode
    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetChildrenNode(filesRepository)
    }

    @Test
    fun `test that get children node repository is invoked when underTest is invoked`() = runTest {
        val parent = mock<MegaNode>()
        val order = null
        underTest(parent, order)

        verify(filesRepository).getChildrenNode(parent, order)
    }

    @Test
    fun `test that underTest returns the value of get children node repository`() = runTest {
        val parent = mock<MegaNode>()
        val order = null
        whenever(filesRepository.getChildrenNode(parent, order)).thenReturn(listOf(mock(), mock()))

        assertTrue { underTest(parent, order).size == 2 }
    }
}