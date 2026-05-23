package org.acoustixaudio.opiqo.remotecontroloverssh.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteControlConfigTest {
    @Test
    fun resolveSliderCommand_prefersPerStepCommand() {
        val commands = mapOf(
            RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, 3) to
                RemoteCommand(remoteProfileId = 1, buttonIdentifier = RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, 3), commandString = "set-volume-3"),
            RemoteControlConfig.SLIDER_1 to
                RemoteCommand(remoteProfileId = 1, buttonIdentifier = RemoteControlConfig.SLIDER_1, commandString = "legacy-%val%")
        )

        val resolved = RemoteControlConfig.resolveSliderCommand(commands, RemoteControlConfig.SLIDER_1, 3)

        assertEquals("set-volume-3", resolved)
    }

    @Test
    fun resolveSliderCommand_fallsBackToLegacyTemplate() {
        val commands = mapOf(
            RemoteControlConfig.SLIDER_2 to
                RemoteCommand(remoteProfileId = 1, buttonIdentifier = RemoteControlConfig.SLIDER_2, commandString = "brightness-%val%")
        )

        val resolved = RemoteControlConfig.resolveSliderCommand(commands, RemoteControlConfig.SLIDER_2, 7)

        assertEquals("brightness-7", resolved)
    }

    @Test
    fun resolveSliderCommand_ignoresBlankCommands() {
        val commands = mapOf(
            RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, 1) to
                RemoteCommand(remoteProfileId = 1, buttonIdentifier = RemoteControlConfig.sliderStepIdentifier(RemoteControlConfig.SLIDER_1, 1), commandString = "  "),
            RemoteControlConfig.SLIDER_1 to
                RemoteCommand(remoteProfileId = 1, buttonIdentifier = RemoteControlConfig.SLIDER_1, commandString = "")
        )

        val resolved = RemoteControlConfig.resolveSliderCommand(commands, RemoteControlConfig.SLIDER_1, 1)

        assertNull(resolved)
    }
}
