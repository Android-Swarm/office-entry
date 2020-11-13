package com.schaeffler.officeentry.di

import android.content.Context
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.schaeffler.officeentry.utils.MaskDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@InstallIn(ActivityComponent::class)
@Module
class ActivityModule {
    @Provides
    fun provideMaskDetector(): MaskDetector {
        // Load custom TFlite Model
        val localModel = LocalModel.Builder()
            .setAssetFilePath(MaskDetector.MODEL_FILENAME)
            .build()

        val customOptions = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .setClassificationConfidenceThreshold(0.8f)
            .enableClassification()
            .build()

        return MaskDetector(customOptions)
    }

    @Provides
    fun provideFaceDetector() = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .build().run {
            FaceDetection.getClient(this)
        }

    @Provides
    fun provideWifiManager(@ApplicationContext context: Context) =
        ContextCompat.getSystemService(context, WifiManager::class.java)!!
}