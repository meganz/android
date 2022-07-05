package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Default create trace string implementation of [CreateTraceString]
 */
class DefaultCreateTraceString @Inject constructor() : CreateTraceString {
    override suspend fun invoke(
        trace: List<StackTraceElement>,
        loggingClasses: List<String>,
    ) = trace
        .firstOrNull { it.className !in loggingClasses }
        ?.let { "${it.fileName}#${it.methodName}:${it.lineNumber}" }
}