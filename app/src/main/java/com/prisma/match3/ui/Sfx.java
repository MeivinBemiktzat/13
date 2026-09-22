package com.prisma.match3.ui;

import android.media.AudioManager;
import android.media.ToneGenerator;

/**
 * Tiny, fully-offline sound layer built on the system ToneGenerator (no audio
 * asset files required). All calls are guarded so a device without tone support
 * simply stays silent. Sound can be muted via the preference.
 */
public class Sfx {
    private ToneGenerator tg;
    private boolean enabled = true;

    public Sfx() {
        try {
            tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (Throwable t) {
            tg = null;
        }
    }

    public void setEnabled(boolean e) { this.enabled = e; }

    private void tone(int type, int ms) {
        if (!enabled || tg == null) return;
        try { tg.startTone(type, ms); } catch (Throwable ignored) {}
    }

    public void click()   { tone(ToneGenerator.TONE_PROP_BEEP, 40); }
    public void match(int combo) {
        int[] steps = {
            ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_5,
            ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_D
        };
        tone(steps[Math.min(combo - 1, steps.length - 1)], 60);
    }
    public void special() { tone(ToneGenerator.TONE_PROP_BEEP2, 90); }
    public void win()     { tone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400); }
    public void lose()    { tone(ToneGenerator.TONE_SUP_ERROR, 300); }
    public void invalid() { tone(ToneGenerator.TONE_SUP_RADIO_NOTAVAIL, 80); }

    public void release() {
        if (tg != null) { try { tg.release(); } catch (Throwable ignored) {} tg = null; }
    }
}
