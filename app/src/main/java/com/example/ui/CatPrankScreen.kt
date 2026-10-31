package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.CatSoundType
import com.example.ui.theme.CatAmberSecondary
import com.example.ui.theme.CatDangerRed
import com.example.ui.theme.CatOrangePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatPrankScreen(viewModel: CatPrankViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept back button playfully
    BackHandler {
        if (uiState.appMode != AppMode.MAIN_CAT) {
            // If in Trap or Timer mode, go back to main screen
            viewModel.setAppMode(AppMode.MAIN_CAT)
            viewModel.resetTrap()
            viewModel.cancelTimerPrank()
        } else {
            val shouldExit = viewModel.recordExitAttempt()
            if (shouldExit) {
                (context as? Activity)?.finish()
            } else {
                showExitDialog = true
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "😸 میووووو!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.testTag("exit_dialog_title")
                )
            },
            text = {
                Text(
                    text = "گربه شیطون اجازه خروج نمی‌ده! شوخی کردیم 😹 اگر می‌خواهید خارج شوید، دوباره دکمه خروج را بزنید.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CatDangerRed),
                    modifier = Modifier.testTag("confirm_exit_button")
                ) {
                    Text("خروج از برنامه")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false },
                    modifier = Modifier.testTag("stay_in_app_button")
                ) {
                    Text("ادامه شوخی با گربه")
                }
            }
        )
    }

    // Switch between modes
    when (uiState.appMode) {
        AppMode.TAP_TRAP -> {
            CatTrapScreen(
                uiState = uiState,
                onTap = { viewModel.triggerTrapTouched() },
                onReset = { viewModel.resetTrap() },
                onExitTrap = { viewModel.setAppMode(AppMode.MAIN_CAT) }
            )
        }
        AppMode.PRANK_TIMER -> {
            CatTimerPrankScreen(
                uiState = uiState,
                onStartTimer = { sec -> viewModel.startTimerPrank(sec) },
                onCancelTimer = { viewModel.cancelTimerPrank() },
                onBack = { viewModel.setAppMode(AppMode.MAIN_CAT) }
            )
        }
        AppMode.MAIN_CAT, AppMode.SOUNDBOARD -> {
            MainPrankScreen(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun MainPrankScreen(
    uiState: CatUiState,
    viewModel: CatPrankViewModel
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_prank_scaffold"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8E7),
                            Color(0xFFFFEDD5),
                            Color(0xFFFED7AA)
                        )
                    )
                )
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // App Header
            item {
                HeaderBar(
                    meowCount = uiState.meowCount,
                    isLoopActive = uiState.isLoopActive,
                    onToggleLoop = { viewModel.toggleLoop() }
                )
            }

            // Big Interactive Cat
            item {
                Spacer(modifier = Modifier.height(16.dp))
                InteractiveCatCard(
                    uiState = uiState,
                    onCatTap = { viewModel.triggerCatTap() }
                )
            }

            // Prank Tools & Quick Actions
            item {
                Spacer(modifier = Modifier.height(20.dp))
                QuickPrankActions(
                    uiState = uiState,
                    onMaxVolume = { viewModel.setMaxVolume() },
                    onSetVolume = { viewModel.setVolume(it) },
                    onEnterTrapMode = { viewModel.setAppMode(AppMode.TAP_TRAP) },
                    onEnterTimerMode = { viewModel.setAppMode(AppMode.PRANK_TIMER) },
                    onToggleLoop = { viewModel.toggleLoop() }
                )
            }

            // Soundboard Title
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎛️ جعبه صداهای شوخی گربه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF431407)
                    )
                    Text(
                        text = "۸ افکت صوتی",
                        fontSize = 13.sp,
                        color = Color(0xFF9A3412),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Soundboard Grid
            item {
                SoundboardGrid(
                    currentPlayingType = if (uiState.isPlayingSound) uiState.currentSoundType else null,
                    onSoundClick = { soundType -> viewModel.playSound(soundType) }
                )
            }
        }
    }
}

@Composable
fun HeaderBar(
    meowCount: Int,
    isLoopActive: Boolean,
    onToggleLoop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "😹 برنامه شوخی گربه",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color(0xFF7C2D12)
            )
            Text(
                text = "صداهای بلند و خنده‌دار برای غافلگیری دوستان",
                fontSize = 12.sp,
                color = Color(0xFF9A3412)
            )
        }

        // Meow Counter badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CatOrangePrimary.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CatOrangePrimary.copy(alpha = 0.4f)),
            modifier = Modifier.testTag("meow_counter_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🐾 $meowCount میو",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC2410C)
                )
            }
        }
    }
}

@Composable
fun InteractiveCatCard(
    uiState: CatUiState,
    onCatTap: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }
    val bounceScale by animateFloatAsState(
        targetValue = if (uiState.isPlayingSound || uiState.isCatShocked) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "cat_bounce"
    )

    // Trigger shake animation when shocked
    LaunchedEffect(uiState.isCatShocked) {
        if (uiState.isCatShocked) {
            for (i in 0..4) {
                shakeOffset.animateTo(12f, tween(30))
                shakeOffset.animateTo(-12f, tween(30))
            }
            shakeOffset.animateTo(0f, tween(30))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(bounceScale)
                .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = CatOrangePrimary.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onCatTap()
                }
                .testTag("interactive_cat_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cat Photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageRes = if (uiState.isCatShocked || (uiState.isPlayingSound && uiState.currentSoundType == CatSoundType.SCREAM_CAT)) {
                        R.drawable.img_cat_surprised
                    } else {
                        R.drawable.img_cat_main
                    }

                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "عکس گربه شوخی",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Floating Speech Bubble
                    if (uiState.speechBubbleText != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CatOrangePrimary,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .testTag("cat_speech_bubble")
                        ) {
                            Text(
                                text = uiState.speechBubbleText ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Sound Ripple Effect indicator
                    if (uiState.isPlayingSound) {
                        Surface(
                            shape = CircleShape,
                            color = CatOrangePrimary.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.Center)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tap me indicator button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (uiState.isPlayingSound) CatOrangePrimary else Color(0xFFFFEDD5),
                    modifier = Modifier.testTag("tap_cat_prompt")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isPlayingSound) "📢 در حال پخش میومیو..." else "👆 گربه رو لمس کن تا میو بگه!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (uiState.isPlayingSound) Color.White else Color(0xFFC2410C)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPrankActions(
    uiState: CatUiState,
    onMaxVolume: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onEnterTrapMode: () -> Unit,
    onEnterTimerMode: () -> Unit,
    onToggleLoop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_actions_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "⚡ تنظیمات سریع شوخی",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF431407)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "صدا",
                    tint = CatOrangePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = uiState.currentVolume.toFloat(),
                    onValueChange = { onSetVolume(it.roundToInt()) },
                    valueRange = 0f..uiState.maxVolume.toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("volume_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CatOrangePrimary,
                        activeTrackColor = CatOrangePrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onMaxVolume,
                    colors = ButtonDefaults.buttonColors(containerColor = CatOrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("max_volume_button")
                ) {
                    Text("حداکثر صدا", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two main prank buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cat Trap Mode (فقط عکس گربه برای شوخی)
                Button(
                    onClick = onEnterTrapMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trap_mode_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🪤 تله لمسی گربه", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("صفحه فقط عکس گربه", fontSize = 11.sp, color = Color(0xFFFFEDD5))
                    }
                }

                // Timer Prank Mode
                Button(
                    onClick = onEnterTimerMode,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("timer_mode_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C2D12)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏱️ تایمر غافلگیری", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("شلیک میو با تأخیر", fontSize = 11.sp, color = Color(0xFFFFEDD5))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Continuous loop toggle button
            OutlinedButton(
                onClick = onToggleLoop,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("loop_toggle_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (uiState.isLoopActive) CatDangerRed.copy(alpha = 0.1f) else Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (uiState.isLoopActive) CatDangerRed else CatOrangePrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isLoopActive) Icons.Default.Stop else Icons.Default.Refresh,
                        contentDescription = "Loop",
                        tint = if (uiState.isLoopActive) CatDangerRed else CatOrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isLoopActive) "⏹️ توقف میومیو مداوم" else "🔁 فعال‌سازی میومیو مداوم (هر ۲ ثانیه)",
                        color = if (uiState.isLoopActive) CatDangerRed else CatOrangePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundboardGrid(
    currentPlayingType: CatSoundType?,
    onSoundClick: (CatSoundType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundboard_grid"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val sounds = CatSoundType.values().toList()
        val chunked = sounds.chunked(2)

        for (row in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (sound in row) {
                    val isPlaying = currentPlayingType == sound
                    SoundboardCard(
                        sound = sound,
                        isPlaying = isPlaying,
                        onClick = { onSoundClick(sound) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SoundboardCard(
    sound: CatSoundType,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "sound_card_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick)
            .testTag("sound_card_${sound.name}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) CatOrangePrimary else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 8.dp else 3.dp),
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFC2410C)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sound.emoji,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sound.titleFa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isPlaying) Color.White else Color(0xFF292524)
                )
                Text(
                    text = sound.bubbleText,
                    fontSize = 11.sp,
                    color = if (isPlaying) Color.White.copy(alpha = 0.85f) else Color(0xFF78716C)
                )
            }
        }
    }
}

// 🪤 Pure Cat Trap Mode - Full screen cat photo that fulfills user's "فقط عکس یک گربه باشه"
@Composable
fun CatTrapScreen(
    uiState: CatUiState,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onExitTrap: () -> Unit
) {
    val context = LocalContext.current
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.isTrapTriggered) {
        if (uiState.isTrapTriggered) {
            for (i in 0..5) {
                shakeOffset.animateTo(18f, tween(25))
                shakeOffset.animateTo(-18f, tween(25))
            }
            shakeOffset.animateTo(0f, tween(25))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onTap()
            }
            .testTag("cat_trap_fullscreen"),
        contentAlignment = Alignment.Center
    ) {
        // Cat Photo: normal cute cat when armed, shocked meme cat when triggered!
        val imageRes = if (uiState.isTrapTriggered) R.drawable.img_cat_surprised else R.drawable.img_cat_main

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "گربه تله شوخی",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // If triggered: Huge comic text and funny message
        if (uiState.isTrapTriggered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CatDangerRed,
                        shadowElevation = 12.dp
                    ) {
                        Text(
                            text = "🙀 میااااااااوووووووو!",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            text = "😹 شوخی بود! دست زدی گربه جیغ کشید!",
                            color = Color(0xFF7C2D12),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onReset,
                            colors = ButtonDefaults.buttonColors(containerColor = CatOrangePrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("آماده‌سازی مجدد تله", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onExitTrap,
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("بازگشت", color = Color(0xFF431407), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Subtle innocent prompt at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "🐾 گربه خوابیده، لمسش کن!",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Discreet exit button at top corner for the prankster
            IconButton(
                onClick = onExitTrap,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
                    .testTag("exit_trap_corner_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "خروج از تله",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

// ⏱️ Timer Prank Screen (Countdown stealth bomb)
@Composable
fun CatTimerPrankScreen(
    uiState: CatUiState,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timer_prank_scaffold"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A1B0E),
                            Color(0xFF431407),
                            Color(0xFF1E0C06)
                        )
                    )
                )
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بازگشت",
                        tint = Color.White
                    )
                }
                Text(
                    text = "💣 تایمر شوخی گربه",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Timer display or Cat waiting
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isTimerRunning) {
                    Text(
                        text = "گوشی رو بذار روی میز و منتظر بمون! 🤫",
                        color = Color(0xFFFDBA74),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        shape = CircleShape,
                        color = CatDangerRed,
                        modifier = Modifier
                            .size(160.dp)
                            .shadow(24.dp, CircleShape, spotColor = CatDangerRed)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${uiState.timerRemainingSec}",
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "بعد از پایان شمارش، صدای گوشی تا آخر زیاد میشه و گربه جیغ می‌کشه!",
                        color = Color(0xFFFFEDD5),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else if (uiState.isTimerTriggered) {
                    Text(
                        text = "🙀 بوم! گربه جیغ کشید!",
                        color = Color(0xFFEF4444),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_cat_surprised),
                        contentDescription = "گربه جیغ‌زن",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_cat_main),
                        contentDescription = "گربه",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "مدت زمان تایمر را انتخاب کنید:",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isTimerRunning) {
                    Button(
                        onClick = onCancelTimer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF57534E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("لغو تایمر شوخی", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(5 to "۵ ثانیه", 10 to "۱۰ ثانیه", 20 to "۲۰ ثانیه").forEach { (sec, label) ->
                            Button(
                                onClick = { onStartTimer(sec) },
                                colors = ButtonDefaults.buttonColors(containerColor = CatOrangePrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
