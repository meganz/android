package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertWithMessage
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * NavKeys are logged through their toString(), directly and nested inside events and state
 * objects, so a NavKey that carries user content (links, keys, names, paths, free text) has to
 * override toString() to keep that content out of the logs.
 *
 * This scans every concrete NavKey on the app classpath, builds each one with a canary in every
 * String, UriPath and nested value it accepts, and fails if the canary comes out of toString().
 * A new key therefore cannot ship with a generated toString() over content fields.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NavKeyToStringGuardTest {

    private val navKeys: List<KClass<out NavKey>> by lazy { projectNavKeyClasses() }

    @Test
    fun `test that no NavKey prints user content from toString`() {
        assertWithMessage("Classpath scan found too few NavKeys to be trustworthy")
            .that(navKeys.size).isAtLeast(MINIMUM_EXPECTED_NAV_KEYS)

        val leaking = navKeys
            .mapNotNull { navKey ->
                val instance = navKey.buildWithCanary()
                val printed = instance.toString()
                if (CANARY in printed) "${navKey.qualifiedName} -> $printed" else null
            }
            .sorted()

        assertWithMessage(
            "These NavKeys print user content from toString(). Override toString() to print " +
                    "only ids, counts and booleans, and add the key to NavKeyToStringTest."
        ).that(leaking).isEmpty()
    }

    private fun KClass<out NavKey>.buildWithCanary(): Any =
        instanceOrNull() ?: construct(this, injectCanary = true, depth = 0)

    /**
     * Builds an instance of [klass] through its primary constructor. The canary goes into every
     * content-typed value, except inside a nested NavKey: that key is checked on its own, and a
     * leak there must not be attributed to the key wrapping it.
     */
    private fun construct(klass: KClass<*>, injectCanary: Boolean, depth: Int): Any {
        klass.instanceOrNull()?.let { return it }
        check(depth < MAX_DEPTH) { "Gave up building ${klass.qualifiedName}: nesting too deep" }
        if (klass.isSealed || klass.java.isInterface || klass.isAbstract) {
            return constructSubtype(klass, injectCanary, depth)
        }
        val constructor = checkNotNull(klass.primaryConstructor) {
            "${klass.qualifiedName} has no primary constructor; extend the guard test to build it"
        }
        val arguments = constructor.parameters.associateWith { parameter ->
            valueFor(parameter.type, klass, parameter, injectCanary, depth)
        }
        constructor.isAccessible = true
        return constructor.callBy(arguments)
    }

    private fun constructSubtype(klass: KClass<*>, injectCanary: Boolean, depth: Int): Any {
        val candidates = if (klass.isSealed) {
            klass.sealedSubclasses
        } else {
            navKeys.filter { klass.java.isAssignableFrom(it.java) }
        }
        val subtype = candidates.firstOrNull { it.instanceOrNull() != null }
            ?: candidates.firstOrNull()
            ?: error("${klass.qualifiedName} has no known concrete subtype; extend the guard test")
        return construct(subtype, injectCanary, depth + 1)
    }

    private fun valueFor(
        type: KType,
        owner: KClass<*>,
        parameter: KParameter,
        injectCanary: Boolean,
        depth: Int,
    ): Any? {
        val text = if (injectCanary && parameter.name !in IDENTIFIER_PARAMETERS) CANARY else FILLER
        val klass = type.classifier as? KClass<*>
        return when (klass) {
            String::class, CharSequence::class -> text
            UriPath::class -> UriPath(text)
            File::class -> File(text)
            Long::class -> 1L
            Int::class -> 1
            Short::class -> 1.toShort()
            Byte::class -> 1.toByte()
            Float::class -> 1f
            Double::class -> 1.0
            Boolean::class -> false
            Char::class -> 'x'
            List::class, Collection::class, Iterable::class ->
                listOf(elementFor(type, owner, parameter, injectCanary, depth))

            Set::class -> setOf(elementFor(type, owner, parameter, injectCanary, depth))
            Map::class -> emptyMap<Any, Any>()
            null -> null
            else -> when {
                klass.java.isEnum -> klass.java.enumConstants.first()
                NavKey::class.java.isAssignableFrom(klass.java) ->
                    construct(klass, injectCanary = false, depth = depth + 1)

                klass.isProjectClass() -> construct(klass, injectCanary, depth + 1)
                type.isMarkedNullable -> null
                else -> error(
                    "Cannot build ${owner.qualifiedName}.${parameter.name}: ${type}; " +
                            "extend the guard test to build it"
                )
            }
        }
    }

    private fun elementFor(
        type: KType,
        owner: KClass<*>,
        parameter: KParameter,
        injectCanary: Boolean,
        depth: Int,
    ): Any {
        val elementType = checkNotNull(type.arguments.firstOrNull()?.type) {
            "Cannot build ${owner.qualifiedName}.${parameter.name}: star-projected $type"
        }
        return checkNotNull(valueFor(elementType, owner, parameter, injectCanary, depth)) {
            "Cannot build an element for ${owner.qualifiedName}.${parameter.name}: $type"
        }
    }

    /** Reads the singleton of an object declaration, including internal and private ones. */
    private fun KClass<*>.instanceOrNull(): Any? = runCatching {
        java.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
    }.getOrNull()

    private fun KClass<*>.isProjectClass() =
        qualifiedName?.let { name -> PROJECT_PACKAGES.any { name.startsWith(it) } } == true

    @Suppress("UNCHECKED_CAST")
    private fun projectNavKeyClasses(): List<KClass<out NavKey>> {
        val loader = Thread.currentThread().contextClassLoader
        return projectClasspathEntries()
            .flatMap { classNamesIn(it) }
            .distinct()
            .mapNotNull { name -> runCatching { Class.forName(name, false, loader) }.getOrNull() }
            .filter { NavKey::class.java.isAssignableFrom(it) }
            .filter { !it.isInterface && !Modifier.isAbstract(it.modifiers) && !it.isAnonymousClass }
            .mapNotNull { runCatching { it.kotlin as KClass<out NavKey> }.getOrNull() }
    }

    private fun projectClasspathEntries(): List<File> {
        val repoRoot = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }
        return System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map(::File)
            .filter { it.startsWith(repoRoot) && TEST_OUTPUT_MARKERS.none { m -> m in it.path } }
    }

    private fun classNamesIn(entry: File): List<String> = when {
        entry.isDirectory -> entry.walk()
            .filter { it.isFile && it.extension == "class" }
            .map { it.relativeTo(entry).path.toClassName() }
            .toList()

        entry.extension == "jar" -> JarFile(entry).use { jar ->
            jar.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .map { it.name.toClassName() }
                .toList()
        }

        else -> emptyList()
    }

    private fun String.toClassName() =
        removeSuffix(".class").replace(File.separatorChar, '.').replace('/', '.')

    private companion object {
        const val CANARY = "https://mega.nz/file/HANDLE#CANARY_NODE_KEY/canary-name.txt"
        const val FILLER = "x"
        const val MINIMUM_EXPECTED_NAV_KEYS = 100
        const val MAX_DEPTH = 6
        val PROJECT_PACKAGES = listOf("mega.privacy.", "mega.android.")

        /**
         * String parameters that hold an enum-like value or an internal identifier rather than
         * user content, so they may stay in the generated toString().
         */
        val IDENTIFIER_PARAMETERS = setOf(
            "action",
            "actionType",
            "mode",
            "type",
            "albumType",
            "sortType",
            "sortOrder",
            "filterType",
            "mediaSource",
            "mimeType",
            "preferenceId",
            "rootKey",
            "launchId",
            "pageId",
            "issueId",
            "activeSubscriptionSku",
            "handle",
            "handles",
            "nodes",
        )
        val TEST_OUTPUT_MARKERS = listOf("UnitTest", "/test/", "screenshotTest")
    }
}
