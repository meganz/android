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
        val staticURL = "https://eu.static.mega.co.nz/psa/"
        val testImageName = "vpn"
        val promoNotification = PromoNotification(
            promoID = 1L,
            title = "title",
            description = "description",
            iconURL = "$staticURL$testImageName@2x.png",
            imageURL = "$staticURL$testImageName@2x.png",
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
            on { iconName } doReturn testImageName
            on { imageName } doReturn testImageName
            on { imagePath } doReturn staticURL
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

    @Test
    fun `test that promo notification list mapper return iconURL and imageURL as empty ones if iconName and imageName is empty`() {
        val staticURL = "https://eu.static.mega.co.nz/psa/"
        val promoNotification = PromoNotification(
            promoID = 1L,
            title = "title",
            description = "description",
            iconURL = "",
            imageURL = "",
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
            on { iconName } doReturn ""
            on { imageName } doReturn ""
            on { imagePath } doReturn staticURL
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