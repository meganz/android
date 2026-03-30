# Mapper Production Code Conventions

## Class Structure

### Simple Mapper (No Dependencies)

```kotlin
internal class ContactItemUiModelMapper @Inject constructor() {

    operator fun invoke(contactItem: ContactItem): ContactUiModel =
        ContactUiModel(
            handle = contactItem.handle,
            email = contactItem.email,
            displayName = contactItem.contactData.alias ?: contactItem.email,
        )
}
```

### Composed Mapper (With Dependencies)

```kotlin
internal class ReferralBonusAchievementsMapper @Inject constructor(
    private val numberOfDaysMapper: NumberOfDaysMapper,
) {

    operator fun invoke(
        awardedAchievementInvite: AwardedAchievementInvite,
        contact: ContactItem?,
    ): ReferralBonusAchievements =
        ReferralBonusAchievements(
            contact = contact,
            expirationInDays = numberOfDaysMapper(awardedAchievementInvite.expirationTimestampInSeconds),
            awardId = awardedAchievementInvite.awardId,
        )
}
```

## Key Rules

- **`class` + `@Inject constructor()`** — always. No `fun interface`, no `typealias`, no top-level functions.
- **`operator fun invoke(...)`** — for callable syntax: `val result = mapper(input)`.
- **Single responsibility** — one mapper transforms one type to another. If a transformation involves multiple distinct steps, compose separate mappers.
- **No state** — mappers are stateless. All data flows through `invoke` parameters.
- **Dependencies** — inject other mappers only. Avoid injecting use cases or repositories unless the mapper performs actual async work (rare). Never inject `Context` (see String Resources below).

## Visibility Rules

- **Feature modules**: `internal class` — mappers should not leak outside the module.
- **Data module**: `internal class` — data layer mappers are implementation details.
- **Domain module**: `internal class` domain layer mappers are implementation details.
- **Shared modules**: `class` (public) — shared mappers are consumed by multiple modules.
- **Core modules**: `class` (public) — core mappers are consumed by multiple modules.

## Naming Conventions

- **Standard**: `{Source}Mapper` — e.g., `ContactItemUiModelMapper`, `TransferMapper`.
- **Clarifying**: `{Source}To{Target}Mapper` — use when the transformation is not obvious from the source type alone, e.g., `StalledIssueToSolvedIssueMapper`.
- **List mappers**: Do **not** create separate `{Source}ListMapper` classes. Instead, add a list overload to the element mapper (see List Mapping below).

## Null Handling

### Nullable Input

```kotlin
operator fun invoke(input: DomainEntity?): UiModel? =
    input?.let {
        UiModel(
            id = it.id,
            name = it.name.orEmpty(),
        )
    }
```

### Nullable Fields

Use safe calls and Elvis operator for nullable fields:

```kotlin
operator fun invoke(input: DomainEntity): UiModel =
    UiModel(
        displayName = input.alias ?: input.fullName ?: input.email,
        avatarUri = input.avatarUri,
    )
```

## List / Collection Mapping

Add a list overload that delegates to the single-item `invoke`:

```kotlin
internal class ItemMapper @Inject constructor() {

    operator fun invoke(item: DomainItem): UiItem =
        UiItem(id = item.id, name = item.name)

    operator fun invoke(items: List<DomainItem>): List<UiItem> =
        items.map { invoke(it) }
}
```

For filtering nulls during list mapping:

```kotlin
operator fun invoke(items: List<DomainItem?>): List<UiItem> =
    items.mapNotNull { it?.let { item -> invoke(item) } }
```

## Suspend Mappers

Only use `suspend operator fun invoke()` when the mapper performs actual async work (e.g., file I/O, calling a use case). Most mappers should be synchronous.

```kotlin
internal class NodeViewItemMapper @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(
        nodeList: List<TypedNode>,
        sourceFolder: TypedFolderNode?,
    ): List<NodeViewItem<TypedNode>> = withContext(ioDispatcher) {
        nodeList.map { node ->
            NodeViewItem(
                node = node,
                isSelected = false,
            )
        }
    }
}
```

## String Resources — Anti-Pattern

Mappers must **never** inject `Context` or `@ApplicationContext`. String resolution belongs in the UI layer.

### Preferred: Let UI Resolve Strings

Return domain/enum values; the Composable resolves the display string:

```kotlin
// Mapper returns the enum directly
internal class StatusMapper @Inject constructor() {
    operator fun invoke(input: DomainStatus): UiStatus =
        UiStatus(status = input.status)
}

// Composable resolves the string
@Composable
fun StatusText(status: UiStatus) {
    Text(text = when (status.status) {
        Status.ACTIVE -> stringResource(R.string.status_active)
        Status.INACTIVE -> stringResource(R.string.status_inactive)
    })
}
```

### Acceptable Exception: `@StringRes` Return

When a mapper's sole purpose is to map a value to a string resource ID, return `@StringRes Int`:

```kotlin
class AccountTypeNameMapper @Inject constructor() {

    @StringRes
    operator fun invoke(input: AccountType?): Int =
        when (input) {
            AccountType.FREE -> R.string.free_account
            AccountType.PRO_LITE -> R.string.prolite_account
            AccountType.PRO_I -> R.string.pro1_account
            else -> R.string.recovering_info
        }
}

// Resolved in Compose:
Text(text = stringResource(accountTypeNameMapper(accountType)))
```

### Never Do This (Legacy Anti-Pattern)

```kotlin
// DO NOT: inject Context to resolve strings
class BadMapper @Inject constructor(
    @ApplicationContext private val context: Context, // Anti-pattern
) {
    operator fun invoke(result: Result): String =
        context.getString(R.string.success_message) // Anti-pattern
}
```

## Composed Mapper Patterns

Inject other mappers as constructor dependencies to keep each mapper single-responsibility:

```kotlin
internal class AccountDetailMapper @Inject constructor(
    private val accountStorageDetailMapper: AccountStorageDetailMapper,
    private val accountSessionDetailMapper: AccountSessionDetailMapper,
    private val accountTransferDetailMapper: AccountTransferDetailMapper,
) {

    operator fun invoke(
        details: MegaAccountDetails,
        numDetails: Int,
    ): AccountDetail =
        AccountDetail(
            sessionDetail = accountSessionDetailMapper(details, numDetails),
            transferDetail = accountTransferDetailMapper(details),
            storageDetail = accountStorageDetailMapper(details),
        )
}
```

## Private Helper Methods

For complex mappers, extract logic into private methods:

```kotlin
internal class ContactItemUiModelMapper @Inject constructor() {

    operator fun invoke(contactItem: ContactItem): ContactUiModel =
        ContactUiModel(
            displayName = resolveDisplayName(contactItem),
            isNew = isWithinLastThreeDays(contactItem.timestamp) && contactItem.chatroomId == null,
        )

    private fun resolveDisplayName(contactItem: ContactItem): String {
        val alias = contactItem.contactData.alias
        val fullName = contactItem.contactData.fullName
        return when {
            !alias.isNullOrBlank() -> alias
            !fullName.isNullOrBlank() -> fullName
            else -> contactItem.email
        }
    }

    private fun isWithinLastThreeDays(timestamp: Long): Boolean {
        val now = LocalDateTime.now()
        val addedTime = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return Duration.between(addedTime, now).toDays() < 3
    }
}
```

## Legacy Patterns to Avoid

The following patterns exist in the codebase but should **not** be used in new code. Flag them in code reviews and migrate in refactor mode.

| Pattern | Problem | Preferred |
|---------|---------|-----------|
| `fun interface Mapper { ... }` + `MapperImpl` | Unnecessary interface/impl split, requires `@Binds` module | `class Mapper @Inject constructor()` |
| `typealias Mapper = (Input) -> Output` | Not a class, requires `@Provides` module, limited composability | `class Mapper @Inject constructor()` |
| `@ApplicationContext context: Context` | Couples mapper to Android framework, harder to test | Return `@StringRes Int` or let UI resolve |
| Mapper without `@Inject constructor` | Cannot be automatically provided by Hilt | Add `@Inject constructor()` |

## File Placement

```
<module>/src/main/java/<package>/
    mapper/
        <Name>Mapper.kt
<module>/src/test/java/<package>/
    mapper/
        <Name>MapperTest.kt
```

In feature modules, place mappers in the appropriate layer subpackage:
- `presentation/mapper/` — for domain-to-UI model mappers
- `data/mapper/` — for SDK/API-to-domain entity mappers
- `domain/mapper/` — for domain-to-domain transformations (rare)

Match existing module conventions by searching for existing mappers in the target module before creating new ones.