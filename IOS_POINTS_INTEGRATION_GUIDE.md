# iOS Points Integration Guide
**Backend Implementation Complete - Ready for iOS Integration**

## Overview

The backend now supports a complete points-based gamification system. Runners earn points by logging activities and can spend points on premium chat features. This guide provides everything needed to integrate the iOS app with the new points APIs.

---

## API Endpoints Reference

### Base URL
```
https://your-backend-url.com/api/v1/points
```

### 1. Get Point Balance
**Endpoint:** `GET /api/v1/points/balance`

**Parameters:**
- `runnerId` (required): Integer - The runner's ID

**Response:**
```json
{
  "currentPoints": 125,
  "lifetimePointsEarned": 450,
  "lastUpdated": "2025-11-30T10:30:00Z"
}
```

**When to Call:**
- On app launch
- After logging any activity
- Before showing point-dependent features
- On profile screen load

---

### 2. Validate Sufficient Points
**Endpoint:** `POST /api/v1/points/validate`

**Request Body:**
```json
{
  "runnerId": 1,
  "featureType": "SEND_GIF"
}
```

**Response:**
```json
{
  "hasEnoughPoints": true,
  "currentBalance": 125,
  "pointsRequired": 5,
  "shortfall": 0
}
```

**Feature Types:**
- `SEND_GIF` - Cost: 5 points
- `PIN_MESSAGE` - Cost: 10 points
- `MESSAGE_ANIMATION` - Cost: 3 points
- `CHANGE_CHAT_NAME` - Cost: 50 points
- `CUSTOM_AVATAR` - Cost: 100 points

**When to Call:**
- Before showing GIF picker UI
- Before enabling pin message button
- Before showing premium feature options
- To display "Insufficient Points" state

---

### 3. Spend Points
**Endpoint:** `POST /api/v1/points/spend`

**Request Body:**
```json
{
  "runnerId": 1,
  "featureType": "SEND_GIF",
  "description": "Sent celebration GIF in team chat"
}
```

**Success Response:**
```json
{
  "success": true,
  "newBalance": 120,
  "pointsSpent": 5,
  "transaction": {
    "id": 12345,
    "runnerId": 1,
    "transactionType": "SPENT",
    "pointsAmount": -5,
    "featureType": "SEND_GIF",
    "description": "Sent celebration GIF in team chat",
    "balanceAfter": 120,
    "createdAt": "2025-11-30T10:30:00Z"
  }
}
```

**Error Response:**
```json
{
  "success": false,
  "newBalance": 3,
  "pointsSpent": 0,
  "errorMessage": "Insufficient points. Need 5, have 3"
}
```

**When to Call:**
- BEFORE performing the premium action
- Store the transaction ID for potential refund

**Critical Flow:**
```
1. User taps premium feature
2. Call /validate to check points
3. If valid, call /spend to deduct points
4. If spend succeeds, perform the action (send GIF, pin message, etc.)
5. If action fails, call /refund with transaction ID
```

---

### 4. Refund Points
**Endpoint:** `POST /api/v1/points/refund`

**Request Body:**
```json
{
  "transactionId": 12345
}
```

**Success Response:**
```json
{
  "success": true,
  "newBalance": 125,
  "pointsRefunded": 5
}
```

**Error Response:**
```json
{
  "success": false,
  "newBalance": 120,
  "pointsRefunded": 0,
  "errorMessage": "Transaction already refunded"
}
```

**When to Call:**
- When premium action fails after points were spent
- Example: GIF send failed, message pin failed, etc.

---

### 5. Get Transaction History
**Endpoint:** `GET /api/v1/points/transactions`

**Parameters:**
- `runnerId` (required): Integer
- `limit` (optional): Integer (default: 50, 0 = all)

**Response:**
```json
[
  {
    "id": 12345,
    "runnerId": 1,
    "transactionType": "EARNED",
    "pointsAmount": 15,
    "activityType": "WORKOUT",
    "activityUuid": "abc-123",
    "description": "Logged workout results",
    "balanceAfter": 125,
    "createdAt": "2025-11-30T10:30:00Z",
    "season": "2025"
  },
  {
    "id": 12344,
    "transactionType": "SPENT",
    "pointsAmount": -5,
    "featureType": "SEND_GIF",
    "description": "Sent celebration GIF",
    "balanceAfter": 110,
    "createdAt": "2025-11-30T09:15:00Z"
  }
]
```

**When to Call:**
- On "Points History" screen
- To show recent activity on profile

---

### 6. Get Points Configuration
**Endpoint:** `GET /api/v1/points/config`

**Response:**
```json
{
  "config": {
    "EARN_TRAINING_RUN": 10,
    "EARN_WORKOUT": 15,
    "EARN_CROSS_TRAINING": 8,
    "EARN_MEET_LOG": 10,
    "EARN_PRE_MEET_LOG": 5,
    "COST_SEND_GIF": 5,
    "COST_PIN_MESSAGE": 10,
    "COST_MESSAGE_ANIMATION": 3,
    "COST_CHANGE_CHAT_NAME": 100,
    "COST_CUSTOM_AVATAR": 200
  }
}
```

**When to Call:**
- On app launch
- Cache locally
- Refresh every 5-10 minutes or on app foreground

---

### 7. Get Leaderboard
**Endpoint:** `GET /api/v1/points/leaderboard`

**Parameters:**
- `limit` (optional): Integer (default: 100)

**Response:**
```json
[
  {
    "id": 5,
    "name": "John Doe",
    "points": 450,
    "lifetimePointsEarned": 890,
    "lastPointsUpdated": "2025-11-30T10:30:00Z",
    "graduatingClass": "2026",
    "team": "Varsity"
  },
  {
    "id": 12,
    "name": "Jane Smith",
    "points": 380,
    "lifetimePointsEarned": 720,
    "lastPointsUpdated": "2025-11-30T09:00:00Z",
    "graduatingClass": "2025",
    "team": "Varsity"
  }
]
```

**When to Call:**
- On leaderboard screen load
- Refresh on pull-to-refresh

---

## Automatic Points Earning

Points are **automatically awarded** when activities are logged via existing endpoints. No changes needed to activity logging - points are added in the backend.

### Activity → Points Mapping

| Activity Type | Endpoint | Points Earned |
|--------------|----------|---------------|
| Training Run | `POST /xc/runners-training-run/create` | 10 points |
| Workout | `PUT /xc/workout/runner-result/put` | 15 points |
| Cross Training | `POST /cross-training-record/create` | 8 points |
| Meet Log | `POST /xc/meet/log/create` | 20 points |
| Pre-Meet Log | `POST /xc/meet/log/pre-meet/create` | 5 points |

### Important Notes:
- **Duplicate Prevention:** Same activity UUID will never award points twice
- **No Updates:** Updating existing activities does NOT award additional points
- **Automatic:** No iOS code changes needed - backend handles it automatically

---

## iOS Implementation Checklist

### Phase 1: Data Layer

#### 1.1 Create Swift Models
```swift
// PointBalance.swift
struct PointBalance: Codable {
    let currentPoints: Int
    let lifetimePointsEarned: Int
    let lastUpdated: Date?
}

// PointsValidation.swift
struct PointsValidation: Codable {
    let hasEnoughPoints: Bool
    let currentBalance: Int
    let pointsRequired: Int
    let shortfall: Int
}

// SpendPointsResponse.swift
struct SpendPointsResponse: Codable {
    let success: Bool
    let newBalance: Int
    let pointsSpent: Int
    let transaction: PointTransaction?
    let errorMessage: String?
}

// PointTransaction.swift
struct PointTransaction: Codable {
    let id: Int64
    let runnerId: Int
    let transactionType: String // "EARNED", "SPENT", "REFUND"
    let pointsAmount: Int
    let activityType: String?
    let featureType: String?
    let activityUuid: String?
    let description: String?
    let balanceAfter: Int
    let createdAt: Date
    let season: String?
}

// PointsConfiguration.swift
struct PointsConfiguration: Codable {
    let config: [String: Int]
}

enum FeatureType: String {
    case sendGif = "SEND_GIF"
    case pinMessage = "PIN_MESSAGE"
    case messageAnimation = "MESSAGE_ANIMATION"
    case changeChatName = "CHANGE_CHAT_NAME"
    case customAvatar = "CUSTOM_AVATAR"

    var displayName: String {
        switch self {
        case .sendGif: return "Send GIF"
        case .pinMessage: return "Pin Message"
        case .messageAnimation: return "Message Animation"
        case .changeChatName: return "Change Chat Name"
        case .customAvatar: return "Custom Avatar"
        }
    }
}
```

#### 1.2 Create API Service
```swift
// PointsService.swift
class PointsService {
    static let shared = PointsService()
    private let baseURL = "https://your-api.com/api/v1/points"

    // Cached configuration
    private var cachedConfig: PointsConfiguration?
    private var lastConfigFetch: Date?

    func getBalance(runnerId: Int) async throws -> PointBalance
    func validatePoints(runnerId: Int, featureType: FeatureType) async throws -> PointsValidation
    func spendPoints(runnerId: Int, featureType: FeatureType, description: String) async throws -> SpendPointsResponse
    func refundPoints(transactionId: Int64) async throws -> RefundPointsResponse
    func getTransactionHistory(runnerId: Int, limit: Int = 50) async throws -> [PointTransaction]
    func getConfiguration() async throws -> PointsConfiguration
    func getLeaderboard(limit: Int = 100) async throws -> [Runner]
}
```

#### 1.3 Create PointsManager (State Management)
```swift
// PointsManager.swift
@MainActor
class PointsManager: ObservableObject {
    static let shared = PointsManager()

    @Published var currentBalance: Int = 0
    @Published var lifetimePoints: Int = 0
    @Published var configuration: [String: Int] = [:]
    @Published var recentTransactions: [PointTransaction] = []

    private let service = PointsService.shared

    func refreshBalance(for runnerId: Int) async
    func refreshConfiguration() async
    func canAfford(_ feature: FeatureType) -> Bool
    func getCost(for feature: FeatureType) -> Int
    func getEarningAmount(for activityType: String) -> Int
}
```

---

### Phase 2: UI Components

#### 2.1 Points Display Badge
**Location:** Navigation bar, Profile screen

```swift
struct PointsBadge: View {
    let points: Int

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "star.fill")
                .foregroundColor(.yellow)
            Text("\(points)")
                .font(.headline)
                .foregroundColor(.primary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.secondary.opacity(0.2))
        .cornerRadius(20)
    }
}
```

#### 2.2 Points Earned Toast
**Show after activity logging**

```swift
struct PointsEarnedToast: View {
    let pointsEarned: Int
    let activityType: String
    @State private var isShowing = false

    var body: some View {
        if isShowing {
            VStack {
                HStack(spacing: 8) {
                    Image(systemName: "star.fill")
                        .foregroundColor(.yellow)
                    Text("+\(pointsEarned) Points")
                        .font(.headline)
                    Text("for \(activityType)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.green.opacity(0.9))
                .cornerRadius(12)
                .shadow(radius: 10)
            }
            .transition(.move(edge: .top).combined(with: .opacity))
            .animation(.spring(), value: isShowing)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    isShowing = false
                }
            }
        }
    }
}
```

#### 2.3 Leaderboard View
```swift
struct PointsLeaderboardView: View {
    @StateObject private var viewModel = LeaderboardViewModel()

    var body: some View {
        List(viewModel.runners.indices, id: \.self) { index in
            LeaderboardRow(
                rank: index + 1,
                runner: viewModel.runners[index]
            )
        }
        .navigationTitle("Points Leaderboard")
        .refreshable {
            await viewModel.refresh()
        }
    }
}

struct LeaderboardRow: View {
    let rank: Int
    let runner: Runner

    var body: some View {
        HStack {
            Text("#\(rank)")
                .font(.headline)
                .foregroundColor(.secondary)
                .frame(width: 40)

            VStack(alignment: .leading) {
                Text(runner.name)
                    .font(.headline)
                Text("\(runner.graduatingClass) • \(runner.team)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            HStack(spacing: 4) {
                Image(systemName: "star.fill")
                    .foregroundColor(.yellow)
                Text("\(runner.points)")
                    .font(.headline)
            }
        }
        .padding(.vertical, 4)
    }
}
```

#### 2.4 Transaction History View
```swift
struct PointsHistoryView: View {
    @StateObject private var viewModel: HistoryViewModel

    var body: some View {
        List(viewModel.transactions) { transaction in
            TransactionRow(transaction: transaction)
        }
        .navigationTitle("Points History")
    }
}

struct TransactionRow: View {
    let transaction: PointTransaction

    var body: some View {
        HStack {
            Image(systemName: transaction.pointsAmount > 0 ? "arrow.up.circle.fill" : "arrow.down.circle.fill")
                .foregroundColor(transaction.pointsAmount > 0 ? .green : .red)

            VStack(alignment: .leading) {
                Text(transaction.description ?? "Unknown")
                    .font(.headline)
                Text(transaction.createdAt, style: .relative)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Text("\(transaction.pointsAmount > 0 ? "+" : "")\(transaction.pointsAmount)")
                .font(.headline)
                .foregroundColor(transaction.pointsAmount > 0 ? .green : .red)
        }
    }
}
```

---

### Phase 3: Integration Points

#### 3.1 App Launch
```swift
// AppDelegate or App.swift
func application(_ application: UIApplication, didFinishLaunchingWithOptions...) {
    Task {
        // Load points configuration
        await PointsManager.shared.refreshConfiguration()

        // Load current user's balance
        if let userId = UserSession.shared.currentUserId {
            await PointsManager.shared.refreshBalance(for: userId)
        }
    }
}
```

#### 3.2 After Activity Logging
```swift
// TrainingRunViewModel.swift
func logTrainingRun(_ run: TrainingRun) async {
    // Existing activity logging
    let response = try await trainingService.createTrainingRun(run)

    // Refresh points balance
    await PointsManager.shared.refreshBalance(for: run.runnerId)

    // Show toast notification
    let pointsEarned = PointsManager.shared.getEarningAmount(for: "TRAINING_RUN")
    showPointsToast(points: pointsEarned, activity: "Training Run")
}
```

#### 3.3 Before Premium Feature
```swift
// ChatViewModel.swift
func handleGifButtonTapped() async {
    let validation = try await PointsService.shared.validatePoints(
        runnerId: currentUserId,
        featureType: .sendGif
    )

    if validation.hasEnoughPoints {
        // Show GIF picker
        showGifPicker = true
    } else {
        // Show insufficient points alert
        showInsufficientPointsAlert(
            required: validation.pointsRequired,
            current: validation.currentBalance
        )
    }
}
```

#### 3.4 Spend → Action → Refund Pattern
```swift
// ChatViewModel.swift
func sendGif(_ gifUrl: URL) async {
    // 1. Spend points
    let spendResult = try await PointsService.shared.spendPoints(
        runnerId: currentUserId,
        featureType: .sendGif,
        description: "Sent GIF in team chat"
    )

    guard spendResult.success else {
        showError(spendResult.errorMessage ?? "Failed to spend points")
        return
    }

    // Store transaction ID for potential refund
    let transactionId = spendResult.transaction?.id

    // 2. Perform action
    do {
        try await sendGifToFirebase(gifUrl)

        // Success - update local balance
        await PointsManager.shared.refreshBalance(for: currentUserId)

    } catch {
        // 3. Action failed - refund points
        if let txId = transactionId {
            _ = try? await PointsService.shared.refundPoints(transactionId: txId)
            await PointsManager.shared.refreshBalance(for: currentUserId)
        }
        showError("Failed to send GIF")
    }
}
```

---

### Phase 4: Error Handling

#### Common Error Scenarios

```swift
enum PointsError: Error {
    case insufficientPoints(required: Int, current: Int)
    case networkError
    case serverError(String)
    case alreadyRefunded
    case invalidTransaction

    var userMessage: String {
        switch self {
        case .insufficientPoints(let required, let current):
            return "You need \(required) points but only have \(current)"
        case .networkError:
            return "Network error. Please try again."
        case .serverError(let msg):
            return msg
        case .alreadyRefunded:
            return "This transaction was already refunded"
        case .invalidTransaction:
            return "Invalid transaction"
        }
    }
}
```

#### Alert Presentation
```swift
struct InsufficientPointsAlert: View {
    let required: Int
    let current: Int
    @Binding var isPresented: Bool

    var body: some View {
        Alert(
            title: Text("Not Enough Points"),
            message: Text("You need \(required) points but only have \(current). Log more activities to earn points!"),
            primaryButton: .default(Text("Log Activity")) {
                // Navigate to activity logging
            },
            secondaryButton: .cancel()
        )
    }
}
```

---

### Phase 5: Caching Strategy

```swift
// PointsCache.swift
class PointsCache {
    static let shared = PointsCache()

    private let userDefaults = UserDefaults.standard
    private let balanceKey = "cached_points_balance"
    private let configKey = "cached_points_config"
    private let lastRefreshKey = "last_points_refresh"

    func cacheBalance(_ balance: PointBalance) {
        let encoder = JSONEncoder()
        if let data = try? encoder.encode(balance) {
            userDefaults.set(data, forKey: balanceKey)
            userDefaults.set(Date(), forKey: lastRefreshKey)
        }
    }

    func getCachedBalance() -> PointBalance? {
        guard let data = userDefaults.data(forKey: balanceKey),
              let balance = try? JSONDecoder().decode(PointBalance.self, from: data) else {
            return nil
        }

        // Check if cache is stale (older than 5 minutes)
        if let lastRefresh = userDefaults.object(forKey: lastRefreshKey) as? Date,
           Date().timeIntervalSince(lastRefresh) < 300 {
            return balance
        }
        return nil
    }
}
```

---

## Testing Checklist

### Manual Testing

- [ ] Points display correctly in UI after app launch
- [ ] Points increase after logging training run
- [ ] Points increase after logging workout
- [ ] Points increase after logging meet log
- [ ] Toast notification shows correct amount after activity
- [ ] Leaderboard displays and sorts correctly
- [ ] Transaction history shows all transactions
- [ ] GIF send deducts correct points
- [ ] Insufficient points prevents GIF send
- [ ] Failed GIF send refunds points
- [ ] Configuration loads on app launch
- [ ] Points update in real-time across screens

### Edge Cases

- [ ] Network offline - graceful degradation
- [ ] Server error - appropriate error message
- [ ] Concurrent activity logging - no duplicate points
- [ ] Activity update (not create) - no points awarded
- [ ] Double-tap on premium feature - only charge once
- [ ] App backgrounded during transaction - proper state

---

## Configuration Reference

Current point values (can be changed via backend config):

**Earning:**
- Training Run: 10 points
- Workout: 15 points
- Cross Training: 8 points
- Meet Log: 10 points
- Pre-Meet Log: 5 points

**Spending:**
- Send GIF: 5 points
- Pin Message: 10 points
- Message Animation: 3 points
- Change Chat Name: 100 points
- Custom Avatar: 200 points

---

## Next Steps

1. **Phase 1**: Implement data models and API service
2. **Phase 2**: Add UI components (badge, toast, leaderboard)
3. **Phase 3**: Integrate into existing activity flows
4. **Phase 4**: Add premium features with point spending
5. **Phase 5**: Test thoroughly
6. **Phase 6**: Deploy to TestFlight

---

## Questions or Issues?

- All endpoints return standard HTTP status codes
- 200 = Success
- 400 = Bad request (invalid parameters)
- 404 = Not found (invalid runner ID)
- 500 = Server error

**Backend is deployed and ready for integration!**