package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.util.Random
import kotlin.math.sin

class AmbientSynthesizer {
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false
    private var thread: Thread? = null

    fun start(soundType: String, volume: Float) {
        stop()
        isPlaying = true

        val sampleRate = 22050 // Lower sample rate is extremely efficient and perfectly suited for low-frequency brown/rain noises
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)

        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.setVolume(volume)
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("AmbientSynthesizer", "Error starting AudioTrack", e)
            return
        }

        thread = Thread {
            val random = Random()
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            
            // Filters for low-pass
            var lastValue = 0.0f
            var lastValue2 = 0.0f

            while (isPlaying) {
                for (i in buffer.indices) {
                    when (soundType) {
                        "Summer Rain & Birds" -> {
                            // Rain: low-pass white noise
                            val white = random.nextFloat() * 2.0f - 1.0f
                            lastValue = 0.94f * lastValue + 0.06f * white
                            
                            // High frequency sharp drops (pitter-patter)
                            var droplet = 0.0f
                            if (random.nextFloat() < 0.0008f) { 
                                droplet = random.nextFloat() * 0.5f
                            }
                            
                            // 0.1Hz wind modulation
                            val wind = 0.7f + 0.3f * sin(2.0 * Math.PI * phase / sampleRate * 0.1).toFloat()
                            phase += 1.0

                            val sample = (lastValue + droplet * 0.4f) * wind
                            buffer[i] = (sample.coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
                        }
                        
                        "Forest Stream" -> {
                            // Stream: bubbly sweeps + bubbling sines + soft background rumble
                            val white = random.nextFloat() * 2.0f - 1.0f
                            lastValue = 0.90f * lastValue + 0.10f * white
                            
                            val bubbleFreq1 = 120.0 + 40.0 * sin(2.0 * Math.PI * phase * 1.5 / sampleRate)
                            val bubbleFreq2 = 250.0 + 70.0 * sin(2.0 * Math.PI * phase * 3.8 / sampleRate)
                            
                            val bubble1 = sin(2.0 * Math.PI * phase * bubbleFreq1 / sampleRate)
                            val bubble2 = sin(2.0 * Math.PI * phase * bubbleFreq2 / sampleRate)
                            
                            val vol1 = 0.5 + 0.5 * sin(2.0 * Math.PI * phase * 0.4 / sampleRate)
                            val vol2 = 0.5 + 0.5 * sin(2.0 * Math.PI * phase * 2.1 / sampleRate)
                            
                            val stream = (lastValue * 0.30f) + 
                                         (bubble1 * 0.15f * vol1).toFloat() + 
                                         (bubble2 * 0.10f * vol2).toFloat()
                            phase += 1.0
                            
                            buffer[i] = (stream.coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
                        }

                        "Warm Fireplace" -> {
                            // Crackling: very heavy low-pass (deep roar of fire) + sudden crackling clicks
                            val white = random.nextFloat() * 2.0f - 1.0f
                            lastValue = 0.98f * lastValue + 0.02f * white // Deep base rumble
                            
                            var pop = 0.0f
                            val roll = random.nextFloat()
                            if (roll < 0.001f) {
                                // Sudden sharp pop
                                pop = (random.nextFloat() * 0.8f - 0.4f)
                            }
                            
                            // Introduce some dynamic modulation (flame flickering)
                            val flicker = 0.8f + 0.2f * sin(2.0 * Math.PI * phase / sampleRate * 1.2).toFloat()
                            phase += 1.0
                            
                            val sample = (lastValue * 0.45f + pop * 0.35f) * flicker
                            buffer[i] = (sample.coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
                        }
                        
                        else -> { // "Cosmic Alpha Drone"
                            // Warm ambient chords with 10Hz binaural differential for flow state
                            val f1 = sin(2.0 * Math.PI * phase * 120.0 / sampleRate) // Deep G oscillation
                            val f2 = sin(2.0 * Math.PI * phase * 130.0 / sampleRate) // 10Hz alpha differential
                            val f3 = sin(2.0 * Math.PI * phase * 180.0 / sampleRate) // Warm minor 5th G-D
                            
                            val swell = 0.6 + 0.4 * sin(2.0 * Math.PI * phase * 0.08 / sampleRate)
                            
                            val white = random.nextFloat() * 2.0f - 1.0f
                            lastValue = 0.97f * lastValue + 0.03f * white
                            
                            val drone = (f1 * 0.3) + (f2 * 0.25) + (f3 * 0.2) + (lastValue * 0.15)
                            phase += 1.0
                            
                            val sample = (drone * swell).toFloat()
                            buffer[i] = (sample.coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
                        }
                    }
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
        thread?.start()
    }

    fun setVolume(volume: Float) {
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            Log.e("AmbientSynthesizer", "Error adjusting volume", e)
        }
    }

    fun stop() {
        isPlaying = false
        thread?.interrupt()
        thread = null
        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AmbientSynthesizer", "Error stopping AudioTrack", e)
        }
        audioTrack = null
    }
}
