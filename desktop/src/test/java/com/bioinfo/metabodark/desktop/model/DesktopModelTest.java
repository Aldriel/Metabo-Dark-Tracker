package com.bioinfo.metabodark.desktop.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.desktop.prefs.UserPreferences;
import com.bioinfo.metabodark.example.SimulatedStudy;
import com.bioinfo.metabodark.model.Language;
import com.bioinfo.metabodark.service.MetaboService;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

class DesktopModelTest {

    @Test
    void theExampleIsAnnotatedAndTheWindowCanChange() {
        Preferences node = Preferences.userRoot().node("metabodark-model-" + UUID.randomUUID());
        UserPreferences preferences = new UserPreferences(node);
        DesktopModel model = new DesktopModel(new MetaboService(), preferences);
        assertEquals("", model.export());
        assertEquals(0, model.report("t").length);
        assertEquals("NO_FILE", model.setNumbers(0.02, 0.7, false));
        model.open(new byte[0], "empty.mzML", false);
        assertEquals("EMPTY", model.errorCode());
        assertFalse(model.simulated());
        model.open(SimulatedStudy.bytes(), null, false);
        assertEquals("", model.fileName());
        assertFalse(model.simulated());
        assertTrue(model.analysis().ms2() > 0);
        model.openExample();
        assertTrue(model.simulated());
        assertTrue(model.export().contains("scan"));
        assertTrue(model.report("Plasma").length > 4);
        assertEquals("TOLERANCE", model.setNumbers(5, 0.7, false));
        assertEquals(0.02, model.settings().mzToleranceDa());
        assertEquals("COSINE", model.setNumbers(0.02, -1, true));
        assertNull(model.setNumbers(0.05, 0.8, false));
        assertEquals(0.05, model.settings().mzToleranceDa());
        model.language(null);
        assertEquals(Language.EN, model.language());
        model.language(Language.FR);
        assertEquals(Language.FR, preferences.language());
        model.changeFont(-100);
        assertEquals(UserPreferences.MIN_FONT, model.fontSize());
        model.changeFont(100);
        assertEquals(UserPreferences.MAX_FONT, model.fontSize());
        model.resetFont();
        assertEquals(UserPreferences.DEFAULT_FONT, model.fontSize());
        model.fontSize(99);
        assertEquals(UserPreferences.MAX_FONT, preferences.fontSize());
        new UserPreferences().language();
        assertEquals(10, UserPreferences.clamp(1));
        assertEquals(24, UserPreferences.clamp(30));
    }
}
