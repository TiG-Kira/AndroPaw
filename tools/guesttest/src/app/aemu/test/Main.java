package app.aemu.test;

/**
 * Tester C: Android 9 and later (API 28+). On top of what every Android runs (Base): frames counted at the
 * display's pulse, the camera into a texture, script results from WebView.
 */
public class Main extends Base {
    String program() { return "C"; }
    String[] tests() { return new String[] { "device", "cpu", "disk", "net", "audio", "sensors", "canvas", "pattern", "ui", "glcheck", "gl1", "gl2", "gl3", "camera1", "camera", "webview", "oem", "fx", "g3d" }; }
    boolean run(String name) {
        if (name.equals("ui")) Api21.ui(this);
        else if (name.equals("camera")) bg(new Runnable() { public void run() { Api21.camera(Main.this); } });
        else if (name.equals("webview")) Api21.webview(this);
        else if (name.equals("oem")) bg(new Runnable() { public void run() { Oem.run(Main.this); } });
        else if (name.equals("fx")) Fx.start(this);
        else if (name.equals("g3d")) G3d.start(this);
        else return false;
        return true;
    }
}
