package mega.privacy.android.app.presentation.transfers.notification

import android.content.Context
import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActionGroupFinishNotificationTitleMapperTest {
    private lateinit var underTest: ActionGroupFinishNotificationTitleMapper

    private val context = mock<Context>()
    private val resources = mock<Resources>()

    @BeforeAll
    fun setup() {
        underTest = ActionGroupFinishNotificationTitleMapper(context)
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            context,
            resources,
        )
        whenever(context.resources) doReturn resources
    }

    @Test
    fun `test that notification title is generated correctly when multiple files download completes`() =
        runTest {
            val actionGroup =
                createGroup(
                    fileNames = listOf("file1", "file2"),
                    finishedFiles = 2,
                    completedFiles = 2
                )
            val expected = "foo"
            whenever(
                resources.getQuantityString(
                    R.plurals.download_service_final_notification,
                    actionGroup.completedFiles,
                    actionGroup.completedFiles,
                )
            ) doReturn expected

            val actual = underTest(
                isDownload = true,
                isPreviewDownload = false,
                titleSuffix = null,
                actionGroup = actionGroup
            )

            assertThat(actual).isEqualTo(expected)

        }

    @Test
    fun `test that notification title is generated correctly when single file download completes`() =
        runTest {
            val actionGroup =
                createGroup(fileNames = listOf("file1"), finishedFiles = 1, completedFiles = 1)
            val expected = "foo"
            whenever(
                resources.getString(
                    sharedR.string.transfers_notification_title_single_download,
                    actionGroup.singleFileName,
                )
            ) doReturn expected

            val actual = underTest(
                isDownload = true,
                isPreviewDownload = false,
                titleSuffix = null,
                actionGroup = actionGroup
            )

            assertThat(actual).isEqualTo(expected)

        }

    @Test
    fun `test that notification title is generated correctly when preview download completes`() =
        runTest {
            val actionGroup =
                createGroup(fileNames = listOf("file1"), finishedFiles = 1, completedFiles = 1)
            val expected = "foo"
            whenever(
                resources.getString(
                    sharedR.string.transfers_notification_title_preview_download,
                    actionGroup.singleFileName
                )
            ) doReturn expected

            val actual = underTest(
                isDownload = true,
                isPreviewDownload = true,
                titleSuffix = null,
                actionGroup = actionGroup
            )

            assertThat(actual).isEqualTo(expected)

        }

    @Test
    fun `test that notification title is generated correctly when multiple files download finishes with incomplete downloads`() =
        runTest {
            val actionGroup =
                createGroup(
                    fileNames = listOf("file1", "file2"),
                    finishedFiles = 2,
                    completedFiles = 1,
                )
            val expected = "foo"
            whenever(
                resources.getQuantityString(
                    R.plurals.download_service_final_notification_with_details,
                    actionGroup.finishedFiles,
                    actionGroup.completedFiles,
                    actionGroup.finishedFiles,
                )
            ) doReturn expected

            val actual = underTest(
                isDownload = true,
                isPreviewDownload = false,
                titleSuffix = null,
                actionGroup = actionGroup
            )

            assertThat(actual).isEqualTo(expected)

        }

    private fun createGroup(
        groupId: Int = 1,
        completedFiles: Int = 0,
        finishedFiles: Int = 0,
        fileNames: List<String> = emptyList(),
    ) = ActiveTransferTotals.ActionGroup(
        groupId = groupId,
        totalFiles = 0,
        finishedFiles = finishedFiles,
        completedFiles = completedFiles,
        alreadyTransferred = 0,
        destination = "",
        fileNames = fileNames,
        singleTransferTag = null,
        startTime = 0,
        pausedFiles = 0,
        totalBytes = 0L,
        transferredBytes = 0L,
        pendingTransferNodeId = null,
    )
}