package mega.privacy.android.domain.usecase.shares


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.usecase.contact.GetContactUserNameFromDatabaseUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MapNodeToShareUseCaseTest {
    private lateinit var underTest: MapNodeToShareUseCase
    private val getContactUserNameFromDatabaseUseCase: GetContactUserNameFromDatabaseUseCase =
        mock()

    @BeforeAll
    internal fun setUp() {
        underTest = MapNodeToShareUseCase(getContactUserNameFromDatabaseUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getContactUserNameFromDatabaseUseCase)
    }

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    internal fun `test that file node without share data is mapped correctly`() = runTest {
        val actual = underTest(mock<TypedFileNode>(), null)

        assertThat(actual).isInstanceOf(ShareFileNode::class.java)
        assertThat(actual.shareData).isNull()
    }

    @Test
    internal fun `test that file node with share data is mapped correctly`() = runTest {
        val shareData = mock<ShareData>()
        val actual = underTest(mock<TypedFileNode>(), shareData)

        assertThat(actual).isInstanceOf(ShareFileNode::class.java)
        assertThat(actual.shareData).isEqualTo(shareData)
    }

    @Test
    internal fun `test that a folder node without share data is mapped correctly`() = runTest {
        val actual = underTest(mock<TypedFolderNode>(), null)

        assertThat(actual).isInstanceOf(ShareFolderNode::class.java)
        assertThat(actual.shareData).isNull()
    }

    @Test
    internal fun `test that folder with share data is mapped correctly`() = runTest {
        val shareData = mock<ShareData>()
        val actual = underTest(mock<TypedFolderNode>(), shareData)

        assertThat(actual).isInstanceOf(ShareFolderNode::class.java)
        assertThat(actual.shareData).isEqualTo(shareData)
    }

    @Test
    internal fun `test share data with count 1 and verified contact fetches user full name from database`() =
        runTest {
            val shareData = mock<ShareData> {
                on { user } doReturn "john@mail.com"
                on { count } doReturn 1
                on { isVerified } doReturn true
            }
            val folderNode = mock<TypedFolderNode>()
            whenever(getContactUserNameFromDatabaseUseCase(any())).thenReturn("John Doe")
            val actual = underTest(folderNode, shareData)
            assertThat(actual).isInstanceOf(ShareFolderNode::class.java)
            verify(getContactUserNameFromDatabaseUseCase).invoke(shareData.user)
        }
}