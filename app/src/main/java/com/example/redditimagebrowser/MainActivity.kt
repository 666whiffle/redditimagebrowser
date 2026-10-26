package com.example.redditimagebrowser

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.ExperimentalComposeUiApi
private val Backdrop = Color(0xFF101014)
private val Panel = Color(0xFF202026)
private val Accent = Color(0xFFFF6638)

private data class GalleryPhoto(val id: Int, val title: String, val imageUrl: String)

// DEMO ONLY. No Reddit requests are made in v0.1.
private fun demoPhotos(subreddit: String): List<GalleryPhoto> = (1..36).map { number ->
    GalleryPhoto(number, "Sample photo $number · r/$subreddit", "https://picsum.photos/seed/gallery-demo-$number/1000/1400")
}

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); RedditAuth.handleRedirect(this, intent) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RedditAuth.handleRedirect(this, intent)
        val preferences = getSharedPreferences("browser_settings", Context.MODE_PRIVATE)
        setContent {
            var subreddit by remember { mutableStateOf(preferences.getString("last_subreddit", "wallpapers") ?: "wallpapers") }
            var draft by remember { mutableStateOf(subreddit) }
            var selected by remember { mutableIntStateOf(-1) }
            var columns by remember { mutableIntStateOf(3) }
            var loginStatus by remember { mutableStateOf(RedditAuth.status(this)) }
            var showLogin by remember { mutableStateOf(false) }
            val photos = remember(subreddit) { demoPhotos(subreddit) }
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, surface = Backdrop, background = Backdrop)) {
                if (selected >= 0) {
                    FullScreenGallery(photos, selected, onClose = { selected = -1 })
                } else {
                    Column(Modifier.fillMaxSize().background(Backdrop)) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("REDDIT IMAGE BROWSER", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showLogin = true }) { Text(if (loginStatus.startsWith("Connected")) "Account" else "Login") }
                        }
                        if (showLogin) { AlertDialog(onDismissRequest = { showLogin = false }, title = { Text("Reddit account") }, text = { Column { Text(loginStatus); Spacer(Modifier.height(8.dp)); Text("Login requires an approved Reddit OAuth client ID. Set it in app/build.gradle.kts before building.") } }, confirmButton = { TextButton(onClick = { RedditAuth.start(this@MainActivity) { loginStatus = it }; showLogin = false }) { Text("Sign in") } }, dismissButton = { TextButton(onClick = { RedditAuth.logout(this@MainActivity); loginStatus = RedditAuth.status(this@MainActivity); showLogin = false }) { Text("Log out") } }) }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = draft, onValueChange = { draft = it },
                                label = { Text("Subreddit") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                val clean = draft.trim().removePrefix("r/").trim('/').lowercase()
                                if (clean.matches(Regex("[a-z0-9_]{2,21}"))) {
                                    subreddit = clean
                                    draft = clean
                                    preferences.edit().putString("last_subreddit", clean).apply()
                                }
                            }) { Text("Go") }
                        }
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("r/$subreddit", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("Columns", color = Color.LightGray)
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { columns = if (columns == 4) 2 else columns + 1 }) { Text("$columns") }
                        }
                        Text("DEMO IMAGES · Reddit API and login not yet connected", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        LazyVerticalGrid(columns = GridCells.Fixed(columns), contentPadding = PaddingValues(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            itemsIndexed(photos) { index, photo ->
                                AsyncImage(
                                    model = photo.imageUrl, contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).background(Panel, RoundedCornerShape(3.dp)).clickable { selected = index }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FullScreenGallery(photos: List<GalleryPhoto>, initialIndex: Int, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val pager = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    var showControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var mouseDownX by remember { mutableFloatStateOf(0f) }
    var mouseDownY by remember { mutableFloatStateOf(0f) }
    var mouseTracking by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Mouse drag support for the Android Studio emulator. Touch gestures are
        // deliberately left to HorizontalPager, including simulated finger drags.
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().pointerInteropFilter { event ->
                if (event.getToolType(0) != MotionEvent.TOOL_TYPE_MOUSE) {
                    false
                } else {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            mouseDownX = event.x
                            mouseDownY = event.y
                            mouseTracking = true
                            true
                        }
                        MotionEvent.ACTION_MOVE -> mouseTracking
                        MotionEvent.ACTION_UP -> {
                            if (mouseTracking) {
                                val dx = event.x - mouseDownX
                                val dy = event.y - mouseDownY
                                if (kotlin.math.abs(dx) > 65f &&
                                    kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.2f) {
                                    val next = (pager.currentPage + if (dx < 0) 1 else -1)
                                        .coerceIn(0, photos.lastIndex)
                                    scope.launch { pager.animateScrollToPage(next) }
                                } else if (kotlin.math.abs(dx) < 12f && kotlin.math.abs(dy) < 12f) {
                                    showControls = !showControls
                                }
                            }
                            mouseTracking = false
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            mouseTracking = false
                            true
                        }
                        else -> false
                    }
                }
            }
        ) { index ->
            var scale by remember(index) { mutableFloatStateOf(1f) }
            var offsetX by remember(index) { mutableFloatStateOf(0f) }
            var offsetY by remember(index) { mutableFloatStateOf(0f) }
            Box(Modifier.fillMaxSize()) {
                // Pager owns normal touch swipes. Only apply transform gestures for two-finger zoom.
                AsyncImage(
                    model = photos[index].imageUrl,
                    contentDescription = photos[index].title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                        .pointerInput(index) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        // Multi-touch zoom; single-finger drags are left to HorizontalPager.
                                        val a = event.changes[0]; val b = event.changes[1]
                                        val previous = (a.previousPosition - b.previousPosition).getDistance()
                                        val current = (a.position - b.position).getDistance()
                                        if (previous > 0f) scale = (scale * (current / previous)).coerceIn(1f, 5f)
                                        if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                                        a.consume(); b.consume()
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                )
            }
        }
        if (showControls) {
            Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color(0xBB000000)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClose) { Text("‹ Back") }
                Text("${pager.currentPage + 1} / ${photos.size}", color = Color.White, modifier = Modifier.weight(1f))
                TextButton(enabled = pager.currentPage > 0, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) { Text("◀") }
                TextButton(enabled = pager.currentPage < photos.lastIndex, onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("▶") }
            }
            Text(photos[pager.currentPage].title, Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xBB000000)).padding(16.dp), color = Color.White)
        }
    }
}
