package mega.privacy.android.rules

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField

/**
 * Unique analytics event id detector
 */
class UniqueAnalyticsEventIdDetector : Detector(), Detector.UastScanner {
    private val uniqueFieldName = "eventTypeIdentifier"
    private val uniqueScope = "mega.privacy.android.domain.entity.analytics.AnalyticsEvent"
    private val usedIdentifiers = mutableSetOf<Int>()

    override fun applicableSuperClasses(): List<String>? {
        return listOf(uniqueScope)
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (!declaration.isInterface) {
            val uniqueIdentifier = getUniqueField(declaration)
            val uniqueIdentifierValue = getUniqueIdentifierValue(uniqueIdentifier)
            if (uniqueIdentifierValue == null) {
                reportMissingIdentifier(context = context, uniqueIdentifier = uniqueIdentifier)
            } else {
                if (!usedIdentifiers.add(uniqueIdentifierValue)) {
                    reportDuplicateIdentifier(
                        context = context,
                        uniqueIdentifier = uniqueIdentifier,
                        uniqueIdentifierValue = uniqueIdentifierValue,
                    )
                }
            }
        }
    }

    private fun getUniqueField(declaration: UClass) =
        declaration.fields.find { it.name == uniqueFieldName }

    private fun getUniqueIdentifierValue(uniqueIdentifier: UField?) =
        uniqueIdentifier?.uastInitializer?.evaluate() as? Int


    private fun reportDuplicateIdentifier(
        context: JavaContext,
        uniqueIdentifier: UField?,
        uniqueIdentifierValue: Int?,
    ) {
        context.report(
            issue = DUPLICATE_ANALYTICS_EVENT_ID_ISSUE,
            location = context.getLocation(uniqueIdentifier),
            message = "Identifier $uniqueIdentifierValue is already defined. Please pick a unique value",
        )
    }

    private fun reportMissingIdentifier(
        context: JavaContext,
        uniqueIdentifier: UField?,
    ) {
        context.report(
            issue = MISSING_ANALYTICS_EVENT_ID_ISSUE,
            location = context.getLocation(uniqueIdentifier),
            message = "Please ensure that $uniqueFieldName is initialised with a value",
        )
    }


    companion object {

        /**
         * UniqueAnalyticsId Issue
         */
        val DUPLICATE_ANALYTICS_EVENT_ID_ISSUE = Issue.create(
            id = "UniqueAnalyticsEventId",
            briefDescription = "Duplicate Analytics Event identifiers",
            explanation = "Each analytics event needs to have a unique identifier across all modules. " +
                    "This means that each event type in the domain needs to have its own range " +
                    "and every implementation of each of the different `AnalyticsInfo` classes " +
                    "needs to have a unique identifier within its own type",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.FATAL,
            Implementation(UniqueAnalyticsEventIdDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        /**
         * MissingAnalyticsId Issue
         */
        val MISSING_ANALYTICS_EVENT_ID_ISSUE = Issue.create(
            id = "MissingAnalyticsEventId",
            briefDescription = "Missing Analytics Event identifiers",
            explanation = "Analytics events need to initialise their unique identifiers. " +
                    "This allows us to verify that they are unique across types",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.FATAL,
            Implementation(UniqueAnalyticsEventIdDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}