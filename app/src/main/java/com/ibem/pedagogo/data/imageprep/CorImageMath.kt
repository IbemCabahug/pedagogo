package com.ibem.pedagogo.data.imageprep

import kotlin.math.ceil
import kotlin.math.sqrt

// Pure image-math helpers for COR photo/screenshot preprocessing.
// Zero Android imports - the scratch/corprep JVM harness pins these, the same
// house pattern as data/parser (OCR is never perfect; the Confirm screen is
// the safety net, and this layer just makes OCR's job easier).

// Perceived luminance (Rec. 601) of one ARGB pixel: 0 (dark) .. 255 (light).
fun corLuma(argb: Int): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return (r * 299 + g * 587 + b * 114) / 1000
}

// Average luminance over sampled pixels. Empty input = assume light page.
fun corAverageLuminance(samples: IntArray): Int =
    if (samples.isEmpty()) 255
    else samples.fold(0) { acc, pixel -> acc + corLuma(pixel) } / samples.size

// Dark-mode screenshots (white text on near-black) get inverted before OCR.
fun corShouldInvert(avgLuma: Int): Boolean = avgLuma < 110

// Invert RGB channels, keep alpha untouched.
fun corInvertPixel(argb: Int): Int {
    val alpha = argb and 0xFF000000.toInt()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return alpha or ((255 - r) shl 16) or ((255 - g) shl 8) or (255 - b)
}

// Sampling-grid stride so any image is probed at ~1000 points, not millions.
fun corSampleStride(width: Int, height: Int): Int {
    val area = width.toLong() * height.toLong()
    if (area <= 1000) return 1
    return ceil(sqrt(area / 1000.0)).toInt().coerceAtLeast(1)
}

// Integer downscale factor (BitmapFactory.inSampleSize style) so the long
// side fits maxSide. Dense COR text needs pixels, so this only trims camera
// overkill (12MP+) and never below what OCR needs.
fun corSampleSize(longSide: Int, maxSide: Int): Int {
    var sample = 1
    while (longSide / (sample * 2) >= maxSide) sample *= 2
    return sample
}
