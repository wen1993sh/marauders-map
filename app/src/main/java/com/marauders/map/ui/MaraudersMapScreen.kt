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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauders.map.ble.BluetoothScanner
import com.marauders.map.model.ScannedDevice
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 雷达最大量程（米），超出会被压到边缘 */
private const val MAX_RANGE = 15.0

@Composable
fun MaraudersMapScreen(
    hasBluetooth: Boolean,
    scanner: BluetoothScanner,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    val devices = remember { mutableStateOf(emptyList<ScannedDevice>()) }
    var scanning by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        scanner.onUpdate = { devices.value = it }
        onDispose {
            scanner.onUpdate = null
            scanner.stop()
        }
    }

    MaraudersTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Parchment) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "活点地图",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Cursive,
                    color = Ink
                )
                Text(
                    "“我庄严宣誓，我干的都不是好事。”",
                    style = MaterialTheme.typography.bodySmall,
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
                    RadarView(devices.value)
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
                        "侦测到 ${devices.value.size} 个设备",
                        color = Ink,
                        fontFamily = FontFamily.Cursive
                    )
                }

                Spacer(Modifier.height(8.dp))
                Divider(color = Ink.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                // 设备列表
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(devices.value) { device -> DeviceRow(device) }
                }
            }
        }
    }
}

@Composable
private fun RadarView(devices: List<ScannedDevice>) {
    val sweep by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 4000, easing = LinearEasing))
    )

    val labelPaint = remember {
        Paint().apply {
            color = AndroidColor.parseColor("#3B2A1A")
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
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

        // 设备光点 + 手写标签
        devices.forEach { d ->
            val dist = d.distance.coerceAtMost(MAX_RANGE)
            val rr = (dist / MAX_RANGE).toFloat() * r
            val a = Math.toRadians(d.angle.toDouble())
            val x = cx + rr * cos(a).toFloat()
            val y = cy + rr * sin(a).toFloat()

            drawCircle(color = Ink, radius = 6f, center = Offset(x, y))
            drawContext.canvas.nativeCanvas.drawText(
                d.name, x + 12f, y + 8f, labelPaint
            )
        }
    }
}

@Composable
private fun DeviceRow(device: ScannedDevice) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                color = Ink,
                fontFamily = FontFamily.Cursive,
                fontSize = 16.sp
            )
            Text(
                text = device.address,
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
