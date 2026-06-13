package com.spaceline.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.spaceline.common.ui.TextColorStyle;
import org.junit.jupiter.api.Test;

class TextColorStyleTest {

    @Test
    void solidIsConstant() {
        TextColorStyle s = TextColorStyle.solid(0xFFFF0000);
        assertEquals(0xFFFF0000, s.colorAt(0f, 0));
        assertEquals(0xFFFF0000, s.colorAt(1f, 1234));
    }

    @Test
    void gradientInterpolatesEnds() {
        TextColorStyle s = TextColorStyle.gradient(0xFF000000, 0xFFFFFFFF);
        assertEquals(0xFF000000, s.colorAt(0f, 0));
        assertEquals(0xFFFFFFFF, s.colorAt(1f, 0));
        int mid = s.colorAt(0.5f, 0);
        int g = (mid >> 8) & 0xFF;
        assertTrue(g > 100 && g < 160, "midpoint should be ~grey, was " + g);
    }

    @Test
    void chromeChangesOverTime() {
        TextColorStyle s = TextColorStyle.chrome();
        assertNotEquals(s.colorAt(0f, 0), s.colorAt(0f, 1500));
    }
}
