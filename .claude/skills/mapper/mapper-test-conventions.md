# Mapper Test Conventions

## Test Class Structure

### Simple Mapper (No Dependencies, Synchronous)

```kotlin
class ContactItemUiModelMapperTest {
    private lateinit var underTest: ContactItemUiModelMapper

    @BeforeEach
    fun setUp() {
        underTest = ContactItemUiModelMapper()
    }

    @Test
    fun `test that display name uses alias when available`() {
        val contactItem = createContactItem(alias = "MyAlias", fullName = "Full Name")

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("MyAlias")
    }

    @Test
    fun `test that display name uses email when both alias and fullName are null`() {
        val contactItem = createContactItem(alias = null, fullName = null)

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("test@example.com")
    }

    private fun createContactItem(
        handle: Long = 1L,
        email: String = "test@example.com",
        alias: String? = "Alias",
        fullName: String? = "Full Name",
        status: UserChatStatus = UserChatStatus.Offline,
    ) = ContactItem(
        handle = handle,
        email = email,
        contactData = ContactData(fullName = fullName, alias = alias),
        status = status,
    )
}
```

### Composed Mapper (Mocked Dependencies)

```kotlin
class AccountDetailMapperTest {
    private lateinit var underTest: AccountDetailMapper

    private val storageDetailMapper = mock<AccountStorageDetailMapper>()
    private val sessionDetailMapper = mock<AccountSessionDetailMapper>()

    @BeforeEach
    fun setUp() {
        underTest = AccountDetailMapper(
            accountStorageDetailMapper = storageDetailMapper,
            accountSessionDetailMapper = sessionDetailMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            storageDetailMapper,
            sessionDetailMapper,
        )
    }

    @Test
    fun `test that storage detail is mapped from storage detail mapper`() {
        val expectedStorage = mock<AccountStorageDetail>()
        whenever(storageDetailMapper(any())).thenReturn(expectedStorage)

        val result = underTest(createDetails())

        assertThat(result.storageDetail).isEqualTo(expectedStorage)
    }

    @Test
    fun `test that session detail mapper is called with correct arguments`() {
        val details = createDetails(numDetails = 5)

        underTest(details)

        verify(sessionDetailMapper).invoke(details, 5)
    }

    private fun createDetails(
        numDetails: Int = 1,
    ) = mock<MegaAccountDetails>()
}
```

### Suspend Mapper

```kotlin
@ExtendWith(CoroutineMainDispatcherExtension::class)
class NodeViewItemMapperTest {
    private lateinit var underTest: NodeViewItemMapper

    @BeforeEach
    fun setUp() {
        underTest = NodeViewItemMapper(
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that nodes are mapped to view items`() = runTest {
        val nodes = listOf(mock<TypedFileNode>(), mock<TypedFileNode>())

        val result = underTest(nodes, sourceFolder = null)

        assertThat(result).hasSize(2)
    }
}
```

## Key Rules

- **JUnit 5** — `@Test`, `@BeforeEach`, `@AfterEach`.
- **`@ExtendWith(CoroutineMainDispatcherExtension::class)`** — only for suspend mappers. Simple synchronous mappers do **not** need it.
- **`underTest`** — always name the mapper under test `underTest`.
- **Dependencies** — mock with `mock<T>()`, create mapper in `@BeforeEach`, reset mocks in `@AfterEach`.
- **No `@AfterEach`** needed for simple mappers with no mocked dependencies.
- **Test names** — backtick style: `` `test that X when Y` ``.
- **Assertions** — Google Truth: `assertThat(...).isEqualTo(...)`.
- **`= runTest { ... }`** — only for suspend mapper tests. Synchronous mapper tests do **not** need it.
- **No Turbine** — mappers do not emit Flows, so Turbine is not needed (unlike ViewModel tests).

## Testing Patterns

### Happy Path

Test the standard mapping with typical input values:

```kotlin
@Test
fun `test that id is mapped correctly`() {
    val input = DomainEntity(id = "123", name = "Test")

    val result = underTest(input)

    assertThat(result.id).isEqualTo("123")
}
```

### Null Input

If the mapper accepts nullable input, test with `null`:

```kotlin
@Test
fun `test that null input returns null`() {
    val result = underTest(null)

    assertThat(result).isNull()
}
```

### Null Fields

Test when individual fields in the input are null:

```kotlin
@Test
fun `test that display name falls back to email when alias is null`() {
    val input = createContactItem(alias = null)

    val result = underTest(input)

    assertThat(result.displayName).isEqualTo(input.email)
}
```

### List Mapping

Test empty list, single item, and multiple items:

```kotlin
@Test
fun `test that empty list returns empty list`() {
    val result = underTest(emptyList())

    assertThat(result).isEmpty()
}

@Test
fun `test that list items are mapped individually`() {
    val items = listOf(
        DomainItem(id = "1"),
        DomainItem(id = "2"),
    )

    val result = underTest(items)

    assertThat(result).hasSize(2)
    assertThat(result[0].id).isEqualTo("1")
    assertThat(result[1].id).isEqualTo("2")
}
```

### Edge Cases

Test boundary values, empty strings, zero values, and enum exhaustiveness:

```kotlin
@Test
fun `test that empty string name maps to empty display name`() {
    val input = createEntity(name = "")

    val result = underTest(input)

    assertThat(result.displayName).isEmpty()
}
```

### Verifying Dependent Mapper Calls

When testing composed mappers, verify that dependent mappers are called with the correct arguments:

```kotlin
@Test
fun `test that storage mapper is called with correct details`() {
    val details = createDetails()

    underTest(details)

    verify(storageDetailMapper).invoke(details)
}
```

### @StringRes Mapper

Test that each input maps to the correct string resource:

```kotlin
@Test
fun `test that FREE maps to free_account string`() {
    val result = underTest(AccountType.FREE)

    assertThat(result).isEqualTo(R.string.free_account)
}

@Test
fun `test that null maps to recovering_info string`() {
    val result = underTest(null)

    assertThat(result).isEqualTo(R.string.recovering_info)
}
```

## Test Data Helpers

Use `create*()` factory functions to build test input objects with sensible defaults:

```kotlin
private fun createContactItem(
    handle: Long = 1L,
    email: String = "test@example.com",
    alias: String? = "Alias",
    fullName: String? = "Full Name",
    status: UserChatStatus = UserChatStatus.Offline,
) = ContactItem(
    handle = handle,
    email = email,
    contactData = ContactData(fullName = fullName, alias = alias),
    status = status,
)
```

- Place helper functions at the bottom of the test class as `private` functions.
- Use default parameter values so each test only specifies the fields it cares about.
- Prefer constructing real domain objects over mocking them, unless the domain object has many required fields.

## Required Imports

```kotlin
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
// For suspend mappers only:
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.extension.ExtendWith
```