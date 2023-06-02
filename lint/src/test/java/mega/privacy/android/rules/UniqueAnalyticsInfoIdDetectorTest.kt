package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

internal class UniqueAnalyticsInfoIdDetectorTest {
    @Test
    internal fun test_that_no_identifier_error_is_raised_if_identifier_is_not_assigned() {
        lint().files(
            givenTestFile.indented(),
            kotlin(
                """
                    package mega.privacy.android.analytics.event

                    class HomeScreenInfo(override val uniqueIdentifier: Int) : ScreenInfo
                """
            ).indented()
        ).issues(UniqueAnalyticsInfoIdDetector.MISSING_ANALYTICS_INFO_ID_ISSUE)
            .run()
            .expect(
                """
                src/mega/privacy/android/analytics/event/HomeScreenInfo.kt:3: Error: Please ensure that uniqueIdentifier is initialised with a value [MissingAnalyticsInfoId]
                class HomeScreenInfo(override val uniqueIdentifier: Int) : ScreenInfo
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
            """
            )
    }

    @Test
    internal fun test_that_no_warning_is_given_if_info_contains_unique_identifier() {
        lint().files(
            givenTestFile.indented(),
            kotlin(
                """
                    package mega.privacy.android.analytics.event

                    object HomeScreenInfo : ScreenInfo {
                        override val uniqueIdentifier = 0
                    }
                """
            ).indented()
        ).issues(UniqueAnalyticsInfoIdDetector.DUPLICATE_ANALYTICS_INFO_ID_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun test_that_duplicates_in_different_types_do_not_raise_any_issues() {
        lint().files(
            givenTestFile.indented(),
            kotlin(
                """
                    package mega.privacy.android.analytics.event

                    object HomeScreenInfo : ScreenInfo {
                        override val uniqueIdentifier = 0
                    }
                    
                    object HomeScreenTabInfo : TabInfo {
                        override val uniqueIdentifier = 0
                    }
                """
            ).indented()
        ).issues(UniqueAnalyticsInfoIdDetector.DUPLICATE_ANALYTICS_INFO_ID_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun test_that_duplicates_in_same_type_raises_an_issue() {
        lint().files(
            givenTestFile.indented(),
            kotlin(
                """
                    package mega.privacy.android.analytics.event

                    object HomeScreenInfo : ScreenInfo {
                        override val uniqueIdentifier = 0
                    }
                    
                    object HomeScreenTabInfo : ScreenInfo {
                        override val uniqueIdentifier = 0
                    }
                """
            ).indented()
        ).issues(UniqueAnalyticsInfoIdDetector.DUPLICATE_ANALYTICS_INFO_ID_ISSUE)
            .run()
            .expect(
                """
                    src/mega/privacy/android/analytics/event/HomeScreenInfo.kt:8: Error: Identifier 0 is already defined for type ScreenInfo. Please pick a unique value [UniqueAnalyticsInfoId]
                        override val uniqueIdentifier = 0
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    private val givenTestFile = kotlin(
        """
                        package mega.privacy.android.analytics.event
    
                        interface AnalyticsInfo {
                             /**
                              * Unique identifier
                              */
                             val uniqueIdentifier: Int
                        } 
                        
                        interface ScreenInfo : AnalyticsInfo
                        interface TabInfo : AnalyticsInfo
                    """
    )
}