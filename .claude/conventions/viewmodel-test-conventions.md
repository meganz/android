# ViewModel Test Conventions

## Test Class Structure

```kotlin
@ExtendWith(CoroutineMainDispatcherExtension::class)
class MyFeatureViewModelTest {
    private lateinit var underTest: MyFeatureViewModel

    private val someUseCase = mock<SomeUseCase>()
    private val anotherUseCase = mock<AnotherUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = MyFeatureViewModel(
            someUseCase = someUseCase,
            anotherUseCase = anotherUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            someUseCase,
            anotherUseCase,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(MyFeatureUiState.Loading)
    }

    @Test
    fun `test that state is Data when use case emits`() = runTest {
        someUseCase.stub {
            on { invoke() } doReturn flow {
                emit(listOf(someDomainEntity))
                awaitCancellation()
            }
        }

        underTest.uiState.test {
            val actual = awaitDataState()
            assertThat(actual.items).hasSize(1)
        }
    }

    private suspend fun ReceiveTurbine<MyFeatureUiState>.awaitDataState(): MyFeatureUiState.Data {
        var item = awaitItem()
        while (item !is MyFeatureUiState.Data) {
            item = awaitItem()
        }
        return item
    }
}
```

## Key Rules

- **JUnit 5** with `@ExtendWith(CoroutineMainDispatcherExtension::class)`.
- ViewModel variable always named **`underTest`**.
- All dependencies mocked with **`mock<T>()`**.
- ViewModel created in **`@BeforeEach`**, mocks reset in **`@AfterEach`**. Because `uiState` is `by lazy`, stubs set after ViewModel construction but before the first `uiState` access will still take effect. Do **not** use a separate `initViewModel()` method or `stubDefaults()` — create the ViewModel directly in `@BeforeEach`.
- Test names: **backtick style** — `` `test that X when Y` ``.
- All tests use **`= runTest { ... }`**.
- Flow testing with **Turbine**: `underTest.uiState.test { awaitItem() }`.
- **`awaitDataState()` helper**: when using the sealed `Loading`/`Data` pattern, include a private extension on `ReceiveTurbine` to skip past intermediate `Loading` emissions (see test example above).
- Assertions with **Google Truth**: `assertThat(...).isEqualTo(...)`.
- Verification: `verify(mock).invoke(args)`, `argumentCaptor<T>()`.
- Flow stubs: `stub { on { invoke() } doReturn flow { emit(...); awaitCancellation() } }`.
- **Property mocking**: When stubbing properties on mock objects, use `this.propertyName` to avoid ambiguity:
   ```kotlin
   // Correct:
   mock<AccountLevelDetail> {
       on { this.accountType } doReturn accountType
   }

   // Incorrect — Mockito may resolve to the wrong property:
   mock<AccountLevelDetail> {
       on { accountType } doReturn accountType
   }


## Required Imports

```kotlin
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
```
