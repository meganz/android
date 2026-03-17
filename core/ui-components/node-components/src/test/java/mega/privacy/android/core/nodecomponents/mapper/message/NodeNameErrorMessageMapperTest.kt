package mega.privacy.android.core.nodecomponents.mapper.message

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.domain.entity.InvalidNameType
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeNameErrorMessageMapperTest {

    private val underTest = NodeNameErrorMessageMapper()


    @ParameterizedTest
    @EnumSource(InvalidNameType::class)
    fun `test that maps correctly for files`(invalidNameType: InvalidNameType) {
        assertThat(underTest(invalidNameType, false)).isEqualTo(
            when (invalidNameType) {
                InvalidNameType.BLANK_NAME -> NodesR.string.invalid_string
                InvalidNameType.INVALID_NAME -> sharedR.string.general_invalid_characters_defined
                InvalidNameType.NAME_ALREADY_EXISTS -> NodesR.string.same_file_name_warning
                InvalidNameType.NO_EXTENSION -> NodesR.string.file_without_extension_warning
                InvalidNameType.DOT_NAME -> sharedR.string.general_invalid_dot_name_warning
                InvalidNameType.DOUBLE_DOT_NAME -> sharedR.string.general_invalid_double_dot_name_warning
                else -> null
            }
        )
    }

    @ParameterizedTest
    @EnumSource(InvalidNameType::class)
    fun `test that maps correctly for folders`(invalidNameType: InvalidNameType) {
        assertThat(underTest(invalidNameType, true)).isEqualTo(
            when (invalidNameType) {
                InvalidNameType.BLANK_NAME -> sharedR.string.create_new_folder_dialog_error_message_empty_folder_name
                InvalidNameType.INVALID_NAME -> sharedR.string.general_invalid_characters_defined
                InvalidNameType.NAME_ALREADY_EXISTS -> sharedR.string.create_new_folder_dialog_error_existing_folder
                InvalidNameType.NO_EXTENSION -> NodesR.string.file_without_extension_warning
                InvalidNameType.DOT_NAME -> sharedR.string.general_invalid_dot_name_warning
                InvalidNameType.DOUBLE_DOT_NAME -> sharedR.string.general_invalid_double_dot_name_warning
                else -> null
            }
        )
    }
}