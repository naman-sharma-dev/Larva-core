package com.stickersnip.app

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * CamScanner-style edge detection for auto-cropping screenshots.
 * Detects dark UI bars (e.g., from Instagram/social media) by analyzing
 * luminance gradients along rows and columns.
 */
object EdgeDetector {

    private const val SAMPLE_STEP = 4
    private const val SMOOTH_WINDOW = 5
    private const val GRADIENT_THRESHOLD = 8.0f
    private const val LUMINANCE_FALLBACK_THRESHOLD = 30f
    private const val PADDING = 4
    private const val MIN_CROP_SIZE = 50

    /**
     * Returns a Rect representing the auto-detected content region of the bitmap.
     */
    fun detectEdges(bitmap: Bitmap): Rect {
        val width = bitmap.width
        val height = bitmap.height

        val rowLuminance = getRowAverageLuminance(bitmap, width, height)
        val colLuminance = getColAverageLuminance(bitmap, width, height)

        val smoothedRows = smooth(rowLuminance, SMOOTH_WINDOW)
        val smoothedCols = smooth(colLuminance, SMOOTH_WINDOW)

        val top = findTopEdge(smoothedRows, height)
        val bottom = findBottomEdge(smoothedRows, height)
        val left = findLeftEdge(smoothedCols, width)
        val right = findRightEdge(smoothedCols, width)

        // Apply padding and clamp
        val finalLeft = (left - PADDING).coerceAtLeast(0)
        val finalTop = (top - PADDING).coerceAtLeast(0)
        val finalRight = (right + PADDING).coerceAtMost(width)
        val finalBottom = (bottom + PADDING).coerceAtMost(height)

        // If result is too small, return full image
        if (finalRight - finalLeft < MIN_CROP_SIZE || finalBottom - finalTop < MIN_CROP_SIZE) {
            return Rect(0, 0, width, height)
        }

        return Rect(finalLeft, finalTop, finalRight, finalBottom)
    }

    /**
     * Computes per-row average luminance by sampling every SAMPLE_STEP pixels.
     */
    private fun getRowAverageLuminance(bitmap: Bitmap, width: Int, height: Int): FloatArray {
        val luminance = FloatArray(height)
        for (y in 0 until height) {
            var sum = 0f
            var count = 0
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                sum += pixelLuminance(pixel)
                count++
                x += SAMPLE_STEP
            }
            luminance[y] = if (count > 0) sum / count else 0f
        }
        return luminance
    }

    /**
     * Computes per-column average luminance by sampling every SAMPLE_STEP pixels.
     */
    private fun getColAverageLuminance(bitmap: Bitmap, width: Int, height: Int): FloatArray {
        val luminance = FloatArray(width)
        for (x in 0 until width) {
            var sum = 0f
            var count = 0
            var y = 0
            while (y < height) {
                val pixel = bitmap.getPixel(x, y)
                sum += pixelLuminance(pixel)
                count++
                y += SAMPLE_STEP
            }
            luminance[x] = if (count > 0) sum / count else 0f
        }
        return luminance
    }

    /**
     * Returns luminance (0–255) for a packed ARGB pixel.
     */
    private fun pixelLuminance(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    /**
     * Smooths an array with a sliding window average.
     */
    private fun smooth(values: FloatArray, window: Int): FloatArray {
        val result = FloatArray(values.size)
        val half = window / 2
        for (i in values.indices) {
            var sum = 0f
            var count = 0
            for (j in (i - half)..(i + half)) {
                if (j in values.indices) {
                    sum += values[j]
                    count++
                }
            }
            result[i] = sum / count
        }
        return result
    }

    /**
     * Finds top edge: row with maximum dark→bright gradient in top 60%.
     */
    private fun findTopEdge(smoothed: FloatArray, height: Int): Int {
        val searchLimit = (height * 0.6f).toInt()
        var maxGradient = 0f
        var bestRow = 0
        for (i in 1 until searchLimit.coerceAtMost(smoothed.size)) {
            val gradient = smoothed[i] - smoothed[i - 1] // dark→bright = positive
            if (gradient > maxGradient) {
                maxGradient = gradient
                bestRow = i
            }
        }
        if (maxGradient < GRADIENT_THRESHOLD) {
            // Fallback: scan until luminance exceeds threshold
            for (i in 0 until searchLimit.coerceAtMost(smoothed.size)) {
                if (smoothed[i] > LUMINANCE_FALLBACK_THRESHOLD) {
                    return i
                }
            }
            return 0
        }
        return bestRow
    }

    /**
     * Finds bottom edge: row with maximum bright→dark gradient in bottom 60%.
     */
    private fun findBottomEdge(smoothed: FloatArray, height: Int): Int {
        val searchStart = (height * 0.4f).toInt()
        var maxGradient = 0f
        var bestRow = height
        for (i in (searchStart + 1 until smoothed.size).reversed()) {
            val gradient = smoothed[i - 1] - smoothed[i] // bright→dark = positive
            if (gradient > maxGradient) {
                maxGradient = gradient
                bestRow = i
            }
        }
        if (maxGradient < GRADIENT_THRESHOLD) {
            // Fallback: scan from bottom until luminance exceeds threshold
            for (i in (smoothed.size - 1) downTo searchStart) {
                if (smoothed[i] > LUMINANCE_FALLBACK_THRESHOLD) {
                    return i + 1
                }
            }
            return height
        }
        return bestRow
    }

    /**
     * Finds left edge: column with maximum dark→bright gradient in left 60%.
     */
    private fun findLeftEdge(smoothed: FloatArray, width: Int): Int {
        val searchLimit = (width * 0.6f).toInt()
        var maxGradient = 0f
        var bestCol = 0
        for (i in 1 until searchLimit.coerceAtMost(smoothed.size)) {
            val gradient = smoothed[i] - smoothed[i - 1]
            if (gradient > maxGradient) {
                maxGradient = gradient
                bestCol = i
            }
        }
        if (maxGradient < GRADIENT_THRESHOLD) {
            for (i in 0 until searchLimit.coerceAtMost(smoothed.size)) {
                if (smoothed[i] > LUMINANCE_FALLBACK_THRESHOLD) {
                    return i
                }
            }
            return 0
        }
        return bestCol
    }

    /**
     * Finds right edge: column with maximum bright→dark gradient in right 60%.
     */
    private fun findRightEdge(smoothed: FloatArray, width: Int): Int {
        val searchStart = (width * 0.4f).toInt()
        var maxGradient = 0f
        var bestCol = width
        for (i in (searchStart + 1 until smoothed.size).reversed()) {
            val gradient = smoothed[i - 1] - smoothed[i]
            if (gradient > maxGradient) {
                maxGradient = gradient
                bestCol = i
            }
        }
        if (maxGradient < GRADIENT_THRESHOLD) {
            for (i in (smoothed.size - 1) downTo searchStart) {
                if (smoothed[i] > LUMINANCE_FALLBACK_THRESHOLD) {
                    return i + 1
                }
            }
            return width
        }
        return bestCol
    }
}
