package lifeos.md.tapilot.experiment

import org.junit.Assert.*
import org.junit.Test

class PreviewCoordinatesTest {
    @Test fun mapsScaledDisplayCenterAndEdges() {
        assertEquals(240f to 400f, PreviewCoordinates.map(120f, 200f, 240, 400, 480, 800))
        assertEquals(0f to 0f, PreviewCoordinates.map(0f, 0f, 240, 400, 480, 800))
        assertNull(PreviewCoordinates.map(240f, 200f, 240, 400, 480, 800))
        assertNull(PreviewCoordinates.map(120f, 400f, 240, 400, 480, 800))
    }
    @Test fun excludesLetterboxingInBothDirections() {
        assertNull(PreviewCoordinates.map(10f, 200f, 400, 400, 480, 800))
        assertEquals(240f to 400f, PreviewCoordinates.map(200f, 200f, 400, 400, 480, 800))
        assertNull(PreviewCoordinates.map(120f, 20f, 240, 600, 480, 800))
        assertEquals(0f to 0f, PreviewCoordinates.map(0f, 100f, 240, 600, 480, 800))
    }
    @Test fun rejectsUnavailableDimensionsAndInvalidTouchCoordinates() {
        assertNull(PreviewCoordinates.map(1f, 1f, 0, 400, 480, 800))
        assertNull(PreviewCoordinates.map(Float.NaN, 1f, 240, 400, 480, 800))
        assertNull(PreviewCoordinates.map(1f, Float.POSITIVE_INFINITY, 240, 400, 480, 800))
        assertNull(PreviewCoordinates.map(-1f, 1f, 240, 400, 480, 800))
    }
}
