package mega.privacy.android.data.mapper.verification

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.verification.OptInVerification
import mega.privacy.android.domain.entity.verification.Unblock
import mega.privacy.android.domain.exception.mapper.UnknownMapperParameterException
import org.junit.Before
import org.junit.Test

class SmsPermissionMapperImplTest {
    private lateinit var underTest: SmsPermissionMapper

    @Before
    fun setUp() {
        underTest = SmsPermissionMapperImpl()
    }

    @Test
    fun `test that 0 returns an empty list`() {
        Truth.assertThat(underTest(0)).isEmpty()
    }

    @Test
    fun `test that 1 returns only unblock`() {
        Truth.assertThat(underTest(1)).containsExactly(Unblock)

    }

    @Test
    fun `test that 2 returns unblock and opt in verification`() {
        Truth.assertThat(underTest(2)).containsExactly(Unblock, OptInVerification)
    }

    @Test(expected = UnknownMapperParameterException::class)
    fun `test that a different input throws an error`() {
        underTest(3)
    }
}