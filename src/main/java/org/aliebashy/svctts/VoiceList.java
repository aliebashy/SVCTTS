package org.aliebashy.svctts;

import java.util.LinkedHashMap;
import java.util.Map;

public class VoiceList {
     // Key = name used in Piper
    // Value = label shown in GUI
    public static final Map<String, String> VOICES = new LinkedHashMap<>();

    static {
        VOICES.put("en_US-lessac-medium", "English (US) - Lessac (Female)");
        VOICES.put("en_US-libritts-medium", "English (US) - LibriTTS (Multi-speaker)");
        VOICES.put("en_GB-amy-medium", "English (UK) - Amy");
        VOICES.put("ja_JP-miou-medium", "Japanese - Miou");
        // add more here if wanted
    }

}

