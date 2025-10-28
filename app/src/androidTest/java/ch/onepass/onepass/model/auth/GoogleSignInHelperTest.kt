package ch.onepass.onepass.model.auth

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleSignInHelperTest {

    private lateinit var googleSignInHelper: GoogleSignInHelper

    @Before
    fun setup() {
        googleSignInHelper = DefaultGoogleSignInHelper()
    }

    @Test
    fun testToFirebaseCredential() {
        val idToken = "test_id_token"
        val credential = googleSignInHelper.toFirebaseCredential(idToken)
        assertNotNull(credential)
        assertEquals(GoogleAuthProvider.PROVIDER_ID, credential.provider)
    }

    @Test
    fun testExtractIdTokenCredential_withEmptyBundle_throwsException() {
        val bundle = Bundle()
        assertThrows(GoogleIdTokenParsingException::class.java) {
            googleSignInHelper.extractIdTokenCredential(bundle)
        }
    }
}
