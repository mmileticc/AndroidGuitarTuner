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

data class TuningStatus(
    val frequency: Float = 0f,
    val closestNote: Note? = null,
    val diffCents: Float = 0f
)

fun calculateDiffCents(currentFreq: Float, targetFreq: Float): Float {
    if (currentFreq <= 0f || targetFreq <= 0f) return 0f
    return (1200 * log2(currentFreq / targetFreq)).toFloat()
}
