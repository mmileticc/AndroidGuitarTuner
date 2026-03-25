package dev.milinko.guitartuner.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class AudioAnalyzer {
    private var dispatcher: AudioDispatcher? = null
    val pitchFlow = MutableStateFlow(-1f)
    val volumeFlow = MutableStateFlow(0f)

    private var smoothedPitch = 0f
    private var smoothedVolume = 0f

    // NOVE VARIJABLE ZA STABILNOST
    private var invalidDetectionCount = 0
    private val MAX_INVALID_ATTEMPTS = 5 // Dozvoljavamo 5 loših očitavanja pre gašenja

    fun startListening() {
        if (dispatcher != null) return // Zaštita da ne pokreneš dva puta

        val sampleRate = 22050
        val bufferSize = 1024 // Smanji na 1024 za brži odziv
        val overlap = 512

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)

        // 1. Prvo izvlačimo jačinu
        val volumeProcessor = object : AudioProcessor {
            override fun process(audioEvent: be.tarsos.dsp.AudioEvent?): Boolean {
                audioEvent?.let {
                    val rms = it.rms.toFloat()
                    smoothedVolume = smoothedVolume * 0.8f + rms * 0.2f // EMA
                    volumeFlow.value = smoothedVolume
                }
                return true
            }
            override fun processingFinished() {}
        }

        // 2. Onda detekcija tona
        val pdh = PitchDetectionHandler { result, _ ->
            val pitch = result.pitch
            val prob = result.probability

            // LOGIKA POVERENJA:
            if (pitch > 60f && pitch < 500f && prob > 0.80f && smoothedVolume > 0.008f) {
                // Signal je čist i jak
                invalidDetectionCount = 0

                // EMA filter za pitch (0.2f je dobro)
                smoothedPitch = smoothedPitch * 0.8f + pitch * 0.2f
                pitchFlow.value = smoothedPitch
            } else {
                // Algoritam trenutno ne vidi dobar ton, ali ne gasimo odmah!
                invalidDetectionCount++

                if (invalidDetectionCount > MAX_INVALID_ATTEMPTS) {
                    fadeOut()
                }
            }
        }

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            sampleRate.toFloat(),
            bufferSize,
            pdh
        )

        dispatcher?.addAudioProcessor(volumeProcessor)
        dispatcher?.addAudioProcessor(pitchProcessor)

        Thread(dispatcher, "Audio Analyzer Thread").start()
    }

    private fun fadeOut() {
        if (smoothedPitch > 0) {
            smoothedPitch *= 0.85f // Lagani pad
            if (smoothedPitch < 40f) smoothedPitch = -1f
            pitchFlow.value = smoothedPitch
        }
    }

    fun stopListening() {
        dispatcher?.stop()
        dispatcher = null
        smoothedPitch = -1f
        pitchFlow.value = -1f
    }
}
