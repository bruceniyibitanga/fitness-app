package app.levitx.core.load

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class LoadCatalogueTest {
    @Test
    fun everyProfileHasAUniqueId() {
        val ids = LoadCatalogue.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun byIdFindsAProfileAndReturnsNullForUnknownIds() {
        assertSame(LoadCatalogue.pushUp, LoadCatalogue.byId("push_up"))
        assertNull(LoadCatalogue.byId("nope"))
    }

    @Test
    fun profileRejectsShareOutsideTheRange() {
        assertFailsWith<IllegalArgumentException> {
            LoadProfile("x", "X", bodyweightShare = 0.0, evidence = LoadEvidence.ESTIMATED)
        }
        assertFailsWith<IllegalArgumentException> {
            LoadProfile("x", "X", bodyweightShare = 1.1, evidence = LoadEvidence.ESTIMATED)
        }
    }

    @Test
    fun measuredProfileNeedsASource() {
        assertFailsWith<IllegalArgumentException> {
            LoadProfile("x", "X", bodyweightShare = 0.5, evidence = LoadEvidence.MEASURED)
        }
    }
}
