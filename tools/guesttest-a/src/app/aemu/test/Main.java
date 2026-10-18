package app.aemu.test;

/**
 * Tester A: Android 1.5 - 4.4 (API 3 - 19). 32-bit, nothing but the first APIs: Canvas, GLES 1, the first camera
 * API, AudioTrack, WebView. Frames are counted where they are drawn (no Choreographer before API 16).
 */
public class Main extends Base {
    String program() { return "A"; }
    String[] tests() { return new String[] { "device", "cpu", "disk", "net", "audio", "sensors", "canvas", "pattern", "ui", "glcheck", "gl1", "gl2", "camera1", "webview" }; }
    boolean run(String name) { return false; }
}
