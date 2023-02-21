package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultCloudExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.GetCloudExplorerSearchNodeUseCase
import mega.privacy.android.app.domain.usecase.GetSearchFromMegaNodeParent
import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetCloudExplorerSearchNodeUseCaseTest {
    private lateinit var underTest: GetCloudExplorerSearchNodeUseCase
    private val megaNodeRepository: MegaNodeRepository = mock()
    private val getSearchFromMegaNodeParent: GetSearchFromMegaNodeParent = mock()
    private val megaCancelToken: MegaCancelToken = mock()

    @Before
    fun setUp() {
        underTest = DefaultCloudExplorerSearchNodeUseCase(
            megaNodeRepository = megaNodeRepository,
            getSearchFromMegaNodeParent = getSearchFromMegaNodeParent
        )
    }

    @Test
    fun `test that when query is null in cloud explorer then it returns empty list`() = runTest {
        val list = underTest(
            query = null,
            megaCancelToken = megaCancelToken,
            parentHandle = -1L,
            parentHandleSearch = -1L
        )
        Truth.assertThat(list).isEmpty()
    }

    @Test
    fun `test that when searched any query in cloud explorer it returns some items in list`() =
        runTest {
            val parentHandle = -1L
            val parent: MegaNode = mock()
            val query = "Some query"
            whenever(megaNodeRepository.getNodeByHandle(parentHandle)).thenReturn(parent)
            whenever(
                getSearchFromMegaNodeParent(
                    query = query,
                    parentHandleSearch = parentHandle,
                    megaCancelToken = megaCancelToken,
                    parent = parent
                )
            ).thenReturn(listOf(mock()))
            val list = underTest(
                query = query,
                megaCancelToken = megaCancelToken,
                parentHandle = parentHandle,
                parentHandleSearch = parentHandle
            )
            verify(megaNodeRepository, times(1)).getNodeByHandle(parentHandle)
            verify(getSearchFromMegaNodeParent, times(1)).invoke(
                query = query,
                parentHandleSearch = parentHandle,
                megaCancelToken = megaCancelToken,
                parent = parent
            )
            Truth.assertThat(list).hasSize(1)
        }
}