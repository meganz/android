package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.user.UserChanges
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserChangeMapperTest {
    private lateinit var underTest: UserChangeMapper

    @BeforeAll
    fun setup() {
        underTest = UserChangeMapper()
    }

    @Test
    fun `test that invoke returns empty list when changeFlags is 0`() {
        assertTrue(underTest(0).isEmpty())
    }

    @Test
    fun `test that invoke returns list of UserChanges`() {
        val changeFlags = MegaUser.CHANGE_TYPE_AUTHRING.toLong()
        val expectedUserChanges = listOf(UserChanges.AuthenticationInformation)
        assertEquals(expectedUserChanges, underTest(changeFlags))
    }

    @Test
    fun `test that invoke returns list of UserChanges when changeFlags contains multiple changes`() {
        val changeFlags =
            MegaUser.CHANGE_TYPE_AUTHRING.toLong() or MegaUser.CHANGE_TYPE_AVATAR.toLong()
        val expectedUserChanges = listOf(UserChanges.AuthenticationInformation, UserChanges.Avatar)
        assertEquals(expectedUserChanges, underTest(changeFlags))
    }
}