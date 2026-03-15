# ViewModel Production Code Conventions

## Class Structure

```kotlin
@HiltViewModel
internal class MyFeatureViewModel @Inject constructor(
    private val someUseCase: SomeUseCase,
    private val anotherUseCase: AnotherUseCase,
    private val someMapper: SomeMapper,
) : ViewModel() {

    val uiState: StateFlow<MyFeatureUiState> by lazy(LazyThreadSafetyMode.NONE) {
        someUseCase()
            .map { data -> someMapper(data) }
            .map { mapped ->
                MyFeatureUiState.Data(items = mapped)
            }
            .catch { e ->
                Timber.e(e, "Failed to load feature data")
            }
            .asUiStateFlow(
                viewModelScope,
                MyFeatureUiState.Loading,
            )
    }

    fun onSomeAction(param: String) {
        viewModelScope.launch {
            runCatching {
                anotherUseCase(param)
            }.onFailure {
                Timber.e(it, "Failed to perform action")
            }
        }
    }
}
```

## Key Rules

- **`@HiltViewModel` + `@Inject constructor`** — always.
- **`internal` visibility** — ViewModels and mappers in feature modules should be `internal class`.
- **Dependencies**: inject use cases and mappers only — **never** repositories or data layer code.
- **State**: expose a single `val uiState: StateFlow<UiState>` property — no other public properties.
  - **Always use `by lazy(LazyThreadSafetyMode.NONE) { ... .asUiStateFlow(viewModelScope, initialValue) }`**.
  - Import `asUiStateFlow` from `mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow`.
  - For domain flows: incorporate directly into the lazy initialization (via `combine`, `map`, etc.).
  - For one-shot events (navigation, snackbar, etc.): use a separate **`Channel<StateEventWithContent<T>>`** per event, incorporated into the lazy block via `.receiveAsFlow().onStart { emit(consumed()) }`. Send with `triggered(value)`, consume with `trySend(consumed())`.
  - For other function-call-triggered state updates: use a **`Channel`** or **private `MutableStateFlow`** as an internal signal, then incorporate into the lazy block via `combine`/`merge`/`receiveAsFlow`.
  - **Never use the old `MutableStateFlow` + `_uiState.update {}` pattern** — flag for migration when encountered.
- **No `init` block** — trigger initial loading through lazy state evaluation or explicit UI calls.
- **Error handling**: `runCatching { }.onFailure { Timber.e(it, "msg") }` for suspend actions; `.catch { }` in flow chains.
- **Coroutines**: `viewModelScope.launch { }`.
- **Logging**: `Timber` only — never `android.util.Log`.

## One-Shot Event Channels

For one-shot UI events (navigation, snackbar, dialog triggers), use a separate `Channel<StateEventWithContent<T>>` per event type. Each channel is incorporated into the `combine` block via `.receiveAsFlow().onStart { emit(consumed()) }`.

Do **not** use a single event channel with `scan` and a sealed `EventUpdate` accumulator — that pattern is over-engineered. Keep it simple: one channel per event.

```kotlin
@HiltViewModel
internal class MyFeatureViewModel @Inject constructor(
    private val monitorDataUseCase: MonitorDataUseCase,
    private val performActionUseCase: PerformActionUseCase,
) : ViewModel() {

    private val navigationEventChannel = Channel<StateEventWithContent<Long>>(Channel.BUFFERED)
    private val actionEventChannel = Channel<StateEventWithContent<ActionResult>>(Channel.BUFFERED)

    val uiState: StateFlow<MyFeatureUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorDataUseCase()
                .catch { Timber.e(it) },
            navigationEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
            actionEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
        ) { data, navigationEvent, actionEvent ->
            MyFeatureUiState.Data(
                items = data,
                navigationEvent = navigationEvent,
                actionEvent = actionEvent,
            )
        }.catch { e ->
            Timber.e(e, "Failed to load data")
        }.asUiStateFlow(
            viewModelScope,
            MyFeatureUiState.Loading,
        )
    }

    fun onNavigate(id: Long) {
        viewModelScope.launch {
            runCatching {
                performActionUseCase(id)
            }.onSuccess { chatId ->
                navigationEventChannel.send(triggered(chatId))
            }.onFailure {
                Timber.e(it, "Navigation failed")
            }
        }
    }

    fun onNavigationEventConsumed() {
        navigationEventChannel.trySend(consumed())
    }

    fun onActionEventConsumed() {
        actionEventChannel.trySend(consumed())
    }
}
```

## Assisted Injection (Runtime Parameters)

When runtime parameters from navigation are needed:

- **Never pass the NavKey itself** as a ViewModel parameter.
- **Single parameter**: pass directly with `@Assisted`.
- **Multiple parameters**: create an `Args` data class inside the ViewModel.

```kotlin
@HiltViewModel(assistedFactory = MyFeatureViewModel.Factory::class)
internal class MyFeatureViewModel @AssistedInject constructor(
    private val someUseCase: SomeUseCase,
    @Assisted val args: Args,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: Args): MyFeatureViewModel
    }

    data class Args(val id: String, val type: SomeType)

    val uiState: StateFlow<MyFeatureUiState> by lazy(LazyThreadSafetyMode.NONE) {
        someUseCase(args.id)
            .map { MyFeatureUiState.Data(it) }
            .catch { e -> Timber.e(e, "Failed to load") }
            .asUiStateFlow(viewModelScope, MyFeatureUiState.Loading)
    }
}
```

## UI State

Use a `@Stable` sealed interface with substates:

```kotlin
@Stable
sealed interface MyFeatureUiState {
    data object Loading : MyFeatureUiState
    data class Data(
        val items: List<Item>,
        val navigationEvent: StateEventWithContent<Long>,
        val actionEvent: StateEventWithContent<ActionResult>,
    ) : MyFeatureUiState
}
```

When using the sealed `Loading`/`Data` pattern, the `Data` subclass properties should **not have default values**. All fields are supplied by the `combine` block in the ViewModel, so defaults would mask missing data and hide bugs at compile time.

For simpler screens, a flat `data class` with defaults is acceptable (since it represents a single evolving state):

```kotlin
@Stable
data class MyFeatureUiState(
    val isLoading: Boolean = true,
    val items: List<Item> = emptyList(),
    val error: String? = null,
)
```

## Mappers

```kotlin
internal class MyEntityMapper @Inject constructor() {
    operator fun invoke(domain: DomainEntity): UiModel =
        UiModel(
            id = domain.id,
            displayName = domain.name,
        )
}
```

- `internal class` — mappers in feature modules should be internal.
- `@Inject constructor()` — always.
- `operator fun invoke(...)` — for callable syntax. Only add `suspend` if the mapper performs actual async work.
- Single responsibility: domain entity -> UI model.
- Place in `mapper/` subpackage.

## File Placement

```
<module>/src/main/java/<package>/
    <Name>ViewModel.kt
    model/
        <Name>UiState.kt
        <UiModelClasses>.kt
    mapper/
        <Name>Mapper.kt
<module>/src/test/java/<package>/
    <Name>ViewModelTest.kt
    mapper/
        <Name>MapperTest.kt
```
