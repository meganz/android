package mega.privacy.android.data.mapper.notification

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.notifications.PromoNotification
import nz.mega.sdk.MegaNotification
import nz.mega.sdk.MegaNotificationList
import nz.mega.sdk.MegaStringMap
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PromoNotificationListMapperTest {

    private lateinit var underTest: PromoNotificationListMapper

    @BeforeAll
    fun setup() {
        underTest = PromoNotificationListMapper()
    }

    @Test
    fun `test that promo notification list mapper returns correctly`() {
        val promoNotification = PromoNotification(
            promoID = 1L,
            title = "title",
            description = "description",
            imageName = "imageName",
            imageURL = "imageURL",
            startTimeStamp = 1L,
            endTimeStamp = 2L,
            actionName = "actionName",
            actionURL = "actionURL"
        )
        val callToAction1Mock = mock<MegaStringMap> {
            on { get("text") } doReturn promoNotification.actionName
            on { get("link") } doReturn promoNotification.actionURL
        }
        val callToAction2Mock = mock<MegaStringMap> {
            on { get("text") } doReturn "actionName1"
            on { get("link") } doReturn "actionURL1"
        }
        val megaNotification = mock<MegaNotification> {
            on { id } doReturn 1L
            on { title } doReturn promoNotification.title
            on { description } doReturn promoNotification.description
            on { imageName } doReturn promoNotification.imageName
            on { imagePath } doReturn promoNotification.imageURL
            on { start } doReturn 1L
            on { end } doReturn 2L
            on { callToAction1 }.thenReturn(callToAction1Mock)
            on { callToAction2 }.thenReturn(callToAction2Mock)
        }
        val megaNotificationList = mock<MegaNotificationList> {
            on { size() } doReturn 1
            on { get(0) }.thenReturn(megaNotification)
        }
        assertThat(underTest.invoke(megaNotificationList))
            .isEqualTo(listOf(promoNotification))
    }
}