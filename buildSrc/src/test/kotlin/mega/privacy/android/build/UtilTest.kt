package mega.privacy.android.build

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SystemStubsExtension::class)
class UtilTest {

    @SystemStub
    private val env: EnvironmentVariables = EnvironmentVariables()

    @Test
    fun `test that readVersionCode returns properly when it is available in system environment`() {
        val expectedVersionCode = "12345"
        env.set("APK_VERSION_CODE_FOR_CD", expectedVersionCode)
        assertThat(readVersionCode()).isEqualTo(expectedVersionCode.toInt())
    }

    @Test
    fun `test that readVersionNameTag returns versions name when it is available in system environment`() {
        val expectedVersionName = "expected_version_name"
        env.set("APK_VERSION_NAME_TAG_FOR_CD", expectedVersionName)
        assertThat(readVersionNameTag()).isEqualTo(expectedVersionName)
    }

    @Test
    fun `test that readVersionNameTag returns empty when it is not available in system environment`() {
        env.set("APK_VERSION_NAME_TAG_FOR_CD", null)
        assertThat(readVersionNameTag()).isEmpty()
    }

    @Test
    fun `test that readVersionNameTag returns empty when it is available but blank in system environment`() {
        env.set("APK_VERSION_NAME_TAG_FOR_CD", "   ")
        assertThat(readVersionNameTag()).isEmpty()
    }

    @Test
    fun `test that readVersionNameChannel returns versions name when it is available in system environment`() {
        val expectedVersionName = "expected_version_name"
        env.set("APK_VERSION_NAME_CHANNEL_FOR_CD", expectedVersionName)
        assertThat(readVersionNameChannel()).isEqualTo(expectedVersionName)
    }

    @Test
    fun `test that readVersionNameChannel returns empty when it is available but blank in system environment`() {
        env.set("APK_VERSION_NAME_CHANNEL_FOR_CD", "   ")
        assertThat(readVersionNameChannel()).isEmpty()
    }

    @Test
    fun `test that readVersionNameChannel returns empty when it is not available in system environment`() {
        env.set("APK_VERSION_NAME_CHANNEL_FOR_CD", null)
        assertThat(readVersionNameChannel()).isEmpty()
    }

    @Test
    fun `test that readReleaseNotes returns versions name when it is available in system environment`() {
        val expectedVersionName = "expected_release_notes"
        env.set("RELEASE_NOTES_FOR_CD", expectedVersionName)
        assertThat(readReleaseNotes()).isEqualTo(expectedVersionName)
    }

    @Test
    fun `test that readReleaseNotes returns default release notes when it is not available in system environment`() {
        env.set("RELEASE_NOTES_FOR_CD", null)
        assertThat(readReleaseNotes()).isEqualTo("Release Note not available")
    }

    @Test
    fun `test that readReleaseNotes returns default release notes when it is available but blank in system environment`() {
        env.set("RELEASE_NOTES_FOR_CD", "   ")
        assertThat(readReleaseNotes()).isEqualTo("Release Note not available")
    }

    @Test
    fun `test that readTesters returns versions name when it is available in system environment`() {
        val expectedVersionName = "expected_testers"
        env.set("TESTERS_FOR_CD", expectedVersionName)
        assertThat(readTesters()).isEqualTo(expectedVersionName)
    }

    @Test
    fun `test that readTesters returns empty when it is not available in system environment`() {
        env.set("TESTERS_FOR_CD", null)
        assertThat(readTesters()).isEmpty()
    }

    @Test
    fun `test that readTesters returns empty when it is available but blank in system environment`() {
        env.set("TESTERS_FOR_CD", "   ")
        assertThat(readTesters()).isEmpty()
    }

    @Test
    fun `test that readTesterGroupList returns versions name when it is available in system environment`() {
        val expectedGroupList = "expected_group_list"
        env.set("TESTER_GROUP_FOR_CD", expectedGroupList)
        assertThat(readTesterGroupList()).isEqualTo(expectedGroupList)
    }

    @Test
    fun `test that readTesterGroupList returns default group list when it is available in system environment`() {
        env.set("TESTER_GROUP_FOR_CD", null)
        assertThat(readTesterGroupList())
            .isEqualTo(
                "internal_qa, internal_dev, external_qa, external_dev, internal_design"
            )
    }

    @Test
    fun `test that isServerBuild returns true when it is available in system environment`() {
        val expectedBuildNumber = "expected_build_number"
        env.set("BUILD_NUMBER", expectedBuildNumber)
        assertThat(isServerBuild()).isTrue()
    }

    @Test
    fun `test that isServerBuild returns false when it is not available in system environment`() {
        env.set("BUILD_NUMBER", null)
        assertThat(isServerBuild()).isFalse()
    }

    @Test
    fun `test that isCiBuild returns true when it is available in system environment`() {
        val expectedIsCiBuild = "true"
        env.set("IS_CI_BUILD", expectedIsCiBuild)
        assertThat(isCiBuild()).isTrue()
    }

    @Test
    fun `test that isCiBuild returns false when it is not available in system environment`() {
        env.set("IS_CI_BUILD", null)
        assertThat(isCiBuild()).isFalse()
    }

    @Test
    fun `test that isCiBuild returns false when it is available in system environment but value is not true`() {
        env.set("IS_CI_BUILD", "other_value")
        assertThat(isCiBuild()).isFalse()
    }

    @Test
    fun `test that shouldCombineLintReports returns true when it is available in system environment`() {
        val expected = "true"
        env.set("COMBINE_LINT_REPORTS", expected)
        assertThat(shouldCombineLintReports()).isTrue()
    }

    @Test
    fun `test that shouldCombineLintReports returns false when it is not available in system environment`() {
        env.set("COMBINE_LINT_REPORTS", null)
        assertThat(shouldCombineLintReports()).isFalse()
    }

    @Test
    fun `test that shouldCombineLintReports returns false when it is available in system environment but value is not true`() {
        env.set("COMBINE_LINT_REPORTS", "other_value")
        assertThat(shouldCombineLintReports()).isFalse()
    }

    @Test
    fun `test that buildTypeMatches returns true if task list contains the type`() {
        val taskList = listOf("clean", "assembleDebug", "assembleRelease")
        assertThat(buildTypeMatches("debug", taskList)).isTrue()
    }

    @Test
    fun `test that buildTypeMatches returns false if task list does not contain the type`() {
        val taskList = listOf("clean", "assembleDebug", "assembleQa")
        assertThat(buildTypeMatches("release", taskList)).isFalse()
    }

    @Test
    fun `test that buildTypeMatches returns false if task list is empty`() {
        val taskList = emptyList<String>()
        assertThat(buildTypeMatches("release", taskList)).isFalse()
    }


    @Test
    fun `test that shouldUsePrebuiltSdk returns true when it is true in system environment`() {
        val expected = "true"
        env.set("USE_PREBUILT_SDK", expected)
        assertThat(shouldUsePrebuiltSdk()).isTrue()
    }

    @Test
    fun `test that shouldUsePrebuiltSdk returns true when it is other value in system environment`() {
        val expected = "something else"
        env.set("USE_PREBUILT_SDK", expected)
        assertThat(shouldUsePrebuiltSdk()).isTrue()
    }

    @Test
    fun `test that shouldUsePrebuiltSdk returns false when it is false in system environment`() {
        val expected = "false"
        env.set("USE_PREBUILT_SDK", expected)
        assertThat(shouldUsePrebuiltSdk()).isFalse()
    }

    @Test
    fun `test that shouldUsePrebuiltSdk returns true when it is not available in system environment`() {
        env.set("USE_PREBUILT_SDK", null)
        assertThat(shouldUsePrebuiltSdk()).isTrue()
    }
}
