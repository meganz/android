---
name: android-code-review
description: >
  Android PR Code Review skill. Performs a comprehensive code review on the current Git
  repository's PR or specified code changes. Covers all review dimensions including architecture,
  Kotlin code quality, Android platform best practices, performance, security, and testability —
  along with a standardized output format. Can also be used as a standalone reference for
  Android review standards.
triggers:
  - /android-code-review
  - review this MR
  - do a code review
---

# Android Code Review

## Usage

```
/android-code-review                          # Review diff between current branch and develop
/android-code-review --branch feature/login   # Review a specific branch
/android-code-review --base develop           # Use a different base branch (default: develop)
/android-code-review --file path/to/File.kt   # Review a single file
/android-code-review --focus security         # Focus on a specific dimension (security/performance/arch)
/android-code-review --severity major         # Only show issues at or above the specified severity
/android-code-review --output ./review.md     # Save report to a file
```

## Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `--branch <name>` | Specify the branch to review | `--branch feature/payment` |
| `--base <branch>` | Base branch for comparison (default: `develop`) | `--base main` |
| `--file <path>` | Review a single file | `--file app/src/.../LoginViewModel.kt` |
| `--focus <dimension>` | Focus on a specific review dimension | `--focus performance` |
| `--severity <level>` | Only show issues at or above this level | `--severity major` |
| `--output <path>` | Save the report to a file | `--output ./review.md` |

## Execution Steps

### Step 1 — Fetch Code Changes
Retrieve the code to review based on the provided arguments:

```bash
# Default: current branch vs develop (or --base value if specified)
git log ${BASE:-develop}...HEAD --oneline
git diff ${BASE:-develop}...HEAD --stat
git diff ${BASE:-develop}...HEAD

# If --branch is specified
git diff ${BASE:-develop}...<branch-name>

# If --file is specified, read the file directly
```

> Note: this reviews **committed** changes only. Uncommitted staged/unstaged changes are not included. Use `--base` to change the base branch (default: `develop`).

### Step 2 — Analyze
Using the dimensions and checklists defined below, analyze each changed file for:
- Architecture & design correctness
- Kotlin code quality
- Android platform best practices
- Performance issues
- Security vulnerabilities
- Testability
- Readability & coding standards

For large PRs (> 500 lines), prioritize core business logic files.

### Step 3 — Generate Review Report
Output the report following the Standard Output Format defined below.

### Step 4 — Save Report (Optional)
If `--output <path>` was specified, use the Write tool to save the full report markdown to that path.

## Notes
- If git commands are unavailable, prompt the user to paste the code directly
- Always deliver feedback in a **constructive and respectful tone**

---

## Tech Stack Conventions
```
Language:         Kotlin (no new Java files)
Min SDK:          API 24 (Android 8.0)
Target SDK:       API 36
Architecture:     MVVM + Clean Architecture
UI Framework:     Jetpack Compose (new screens), View system (legacy screens)
DI:               Hilt
Async:            Kotlin Coroutines + Flow
Local Storage:    Room, DataStore, SQLCipher (encrypted DB)
Image Loading:    Coil
Unit Testing:     JUnit5 + MockK + Turbine
UI Testing:       Compose Testing
Build:            Convention plugins (build-logic/convention/), Version Catalogs (gradle/catalogs/)
```

---

## Review Dimensions & Checklists

> **ViewModel files**: When reviewing `*ViewModel.kt` files, also apply the conventions in [viewmodel-conventions.md](../viewmodel/viewmodel-conventions.md).

### 1. Architecture & Design

**Checklist:**
- [ ] Follows MVVM layering: UI → ViewModel → UseCase → Repository → Facade → Gateway
- [ ] Dependency direction is correct (Repository must not depend on ViewModel)
- [ ] Each UseCase has a single responsibility
- [ ] No cross-layer direct calls (e.g., UI layer accessing Repository directly)
- [ ] New modules are discussed before implementation
- [ ] Interfaces are appropriately abstracted (not over- or under-engineered)
- [ ] Hilt annotations used correctly (`@HiltViewModel`, `@AndroidEntryPoint`, `@Module @InstallIn`)
- [ ] Gateway/Facade interfaces used to abstract SDK access (not calling SDK directly from Repository)
- [ ] New repository implementations live in the data layer/package in a `data` module, not in the `app` module
- [ ] Repositories never inject use cases — this inverts Clean Architecture's dependency flow
- [ ] Repositories stay focused on data access; business/orchestration logic belongs in use cases
- [ ] `@HiltViewModel` ViewModels use `hiltViewModel()` in Compose — never `viewModel()` (causes `NoSuchMethodException` crash at runtime)

#### Modular Dependency checklist
- [ ] **`:feature:`** modules can depend on `:shared`, `:core`, and their own `:*-snowflakes`.
- [ ] **`:shared:`** modules can depend on `:core` and their own `:*-snowflakes`.
- [ ] **`:core:`** modules can only depend on other `:core` modules.
- [ ] **`Snowflake`** modules (ending in `-snowflakes` or `-snowflake-components`) can only depend on `:core`.


**Common Issues:**
```kotlin
// ❌ ViewModel holding Context — causes memory leaks
class LoginViewModel(private val context: Context) : ViewModel()

// ✅ Use @ApplicationContext annotation is fine in ViewModel if needed
class LoginViewModel(@ApplicationContext private val context: Context) : ViewModel()

// ✅ Use Application if needed
class LoginViewModel(private val app: Application) : ViewModel()

// ❌ Repository injected directly into Fragment
class LoginFragment {
    @Inject lateinit var userRepository: UserRepository
}

// ✅ Access data only through ViewModel
class LoginFragment {
    private val viewModel: LoginViewModel by viewModels()
}

// ❌ SDK called directly from Repository — bypasses gateway abstraction
class DefaultAccountRepository @Inject constructor(
    private val megaApi: MegaApiAndroid  // ❌ SDK dependency
)

// ✅ Abstract SDK behind a Gateway interface
class DefaultAccountRepository @Inject constructor(
    private val megaApiGateway: MegaApiGateway  // ✅ Gateway interface
)

// ❌ Hilt module without scope annotation — new instance on every injection
@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideDatabase(): AppDatabase = ...  // ❌ missing @Singleton
}

// ✅ Scoped correctly
@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Singleton
    @Provides
    fun provideDatabase(): AppDatabase = ...
}

// ❌ Repository in app module — belongs in data layer
// app/src/main/kotlin/.../DefaultUserRepository.kt

// ✅ Repository in data module
// data/account/src/main/kotlin/.../DefaultAccountRepository.kt

// ❌ Use case injected into repository — inverts dependency flow
class DefaultUserRepository @Inject constructor(
    private val loginUseCase: LoginUseCase  // ❌
)

// ✅ Repository depends only on data sources, gateways, mappers
class DefaultUserRepository @Inject constructor(
    private val userApiGateway: UserApiGateway,
    private val userMapper: UserMapper
)

// ❌ viewModel() with @HiltViewModel — crashes with NoSuchMethodException (no no-arg constructor)
@HiltViewModel
class LoginViewModel @Inject constructor(private val useCase: LoginUseCase) : ViewModel()

@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) // ❌ CRASH

// ✅ Use hiltViewModel() so Hilt provides constructor dependencies
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) // ✅

// ❌ Business logic in repository — orchestration belongs in use case
class DefaultUserRepository {
    suspend fun getActiveUser(): User {
        val user = api.fetchUser()
        if (user.isExpired) refreshToken()  // ❌ business logic
        return user
    }
}

// ✅ Repository: data access only. Use case: orchestration
class DefaultUserRepository {
    suspend fun getUser(): User = api.fetchUser()
}
class GetActiveUserUseCase {
    suspend operator fun invoke(): User {
        val user = repository.getUser()
        if (user.isExpired) refreshTokenUseCase()
        return user
    }
}
```

---

### 2. Kotlin Code Quality

**Checklist:**
- [ ] `val` preferred over `var` wherever possible
- [ ] No unsafe `!!` non-null assertions
- [ ] Scope functions (`let`, `run`, `apply`, `also`, `with`) used appropriately
- [ ] Data-holding classes use `data class`
- [ ] `sealed class` used for state/result modeling
- [ ] Extension functions are placed logically and not overused
- [ ] Naming follows conventions (camelCase, semantically clear)
- [ ] No duplicated code (DRY principle)
- [ ] Functions are reasonably sized (recommended ≤ 40 lines)
- [ ] Complex logic has explanatory comments

**Common Issues:**
```kotlin
// ❌ Force unwrap — potential crash
val name = user!!.name

// ✅ Safe handling
val name = user?.name ?: "Unknown"

// ❌ Mutable var for a value that never changes
var userId: String = "123"

// ✅
val userId: String = "123"

// ✅ Sealed class for UI state
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: UserInfo) : UiState()
    data class Error(val message: String) : UiState()
}
```

---

### 3. Android Platform Best Practices

**Checklist:**
- [ ] No memory leak risk (Fragment holding View references beyond lifecycle)
- [ ] StateFlow collected within the correct lifecycle scope
- [ ] No long-running operations on the main thread
- [ ] Permissions follow the principle of least privilege
- [ ] Configuration changes (e.g., screen rotation) are handled properly
- [ ] Composables avoid creating side effects outside `LaunchedEffect` / `SideEffect`
- [ ] `LaunchedEffect` / `SideEffect` / `DisposableEffect` are used correctly
- [ ] Use `Timber` for logging, do not use Android Logger
- [ ] Navigation entry metadata uses `buildMetadata { }` DSL instead of direct creation functions

**Common Issues:**
```kotlin
// ❌ Collects Flow without lifecycle awareness — runs in background
class MyFragment : Fragment() {
    override fun onViewCreated(...) {
        lifecycleScope.launch {
            viewModel.uiState.collect { ... }
        }
    }
}

// ✅ Use repeatOnLifecycle to stop collection when backgrounded
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { ... }
    }
}

// ❌ Side effect called directly in Composable — runs on every recomposition
@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    viewModel.loadUser()
}

// ✅ Use LaunchedEffect for one-time side effects
@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }
}

// ❌ Direct metadata creation — does not compose with other metadata extensions
entry<InfoPsaBottomSheet>(
    metadata = bottomSheetMetadata(
        dismissOnBack = false,
        dismissOnOutsideClick = false
    )
) { ... }

// ✅ Use buildMetadata DSL — composable and extensible
entry<StandardPsaBottomSheet>(
    metadata = buildMetadata {
        withBottomSheet(
            dismissOnBack = false,
            dismissOnOutsideClick = false
        )
    },
) { ... }
```

---

### 4. Coroutines & Async

**Checklist:**
- [ ] Correct Dispatcher used (dispatchers are injected by DI with correct annotation, main thread for UI updates)
- [ ] Exceptions are properly handled (`runCatching`)
- [ ] No coroutine leaks (all coroutines are cancellable / scoped)
- [ ] `flowOn` used in the appropriate place for thread switching
- [ ] `GlobalScope` is not used
- [ ] `suspend` functions follow single responsibility
- [ ] Dispatchers should be injected by annotation (like `@IoDispatcher`, `@MainDispatcher` or `@DefaultDispatcher`), instead of specifying concrete type  

**Common Issues:**
```kotlin
// ❌ UI update inside IO dispatcher. 
viewModelScope.launch(ioDispatcher) {
    val data = repository.fetchData()
    uiState.value = data  // ❌ Must update on main thread
}

// ✅ Switch context properly
viewModelScope.launch {
    val data = withContext(ioDispatcher) { repository.fetchData() }
    uiState.value = data
}

// ❌ Unhandled exception — causes crash
viewModelScope.launch {
    repository.fetchUser()
}

// ✅ Handle errors explicitly
viewModelScope.launch {
    runCatching { repository.fetchUser() }
        .onSuccess { uiState.value = UiState.Success(it) }
        .onFailure { uiState.value = UiState.Error(it.message ?: "Unknown error") }
}
```

---

### 5. Performance

**Checklist:**
- [ ] Compose: unnecessary recompositions avoided (`remember`, `derivedStateOf` used correctly)
- [ ] Lists use `LazyColumn` (never full lists inside `ScrollView`)
- [ ] Images loaded with explicit size constraints (prevent OOM)
- [ ] No database or network calls on the main thread
- [ ] ViewModel does not cache unnecessarily large datasets in memory

**Common Issues:**
```kotlin
// ❌ Expensive operation runs on every recomposition
@Composable
fun UserList(users: List<User>) {
    val sortedUsers = users.sortedBy { it.name }
}

// ✅ Cache with remember, keyed on input
@Composable
fun UserList(users: List<User>) {
    val sortedUsers = remember(users) { users.sortedBy { it.name } }
}
```

---

### 6. Security

**Checklist:**
- [ ] Sensitive data (tokens, passwords) not stored in plaintext SharedPreferences
- [ ] No sensitive data logged (e.g., `Log.d("token", token)`)
- [ ] User inputs are validated and sanitized
- [ ] Sensitive operations require authentication
- [ ] Database encrypted with SQLCipher
- [ ] Sensitive fields in entities/mappers encrypted using `EncryptData` before persistence
- [ ] No API keys or secrets committed to the repository

**Common Issues:**
```kotlin
// ❌ Token stored in plaintext
sharedPreferences.edit().putString("token", authToken).apply()

// ✅ Use EncryptedSharedPreferences
val encryptedPrefs = EncryptedSharedPreferences.create(
    context, "secure_prefs",
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
encryptedPrefs.edit().putString("token", authToken).apply()

// ❌ Logging sensitive data
Log.d("Auth", "User token: $token")

// ✅ Debug-only and redacted
if (BuildConfig.DEBUG) Log.d("Auth", "Token received successfully")

// ❌ Sensitive field stored unencrypted in Room entity
@Entity
data class BackupEntity(val backupId: String)

// ✅ Encrypt sensitive fields using EncryptData before persisting
suspend operator fun invoke(backup: Backup): BackupEntity? {
    return BackupEntity(
        encryptedBackupId = encryptData(backup.backupId.toString()) ?: return null
    )
}
```

---

### 7. Testability

> **ViewModel tests**: When reviewing ViewModel test files (`*ViewModelTest.kt`), also apply the conventions in [viewmodel-test-conventions.md](../viewmodel/viewmodel-test-conventions.md).

**Checklist:**
- [ ] ViewModel, UseCase and Repository implementation business logics have corresponding unit tests
- [ ] **Test method naming:** Follow the patterns below (see Test Method Naming)
- [ ] New logic has corresponding tests — no missing tests for new behavior
- [ ] Compose View has UI tests
- [ ] ViewModels can be tested without Android framework dependencies
- [ ] Test coverage meets team requirements (recommended ≥ 80% for core logic)
- [ ] Test class annotated with `@ExtendWith(CoroutineMainDispatcherExtension::class)` and `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`
- [ ] Flow emissions tested using Turbine (`underTest.state.test { ... }`)

#### Test Method Naming
- **Format**: Use one of the following patterns for test method names:
  - `` `test that <method> <action>` `` — e.g. `` `test that init does not call loginToFolderUseCase` ``()
  - `` `test that <method> <action> when <cause>` `` — e.g. `` `test that init emits Loaded when login succeeds` ``()

**Common Issues:**
```kotlin
// ❌ Test name does not follow "test that" patterns — inconsistent with project convention
@Test
fun `loading state is shown`() { ... }

// ✅ Use "test that <method> <action>" or "test that <method> <action> when <cause>"
@Test
fun `test that init emits Loaded when login succeeds`() { ... }
@Test
fun `test that init does not call loginToFolderUseCase`() { ... }
```

---

### 8. Code Style & Formatting

**Checklist:**
- [ ] Use explicit imports at the top of the file — never inline fully qualified class names in code
- [ ] Package structure follows conventions (e.g., `mega.privacy.android.feature.{feature}.{layer}`)
- [ ] Resources follow naming conventions (`ic_` icons, `bg_` backgrounds, `item_` list layouts)
- [ ] No hardcoded strings (should be in `shared_strings.xml`)
- [ ] Naming and comments are clear; intent is obvious (no unclear naming or comments)
- [ ] No leftover debug code or TODOs in production code
- [ ] **Extra or unused code after implementation switch** — When the change refactors or replaces an approach, check for leftover files, types, or dependencies that are no longer used and suggest removing or slimming them
- [ ] Naming conventions followed:
  - ViewModels: `{Feature}ViewModel` (e.g., `GlobalStateViewModel`)
  - Use Cases: `{Action}UseCase` (e.g., `LoginUseCase`)
  - Repositories: `Default{Feature}Repository` (e.g., `DefaultAccountRepository`)
  - Mappers: `{Source}Mapper` (e.g., `TransferMapper`)
  - Test files: `{ClassName}Test.kt`
- [ ] Indentation uses 4 spaces (not tabs) consistently, including Compose modifier chains and multi-line parameters
- [ ] KDoc present on public API classes, functions, and interfaces
- [ ] Build files use module name as filename (e.g., `feature/home/home.gradle.kts`)
- [ ] Convention plugins used for build configuration (`mega.android.library`, `mega.android.hilt`, etc.)

**Common Issues:**
```kotlin
// ❌ Inline fully qualified name
val handler = android.os.Handler(android.os.Looper.getMainLooper())

// ✅ Explicit import at top
import android.os.Handler
import android.os.Looper
val handler = Handler(Looper.getMainLooper())
```

---

## Severity Level Definitions

| Level | Badge | Description | Blocks Merge? |
|-------|-------|-------------|---------------|
| Critical | 🔴 | Crash risk, data breach, severe performance issue | **Yes** |
| Major | 🟠 | Architecture violation, memory leak, logic error | **Yes** |
| Minor | 🟡 | Readability, naming conventions, small optimizations | No |
| Suggestion | 🔵 | Optional improvement, not required in this PR | No |

---

## Standard Output Format

All review reports must strictly follow this format:

````markdown
# PR Code Review Report

## Summary
- **Branch**: userid/JiraID-branch → develop
- **Files Changed**: X
- **Review Date**: YYYY-MM-DD HH:mm
- **Overall**: [One-sentence overall assessment]

## Issue Overview
| Severity | Count |
|----------|-------|
| 🔴 Critical | X |
| 🟠 Major | X |
| 🟡 Minor | X |
| 🔵 Suggestion | X |

---

## Detailed Findings

### `path/to/FileName.kt`

#### 🔴 [Critical] Issue Title
**Location**: Line XX
**Problem**: Clear description of what the issue is and why it's risky.
**Suggestion**:
```kotlin
// ❌ Current code
...

// ✅ Suggested fix
...
```

#### 🟡 [Minor] Issue Title
**Location**: Line XX
**Problem**: ...
**Suggestion**: ...

---

## Highlights 👍
> Good practices worth acknowledging (at least one per review)
- `FileName.kt`: ...

## Conclusion
> [Approved ✅ / Request Changes 🔄 / Needs Discussion 💬]
>
> Brief explanation of the conclusion, and a list of blocking issues that must be fixed before merge (if any).
````

---

## Review Mindset & Etiquette

1. **Critique the code, not the person** — say "this function" not "you wrote"
2. **Explain the why** — every issue should include a reason, not just "this is wrong"
3. **Always provide a solution** — point out problems and suggest concrete fixes
4. **Acknowledge the good** — find at least one thing done well in every review
5. **Distinguish must-fix from nice-to-have** — Minor and Suggestion items are not blocking
