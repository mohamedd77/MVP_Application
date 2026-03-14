package com.example.mvpapplication

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val viewModel: SpeechViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startRecording()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFE94560),
                    background = Color(0xFF0F0F1A),
                    surface = Color(0xFF1A1A2E),
                    onBackground = Color.White,
                    onSurface = Color.White,
                )
            ) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                SpeechScreen(
                    uiState = uiState,
                    onToggleRecording = {
                        if (!uiState.isRecording) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.stopRecording()
                        }
                    },
                    onSwitchLanguage = { viewModel.switchLanguage(it) },
                    onClear = viewModel::clearTranscript
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════
// الشاشة الرئيسية
// ════════════════════════════════════════════════════════
@Composable
fun SpeechScreen(
    uiState: SpeechUiState,
    onToggleRecording: () -> Unit,
    onSwitchLanguage: (AppLanguage) -> Unit,
    onClear: () -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.fullTranscript) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ── Title ────────────────────────────────────
            Text(
                text = "🎙️ MVP App",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Status ───────────────────────────────────
            StatusChip(uiState)

            Spacer(modifier = Modifier.height(20.dp))

            // ── Language Switch ──────────────────────────
            LanguageToggle(
                currentLanguage = uiState.currentLanguage,
                isEnabled = uiState.status != AppStatus.LOADING_MODEL,
                onSwitch = onSwitchLanguage
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Partial Card ─────────────────────────────
            PartialResultCard(partialText = uiState.partialText)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Full Transcript ──────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📝 الـ Transcript", fontSize = 13.sp, color = Color.Gray)
                        if (uiState.fullTranscript.isNotEmpty()) {
                            IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = Color(0xFFE94560),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.fullTranscript.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "ابدأ التسجيل وهيظهر هنا...",
                                color = Color.Gray,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = uiState.fullTranscript,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── زرار التسجيل ─────────────────────────────
            RecordButton(
                isRecording = uiState.isRecording,
                isEnabled = uiState.status != AppStatus.LOADING_MODEL,
                onClick = onToggleRecording
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ════════════════════════════════════════════════════════
// Language Toggle
// ════════════════════════════════════════════════════════
@Composable
fun LanguageToggle(
    currentLanguage: AppLanguage,
    isEnabled: Boolean,
    onSwitch: (AppLanguage) -> Unit
) {
    Card(
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            // زرار العربي
            Button(
                onClick = { onSwitch(AppLanguage.ARABIC) },
                enabled = isEnabled,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLanguage == AppLanguage.ARABIC)
                        Color(0xFFE94560) else Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.Gray
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("🇸🇦 عربي", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            // زرار الإنجليزي
            Button(
                onClick = { onSwitch(AppLanguage.ENGLISH) },
                enabled = isEnabled,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLanguage == AppLanguage.ENGLISH)
                        Color(0xFFE94560) else Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.Gray
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("🇺🇸 English", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ════════════════════════════════════════════════════════
// Status Chip
// ════════════════════════════════════════════════════════
@Composable
fun StatusChip(uiState: SpeechUiState) {
    val (text, color) = when (uiState.status) {
        AppStatus.LOADING_MODEL -> uiState.loadingMessage to Color.Gray
        AppStatus.READY         -> "✅ جاهز للتسجيل" to Color(0xFF4CAF50)
        AppStatus.RECORDING     -> "🔴 بيسمع..." to Color(0xFFE94560)
        AppStatus.STOPPED       -> "⏹️ متوقف" to Color(0xFFFF9800)
        AppStatus.ERROR         -> "❌ ${uiState.errorMessage}" to Color.Red
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ════════════════════════════════════════════════════════
// Partial Card
// ════════════════════════════════════════════════════════
@Composable
fun PartialResultCard(partialText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Color(0xFFE94560).copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("⏳ بيتكتب دلوقتي...", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (partialText.isNotEmpty()) partialText else "...",
                fontSize = 18.sp,
                color = if (partialText.isNotEmpty()) Color(0xFFE94560)
                else Color.Gray.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════
// Record Button
// ════════════════════════════════════════════════════════
@Composable
fun RecordButton(isRecording: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = { if (isEnabled) onClick() },
            modifier = Modifier.size(72.dp).scale(scale),
            shape = CircleShape,
            containerColor = if (isRecording) Color(0xFFE94560) else Color(0xFF1A1A2E),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                !isEnabled  -> "جاري التحميل..."
                isRecording -> "وقف التسجيل"
                else        -> "ابدأ التسجيل"
            },
            fontSize = 13.sp,
            color = if (isEnabled) Color.White else Color.Gray
        )
    }
}
