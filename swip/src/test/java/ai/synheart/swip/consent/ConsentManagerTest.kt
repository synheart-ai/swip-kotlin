package ai.synheart.swip.consent

import ai.synheart.swip.models.ConsentContext
import ai.synheart.swip.models.ConsentLevel
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class ConsentManagerTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var consentManager: ConsentManager

    @Before
    fun setup() {
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putInt(anyString(), anyInt())).thenReturn(editor)
        `when`(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.clear()).thenReturn(editor)
        `when`(sharedPreferences.getInt(anyString(), anyInt())).thenReturn(0)

        consentManager = ConsentManager(context)
    }

    @Test
    fun `test default consent level is ON_DEVICE`() {
        // When creating a new ConsentManager
        // Then default consent should be ON_DEVICE
        assertEquals(ConsentLevel.ON_DEVICE, consentManager.currentLevel)
    }

    @Test
    fun `test grant consent updates level`() = runTest {
        // When granting consent
        consentManager.grantConsent(ConsentLevel.LOCAL_EXPORT, "User requested export")

        // Then consent level should be updated
        assertEquals(ConsentLevel.LOCAL_EXPORT, consentManager.currentLevel)
    }

    @Test
    fun `test revoke consent resets to ON_DEVICE`() = runTest {
        // Given elevated consent
        consentManager.grantConsent(ConsentLevel.DASHBOARD_SHARE, "Testing")

        // When revoking consent
        consentManager.revokeConsent()

        // Then should reset to ON_DEVICE
        assertEquals(ConsentLevel.ON_DEVICE, consentManager.currentLevel)
    }

    @Test
    fun `test can perform action checks consent level`() = runTest {
        // Given ON_DEVICE consent
        consentManager.grantConsent(ConsentLevel.ON_DEVICE, "Default")

        // Then ON_DEVICE actions are allowed
        assertTrue(consentManager.canPerformAction(ConsentLevel.ON_DEVICE))

        // But higher levels are not allowed
        assertFalse(consentManager.canPerformAction(ConsentLevel.LOCAL_EXPORT))
        assertFalse(consentManager.canPerformAction(ConsentLevel.DASHBOARD_SHARE))
    }

    @Test
    fun `test higher consent allows lower levels`() = runTest {
        // Given DASHBOARD_SHARE consent
        consentManager.grantConsent(ConsentLevel.DASHBOARD_SHARE, "Testing")

        // Then all lower levels should be allowed
        assertTrue(consentManager.canPerformAction(ConsentLevel.ON_DEVICE))
        assertTrue(consentManager.canPerformAction(ConsentLevel.LOCAL_EXPORT))
        assertTrue(consentManager.canPerformAction(ConsentLevel.DASHBOARD_SHARE))
    }

    @Test
    fun `test consent history tracks grants`() = runTest {
        // When granting different levels
        consentManager.grantConsent(ConsentLevel.LOCAL_EXPORT, "First grant")
        consentManager.grantConsent(ConsentLevel.DASHBOARD_SHARE, "Second grant")

        // Then history should contain both
        val history = consentManager.getConsentHistory()
        assertTrue(history.containsKey(ConsentLevel.LOCAL_EXPORT))
        assertTrue(history.containsKey(ConsentLevel.DASHBOARD_SHARE))
    }

    @Test
    fun `test consent validator throws on insufficient consent`() {
        // Given ON_DEVICE consent
        // When validating for higher level
        // Then should throw
        assertThrows(Exception::class.java) {
            ConsentValidator.validateConsent(
                required = ConsentLevel.DASHBOARD_SHARE,
                current = ConsentLevel.ON_DEVICE,
                operation = "test"
            )
        }
    }

    @Test
    fun `test consent validator passes with sufficient consent`() {
        // Given DASHBOARD_SHARE consent
        // When validating for lower level
        // Then should not throw
        ConsentValidator.validateConsent(
            required = ConsentLevel.LOCAL_EXPORT,
            current = ConsentLevel.DASHBOARD_SHARE,
            operation = "test"
        )
        // If no exception, test passes
        assertTrue(true)
    }

    @Test
    fun `test purge all data clears history`() = runTest {
        // Given some consent history
        consentManager.grantConsent(ConsentLevel.LOCAL_EXPORT, "Testing")

        // When purging all data
        consentManager.purgeAllData()

        // Then consent should reset
        assertEquals(ConsentLevel.ON_DEVICE, consentManager.currentLevel)
    }
}
