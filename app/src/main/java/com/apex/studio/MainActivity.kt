package com.apex.studio

import android.content.ComponentCallbacks2
import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E5FF),
                    background = Color(0xFF09090D),
                    surface = Color(0xFF13131B)
                )
            ) {
                ApexLowRamStudioScreen()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            LowRamBitmapLoader.trimMemory()
        }
    }
}

@Composable
fun ApexLowRamStudioScreen() {
    val context = LocalContext.current

    var editorMode by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableIntStateOf(0) }

    var gradeState by remember { mutableStateOf(StudioGradeState()) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoSpeed by remember { mutableFloatStateOf(1.0f) }

    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val strokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val colorMatrixValues = remember { FloatArray(20) }

    val layers = remember {
        mutableStateListOf(
            OverlayLayer(1L, "NEW UPDATE!", isBadge = true, colorHex = 0xFFFFFFFF, strokeHex = 0xFFEF4444, xNorm = 0.28f, yNorm = 0.22f, scale = 0.9f),
            OverlayLayer(2L, "INSANE FPS", isBadge = false, colorHex = 0xFFFACC15, strokeHex = 0xFF000000, xNorm = 0.5f, yNorm = 0.55f, scale = 1.25f),
            OverlayLayer(3L, "2GB RAM OPTIMIZED", isBadge = false, colorHex = 0xFF00E5FF, strokeHex = 0xFF000000, xNorm = 0.5f, yNorm = 0.78f, scale = 0.85f)
        )
    }
    var selectedLayerId by remember { mutableLongStateOf(2L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            if (editorMode == 0) {
                photoBitmap = LowRamBitmapLoader.decodeOptimized(context, uri, 1280, 720)
            } else {
                videoUri = uri
                exoPlayer.setMediaItem(MediaItem.fromUri(uri))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF13131B))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("APEX NATIVE", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .background(Color(0xFF1E1E2C), RoundedCornerShape(6.dp))
                        .padding(2.dp)
                ) {
                    Text(
                        "Thumb/Photo",
                        color = if (editorMode == 0) Color.Black else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(if (editorMode == 0) Color(0xFF00E5FF) else Color.Transparent, RoundedCornerShape(4.dp))
                            .clickable { editorMode = 0; exoPlayer.pause() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        "Video Cut",
                        color = if (editorMode == 1) Color.Black else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(if (editorMode == 1) Color(0xFF00E5FF) else Color.Transparent, RoundedCornerShape(4.dp))
                            .clickable { editorMode = 1 }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { mediaPicker.launch(if (editorMode == 0) "image/*" else "video/*") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252536))
                ) {
                    Text(if (editorMode == 0) "+ Photo" else "+ Video", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        exportHighResThumbnailToGallery(context, gradeState, photoBitmap, layers)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Export HD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (editorMode == 1 && videoUri != null) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).also { textureView ->
                            exoPlayer.setVideoTextureView(textureView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(gradeState.aspectRatio)
                    .border(1.dp, Color(0xFF2A2A3A))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                layers.reversed().firstOrNull { lyr ->
                                    kotlin.math.abs(offset.x - lyr.xNorm * w) < 160f &&
                                            kotlin.math.abs(offset.y - lyr.yNorm * h) < 70f
                                }?.let { selectedLayerId = it.id }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val idx = layers.indexOfFirst { it.id == selectedLayerId }
                                if (idx != -1) {
                                    val cur = layers[idx]
                                    layers[idx] = cur.copy(
                                        xNorm = (cur.xNorm + dragAmount.x / size.width).coerceIn(0.05f, 0.95f),
                                        yNorm = (cur.yNorm + dragAmount.y / size.height).coerceIn(0.05f, 0.95f)
                                    )
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val nativeCanvas = drawContext.canvas.nativeCanvas

                ColorMatrixBuilder.buildLumetriMatrix(gradeState, colorMatrixValues)
                paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(colorMatrixValues))

                if (editorMode == 0 || videoUri == null) {
                    if (photoBitmap != null && !photoBitmap!!.isRecycled) {
                        val dst = RectF(0f, 0f, w, h)
                        nativeCanvas.drawBitmap(photoBitmap!!, null, dst, paint)
                    } else {
                        paint.color = android.graphics.Color.parseColor("#0D1021")
                        nativeCanvas.drawRect(0f, 0f, w, h, paint)
                    }
                }

                layers.forEach { lyr ->
                    val cx = lyr.xNorm * w
                    val cy = lyr.yNorm * h
                    val fontPx = 56f * lyr.scale

                    textPaint.textSize = fontPx
                    textPaint.color = lyr.colorHex.toInt()

                    strokePaint.textSize = fontPx
                    strokePaint.strokeWidth = if (gradeState.stickerBorder) 14f * lyr.scale else 4f
                    strokePaint.color = lyr.strokeHex.toInt()

                    if (lyr.isBadge) {
                        val textW = textPaint.measureText(lyr.text) + 44f
                        val boxH = fontPx + 24f
                        paint.colorFilter = null
                        paint.color = lyr.strokeHex.toInt()
                        nativeCanvas.drawRoundRect(
                            cx - textW / 2, cy - boxH / 2,
                            cx + textW / 2, cy + boxH / 2,
                            16f, 16f, paint
                        )
                        nativeCanvas.drawText(lyr.text, cx, cy + fontPx * 0.34f, textPaint)
                    } else {
                        nativeCanvas.drawText(lyr.text, cx, cy, strokePaint)
                        nativeCanvas.drawText(lyr.text, cx, cy, textPaint)
                    }
                }

                if (gradeState.cinemaBars > 0f) {
                    val barH = h * gradeState.cinemaBars
                    paint.colorFilter = null
                    paint.color = android.graphics.Color.BLACK
                    nativeCanvas.drawRect(0f, 0f, w, barH, paint)
                    nativeCanvas.drawRect(0f, h - barH, w, h, paint)
                }
            }
        }

        if (editorMode == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF161622))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text("Play / Pause", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed: ${videoSpeed}x", color = Color.White, fontSize = 11.sp)
                    Slider(
                        value = videoSpeed,
                        onValueChange = {
                            videoSpeed = it
                            exoPlayer.playbackParameters = PlaybackParameters(it)
                        },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.width(130.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .background(Color(0xFF13131B))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (activeTab) {
                0 -> {
                    Text("1-Tap Viral Templates & Aspect Ratio", color = Color.Gray, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallChip("16:9 YouTube") { gradeState = gradeState.copy(aspectRatio = 16f / 9f) }
                        SmallChip("9:16 Reels/Shorts") { gradeState = gradeState.copy(aspectRatio = 9f / 16f) }
                        SmallChip("1:1 Square") { gradeState = gradeState.copy(aspectRatio = 1f) }
                        SmallChip("Toggle Sticker Border") {
                            gradeState = gradeState.copy(stickerBorder = !gradeState.stickerBorder)
                        }
                    }
                }
                1 -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallChip("+ Yellow Impact Text") {
                            val id = System.currentTimeMillis()
                            layers.add(OverlayLayer(id, "VIRAL TEXT", false, 0xFFFACC15, 0xFF000000))
                            selectedLayerId = id
                        }
                        SmallChip("+ Red LIVE Badge") {
                            val id = System.currentTimeMillis()
                            layers.add(OverlayLayer(id, "LIVE ●", true, 0xFFFFFFFF, 0xFFEF4444))
                            selectedLayerId = id
                        }
                        SmallChip("Delete Selected") {
                            layers.removeAll { it.id == selectedLayerId }
                        }
                    }
                    val selIdx = layers.indexOfFirst { it.id == selectedLayerId }
                    if (selIdx != -1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Layer Scale", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                            Slider(
                                value = layers[selIdx].scale,
                                onValueChange = { layers[selIdx] = layers[selIdx].copy(scale = it) },
                                valueRange = 0.4f..2.2f
                            )
                        }
                    }
                }
                2 -> {
                    CompactSlider("Exposure", gradeState.exposure, -1f..1f) {
                        gradeState = gradeState.copy(exposure = it)
                    }
                    CompactSlider("Contrast", gradeState.contrast, 0.5f..1.8f) {
                        gradeState = gradeState.copy(contrast = it)
                    }
                    CompactSlider("Saturation", gradeState.saturation, 0f..2f) {
                        gradeState = gradeState.copy(saturation = it)
                    }
                }
                3 -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallChip("Normal") { gradeState = gradeState.copy(lutPreset = 0) }
                        SmallChip("Teal & Orange") { gradeState = gradeState.copy(lutPreset = 1) }
                        SmallChip("Cyberpunk") { gradeState = gradeState.copy(lutPreset = 2) }
                        SmallChip("Viral Pop") { gradeState = gradeState.copy(lutPreset = 3) }
                        SmallChip("Noir B&W") { gradeState = gradeState.copy(lutPreset = 4) }
                    }
                    CompactSlider("2.35:1 Cinema Bars", gradeState.cinemaBars, 0f..0.15f) {
                        gradeState = gradeState.copy(cinemaBars = it)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(0xFF0E0E14)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab("Templates", activeTab == 0) { activeTab = 0 }
            NavTab("Canva Text", activeTab == 1) { activeTab = 1 }
            NavTab("Lightroom", activeTab == 2) { activeTab = 2 }
            NavTab("Premiere LUT", activeTab == 3) { activeTab = 3 }
        }
    }
}

@Composable
fun SmallChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFF232334), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF34344A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CompactSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(34.dp)) {
        Text(label, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(105.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
    }
}

@Composable
fun NavTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = title,
        color = if (selected) Color(0xFF00E5FF) else Color.Gray,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

fun exportHighResThumbnailToGallery(
    context: android.content.Context,
    grade: StudioGradeState,
    sourcePhoto: Bitmap?,
    layers: List<OverlayLayer>
) {
    val outBitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(outBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val matrixVals = FloatArray(20)
    ColorMatrixBuilder.buildLumetriMatrix(grade, matrixVals)
    paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrixVals))

    if (sourcePhoto != null && !sourcePhoto.isRecycled) {
        canvas.drawBitmap(sourcePhoto, null, RectF(0f, 0f, 1280f, 720f), paint)
    } else {
        paint.color = android.graphics.Color.parseColor("#0D1021")
        canvas.drawRect(0f, 0f, 1280f, 720f, paint)
    }

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    layers.forEach { lyr ->
        val cx = lyr.xNorm * 1280f
        val cy = lyr.yNorm * 720f
        val fontPx = 68f * lyr.scale
        textPaint.textSize = fontPx
        textPaint.color = lyr.colorHex.toInt()
        strokePaint.textSize = fontPx
        strokePaint.strokeWidth = 16f * lyr.scale
        strokePaint.color = lyr.strokeHex.toInt()

        canvas.drawText(lyr.text, cx, cy, strokePaint)
        canvas.drawText(lyr.text, cx, cy, textPaint)
    }

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "Apex_Thumb_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ApexStudio")
        }
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri != null) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            outBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Toast.makeText(context, "Saved HD Thumbnail to Gallery!", Toast.LENGTH_SHORT).show()
    }
    outBitmap.recycle()
}
