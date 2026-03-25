# 🎸 Guitar Tuner (Ad-Free & Open Source)

A high-precision, lightweight guitar tuner built with **Jetpack Compose**. 

Most tuners on the Play Store are cluttered with intrusive ads and unnecessary subscriptions. 
This project was developed as a clean, reliable alternative for guitarists who want a straightforward tool that just works.

## 🚀 Why this Tuner?
- **Zero Ads:** No interruptions, no tracking, no bloatware.
- **Precision:** Real-time audio analysis with cent-deviation tracking.
- **User-Centric Permissions:** Handles microphone access gracefully. If you deny permission, it won't "break" or get stuck; it guides you clearly on how to enable it.
- **Modern UI:** Built entirely with Jetpack Compose and Material 3 for a fluid, native Android experience.

## 🛠 Technical Features
- **MVVM Architecture:** Separation of concerns between audio processing and UI.
- **Robust Permission Gate:** A custom implementation that handles "Don't ask again" scenarios and syncs with system settings in real-time.
- **Lifecycle Awareness:** Automatically starts and stops the microphone based on the app's lifecycle (`onResume`/`onPause`) to save battery and ensure privacy.
- **Version Catalog (TOML):** Modern Gradle dependency management.

## 📦 Installation
1. Clone this repository.
2. Open in Android Studio.
3. Build the APK and install it on your device.

## 🔧 Future Improvements
- [ ] Add haptic feedback for "Perfect Tune" (0 cents).
- [ ] Implement more custom tunings (Open G, DADGAD, etc.).
- [ ] Visual pulse animation for the permission screen.

---
**Author:** Milinko Miletić  
*Student at the School of Electrical Engineering (ETF), University of Belgrade.*
