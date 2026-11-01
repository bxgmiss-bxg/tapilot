package lifeos.md.tapilot.theme

import lifeos.md.tapilot.AppTheme
import org.junit.Assert.*
import org.junit.Test

class ThemeConfigurationTest {
    @Test fun oldOrUnknownPreferencesKeepVibeletDefaults() {
        listOf(null, "", "removed", "dark").forEach { assertEquals("minimal", AppTheme.parsePaletteId(it)) }
        assertEquals("minimal", AppTheme.parsePaletteId("minimal"))
        assertEquals("classic", AppTheme.parsePaletteId("classic"))
        listOf(null, "", "removed", "classic").forEach { assertEquals("system", AppTheme.parsePreference(it)) }
    }

    @Test fun explicitModeOverridesSystemAndSystemModeFollowsBothStates() {
        for (systemDark in listOf(false, true)) {
            assertTrue(AppTheme.resolveDark("dark", systemDark))
            assertFalse(AppTheme.resolveDark("light", systemDark))
            assertEquals(systemDark, AppTheme.resolveDark("system", systemDark))
            assertEquals(systemDark, AppTheme.resolveDark("invalid", systemDark))
        }
    }

    @Test fun bothPalettesHaveIndependentLightAndDarkColors() {
        assertEquals(0xFFF4EFE7.toInt(), ThemePalettes.get("classic", false).background)
        assertEquals(0xFF161C22.toInt(), ThemePalettes.get("classic", true).background)
        assertEquals(0xFFFFFFFF.toInt(), ThemePalettes.get("minimal", false).background)
        assertEquals(0xFF181818.toInt(), ThemePalettes.get("minimal", true).background)
        assertEquals(ThemePalettes.get("classic", true), ThemePalettes.get("removed", true))
    }

    @Test fun minimalBubblesKeepTheirOwnTextColorInsteadOfButtonContrast() {
        val light = ThemePalettes.get("minimal", false)
        assertEquals(0xFFEAF2ED.toInt(), light.bubbleStart)
        assertEquals(0xFF2F7A4B.toInt(), light.bubbleText)
        val dark = ThemePalettes.get("minimal", true)
        assertEquals(0xFF1F2821.toInt(), dark.bubbleStart)
        assertEquals(0xFF5BB575.toInt(), dark.bubbleText)
        assertEquals(0xFF181818.toInt(), dark.contrast)
        assertNotEquals(dark.bubbleText, dark.contrast)
    }
}
