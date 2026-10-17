package com.apex.studio

import androidx.compose.runtime.Immutable

@Immutable
data class StudioGradeState(
    val exposure: Float = 0f,
    val contrast: Float = 1.05f,
    val saturation: Float = 1.10f,
    val vignette: Float = 0.25f,
    val cinemaBars: Float = 0.0f,
    val lutPreset: Int = 0,
    val stickerBorder: Boolean = true,
    val aspectRatio: Float = 16f / 9f
)

@Immutable
data class OverlayLayer(
    val id: Long,
    val text: String,
    val isBadge: Boolean = false,
    val colorHex: Long = 0xFFFACC15,
    val strokeHex: Long = 0xFF000000,
    val xNorm: Float = 0.5f,
    val yNorm: Float = 0.5f,
    val scale: Float = 1.0f
)

object ColorMatrixBuilder {
    fun buildLumetriMatrix(state: StudioGradeState, outMatrix: FloatArray) {
        val exp = Math.pow(2.0, state.exposure.toDouble()).toFloat()
        val c = state.contrast
        val t = (1.0f - c) * 128f

        val s = if (state.lutPreset == 4) 0f else state.saturation
        val invS = 1.0f - s
        val rLum = 0.2126f * invS
        val gLum = 0.7152f * invS
        val bLum = 0.0722f * invS

        var rBoost = 1f; var gBoost = 1f; var bBoost = 1f
        when (state.lutPreset) {
            1 -> { rBoost = 1.08f; gBoost = 1.02f; bBoost = 1.12f }
            2 -> { rBoost = 1.14f; gBoost = 0.92f; bBoost = 1.25f }
            3 -> { rBoost = 1.12f; gBoost = 1.10f; bBoost = 1.05f }
        }

        val scaleR = c * exp * rBoost
        val scaleG = c * exp * gBoost
        val scaleB = c * exp * bBoost

        outMatrix[0] = (rLum + s) * scaleR
        outMatrix[1] = gLum * scaleR
        outMatrix[2] = bLum * scaleR
        outMatrix[3] = 0f
        outMatrix[4] = t

        outMatrix[5] = rLum * scaleG
        outMatrix[6] = (gLum + s) * scaleG
        outMatrix[7] = bLum * scaleG
        outMatrix[8] = 0f
        outMatrix[9] = t

        outMatrix[10] = rLum * scaleB
        outMatrix[11] = gLum * scaleB
        outMatrix[12] = (bLum + s) * scaleB
        outMatrix[13] = 0f
        outMatrix[14] = t

        outMatrix[15] = 0f; outMatrix[16] = 0f; outMatrix[17] = 0f; outMatrix[18] = 1f; outMatrix[19] = 0f
    }
}
