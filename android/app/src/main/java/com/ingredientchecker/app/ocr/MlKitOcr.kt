package com.ingredientchecker.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** On-device text recognition via ML Kit (no network). */
object MlKitOcr {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(context: Context, uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val lines = result.textBlocks.flatMap { block ->
                        block.lines.mapNotNull { line -> line.text?.trim()?.takeIf { it.isNotEmpty() } }
                    }
                    val text = if (lines.isNotEmpty()) {
                        lines.joinToString("\n")
                    } else {
                        result.text.trim()
                    }
                    cont.resume(text)
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
