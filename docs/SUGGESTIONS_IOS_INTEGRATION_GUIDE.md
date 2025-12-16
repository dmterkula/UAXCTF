# Suggestions Feature - iOS Integration Guide

## Overview
This guide provides complete iOS integration instructions for the Runner Suggestions feature, including Swift models, API service methods, and example UI implementation patterns.

## Table of Contents
1. [API Endpoints](#api-endpoints)
2. [Swift Models](#swift-models)
3. [API Service Layer](#api-service-layer)
4. [Usage Examples](#usage-examples)
5. [Error Handling](#error-handling)
6. [UI Implementation Patterns](#ui-implementation-patterns)

---

## API Endpoints

### Base URL
```swift
let baseURL = "https://your-api-domain.com/api"
```

### Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/suggestions` | Create new suggestion |
| GET | `/api/suggestions?team={team}&runnerId={id}` | List all suggestions |
| GET | `/api/suggestions/{uuid}?runnerId={id}` | Get suggestion detail |
| PUT | `/api/suggestions/status` | Update status (coaches) |
| POST | `/api/suggestions/thumbs-up` | Toggle thumbs up |
| POST | `/api/suggestions/comments` | Add comment |
| DELETE | `/api/suggestions/{uuid}` | Delete suggestion (coaches) |

---

## Swift Models

### Core Models

```swift
import Foundation

// MARK: - Suggestion Status Enum
enum SuggestionStatus: String, Codable {
    case underReview = "under_review"
    case underConsideration = "under_consideration"
    case inProgress = "in_progress"
    case complete = "complete"
    case rejected = "rejected"

    var displayName: String {
        switch self {
        case .underReview: return "Under Review"
        case .underConsideration: return "Under Consideration"
        case .inProgress: return "In Progress"
        case .complete: return "Complete"
        case .rejected: return "Rejected"
        }
    }

    var color: UIColor {
        switch self {
        case .underReview: return .systemBlue
        case .underConsideration: return .systemOrange
        case .inProgress: return .systemPurple
        case .complete: return .systemGreen
        case .rejected: return .systemRed
        }
    }
}

// MARK: - Suggestion Category Enum
enum SuggestionCategory: String, Codable {
    case featureRequest = "feature_request"
    case bugReport = "bug_report"
    case uiUx = "ui_ux"
    case performance = "performance"
    case other = "other"

    var displayName: String {
        switch self {
        case .featureRequest: return "Feature Request"
        case .bugReport: return "Bug Report"
        case .uiUx: return "UI/UX"
        case .performance: return "Performance"
        case .other: return "Other"
        }
    }

    var icon: String {
        switch self {
        case .featureRequest: return "lightbulb.fill"
        case .bugReport: return "ant.fill"
        case .uiUx: return "paintbrush.fill"
        case .performance: return "speedometer"
        case .other: return "ellipsis.circle.fill"
        }
    }
}

// MARK: - Runner Model
struct Runner: Codable {
    let id: Int
    let name: String
    let graduatingClass: String
    let isActive: Bool
    let doesXc: Bool
    let doesTrack: Bool
    let team: String
    let points: Int
    let lifetimePointsEarned: Int
}

// MARK: - Suggestion DTO
struct SuggestionDTO: Codable, Identifiable {
    let uuid: String
    let title: String
    let description: String
    let category: String
    let runner: Runner
    let status: String
    let createdAt: String
    let statusChangedAt: String?
    let thumbsUpCount: Int
    let hasThumbsUp: Bool?
    let commentCount: Int

    var id: String { uuid }

    var categoryEnum: SuggestionCategory? {
        SuggestionCategory(rawValue: category)
    }

    var statusEnum: SuggestionStatus? {
        SuggestionStatus(rawValue: status)
    }

    var createdDate: Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.date(from: createdAt)
    }
}

// MARK: - Suggestion Comment
struct SuggestionComment: Codable, Identifiable {
    let uuid: String
    let suggestionUuid: String
    let username: String
    let displayName: String
    let message: String
    let createdAt: String
    let updatedAt: String

    var id: String { uuid }

    var createdDate: Date? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.date(from: createdAt)
    }
}

// MARK: - Response Models
struct SuggestionsListResponse: Codable {
    let suggestions: [SuggestionDTO]
}

struct SuggestionDetailResponse: Codable {
    let suggestion: SuggestionDTO
    let comments: [SuggestionComment]
}

struct ThumbsUpResponse: Codable {
    let suggestionUuid: String
    let thumbsUpCount: Int
    let hasThumbsUp: Bool
}

// MARK: - Request Models
struct CreateSuggestionRequest: Codable {
    let uuid: String
    let title: String
    let description: String
    let category: String
    let runnerId: Int
    let team: String
    let season: String?
}

struct UpdateSuggestionStatusRequest: Codable {
    let suggestionUuid: String
    let status: String
}

struct ToggleThumbsUpRequest: Codable {
    let suggestionUuid: String
    let runnerId: Int
}

struct CreateSuggestionCommentRequest: Codable {
    let uuid: String
    let suggestionUuid: String
    let username: String
    let displayName: String
    let message: String
}
```

---

## API Service Layer

### SuggestionAPIService.swift

```swift
import Foundation

class SuggestionAPIService {

    static let shared = SuggestionAPIService()
    private let baseURL = "https://your-api-domain.com/api"

    private init() {}

    // MARK: - Create Suggestion
    func createSuggestion(
        title: String,
        description: String,
        category: SuggestionCategory,
        runnerId: Int,
        team: String,
        season: String? = nil,
        completion: @escaping (Result<SuggestionDTO, Error>) -> Void
    ) {
        let url = URL(string: "\(baseURL)/suggestions")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let requestBody = CreateSuggestionRequest(
            uuid: UUID().uuidString,
            title: title,
            description: description,
            category: category.rawValue,
            runnerId: runnerId,
            team: team,
            season: season
        )

        do {
            request.httpBody = try JSONEncoder().encode(requestBody)
        } catch {
            completion(.failure(error))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            guard let data = data else {
                completion(.failure(NSError(domain: "No data", code: -1)))
                return
            }

            do {
                let suggestion = try JSONDecoder().decode(SuggestionDTO.self, from: data)
                completion(.success(suggestion))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // MARK: - Get All Suggestions
    func getSuggestions(
        team: String,
        runnerId: Int? = nil,
        completion: @escaping (Result<[SuggestionDTO], Error>) -> Void
    ) {
        var components = URLComponents(string: "\(baseURL)/suggestions")!
        components.queryItems = [
            URLQueryItem(name: "team", value: team)
        ]
        if let runnerId = runnerId {
            components.queryItems?.append(URLQueryItem(name: "runnerId", value: "\(runnerId)"))
        }

        guard let url = components.url else {
            completion(.failure(NSError(domain: "Invalid URL", code: -1)))
            return
        }

        URLSession.shared.dataTask(with: url) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            guard let data = data else {
                completion(.failure(NSError(domain: "No data", code: -1)))
                return
            }

            do {
                let response = try JSONDecoder().decode(SuggestionsListResponse.self, from: data)
                completion(.success(response.suggestions))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // MARK: - Get Suggestion Detail
    func getSuggestionDetail(
        uuid: String,
        runnerId: Int? = nil,
        completion: @escaping (Result<SuggestionDetailResponse, Error>) -> Void
    ) {
        var components = URLComponents(string: "\(baseURL)/suggestions/\(uuid)")!
        if let runnerId = runnerId {
            components.queryItems = [URLQueryItem(name: "runnerId", value: "\(runnerId)")]
        }

        guard let url = components.url else {
            completion(.failure(NSError(domain: "Invalid URL", code: -1)))
            return
        }

        URLSession.shared.dataTask(with: url) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            guard let data = data else {
                completion(.failure(NSError(domain: "No data", code: -1)))
                return
            }

            do {
                let detail = try JSONDecoder().decode(SuggestionDetailResponse.self, from: data)
                completion(.success(detail))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // MARK: - Update Status (Coaches Only)
    func updateStatus(
        suggestionUuid: String,
        status: SuggestionStatus,
        completion: @escaping (Result<SuggestionDTO, Error>) -> Void
    ) {
        let url = URL(string: "\(baseURL)/suggestions/status")!
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let requestBody = UpdateSuggestionStatusRequest(
            suggestionUuid: suggestionUuid,
            status: status.rawValue
        )

        do {
            request.httpBody = try JSONEncoder().encode(requestBody)
        } catch {
            completion(.failure(error))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            guard let data = data else {
                completion(.failure(NSError(domain: "No data", code: -1)))
                return
            }

            do {
                let suggestion = try JSONDecoder().decode(SuggestionDTO.self, from: data)
                completion(.success(suggestion))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // MARK: - Toggle Thumbs Up
    func toggleThumbsUp(
        suggestionUuid: String,
        runnerId: Int,
        completion: @escaping (Result<ThumbsUpResponse, Error>) -> Void
    ) {
        let url = URL(string: "\(baseURL)/suggestions/thumbs-up")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let requestBody = ToggleThumbsUpRequest(
            suggestionUuid: suggestionUuid,
            runnerId: runnerId
        )

        do {
            request.httpBody = try JSONEncoder().encode(requestBody)
        } catch {
            completion(.failure(error))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            guard let data = data else {
                completion(.failure(NSError(domain: "No data", code: -1)))
                return
            }

            do {
                let response = try JSONDecoder().decode(ThumbsUpResponse.self, from: data)
                completion(.success(response))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // MARK: - Add Comment
    func addComment(
        suggestionUuid: String,
        username: String,
        displayName: String,
        message: String,
        completion: @escaping (Result<SuggestionComment, Error>) -> Void
    ) {
        let url = URL(string: "\(baseURL)/suggestions/comments")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let requestBody = CreateSuggestionCommentRequest(
            uuid: UUID().uuidString,
            suggestionUuid: suggestionUuid,
            username: username,
            displayName: displayName,
            message: message
        )

        do {
            request.httpBody = try JSONEncoder().encode(requestBody)
        } catch {
            completion(.failure(error))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            guard let data = data else {
                completion(.failure(NSError(domain: "No data", code: -1)))
                return
            }

            do {
                let comment = try JSONDecoder().decode(SuggestionComment.self, from: data)
                completion(.success(comment))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    // MARK: - Delete Suggestion (Coaches Only)
    func deleteSuggestion(
        uuid: String,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        let url = URL(string: "\(baseURL)/suggestions/\(uuid)")!
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(error))
                return
            }

            if let httpResponse = response as? HTTPURLResponse,
               httpResponse.statusCode == 200 {
                completion(.success(()))
            } else {
                completion(.failure(NSError(domain: "Delete failed", code: -1)))
            }
        }.resume()
    }
}
```

---

## Usage Examples

### 1. Creating a Suggestion

```swift
SuggestionAPIService.shared.createSuggestion(
    title: "Add dark mode support",
    description: "It would be great to have a dark mode option for better visibility at night.",
    category: .featureRequest,
    runnerId: 123,
    team: "UAXC",
    season: "XC"
) { result in
    DispatchQueue.main.async {
        switch result {
        case .success(let suggestion):
            print("Suggestion created: \(suggestion.title)")
            // Update UI
        case .failure(let error):
            print("Error creating suggestion: \(error)")
            // Show error alert
        }
    }
}
```

### 2. Loading Suggestions List

```swift
SuggestionAPIService.shared.getSuggestions(
    team: "UAXC",
    runnerId: 123
) { result in
    DispatchQueue.main.async {
        switch result {
        case .success(let suggestions):
            // Update table view or collection view
            self.suggestions = suggestions
            self.tableView.reloadData()
        case .failure(let error):
            print("Error loading suggestions: \(error)")
        }
    }
}
```

### 3. Toggling Thumbs Up

```swift
@IBAction func thumbsUpTapped(_ sender: UIButton) {
    SuggestionAPIService.shared.toggleThumbsUp(
        suggestionUuid: suggestion.uuid,
        runnerId: currentUser.runnerId
    ) { result in
        DispatchQueue.main.async {
            switch result {
            case .success(let response):
                // Update UI
                self.thumbsUpCountLabel.text = "\(response.thumbsUpCount)"
                self.thumbsUpButton.isSelected = response.hasThumbsUp
            case .failure(let error):
                print("Error toggling thumbs up: \(error)")
            }
        }
    }
}
```

### 4. Adding a Comment

```swift
@IBAction func postCommentTapped(_ sender: UIButton) {
    guard let message = commentTextField.text, !message.isEmpty else { return }

    SuggestionAPIService.shared.addComment(
        suggestionUuid: suggestion.uuid,
        username: currentUser.username,
        displayName: currentUser.displayName,
        message: message
    ) { result in
        DispatchQueue.main.async {
            switch result {
            case .success(let comment):
                self.comments.append(comment)
                self.tableView.reloadData()
                self.commentTextField.text = ""
            case .failure(let error):
                print("Error posting comment: \(error)")
            }
        }
    }
}
```

### 5. Updating Status (Coaches Only)

```swift
func updateSuggestionStatus(to status: SuggestionStatus) {
    SuggestionAPIService.shared.updateStatus(
        suggestionUuid: suggestion.uuid,
        status: status
    ) { result in
        DispatchQueue.main.async {
            switch result {
            case .success(let updatedSuggestion):
                self.suggestion = updatedSuggestion
                self.updateStatusUI()
            case .failure(let error):
                print("Error updating status: \(error)")
            }
        }
    }
}
```

---

## Error Handling

### Custom Error Types

```swift
enum SuggestionError: LocalizedError {
    case networkError
    case invalidResponse
    case unauthorized
    case notFound
    case serverError(String)

    var errorDescription: String? {
        switch self {
        case .networkError:
            return "Network connection failed. Please check your internet connection."
        case .invalidResponse:
            return "Invalid response from server."
        case .unauthorized:
            return "You don't have permission to perform this action."
        case .notFound:
            return "Suggestion not found."
        case .serverError(let message):
            return "Server error: \(message)"
        }
    }
}
```

### Error Handling Example

```swift
func handleError(_ error: Error) {
    let alert = UIAlertController(
        title: "Error",
        message: error.localizedDescription,
        preferredStyle: .alert
    )
    alert.addAction(UIAlertAction(title: "OK", style: .default))
    present(alert, animated: true)
}
```

---

## UI Implementation Patterns

### SwiftUI View Example

```swift
import SwiftUI

struct SuggestionsListView: View {
    @State private var suggestions: [SuggestionDTO] = []
    @State private var isLoading = false
    let team: String
    let runnerId: Int

    var body: some View {
        List(suggestions) { suggestion in
            NavigationLink(destination: SuggestionDetailView(suggestion: suggestion)) {
                SuggestionRow(suggestion: suggestion)
            }
        }
        .navigationTitle("Suggestions")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(action: { /* Show create suggestion view */ }) {
                    Image(systemName: "plus")
                }
            }
        }
        .onAppear(perform: loadSuggestions)
        .overlay {
            if isLoading {
                ProgressView()
            }
        }
    }

    func loadSuggestions() {
        isLoading = true
        SuggestionAPIService.shared.getSuggestions(team: team, runnerId: runnerId) { result in
            DispatchQueue.main.async {
                isLoading = false
                switch result {
                case .success(let loadedSuggestions):
                    self.suggestions = loadedSuggestions
                case .failure(let error):
                    print("Error: \(error)")
                }
            }
        }
    }
}

struct SuggestionRow: View {
    let suggestion: SuggestionDTO

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                if let category = suggestion.categoryEnum {
                    Image(systemName: category.icon)
                        .foregroundColor(.blue)
                }
                Text(suggestion.title)
                    .font(.headline)
                Spacer()
                if let status = suggestion.statusEnum {
                    Text(status.displayName)
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(status.color).opacity(0.2))
                        .foregroundColor(Color(status.color))
                        .cornerRadius(8)
                }
            }

            Text(suggestion.description)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .lineLimit(2)

            HStack {
                Label("\(suggestion.thumbsUpCount)", systemImage: "hand.thumbsup.fill")
                    .font(.caption)
                    .foregroundColor(.blue)

                Label("\(suggestion.commentCount)", systemImage: "bubble.right.fill")
                    .font(.caption)
                    .foregroundColor(.gray)

                Spacer()

                Text(suggestion.runner.name)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
```

### UIKit TableView Cell Example

```swift
class SuggestionTableViewCell: UITableViewCell {

    @IBOutlet weak var categoryIconImageView: UIImageView!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var descriptionLabel: UILabel!
    @IBOutlet weak var statusLabel: UILabel!
    @IBOutlet weak var thumbsUpCountLabel: UILabel!
    @IBOutlet weak var commentCountLabel: UILabel!
    @IBOutlet weak var runnerNameLabel: UILabel!
    @IBOutlet weak var thumbsUpButton: UIButton!

    var suggestion: SuggestionDTO? {
        didSet {
            updateUI()
        }
    }

    private func updateUI() {
        guard let suggestion = suggestion else { return }

        titleLabel.text = suggestion.title
        descriptionLabel.text = suggestion.description
        thumbsUpCountLabel.text = "\(suggestion.thumbsUpCount)"
        commentCountLabel.text = "\(suggestion.commentCount)"
        runnerNameLabel.text = suggestion.runner.name

        if let category = suggestion.categoryEnum {
            categoryIconImageView.image = UIImage(systemName: category.icon)
        }

        if let status = suggestion.statusEnum {
            statusLabel.text = status.displayName
            statusLabel.backgroundColor = status.color.withAlphaComponent(0.2)
            statusLabel.textColor = status.color
        }

        if let hasThumbsUp = suggestion.hasThumbsUp {
            thumbsUpButton.isSelected = hasThumbsUp
        }
    }
}
```

---

## Best Practices

### 1. Async/Await Support (iOS 13+)

Convert the completion-based API to async/await:

```swift
extension SuggestionAPIService {
    func getSuggestions(team: String, runnerId: Int? = nil) async throws -> [SuggestionDTO] {
        try await withCheckedThrowingContinuation { continuation in
            getSuggestions(team: team, runnerId: runnerId) { result in
                continuation.resume(with: result)
            }
        }
    }
}

// Usage
Task {
    do {
        let suggestions = try await SuggestionAPIService.shared.getSuggestions(team: "UAXC", runnerId: 123)
        // Update UI
    } catch {
        // Handle error
    }
}
```

### 2. Caching Strategy

```swift
class SuggestionCache {
    static let shared = SuggestionCache()
    private var cache: [String: [SuggestionDTO]] = [:]

    func get(forTeam team: String) -> [SuggestionDTO]? {
        return cache[team]
    }

    func set(_ suggestions: [SuggestionDTO], forTeam team: String) {
        cache[team] = suggestions
    }

    func invalidate(forTeam team: String) {
        cache.removeValue(forKey: team)
    }
}
```

### 3. Pagination Support

If the API supports pagination in the future:

```swift
func getSuggestions(
    team: String,
    runnerId: Int? = nil,
    page: Int = 0,
    pageSize: Int = 20,
    completion: @escaping (Result<[SuggestionDTO], Error>) -> Void
)
```

---

## Testing

### Unit Test Example

```swift
import XCTest

class SuggestionAPIServiceTests: XCTestCase {

    func testCreateSuggestion() {
        let expectation = self.expectation(description: "Create suggestion")

        SuggestionAPIService.shared.createSuggestion(
            title: "Test Suggestion",
            description: "Test Description",
            category: .featureRequest,
            runnerId: 1,
            team: "UAXC"
        ) { result in
            switch result {
            case .success(let suggestion):
                XCTAssertEqual(suggestion.title, "Test Suggestion")
                expectation.fulfill()
            case .failure(let error):
                XCTFail("Error: \(error)")
            }
        }

        waitForExpectations(timeout: 5.0)
    }
}
```

---

## Security Considerations

1. **Authentication**: Add authentication tokens to all API requests
2. **Role Validation**: Validate coach role before showing admin actions
3. **Input Validation**: Validate user input before sending to API
4. **SSL Pinning**: Consider implementing SSL pinning for production

```swift
// Example: Adding authentication header
extension URLRequest {
    mutating func addAuthToken(_ token: String) {
        setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    }
}
```

---

## Support

For questions or issues, please contact the backend team or refer to the API documentation.
