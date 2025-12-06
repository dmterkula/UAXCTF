# Points Gamification System - Backend Implementation Summary

## Overview

Successfully implemented Phases 1-3 of the points-based gamification system for the UAXCTF backend. This system operates independently from the existing Skulls achievement system and provides consumable points that runners earn by logging activities and spend on premium features.

## Implementation Date

November 30, 2025

## Critical Security Fix Applied

**Issue**: Initially, points were being awarded in controllers, which would allow runners to earn infinite points by repeatedly updating (not creating) the same activity.

**Solution**: Moved all points awarding logic from controllers into service layer, where points are ONLY awarded when creating NEW records (checked via `if (existing == null)` conditions), NOT when updating existing records.

This prevents the "infinite points farming" vulnerability.

## What Was Implemented

### Phase 1: Backend Foundation ✅

#### 1. Database Migration
**File:** `/db-migrations/add_points_gamification.sql`

Created comprehensive SQL migration with:
- Added `points`, `lifetime_points_earned`, and `last_points_updated` columns to `runners` table
- Created `point_transactions` table to track all point activities (earning, spending, refunds)
- Created `points_configuration` table for backend-driven configuration values
- Added appropriate indexes for performance
- Seeded initial configuration values for earning and spending

#### 2. Domain Models
**Files Created:**
- `/src/main/kotlin/com/terkula/uaxctf/statisitcs/model/PointTransaction.kt`
- `/src/main/kotlin/com/terkula/uaxctf/statisitcs/model/PointsConfiguration.kt`

**File Modified:**
- `/src/main/kotlin/com/terkula/uaxctf/statisitcs/model/Runner.kt` - Added points fields

All models follow JPA/Hibernate patterns with proper annotations and timestamp management.

#### 3. Request/Response DTOs
**Files Created:**
- `/src/main/kotlin/com/terkula/uaxctf/statistics/request/PointsRequests.kt`
  - `ValidatePointsRequest`
  - `SpendPointsRequest`
  - `RefundPointsRequest`
  - `UpdatePointsConfigRequest`
  - `EarnPointsRequest`

- `/src/main/kotlin/com/terkula/uaxctf/statistics/response/PointsResponses.kt`
  - `PointBalanceResponse`
  - `ValidatePointsResponse`
  - `SpendPointsResponse`
  - `RefundPointsResponse`
  - `PointsConfigResponse`
  - `EarnPointsResponse`

#### 4. Repositories
**Files Created:**
- `/src/main/kotlin/com/terkula/uaxctf/statistics/repository/PointTransactionRepository.kt`
- `/src/main/kotlin/com/terkula/uaxctf/statistics/repository/PointsConfigurationRepository.kt`

Both repositories extend `CrudRepository` and provide custom query methods.

#### 5. Service Layer
**File:** `/src/main/kotlin/com/terkula/uaxctf/statistics/service/PointsService.kt`

Comprehensive service with:
- **Point awarding** - `earnPoints()` with duplicate detection via UUID
- **Point spending** - `spendPoints()` with transaction isolation for race condition prevention
- **Point validation** - `validatePoints()` for pre-flight checks
- **Point refunds** - `refundPoints()` for failed actions
- **Transaction history** - `getTransactionHistory()`
- **Configuration management** - In-memory caching with 5-minute refresh
- **Leaderboard** - `getLeaderboard()` for top runners

#### 6. Controller/API Layer
**File:** `/src/main/kotlin/com/terkula/uaxctf/statistics/controller/PointsController.kt`

REST API endpoints:
- `GET /api/v1/points/balance` - Get runner's current balance
- `POST /api/v1/points/validate` - Validate sufficient points
- `POST /api/v1/points/spend` - Spend points on features
- `POST /api/v1/points/refund` - Refund failed transactions
- `POST /api/v1/points/earn` - Award points (internal)
- `GET /api/v1/points/transactions` - Get transaction history
- `GET /api/v1/points/config` - Get configuration values
- `PUT /api/v1/points/config` - Update configuration (admin)
- `GET /api/v1/points/leaderboard` - Get points leaderboard

### Phase 3: Activity Logging Integration ✅

Integrated points awarding into existing activity controllers:

#### Modified Controllers:

1. **TrainingRunsController**
   - Endpoint: `POST /xc/runners-training-run/create`
   - Awards: 10 points for logging training run
   - Activity Type: `TRAINING_RUN`

2. **WorkoutController**
   - Endpoint: `PUT /xc/workout/runner-result/put`
   - Awards: 15 points for logging workout results
   - Activity Type: `WORKOUT`

3. **CrossTrainingController**
   - Endpoint: `POST /cross-training-record/create`
   - Awards: 8 points for logging cross training
   - Activity Type: `CROSS_TRAINING`

4. **MeetLogController**
   - Endpoint: `POST /xc/meet/log/create`
   - Awards: 20 points for logging meet performance
   - Activity Type: `MEET_LOG`

   - Endpoint: `POST /xc/meet/log/pre-meet/create`
   - Awards: 5 points for logging pre-meet prep
   - Activity Type: `PRE_MEET_LOG`

All integrations include:
- Try-catch error handling (won't fail activity creation if points fail)
- UUID-based duplicate detection (same activity won't award points twice)
- Proper activity type tagging for transaction history

## Configuration Values (Seeded in Database)

### Points Earning:
- `EARN_TRAINING_RUN`: 10 points
- `EARN_WORKOUT`: 15 points
- `EARN_CROSS_TRAINING`: 8 points
- `EARN_MEET_LOG`: 20 points
- `EARN_PRE_MEET_LOG`: 5 points

### Points Spending (for future iOS implementation):
- `COST_SEND_GIF`: 5 points
- `COST_PIN_MESSAGE`: 10 points
- `COST_MESSAGE_ANIMATION`: 3 points
- `COST_CHANGE_CHAT_NAME`: 50 points
- `COST_CUSTOM_AVATAR`: 100 points

## Architecture Highlights

### Key Features:
1. **Backend-Driven Configuration** - Point values stored in database, cached in memory (5-min refresh)
2. **Transaction Safety** - Uses `REPEATABLE_READ` isolation level to prevent race conditions
3. **Duplicate Prevention** - Activity UUIDs prevent double-awarding points for same activity
4. **Audit Trail** - Full transaction history with timestamps and descriptions
5. **Graceful Degradation** - Points failures don't block activity logging
6. **Refund Support** - Failed actions can be refunded via transaction IDs

### Security:
- All endpoints are public read (as per requirements)
- Future: Admin-only configuration updates should be secured
- Database-level locking prevents concurrent modification issues

## Deployment Steps

### 1. Run Database Migration
```bash
mysql -u admin -p -h uaxctf-database.cozeoatutewd.us-east-2.rds.amazonaws.com uaxc < db-migrations/add_points_gamification.sql
```

### 2. Build Project
```bash
mvn clean package
```

### 3. Deploy to AWS
Deploy the updated JAR to your AWS environment (EC2/Elastic Beanstalk/etc.)

### 4. Verify Deployment
Test the new endpoints:
```bash
# Get configuration
curl https://your-api.com/api/v1/points/config

# Get leaderboard
curl https://your-api.com/api/v1/points/leaderboard?limit=10

# Get balance for runner ID 1
curl https://your-api.com/api/v1/points/balance?runnerId=1
```

## What's NOT Implemented (Future Work)

### Phase 2: iOS Client (Not in scope for backend-only task)
- iOS PointsManager
- iOS UI components
- iOS DataService integration

### Phase 4-7: Chat Features (Future Backend Work)
- GIF sending integration
- Message pinning
- Chat name changes
- Message animations
- Custom avatar upload

### Phase 8: Testing & Polish
- **Unit Tests** - Service layer tests
- **Integration Tests** - API endpoint tests
- **Performance Tests** - Load testing for concurrent point operations

### Additional Improvements:
- Logging framework integration (currently using `e.printStackTrace()`)
- Metrics/monitoring (Prometheus, CloudWatch)
- Rate limiting on points endpoints
- Admin authentication for configuration updates
- Season-based point resets
- Daily login bonuses
- Streak multipliers

## Testing Locally

### Manual Testing Workflow:

1. **Create a training run** via existing endpoint
   ```
   POST /xc/runners-training-run/create
   ```
   - Should automatically award 10 points
   - Check transaction table for entry

2. **Check balance**
   ```
   GET /api/v1/points/balance?runnerId=<id>
   ```

3. **View transaction history**
   ```
   GET /api/v1/points/transactions?runnerId=<id>
   ```

4. **Test validation**
   ```
   POST /api/v1/points/validate
   Body: { "runnerId": 1, "featureType": "SEND_GIF" }
   ```

5. **Test spending**
   ```
   POST /api/v1/points/spend
   Body: { "runnerId": 1, "featureType": "SEND_GIF", "description": "Test" }
   ```

6. **View leaderboard**
   ```
   GET /api/v1/points/leaderboard?limit=20
   ```

## Database Schema Summary

### `runners` table additions:
- `points` INT DEFAULT 0
- `lifetime_points_earned` INT DEFAULT 0
- `last_points_updated` TIMESTAMP

### `point_transactions` table:
- Tracks all earning, spending, and refund transactions
- Links to runner via `runner_id`
- Contains activity UUID for duplicate detection
- Stores balance snapshot after each transaction

### `points_configuration` table:
- Backend-driven configuration
- Allows tuning point economy without app updates
- Cached in memory with periodic refresh

## Notes for iOS Team

When implementing the iOS client, you'll need to:

1. **Fetch Configuration on App Launch**
   ```
   GET /api/v1/points/config
   ```
   Cache these values locally and refresh periodically

2. **Display Balance in UI**
   ```
   GET /api/v1/points/balance?runnerId=<id>
   ```
   Show in navigation bar or profile

3. **Show Toast After Activity**
   After successfully logging an activity, fetch updated balance and show "+X Points Earned" toast

4. **Pre-Validate Before Features**
   Before showing GIF picker or premium features:
   ```
   POST /api/v1/points/validate
   ```
   Disable UI if insufficient points

5. **Spend → Action → Refund Pattern**
   ```
   1. POST /api/v1/points/spend
   2. Perform action (e.g., send to Firebase)
   3. If action fails: POST /api/v1/points/refund
   ```

## Success Metrics to Track

Once deployed, monitor:
- Activity logging rate increase
- Points balance distribution
- Transaction volume by type
- Feature usage (once chat features implemented)
- API latency (target <100ms for validation)
- Failed transaction rate

## Questions or Issues?

- All code follows existing Kotlin/Spring patterns in the codebase
- Uses same package structure (note the typo: `statisitcs` not `statistics`)
- Follows existing controller/service/repository/model architecture
- Ready for staging deployment and testing

---

**Implementation Status**: ✅ Complete for Phases 1-3
**Next Steps**: Run database migration → Deploy to staging → Test endpoints → Coordinate with iOS team for Phase 2