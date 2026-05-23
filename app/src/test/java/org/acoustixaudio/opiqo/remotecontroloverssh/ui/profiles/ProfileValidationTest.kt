package org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidationTest {
    @Test
    fun validateSshProfileInput_requiresExpectedFields() {
        val errors = validateSshProfileInput(
            alias = "",
            host = "",
            port = "70000",
            username = "",
            hasPrivateKey = false,
            fingerprint = "invalid"
        )

        assertTrue(errors.hasErrors())
        assertTrue(errors.alias != null)
        assertTrue(errors.host != null)
        assertTrue(errors.port != null)
        assertTrue(errors.username != null)
        assertTrue(errors.privateKey != null)
        assertTrue(errors.fingerprint != null)
    }

    @Test
    fun validateSshProfileInput_acceptsFingerprintAndPort() {
        val errors = validateSshProfileInput(
            alias = "Living Room",
            host = "192.168.1.20",
            port = "22",
            username = "dj",
            hasPrivateKey = true,
            fingerprint = "SHA256:AbCdEf1234567890+/="
        )

        assertFalse(errors.hasErrors())
    }

    @Test
    fun validateRemoteProfileInput_requiresName() {
        assertTrue(validateRemoteProfileInput("").hasErrors())
        assertFalse(validateRemoteProfileInput("TV Remote").hasErrors())
    }
}
