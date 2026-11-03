package com.example.sound

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.model.StringsDictionary
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * Specialized Amharic Language Pack & Local Audio Library for YEME BINGO.
 * Provides high-fidelity native pronunciation, Ge'ez numeral transliteration,
 * and intelligent caching for all Bingo game calls.
 */
class AmharicLanguagePack(private val context: Context) {

  data class PackInfo(
    val name: String = "Ethiopian Native Amharic Voice Pack",
    val locale: String = "am_ET",
    val version: String = "2.4.0",
    val totalAudioClips: Int = 80,
    val isBundled: Boolean = true
  )

  val info = PackInfo()

  private val cacheDir = File(context.cacheDir, "amharic_voicepack").apply {
    if (!exists()) mkdirs()
  }

  private var activePlayer: MediaPlayer? = null
  private val playerLock = Any()

  /**
   * Retrieves the phonetic Amharic script text for any Bingo ball number (1..75)
   * Example: 15 -> "ቢ አስራ አምስት", 21 -> "አይ ሃያ አንድ"
   */
  fun getBallPhoneticText(number: Int): String {
    val letter = StringsDictionary.getBallLetter(number)
    val amLetter = StringsDictionary.AM_LETTERS[letter] ?: letter
    val amNumber = StringsDictionary.amharicNumber(number)
    return "$amLetter $amNumber"
  }

  /**
   * Plays a pre-recorded, studio-quality native Amharic audio clip from the bundled
   * assets library (`assets/amharic/`).
   */
  fun playAssetClip(
    assetFileName: String,
    volume: Float = 1.0f,
    onCompletion: (() -> Unit)? = null
  ): Boolean {
    synchronized(playerLock) {
      stopActivePlayback()

      return try {
        val afd: AssetFileDescriptor = context.assets.openFd("amharic/$assetFileName")
        val player = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
          setVolume(volume, volume)
          setOnCompletionListener { mp ->
            synchronized(playerLock) {
              try {
                mp.reset()
                mp.release()
                if (activePlayer == mp) activePlayer = null
              } catch (_: Exception) {
              }
            }
            onCompletion?.invoke()
          }
          prepare()
          start()
        }
        afd.close()
        activePlayer = player
        true
      } catch (e: Exception) {
        false
      }
    }
  }

  /**
   * Plays native Amharic call for a Bingo ball number (1..75)
   */
  fun playBallCall(number: Int, volume: Float = 1.0f, onCompletion: (() -> Unit)? = null): Boolean {
    return playAssetClip("ball_$number.mp3", volume, onCompletion)
  }

  /**
   * Plays countdown voice: "ተዘጋጁ" (Ready), "ሶስት" (3), "ሁለት" (2), "አንድ" (1)
   */
  fun playCountdownStep(step: String, volume: Float = 1.0f, onCompletion: (() -> Unit)? = null): Boolean {
    val asset = when (step) {
      "Ready" -> "ready.mp3"
      "3" -> "count_3.mp3"
      "2" -> "count_2.mp3"
      "1" -> "count_1.mp3"
      else -> return false
    }
    return playAssetClip(asset, volume, onCompletion)
  }

  /**
   * Plays celebration call: "ቢንጎ! አሸንፈዋል!"
   */
  fun playWinCelebration(volume: Float = 1.0f, onCompletion: (() -> Unit)? = null): Boolean {
    return playAssetClip("win.mp3", volume, onCompletion)
  }

  /**
   * Streams or loads dynamic Amharic sentences with local persistent disk caching.
   * Useful for dynamic notifications like player names or custom win banners.
   */
  fun speakDynamicAmharic(
    text: String,
    volume: Float = 1.0f,
    onCompletion: (() -> Unit)? = null
  ) {
    val safeHash = text.hashCode().toString()
    val cachedFile = File(cacheDir, "dyn_$safeHash.mp3")

    if (cachedFile.exists() && cachedFile.length() > 500) {
      playLocalFile(cachedFile, volume, onCompletion)
      return
    }

    // Fetch and cache in background
    thread(start = true) {
      try {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = URL("https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=am&q=$encoded")
        val conn = (url.openConnection() as HttpURLConnection).apply {
          setRequestProperty("User-Agent", "Mozilla/5.0")
          connectTimeout = 4000
          readTimeout = 4000
        }

        if (conn.responseCode == 200) {
          conn.inputStream.use { input ->
            FileOutputStream(cachedFile).use { output ->
              input.copyTo(output)
            }
          }
          playLocalFile(cachedFile, volume, onCompletion)
        } else {
          onCompletion?.invoke()
        }
      } catch (_: Exception) {
        onCompletion?.invoke()
      }
    }
  }

  private fun playLocalFile(file: File, volume: Float, onCompletion: (() -> Unit)? = null) {
    synchronized(playerLock) {
      stopActivePlayback()
      try {
        val player = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build()
          )
          setDataSource(file.absolutePath)
          setVolume(volume, volume)
          setOnCompletionListener { mp ->
            synchronized(playerLock) {
              try {
                mp.reset()
                mp.release()
                if (activePlayer == mp) activePlayer = null
              } catch (_: Exception) {
              }
            }
            onCompletion?.invoke()
          }
          prepare()
          start()
        }
        activePlayer = player
      } catch (_: Exception) {
        onCompletion?.invoke()
      }
    }
  }

  fun stopActivePlayback() {
    synchronized(playerLock) {
      activePlayer?.let { player ->
        try {
          if (player.isPlaying) player.stop()
          player.reset()
          player.release()
        } catch (_: Exception) {
        }
      }
      activePlayer = null
    }
  }

  fun release() {
    stopActivePlayback()
  }
}
