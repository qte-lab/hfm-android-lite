# Home Money - Android Native Application

## Overview

This is the native Android implementation of the Home Money financial tracking application. The app is built using modern Android development practices with Kotlin, Jetpack Compose, and follows Clean Architecture principles. It provides a comprehensive set of features for expense tracking, budget management, data synchronization, and more.

## Features

### Core Functionality
- **Expense Tracking**: Add, view, edit, and delete expense records with support for 21 expense categories
- **AI-Powered Recognition**: Intelligent expense recognition from images and text using SiliconFlow API
- **Budget Management**: Set monthly spending limits with warning thresholds and real-time usage tracking
- **Data Synchronization**: Automatic background sync with server, offline support with local caching
- **Search & Filtering**: Advanced filtering by date range, expense type, amount range, and keywords
- **Multi-language Support**: Full internationalization support for English, Simplified Chinese, Traditional Chinese, Hong Kong, Macau, and Singapore variants

### New & Enhanced Features
- **LAN Device Sync**: Peer-to-peer data synchronization between devices over local network using Bluetooth
- **Data Visualization**: Interactive charts and radar charts for weekday spending analysis
- **Membership Management**: User profile and membership features
- **Excel Import/Export**: Import expenses from Excel files and export data for backup
- **Image Cropping**: Built-in image cropping for AI expense recognition
- **Error Reporting**: Automatic crash reporting and error logging for debugging
- **Health Check Service**: Server health monitoring

### Technical Features
- **Encrypted Database**: SQLCipher-encrypted local storage for sensitive financial data
- **Material Design 3 Expressive**: Modern UI following Google's latest design guidelines with expressive components
- **Edge-to-Edge Display**: Immersive full-screen experience
- **Developer Mode**: Built-in database testing and debugging tools
- **Customizable Theme**: Color picker for personalizing the app appearance

## Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Compose UI + ViewModels)              │
├─────────────────────────────────────────┤
│         Domain Layer                    │
│  (Use Cases + Models + Repositories)    │
├─────────────────────────────────────────┤
│         Data Layer                      │
│  (Room DB + Retrofit + Mappers)         │
├─────────────────────────────────────────┤
│         Framework Layer                 │
│  (Android SDK + Third-party Libraries)  │
└─────────────────────────────────────────┘
```

### Key Components

#### Data Layer
- **Room Database**: Encrypted local storage with SQLCipher
- **Retrofit**: RESTful API client for server communication
- **Repository Pattern**: Abstraction layer for data sources
- **Data Mappers**: Convert between Entity, Domain, and DTO models
- **Excel Integration**: fastExcel for import/export functionality

#### Domain Layer
- **Use Cases**: Business logic encapsulation
- **Domain Models**: Pure Kotlin data classes
- **Repository Interfaces**: Contracts for data operations
- **Sync Managers**: LAN and server synchronization

#### Presentation Layer
- **Jetpack Compose**: Modern declarative UI framework
- **ViewModels**: UI state management with Kotlin Flow
- **Navigation Component**: Type-safe navigation between screens
- **Material 3 Expressive**: Enhanced UI components

## Tech Stack

### Core Technologies
- **Language**: Kotlin 2.3.20
- **UI Framework**: Jetpack Compose (BOM 2026.03.01)
- **Dependency Injection**: Hilt 2.59.2
- **Database**: Room 2.8.4 with SQLCipher 4.14.0
- **Networking**: Retrofit 3.0.0 + OkHttp 5.3.2
- **Async**: Kotlin Coroutines 1.10.2 + Flow

### Key Libraries
- **Material Design 3**: Modern UI components (1.5.0-alpha16)
- **Material Expressive**: Enhanced Material components (1.14.0-alpha10)
- **Navigation Compose**: Type-safe navigation (2.9.7)
- **Paging 3**: Efficient data loading (3.4.2)
- **WorkManager**: Background task scheduling (2.11.2)
- **Coil**: Image loading and caching (2.7.0)
- **Gson**: JSON serialization
- **DataStore**: Preferences storage (1.2.1)
- **fastExcel**: Excel file handling (0.20.0)
- **uCrop**: Image cropping (2.2.11)
- **m3color**: Material 3 color utilities (2025.4)

### Security
- **SQLCipher**: Database encryption
- **EncryptedSharedPreferences**: Secure key storage
- **Android Keystore**: Hardware-backed key management
- **Error Reporting**: Secure error logging

## Project Structure

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/chronie/homemoney/
│   │   │   │   ├── core/              # Core utilities
│   │   │   │   │   ├── common/        # Common utilities (Language, DeveloperMode)
│   │   │   │   │   ├── error/         # Error reporting system
│   │   │   │   │   └── network/       # Network monitoring
│   │   │   │   ├── data/              # Data layer
│   │   │   │   │   ├── local/         # Room database
│   │   │   │   │   │   ├── dao/       # Data access objects
│   │   │   │   │   │   └── entity/    # Database entities
│   │   │   │   │   ├── remote/        # API layer
│   │   │   │   │   │   ├── api/       # Retrofit interfaces
│   │   │   │   │   │   ├── dto/       # Data transfer objects
│   │   │   │   │   │   └── interceptor/ # HTTP interceptors
│   │   │   │   │   ├── repository/    # Repository implementations
│   │   │   │   │   ├── mapper/        # Data mappers
│   │   │   │   │   └── sync/          # Sync management (LAN + Server)
│   │   │   │   ├── di/                # Dependency injection
│   │   │   │   ├── domain/            # Domain layer
│   │   │   │   │   ├── model/         # Domain models
│   │   │   │   │   ├── repository/    # Repository interfaces
│   │   │   │   │   ├── usecase/       # Use cases
│   │   │   │   │   └── sync/          # Sync interfaces
│   │   │   │   ├── service/           # Background services
│   │   │   │   ├── ui/                # Presentation layer
│   │   │   │   │   ├── budget/        # Budget management
│   │   │   │   │   ├── charts/        # Data visualization
│   │   │   │   │   ├── components/    # Reusable UI components
│   │   │   │   │   ├── expense/       # Expense tracking
│   │   │   │   │   ├── main/          # Main screen
│   │   │   │   │   ├── membership/    # Membership features
│   │   │   │   │   ├── settings/      # Settings
│   │   │   │   │   ├── sync/          # LAN sync screen
│   │   │   │   │   ├── test/          # Testing screens
│   │   │   │   │   ├── theme/         # Material theme
│   │   │   │   │   └── welcome/       # Welcome screen
│   │   │   │   ├── worker/            # Background workers
│   │   │   │   └── MainActivity.kt    # Main activity
│   │   │   ├── res/                   # Resources
│   │   │   │   ├── values/            # English strings
│   │   │   │   ├── values-zh/         # Simplified Chinese
│   │   │   │   ├── values-zh-rHK/     # Hong Kong Chinese
│   │   │   │   ├── values-zh-rMO/     # Macau Chinese
│   │   │   │   ├── values-zh-rSG/     # Singapore Chinese
│   │   │   │   └── values-zh-rTW/     # Traditional Chinese
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/               # Instrumented tests
│   └── build.gradle                   # App build config
├── gradle/                            # Gradle wrapper
├── build.gradle                       # Project build config
├── settings.gradle                    # Project settings
├── variables.gradle                   # Version variables
└── README.md                          # This file
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK36 (Android 16)
- Minimum SDK 26 (Android 8.0)

### Building the Project

#### Using Android Studio
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `android` directory
4. Wait for Gradle sync to complete
5. Click "Run" or press Shift+F10

#### Using Command Line
```bash
cd android

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

#### Using Batch Scripts (Windows)
```bash
# Clean and build
clean-build.bat

# Build APK
build-apk.bat

# Force build (stops Gradle daemon first)
force-build.bat
```

### APK Location
After building, the APK can be found at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

### Server Connection
Update the base URL in `NetworkModule.kt`:
```kotlin
private const val BASE_URL = "http://YOUR_SERVER_IP:3010/"
```

### API Keys
Configure API keys in the Settings screen:
- **SiliconFlow API Key**: Required for AI expense recognition feature

### Versioning
The app uses dynamic versioning based on build date and time:
- `versionCode`: Timestamp-based unique identifier
- `versionName`: Format `1.YYYYMMDD.HHMM`

## Features Guide

### 1. Expense Management

#### Adding Expenses
- Tap the "+" button on the expense list screen
- Fill in expense details (type, amount, date, notes)
- Save to local database and sync queue

#### AI Recognition
- Tap the AI icon in the add expense screen
- Select images or enter text description
- Crop images if needed
- Review and edit recognized expenses
- Save all records at once

#### Viewing Expenses
- Scroll through the expense list
- View statistics card showing total, average, and median
- Pull to refresh for latest data
- Automatic pagination for large datasets

#### Filtering & Search
- Tap the filter icon in the toolbar
- Set date range, expense types, amount range
- Enter keywords to search notes
- Apply filters to narrow down results

### 2. Budget Management

#### Setting Budget
- Go to Settings → Budget Management
- Enable budget tracking
- Set monthly limit and warning threshold (default 80%)
- Save settings

#### Monitoring Budget
- View budget card on expense list screen
- See current spending, remaining amount, and percentage
- Color-coded status indicators:
  - Green: Normal (below warning threshold)
  - Yellow: Warning (above threshold)
  - Red: Over budget

### 3. Data Visualization

#### Charts Screen
- View weekly spending trends
- Weekday radar chart for spending pattern analysis
- Tap on weekdays to see detailed breakdown
- Filter by date range for specific periods

### 4. Data Synchronization

#### Server Sync
- Background sync runs every hour
- Syncs when network becomes available
- Uploads local changes to server
- Downloads server updates

#### LAN Device Sync
- Go to Settings → LAN Sync
- Enable Bluetooth and location permissions
- Discover nearby devices
- Pair and sync data directly without server
- Conflict resolution based on timestamps

#### Manual Sync
- Go to Settings → Data Sync
- Tap "Sync Now" button
- View sync status and last sync time
- See pending items count

#### Conflict Resolution
- Automatic resolution based on timestamps
- Newer version always wins
- Conflicts are logged for review

### 5. Data Import/Export

#### Export Expenses
- Go to Settings → Import/Export
- Select "Export to Excel"
- Choose date range
- Save Excel file to device storage

#### Import Expenses
- Go to Settings → Import/Export
- Select "Import from Excel"
- Choose Excel file from device
- Review and confirm imported data
- Save to database

### 6. Membership Management

#### User Profile
- View and edit user profile
- Upload avatar image
- Update personal information

#### Login/Logout
- Secure login with server
- Automatic token refresh
- Persistent login state

### 7. Language & Theme

#### Language Settings
- Go to Settings → Language
- Choose from English, Simplified Chinese, Traditional Chinese, Hong Kong, Macau, or Singapore variants
- UI updates immediately without restart
- Preference is saved and persists across app restarts

#### Theme Customization
- Go to Settings → Theme
- Use color picker to select custom accent colors
- Preview theme changes in real-time
- Save custom theme preferences

### 8. Developer Mode
- Go to Settings → Developer Options
- Enable Developer Mode
- Access database testing screen from main menu
- Add test data, view records, and clear database
- View error logs and crash reports

## Database Schema

### Expenses Table
```sql
CREATE TABLE expenses (
    id INTEGER PRIMARY KEY,
    type TEXT NOT NULL,
    remark TEXT,
    amount REAL NOT NULL,
    time INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER NOT NULL DEFAULT 0,
    server_id TEXT
)
```

### Budgets Table
```sql
CREATE TABLE budgets (
    id INTEGER PRIMARY KEY,
    monthly_limit REAL NOT NULL,
    warning_threshold REAL NOT NULL DEFAULT 0.8,
    is_enabled INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
)
```

### Members Table
```sql
CREATE TABLE members (
    id INTEGER PRIMARY KEY,
    name TEXT,
    email TEXT,
    avatar TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER NOT NULL DEFAULT 0,
    server_id TEXT
)
```

### Sync Queue Table
```sql
CREATE TABLE sync_queue (
    id INTEGER PRIMARY KEY,
    entity_type TEXT NOT NULL,
    entity_id INTEGER NOT NULL,
    operation TEXT NOT NULL,
    data TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
)
```

## API Integration

### Expense API
- `GET /api/expenses` - List expenses with pagination and filters
- `POST /api/expenses` - Create new expense
- `PUT /api/expenses/:id` - Update expense
- `DELETE /api/expenses/:id` - Delete expense
- `GET /api/expenses/statistics` - Get expense statistics
- `POST /api/expenses/export` - Export expenses to Excel
- `POST /api/expenses/import` - Import expenses from Excel

### AI Recognition API
- `POST /api/ai/parse` - Parse text or images to extract expense records
- Uses SiliconFlow API with Qwen models
- Supports multiple images in a single request

### Member API
- `GET /api/members/current` - Get current user info
- `POST /api/members` - Create or update member
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout

### Health Check API
- `GET /api/health` - Check server health status

### Error Report API
- `POST /api/error-report` - Submit error reports
- `GET /api/error-report/logs` - Get error logs

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing
1. Enable Developer Mode in Settings
2. Access Database Test screen
3. Add test data and verify operations
4. Check sync functionality (server and LAN)
5. Test offline mode by disabling network
6. Test Excel import/export
7. Test LAN device sync between multiple devices

## Troubleshooting

### Build Issues

#### Gradle Sync Failed
- Check internet connection
- Invalidate caches: File → Invalidate Caches / Restart
- Delete `.gradle` folder and sync again

#### R.jar File Locked
- Stop all Gradle daemons: `./gradlew --stop`
- Close Android Studio
- Delete `app/build` directory
- Restart and rebuild

### Runtime Issues

#### App Crashes on Startup
- Check Logcat for error messages
- Verify database migrations are correct
- Clear app data and reinstall
- Check error reports in Developer Mode

#### Sync Not Working
- Check network connection
- Verify server is running and accessible
- Check API key configuration
- Review sync logs in Settings
- For LAN sync: Ensure Bluetooth is enabled and devices are paired

#### Language Not Changing
- Ensure language is saved in Settings
- Check that string resources exist for all languages

#### Import/Export Issues
- Verify storage permissions are granted
- Check Excel file format is correct
- Ensure file path is accessible

## Performance Optimization

### Database
- Indexes on frequently queried columns (time, type, is_synced)
- Pagination for large datasets
- Efficient queries using Room's compile-time verification

### Network
- Request/response caching with OkHttp
- Automatic retry with exponential backoff
- Connection pooling for better performance

### UI
- LazyColumn for efficient list rendering
- Image loading with Coil's memory and disk caching
- Debounced search input to reduce queries

## Security Considerations

### Data Protection
- SQLCipher encryption for local database
- Encrypted SharedPreferences for sensitive data
- HTTPS for all network communication
- No sensitive data in logs (production builds)
- Secure error reporting with anonymized data

### Authentication
- JWT token-based authentication
- Automatic token refresh
- Secure token storage in EncryptedSharedPreferences

### Permissions
- Camera: For AI expense recognition
- Bluetooth: For LAN device sync
- Location: For Bluetooth scanning (Android 6.0+)
- Storage: For Excel import/export
- Images: For reading media files (Android 13+)

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use ktlint for code formatting
- Write meaningful commit messages
- Add comments for complex logic (in English)

### Pull Request Process
1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Update documentation
5. Submit pull request with description

#### Known Issues
- None currently reported

## License

This project is part of the Home Money application. See the main project README for license information.

## Contact & Support

For issues, questions, or contributions, please refer to the main project repository.

## Acknowledgments

- Built with Jetpack Compose and Material Design 3 Expressive
- Uses SiliconFlow API for AI features
- Inspired by modern Android development best practices
- Uses fastExcel for Excel handling
- Uses uCrop for image cropping
