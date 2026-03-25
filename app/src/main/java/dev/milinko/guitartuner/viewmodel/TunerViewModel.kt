package dev.milinko.guitartuner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.milinko.guitartuner.audio.AudioAnalyzer
import dev.milinko.guitartuner.model.GuitarTunings
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

    // Novi StateFlow za praćenje selektovanog štima
    private val _selectedTuning = MutableStateFlow(GuitarTunings.ALL_TUNINGS[0])
    val selectedTuning: StateFlow<Tuning> = _selectedTuning.asStateFlow()

    private var smoothedPitch = 0f
    private var lockedNote: Note? = null

    private val pitchBuffer = ArrayDeque<Float>()

    init {
        viewModelScope.launch {
            audioAnalyzer.pitchFlow.collect { pitch ->
                if (pitch > 0) processPitch(pitch)
            }
        }
    }

    private var lastStablePitch = 0f
    private var jumpCounter = 0
    private val MAX_JUMPS = 3 // Koliko puta dozvoljavamo "skok" pre nego što poverujemo

    private var detectionStartTime = 0L
    private fun processPitch(rawPitch: Float) {

        val currentTime = System.currentTimeMillis()
        val volume = volumeFlow.value

        // Ako je signal tek počeo (bio na nuli), zabeleži vreme
        if (smoothedPitch == 0f) {
            detectionStartTime = currentTime
            smoothedPitch = rawPitch // Postavi bazu, ali ne šalji još u UI status ako želiš mirnu iglu
            return
        }

        // Ignoriši prvih 150ms udara (Attack phase) da igla ne bi letela levo-desno
        if (currentTime - detectionStartTime < 150) {
            return
        }

        //val volume = volumeFlow.value
        if (volume < 0.015f) return

        // 1. FILTER SKOKOVA (Anti-Harmonic Logic)
        // Ako je skok preveliki (npr. više od oktave ili 50 Hz naglo), budi sumnjičav
        if (lastStablePitch > 0) {
            val deltaFromLast = abs(rawPitch - lastStablePitch)
            if (deltaFromLast > 30f) { // Prag za sumnjiv skok
                jumpCounter++
                if (jumpCounter < MAX_JUMPS) {
                    return // Ignorišemo ovaj "spike" dok se ne ponovi više puta
                }
            } else {
                jumpCounter = 0
            }
        }

        // 2. MEDIAN FILTER (Ovo ti je odlično, zadrži ga)
        val pitch = medianPitch(rawPitch)
        lastStablePitch = pitch

        // 3. ADAPTIVE SMOOTHING (Tvoja logika je super, samo mala korekcija)
        val delta = pitch - smoothedPitch
        val absDelta = abs(delta)

        // Ako je signal tek počeo (bio na 0), skoči odmah na taj pitch
        if (smoothedPitch == 0f) {
            smoothedPitch = pitch
        } else {
            val alpha = when {
                absDelta > 50f -> 0.8f // Nagli početak nove note
                absDelta > 10f -> 0.3f
                else           -> 0.1f // Fino štimovanje
            }
            smoothedPitch += alpha * delta
        }

        // 4. NOTE LOCK (Histerezis - tvoja getStableNote je već dobra)
        val note = getStableNote(smoothedPitch)
        var diffCents = calculateDiffCents(smoothedPitch, note.frequency)

        // 5. DEAD ZONE (Odlično za stabilnost igle)
        if (abs(diffCents) < 1.5f) diffCents = 0f

        _tuningStatus.value = TuningStatus(
            frequency = smoothedPitch,
            closestNote = note,
            diffCents = diffCents.coerceIn(-50f, 50f)
        )
    }
    private fun medianPitch(newPitch: Float): Float {
        pitchBuffer.addLast(newPitch)
        if (pitchBuffer.size > 5) pitchBuffer.removeFirst()

        val sorted = pitchBuffer.sorted()
        return sorted[sorted.size / 2]
    }

    private fun getStableNote(pitch: Float): Note {
        val closest = findClosestNote(pitch)

        if (lockedNote == null) {
            lockedNote = closest
        }

        val diff = abs(calculateDiffCents(pitch, lockedNote!!.frequency))

        if (diff > 35) { // tek kad se BAŠ pomeri menjaj notu
            lockedNote = closest
        }

        return lockedNote!!
    }

    fun changeTuning(tuning: Tuning) {
        _selectedTuning.value = tuning
        lockedNote = null // Resetuj lock da bi brže uhvatio nove ciljne frekvencije
        _tuningStatus.value = TuningStatus() // Očisti UI
    }
    private fun findClosestNote(pitch: Float): Note {
        val notes = _selectedTuning.value.notes
        return notes.minByOrNull {
            abs(calculateDiffCents(pitch, it.frequency))
        } ?: notes.first()
    }

    fun startListening() = audioAnalyzer.startListening()
    fun stopListening() = audioAnalyzer.stopListening()
}