package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.example.model.StringsDictionary
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.sin

/**
 * Custom Text-To-Speech (TTS) Engine Wrapper for YEME BINGO.
 * Integrates specialized Amharic language packs and local audio library
 * with fallback to Android TextToSpeech and synthesized acoustic cues.
 */
class CustomBingoTtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

  enum class VoiceEngineType {
    AMHARIC_SPECIALIZED_PACK,
    ENGLISH_TTS
  }

  private val amharicPack = AmharicLanguagePack(context)
  private var platformTts: TextToSpeech? = null
  private var isTtsInitialized = false
  private var englishVoice: Voice? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  var isSoundMuted: Boolean = false
  var activeEngineType: VoiceEngineType = VoiceEngineType.AMHARIC_SPECIALIZED_PACK
  var volumeLevel: Float = 1.0f

  init {
    try {
      platformTts = TextToSpeech(context.applicationContext, this)
    } catch (_: Exception) {
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      isTtsInitialized = true
      detectAndConfigureVoices()
    }
  }

  private fun detectAndConfigureVoices() {
    val tts = platformTts ?: return
    try {
      val voices = tts.voices ?: emptySet()
      englishVoice = voices.firstOrNull { it.locale.language.equals("en", ignoreCase = true) }
      if (englishVoice != null) {
        tts.voice = englishVoice
      } else {
        tts.language = Locale.US
      }
      tts.setPitch(0.9f)
      tts.setSpeechRate(0.85f)
    } catch (_: Exception) {
      try {
        tts.language = Locale.US
      } catch (_: Exception) {
      }
    }
  }

  /**
   * Main Bingo Ball Announcer
   * Calls the number exclusively in native Amharic.
   */
  fun callBingoBall(number: Int) {
    if (isSoundMuted) return

    // Calling voice is exclusively Amharic
    val success = amharicPack.playBallCall(number, volume = volumeLevel)
    if (!success) {
      val phoneticText = amharicPack.getBallPhoneticText(number)
      amharicPack.speakDynamicAmharic(phoneticText, volume = volumeLevel)
    }
  }

  /**
   * Countdown Announcer ("ተዘጋጁ", "ሶስት", "ሁለት", "አንድ") exclusively in Amharic
   */
  fun callCountdown(step: String) {
    if (isSoundMuted) return

    val success = amharicPack.playCountdownStep(step, volume = volumeLevel)
    if (!success) {
      val amharicStep = when (step) {
        "Ready" -> "ተዘጋጁ"
        "3" -> "ሶስት"
        "2" -> "ሁለት"
        "1" -> "አንድ"
        else -> step
      }
      amharicPack.speakDynamicAmharic(amharicStep, volume = volumeLevel)
    }
  }

  /**
   * Celebration & Win Announcer exclusively in Amharic
   */
  fun callWin() {
    if (isSoundMuted) return

    val success = amharicPack.playWinCelebration(volume = volumeLevel)
    if (!success) {
      amharicPack.speakDynamicAmharic("ቢንጎ! አሸንፈዋል!", volume = volumeLevel)
    }
    playCelebrationFanfare()
  }

  /**
   * General speech synthesis routed exclusively to Amharic
   */
  fun speak(textEn: String, textAm: String? = null) {
    if (isSoundMuted) return

    val phrase = if (!textAm.isNullOrBlank()) textAm else textEn
    when (phrase) {
      "ተዘጋጁ" -> amharicPack.playCountdownStep("Ready", volume = volumeLevel)
      "ቢንጎ! አሸንፈዋል!" -> amharicPack.playWinCelebration(volume = volumeLevel)
      else -> amharicPack.speakDynamicAmharic(phrase, volume = volumeLevel)
    }
  }

  private fun speakEnglishPhrase(phrase: String) {
    val tts = platformTts
    if (tts == null || !isTtsInitialized) {
      playAcousticChime(880, 150)
      return
    }
    try {
      tts.stop()
      tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "BINGO_TTS")
    } catch (_: Exception) {
      playAcousticChime(880, 150)
    }
  }

  fun stop() {
    amharicPack.stopActivePlayback()
    try {
      platformTts?.stop()
    } catch (_: Exception) {
    }
  }

  fun playAcousticChime(freqHz: Int, durationMs: Int) {
    if (isSoundMuted) return
    thread(start = true) {
      try {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(numSamples)
        val angularFreq = 2.0 * Math.PI * freqHz / sampleRate

        for (i in 0 until numSamples) {
          val envelope = when {
            i < 200 -> i / 200.0
            i > numSamples - 200 -> (numSamples - i) / 200.0
            else -> 1.0
          }
          buffer[i] = (sin(angularFreq * i) * 32767.0 * 0.4 * envelope).toInt().toShort()
        }

        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(buffer.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        mainHandler.postDelayed({
          try {
            track.stop()
            track.release()
          } catch (_: Exception) {
          }
        }, durationMs + 100L)
      } catch (_: Exception) {
      }
    }
  }

  fun playCelebrationFanfare() {
    thread(start = true) {
      val notes = listOf(523, 659, 784, 1046)
      for (n in notes) {
        playAcousticChime(n, 140)
        try {
          Thread.sleep(150)
        } catch (_: Exception) {
        }
      }
    }
  }

  fun release() {
    amharicPack.release()
    try {
      platformTts?.stop()
      platformTts?.shutdown()
      platformTts = null
    } catch (_: Exception) {
    }
  }
}
