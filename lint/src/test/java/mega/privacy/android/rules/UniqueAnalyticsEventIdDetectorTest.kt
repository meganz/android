package mega.privacy.android.rules

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

internal class UniqueAnalyticsEventIdDetectorTest {
    @Test
    internal fun test_that_no_identifier_error_is_raised_if_identifier_is_not_assigned() {
        lint().files(
            givenTestFile.indented(),
            kotlin(
                """
                    package mega.privacy.android.domain.entity.analytics

                    class DialogDisplayedEvent(
                            private val identifier: Int,
                            override val eventTypeIdentifier: Int,
                        ) : AnalyticsEvent() {
                        }
                """
            ).indented()
        ).issues(UniqueAnalyticsEventIdDetector.MISSING_ANALYTICS_EVENT_ID_ISSUE)
            .run()
            .expect(
                """
                    src/mega/privacy/android/domain/entity/analytics/DialogDisplayedEvent.kt:5: Error: Please ensure that eventTypeIdentifier is initialised with a value [MissingAnalyticsEventId]
                            override val eventTypeIdentifier: Int,
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
                    package mega.privacy.android.domain.entity.analytics

                    class DialogDisplayedEvent(
                            private val identifier: Int,
                        ) : AnalyticsEvent() {
                            override val eventTypeIdentifier = 3000
                        }
                """
            ).indented()
        ).issues(UniqueAnalyticsEventIdDetector.DUPLICATE_ANALYTICS_EVENT_ID_ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun test_that_duplicates_raises_an_issue() {
        lint().files(
            givenTestFile.indented(),
            kotlin(
                """
                    package mega.privacy.android.domain.entity.analytics

                    class DialogDisplayedEvent(
                            private val identifier: Int,
                        ) : AnalyticsEvent() {
                            override val eventTypeIdentifier = 3000
                        }

                    class OtherEvent(
                            private val identifier: Int,
                        ) : AnalyticsEvent() {
                            override val eventTypeIdentifier = 3000
                        }
                """
            ).indented()
        ).issues(UniqueAnalyticsEventIdDetector.DUPLICATE_ANALYTICS_EVENT_ID_ISSUE)
            .run()
            .expect(
                """
                    src/mega/privacy/android/domain/entity/analytics/DialogDisplayedEvent.kt:12: Error: Identifier 3000 is already defined. Please pick a unique value [UniqueAnalyticsEventId]
                            override val eventTypeIdentifier = 3000
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """.trimIndent()
            )
    }

    private val givenTestFile = kotlin(
        """
                        package mega.privacy.android.domain.entity.analytics
    
                        interface AnalyticsEvent {
                             /**
                              * Unique identifier
                              */
                             val eventTypeIdentifier: Int
                        } 
                        
                    """
    )
}