package mega.privacy.android.rules

import com.android.SdkConstants.TAG_PLURALS
import com.android.SdkConstants.TAG_STRING
import com.android.SdkConstants.TAG_STRING_ARRAY
import com.android.annotations.NonNull
import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.utils.forEach
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Non index string template detector
 *
 * It detect all string template without index. For example "Hello %s" should become to "Hello %1$s"
 */
@Suppress("UnstableApiUsage")
internal class NonIndexStringTemplateDetector : ResourceXmlDetector() {

    override fun appliesTo(@NonNull folderType: ResourceFolderType): Boolean =
        folderType == ResourceFolderType.VALUES

    override fun getApplicableElements(): List<String> = listOf(TAG_STRING, TAG_PLURALS)

    override fun visitElement(context: XmlContext, element: Element) {
        element.childNodes.forEach { child ->
            if (child.nodeType == Node.TEXT_NODE) {
                // process string define example
                // <string name="settings_compression_queue_subtitle">The minimum size is %1$s and the maximum size is %2$s.</string>
                processNode(
                    context = context,
                    item = child,
                    element = element
                )
            } else if (child.nodeType == Node.ELEMENT_NODE &&
                (child.parentNode.nodeName.equals(TAG_STRING_ARRAY) ||
                        child.parentNode.nodeName.equals(TAG_PLURALS))
            ) {
                // process plurals string define example
                //    <plurals name="subtitle_notification_added_files">
                //        <item quantity="one">[A]%1$s [/A][B]added %2$d file.[/B]</item>
                //        <item quantity="other">[A]%1$s [/A][B]added %2$d files.[/B]</item>
                //    </plurals>
                child.childNodes.forEach { item ->
                    if (item.nodeType == Node.TEXT_NODE) {
                        processNode(
                            context = context,
                            item = item,
                            element = element
                        )
                    }
                }
            }
        }
    }

    private fun processNode(
        context: XmlContext,
        item: Node,
        element: Element,
    ) {
        val items = parseValue(item.nodeValue)
        if (items.isNotEmpty()) {
            context.report(
                issue = ISSUE,
                scope = element,
                location = context.getNameLocation(item),
                message = DESCRIPTION
            )
        }
    }

    private fun parseValue(value: String): List<String> {
        return PARAMETER_WITHOUT_INDEX_PATTERN.findAll(value).map { it.value }.toList()
    }

    companion object {
        private val PARAMETER_WITHOUT_INDEX_PATTERN = "%[a-z]".toRegex()

        private const val DESCRIPTION =
            "Defining a String template without index parameters"

        /**
         * Issue
         */
        val ISSUE = Issue.create(
            id = "NonIndexStringTemplate",
            briefDescription = DESCRIPTION,
            explanation = "Defining a String template without index parameters can potentially crash the app in some languages if the order of parameters are changed.",
            category = Category.CORRECTNESS,
            priority = 4,
            severity = Severity.WARNING,
            Implementation(NonIndexStringTemplateDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )
    }
}