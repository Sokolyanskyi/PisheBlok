# PisheBlok 🥘

**PisheBlok** is an Android app for tracking household supplies: where and how much food is stored, what is running low, what to buy, plus a dedicated section for household chemicals. Offline-first with real-time cloud sync between devices.

Languages: **English / Русский / Українська** (switchable in-app, auto-detected on first launch).

## Features

- **Food** — rooms → storage places (fridge, cupboard, drawer…) → nested sections (fridge compartment, freezer). Every item has quantity + unit, category, expiry date (with "expired" / "expires soon" badges) and a low-stock threshold.
- **Buy** — an automatic "Running low" section (triggered when quantity ≤ threshold) and a shopping list split into **Urgent** / **Later**. Any product or chemical can be added to the list with a 🛒 button straight from its card — including how many to buy. Marking an item as bought offers to restock it.
- **Chemicals** — home floors → rooms → storage places, with chemical-specific categories.
- **Barcode scanner** — point the camera at a barcode and the name is auto-filled from the free [Open Food Facts](https://world.openfoodfacts.org) database.
- **Quick-add suggestions** — previously used items appear as one-tap hints while typing.
- **Expiry reminders** — a daily notification summarising expired / expiring / running-low items.
- **Home-screen widget** — expired / running-low / to-buy counters at a glance.
- **Search** — finds current items and removed ones (showing where they used to be stored).
- Everything is editable and deletable (long-press a location card to edit or delete it with its contents).

## Screens

| | | |
|---|---|---|
| Room grid | Item list | Shopping list |

## Tech stack

- Kotlin 1.9 + Jetpack Compose (Material 3), single `:app` module, package `com.pisheblok.app`
- **Room** as the source of truth — full offline support. Every record carries `updatedAt`, `dirty` and `isDeleted` (tombstone) flags.
- **SyncManager** — pull of `users/{uid}/{locations|items|shopping}` from Cloud Firestore merged last-write-wins by `updatedAt`, then batched push of dirty records; a realtime snapshot listener applies changes from other devices instantly.
- **Firebase Auth** (email/password). Firebase is initialised manually from `res/values/firebase_config.xml` — no `google-services.json` or Gradle plugin required.
- **ML Kit** barcode scanning + **CameraX**; **WorkManager** daily reminders; **Glance** home-screen widget.
- Per-app language switching via `AppCompatDelegate.setApplicationLocales` (works on Android 8+).
- minSdk 26 (Android 8.0), targetSdk 34.

## Building

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (min Android 8.0 / API 26).

The bundled Firebase config (`app/src/main/res/values/firebase_config.xml`) contains client-side identifiers of the demo project — these keys are public by design (they identify the Firebase project, access is protected by Firestore security rules). To use your own backend, create a free Firebase project, enable Email/Password auth and Firestore, and replace the four values in that file. Security rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

With empty config values the app runs in **local-only mode** (no cloud, everything stays on the device).

Free Spark tier limits: 50k Firestore reads / 20k writes per day, 1 GiB storage — more than enough for a household.

## Project structure

```
app/src/main/java/com/pisheblok/app/
  data/model/    enums & UI models
  data/local/    Room: entities, DAOs, database (migrations)
  data/repo/     Repository — all mutations stamp dirty flags
  auth/          AuthManager (Firebase Auth)
  sync/          SyncManager (Firestore LWW replication), NetworkObserver
  scanner/       CameraX + ML Kit barcode scanner activity
  reminders/     daily WorkManager reminders
  widget/        Glance home-screen widget
  ui/theme/      palette, typography, shapes
  ui/components/ cards, badges, dialogs, pickers
  ui/locations/  location browser (food & chemicals), item form
  ui/shopping/   Buy tab (urgent / later)
  ui/auth/       sign in / sign up / password reset
  ui/nav/        root, tabs, account menu, language dialog
```

## License

MIT — see [LICENSE](LICENSE).
