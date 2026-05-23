package org.acoustixaudio.opiqo.remotecontroloverssh.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteCommand
import org.acoustixaudio.opiqo.remotecontroloverssh.data.RemoteControlConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "${uiState.remoteProfile?.name ?: "Remote"} (${uiState.connectionStatus})",
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Slider 1
            RemoteSlider(
                label = "Volume Slider (0 to 10)",
                value = uiState.slider1Value,
                onValueChange = { viewModel.onSlider1Change(it) },
                commandPreview = sliderCommandPreview(
                    commands = uiState.commands,
                    sliderId = RemoteControlConfig.SLIDER_1,
                    value = uiState.slider1Value
                )
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // D-Pad
            Text("Five-Way Keys", style = MaterialTheme.typography.titleSmall)
            FiveWayDPad(
                onUp = { viewModel.onButtonClick(RemoteControlConfig.DPAD_UP) },
                onDown = { viewModel.onButtonClick(RemoteControlConfig.DPAD_DOWN) },
                onLeft = { viewModel.onButtonClick(RemoteControlConfig.DPAD_LEFT) },
                onRight = { viewModel.onButtonClick(RemoteControlConfig.DPAD_RIGHT) },
                onSelect = { viewModel.onButtonClick(RemoteControlConfig.DPAD_SELECT) }
            )

            // Back/Home Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DashboardButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Back",
                    onClick = { viewModel.onButtonClick(RemoteControlConfig.BTN_BACK) }
                )
                DashboardButton(
                    icon = Icons.Default.Home,
                    label = "Home",
                    onClick = { viewModel.onButtonClick(RemoteControlConfig.BTN_HOME) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Slider 2
            RemoteSlider(
                label = "Brightness",
                value = uiState.slider2Value,
                onValueChange = { viewModel.onSlider2Change(it) },
                commandPreview = sliderCommandPreview(
                    commands = uiState.commands,
                    sliderId = RemoteControlConfig.SLIDER_2,
                    value = uiState.slider2Value
                )
            )
        }
    }
}

private fun sliderCommandPreview(
    commands: Map<String, RemoteCommand>,
    sliderId: String,
    value: Int
): String = RemoteControlConfig.resolveSliderCommand(commands, sliderId, value).orEmpty()

@Composable
fun RemoteSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    commandPreview: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text("Value: $value", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..10f,
            steps = 9
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (0..10).forEach { 
                Text(it.toString(), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (commandPreview.isNotEmpty()) {
            Text(
                "command: $commandPreview",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun FiveWayDPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(280.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Up
        DPadSection(
            icon = Icons.Default.KeyboardArrowUp,
            label = "Up",
            modifier = Modifier.align(Alignment.TopCenter),
            onClick = onUp
        )
        // Down
        DPadSection(
            icon = Icons.Default.KeyboardArrowDown,
            label = "Down",
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick = onDown
        )
        // Left
        DPadSection(
            icon = Icons.Default.KeyboardArrowLeft,
            label = "Left",
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = onLeft
        )
        // Right
        DPadSection(
            icon = Icons.Default.KeyboardArrowRight,
            label = "Right",
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = onRight
        )
        // Select
        Surface(
            onClick = onSelect,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "Select",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun DPadSection(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(90.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DashboardButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Icon(
                icon, 
                contentDescription = label, 
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}
