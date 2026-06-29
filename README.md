# HFM Lite- Android Native Application

## Overview

This is the native Android implementation of the Home Money financial tracking lite version application, designed to provide a comprehensive and modern solution for personal finance management. Built with **Kotlin** and **Jetpack Compose**, the app adheres to **Clean Architecture principles** to ensure maintainability, scalability, and testability.

## Features

### Core Functionality

*   **Expense Tracking**: Users can easily add, view, edit, and delete expense records, categorized across 21 predefined categories.
*   **AI-Powered Recognition**: Leverage the **SiliconFlow API** for intelligent expense recognition from images and text, streamlining the data entry process.
*   **Budget Management**: Set monthly spending limits with customizable warning thresholds and monitor real-time usage to stay within financial goals.
*   **Data Synchronization**: Enjoy seamless financial management with automatic background synchronization with a server and robust offline support through local caching.
*   **Search & Filtering**: Advanced search capabilities allow users to filter expenses by date range, expense type, amount range, and keywords.
*   **Multi-language Support**: The application offers full internationalization, supporting English, Simplified Chinese, Traditional Chinese.

### Enhanced Features

*   **Data Visualization**: Provides interactive charts and radar charts for insightful weekday spending analysis, helping users understand their financial habits.
*   **Excel Import/Export**: Allows for easy import of expense data from Excel files and export of data for backup or further analysis.
*   **Image Cropping**: Includes a built-in image cropping tool specifically designed to optimize images for AI expense recognition.
*   **Error Reporting**: Features automatic crash reporting and error logging to aid in debugging and improving application stability.
*   **Health Check Service**: Monitors server health to ensure continuous and reliable operation.

### Technical Highlights

*   **Encrypted Database**: Sensitive financial data is secured using **SQLCipher-encrypted local storage**.
*   **Material Design 3 Expressive UI**: The user interface follows Google's latest Material Design 3 guidelines, offering a modern and expressive user experience.
*   **Edge-to-Edge Display**: Provides an immersive full-screen experience by utilizing the entire display area.
*   **Customizable Theme**: Users can personalize the app's appearance with a color picker for theme customization.

## Technical Stack

*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose
*   **Architecture**: Clean Architecture
*   **Database Encryption**: SQLCipher
*   **API Integration**: SiliconFlow API (for AI recognition)
*   **Minimum SDK Version**: 24
*   **Compile SDK Version**: 37
*   **Target SDK Version**: 37

## Getting Started

### Prerequisites
- Android Studio 
- JDK 17 or later
- Android SDK37 (Android 17)
- Minimum SDK 24 (Android 7.0)

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

### 6. Language & Theme

#### Language Settings
- Go to Settings → Language
- Choose from English, Simplified Chinese, Traditional Chinese, Hong Kong, Macau variants
- UI updates immediately without restart
- Preference is saved and persists across app restarts

#### Theme Customization
- Go to Settings → Theme
- Use color picker to select custom accent colors
- Preview theme changes in real-time
- Save custom theme preferences

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
