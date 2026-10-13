package com.bioinfo.metabodark.desktop.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bioinfo.metabodark.desktop.AppInfo;
import com.bioinfo.metabodark.model.Language;

import org.junit.jupiter.api.Test;

class MessagesTest {

    @Test
    void bothLanguagesShareTheirKeys() {
        Messages french = Messages.of(Language.FR);
        Messages english = Messages.of(null);
        assertEquals(Language.EN, english.language());
        assertEquals(french.keys(), english.keys());
        assertTrue(french.guide().contains("simulé"));
        assertTrue(english.guide().contains("simulated"));
        assertEquals("!missing!", english.get("missing"));
        assertEquals(english.get("help.title"), english.read("missing.txt"));
        assertTrue(AppInfo.version().contains("0.1.0"));
        assertEquals("contact@maximeethier.com", AppInfo.CONTACT_EMAIL);
    }
}
