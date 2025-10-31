# tubeFetch-Kotlin


## 🏗️ **Technology Stack & Architecture**

```
📱 Android App Architecture:
├── 🎯 Clean Architecture + MVVM
├── 🗃️ Room Database (for download history)
├── 🌐 Retrofit + OkHttp (for networking)
├── 💉 Hilt (Dependency Injection)
├── 🎨 Jetpack Compose (UI)
├── 🎵 MediaStore (for file storage)
├── 🔄 WorkManager (background downloads)
├── 📁 File System (actual file storage)
└── 🔐 BuildConfig (API keys management)
```

## 📁 **Project Structure**

Let me create the complete project structure:

### **1. Project Root Structure**
```
app/
├── src/main/
│   ├── java/dev/korryr/tubefetch/
│   │   ├── data/           # Data Layer
│   │   ├── domain/         # Domain Layer  
│   │   ├── ui/            # Presentation Layer
│   │   ├── di/            # Dependency Injection
│   │   └── utils/         # Utilities
│   └── res/
├── build.gradle.kts
├── proguard-rules.pro
└── keys.properties        # API keys (gitignored)
```

