package mega.privacy.android.data.test.engine

/**
 * A recorded call to a fake gateway method.
 *
 * @property methodName the [kotlin.reflect.KFunction.name] of the gateway method that was called
 * @property arguments every argument in declaration order, including listeners
 */
data class Invocation(
    val methodName: String,
    val arguments: List<Any?>,
)
