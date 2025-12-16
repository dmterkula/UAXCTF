# iOS Implementation Guide - Team Talk View Tracking

## Overview
This guide provides iOS implementation details for the team talk view tracking feature. View tracking allows coaches to see who has viewed their team talks and how many times.

## Key Requirements
- Track views automatically when user opens team talk detail page
- Only track authenticated users
- Display view counts/lists only to coaches
- Silent tracking for runners/athletes (no UI shown)

---

## API Integration

### Base URL
```swift
let baseURL = "https://your-api-domain.com/api/v1/team-talks"
```

### Endpoint: Track View

**URL:** `POST /api/v1/team-talks/views/track`

**Purpose:** Track a single view event when user loads team talk detail page

---

## Request Models

### TrackTeamTalkViewRequest

```swift
struct TrackTeamTalkViewRequest: Codable {
    let teamTalkUuid: String
    let username: String
    let displayName: String
}
```

**Field Descriptions:**
- `teamTalkUuid`: UUID of the team talk being viewed
- `username`: Authenticated user's username (from AppUser)
- `displayName`: User's display name (runner name for athletes, username for coaches)

**Example JSON:**
```json
{
  "teamTalkUuid": "abc-123-def-456",
  "username": "jsmith",
  "displayName": "John Smith"
}
```

---

## Response Models

### ViewDetail

Represents a single view event.

```swift
struct ViewDetail: Codable, Identifiable {
    let username: String
    let displayName: String
    let viewedAt: String  // ISO 8601 timestamp

    var id: String {
        "\(username)-\(viewedAt)"
    }
}
```

**Field Descriptions:**
- `username`: Username of the viewer
- `displayName`: Display name of the viewer
- `viewedAt`: ISO 8601 timestamp string (e.g., "2024-12-16T14:32:10Z")

### ViewSummary

Aggregated view statistics for a team talk.

```swift
struct ViewSummary: Codable {
    let totalViews: Int
    let uniqueViewers: Int
    let recentViews: [ViewDetail]
}
```

**Field Descriptions:**
- `totalViews`: Total number of view events (includes repeat views by same user)
- `uniqueViewers`: Count of distinct users who viewed the team talk
- `recentViews`: Array of last 20 view events, ordered by most recent first

**Example JSON:**
```json
{
  "totalViews": 47,
  "uniqueViewers": 23,
  "recentViews": [
    {
      "username": "jdoe",
      "displayName": "John Doe",
      "viewedAt": "2024-12-16T14:32:10Z"
    },
    {
      "username": "asmith",
      "displayName": "Alice Smith",
      "viewedAt": "2024-12-16T14:30:05Z"
    }
  ]
}
```

### Updated TeamTalkResponse

The existing `TeamTalkResponse` now includes view summary data.

```swift
struct TeamTalkResponse: Codable {
    let teamTalk: TeamTalk
    let reactions: [ReactionSummary]
    let comments: [NestedComment]
    let totalCommentCount: Int
    let viewSummary: ViewSummary  // NEW FIELD
}
```

**What Changed:**
- Added `viewSummary` field to existing response
- All GET endpoints for team talks now include this field
- For new team talks, view summary will have zero counts

---

## Service Layer Implementation

### TeamTalkService

Add view tracking capability to your existing `TeamTalkService`.

```swift
class TeamTalkService {
    private let baseURL = "https://your-api-domain.com/api/v1/team-talks"

    // MARK: - Track View

    /// Track a view of a team talk
    /// Should be called when TeamTalkDetailView appears
    /// - Parameters:
    ///   - teamTalkUuid: UUID of the team talk
    ///   - username: Current user's username
    ///   - displayName: Current user's display name
    func trackView(
        teamTalkUuid: String,
        username: String,
        displayName: String
    ) async throws {
        let url = URL(string: "\(baseURL)/views/track")!

        let request = TrackTeamTalkViewRequest(
            teamTalkUuid: teamTalkUuid,
            username: username,
            displayName: displayName
        )

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.httpBody = try JSONEncoder().encode(request)

        let (_, response) = try await URLSession.shared.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw TeamTalkError.trackViewFailed
        }
    }

    /// Track view without throwing errors (fire-and-forget)
    /// Use this in view lifecycle to avoid disrupting UX
    func trackViewSilently(
        teamTalkUuid: String,
        username: String,
        displayName: String
    ) {
        Task {
            try? await trackView(
                teamTalkUuid: teamTalkUuid,
                username: username,
                displayName: displayName
            )
        }
    }
}

enum TeamTalkError: Error {
    case trackViewFailed
    // ... other errors
}
```

---

## View Implementation

### TeamTalkDetailView

Update your team talk detail view to track views on appear.

```swift
struct TeamTalkDetailView: View {
    let teamTalk: TeamTalkResponse
    @EnvironmentObject var authService: AuthenticationService
    @StateObject private var teamTalkService = TeamTalkService()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Team talk title
                Text(teamTalk.teamTalk.title)
                    .font(.title)
                    .fontWeight(.bold)

                // Team talk content
                MarkdownText(teamTalk.teamTalk.content)

                // Reactions section
                ReactionsView(reactions: teamTalk.reactions)

                // Comments section
                CommentsView(comments: teamTalk.comments)

                // View stats (coaches only)
                if authService.currentUser?.role == "coach" {
                    ViewStatsView(viewSummary: teamTalk.viewSummary)
                }
            }
            .padding()
        }
        .navigationTitle("Team Talk")
        .onAppear {
            trackView()
        }
    }

    private func trackView() {
        guard let user = authService.currentUser else { return }

        teamTalkService.trackViewSilently(
            teamTalkUuid: teamTalk.teamTalk.uuid,
            username: user.username,
            displayName: user.displayName
        )
    }
}
```

### ViewStatsView (Coaches Only)

Display view statistics for coaches.

```swift
struct ViewStatsView: View {
    let viewSummary: ViewSummary
    @State private var showingRecentViewers = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "eye.fill")
                    .foregroundColor(.blue)
                Text("View Analytics")
                    .font(.headline)
                Spacer()
            }

            Divider()

            HStack(spacing: 32) {
                StatBox(
                    title: "Total Views",
                    value: "\(viewSummary.totalViews)",
                    icon: "eye"
                )

                StatBox(
                    title: "Unique Viewers",
                    value: "\(viewSummary.uniqueViewers)",
                    icon: "person.2"
                )
            }

            if !viewSummary.recentViews.isEmpty {
                Button(action: {
                    showingRecentViewers.toggle()
                }) {
                    HStack {
                        Text("Recent Viewers")
                            .font(.subheadline)
                        Spacer()
                        Image(systemName: showingRecentViewers ? "chevron.up" : "chevron.down")
                    }
                    .foregroundColor(.blue)
                }

                if showingRecentViewers {
                    RecentViewersList(recentViews: viewSummary.recentViews)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct StatBox: View {
    let title: String
    let value: String
    let icon: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.caption)
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
        }
    }
}

struct RecentViewersList: View {
    let recentViews: [ViewDetail]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(recentViews) { view in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(view.displayName)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text(formatViewTime(view.viewedAt))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                }
                .padding(.vertical, 4)

                if view.id != recentViews.last?.id {
                    Divider()
                }
            }
        }
        .padding(.vertical, 8)
    }

    private func formatViewTime(_ isoString: String) -> String {
        let formatter = ISO8601DateFormatter()
        guard let date = formatter.date(from: isoString) else {
            return isoString
        }

        let relativeFormatter = RelativeDateTimeFormatter()
        relativeFormatter.unitsStyle = .full
        return relativeFormatter.localizedString(for: date, relativeTo: Date())
    }
}
```

---

## Date Formatting Utilities

### ISO 8601 Parsing

```swift
extension ViewDetail {
    var viewedAtDate: Date? {
        let formatter = ISO8601DateFormatter()
        return formatter.date(from: viewedAt)
    }

    var relativeTimeString: String {
        guard let date = viewedAtDate else { return viewedAt }

        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: date, relativeTo: Date())
    }

    var formattedDateString: String {
        guard let date = viewedAtDate else { return viewedAt }

        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }
}
```

**Usage:**
```swift
Text(viewDetail.relativeTimeString)  // "2 hours ago"
Text(viewDetail.formattedDateString) // "Dec 16, 2024 at 2:32 PM"
```

---

## Testing

### Manual Testing Checklist

1. **View Tracking (All Users)**
   - [ ] Open a team talk as an athlete
   - [ ] Verify network request sent to `/views/track`
   - [ ] Verify no UI changes or errors shown
   - [ ] Close and reopen same team talk
   - [ ] Verify another view is tracked

2. **View Stats Display (Coaches Only)**
   - [ ] Login as coach
   - [ ] Open a team talk
   - [ ] Verify "View Analytics" section visible
   - [ ] Verify total views count displayed
   - [ ] Verify unique viewers count displayed
   - [ ] Tap "Recent Viewers" to expand
   - [ ] Verify list shows viewer names and relative times
   - [ ] Verify timestamps are properly formatted

3. **Privacy (Athletes)**
   - [ ] Login as athlete/runner
   - [ ] Open a team talk
   - [ ] Verify NO view stats section displayed
   - [ ] Verify view tracking still happens in background

4. **Error Handling**
   - [ ] Turn off network
   - [ ] Open team talk
   - [ ] Verify app doesn't crash (silent failure)
   - [ ] Verify team talk still displays normally

### Unit Test Examples

```swift
import XCTest
@testable import YourApp

class ViewTrackingTests: XCTestCase {

    func testTrackViewRequestEncoding() throws {
        let request = TrackTeamTalkViewRequest(
            teamTalkUuid: "test-uuid-123",
            username: "testuser",
            displayName: "Test User"
        )

        let encoder = JSONEncoder()
        let data = try encoder.encode(request)
        let json = try JSONSerialization.jsonObject(with: data) as? [String: String]

        XCTAssertEqual(json?["teamTalkUuid"], "test-uuid-123")
        XCTAssertEqual(json?["username"], "testuser")
        XCTAssertEqual(json?["displayName"], "Test User")
    }

    func testViewSummaryDecoding() throws {
        let json = """
        {
            "totalViews": 47,
            "uniqueViewers": 23,
            "recentViews": [
                {
                    "username": "jdoe",
                    "displayName": "John Doe",
                    "viewedAt": "2024-12-16T14:32:10Z"
                }
            ]
        }
        """.data(using: .utf8)!

        let decoder = JSONDecoder()
        let summary = try decoder.decode(ViewSummary.self, from: json)

        XCTAssertEqual(summary.totalViews, 47)
        XCTAssertEqual(summary.uniqueViewers, 23)
        XCTAssertEqual(summary.recentViews.count, 1)
        XCTAssertEqual(summary.recentViews[0].username, "jdoe")
    }

    func testViewDetailDateParsing() throws {
        let viewDetail = ViewDetail(
            username: "test",
            displayName: "Test User",
            viewedAt: "2024-12-16T14:32:10Z"
        )

        XCTAssertNotNil(viewDetail.viewedAtDate)
        XCTAssertFalse(viewDetail.relativeTimeString.isEmpty)
    }
}
```

---

## Integration Checklist

### Step 1: Update Models
- [ ] Add `ViewDetail` struct to models
- [ ] Add `ViewSummary` struct to models
- [ ] Add `TrackTeamTalkViewRequest` struct to models
- [ ] Update `TeamTalkResponse` to include `viewSummary` field

### Step 2: Update Service
- [ ] Add `trackView()` method to `TeamTalkService`
- [ ] Add `trackViewSilently()` helper method
- [ ] Test network requests with Postman/curl

### Step 3: Update Views
- [ ] Add `.onAppear { trackView() }` to `TeamTalkDetailView`
- [ ] Create `ViewStatsView` component
- [ ] Add conditional rendering based on user role
- [ ] Test with both coach and athlete accounts

### Step 4: Testing
- [ ] Test view tracking for all users
- [ ] Test stats display for coaches only
- [ ] Test privacy (athletes can't see stats)
- [ ] Test error handling (network failures)
- [ ] Test date formatting and relative times

---

## Troubleshooting

### View tracking not working
**Symptoms:** No views showing up in backend
**Solutions:**
- Verify user is authenticated before calling `trackView()`
- Check network requests in Xcode network debugger
- Verify `username` and `displayName` are not empty strings
- Check backend logs for errors

### Stats not visible for coaches
**Symptoms:** Coach can't see view analytics
**Solutions:**
- Verify `authService.currentUser?.role == "coach"` returns true
- Check that `viewSummary` field exists in API response
- Verify backend is returning view data (not null/empty)

### App crashes on view tracking
**Symptoms:** App crashes when opening team talk
**Solutions:**
- Use `trackViewSilently()` instead of throwing version
- Add proper error handling in service layer
- Ensure all required fields are non-nil

### Timestamps showing incorrectly
**Symptoms:** "Invalid date" or wrong timezone
**Solutions:**
- Verify backend returns ISO 8601 format strings
- Use `ISO8601DateFormatter()` for parsing
- Check timezone settings in date formatter

---

## Performance Considerations

### Best Practices

1. **Silent Tracking**
   - Use fire-and-forget approach (`trackViewSilently`)
   - Don't block UI on view tracking
   - Handle errors gracefully without user notification

2. **Caching**
   - Cache view summaries to reduce API calls
   - Refresh on pull-to-refresh or significant user action
   - Don't refetch on every appear (track view once per session)

3. **Network Efficiency**
   - Track view in background queue
   - Batch multiple views if needed (future enhancement)
   - Use URLSession's built-in retry mechanisms

### Example: Track Once Per Session

```swift
struct TeamTalkDetailView: View {
    let teamTalk: TeamTalkResponse
    @EnvironmentObject var authService: AuthenticationService
    @StateObject private var teamTalkService = TeamTalkService()
    @State private var hasTrackedView = false

    var body: some View {
        // ... view content
        .onAppear {
            if !hasTrackedView {
                trackView()
                hasTrackedView = true
            }
        }
    }
}
```

---

## API Response Examples

### GET /api/v1/team-talks/{uuid}

**Response with View Summary:**

```json
{
  "teamTalk": {
    "uuid": "abc-123-def-456",
    "author": "coach_mike",
    "season": "xc",
    "year": "2024",
    "title": "Pre-Race Strategy",
    "content": "# Race Day Tips\n\n...",
    "team": "UA",
    "createdAt": "2024-10-01T10:00:00Z",
    "updatedAt": "2024-10-01T10:00:00Z"
  },
  "reactions": [
    {
      "emoji": "🔥",
      "count": 12,
      "usernames": ["jdoe", "asmith"]
    }
  ],
  "comments": [...],
  "totalCommentCount": 8,
  "viewSummary": {
    "totalViews": 47,
    "uniqueViewers": 23,
    "recentViews": [
      {
        "username": "jdoe",
        "displayName": "John Doe",
        "viewedAt": "2024-12-16T14:32:10Z"
      },
      {
        "username": "asmith",
        "displayName": "Alice Smith",
        "viewedAt": "2024-12-16T14:30:05Z"
      }
    ]
  }
}
```

### POST /api/v1/team-talks/views/track

**Request:**
```json
{
  "teamTalkUuid": "abc-123-def-456",
  "username": "jsmith",
  "displayName": "John Smith"
}
```

**Response:**
```
200 OK
(empty body)
```

---

## Summary

### What to Implement

1. **Models**: Add `ViewDetail`, `ViewSummary`, `TrackTeamTalkViewRequest`
2. **Service**: Add `trackView()` and `trackViewSilently()` methods
3. **Views**: Add view tracking on appear, display stats for coaches only
4. **Testing**: Verify tracking works, stats display correctly, privacy maintained

### Key Points

✅ Track views silently in background (don't disrupt UX)
✅ Display stats only to coaches (privacy for athletes)
✅ Handle errors gracefully (fire-and-forget approach)
✅ Format timestamps properly (relative time for better UX)
✅ All field names match backend exactly (camelCase in Swift, same as JSON)

The implementation is straightforward and follows the existing patterns in your team talk feature. View tracking integrates seamlessly without changing existing functionality.
