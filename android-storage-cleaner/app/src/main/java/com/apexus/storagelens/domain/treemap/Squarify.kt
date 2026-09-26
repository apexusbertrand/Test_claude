package com.apexus.storagelens.domain.treemap

import kotlin.math.max
import kotlin.math.min

data class TreemapRect(val x: Float, val y: Float, val width: Float, val height: Float)

data class TreemapCell<T>(val item: T, val rect: TreemapRect)

/**
 * Algorithme « squarified » (Bruls, Huizing, van Wijk) : répartit des éléments pondérés
 * dans un rectangle en cherchant des cellules aussi carrées que possible.
 */
object Squarify {

    fun <T> layout(items: List<T>, weight: (T) -> Long, bounds: TreemapRect): List<TreemapCell<T>> {
        val positive = items.filter { weight(it) > 0 }.sortedByDescending(weight)
        if (positive.isEmpty() || bounds.width <= 0f || bounds.height <= 0f) return emptyList()
        val total = positive.sumOf { weight(it).toDouble() }
        val scale = bounds.width.toDouble() * bounds.height / total
        val areas = positive.map { weight(it) * scale }

        val result = ArrayList<TreemapCell<T>>(positive.size)
        var x = bounds.x.toDouble()
        var y = bounds.y.toDouble()
        var w = bounds.width.toDouble()
        var h = bounds.height.toDouble()
        var start = 0

        while (start < positive.size) {
            val side = min(w, h)
            var end = start + 1
            var rowArea = areas[start]
            var best = worst(areas, start, end, rowArea, side)
            while (end < positive.size) {
                val candidateArea = rowArea + areas[end]
                val candidate = worst(areas, start, end + 1, candidateArea, side)
                if (candidate > best) break
                best = candidate
                rowArea = candidateArea
                end++
            }

            val thickness = if (side > 0) rowArea / side else 0.0
            var offset = 0.0
            for (i in start until end) {
                val length = if (thickness > 0) areas[i] / thickness else 0.0
                val rect = if (w >= h) {
                    TreemapRect(x.toFloat(), (y + offset).toFloat(), thickness.toFloat(), length.toFloat())
                } else {
                    TreemapRect((x + offset).toFloat(), y.toFloat(), length.toFloat(), thickness.toFloat())
                }
                result += TreemapCell(positive[i], rect)
                offset += length
            }
            if (w >= h) { x += thickness; w -= thickness } else { y += thickness; h -= thickness }
            start = end
        }
        return result
    }

    /** Pire rapport d'aspect d'une rangée (plus il est proche de 1, mieux c'est). */
    private fun worst(areas: List<Double>, from: Int, to: Int, sum: Double, side: Double): Double {
        if (sum <= 0 || side <= 0) return Double.MAX_VALUE
        var maxArea = 0.0
        var minArea = Double.MAX_VALUE
        for (i in from until to) {
            maxArea = max(maxArea, areas[i])
            minArea = min(minArea, areas[i])
        }
        val s2 = side * side
        val sum2 = sum * sum
        return max(s2 * maxArea / sum2, sum2 / (s2 * minArea))
    }
}
