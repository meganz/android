package mega.privacy.android.app.presentation.recentactions.mapper

import android.content.Context
import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.TimeUtils.formatBucketDate
import mega.privacy.android.app.utils.TimeUtils.formatTime
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentActionBucketUiEntityMapperTest {
    private val context = mock<Context>()
    private val resources = mock<Resources>()
    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val timeUtilsMock = Mockito.mockStatic(TimeUtils::class.java)
    private val underTest: RecentActionBucketUiEntityMapper = RecentActionBucketUiEntityMapper(
        context,
        fileTypeIconMapper,
    )

    private val imageNode1 = mock<TypedFileNode> {
        on { name }.thenReturn("testFile.jpeg")
        on { type }.thenReturn(
            StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "jpeg",
            )
        )
        on { isFavourite }.thenReturn(true)
    }
    private val imageNode2 = mock<TypedFileNode> {
        on { name }.thenReturn("testFile2.jpeg")
        on { type }.thenReturn(
            StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "jpeg",
            )
        )
    }

    @BeforeAll
    fun setup() {
        whenever(context.resources).thenReturn(resources)
        timeUtilsMock.`when`<String> {
            formatTime(100L)
        }.thenReturn("12:20 PM")
        timeUtilsMock.`when`<String> {
            formatBucketDate(100L, context)
        }.thenReturn("12 April 2024")
    }

    @Test
    fun `test that RecentActionBucketUiEntity can be mapped correctly`() = runTest {
        val recentActionBucket = mock<RecentActionBucket> {
            on { timestamp }.thenReturn(100L)
            on { userEmail }.thenReturn("test@example.com")
            on { parentNodeId }.thenReturn(NodeId(123456L))
            on { isUpdate }.thenReturn(true)
            on { isMedia }.thenReturn(true)
            on { nodes }.thenReturn(listOf(imageNode1))
            on { userName }.thenReturn("testUser")
            on { parentFolderName }.thenReturn("testFolder")
            on { currentUserIsOwner }.thenReturn(true)
            on { isKeyVerified }.thenReturn(true)
            on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.NONE)
        }

        val recentActionBucketUiEntity = underTest(recentActionBucket)

        assertThat(recentActionBucketUiEntity.bucket).isEqualTo(recentActionBucket)
        assertThat(recentActionBucketUiEntity.firstLineText).isEqualTo("testFile.jpeg")
        assertThat(recentActionBucketUiEntity.icon).isNotNull()
        assertThat(recentActionBucketUiEntity.shareIcon).isNull()
        assertThat(recentActionBucketUiEntity.actionIcon).isEqualTo(R.drawable.ic_versions_small)
        assertThat(recentActionBucketUiEntity.parentFolderName).isEqualTo("testFolder")
        assertThat(recentActionBucketUiEntity.showMenuButton).isTrue()
        assertThat(recentActionBucketUiEntity.date).isNotEmpty()
        assertThat(recentActionBucketUiEntity.time).isNotEmpty()
        assertThat(recentActionBucketUiEntity.updatedByText).isNull()
        assertThat(recentActionBucketUiEntity.isFavourite).isTrue()
        assertThat(recentActionBucketUiEntity.labelColor).isNull()
    }

    @Test
    fun `test that Undecrypted label is shown when key is not verified`() = runTest {
        val recentActionBucket = mock<RecentActionBucket> {
            on { timestamp }.thenReturn(100L)
            on { userEmail }.thenReturn("test@example.com")
            on { parentNodeId }.thenReturn(NodeId(123456L))
            on { isUpdate }.thenReturn(true)
            on { isMedia }.thenReturn(true)
            on { nodes }.thenReturn(listOf(imageNode1))
            on { userName }.thenReturn("testUser")
            on { parentFolderName }.thenReturn("testFolder")
            on { currentUserIsOwner }.thenReturn(true)
            on { isKeyVerified }.thenReturn(false)
            on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.NONE)
        }

        val expectedFirstLine = "[Undecrypted file]"
        val expectedParentFolder = "[Undecrypted folder]"
        whenever(
            context.resources.getQuantityString(
                R.plurals.cloud_drive_undecrypted_file,
                1
            )
        ).thenReturn(expectedFirstLine)
        whenever(context.getString(R.string.shared_items_verify_credentials_undecrypted_folder)).thenReturn(
            expectedParentFolder
        )

        val recentActionBucketUiEntity = underTest(recentActionBucket)

        assertThat(recentActionBucketUiEntity.firstLineText).isEqualTo(expectedFirstLine)
        assertThat(recentActionBucketUiEntity.parentFolderName).isEqualTo(expectedParentFolder)
    }

    @Test
    fun `test that media title is shown when there are multiple media nodes`() = runTest {
        val recentActionBucket = mock<RecentActionBucket> {
            on { timestamp }.thenReturn(100L)
            on { userEmail }.thenReturn("test@example.com")
            on { parentNodeId }.thenReturn(NodeId(123456L))
            on { isUpdate }.thenReturn(true)
            on { isMedia }.thenReturn(true)
            on { nodes }.thenReturn(listOf(imageNode1, imageNode2))
            on { userName }.thenReturn("testUser")
            on { parentFolderName }.thenReturn("testFolder")
            on { currentUserIsOwner }.thenReturn(true)
            on { isKeyVerified }.thenReturn(true)
            on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.NONE)
        }

        whenever(
            resources.getQuantityString(
                R.plurals.title_media_bucket_only_images,
                2,
                2
            )
        ).thenReturn("2 Images")

        val recentActionBucketUiEntity = underTest(recentActionBucket)

        assertThat(recentActionBucketUiEntity.firstLineText).isEqualTo("2 Images")
    }

    @Test
    fun `test that stack icon is set when there are multiple media nodes`() = runTest {
        val recentActionBucket = mock<RecentActionBucket> {
            on { timestamp }.thenReturn(100L)
            on { userEmail }.thenReturn("test@example.com")
            on { parentNodeId }.thenReturn(NodeId(123456L))
            on { isUpdate }.thenReturn(true)
            on { isMedia }.thenReturn(true)
            on { nodes }.thenReturn(listOf(imageNode1, imageNode2))
            on { userName }.thenReturn("testUser")
            on { parentFolderName }.thenReturn("testFolder")
            on { currentUserIsOwner }.thenReturn(true)
            on { isKeyVerified }.thenReturn(true)
            on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.NONE)
        }

        whenever(context.getString(any())).thenReturn("text")
        whenever(resources.getQuantityString(any(), any(), any())).thenReturn("quantity text")

        val recentActionBucketUiEntity = underTest(recentActionBucket)

        assertThat(recentActionBucketUiEntity.icon).isEqualTo(mega.privacy.android.icon.pack.R.drawable.ic_image_stack_medium_solid)
    }

    @Test
    fun `test that updatedByText is set when current user is not the owner of the node`() =
        runTest {
            val recentActionBucket = mock<RecentActionBucket> {
                on { timestamp }.thenReturn(100L)
                on { userEmail }.thenReturn("test@example.com")
                on { parentNodeId }.thenReturn(NodeId(123456L))
                on { isUpdate }.thenReturn(true)
                on { isMedia }.thenReturn(true)
                on { nodes }.thenReturn(listOf(imageNode1))
                on { userName }.thenReturn("testUser")
                on { parentFolderName }.thenReturn("testFolder")
                on { currentUserIsOwner }.thenReturn(false)
                on { isKeyVerified }.thenReturn(false)
                on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.NONE)
            }
            val expected = "[A]updated by [/A]testUser"
            whenever(context.getString(any())).thenReturn("text")
            whenever(resources.getQuantityString(any(), any())).thenReturn("quantity text")
            whenever(context.getString(R.string.update_action_bucket, "testUser")).thenReturn(
                expected
            )

            val recentActionBucketUiEntity = underTest(recentActionBucket)

            assertThat(recentActionBucketUiEntity.updatedByText).isEqualTo(expected)
        }


    @Test
    fun `test that updatedByText is set when current user is not the owner of the node and update is false`() =
        runTest {
            val recentActionBucket = mock<RecentActionBucket> {
                on { timestamp }.thenReturn(100L)
                on { userEmail }.thenReturn("test@example.com")
                on { parentNodeId }.thenReturn(NodeId(123456L))
                on { isUpdate }.thenReturn(false)
                on { isMedia }.thenReturn(true)
                on { nodes }.thenReturn(listOf(imageNode1))
                on { userName }.thenReturn("testUser")
                on { parentFolderName }.thenReturn("testFolder")
                on { currentUserIsOwner }.thenReturn(false)
                on { isKeyVerified }.thenReturn(true)
                on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.NONE)
            }

            val expected = "[A]created by [/A]testUser"
            whenever(context.getString(R.string.create_action_bucket, "testUser")).thenReturn(
                expected
            )

            val recentActionBucketUiEntity = underTest(recentActionBucket)

            assertThat(recentActionBucketUiEntity.updatedByText).isEqualTo(expected)
        }

    @Test
    fun `test that shareIcon is set when there parent folder shares type is not none`() = runTest {
        val recentActionBucket = mock<RecentActionBucket> {
            on { timestamp }.thenReturn(100L)
            on { userEmail }.thenReturn("test@example.com")
            on { parentNodeId }.thenReturn(NodeId(123456L))
            on { isUpdate }.thenReturn(false)
            on { isMedia }.thenReturn(true)
            on { nodes }.thenReturn(listOf(imageNode1))
            on { userName }.thenReturn("testUser")
            on { parentFolderName }.thenReturn("testFolder")
            on { currentUserIsOwner }.thenReturn(false)
            on { isKeyVerified }.thenReturn(false)
            on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.INCOMING_SHARES)
        }
        whenever(resources.getQuantityString(any(), any())).thenReturn("quantity text")

        val recentActionBucketUiEntity = underTest(recentActionBucket)

        assertThat(recentActionBucketUiEntity.shareIcon).isEqualTo(mega.privacy.android.icon.pack.R.drawable.ic_folder_incoming_medium_solid)
    }

    @Test
    fun `test that actionIcon is set correctly when isUpdate is false`() = runTest {
        val recentActionBucket = mock<RecentActionBucket> {
            on { timestamp }.thenReturn(100L)
            on { userEmail }.thenReturn("test@example.com")
            on { parentNodeId }.thenReturn(NodeId(123456L))
            on { isUpdate }.thenReturn(false)
            on { isMedia }.thenReturn(true)
            on { nodes }.thenReturn(listOf(imageNode1))
            on { userName }.thenReturn("testUser")
            on { parentFolderName }.thenReturn("testFolder")
            on { currentUserIsOwner }.thenReturn(false)
            on { isKeyVerified }.thenReturn(true)
            on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.INCOMING_SHARES)
        }

        val recentActionBucketUiEntity = underTest(recentActionBucket)

        assertThat(recentActionBucketUiEntity.actionIcon).isEqualTo(R.drawable.ic_recents_up)
    }

    @Test
    fun `test that showMenuButton and favorite are set false when bucket has multiple nodes`() =
        runTest {
            val recentActionBucket = mock<RecentActionBucket> {
                on { timestamp }.thenReturn(100L)
                on { userEmail }.thenReturn("test@example.com")
                on { parentNodeId }.thenReturn(NodeId(123456L))
                on { isUpdate }.thenReturn(false)
                on { isMedia }.thenReturn(true)
                on { nodes }.thenReturn(listOf(imageNode1, imageNode2))
                on { userName }.thenReturn("testUser")
                on { parentFolderName }.thenReturn("testFolder")
                on { currentUserIsOwner }.thenReturn(false)
                on { isKeyVerified }.thenReturn(false)
                on { parentFolderSharesType }.thenReturn(RecentActionsSharesType.INCOMING_SHARES)
            }
            whenever(resources.getQuantityString(any(), any())).thenReturn("quantity text")

            val recentActionBucketUiEntity = underTest(recentActionBucket)

            assertThat(recentActionBucketUiEntity.isFavourite).isFalse()
            assertThat(recentActionBucketUiEntity.showMenuButton).isFalse()
        }

    @AfterEach
    fun resetMocks() {
        reset(fileTypeIconMapper, resources)
    }

    @AfterAll
    fun tearDown() {
        timeUtilsMock.close()
        reset(context)
    }
}