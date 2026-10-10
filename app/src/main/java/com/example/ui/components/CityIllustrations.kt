package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CityColors
import com.example.util.cityText

@Composable
fun NeighborhoodArt(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().aspectRatio(2f)) {
        val k = minOf(size.width / 360f, size.height / 180f)
        withTransform({ translate((size.width - 360f * k) / 2, (size.height - 180f * k) / 2); scale(k, k, Offset.Zero) }) {
            val cream = Color(0xFFF1F2D6); val mint = Color(0xFF87B996); val dark = Color(0xFF0B3526)
            drawCircle(Color(0xFFDCEAA7), 32f, Offset(294f, 42f))
            drawRoundRect(Color(0xFF286347), Offset(0f, 117f), Size(360f, 55f), CornerRadius(22f))
            drawLine(Color(0xFF709379), Offset(0f, 157f), Offset(360f, 157f), 2f)
            // A row of distinct buildings forms the CleanCity visual signature.
            listOf(Triple(34f, 54f, 52f), Triple(98f, 34f, 62f), Triple(174f, 70f, 49f)).forEachIndexed { i, (x, y, w) ->
                drawRoundRect(if (i == 1) cream else mint, Offset(x, y), Size(w, 116f - y), CornerRadius(4f))
                for (row in 0..1) for (col in 0..1) drawRoundRect(dark, Offset(x + 10 + col * 22, y + 12 + row * 24), Size(12f, 14f), CornerRadius(2f))
                drawRect(dark, Offset(x + w / 2 - 6, 97f), Size(12f, 19f))
            }
            listOf(18f, 247f, 330f).forEach { x ->
                drawLine(cream, Offset(x, 118f), Offset(x, 70f), 3f)
                drawOval(mint, Offset(x - 16, 52f), Size(32f, 42f))
                drawLine(dark, Offset(x, 98f), Offset(x, 66f), 2f)
            }
            // Friendly collection truck, drawn locally so it works without a network.
            drawRoundRect(Color(0xFFDCEAA7), Offset(126f, 108f), Size(77f, 35f), CornerRadius(5f))
            drawRoundRect(cream, Offset(203f, 118f), Size(28f, 25f), CornerRadius(4f))
            drawRoundRect(dark, Offset(207f, 122f), Size(16f, 10f), CornerRadius(2f))
            listOf(143f, 213f).forEach { x ->
                drawCircle(dark, 9f, Offset(x, 145f)); drawCircle(mint, 4f, Offset(x, 145f))
            }
            drawLine(dark, Offset(143f, 120f), Offset(184f, 120f), 3f, StrokeCap.Round)
            drawLine(dark, Offset(143f, 129f), Offset(170f, 129f), 3f, StrokeCap.Round)
        }
    }
}
@Composable
fun BinArt(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(86.dp, 100.dp)) {
        val w = size.width; val h = size.height
        drawRoundRect(color.copy(alpha = .14f), Offset(0f, h * .1f), Size(w, h * .85f), CornerRadius(w * .3f))
        val p = Path().apply { moveTo(w*.23f,h*.3f); lineTo(w*.77f,h*.3f); lineTo(w*.7f,h*.79f); lineTo(w*.3f,h*.79f); close() }
        drawPath(p, color)
        drawRoundRect(color, Offset(w*.17f,h*.23f), Size(w*.66f,h*.07f), CornerRadius(3f))
        drawRoundRect(color, Offset(w*.37f,h*.17f), Size(w*.26f,h*.07f), CornerRadius(3f))
        drawLine(Color.White.copy(alpha=.65f), Offset(w*.42f,h*.42f), Offset(w*.42f,h*.67f), 3f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha=.65f), Offset(w*.58f,h*.42f), Offset(w*.58f,h*.67f), 3f, StrokeCap.Round)
        drawCircle(color, w*.045f, Offset(w*.33f,h*.83f)); drawCircle(color, w*.045f, Offset(w*.67f,h*.83f))
    }
}
@Composable
fun RouteIllustration(completed: Int = 0, total: Int = 4) {
    val background = CityColors.soft; val road = CityColors.surface; val forest = CityColors.primary; val block = CityColors.line
    CityPanel {
        StatusBadge(cityText("sampleMap"), "blue")
        Canvas(Modifier.fillMaxWidth().height(132.dp)) {
            drawRoundRect(background, cornerRadius = CornerRadius(20f))
            for (x in 0..4) for (y in 0..2) drawRoundRect(block, Offset(size.width * (.03f + x*.2f), size.height*(.06f+y*.32f)), Size(size.width*.13f,size.height*.21f), CornerRadius(8f))
            val route = Path().apply { moveTo(size.width*.08f,size.height*.72f); cubicTo(size.width*.45f,size.height*.9f,size.width*.35f,size.height*.18f,size.width*.64f,size.height*.27f); lineTo(size.width*.91f,size.height*.27f) }
            drawPath(route, road, style = Stroke(18.dp.toPx(), cap = StrokeCap.Round))
            drawPath(route, forest.copy(alpha=.65f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            for (i in 0 until total.coerceAtMost(12)) {
                val fraction = i.toFloat()/maxOf(total-1,1)
                val pt = Offset(size.width*(.08f+.83f*fraction),size.height*(.72f-.45f*fraction))
                drawCircle(road, 10.dp.toPx(), pt)
                drawCircle(if (i < completed) forest else block, 6.dp.toPx(), pt)
            }
        }
        Text(cityText("completed") + "  " + completed + " / " + total, style = MaterialTheme.typography.bodySmall, color = CityColors.muted)
    }
}

