# UseCase Test Conventions

## Test Class Structure

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPricingUseCaseTest {
    private lateinit var underTest: GetPricingUseCase

    private val pricingRepository = mock<PricingRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetPricingUseCase(
            pricingRepository = pricingRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(pricingRepository)
    }

    @Test
    fun `test that invoke returns pricing from repository`() = runTest {
        val expected = mock<Pricing>()
        whenever(pricingRepository.getPricing(true)).thenReturn(expected)

        val actual = underTest(forceRefresh = true)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke passes forceRefresh to repository`() = runTest {
        whenever(pricingRepository.getPricing(any())).thenReturn(mock())

        underTest(forceRefresh = false)

        verify(pricingRepository).getPricing(false)
    }
}
```

## Key Rules

- **JUnit 5** with `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`.
- **`@ExtendWith(CoroutineMainDispatcherExtension::class)`** — include when the use case injects a dispatcher qualifier (e.g., `@DefaultDispatcher`) or when testing Flows that require the main dispatcher. For simple suspend use cases that only delegate to repositories, it is not required.
- UseCase variable always named **`underTest`**.
- All dependencies mocked with **`mock<T>()`**.
- UseCase created in **`@BeforeAll`** — use cases are stateless, so one instance suffices for all tests. This differs from ViewModels which use `@BeforeEach` due to stateful lazy initialization.
- Mocks reset in **`@BeforeEach`** using `reset(mock1, mock2, ...)`.
- Test names: **backtick style** — `` `test that <method> <action>` `` or `` `test that <method> <action> when <cause>` ``.
- All tests use **`= runTest { ... }`**.
- Assertions with **Google Truth**: `assertThat(...).isEqualTo(...)`.

## Test Naming

Follow the backtick naming pattern:

```kotlin
`test that invoke returns pricing from repository`
`test that invoke throws exception when repository fails`
`test that invoke calls refresh when cache is stale`
`test that invoke emits updated list when monitor emits`
`test that invoke returns null when node not found`
```

## Testing Patterns by Return Type

### Suspend one-shot — simple delegation

Verify the use case delegates correctly and returns the expected value:

```kotlin
@Test
fun `test that invoke returns value from repository`() = runTest {
    val expected = Pricing(amount = 100)
    whenever(pricingRepository.getPricing(true)).thenReturn(expected)

    val actual = underTest(forceRefresh = true)

    assertThat(actual).isEqualTo(expected)
}
```

### Suspend one-shot — side effect verification

Verify the use case calls the right methods with the right arguments:

```kotlin
@Test
fun `test that invoke calls repository with correct parameters`() = runTest {
    whenever(repository.saveData(any())).thenReturn(Unit)

    underTest(data = testData)

    verify(repository).saveData(testData)
}
```

### Suspend one-shot — exception propagation

Verify exceptions from dependencies propagate correctly:

```kotlin
@Test
fun `test that invoke throws when repository fails`() = runTest {
    whenever(repository.getData()).thenThrow(RuntimeException("error"))

    assertThrows<RuntimeException> { underTest() }
}
```

### Flow-returning — with Turbine

Verify Flow emissions using Turbine:

```kotlin
@Test
fun `test that invoke emits values from repository`() = runTest {
    val expected = listOf(Node(id = 1L))
    whenever(repository.monitorNodes()).thenReturn(flowOf(expected))

    underTest().test {
        assertThat(awaitItem()).isEqualTo(expected)
        awaitComplete()
    }
}
```

### Flow-returning — long-lived with `awaitCancellation`

For Flows that don't complete naturally:

```kotlin
@Test
fun `test that invoke emits transformed data`() = runTest {
    whenever(repository.monitorData()).thenReturn(flow {
        emit(rawData)
        awaitCancellation()
    })

    underTest().test {
        val item = awaitItem()
        assertThat(item.name).isEqualTo("expected")
    }
}
```

### Flow-returning — multiple emissions

```kotlin
@Test
fun `test that invoke emits updates when repository emits`() = runTest {
    whenever(repository.monitorData()).thenReturn(flow {
        emit(firstValue)
        emit(secondValue)
        awaitCancellation()
    })

    underTest().test {
        assertThat(awaitItem()).isEqualTo(firstExpected)
        assertThat(awaitItem()).isEqualTo(secondExpected)
    }
}
```

### Nullable return

```kotlin
@Test
fun `test that invoke returns null when not found`() = runTest {
    whenever(repository.findNode(1L)).thenReturn(null)

    assertThat(underTest(1L)).isNull()
}
```

### Boolean check use cases

```kotlin
@Test
fun `test that invoke returns true when user is admin`() = runTest {
    whenever(accountRepository.getUserRole()).thenReturn(UserRole.Admin)

    assertThat(underTest()).isTrue()
}

@Test
fun `test that invoke returns false when user is not admin`() = runTest {
    whenever(accountRepository.getUserRole()).thenReturn(UserRole.Member)

    assertThat(underTest()).isFalse()
}
```

## Mocking Patterns

- **Simple returns**: `whenever(mock.method()).thenReturn(value)`
- **Exceptions**: `whenever(mock.method()).thenThrow(RuntimeException("error"))`
- **Verify calls**: `verify(mock).method(args)`
- **Verify no calls**: `verify(mock, never()).method()`
- **Capture arguments**: `argumentCaptor<T>().apply { verify(mock).method(capture()); assertThat(firstValue).isEqualTo(expected) }`
- **Flow stubs**: `whenever(mock.monitorX()).thenReturn(flowOf(value))`
- **Any matcher**: `whenever(mock.method(any())).thenReturn(value)`

## Test Structure for Dispatcher Injection

When the use case injects a dispatcher, add the coroutine extension:

```kotlin
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessDataUseCaseTest {
    private lateinit var underTest: ProcessDataUseCase

    private val repository = mock<DataRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ProcessDataUseCase(
            repository = repository,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }
}
```

## Required Imports

```kotlin
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
```

Plus conditionally:
```kotlin
// For Flow testing
import app.cash.turbine.test
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

// For dispatcher injection tests
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.extension.ExtendWith
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension

// For argument capture and advanced verification
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
```

## File Placement

Test files mirror the production package structure in the domain module:

```
domain/src/test/kotlin/mega/privacy/android/domain/usecase/{package}/
    {Name}UseCaseTest.kt
```