package dev.milinko.guitartuner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.milinko.guitartuner.audio.AudioAnalyzer
import dev.milinko.guitartuner.model.Note
import dev.milinko.guitartuner.model.StandardTuning
import dev.milinko.guitartuner.model.Tuning
import dev.milinko.guitartuner.model.TuningStatus
import dev.milinko.guitartuner.model.calculateDiffCents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class TunerViewModel : ViewModel() {

    private val audioAnalyzer = AudioAnalyzer()

    private val _tuningStatus = MutableStateFlow(TuningStatus())
    val tuningStatus: StateFlow<TuningStatus> = _tuningStatus

    val volumeFlow: StateFlow<Float> = audioAnalyzer.volumeFlow

    private val currentTuning: Tuning = StandardTuning.GUITAR_6_STRING

    private var smoothedPitch = 0f
    private var lockedNote: Note? = null
    private var lockCounter = 0

    private val alphaSlow = 0.1f
    private val alphaFast = 0.3f

    init {
        viewModelScope.launch {
            audioAnalyzer.pitchFlow.collect { pitch ->
                if (pitch > 0) processPitch(pitch)
                else reset()
            }
        }
    }

    private fun processPitch(pitch: Float) {
        if (pitch !in 40f..1200f) return

        // 👉 ADAPTIVE EMA
        val diff = abs(pitch - smoothedPitch)
        val alpha = if (diff > 5f) alphaFast else alphaSlow
        smoothedPitch += alpha * (pitch - smoothedPitch)

        val closestNote = findClosestNote(smoothedPitch)
        val diffCents = calculateDiffCents(smoothedPitch, closestNote.frequency)

        // 👉 NOTE LOCK
        if (lockedNote == null || lockedNote == closestNote) {
            lockCounter++
            if (lockCounter > 3) lockedNote = closestNote
        } else {
            lockCounter = 0
        }

        val finalNote = lockedNote ?: closestNote

        // 👉 HYSTERESIS (ne menjaj notu ako je razlika mala)
        if (lockedNote != null) {
            val centsFromLocked = calculateDiffCents(smoothedPitch, lockedNote!!.frequency)
            if (abs(centsFromLocked) < 20) {
                // ostani na istoj noti
            } else {
                lockedNote = null
            }
        }

        _tuningStatus.value = TuningStatus(
            frequency = smoothedPitch,
            closestNote = finalNote,
            diffCents = calculateDiffCents(smoothedPitch, finalNote.frequency)
        )
    }

    private fun reset() {
        smoothedPitch *= 0.9f
        if (smoothedPitch < 10f) {
            smoothedPitch = 0f
            lockedNote = null
        }

        _tuningStatus.value = TuningStatus(frequency = smoothedPitch)
    }

    private fun findClosestNote(pitch: Float): Note {
        return currentTuning.notes.minByOrNull {
            abs(calculateDiffCents(pitch, it.frequency))
        }!!
    }

    fun startListening() = audioAnalyzer.startListening()

    fun stopListening() {
        audioAnalyzer.stopListening()
        reset()
    }

    override fun onCleared() {
        super.onCleared()
        audioAnalyzer.stopListening()
    }
}
