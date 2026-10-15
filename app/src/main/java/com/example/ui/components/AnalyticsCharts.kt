package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TeamMember

data class ChartBarData(
    val label: String,
    val targetValue: Float,
    val achievedValue: Float
)

data class TrendPoint(
    val label: String,
    val value: Float
)

/**
 * Modern Bar Chart visualizing Target vs Achieved for Sales Reps (Admin/Team View).
 * Built with native Compose Canvas replicating D3/Recharts styling.
 */
@Composable
fun TeamSalesBarChart(
    data: List<ChartBarData>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxVal = remember(data) {
        val max = data.maxOfOrNull { maxOf(it.targetValue, it.achievedValue) } ?: 1f
        if (max == 0f) 1f else max * 1.15f
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("team_sales_bar_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Team Performance (Target vs Achieved)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Revenue in Lakhs (₹)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Achieved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Target", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Canvas Bar Drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height - 30f // reserve bottom for labels
                    val groupCount = data.size
                    if (groupCount == 0) return@Canvas

                    val groupWidth = width / groupCount
                    val barWidth = (groupWidth * 0.32f).coerceAtMost(28f)
                    val spacing = 4f

                    // Draw 3 horizontal gridlines
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    data.forEachIndexed { index, item ->
                        val groupCenterX = (index * groupWidth) + (groupWidth / 2f)

                        // Target Bar (Background bar)
                        val targetHeight = (item.targetValue / maxVal) * height
                        val targetTop = height - targetHeight
                        val targetLeft = groupCenterX - barWidth - (spacing / 2f)

                        drawRoundRect(
                            color = Color(0xFFCBD5E1).copy(alpha = 0.6f),
                            topLeft = Offset(targetLeft, targetTop),
                            size = Size(barWidth, targetHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Achieved Bar (Foreground vibrant bar)
                        val achievedHeight = (item.achievedValue / maxVal) * height
                        val achievedTop = height - achievedHeight
                        val achievedLeft = groupCenterX + (spacing / 2f)

                        val barBrush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)),
                            startY = achievedTop,
                            endY = height
                        )

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(achievedLeft, achievedTop),
                            size = Size(barWidth, achievedHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Rep Label Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                data.forEach { item ->
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Curved Line / Area Sales Trend Chart (replicating Recharts AreaChart).
 */
@Composable
fun RevenueTrendLineChart(
    title: String,
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(points) {
        val max = points.maxOfOrNull { it.value } ?: 1f
        if (max == 0f) 1f else max * 1.2f
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("revenue_trend_line_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "+18.4% vs last period",
                        color = Color(0xFF16A34A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Area & Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height - 20f
                    if (points.size < 2) return@Canvas

                    val stepX = width / (points.size - 1)

                    val linePath = Path()
                    val areaPath = Path()

                    points.forEachIndexed { i, p ->
                        val x = i * stepX
                        val y = height - ((p.value / maxVal) * height)
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, height)
                            areaPath.lineTo(x, y)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevY = height - ((points[i - 1].value / maxVal) * height)
                            val cX1 = prevX + (x - prevX) / 2f
                            val cY1 = prevY
                            val cX2 = prevX + (x - prevX) / 2f
                            val cY2 = y
                            linePath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                            areaPath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                        }
                    }

                    areaPath.lineTo(width, height)
                    areaPath.close()

                    // Draw area gradient fill
                    val gradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF38BDF8).copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = height
                    )
                    drawPath(path = areaPath, brush = gradient)

                    // Draw trend line
                    drawPath(
                        path = linePath,
                        color = Color(0xFF0284C7),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw point dots
                    points.forEachIndexed { i, p ->
                        val x = i * stepX
                        val y = height - ((p.value / maxVal) * height)
                        drawCircle(
                            color = Color(0xFF0284C7),
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Horizontal labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { p ->
                    Text(
                        text = p.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
