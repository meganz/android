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


@Suppress("UnstableApiUsage")
internal class TranslatedStringTemplateDetector : ResourceXmlDetector() {

    private val nodeParameter = hashMapOf<String, List<String>>()
    override fun appliesTo(@NonNull folderType: ResourceFolderType): Boolean =
        folderType === ResourceFolderType.VALUES

    override fun getApplicableElements(): List<String> = listOf(
        TAG_STRING,
        TAG_PLURALS
    )

    override fun visitElement(context: XmlContext, element: Element) {
        element.childNodes.forEach { child ->
            if (child.nodeType == Node.TEXT_NODE) {
                // process string define example
                // <string name="settings_compression_queue_subtitle">The minimum size is %1$s and the maximum size is %2$s.</string>
                processNode(
                    context = context,
                    item = child,
                    tag = child.parentNode.attributes.item(0).nodeValue,
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
                            tag = "${child.parentNode.attributes.item(0).nodeValue}-${
                                item.parentNode.attributes.item(0).nodeValue
                            }",
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
        tag: String,
        element: Element,
    ) {
        val filePath = context.getLocation(item).file.absolutePath
        if (filePath.contains(DEFAULT_STRING_RESOURCE_FILE_PATH)) {
            // default string
            nodeParameter[tag] =
                parseValue(item.nodeValue)
        } else {
            // other resource file string compare with default string definition
            val defaultParameters =
                nodeParameter[tag].orEmpty()
            if (defaultParameters.isNotEmpty() && defaultParameters != parseValue(item.nodeValue)) {
                context.report(
                    issue = ISSUE,
                    scope = element,
                    location = context.getNameLocation(item),
                    message = DESCRIPTION
                )
            }
        }
    }

    private fun parseValue(value: String): List<String> {
        // we need to sort if parameter has index here example
        // Hello %1$s My name %2$s
        // Ten toi %2$s Chao ban %1$s
        val indexParameter =
            PARAMETER_WITH_INDEX_PATTERN.findAll(value).map { it.value }.sorted().toList()
        if (indexParameter.isNotEmpty()) return indexParameter
        // we must keep the order because parameter don't have index
        // Hello %s My name %s
        return PARAMETER_WITHOUT_INDEX_PATTERN.findAll(value).map { it.value }.toList()
    }

    companion object {
        private const val DEFAULT_STRING_RESOURCE_FILE_PATH = "res/values/strings.xml"
        private val PARAMETER_WITH_INDEX_PATTERN = "%[1-9][0-9]*\\$[a-z]".toRegex()
        private val PARAMETER_WITHOUT_INDEX_PATTERN = "%[a-z]".toRegex()

        private const val DESCRIPTION =
            "Define string template different parameter with default string value in values/string.xml"

        /**
         * Issue
         */
        val ISSUE = Issue.create(
            id = "TranslatedStringTemplate",
            briefDescription = DESCRIPTION,
            explanation = "You need to following how you define the number of parameter and parameter type of default string in values/string.xml",
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            Implementation(TranslatedStringTemplateDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )
    }
}