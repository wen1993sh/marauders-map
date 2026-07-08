package com.marauders.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marauders.map.data.local.SightingEntity
import com.marauders.map.model.UiDevice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设备详情卡（底部弹层内容）：名字、MAC、信号、距离、
 * 首次出现、出现次数，以及近期出现时间线。
 */
@Composable
fun DeviceDetailSheet(
    device: UiDevice,
    sightings: List<SightingEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Parchment,
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        device.name,
                        fontFamily = FontFamily.Cursive,
                        fontSize = 22.sp,
                        color = Ink
                    )
                    Text(
                        device.mac,
                        fontSize = 11.sp,
                        color = Ink.copy(alpha = 0.5f)
                    )
                }
                Button(onClick = onDismiss) { Text("关闭") }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailStat("信号", "%d dBm".format(device.rssi))
                DetailStat("估算距离", "%.1f m".format(device.distance))
                DetailStat("出现次数", "%d 次".format(device.seenCount))
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "首次出现：${device.firstSeen?.let { dateFmt.format(Date(it)) } ?: "—"}",
                color = Ink,
                fontFamily = FontFamily.Cursive
            )

            Spacer(Modifier.height(10.dp))
            Text("近期时间线", color = Ink, fontFamily = FontFamily.Cursive)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                items(sightings) { s ->
                    Text(
                        "%s · %d dBm · %.1f m".format(
                            dateFmt.format(Date(s.timestamp)), s.rssi, s.distance
                        ),
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Ink.copy(alpha = 0.5f))
        Text(
            value,
            fontSize = 16.sp,
            color = Ink,
            fontFamily = FontFamily.Cursive
        )
    }
}
