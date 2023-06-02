package mega.privacy.android.rules

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField

/**
 * Unique analytics info id detector
 */
@Suppress("UnstableApiUsage")
class UniqueAnalyticsInfoIdDetector : Detector(), Detector.UastScanner {
    private val uniqueFieldName = "uniqueIdentifier"
    private val uniqueScopeInterface = "mega.privacy.android.analytics.event.AnalyticsInfo"
    private val usedIdentifiers = mutableMapOf<String, MutableSet<Int>>()

    override fun applicableSuperClasses(): List<String>? {
        return listOf(uniqueScopeInterface)
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (!declaration.isInterface) {
            val identifierKey = getIdentifierKey(declaration) ?: return
            val identifiersForType = usedIdentifiers.getOrPut(identifierKey) { mutableSetOf() }
            val uniqueIdentifier = getUniqueField(declaration)
            val uniqueIdentifierValue = getUniqueIdentifierValue(uniqueIdentifier)
            if (uniqueIdentifierValue == null) {
                reportMissingIdentifier(context = context, uniqueIdentifier = uniqueIdentifier)
            } else {
                if (!identifiersForType.add(uniqueIdentifierValue)) {
                    reportDuplicateIdentifier(
                        context = context,
                        uniqueIdentifier = uniqueIdentifier,
                        uniqueIdentifierValue = uniqueIdentifierValue,
                        identifierKey = identifierKey
                    )
                } else {
                    usedIdentifiers.replace(identifierKey, identifiersForType)
                }
            }
        }
    }

    private fun getIdentifierKey(declaration: UClass) =
        declaration.interfaces.map { it.name }.firstOrNull()

    private fun getUniqueField(declaration: UClass) =
        declaration.fields.find { it.name == uniqueFieldName }

    private fun getUniqueIdentifierValue(uniqueIdentifier: UField?) =
        uniqueIdentifier?.uastInitializer?.evaluate() as? Int


    private fun reportDuplicateIdentifier(
        context: JavaContext,
        uniqueIdentifier: UField?,
        uniqueIdentifierValue: Int?,
        identifierKey: @NlsSafe String,
    ) {
        context.report(
            issue = DUPLICATE_ANALYTICS_INFO_ID_ISSUE,
            location = context.getLocation(uniqueIdentifier),
            message = "Identifier $uniqueIdentifierValue is already defined for type $identifierKey. Please pick a unique value",
        )
    }

    private fun reportMissingIdentifier(
        context: JavaContext,
        uniqueIdentifier: UField?,
    ) {
        context.report(
            issue = MISSING_ANALYTICS_INFO_ID_ISSUE,
            location = context.getLocation(uniqueIdentifier),
            message = "Please ensure that $uniqueFieldName is initialised with a value",
        )
    }


    companion object {

        /**
         * UniqueAnalyticsId Issue
         */
        val DUPLICATE_ANALYTICS_INFO_ID_ISSUE = Issue.create(
            id = "UniqueAnalyticsInfoId",
            briefDescription = "Duplicate Analytics info identifiers",
            explanation = "Each analytics event needs to have a unique identifier across all modules. " +
                    "This means that each event type in the domain needs to have its own range " +
                    "and every implementation of each of the different `AnalyticsInfo` classes " +
                    "needs to have a unique identifier within its own type",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.FATAL,
            Implementation(UniqueAnalyticsInfoIdDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        /**
         * MissingAnalyticsId Issue
         */
        val MISSING_ANALYTICS_INFO_ID_ISSUE = Issue.create(
            id = "MissingAnalyticsInfoId",
            briefDescription = "Missing Analytics info identifiers",
            explanation = "Analytics info need to initialise their unique identifiers. " +
                    "This allows us to verify that they are unique across types",
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.FATAL,
            Implementation(UniqueAnalyticsInfoIdDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}