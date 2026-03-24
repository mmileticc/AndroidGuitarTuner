package dev.milinko.guitartuner.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.flow.MutableStateFlow

class AudioAnalyzer {

    private var dispatcher: AudioDispatcher? = null

    val pitchFlow = MutableStateFlow(-1f)
    val volumeFlow = MutableStateFlow(0f)

    private val pitchBuffer = ArrayDeque<Float>()
    private val BUFFER_SIZE = 5

    private var smoothedVolume = 0f
    private val volumeAlpha = 0.1f

    fun startListening() {
        stopListening()

        val sampleRate = 22050
        val bufferSize = 2048
        val overlap = 0

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)

        val pdh = PitchDetectionHandler { result, _ ->
            val pitch = result.pitch
            val probability = result.probability

            if (pitch > 40f && probability > 0.85f && smoothedVolume > 0.01f) {

                // 👉 MEDIAN FILTER
                pitchBuffer.addLast(pitch)
                if (pitchBuffer.size > BUFFER_SIZE) pitchBuffer.removeFirst()

                val medianPitch = pitchBuffer.sorted()[pitchBuffer.size / 2]

                pitchFlow.value = medianPitch

            } else {
                // 👉 SOFT RESET (ne odmah na -1)
                pitchFlow.value *= 0.9f
                if (pitchFlow.value < 10f) pitchFlow.value = -1f
            }
        }

        val processor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            sampleRate.toFloat(),
            bufferSize,
            pdh
        )

        val volumeProcessor = object : AudioProcessor {
            override fun process(audioEvent: be.tarsos.dsp.AudioEvent?): Boolean {
                audioEvent?.let {
                    val rms = it.rms.toFloat()

                    // 👉 EMA ZA VOLUME
                    smoothedVolume += volumeAlpha * (rms - smoothedVolume)
                    volumeFlow.value = smoothedVolume
                }
                return true
            }

            override fun processingFinished() {}
        }

        dispatcher?.addAudioProcessor(volumeProcessor)
        dispatcher?.addAudioProcessor(processor)

        Thread(dispatcher, "Audio Dispatcher").start()
    }

    fun stopListening() {
        dispatcher?.stop()
        dispatcher = null
    }
}