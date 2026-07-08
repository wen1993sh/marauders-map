package com.marauders.map.ui

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.marauders.map.ble.BluetoothScanner
import com.marauders.map.data.FOCUS_COUNT
import com.marauders.map.data.MAX_RANGE_M
import com.marauders.map.data.focusMacs
import com.marauders.map.model.UiDevice
import com.marauders.map.ui.MapViewModel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun MaraudersMapScreen(
    viewModel: MapViewModel,
    hasBluetooth: Boolean,
    scanner: BluetoothScanner,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val selected by viewModel.selectedDevice.collectAsState()
    val sightings by viewModel.recentSightings.collectAsState()
    val focus = remember(devices) { focusMacs(devices, FOCUS_COUNT) }

    var scanning by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Parchment) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "活点地图",
                    style = MaterialTheme.typography.h4,
                    fontFamily = FontFamily.Cursive,
                    color = Ink
                )
                Text(
                    "“我庄严宣誓，我干的都不是好事。”",
                    style = MaterialTheme.typography.body2,
                    color = Ink.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Cursive
                )

                if (!hasBluetooth) {
                    Spacer(Modifier.height(12.dp))
                    Text("此设备不支持蓝牙低功耗（BLE）。", color = Ink)
                }

                Spacer(Modifier.height(8.dp))

                // 雷达
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RadarView(
                        devices = devices,
                        focus = focus,
                        onSelect = viewModel::select
                    )
                }

                // 控制条
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        if (scanning) {
                            scanner.stop()
                            scanning = false
                        } else {
                            scanning = true
                            onStartScan()
                        }
                    }) {
                        Text(if (scanning) "停止侦测" else "开始侦测")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "侦测到 ${devices.size} 个设备",
                        color = Ink,
                        fontFamily = FontFamily.Cursive
                    )
                }

                Spacer(Modifier.height(8.dp))
                Divider(color = Ink.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                // 最近 N 个设备的紧凑列表
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(devices.sortedBy { it.distance }.take(FOCUS_COUNT)) { device ->
                        DeviceRow(device = device, onClick = { viewModel.select(device.mac) })
                    }
                }
            }

            // 设备详情卡（底部弹层）
            if (selected != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { viewModel.select(null) }
                    )
                    DeviceDetailSheet(
                        device = selected!!,
                        sightings = sightings,
                        onDismiss = { viewModel.select(null) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarView(
    devices: List<UiDevice>,
    focus: Set<String>,
    onSelect: (String) -> Unit
) {
    val sweep by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 4000, easing = LinearEasing))
    )

    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val labelPaint = remember {
        Paint().apply {
            color = AndroidColor.parseColor("#3B2A1A")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(devices, focus, boxSize) {
                detectTapGestures { offset ->
                    val placed = placedPoints(devices, boxSize.toSize())
                    val hit = placed.minByOrNull { p ->
                        val dx = p.x - offset.x
                        val dy = p.y - offset.y
                        dx * dx + dy * dy
                    }
                    if (hit != null) {
                        val dx = hit.x - offset.x
                        val dy = hit.y - offset.y
                        // 命中阈值约 34dp（近似）
                        if (dx * dx + dy * dy <= 34f * 34f * 4f) onSelect(hit.device.mac)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().onSizeChanged { boxSize = it }) {
            val w = size.width
            val h = size.height
            val r = min(w, h) / 2f
            val cx = w / 2f
            val cy = h / 2f

            // 同心距离环
            for (i in 1..4) {
                drawCircle(
                    color = Ink.copy(alpha = 0.25f),
                    radius = r * i / 4f,
                    center = Offset(cx, cy),
                    style = Stroke(1.5f)
                )
            }
            // 十字准线
            drawLine(Ink.copy(alpha = 0.25f), Offset(cx - r, cy), Offset(cx + r, cy), 1.5f)
            drawLine(Ink.copy(alpha = 0.25f), Offset(cx, cy - r), Offset(cx, cy + r), 1.5f)

            // 旋转扫描扇形
            rotate(sweep, Offset(cx, cy)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(SweepColor.copy(alpha = 0.55f), Color.Transparent)
                    ),
                    startAngle = -45f,
                    sweepAngle = 45f,
                    useCenter = true,
                    topLeft = Offset(cx - r, cy - r),
                    size = Size(r * 2, r * 2)
                )
            }

            // 设备光点：聚焦的更大更亮且带标签；无名设备整体更淡
            placedPoints(devices, size).forEach { p ->
                val inFocus = focus.contains(p.device.mac)
                val baseAlpha = if (p.device.isNamed) 1f else 0.45f
                val alpha = if (inFocus) baseAlpha else baseAlpha * 0.7f
                val pointRadius = if (inFocus) 7f else 4f

                drawCircle(
                    color = Ink.copy(alpha = alpha),
                    radius = pointRadius,
                    center = Offset(p.x, p.y)
                )
                if (inFocus) {
                    drawContext.canvas.nativeCanvas.drawText(
                        p.device.name, p.x + 12f, p.y + 8f, labelPaint
                    )
                }
            }
        }
    }
}

/** 计算设备光点在画布上的坐标（供绘制与点击命中复用） */
private fun placedPoints(devices: List<UiDevice>, size: Size): List<Placed> {
    val w = size.width
    val h = size.height
    val r = min(w, h) / 2f
    val cx = w / 2f
    val cy = h / 2f
    return devices.map { d ->
        val dist = d.distance.coerceAtMost(MAX_RANGE_M)
        val rr = (dist / MAX_RANGE_M).toFloat() * r
        val a = Math.toRadians(d.angle.toDouble())
        Placed(d, cx + rr * cos(a).toFloat(), cy + rr * sin(a).toFloat())
    }
}

private data class Placed(val device: UiDevice, val x: Float, val y: Float)

@Composable
private fun DeviceRow(device: UiDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                color = if (device.isNamed) Ink else Ink.copy(alpha = 0.5f),
                fontFamily = FontFamily.Cursive,
                fontSize = 16.sp
            )
            Text(
                text = device.mac,
                color = Ink.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        Text(
            text = "%.1f m · %d dBm".format(device.distance, device.rssi),
            color = Ink.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}
