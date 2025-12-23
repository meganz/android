package mega.privacy.android.domain.usecase.recentactions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetRecentActionsUseCaseTest {

    private lateinit var underTest: GetRecentActionsUseCase

    private val recentActionsRepository = mock<RecentActionsRepository>()
    private val contactsRepository = mock<ContactsRepository>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val typedRecentActionBucketMapper = mock<TypedRecentActionBucketMapper>()

    private val dummyNode1 = mock<FileNode> {
        on { id } doReturn NodeId(123L)
        on { isNodeKeyDecrypted }.thenReturn(true)
    }
    private val dummyRecentActionBucketUnTyped = RecentActionBucketUnTyped(
        identifier = "M_false-U_false-D_1970-01-01-UE_aaa@aaa.com-PNH_321",
        timestamp = 0L,
        userEmail = "aaa@aaa.com",
        parentNodeId = NodeId(321L),
        isUpdate = false,
        isMedia = false,
        nodes = listOf(dummyNode1)
    )
    private val dummyRecentActionBucket = mock<RecentActionBucket> {
        on { nodes }.thenReturn(listOf(mock<TypedFileNode>()))
        on { timestamp }.thenReturn(0L)
        on { userEmail }.thenReturn("aaa@aaa.com")
        on { parentNodeId }.thenReturn(NodeId(321L))
        on { isUpdate }.thenReturn(false)
        on { isMedia }.thenReturn(false)
        on { currentUserIsOwner }.thenReturn(false)
        on { userName }.thenReturn("aaa@aaa.com")
        on { parentFolderName }.thenReturn("")
        on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.NONE)
        on { isKeyVerified }.thenReturn(true)
        on { isNodeKeyDecrypted }.thenReturn(true)
    }

    @BeforeEach
    fun setUp() {
        commonStub()
        underTest = GetRecentActionsUseCase(
            recentActionsRepository = recentActionsRepository,
            getCurrentUserEmail = getCurrentUserEmail,
            contactsRepository = contactsRepository,
            typedRecentActionBucketMapper = typedRecentActionBucketMapper,
        )
    }

    private fun commonStub() = runTest {
        whenever(contactsRepository.getAllContactsName()).thenReturn(emptyMap())
        whenever(getCurrentUserEmail(false)).thenReturn("aaa@aaa.com")
        whenever(typedRecentActionBucketMapper(any(), any(), any())).thenReturn(emptyList())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            recentActionsRepository,
            getCurrentUserEmail,
            contactsRepository,
            typedRecentActionBucketMapper,
        )
    }

    @Test
    fun `test that recentActionsRepository getRecentActions is invoked`() = runTest {
        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(emptyList())
        underTest(false)
        verify(recentActionsRepository).getRecentActions(false, 500)
    }

    @Test
    fun `test that typedRecentActionBucketMapper is invoked with correct parameters`() = runTest {
        val buckets = listOf(
            RecentActionBucketUnTyped(
                identifier = "M_false-U_false-D_1970-01-01-UE_aaa@aaa.com-PNH_0",
                timestamp = 0L,
                userEmail = "aaa@aaa.com",
                parentNodeId = NodeId(0L),
                isUpdate = false,
                isMedia = false,
                nodes = listOf(dummyNode1)
            ),
        )
        val visibleContacts = mapOf("aaa@aaa.com" to "Test User")
        val currentUserEmail = "aaa@aaa.com"
        val expectedResult = listOf(dummyRecentActionBucket)

        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(buckets)
        whenever(contactsRepository.getAllContactsName()).thenReturn(visibleContacts)
        whenever(getCurrentUserEmail(false)).thenReturn(currentUserEmail)
        whenever(typedRecentActionBucketMapper(any(), any(), any()))
            .thenReturn(expectedResult)

        val result = underTest(false)

        verify(typedRecentActionBucketMapper).invoke(
            eq(buckets),
            eq(visibleContacts),
            eq(currentUserEmail)
        )
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `test that mapper result is filtered to exclude buckets with no nodes`() = runTest {
        val buckets = listOf(dummyRecentActionBucketUnTyped)
        val bucketWithNodes = dummyRecentActionBucket
        val bucketWithoutNodes = mock<RecentActionBucket> {
            on { nodes }.thenReturn(emptyList<TypedFileNode>())
        }
        val mapperResult = listOf(bucketWithNodes, bucketWithoutNodes)

        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(buckets)
        whenever(typedRecentActionBucketMapper(any(), any(), any())).thenReturn(mapperResult)

        val result = underTest(false)

        // Should only return bucket with nodes
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(bucketWithNodes)
    }

    @Test
    fun `test that getCurrentUserEmail is invoked`() = runTest {
        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(
            listOf(dummyRecentActionBucketUnTyped)
        )
        whenever(typedRecentActionBucketMapper(any(), any(), any())).thenReturn(emptyList())

        underTest(false)

        verify(getCurrentUserEmail).invoke(false)
    }

    @Test
    fun `test that contactsRepository getAllContactsName is invoked`() = runTest {
        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(
            listOf(dummyRecentActionBucketUnTyped)
        )
        whenever(typedRecentActionBucketMapper(any(), any(), any())).thenReturn(emptyList())

        underTest(false)

        verify(contactsRepository).getAllContactsName()
    }

    @Test
    fun `test that visible contacts are passed to mapper`() = runTest {
        val expectedContacts = mapOf("aaa@aaa.com" to "FirstName LastName")
        val buckets = listOf(dummyRecentActionBucketUnTyped)
        val expectedResult = listOf(dummyRecentActionBucket)

        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(buckets)
        whenever(contactsRepository.getAllContactsName()).thenReturn(expectedContacts)
        whenever(typedRecentActionBucketMapper(any(), any(), any()))
            .thenReturn(expectedResult)

        underTest(false)

        verify(typedRecentActionBucketMapper).invoke(eq(buckets), eq(expectedContacts), any())
    }

    @Test
    fun `test that current user email is passed to mapper`() = runTest {
        val currentUserEmail = "current@example.com"
        val buckets = listOf(dummyRecentActionBucketUnTyped)
        val expectedResult = listOf(dummyRecentActionBucket)

        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(buckets)
        whenever(getCurrentUserEmail(false)).thenReturn(currentUserEmail)
        whenever(typedRecentActionBucketMapper(any(), any(), any()))
            .thenReturn(expectedResult)

        underTest(false)

        verify(typedRecentActionBucketMapper).invoke(eq(buckets), any(), eq(currentUserEmail))
    }


    @Test
    fun `test that maxBucketCount is passed to repository`() = runTest {
        val buckets = listOf(dummyRecentActionBucketUnTyped)
        val expectedResult = listOf(dummyRecentActionBucket)

        whenever(recentActionsRepository.getRecentActions(any(), any())).thenReturn(buckets)
        whenever(typedRecentActionBucketMapper(any(), any(), any()))
            .thenReturn(expectedResult)

        underTest(false, maxBucketCount = 10)

        verify(recentActionsRepository).getRecentActions(eq(false), eq(10))
    }
}