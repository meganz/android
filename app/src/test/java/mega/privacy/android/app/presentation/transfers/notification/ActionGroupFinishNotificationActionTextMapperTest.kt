package mega.privacy.android.app.presentation.transfers.notification

import android.content.Context
import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsContentUriUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
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
class ActionGroupFinishNotificationActionTextMapperTest {
    private lateinit var underTest: ActionGroupFinishNotificationActionTextMapper

    private val context = mock<Context>()
    private val resources = mock<Resources>()

    @BeforeAll
    fun setup() {
        underTest = ActionGroupFinishNotificationActionTextMapper(
            context,
        )
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
    fun `test that action text is null when there are no files`() = runTest {
        val actionGroup = createGroup()

        val actual = underTest(
            isLoggedIn = true,
            isPreviewDownload = false,
            isOfflineDownload = false,
            actionGroup = actionGroup
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `test that preview action text is returned when it is a preview download`() = runTest {
        val actionGroup = createGroup(completedFiles = 1)
        val expected = "foo"
        whenever(resources.getString(sharedR.string.transfers_notification_preview_action)) doReturn expected

        val actual = underTest(
            isLoggedIn = true,
            isPreviewDownload = true,
            isOfflineDownload = false,
            actionGroup = actionGroup
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that locate action text is returned when user is logged`() = runTest {
        val actionGroup = createGroup(completedFiles = 1)
        val expected = "foo"
        whenever(resources.getString(sharedR.string.transfers_notification_location_action)) doReturn expected

        val actual = underTest(
            isLoggedIn = true,
            isPreviewDownload = false,
            isOfflineDownload = false,
            actionGroup = actionGroup
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that locate action text is returned when user is not logged but is not an offline download`() =
        runTest {
            val actionGroup = createGroup(completedFiles = 1)
            val expected = "foo"
            whenever(resources.getString(sharedR.string.transfers_notification_location_action)) doReturn expected

            val actual = underTest(
                isLoggedIn = false,
                isPreviewDownload = false,
                isOfflineDownload = false,
                actionGroup = actionGroup
            )

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that locate action is null when user is not logged and is an offline download`() =
        runTest {
            val actionGroup = createGroup(completedFiles = 1)
            val expected = "foo"
            whenever(resources.getString(sharedR.string.transfers_notification_location_action)) doReturn expected

            val actual = underTest(
                isLoggedIn = false,
                isPreviewDownload = false,
                isOfflineDownload = true,
                actionGroup = actionGroup
            )

            assertThat(actual).isNull()
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