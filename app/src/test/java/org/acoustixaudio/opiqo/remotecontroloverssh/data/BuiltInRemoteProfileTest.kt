package org.acoustixaudio.opiqo.remotecontroloverssh.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInRemoteProfileTest {
    @Test
    fun parseBuiltInRemoteProfiles_readsCommandsFromJson() {
        val json = """
            {
              "profiles": [
                {
                  "id": "desktop-navigation-volume",
                  "name": "Desktop Navigation + Volume",
                  "commands": {
                    "DPAD_UP": "xdotool key Up",
                    "SLIDER_1_STEP_5": "pactl set-sink-volume @DEFAULT_SINK@ 50%"
                  }
                }
              ]
            }
        """.trimIndent()

        val profiles = parseBuiltInRemoteProfiles(json)

        assertEquals(1, profiles.size)
        assertEquals("desktop-navigation-volume", profiles.first().id)
        assertEquals("xdotool key Up", profiles.first().commands[RemoteControlConfig.DPAD_UP])
        assertEquals(
            "pactl set-sink-volume @DEFAULT_SINK@ 50%",
            profiles.first().commands[RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, 5)]
        )
        assertTrue(profiles.first().commands.containsKey(RemoteControlConfig.DPAD_UP))
    }
}
