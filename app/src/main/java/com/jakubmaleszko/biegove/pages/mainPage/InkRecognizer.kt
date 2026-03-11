package com.jakubmaleszko.biegove.pages.mainPage

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink

class InkRecognizer(context: Context) {
    private var recognizer: DigitalInkRecognizer? = null
    private val model: DigitalInkRecognitionModel

    init {
        // "en" is excellent for general digits (0-9)
        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en")!!
        model = DigitalInkRecognitionModel.builder(modelIdentifier).build()

        val remoteModelManager = RemoteModelManager.getInstance()
        remoteModelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model).build()
                )
                android.util.Log.d("InkRecognizer", "Model ready for digits")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("InkRecognizer", "Model download failed", e)
            }
    }

    fun recognize(ink: Ink, onResult: (String) -> Unit) {
        val client = recognizer
        if (client == null) {
            onResult("") // Model not downloaded yet
            return
        }

        client.recognize(ink)
            .addOnSuccessListener { result ->
                // Get the best candidate
                val candidate = result.candidates.firstOrNull()?.text ?: ""

                // Clean the output: Keep only digits
                val digitsOnly = candidate.filter { it.isDigit() }

                onResult(digitsOnly)
            }
            .addOnFailureListener { e ->
                onResult("")
            }
    }
}