package com.example.sound

import android.content.Context

class VoiceAndSoundManager(context: Context) {

  val customTtsEngine = CustomBingoTtsEngine(context)

  var isSoundEnabled: Boolean
    get() = !customTtsEngine.isSoundMuted
    set(value) {
      customTtsEngine.isSoundMuted = !value
    }

  var voiceLang: String
    get() = if (customTtsEngine.activeEngineType == CustomBingoTtsEngine.VoiceEngineType.AMHARIC_SPECIALIZED_PACK) "am" else "en"
    set(value) {
      customTtsEngine.activeEngineType = if (value.equals("am", ignoreCase = true)) {
        CustomBingoTtsEngine.VoiceEngineType.AMHARIC_SPECIALIZED_PACK
      } else {
        CustomBingoTtsEngine.VoiceEngineType.ENGLISH_TTS
      }
    }

  fun speak(textEn: String, textAm: String? = null) {
    customTtsEngine.speak(textEn, textAm)
  }

  fun announceBall(number: Int) {
    customTtsEngine.callBingoBall(number)
  }

  fun announceReady(step: String) {
    customTtsEngine.callCountdown(step)
  }

  fun announceWin() {
    customTtsEngine.callWin()
  }

  fun announceWinOther(winnerName: String) {
    customTtsEngine.callWin()
  }

  fun playTone(freqHz: Int, durationMs: Int) {
    customTtsEngine.playAcousticChime(freqHz, durationMs)
  }

  fun playFanfare() {
    customTtsEngine.playCelebrationFanfare()
  }

  fun release() {
    customTtsEngine.release()
  }
}
