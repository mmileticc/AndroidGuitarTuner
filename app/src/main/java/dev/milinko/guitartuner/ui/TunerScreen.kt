package dev.milinko.guitartuner.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.milinko.guitartuner.model.GuitarTunings
import dev.milinko.guitartuner.ui.composables.NoteIndicatorBar
import dev.milinko.guitartuner.ui.composables.TunerScale
import dev.milinko.guitartuner.ui.composables.TuningSelector
import dev.milinko.guitartuner.viewmodel.TunerViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(viewModel: TunerViewModel) {
    val status by viewModel.tuningStatus.collectAsState()
    val volume by viewModel.volumeFlow.collectAsState()
    val currentTuning by viewModel.selectedTuning.collectAsState() // Uzmi trenutni štim
    var expanded by remember { mutableStateOf(false) }
    val volumeAnim by animateFloatAsState(targetValue = if(status.frequency > 0) volume else 0f, label = "vol")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {


        TuningSelector (
            currentTuning = currentTuning,
            onTuningChange = { viewModel.changeTuning(it) }
        )

        // 1. Gornji info - Fiksna visina (Box sa minHeight)
        Box(
            modifier = Modifier.height(80.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (status.frequency > 0) "${"%.1f".format(status.frequency)} Hz" else "--- Hz",
                    fontSize = 18.sp,
                    color = Color.Gray
                )
                // Koristimo fiksno mesto za tekst cilja da se ne pomera layout
                Text(
                    text = if (status.closestNote != null && status.frequency > 0)
                        "Cilj: ${status.closestNote!!.frequency} Hz" else "",
                    fontSize = 12.sp,
                    color = Color.Gray.copy(alpha = 0.6f)
                )
            }
        }

        // 2. Centralni deo - Koristi weight da popuni sredinu
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer(scaleX = 1f + volumeAnim * 5, scaleY = 1f + volumeAnim * 5)
                    .drawBehind {
                        drawCircle(
                            color = if (abs(status.diffCents) < 5 && status.frequency > 0)
                                Color.Green.copy(0.1f) else Color.Blue.copy(0.05f),
                            radius = size.minDimension / 2
                        )
                    }
            )

            Text(
                text = status.closestNote?.name ?: "--",
                fontSize = 100.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    status.frequency <= 0 -> Color.LightGray
                    abs(status.diffCents) < 5 -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        // --- NOVO: Vizuelni prikaz žica ---
        NoteIndicatorBar(
            notes = currentTuning.notes,
            activeNote = if (status.frequency > 0) status.closestNote else null,
            isTuned = abs(status.diffCents) < 5
        )
        // ----------------------------------

        // 3. Donji deo - Fiksiran kontejner
        Column(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TunerScale(diffCents = status.diffCents)

            Spacer(modifier = Modifier.height(20.dp))

            // ISPRAVLJENA LOGIKA:
            // diffCents > 0 -> Frekvencija je VEĆA od cilja -> OPUŠTAJ
            // diffCents < 0 -> Frekvencija je MANJA od cilja -> ZATEŽI
            Text(
                text = when {
                    status.frequency <= 0 -> "SVIRAJ ŽICU"
                    status.diffCents > 5 -> "OPUŠTAJ ↓"  // Previsoko
                    status.diffCents < -5 -> "ZATEŽI ↑"   // Prenisko
                    else -> "IDEALNO"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (abs(status.diffCents) < 5 && status.frequency > 0)
                    Color(0xFF2E7D32) else Color.Gray
            )

            // Fiksno mesto za cente (uvek zauzima prostor)
            Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
                if (status.frequency > 0) {
                    Text(
                        text = "${if (status.diffCents > 0) "+" else ""}${status.diffCents.toInt()} cents",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
