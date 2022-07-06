package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCreateTraceStringTest {
    private val underTest: CreateTraceString = DefaultCreateTraceString()

    @Test
    fun `test that element not in the excluded list is returned`() = runTest {
        val loggingClassName = "LoggingClass"
        val loggingClasses = listOf(loggingClassName)
        val className = "NotLoggingClass"
        val methodName = "methodName"
        val filename = "filename"
        val lineNumber = 1
        val input = listOf(StackTraceElement(
            className,
            methodName,
            filename,
            lineNumber,
        ))

        val actual = underTest(input, loggingClasses)
        assertThat(actual).contains(methodName)
        assertThat(actual).contains(filename)
        assertThat(actual).contains(lineNumber.toString())
    }

    @Test
    fun `test that single element in the excluded list returns null`() = runTest {
        val loggingClassName = "LoggingClass"
        val loggingClasses = listOf(loggingClassName)
        val methodName = "methodName"
        val filename = "filename"
        val lineNumber = 1
        val input = listOf(StackTraceElement(
            loggingClassName,
            methodName,
            filename,
            lineNumber,
        ))

        val actual = underTest(input, loggingClasses)
        assertThat(actual).isNull()
    }

    @Test
    fun `test that first matching element is returned`() = runTest {
        val loggingClassName = "LoggingClass"
        val loggingClasses = listOf(loggingClassName)
        val className = "NotLoggingClass"
        val methodName = "methodName"
        val filename = "filename"
        val lineNumber = 1

        val input = listOf(
            StackTraceElement(
                loggingClassName,
                methodName,
                filename,
                lineNumber,
            ),
            StackTraceElement(
                className,
                methodName,
                filename,
                lineNumber,
            ),
            StackTraceElement(
                className + "a",
                methodName + "a",
                filename + "a",
                lineNumber + 1,
            ),
        )

        val actual = underTest(input, loggingClasses)
        assertThat(actual).contains(methodName)
        assertThat(actual).contains(filename)
        assertThat(actual).contains(lineNumber.toString())

        assertThat(actual).doesNotContain(methodName + "a")
        assertThat(actual).doesNotContain(filename + "a")
        assertThat(actual).doesNotContain((lineNumber + 1).toString())
    }

    @Test
    fun `test format`() = runTest {
        val loggingClassName = "LoggingClass"
        val loggingClasses = listOf(loggingClassName)
        val className = "NotLoggingClass"
        val methodName = "methodName"
        val filename = "filename"
        val lineNumber = 1
        val input = listOf(StackTraceElement(
            className,
            methodName,
            filename,
            lineNumber,
        ))

        val actual = underTest(input, loggingClasses)
        assertThat(actual).isEqualTo("${filename}#${methodName}:${lineNumber}")
    }
}