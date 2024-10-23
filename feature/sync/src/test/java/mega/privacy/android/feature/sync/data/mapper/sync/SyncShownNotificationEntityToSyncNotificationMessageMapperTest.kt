package mega.privacy.android.feature.sync.data.mapper.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import mega.privacy.android.feature.sync.data.mapper.notification.GenericErrorToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.StalledIssuesToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.SyncShownNotificationEntityToSyncNotificationMessageMapper
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncShownNotificationEntityToSyncNotificationMessageMapperTest {

    private val genericErrorToNotificationMessageMapper: GenericErrorToNotificationMessageMapper =
        mock()
    private val stalledIssuesToNotificationMessageMapper: StalledIssuesToNotificationMessageMapper =
        mock()
    private val json: Json = mock()

    private val underTest = SyncShownNotificationEntityToSyncNotificationMessageMapper(
        genericErrorToNotificationMessageMapper,
        stalledIssuesToNotificationMessageMapper,
        json
    )

    @AfterEach
    fun tearDown() {
        reset(
            genericErrorToNotificationMessageMapper,
            stalledIssuesToNotificationMessageMapper,
            json
        )
    }

    @Test
    fun `test that mapper converts battery low notification to domain entity`() {
        val dbEntity = SyncShownNotificationEntity(
            id = 1,
            notificationType = "BATTERY_LOW",
        )
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.BATTERY_LOW,
            notificationDetails = NotificationDetails(path = "", errorCode = null)
        )
        whenever(genericErrorToNotificationMessageMapper(SyncNotificationType.BATTERY_LOW, "", 0))
            .thenReturn(notificationMessage)

        val result = underTest(dbEntity)

        assertThat(result.syncNotificationType).isEqualTo(SyncNotificationType.BATTERY_LOW)
    }

    @Test
    fun `test that mapper converts not connect to wifi notification to domain entity`() {
        val dbEntity = SyncShownNotificationEntity(
            id = 1,
            notificationType = "NOT_CONNECTED_TO_WIFI",
        )
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.NOT_CONNECTED_TO_WIFI,
            notificationDetails = NotificationDetails(path = "", errorCode = null)
        )
        whenever(
            genericErrorToNotificationMessageMapper(
                SyncNotificationType.NOT_CONNECTED_TO_WIFI,
                "",
                0
            )
        )
            .thenReturn(notificationMessage)

        val result = underTest(dbEntity)

        assertThat(result.syncNotificationType).isEqualTo(SyncNotificationType.NOT_CONNECTED_TO_WIFI)
    }

    @Test
    fun `test that mapper converts error notification to domain entity`() {
        val dbEntity = SyncShownNotificationEntity(
            id = 1,
            notificationType = "ERROR",
            otherIdentifiers = """
                {
                  "path": "/example/path",
                  "errorCode": 3
                }
            """.trimIndent()
        )
        val notificationDetails = NotificationDetails("/example/path", 3)
        val notificationMessage: SyncNotificationMessage = mock()
        whenever(json.decodeFromString<NotificationDetails>(dbEntity.otherIdentifiers ?: ""))
            .thenReturn(notificationDetails)
        whenever(
            genericErrorToNotificationMessageMapper(
                SyncNotificationType.ERROR,
                notificationDetails.path.orEmpty(),
                notificationDetails.errorCode ?: 0
            )
        ).thenReturn(notificationMessage)

        val result = underTest(dbEntity)

        assertThat(result).isEqualTo(notificationMessage)
    }

    @Test
    fun `test that mapper converts stalled issue notification to domain entity`() {
        val dbEntity = SyncShownNotificationEntity(
            id = 1,
            notificationType = "STALLED_ISSUE",
            otherIdentifiers = """
                {
                  "path": "/example/path"
                }
            """.trimIndent()
        )
        val notificationDetails = NotificationDetails("/example/path", null)
        val notificationMessage: SyncNotificationMessage = mock()
        whenever(json.decodeFromString<NotificationDetails>(dbEntity.otherIdentifiers ?: ""))
            .thenReturn(notificationDetails)
        whenever(stalledIssuesToNotificationMessageMapper(notificationDetails.path.orEmpty()))
            .thenReturn(notificationMessage)

        val result = underTest(dbEntity)

        assertThat(result).isEqualTo(notificationMessage)
    }

    @Test
    fun `test that mapper converts from domain to battery low notification`() {
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.BATTERY_LOW,
            notificationDetails = NotificationDetails(path = "", errorCode = null)
        )
        val dbEntity = SyncShownNotificationEntity(
            notificationType = SyncNotificationType.BATTERY_LOW.name,
            otherIdentifiers = null
        )
        whenever(json.encodeToString(notificationMessage.notificationDetails)).thenReturn(null)

        val result = underTest(notificationMessage)

        assertThat(result).isEqualTo(dbEntity)
    }

    @Test
    fun `test that mapper converts from domain to not connect to wifi notification`() {
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.NOT_CONNECTED_TO_WIFI,
            notificationDetails = NotificationDetails(path = "", errorCode = null)
        )
        val dbEntity = SyncShownNotificationEntity(
            notificationType = SyncNotificationType.NOT_CONNECTED_TO_WIFI.name,
            otherIdentifiers = null
        )
        whenever(json.encodeToString(notificationMessage.notificationDetails)).thenReturn(null)

        val result = underTest(notificationMessage)

        assertThat(result).isEqualTo(dbEntity)
    }

    @Test
    fun `test that mapper converts from domain to error notification`() {
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.ERROR,
            notificationDetails = NotificationDetails(path = "/example/path", errorCode = 3)
        )
        val dbEntity = SyncShownNotificationEntity(
            notificationType = SyncNotificationType.ERROR.name,
            otherIdentifiers = """
                {
                  "path": "/example/path",
                  "errorCode": 3
                }
            """.trimIndent()
        )
        whenever(json.encodeToString(notificationMessage.notificationDetails)).thenReturn(dbEntity.otherIdentifiers)

        val result = underTest(notificationMessage)

        assertThat(result).isEqualTo(dbEntity)
    }

    @Test
    fun `test that mapper converts from domain to stalled issue notification`() {
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.STALLED_ISSUE,
            notificationDetails = NotificationDetails(path = "/example/path", errorCode = null)
        )
        val dbEntity = SyncShownNotificationEntity(
            notificationType = SyncNotificationType.STALLED_ISSUE.name,
            otherIdentifiers = """
                {
                  "path": "/example/path"
                }
            """.trimIndent()
        )
        whenever(json.encodeToString(notificationMessage.notificationDetails)).thenReturn(dbEntity.otherIdentifiers)

        val result = underTest(notificationMessage)

        assertThat(result).isEqualTo(dbEntity)
    }
}