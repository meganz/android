package mega.privacy.android.data.gateway.contact

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactGatewayImplTest {

    private lateinit var underTest: ContactGateway

    private val context: Context = mock()

    @BeforeEach
    fun setup() {
        underTest = ContactGatewayImpl(context)
    }

    @AfterEach
    fun tearDown() {
        reset(context)
    }

    @Test
    fun `test that a list of local data is returned when contact items from the content provider are not NULL`() =
        runTest {
            val contactID = 1L
            val contactName = "name"
            val photoUri = "photoUri"
            val projection = arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
            )
            val mockCursor = mock<Cursor> {
                on { getLong(0) }.thenReturn(contactID)
                on { getString(1) }.thenReturn(contactName)
                on { getString(2) }.thenReturn(photoUri)
                on { getString(3) }.thenReturn(photoUri)

                on { moveToNext() }.thenReturn(true, false)
            }
            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        Contacts.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContacts()

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    name = contactName,
                    photoUri = UriPath(photoUri)
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that a local data with NULL photo data is returned when the contact item has no photo`() =
        runTest {
            val contactID = 1L
            val contactName = "name"
            val projection = arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
            )
            val mockCursor = mock<Cursor> {
                on { getLong(0) }.thenReturn(contactID)
                on { getString(1) }.thenReturn(contactName)
                on { getString(2) }.thenReturn(null)
                on { getString(3) }.thenReturn(null)

                on { moveToNext() }.thenReturn(true, false)
            }
            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        Contacts.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContacts()

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    name = contactName,
                    photoUri = null
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that the returned Local Contact contains a photo Uri when the photo thumbnail Uri is NULL`() =
        runTest {
            val contactID = 1L
            val contactName = "name"
            val photoUri = "photoUri"
            val projection = arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
            )
            val mockCursor = mock<Cursor> {
                on { getLong(0) }.thenReturn(contactID)
                on { getString(1) }.thenReturn(contactName)
                on { getString(2) }.thenReturn(photoUri)
                on { getString(3) }.thenReturn(null)

                on { moveToNext() }.thenReturn(true, false)
            }
            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        Contacts.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContacts()

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    name = contactName,
                    photoUri = UriPath(photoUri)
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that the returned Local Contact contains a photo thumbnail Uri when the photo thumbnail Uri is not NULL`() =
        runTest {
            val contactID = 1L
            val contactName = "name"
            val photoThumbnailUri = "photoThumbnailUri"
            val projection = arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
            )
            val mockCursor = mock<Cursor> {
                on { getLong(0) }.thenReturn(contactID)
                on { getString(1) }.thenReturn(contactName)
                on { getString(2) }.thenReturn(null)
                on { getString(3) }.thenReturn(photoThumbnailUri)

                on { moveToNext() }.thenReturn(true, false)
            }
            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        Contacts.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContacts()

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    name = contactName,
                    photoUri = UriPath(photoThumbnailUri)
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that the name of a local contact is empty when a contact's name is NULL`() =
        runTest {
            val contactID = 1L
            val contactName = null
            val projection = arrayOf(
                Contacts._ID,
                Contacts.DISPLAY_NAME,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI
            )
            val mockCursor = mock<Cursor> {
                on { getLong(0) }.thenReturn(contactID)
                on { getString(1) }.thenReturn(contactName)
                on { getString(2) }.thenReturn(null)
                on { getString(3) }.thenReturn(null)

                on { moveToNext() }.thenReturn(true, false)
            }
            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        Contacts.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContacts()

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    name = ""
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that an empty list is returned when contact items from the content provider are NULL`() =
        runTest {
            val contentResolver = mock<ContentResolver> {
                on { query(any(), any(), any(), any(), any()) }.thenReturn(null)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContacts()

            assertThat(actual).isEqualTo(emptyList<LocalContact>())
        }

    @Test
    fun `test that a map of contact IDs to pairs of lists of phone numbers and normalized phone numbers is returned when the contact's phone items from the content provider are not NULL`() =
        runTest {
            val contactID = 1L
            val phoneNumber = "1234567890"
            val normalizedPhoneNumber = "+621234567890"
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            )
            val mockCursor = mock<Cursor> {
                on { getLong(0) }.thenReturn(contactID)
                on { getString(1) }.thenReturn(phoneNumber)
                on { getString(2) }.thenReturn(normalizedPhoneNumber)

                on { moveToNext() }.thenReturn(true, false)
            }
            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContactNumbers()

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    phoneNumbers = listOf(phoneNumber),
                    normalizedPhoneNumbers = listOf(normalizedPhoneNumber)
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that an empty map is returned when contact's phone items from the content provider are NULL`() =
        runTest {
            val contentResolver = mock<ContentResolver> {
                on { query(any(), any(), any(), any(), any()) }.thenReturn(null)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContactNumbers()

            assertThat(actual).isEqualTo(emptyList<LocalContact>())
        }

    @Test
    fun `test that a map of contact IDs to pairs of lists of contact emails is returned when the contact's email items from the content provider are not NULL`() =
        runTest {
            val contactID = 1L
            val email = "test@test.com"
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            )
            val mockCursor = mock<Cursor> {
                on { getLong(0) }.thenReturn(contactID)
                on { getString(1) }.thenReturn(email)

                on { moveToNext() }.thenReturn(true, false)
            }
            val contentResolver = mock<ContentResolver> {
                on {
                    query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )
                }.thenReturn(mockCursor)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContactEmailAddresses()

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    emails = listOf(email)
                )
            )
            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that an empty list is returned when contact's email items from the content provider are NULL`() =
        runTest {
            val contentResolver = mock<ContentResolver> {
                on { query(any(), any(), any(), any(), any()) }.thenReturn(null)
            }
            whenever(context.contentResolver).thenReturn(contentResolver)

            val actual = underTest.getLocalContactEmailAddresses()

            assertThat(actual).isEqualTo(emptyList<LocalContact>())
        }
}
