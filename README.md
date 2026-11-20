# Prompta

**Prompt saving app** — Telegram Bot + Private Channel as database.

## Features
- 📝 Rich markdown editor (22 formatting tools)
- 🔄 Auto-sync to Telegram private channel
- 🔍 Animated search bar
- 💾 Auto-save with 2s debounce
- 🎨 Customizable font family, size, line spacing
- ↩️ Custom Undo/Redo (up to 100 steps)
- 👁️ Markdown preview mode
- 📋 Line numbers panel
- 🌐 Works offline, syncs when connected

## Build

### Via GitHub Actions (recommended)
1. Push to GitHub
2. Actions tab → `Build Prompta APK` → Run workflow
3. Download APK from Artifacts

### Local Build
```bash
# Requirements: JDK 17, Android SDK (API 34)
chmod +x gradlew
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Setup in App
1. Create a Telegram Bot via [@BotFather](https://t.me/BotFather)
2. Create a private Telegram channel
3. Add your bot as Admin to the channel
4. Open **Prompta → Settings → Telegram**
5. Enter Bot Token + Channel ID → Save & Test

## Project Structure
```
com.prompta.tg/
├── MainActivity.java          # Edge-to-edge, fragment nav
├── PromptaApp.java            # Application class, WorkManager init
├── adapter/
│   └── PromptAdapter.java     # RecyclerView + DiffUtil
├── editor/
│   ├── AutoSaveHandler.java   # 2s debounce auto-save
│   ├── FormatHelper.java      # 22 markdown format operations
│   ├── LineNumberWatcher.java # Live line number panel
│   ├── MarkdownStyler.java    # Live syntax highlight spans
│   └── UndoRedoManager.java   # Custom undo/redo stacks
├── fragment/
│   ├── EditorFragment.java    # Full-screen editor
│   ├── HomeFragment.java      # Prompt list + animated search
│   └── SettingsFragment.java  # Font, Telegram, Sync settings
├── model/
│   └── Prompt.java            # Data model (UUID, title, content)
├── storage/
│   └── PromptRepository.java  # Gson + SharedPreferences local DB
├── sync/
│   ├── SyncManager.java       # WorkManager orchestrator
│   └── SyncWorker.java        # Background sync worker
└── telegram/
    ├── TelegramApi.java        # Retrofit interface
    ├── TelegramClient.java     # OkHttp + Retrofit builder
    ├── TelegramSyncService.java# Send/Edit/Delete/Test
    └── ...request/response models
```

## Stack
| Component | Library |
|-----------|---------|
| UI | Material Components, CardView, RecyclerView |
| Markdown | Markwon 4.6.2 |
| Network | Retrofit 2.9 + OkHttp 4.12 |
| Background | WorkManager 2.9 |
| Storage | Gson 2.10 + SharedPreferences |
| Build | Gradle 8.6 + JDK 17 |

## Specs
- **Package**: `com.prompta.tg`  
- **Min SDK**: 26 (Android 8.0)  
- **Target SDK**: 34 (Android 14)  
- **Language**: Java 17 + XML
