package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val TermColor = Color(0xFFFFB000)
val TermBg = Color(0xFF0A0A0A)
val TermGlow = Shadow(color = TermColor.copy(alpha = 0.5f), blurRadius = 10f)

@Composable
fun TerminalScreen(viewModel: TerminalViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_SYSTEM, 100) }

    // Auto-scroll logic
    LaunchedEffect(state.history.size, state.currentAiText) {
        if (state.history.isNotEmpty() || state.currentAiText.isNotEmpty()) {
            listState.animateScrollToItem(
                if (state.currentAiText.isNotEmpty()) state.history.size else state.history.size - 1
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            HeaderHud(status = state.status)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.history) { message ->
                    MessageItem(message = message)
                }
                
                if (state.isGenerating && state.currentAiText.isNotEmpty()) {
                    item {
                        GeneratingAiMessage(text = state.currentAiText)
                        LaunchedEffect(state.currentAiText.length) {
                            if (state.currentAiText.length % 3 == 0) {
                                toneGen.startTone(ToneGenerator.TONE_DTMF_0, 20)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InputArea(
                isGenerating = state.isGenerating,
                onSubmit = {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    viewModel.processCommand(it)
                    focusManager.clearFocus()
                },
                onType = {
                    toneGen.startTone(ToneGenerator.TONE_DTMF_0, 20)
                }
            )
            
            ControlsArea(
                onCopy = {
                    val textToCopy = state.history.lastOrNull { it.role == Role.AI }?.text ?: ""
                    if (textToCopy.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "DATOS COPIADOS AL PORTAPAPELES", Toast.LENGTH_SHORT).show()
                    }
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 50)
                },
                onSave = {
                    Toast.makeText(context, "FUNCIÓN NO DISPONIBLE EN MODO TERMINAL MÓVIL", Toast.LENGTH_SHORT).show()
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                }
            )
        }
        
        // CRT Effects OVERLAY
        CrtEffects()
    }
}

@Composable
fun CrtEffects() {
    val infiniteTransition = rememberInfiniteTransition(label = "flicker")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(TermColor.copy(alpha = 0.02f))
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val ySteps = size.height / 6f
        for (i in 0..ySteps.toInt()) {
            val y = i * 6f
            drawLine(
                color = Color.Black.copy(alpha = 0.25f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f
            )
        }
        
        // Vignette effect
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.width.coerceAtLeast(size.height) / 1.2f
            ),
            size = size
        )
    }
}

@Composable
fun HeaderHud(status: String) {
    var currentDate by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        while (true) {
            currentDate = format.format(Date()) + " UTC"
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.dp, color = Color.Transparent)
            .background(Color.Transparent),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "A.V.T. SYS v3.1.4",
                color = TermColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                style = TextStyle(shadow = TermGlow)
            )
            Text(
                text = currentDate.ifEmpty { "CARGANDO..." },
                color = TermColor.copy(alpha = 0.75f),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "MEMORIA: OK",
                color = TermColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            
            val isBlinking = status == "PROCESANDO..."
            val infiniteTransition = rememberInfiniteTransition(label = "status_blink")
            val blinkAlpha by infiniteTransition.animateFloat(
                initialValue = if (isBlinking) 0f else 1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = FastOutLinearInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "status_blink_alpha"
            )
            
            Text(
                text = status,
                color = TermColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.alpha(if (isBlinking) blinkAlpha else 1f)
            )
        }
    }
    Spacer(modifier = Modifier
        .height(2.dp)
        .fillMaxWidth()
        .background(TermColor))
}

@Composable
fun MessageItem(message: Message) {
    val rolePrefix = when (message.role) {
        Role.USER -> "> OPERARIO: "
        Role.AI -> "[UNIDAD CENTRAL]:\n"
        Role.SYSTEM -> ""
        Role.ERROR -> "ERROR: "
    }
    
    val textColor = when (message.role) {
        Role.USER -> TermColor.copy(alpha = 0.8f)
        Role.AI -> Color.White
        Role.SYSTEM -> TermColor
        Role.ERROR -> Color.Red
    }
    
    val glow = if (message.role == Role.AI) {
        Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 5f)
    } else if (message.role == Role.SYSTEM) {
        TermGlow
    } else {
        null
    }

    Text(
        text = rolePrefix + message.text,
        color = textColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 20.sp,
        style = TextStyle(shadow = glow),
        lineHeight = 24.sp
    )
}

@Composable
fun GeneratingAiMessage(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Text(
        text = "[UNIDAD CENTRAL]:\n$text",
        color = Color.White,
        fontFamily = FontFamily.Monospace,
        fontSize = 20.sp,
        style = TextStyle(shadow = Shadow(color = Color.White.copy(alpha = 0.5f), blurRadius = 5f)),
        lineHeight = 24.sp
    )
    Box(
        modifier = Modifier
            .width(10.dp)
            .height(20.dp)
            .alpha(cursorAlpha)
            .background(TermColor)
    )
}

@Composable
fun InputArea(
    isGenerating: Boolean,
    onSubmit: (String) -> Unit,
    onType: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, TermColor)
            .background(TermColor.copy(alpha = 0.05f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ">",
            color = TermColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        BasicTextField(
            value = text,
            onValueChange = { 
                if (it.length > text.length) onType()
                text = it 
            },
            enabled = !isGenerating,
            textStyle = TextStyle(
                color = TermColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                shadow = TermGlow
            ),
            cursorBrush = SolidColor(TermColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (text.isNotBlank()) {
                        onSubmit(text)
                        text = ""
                    }
                }
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = "Escriba su comando aquí...",
                        color = TermColor.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp
                    )
                }
                innerTextField()
            }
        )
        
        Text(
            text = "[ ENVIAR ]",
            color = TermColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(enabled = !isGenerating && text.isNotBlank()) {
                    onSubmit(text)
                    text = ""
                }
                .padding(start = 8.dp)
                .alpha(if (!isGenerating && text.isNotBlank()) 1f else 0.5f)
        )
    }
}

@Composable
fun ControlsArea(
    onCopy: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        RetroButton(text = "[ COPIAR RESPUESTA ]", onClick = onCopy)
        Spacer(modifier = Modifier.width(16.dp))
        RetroButton(text = "[ GUARDAR REGISTRO ]", onClick = onSave)
    }
}

@Composable
fun RetroButton(text: String, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Text(
        text = text,
        color = if (isPressed) TermBg else TermColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp,
        modifier = Modifier
            .border(1.dp, TermColor)
            .background(if (isPressed) TermColor else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) {
                isPressed = true
                onClick()
                // Reset after click
                // This is a simple visual effect without full indication handling
            }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}
