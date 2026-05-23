package org.acoustixaudio.opiqo.remotecontroloverssh

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class RemoteControlApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        setupBouncyCastle()
    }

    private fun setupBouncyCastle() {
        // Remove the restricted Android system provider
        Security.removeProvider("BC")
        // Insert the full Bouncy Castle provider at the top priority
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}
