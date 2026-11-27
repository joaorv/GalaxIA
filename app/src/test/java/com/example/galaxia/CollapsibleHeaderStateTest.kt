package com.example.galaxia

import com.example.galaxia.ui.components.CollapsibleHeaderState
import org.junit.Assert.assertEquals
import org.junit.Test

class CollapsibleHeaderStateTest {

    @Test
    fun computeNextOffset_clampsBetweenNegativeHeightAndZero() {
        val height = 120f

        assertEquals(-40f, CollapsibleHeaderState.computeNextOffset(0f, -40f, height), 0.01f)
        assertEquals(-120f, CollapsibleHeaderState.computeNextOffset(-90f, -80f, height), 0.01f)
        assertEquals(0f, CollapsibleHeaderState.computeNextOffset(-30f, 60f, height), 0.01f)
        assertEquals(0f, CollapsibleHeaderState.computeNextOffset(0f, -20f, 0f), 0.01f)
    }

    @Test
    fun computeSnapTarget_expandsWhenAtTopOrMoreThanHalfVisible() {
        val height = 100f

        // No topo da lista sempre expande
        assertEquals(0f, CollapsibleHeaderState.computeSnapTarget(-90f, height, isAtTop = true), 0.01f)

        // Mais da metade visível (-40 > -50) -> expande para 0
        assertEquals(0f, CollapsibleHeaderState.computeSnapTarget(-40f, height, isAtTop = false), 0.01f)

        // Mais da metade oculta (-60 <= -50) -> recolhe para -100
        assertEquals(-100f, CollapsibleHeaderState.computeSnapTarget(-60f, height, isAtTop = false), 0.01f)
    }
}
