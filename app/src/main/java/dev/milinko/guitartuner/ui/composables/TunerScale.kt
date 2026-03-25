package dev.milinko.guitartuner.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun TunerScale(diffCents: Float) {

    val animatedDiff by animateFloatAsState(
        targetValue = diffCents,
        animationSpec = tween(90), // 🔥 brz ali stabilan
        label = "diff"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 20.dp)
    ) {
        val center = size.width / 2
        val rangeCents = 50f
        val pixelsPerCent = size.width / (rangeCents * 2)

        // crtice
        for (i in -50..50 step 10) {
            val x = center + (i * pixelsPerCent)
            val lineHeight = if (i == 0) 40.dp.toPx() else 20.dp.toPx()
            val color = if (i == 0) Color.Red else Color.Gray.copy(alpha = 0.5f)

            drawLine(
                color = color,
                start = Offset(x, size.height / 2 - lineHeight / 2),
                end = Offset(x, size.height / 2 + lineHeight / 2),
                strokeWidth = if (i == 0) 3.dp.toPx() else 1.dp.toPx()
            )
        }

        // 👉 igla
        val pointerX = center + (animatedDiff.coerceIn(-50f, 50f) * pixelsPerCent)

        drawLine(
            color = if (abs(diffCents) < 5) Color.Green else Color(0xFF2196F3),
            start = Offset(pointerX, 0f),
            end = Offset(pointerX, size.height),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
