package org.acoustixaudio.opiqo.remotecontroloverssh.data

object RemoteControlConfig {
    const val SLIDER_1 = "SLIDER_1"
    const val SLIDER_2 = "SLIDER_2"
    const val DPAD_UP = "DPAD_UP"
    const val DPAD_DOWN = "DPAD_DOWN"
    const val DPAD_LEFT = "DPAD_LEFT"
    const val DPAD_RIGHT = "DPAD_RIGHT"
    const val DPAD_SELECT = "DPAD_SELECT"
    const val BTN_BACK = "BTN_BACK"
    const val BTN_HOME = "BTN_HOME"

    val sliderSteps: IntRange = 0..10

    fun sliderStepIdentifier(sliderId: String, step: Int): String = "${sliderId}_STEP_$step"

    fun resolveSliderCommand(
        commands: Map<String, RemoteCommand>,
        sliderId: String,
        step: Int
    ): String? {
        val perStepCommand = commands[sliderStepIdentifier(sliderId, step)]
            ?.commandString
            ?.takeIf { it.isNotBlank() }
        if (perStepCommand != null) {
            return perStepCommand
        }

        return commands[sliderId]
            ?.commandString
            ?.takeIf { it.isNotBlank() }
            ?.replace("%val%", step.toString())
    }
}
