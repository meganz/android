package mega.privacy.android.core.coroutine

/**
 * The simple class name of this value for log output, or "null" when the value is absent.
 *
 * Logging the type instead of the value keeps the user content, links and tokens carried by data
 * classes out of the logs while still identifying which destination or event was handled. The
 * receiver is nullable so a value that is unexpectedly null at runtime is logged as such instead
 * of failing the log statement.
 */
fun Any?.logTypeName(): String = when (this) {
    null -> "null"
    else -> this::class.simpleName ?: "Anonymous"
}
