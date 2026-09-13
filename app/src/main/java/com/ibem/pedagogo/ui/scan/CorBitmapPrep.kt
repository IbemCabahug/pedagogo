package com.ibem.pedagogo.ui.scan

import android.graphics.Bitmap
import com.ibem.pedagogo.data.imageprep.corAverageLuminance
import com.ibem.pedagogo.data.imageprep.corInvertPixel
import com.ibem.pedagogo.data.imageprep.corSampleSize
import com.ibem.pedagogo.data.imageprep.corSampleStride
import com.ibem.pedagogo.data.imageprep.corShouldInvert

// Bitmap-side wrapper around the pure math in data/imageprep: darkness
// detection -> optional inversion (dark-mode screenshots) -> conservative
// downscale. Runs on Dispatchers.Default from the ViewModel - never on the
// UI thread.
fun prepareForOcr(src: Bitmap, maxSide: Int = 2400): Bitmap {
    val stride = corSampleStride(src.width, src.height)
    val samples = ArrayList<Int>(1024)
    var y = 0
    while (y < src.height) {
        var x = 0
        while (x < src.width) {
            samples += src.getPixel(x, y)
            x += stride
        }
        y += stride
    }
    var work = src
    if (corShouldInvert(corAverageLuminance(samples.toIntArray()))) {
        val inverted = work.copy(Bitmap.Config.ARGB_8888, false)
        val pixels = IntArray(inverted.width * inverted.height)
        inverted.getPixels(pixels, 0, inverted.width, 0, 0, inverted.width, inverted.height)
        for (i in pixels.indices) pixels[i] = corInvertPixel(pixels[i])
        inverted.setPixels(pixels, 0, inverted.width, 0, 0, inverted.width, inverted.height)
        work = inverted
    }
    val sample = corSampleSize(maxOf(work.width, work.height), maxSide)
    if (sample <= 1) return work
    return Bitmap.createScaledBitmap(
        work,
        (work.width / sample).coerceAtLeast(1),
        (work.height / sample).coerceAtLeast(1),
        true
    )
}
