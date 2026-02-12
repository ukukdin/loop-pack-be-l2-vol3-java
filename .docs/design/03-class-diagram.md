# 6. 도메인 객체 설계 (Class Diagram)

클린 아키텍처 기반으로 **도메인 계층이 어떤 외부 기술에도 의존하지 않도록** 설계했습니다.

```
Interfaces → Application → Domain ← Infrastructure
```

---

## 6-1. User 도메인 (현재 구현)

### 전체 구조

```mermaid
classDiagram
    direction TB

    %% === Interfaces ===
    class UserController {
        -RegisterUseCase registerUseCase
        -AuthenticationUseCase authenticationUseCase
        -UserQueryUseCase userQueryUseCase
        -PasswordUpdateUseCase passwordUpdateUseCase
        +register(UserRegisterRequest) ResponseEntity
        +getMyInfo(loginId, loginPw) ResponseEntity
        +updatePassword(loginId, loginPw, PasswordUpdateRequest) ResponseEntity
    }

    %% === Application ===
    class RegisterUseCase {
        <<interface>>
        +register(loginId, name, rawPassword, birthday, email)
    }
    class AuthenticationUseCase {
        <<interface>>
        +authenticate(userId, rawPassword)
    }
    class UserQueryUseCase {
        <<interface>>
        +getUserInfo(userId) UserInfoResponse
    }
    class PasswordUpdateUseCase {
        <<interface>>
        +updatePassword(userId, currentRawPassword, newRawPassword)
    }
    class UserService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        +register()
        +authenticate()
        +getUserInfo()
        +updatePassword()
        -findUser(UserId) User
        -maskName(String) String
    }

    %% === Domain ===
    class User {
        <<Aggregate Root>>
        -Long id
        -UserId userId
        -UserName userName
        -String encodedPassword
        -Birthday birth
        -Email email
        -WrongPasswordCount wrongPasswordCount
        -LocalDateTime createdAt
        +register(...)$ User
        +reconstitute(...)$ User
        +changePassword(String) User
    }
    class UserRepository {
        <<interface>>
        +save(User) User
        +findById(UserId) Optional~User~
    }
    class PasswordEncoder {
        <<interface>>
        +encrypt(String) String
        +matches(String, String) boolean
    }

    %% === Infrastructure ===
    class UserRepositoryImpl {
        +save(User) User
        +findById(UserId) Optional~User~
    }
    class Sha256PasswordEncoder {
        +encrypt(String) String
        +matches(String, String) boolean
    }

    %% --- 관계 ---
    UserController --> RegisterUseCase
    UserController --> AuthenticationUseCase
    UserController --> UserQueryUseCase
    UserController --> PasswordUpdateUseCase

    UserService ..|> RegisterUseCase
    UserService ..|> AuthenticationUseCase
    UserService ..|> UserQueryUseCase
    UserService ..|> PasswordUpdateUseCase
    UserService --> UserRepository
    UserService --> PasswordEncoder

    UserRepositoryImpl ..|> UserRepository
    Sha256PasswordEncoder ..|> PasswordEncoder
```

### UserService 메서드별 책임

| 메서드 | UseCase | 트랜잭션 | 핵심 로직 |
|---|---|---|---|
| `register()` | `RegisterUseCase` | `@Transactional` | 값 객체 검증 → 암호화 → 저장 (중복 시 예외) |
| `authenticate()` | `AuthenticationUseCase` | `readOnly` | 사용자 조회 → 비밀번호 매칭 |
| `getUserInfo()` | `UserQueryUseCase` | `readOnly` | 사용자 조회 → 이름 마스킹 |
| `updatePassword()` | `PasswordUpdateUseCase` | `@Transactional` | 기존 PW 검증 → 신규 PW 검증 → 암호화 → 저장 |

### API 엔드포인트

| Method | Path | 인증 |
|---|---|---|
| `POST` | `/api/v1/users/register` | 불필요 |
| `GET` | `/api/v1/users/me` | `X-Loopers-LoginId`, `X-Loopers-LoginPw` |
| `PUT` | `/api/v1/users/me/password` | `X-Loopers-LoginId`, `X-Loopers-LoginPw` |

---

## 6-2. Value Objects & 검증 규칙

```mermaid
classDiagram
    User *-- UserId
    User *-- UserName
    User *-- Birthday
    User *-- Email
    User *-- WrongPasswordCount
    User ..> Password : register/updatePassword에서 사용

    class UserId {
        -String value
        +of(String)$ UserId
    }
    class UserName {
        -String value
        +of(String)$ UserName
    }
    class Password {
        -String value
        +of(String, LocalDate)$ Password
    }
    class Email {
        -String value
        +of(String)$ Email
    }
    class Birthday {
        -LocalDate value
        +of(LocalDate)$ Birthday
    }
    class WrongPasswordCount {
        -int value
        +init()$ WrongPasswordCount
        +increment() WrongPasswordCount
        +isLocked() boolean
    }
```

| Value Object | 검증 규칙 | 예외 메시지 |
|---|---|---|
| `UserId` | 4~10자, 영문 소문자+숫자만 | `로그인 ID는 4~10자의 영문 소문자, 숫자만 가능합니다.` |
| `UserName` | 2~20자, 한글/영문/숫자 | `이름은 2~20자의 한글 또는 영문만 가능합니다.` |
| `Password` | 8~16자, 영문+숫자+특수문자, 생년월일 포함 불가 | `비밀번호는 8~16자리 영문 대소문자, 숫자, 특수문자만 가능합니다.` |
| `Email` | 이메일 형식 정규식 | `올바른 이메일 형식이 아닙니다` |
| `Birthday` | not null, 미래 불가, 1900년 이후 | `생년월일은 미래 날짜일 수 없습니다.` |
| `WrongPasswordCount` | 음수 불가, 5회 이상 잠금 | `비밀번호 오류 횟수는 음수일 수 없습니다.` |

### 설계 결정

- **`User.register()`**: id = null로 생성 (영속화 전 신규 객체)
- **`User.reconstitute()`**: DB에서 복원할 때 사용 (id 포함)
- **`User.changePassword()`**: 새로운 User 인스턴스 반환 (불변성 유지)

---

## 6-3. Infrastructure 계층

```mermaid
classDiagram
    UserRepositoryImpl ..|> UserRepository
    UserRepositoryImpl --> UserJpaRepository
    UserRepositoryImpl ..> UserJpaEntity
    Sha256PasswordEncoder ..|> PasswordEncoder

    class UserRepositoryImpl {
        +save(User) User
        +findById(UserId) Optional~User~
        -toEntity(User) UserJpaEntity
        -toDomain(UserJpaEntity) User
    }
    class UserJpaRepository {
        <<interface>>
        +findByUserId(String) Optional~UserJpaEntity~
        +existsByUserId(String) boolean
    }
    class UserJpaEntity {
        -Long id
        -String userId
        -String encodedPassword
        -String username
        -LocalDate birthday
        -String email
        -LocalDateTime createdAt
    }
    class Sha256PasswordEncoder {
        +encrypt(String) String
        +matches(String, String) boolean
    }
```

**변환 흐름**: `User` → `toEntity()` → `UserJpaEntity` → JPA save → `toDomain()` → `User`

**암호화 형식**: `salt:hash` (SHA-256 + Base64 Salt)

---

## 6-4. 에러 처리

| 예외 | HTTP 상태 | 발생 위치 |
|---|---|---|
| `IllegalArgumentException` | 400 | Value Object 검증, Service 비즈니스 검증 |
| `MethodArgumentNotValidException` | 400 | DTO `@Valid` 검증 |
| `MissingRequestHeaderException` | 400 | 필수 헤더 누락 |
| `CoreException` | ErrorType에 따름 | 명시적 도메인 예외 |
| `Exception` | 500 | 예상치 못한 서버 오류 |

---

## 6-5. 의존성 방향 요약

```
┌─────────────────────────────────────────────────────┐
│  Interfaces (Controller, DTO)                       │
│    └─ 의존 → UseCase 인터페이스 (Application 계층)    │
├─────────────────────────────────────────────────────┤
│  Application (UseCase, UserService)                 │
│    └─ 의존 → Domain 인터페이스 (Repository, Encoder) │
├─────────────────────────────────────────────────────┤
│  Domain (User, Value Objects, Interface)            │
│    └─ 외부 의존 없음 (순수 Java)                      │
├─────────────────────────────────────────────────────┤
│  Infrastructure (JPA, SHA-256)                      │
│    └─ 의존 → Domain 인터페이스를 구현                  │
└─────────────────────────────────────────────────────┘
```

---

# 향후 확장 도메인 설계 (미래 목표)

> `01-requirements.md`에 정의된 기능 요구사항 기반의 **미래 구현 목표**입니다.

## 6-6. 전체 도메인 관계도

```mermaid
classDiagram
    direction TB

    class User { <<Aggregate Root>> }
    class Brand { <<Aggregate Root>> }
    class Product { <<Aggregate Root>> }
    class Like { <<Entity>> }
    class Order { <<Aggregate Root>> }
    class OrderItem { <<Entity>> }
    class OrderSnapshot { <<Value Object>> }

    User "1" --> "*" Like : 좋아요
    User "1" --> "*" Order : 주문
    Brand "1" --> "*" Product : 보유 상품
    Product "1" --> "*" Like : 좋아요 대상
    Product "1" --> "*" OrderItem : 주문 항목
    Order "1" *-- "*" OrderItem : 주문 상세
    Order "1" *-- "1" OrderSnapshot : 주문 시점 스냅샷
```

---

## 6-7. Brand 도메인

```mermaid
classDiagram
    Brand *-- BrandName

    class Brand {
        <<Aggregate Root>>
        -Long id
        -BrandName name
        -String description
        -LocalDateTime createdAt
        +register(name, description)$ Brand
        +reconstitute(...)$ Brand
        +update(BrandName, String) Brand
    }
    class BrandName {
        <<Value Object>>
        -String value
        +of(String)$ BrandName
    }
```

| Role | Method | Path | UseCase |
|---|---|---|---|
| Any | `GET` | `/api/v1/brands/{brandId}` | `BrandQueryUseCase` |
| Admin | `GET` | `/api-admin/v1/brands` | `AdminBrandUseCase` |
| Admin | `POST` | `/api-admin/v1/brands` | `AdminBrandUseCase` |
| Admin | `PUT` | `/api-admin/v1/brands/{id}` | `AdminBrandUseCase` |
| Admin | `DELETE` | `/api-admin/v1/brands/{id}` | `AdminBrandUseCase` (하위 상품 Cascade) |

---

## 6-8. Product 도메인

```mermaid
classDiagram
    Product *-- ProductName
    Product *-- Money
    Product *-- StockQuantity
    Product *-- "0..*" ProductImage

    class Product {
        <<Aggregate Root>>
        -Long id
        -Long brandId
        -ProductName name
        -Money price
        -StockQuantity stockQuantity
        -String description
        -List~ProductImage~ images
        -int likeCount
        +register(...)$ Product
        +decreaseStock(int) Product
        +isOutOfStock() boolean
    }
    class Money {
        <<Value Object>>
        -int value
        +of(int)$ Money
    }
    class StockQuantity {
        <<Value Object>>
        -int value
        +decrease(int) StockQuantity
        +isZero() boolean
    }
```

| Role | Method | Path |
|---|---|---|
| Any | `GET` | `/api/v1/products?brandId=&sort=&page=&size=` |
| Any | `GET` | `/api/v1/products/{productId}` |
| Admin | `POST` | `/api-admin/v1/products` |
| Admin | `PUT` | `/api-admin/v1/products/{id}` |
| Admin | `DELETE` | `/api-admin/v1/products/{id}` |

정렬: `latest` (기본) | `price_asc` | `likes_desc`

---

## 6-9. Like 도메인

```mermaid
classDiagram
    class Like {
        <<Entity>>
        -Long id
        -UserId userId
        -Long productId
        -LocalDateTime createdAt
        +create(userId, productId)$ Like
    }
    class LikeRepository {
        <<interface>>
        +save(Like) Like
        +delete(UserId, Long) void
        +existsByUserIdAndProductId(UserId, Long) boolean
    }
```

- **멱등성**: 이미 좋아요한 상품에 다시 좋아요 → 예외 없이 무시
- **유저당 1상품 1좋아요**: `UNIQUE(user_id, product_id)` 제약
- **좋아요 수 동기화**: `Like` 생성/삭제 시 `Product.likeCount` 증감

| Role | Method | Path |
|---|---|---|
| User | `POST` | `/api/v1/products/{id}/likes` |
| User | `DELETE` | `/api/v1/products/{id}/likes` |
| User | `GET` | `/api/v1/users/me/likes` |

---

## 6-10. Order 도메인

```mermaid
classDiagram
    Order *-- "1..*" OrderItem
    Order *-- "1" OrderSnapshot
    Order *-- "1" ShippingInfo
    Order *-- "1" PaymentMethod
    Order --> OrderStatus

    class Order {
        <<Aggregate Root>>
        -Long id
        -UserId userId
        -Money totalAmount
        -Money discountAmount
        -Money paymentAmount
        -OrderStatus status
        +create(...)$ Order
        +cancel() Order
        +isCancellable() boolean
    }
    class OrderItem {
        <<Entity>>
        -Long productId
        -int quantity
        -Money price
    }
    class OrderStatus {
        <<enum>>
        PAYMENT_COMPLETED
        PREPARING
        SHIPPING
        DELIVERED
        CANCELLED
    }
    class OrderSnapshot {
        <<Value Object>>
        -String snapshotData
        +capture(...)$ OrderSnapshot
    }
```

**주문 생성 프로세스**: 재고 확인 → 재고 차감 → 결제 금액 검증 → 스냅샷 생성 → 주문 생성

| 상태 | 주문 취소 | 배송지 변경 |
|---|---|---|
| `PAYMENT_COMPLETED` | 가능 | 가능 |
| `PREPARING` | 가능 | 가능 |
| `SHIPPING` | 불가 | 불가 |
| `DELIVERED` | 불가 | 불가 |

| Role | Method | Path |
|---|---|---|
| User | `POST` | `/api/v1/orders` |
| User | `GET` | `/api/v1/orders/me` |
| User | `GET` | `/api/v1/orders/{id}` |
| Admin | `GET` | `/api-admin/v1/orders` |

---

## 6-11. Admin 인증

관리자 API는 `X-Loopers-Ldap` 헤더로 권한 검증합니다.

| 규칙 | 설명 |
|---|---|
| Admin 인증 | `X-Loopers-Ldap: loopers.admin` 헤더 필수 |
| User 접근 차단 | `/api-admin/**` 호출 시 403 Forbidden |
| 타 유저 접근 차단 | 유저는 자신의 정보만 조회 가능 |
