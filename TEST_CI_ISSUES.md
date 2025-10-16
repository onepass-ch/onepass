# CI Test Failure Analysis & Solutions

## 🔴 Issues That Cause CreateEventFormComposeTest to Fail in CI

### 1. **Scrolling Issues (FIXED)** ✅
**Problem:**
- The `CreateEventForm` is a long, scrollable form
- CI emulators may have smaller screens than local devices
- Tests were asserting elements are displayed without scrolling to them first

**Failing Tests:**
- `createEventForm_displaysAllRequiredFields` - Fields below the fold not visible
- `createTicketButton_isDisplayed` - Button at bottom of form not visible
- `formPreservesStateOnRecomposition` - Couldn't verify state after scrolling

**Root Cause:**
```kotlin
// ❌ FAILS IN CI: Element might not be visible
composeTestRule.onNodeWithText("Tickets*").assertIsDisplayed()

// ✅ WORKS: Scrolls to element first
composeTestRule.onNodeWithText("Tickets*").performScrollTo().assertIsDisplayed()
```

**Solution Applied:**
- Added `performScrollTo()` before assertions for elements that might not be visible
- Tests now scroll to each element before interacting with it

---

## 🔴 Issues That Could Cause CreateEventFormFirestoreTest to Fail in CI

### 1. **Firebase Emulator Not Running** ⚠️ (CRITICAL)
**Problem:**
- Tests require Firebase Firestore emulator to be running
- Emulator must be accessible at `10.0.2.2:8080` (from Android emulator perspective)

**Solution:**
```yaml
# In CI workflow (e.g., GitHub Actions)
- name: Start Firebase Emulators
  run: |
    firebase emulators:start --only firestore,auth &
    sleep 5  # Wait for emulators to be ready
    
- name: Wait for Emulators
  run: |
    npx wait-on http://localhost:8080
```

**Detection:**
The `FirebaseEmulator` object checks if emulators are running:
```kotlin
val isRunning = areEmulatorsRunning()  // Checks http://10.0.2.2:4400/emulators
```

If this returns `false`, the tests will **fail** or connect to production Firestore (dangerous!).

---

### 2. **Network/Port Configuration Issues**
**Problem:**
- CI environment network isolation
- Port forwarding not configured
- Firewall blocking emulator ports

**Emulator Ports Required:**
```kotlin
const val HOST = "10.0.2.2"  // Android emulator's host machine
const val FIRESTORE_PORT = 8080
const val AUTH_PORT = 9099
const val EMULATORS_PORT = 4400
```

**CI Configuration Example:**
```yaml
# GitHub Actions
- name: Setup Firebase Emulators
  run: |
    npm install -g firebase-tools
    firebase emulators:start --only firestore,auth --project demo-test &
    
- name: Forward ports (if needed)
  run: |
    adb reverse tcp:8080 tcp:8080
    adb reverse tcp:9099 tcp:9099
    adb reverse tcp:4400 tcp:4400
```

---

### 3. **Timing Issues (FIXED)** ✅
**Problem:**
- ViewModel operations are asynchronous
- Tests were using virtual time (`runTest`) but real Firestore operations need real time
- Insufficient delays causing race conditions

**Root Cause:**
```kotlin
// ❌ FAILS: Virtual time doesn't wait for real Firestore
= runTest {
    viewModel.createEvent(userId, "Organizer")
    withTimeout(5000) { ... }  // Virtual time timeout
}

// ✅ WORKS: Real delays for real async operations
= runBlocking {
    viewModel.createEvent(userId, "Organizer")
    delay(2000)  // Real 2-second delay
}
```

**Solution Applied:**
- Changed from `runTest` to `runBlocking`
- Replaced `withTimeout` with simple `delay(2000)`
- Increased delays to 2000ms to handle slower CI environments

---

### 4. **Authentication State Issues**
**Problem:**
- Anonymous auth might fail or timeout
- User not properly authenticated before creating events

**Current Setup:**
```kotlin
@Before
override fun setUp() {
    super.setUp()
    runBlocking {
        FirebaseEmulator.auth.signInAnonymously().await()
        userId = FirebaseEmulator.auth.currentUser?.uid ?: "test-user"
    }
}
```

**Potential CI Issue:**
- If auth emulator is slow to respond
- Network issues preventing authentication

**Solution:**
Add retry logic and longer timeouts:
```kotlin
repeat(3) { attempt ->
    try {
        FirebaseEmulator.auth.signInAnonymously().await()
        break
    } catch (e: Exception) {
        if (attempt == 2) throw e
        delay(1000)
    }
}
```

---

### 5. **Test Isolation Issues**
**Problem:**
- Previous test data not cleaned up
- Firestore emulator state persists between tests

**Current Protection:**
```kotlin
@After
open fun tearDown() {
    runBlocking { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
}
```

**Potential Issue:**
If `clearTestCollection()` fails, next test might see old data.

**CI-Specific Risk:**
- Parallel test execution could interfere
- Emulator might not reset properly between test runs

**Solution:**
```yaml
# Reset emulator between test classes
- name: Run Tests
  run: |
    ./gradlew connectedDebugAndroidTest
    firebase emulators:exec --only firestore "sleep 1" # Force cleanup
```

---

### 6. **CI Environment Differences**
**Problem:**
- Different Android API levels
- Different emulator configurations  
- Resource constraints (CPU, memory)

**Current Tests Assume:**
- Android API 14 (emulator-5554 - 14)
- Sufficient memory for Firestore operations
- No strict timeouts

**CI Hardening:**
```kotlin
// Add longer delays for CI environments
val isCI = System.getenv("CI")?.toBoolean() ?: false
val delayTime = if (isCI) 3000L else 2000L
delay(delayTime)
```

---

## 📋 Complete CI Checklist

### Before Running Tests:
- [ ] Firebase emulators installed (`npm install -g firebase-tools`)
- [ ] Emulators running (`firebase emulators:start`)
- [ ] Ports forwarded to Android emulator (`adb reverse`)
- [ ] Emulator health check passes (check port 4400)

### CI Configuration Requirements:
```yaml
# Example GitHub Actions workflow
name: Android Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: macos-latest  # For hardware acceleration
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '11'
          
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Install Firebase Tools
        run: npm install -g firebase-tools
        
      - name: Start Firebase Emulators
        run: |
          firebase emulators:start --only firestore,auth --project demo-test &
          npx wait-on http://localhost:8080
          npx wait-on http://localhost:9099
          
      - name: Setup Android Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: google_apis
          arch: x86_64
          script: |
            adb reverse tcp:8080 tcp:8080
            adb reverse tcp:9099 tcp:9099
            adb reverse tcp:4400 tcp:4400
            ./gradlew connectedDebugAndroidTest
```

---

## 🛠️ Debugging CI Failures

### 1. Check Emulator Connection
```bash
# In CI, add this before tests:
adb shell ping -c 1 10.0.2.2  # Should succeed
curl http://localhost:4400/emulators  # Should return emulator status
```

### 2. Enable Verbose Logging
```kotlin
// Add to tests:
@Before
fun setUp() {
    Log.d("TEST", "Emulator running: ${FirebaseEmulator.isRunning}")
    Log.d("TEST", "Firestore host: ${Firebase.firestore.firestoreSettings.host}")
}
```

### 3. Screenshot on Failure
```yaml
- name: Upload Screenshots
  if: failure()
  uses: actions/upload-artifact@v3
  with:
    name: test-screenshots
    path: app/build/outputs/androidTest-results/
```

---

## ✅ Summary of Fixes Applied

1. ✅ **Scrolling fixed** - Added `performScrollTo()` to all Compose tests
2. ✅ **Timing fixed** - Changed to `runBlocking` with real delays
3. ✅ **Test robustness** - Increased delays for slower CI environments

## ⚠️ Still Required for CI

1. ⚠️ Firebase emulator must be running
2. ⚠️ Port forwarding must be configured
3. ⚠️ Sufficient timeout/delay for slow CI machines

---

## 📊 Expected Test Results

**When properly configured:**
- ✅ CreateEventFormComposeTest: 18/18 tests passing
- ✅ CreateEventFormFirestoreTest: 11/11 tests passing
- ✅ CreateEventFormViewModelTest: 27/27 tests passing

**Total: 56 tests covering CreateEventForm functionality**
