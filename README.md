# Flip Face (Cat S22 External Screen Controller)

Flip Face is a premium, highly-customizable external screen launcher and media controller specifically tailored for the **Cat S22 Flip**. It transforms your tiny 1.44" external display into a dedicated, dynamic Walkman-style music player and a customizable lock screen.

## 🎵 Features

### The "Walkman" Media Player Engine
*   **Presence-Based Listening:** Runs a lightweight `NotificationListenerService` that only activates when a music app (Spotify, YouTube Music, etc.) is broadcasting a `MediaStyle` notification.
*   **Dynamic Album Art:** Stretches the currently playing high-resolution Album Art seamlessly across your external display.
*   **Algorithmic Palette Theming:** Uses the Android `Palette` API to analyze the album art pixels in real-time. It automatically colors the Song Title to match the art's "Light Vibrant" tones, and the Artist Name to match "Light Muted" tones, ensuring maximum contrast and beauty.
*   **Marquee Auto-Scroll:** Long song or podcast titles are never cut off. The text automatically scrolls horizontally (`ellipsize="marquee"`) so you can read the entire title.

### Universal Image & GIF Engine
*   **Custom Backgrounds:** When music is not playing, the app acts as a custom lock screen. You can select *any* `.gif`, `.jpg`, or `.png` file from your device to loop seamlessly on your external screen.
*   **Secure Caching:** Selected media is cached into the app's internal storage, so it won't break if you delete the original file from your gallery.

### Material Settings & D-Pad Routing
*   **Fluid Font Scaler:** The settings menu features a smooth slider that allows you to scale the clock and text sizes from `0.5x` to `2.0x`.
*   **Cat S22 Hardware Support:** Every interactive element in the app has been hard-coded with `nextFocus` properties, allowing you to flawlessly navigate the entire configuration menu using only the physical D-Pad on your Cat S22. No touch screen required!

## ⚙️ Installation & Usage

1.  **Download the APK:** Compile the project using `./gradlew assembleDebug` or download the pre-compiled APK.
2.  **Permissions:** Upon first launch, the app will request Notification Access. This is *required* for the Walkman player engine to detect when music is playing.
3.  **Customization:** Open the Flip Face Settings app from your launcher to pick custom GIFs, scale your fonts, and toggle notification pop-ups.

## 🐛 Known Bugs & Limitations

*   **Media Token Missing:** Some obscure or outdated music players do not broadcast a standard Android `MediaSession.Token`. If a token isn't provided, Flip Face cannot extract the album art. It will default to a fallback Walkman mode displaying just the text.
*   **Display Wakelocks:** Android's `Presentation` class is notorious for aggressive wakelocks. If you notice severe battery drain, try disabling custom looping GIFs, as the `AnimatedImageDrawable` keeps the screen rendering at a high framerate.
*   **Physical Keypad Trapping:** While D-Pad routing is mapped perfectly, pressing "Back" too many times inside the standard Android notification permission menu might bounce you to the homescreen instead of back into the app.

## 🛠️ Development

This project targets Android API 34 (Android 14) but maintains a `minSdk` of 30 to support the Cat S22 Flip's native Android 11 environment (Go Edition).

To build:
```bash
$env:JAVA_HOME = "C:\Path\To\JDK17"
./gradlew assembleDebug
```
