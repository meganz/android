package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetParentNodeHandle
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetParentNodeHandleTest {
    private lateinit var underTest: GetParentNodeHandle

    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetParentNodeHandle(
            getNodeByHandle,
            filesRepository,
        )
    }

    @Test
    fun `test that execute will retrieve node`() =
        runTest {
            underTest(any())
            verify(getNodeByHandle).invoke(any())
        }

    @Test
    fun `test that execute will return null when retrieve node is null`() =
        runTest {
            whenever(getNodeByHandle(any())).thenReturn(null)

            assertThat(underTest(any())).isEqualTo(null)
        }

    @Test
    fun `test that invoke retrieve parent node`() =
        runTest {
            val node = mock<MegaNode>()
            whenever(getNodeByHandle(any())).thenReturn(node)
            underTest(any())

            verify(filesRepository).getParentNode(node)
        }

    @Test
    fun `test that invoke will return parent node handle`() =
        runTest {
            val expectedHandle = 123456789L
            val parentNode = mock<MegaNode> {
                on { handle }.thenReturn(expectedHandle)
            }
            whenever(getNodeByHandle(any())).thenReturn(mock())
            whenever(filesRepository.getParentNode(any())).thenReturn(parentNode)

            assertThat(underTest(any())).isEqualTo(expectedHandle)
        }
}