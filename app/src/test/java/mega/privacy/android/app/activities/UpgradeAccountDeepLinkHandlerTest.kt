package mega.privacy.android.app.activities

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpgradeAccountDeepLinkHandlerTest {
    private lateinit var underTest: UpgradeAccountDeepLinkHandler


    @BeforeAll
    fun setup() {
        underTest = UpgradeAccountDeepLinkHandler()
    }

    @Test
    fun `test that correct nav key is returned when uri matches UPGRADE_PAGE_LINK pattern type`() =
        runTest {
            val uriString = "https://mega.app/upgrade"
            val expected = UpgradeAccountNavKey()
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.UPGRADE_PAGE_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that correct nav key is returned when uri matches UPGRADE_LINK pattern type`() =
        runTest {
            val uriString = "https://mega.app/pro"
            val expected = UpgradeAccountNavKey()
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.UPGRADE_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when uri does not match upgrade pattern types`() =
        runTest {
            val uriString = "https://mega.app/other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK)

            assertThat(actual).isNull()
        }
}

