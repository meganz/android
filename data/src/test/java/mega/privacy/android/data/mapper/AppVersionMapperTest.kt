package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AppVersion
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppVersionMapperTest {
    private lateinit var underTest: AppVersionMapper

    @BeforeAll
    fun init() {
        underTest = AppVersionMapper()
    }

    @Test
    fun `test that a full version string is mapped to an AppVersion with debug suffix`() {
        val result = underTest("14.2(9999_debug)")
        assertThat(result).isEqualTo(AppVersion(major = 14, minor = 2, patch = null))
    }

    @Test
    fun `test that a full version string is mapped to an AppVersion with all fields`() {
        val result = underTest("14.2.1")
        assertThat(result).isEqualTo(AppVersion(major = 14, minor = 2, patch = 1))
    }

    @Test
    fun `test that a version string without patch is mapped to an AppVersion with null patch`() {
        val result = underTest("14.2")
        assertThat(result).isEqualTo(AppVersion(major = 14, minor = 2, patch = null))
    }

    @Test
    fun `test that a non-numeric version string returns null`() {
        val result = underTest("invalid")
        assertThat(result).isNull()
    }

    @Test
    fun `test that an empty version string returns null`() {
        val result = underTest("")
        assertThat(result).isNull()
    }

    @Test
    fun `test that an AppVersion with patch is mapped to the correct version string`() {
        val result = underTest(AppVersion(major = 14, minor = 2, patch = 1))
        assertThat(result).isEqualTo("14.2.1")
    }

    @Test
    fun `test that an AppVersion with null patch is mapped to a string with null`() {
        val result = underTest(AppVersion(major = 14, minor = 2, patch = null))
        assertThat(result).isEqualTo("14.2")
    }
}
