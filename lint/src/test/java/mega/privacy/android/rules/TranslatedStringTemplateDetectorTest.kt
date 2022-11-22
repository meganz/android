package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

@Suppress("UnstableApiUsage")
class TranslatedStringTemplateDetectorTest {
    @Test
    fun test_when_define_string_index_incorrect_then_show_error() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <string name="label">Hello %1＄s My name %2＄s</string>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <string name="label">Xin chào %1＄s Tôi là %1＄s</string>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    res/values-vi/strings.xml:3: Error: Define string template different parameter with default string value in values/string.xml [TranslatedStringTemplate]
                                                <string name="label">Xin chào %1＄s Tôi là %1＄s</string>
                                                                     ~~~~~
                    1 errors, 0 warnings
                 """
            )
    }

    @Test
    fun test_when_define_string_different_type_then_show_error() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <string name="label">Hello %1＄s My name %2＄s</string>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <string name="label">Xin chào %1＄s Tôi là %2＄d</string>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    res/values-vi/strings.xml:3: Error: Define string template different parameter with default string value in values/string.xml [TranslatedStringTemplate]
                                                <string name="label">Xin chào %1＄s Tôi là %2＄d</string>
                                                                     ~~~~~
                    1 errors, 0 warnings
                 """
            )
    }

    @Test
    fun test_when_define_string_missing_parameter_then_show_error() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <string name="label">Hello %1＄s My name %2＄s</string>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <string name="label">Xin chào %1＄s Tôi là</string>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    res/values-vi/strings.xml:3: Error: Define string template different parameter with default string value in values/string.xml [TranslatedStringTemplate]
                                                <string name="label">Xin chào %1＄s Tôi là</string>
                                                                     ~~~~~
                    1 errors, 0 warnings
                 """
            )
    }

    @Test
    fun test_when_define_string_correct_then_no_warning_show() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <string name="label">Hello %1＄s My name %2＄s</string>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <string name="label">Xin chào %1＄s Tôi là %2＄s</string>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    No warnings.
                 """
            )
    }


    @Test
    fun test_when_define_string_same_type_but_different_order_then_no_warning_show() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <string name="label">Hello %1＄s My name %2＄s</string>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <string name="label">Tôi là %2＄s Xin chào %1＄s</string>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    No warnings.
                 """
            )
    }

    @Test
    fun test_when_define_string_without_index_and_different_type_then_show_error() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <string name="label">Hello %s My name %s</string>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <string name="label">Tôi là %d Xin chào %s</string>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    res/values-vi/strings.xml:3: Error: Define string template different parameter with default string value in values/string.xml [TranslatedStringTemplate]
                                                <string name="label">Tôi là %d Xin chào %s</string>
                                                                     ~~~~~
                    1 errors, 0 warnings
                 """
            )
    }

    @Test
    fun test_when_define_string_without_index_and_same_type_then_no_warning_show() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <string name="label">Hello %s My name %s</string>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <string name="label">Xin chào %s Tôi là %s</string>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    No warnings.
                 """
            )
    }

    @Test
    fun test_when_plural_define_index_incorrect_then_show_error() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %2＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %1＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    res/values-vi/strings.xml:4: Error: Define string template different parameter with default string value in values/string.xml [TranslatedStringTemplate]
                                                    <item quantity="one">Hello %1＄s My name %1＄s</item>
                                                                         ~~~~~
                    1 errors, 0 warnings
                 """
            )
    }

    @Test
    fun test_when_plural_define_different_type_then_show_error() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %2＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %1＄d</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    res/values-vi/strings.xml:4: Error: Define string template different parameter with default string value in values/string.xml [TranslatedStringTemplate]
                                                    <item quantity="one">Hello %1＄s My name %1＄d</item>
                                                                         ~~~~~
                    1 errors, 0 warnings
                 """
            )
    }

    @Test
    fun test_when_plural_define_missing_parameter_then_show_error() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %2＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    res/values-vi/strings.xml:4: Error: Define string template different parameter with default string value in values/string.xml [TranslatedStringTemplate]
                                                    <item quantity="one">Hello %1＄s My name</item>
                                                                         ~~~~~
                    1 errors, 0 warnings
                 """
            )
    }

    @Test
    fun test_when_plural_define_correct_then_no_warning_show() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %2＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %2＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    No warnings.
                 """
            )
    }

    @Test
    fun test_when_plural_define_more_element_than_default_then_no_warning_show() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %2＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
                xml(
                    "res/values-vi/strings.xml", """
                        <resources>
                            <plurals name="plural_select_file">
                                <item quantity="one">Hello %1＄s My name %2＄s</item>
                                <item quantity="two">Hello %1＄s My name %2＄s</item>
                                <item quantity="other">Choose Files</item>
                            </plurals>
                        </resources>
                """
                ),
            )
            .issues(TranslatedStringTemplateDetector.ISSUE)
            .skipTestModes(TestMode.CDATA)
            .run()
            .expect(
                """
                    No warnings.
                 """
            )
    }
}