package dev.vanguard.module.impl;

import dev.vanguard.module.Category;
import dev.vanguard.module.Module;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Shows the song Spotify is playing in a pill at the top centre of the screen (drawn by Hud). It
 * asks the operating system what is playing instead of talking to Spotify's web API, so there is
 * nothing to log in to. Linux: playerctl (or dbus-send as a fallback). Windows: Spotify's window
 * title. macOS: AppleScript. Checked in the background about every 1.5 seconds.
 */
public class SpotifyHud extends Module {
    private static volatile String artist = "";
    private static volatile String title = "";
    private static volatile boolean playing;
    private static volatile long positionMs = -1;
    private static volatile long lengthMs = -1;
    private static volatile long sampleAt;

    private volatile boolean running;
    private boolean warned;
    private Thread poller;

    public SpotifyHud() {
        super("SpotifyHud", Category.VISUALS, "Shows the song Spotify is playing at the top of the screen");
        setting("y", 4);          // distance from the top of the screen
        setting("showpaused", 0); // 1 = keep showing the song while it is paused
        setting("progress", 1);   // 1 = thin progress bar (where the system reports it)
        setting("maxwidth", 40);  // widest the pill may get, as a percent of the screen
    }

    public static String artist() { return artist; }
    public static String title() { return title; }
    public static boolean playing() { return playing; }
    public static long positionMs() { return positionMs; }
    public static long lengthMs() { return lengthMs; }
    public static long sampleAt() { return sampleAt; }

    @Override
    protected void onEnable() {
        running = true;
        warned = false;
        poller = new Thread(this::pollLoop, "vanguard-spotify");
        poller.setDaemon(true);
        poller.start();
    }

    @Override
    protected void onDisable() {
        running = false;
        if (poller != null) poller.interrupt();
        artist = "";
        title = "";
        playing = false;
    }

    private void pollLoop() {
        while (running) {
            try {
                poll();
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException ignored) {
                // keep polling
            }
        }
    }

    private void poll() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) pollWindows();
        else if (os.contains("mac")) pollMac();
        else pollLinux();
    }

    private void set(boolean isPlaying, String a, String t, long pos, long len) {
        artist = a;
        title = t;
        playing = isPlaying;
        positionMs = pos;
        lengthMs = len;
        sampleAt = System.currentTimeMillis();
    }

    private void clear() {
        title = "";
        artist = "";
        playing = false;
    }

    // ---- Linux ----

    private void pollLinux() {
        String out = run(1200, "playerctl", "-p", "spotify", "metadata", "--format",
            "{{status}}\u001f{{artist}}\u001f{{title}}\u001f{{position}}\u001f{{mpris:length}}");
        if (out != null && !out.isEmpty()) {
            String[] f = out.split("\u001f", -1);
            if (f.length >= 5) {
                set("Playing".equalsIgnoreCase(f[0]), f[1], f[2], micros(f[3]), micros(f[4]));
                return;
            }
        }
        // Fallback that ships with most Linux desktops
        String meta = run(1200, "dbus-send", "--print-reply", "--dest=org.mpris.MediaPlayer2.spotify",
            "/org/mpris/MediaPlayer2", "org.freedesktop.DBus.Properties.Get",
            "string:org.mpris.MediaPlayer2.Player", "string:Metadata");
        if (meta != null && !meta.isEmpty()) {
            String t = firstGroup(meta, "string \"xesam:title\"\\s+variant\\s+string \"(.*?)\"");
            String a = firstGroup(meta, "string \"xesam:artist\"\\s+variant\\s+array \\[\\s+string \"(.*?)\"");
            String status = run(1200, "dbus-send", "--print-reply", "--dest=org.mpris.MediaPlayer2.spotify",
                "/org/mpris/MediaPlayer2", "org.freedesktop.DBus.Properties.Get",
                "string:org.mpris.MediaPlayer2.Player", "string:PlaybackStatus");
            boolean isPlaying = status != null && status.contains("\"Playing\"");
            if (t != null) {
                set(isPlaying, a == null ? "" : a, t, -1, -1);
                return;
            }
        }
        clear();
        if (out == null && meta == null && !warned) {
            warned = true;
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal(
                        "[Vanguard] SpotifyHud found nothing playing. On Linux install playerctl (sudo pacman -S playerctl) "
                            + "and make sure Spotify is running."), false);
                }
            });
        }
    }

    // ---- Windows ----

    private void pollWindows() {
        String out = run(2500, "powershell", "-NoProfile", "-Command",
            "(Get-Process spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle } | Select-Object -First 1).MainWindowTitle");
        if (out == null || out.isEmpty() || out.startsWith("Spotify")) {
            // "Spotify", "Spotify Free" or "Spotify Premium" is the title while nothing is playing
            if (out != null && out.startsWith("Spotify")) {
                playing = false;
            } else {
                clear();
            }
            return;
        }
        int dash = out.indexOf(" - ");
        if (dash > 0) set(true, out.substring(0, dash), out.substring(dash + 3), -1, -1);
        else set(true, "", out, -1, -1);
    }

    // ---- macOS ----

    private void pollMac() {
        String out = run(1500, "osascript", "-e",
            "tell application \"Spotify\" to if player state is playing then (artist of current track) & \"|\" & (name of current track)");
        if (out == null || out.isEmpty() || !out.contains("|")) {
            clear();
            return;
        }
        int bar = out.indexOf('|');
        set(true, out.substring(0, bar), out.substring(bar + 1), -1, -1);
    }

    // ---- helpers ----

    private static long micros(String s) {
        try {
            return Long.parseLong(s.trim()) / 1000L;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String firstGroup(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /** Runs a fixed command and returns its output, or null if it is missing, fails or takes too long. */
    private static String run(long timeoutMs, String... command) {
        try {
            Process p = new ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            p.getOutputStream().close();
            if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return null;
            }
            if (p.exitValue() != 0) return null;
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
