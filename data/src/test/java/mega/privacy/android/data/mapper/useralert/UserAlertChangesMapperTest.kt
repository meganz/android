package mega.privacy.android.data.mapper.useralert

import mega.privacy.android.domain.entity.useralert.UserAlertChange
import nz.mega.sdk.MegaUserAlert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserAlertChangesMapperTest {

    private lateinit var underTest: UserAlertChangesMapper
    private val userAlert: MegaUserAlert = mock()

    @Before
    fun setUp() {
        underTest = UserAlertChangesMapper()
    }

    @Test
    fun `test that invoke filters UserAlertChange values correctly`() {
        val expectedChanges = listOf(
            UserAlertChange.Title,
            UserAlertChange.Description
        )
        whenever(userAlert.hasSchedMeetingChanged(MegaUserAlert.SM_CHANGE_TYPE_TITLE.toLong())).thenReturn(true)
        whenever(userAlert.hasSchedMeetingChanged(MegaUserAlert.SM_CHANGE_TYPE_DESCRIPTION.toLong())).thenReturn(true)

        val changes = underTest(userAlert)

        assertEquals(expectedChanges, changes)
    }
}
