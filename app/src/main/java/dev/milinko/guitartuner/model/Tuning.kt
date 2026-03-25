package dev.milinko.guitartuner.model

import kotlin.math.*

data class Note(
    val name: String,
    val frequency: Float
)

data class Tuning(
    val name: String,
    val notes: List<Note>
)

object StandardTuning {
    val GUITAR_6_STRING = Tuning(
        "Standard Guitar (6 strings)",
        listOf(
            Note("E2", 82.41f),
            Note("A2", 110.00f),
            Note("D3", 146.83f),
            Note("G3", 196.00f),
            Note("B3", 246.94f),
            Note("E4", 329.63f)
        )
    )
}
object GuitarTunings {
    val ALL_TUNINGS = listOf(
        Tuning("Standard", listOf(
            Note("E2", 82.41f), Note("A2", 110.00f), Note("D3", 146.83f),
            Note("G3", 196.00f), Note("B3", 246.94f), Note("E4", 329.63f)
        )),
        Tuning("Drop D", listOf(
            Note("D2", 73.42f), Note("A2", 110.00f), Note("D3", 146.83f),
            Note("G3", 196.00f), Note("B3", 246.94f), Note("E4", 329.63f)
        )),
        Tuning("Half Step Down", listOf(
            Note("Eb2", 77.78f), Note("Ab2", 103.83f), Note("Db3", 138.59f),
            Note("Gb3", 185.00f), Note("Bb3", 233.08f), Note("Eb4", 311.13f)
        )),
        Tuning("Open G", listOf(
            Note("D2", 73.42f), Note("G2", 98.00f), Note("D3", 146.83f),
            Note("G3", 196.00f), Note("B3", 246.94f), Note("D4", 293.66f)
        )),
        Tuning("DADGAD", listOf(
            Note("D2", 73.42f), Note("A2", 110.00f), Note("D3", 146.83f),
            Note("G3", 196.00f), Note("A3", 220.00f), Note("D4", 293.66f)
        ))
    )
}

data class TuningStatus(
    val frequency: Float = 0f,
    val closestNote: Note? = null,
    val diffCents: Float = 0f
)

fun calculateDiffCents(currentFreq: Float, targetFreq: Float): Float {
    if (currentFreq <= 0f || targetFreq <= 0f) return 0f
    return (1200 * log2(currentFreq / targetFreq)).toFloat()
}
