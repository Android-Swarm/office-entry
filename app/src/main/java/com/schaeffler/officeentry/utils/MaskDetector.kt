package com.schaeffler.officeentry.utils

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import kotlinx.coroutines.tasks.await

/**
 * Represents face mask detection result.
 *
 * @property confidence The confidence of the result.
 * @property isWearingMask `true` if the detection result is wearing mask.
 */
data class DetectionResult(val confidence: Float, val isWearingMask: Boolean)

class MaskDetector(options: CustomObjectDetectorOptions, private val maskIndex: Int = 0) {
    private val labeler = ObjectDetection.getClient(options)

    /**
     * Sends an [InputImage] for mask detection.
     *
     * @param inputImage The image to check.
     */
    suspend fun detectMask(inputImage: InputImage): DetectionResult {
        labeler.process(inputImage)
            .await()
            .firstOrNull()
            ?.let { detected ->
                detected.labels
                    .also { list ->
                        list.joinToString("; ") { "(${it.index}: ${it.confidence})" }
                            .also { Log.d(TAG, it) }
                    }
                    .maxByOrNull { it.confidence }
                    ?.let { bestLabel ->

                        return DetectionResult(bestLabel.confidence, bestLabel.index == maskIndex)
                    }
            }

        Log.d(TAG, "No detected object!")

        return DetectionResult(1f, true)
    }

    companion object {
        private val TAG = MaskDetector::class.simpleName
        const val MODEL_FILENAME = "model.tflite"
    }
}