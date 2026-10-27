package com.rainy.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Wraps Android SpeechRecognizer and TextToSpeech. All calls must come from the main thread.
 * The microphone is only open between startListening() and the result, error or cancel.
 */
class VoiceController(context: Context, private val listener: Listener) {

    interface Listener {
        fun onListeningChanged(active: Boolean)
        fun onSpeakingChanged(active: Boolean)
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pending: String? = null

    // ---------------- speech to text ----------------

    fun startListening() {
        stopSpeaking()
        cancelListening()
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            listener.onError("Is phone par speech recognition service nahi mili. Text se likho.")
            return
        }
        val r = try {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        } catch (e: Exception) {
            listener.onError("Voice input start nahi hua. Text se likho.")
            return
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                if (recognizer !== r) return
                finish(r)
                listener.onError(describe(error))
            }

            override fun onResults(results: Bundle?) {
                if (recognizer !== r) return
                val text = first(results)
                finish(r)
                if (text.isNullOrBlank()) {
                    listener.onError("Kuch suna nahi gaya. Mic dabake dobara bolo.")
                } else {
                    listener.onFinal(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (recognizer !== r) return
                val text = first(partialResults)
                if (!text.isNullOrBlank()) listener.onPartial(text)
            }
        })
        recognizer = r
        listener.onListeningChanged(true)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        try {
            r.startListening(intent)
        } catch (e: Exception) {
            finish(r)
            listener.onError("Voice input start nahi hua. Text se likho.")
        }
    }

    /** Ends the recording normally; the recognizer then delivers the text it heard. */
    fun stopListening() {
        val r = recognizer ?: return
        try {
            r.stopListening()
        } catch (e: Exception) {
            finish(r)
        }
    }

    /** Discards the current recording immediately. */
    fun cancelListening() {
        val r = recognizer ?: return
        recognizer = null
        try {
            r.cancel()
        } catch (e: Exception) {
            // ignore
        }
        finish(r)
    }

    private fun finish(r: SpeechRecognizer) {
        if (recognizer === r) recognizer = null
        listener.onListeningChanged(false)
        main.post {
            try {
                r.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun first(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun describe(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Kuch suna nahi gaya. Mic dabake dobara bolo."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Mic permission nahi hai. Android Settings mein Microphone allow karo."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech service ko internet chahiye. Connection check karo ya text se likho."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "Speech service busy hai. Thori der baad try karo."
        SpeechRecognizer.ERROR_AUDIO ->
            "Mic se audio nahi mil rahi. Koi aur app mic use kar rahi ho sakti hai."
        SpeechRecognizer.ERROR_SERVER ->
            "Speech service mein error aaya. Baad mein try karo."
        else -> "Voice input fail hua (code $code). Text se likho."
    }

    // ---------------- text to speech ----------------

    fun speak(raw: String) {
        val clean = cleanForSpeech(raw)
        if (clean.isBlank()) return
        if (ttsReady) {
            doSpeak(clean)
        } else {
            pending = clean
            ensureTts()
        }
    }

    fun stopSpeaking() {
        pending = null
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
        listener.onSpeakingChanged(false)
    }

    fun shutdown() {
        cancelListening()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        tts = null
        ttsReady = false
    }

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status -> main.post { onTtsInit(status) } }
    }

    private fun resetTts() {
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
        tts = null
        ttsReady = false
        pending = null
    }

    private fun onTtsInit(status: Int) {
        val t = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            resetTts()
            listener.onError("Text-to-speech start nahi hua. Android Settings mein TTS engine check karo.")
            return
        }
        var r = t.setLanguage(Locale.forLanguageTag("en-IN"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            r = t.setLanguage(Locale.getDefault())
        }
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            resetTts()
            listener.onError(
                "Is phone par koi awaaz install nahi hai. Android Settings > Text-to-speech mein voice install karo."
            )
            return
        }
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post { listener.onSpeakingChanged(true) }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == UTT_LAST) main.post { listener.onSpeakingChanged(false) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { listener.onSpeakingChanged(false) }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                main.post { listener.onSpeakingChanged(false) }
            }
        })
        ttsReady = true
        val p = pending
        pending = null
        if (p != null) doSpeak(p)
    }

    private fun doSpeak(clean: String) {
        val t = tts ?: return
        val chunks = clean.chunked(3500)
        chunks.forEachIndexed { i, c ->
            val id = if (i == chunks.lastIndex) UTT_LAST else UTT_PART
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            if (t.speak(c, mode, null, id) == TextToSpeech.ERROR) {
                listener.onSpeakingChanged(false)
                listener.onError("Awaaz play nahi ho saki.")
                return
            }
        }
    }

    private fun cleanForSpeech(text: String): String =
        text
            .replace(Regex("```[\\s\\S]*?```"), " code block. ")
            .replace(Regex("`([^`]*)`"), "\$1")
            .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "\$1")
            .replace(Regex("[*#>~]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    companion object {
        private const val UTT_PART = "rainy_part"
        private const val UTT_LAST = "rainy_last"
    }
}
