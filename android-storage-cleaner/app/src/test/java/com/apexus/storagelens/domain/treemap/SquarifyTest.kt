package com.apexus.storagelens.domain.treemap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SquarifyTest {
    private val bounds = TreemapRect(0f, 0f, 600f, 400f)

    @Test
    fun `areas are proportional to weights and fill the bounds`() {
        val weights = listOf(6L, 6L, 4L, 3L, 2L, 2L, 1L)
        val cells = Squarify.layout(weights.withIndex().toList(), { it.value }, bounds)
        assertEquals(weights.size, cells.size)
        val total = weights.sum().toFloat()
        cells.forEach { cell ->
            val expected = cell.item.value / total * 600f * 400f
            assertEquals(expected, cell.rect.width * cell.rect.height, expected * 0.001f)
            assertTrue(cell.rect.x >= -0.01f && cell.rect.y >= -0.01f)
            assertTrue(cell.rect.x + cell.rect.width <= 600.01f && cell.rect.y + cell.rect.height <= 400.01f)
        }
        assertEquals(600f * 400f, cells.sumOf { (it.rect.width * it.rect.height).toDouble() }.toFloat(), 1f)
    }

    @Test
    fun `ignores empty weights and empty bounds`() {
        assertEquals(1, Squarify.layout(listOf(0L, 5L), { it }, bounds).size)
        assertTrue(Squarify.layout(listOf(5L), { it }, TreemapRect(0f, 0f, 0f, 10f)).isEmpty())
    }
}
