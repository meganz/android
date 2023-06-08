package mega.privacy.android.domain.entity.analytics.identifier

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EventIdentifierTest {

    @ParameterizedTest(name = "EventIdentifier: {1}")
    @MethodSource("provideConstructors")
    internal fun `test that an exception is thrown if the identifier is negative`(
        constructor: (Int) -> Any,
        className: String,
    ) {
        assertThrows<IllegalArgumentException> {
            constructor(-1)
        }
    }

    @ParameterizedTest(name = "EventIdentifier: {1}")
    @MethodSource("provideConstructors")
    internal fun `test that an exception is thrown if the identifier greater than 999`(
        constructor: (Int) -> Any,
        className: String,
    ) {
        assertThrows<IllegalArgumentException> {
            constructor(999 + 1)
        }
    }

    private fun provideConstructors() = Stream.of(
        Arguments.of(
            { id: Int ->
                DialogDisplayedEventIdentifier(
                    screenName = "",
                    dialogName = "",
                    uniqueIdentifier = id
                )
            },
            DialogDisplayedEventIdentifier::class.simpleName
        ),
        Arguments.of(
            { id: Int ->
                ScreenViewEventIdentifier(
                    name = "",
                    uniqueIdentifier = id
                )
            },
            ScreenViewEventIdentifier::class.simpleName
        ),
        Arguments.of(
            { id: Int ->
                TabSelectedEventIdentifier(
                    screenName = "",
                    tabName = "",
                    uniqueIdentifier = id
                )
            },
            TabSelectedEventIdentifier::class.simpleName
        ),
        Arguments.of(
            { id: Int ->
                ButtonPressedEventIdentifier(
                    buttonName = "",
                    uniqueIdentifier = id,
                    screenName = "",
                    dialogName = "",
                )
            },
            ButtonPressedEventIdentifier::class.simpleName
        ),
        Arguments.of(
            { id: Int ->
                NavigationEventIdentifier(
                    uniqueIdentifier = id,
                    navigationElementType = "",
                    destination = "",
                )
            },
            NavigationEventIdentifier::class.simpleName
        ),
        Arguments.of(
            { id: Int ->
                MenuItemEventIdentifier(
                    menuItem = "",
                    uniqueIdentifier = id,
                    screenName = "",
                    menuType = "",
                )
            },
            MenuItemEventIdentifier::class.simpleName
        ),
        Arguments.of(
            { id: Int ->
                GeneralEventIdentifier(
                    name = "",
                    info = "",
                    uniqueIdentifier = id
                )
            },
            GeneralEventIdentifier::class.simpleName
        ),
    )

}