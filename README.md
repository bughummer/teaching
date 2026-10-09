# Range Sense

An Android app for practicing core poker strategy concepts (position, ranges,
continuation betting, check-raising, three-betting, bet sizing, pot odds, and
balanced bluffing) through short, one-decision practice drills. All lesson
text is original — it does not reference or quote any specific course, book,
or instructor.

## Structure

- `engine/` — pure Kotlin module (no Android dependency): card/hand logic,
  pot-odds math, and all lesson + drill content. Has unit tests.
- `app/` — the Android app (Jetpack Compose): lesson list → lesson detail →
  short practice session, with a per-lesson best-score tracker stored locally
  on the device (`SharedPreferences`, no account/network needed).

## Building

Open the project root in Android Studio (Giraffe/Koala or newer) and let it
sync — it needs network access to Google's Maven repo for the Android Gradle
Plugin and Compose, which this build sandbox could not reach, so the `:app`
module has not been compiled yet in this environment. The `:engine` module
is plain Kotlin and has been built and tested here:

```
gradle :engine:test
```

From Android Studio: Run ▸ Run 'app' on an emulator or a connected device.

## Adding more lessons or drills

Edit `engine/src/main/kotlin/trainer/engine/content/Lessons.kt` and
`Scenarios.kt`. `ContentTest.kt` checks that every lesson has at least one
drill and that every drill's options are well-formed, so a broken addition
fails `gradle :engine:test` immediately.
