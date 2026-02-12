# 5. 시스템 시퀀스 다이어그램 (System Sequence Diagrams)

모든 핵심 기능(회원가입, 인증, 조회, 어드민 등록)에 대해 **객체의 역할과 책임(Responsibility)**이 명확히 드러나도록 시퀀스 다이어그램을 작성했습니다.

단순한 `Service` 하나가 모든 일을 다 하는 것이 아니라, **인증(Authentication), 값 객체 검증(Value Object), 암호화(Encoder), 조회(Query)** 등의 책임이 분리된 구조입니다.

| Flow | 핵심 책임 |
|------|-----------|
| User Flow | 회원가입, 헤더 기반 인증, 정보 조회, 비밀번호 변경 |
| Read Flow | 데이터 조회와 DTO 변환 |
| Write Flow (Admin) | 권한 체크와 데이터 무결성(참조 관계) |
| Order Flow | 재고/결제/스냅샷의 트랜잭션 |
| Coupon Flow | 동시성 제어(선착순) |

---

## 5-1. 회원 기능 (User Flow)

**핵심 책임 객체:**

| 객체 | 책임 |
|------|------|
| `UserController` | HTTP 요청 수신 및 UseCase 위임 |
| `UserRegisterService` | 회원가입 오케스트레이션 (값 객체 검증, 중복 확인, 암호화, 저장) |
| `AuthenticationService` | 헤더 기반 인증 (사용자 조회, 비밀번호 매칭) |
| `UserQueryService` | 사용자 정보 조회 및 이름 마스킹 |
| `PasswordUpdateService` | 비밀번호 변경 (기존 검증, 신규 검증, 암호화, 저장) |
| `PasswordEncoder` | 비밀번호 암호화 및 매칭 (SHA-256) |
| `UserRepository` | 중복 ID 체크 및 사용자 영속화 |

### Scenario 1 — 회원가입 (Register)

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant API as 🌐 UserController
    participant Service as 📦 UserRegisterService
    participant VO as 🔒 Value Objects
    participant Encoder as 🛡️ PasswordEncoder
    participant DB as 💾 UserRepository

    User->>API: POST /api/v1/users/register (loginId, password, name, birthday, email)
    API->>Service: register(loginId, name, rawPassword, birthday, email)

    rect rgb(240, 248, 255)
        Note right of Service: [책임 1] 값 객체 검증
        Service->>VO: UserId.of(loginId), UserName.of(name), Birthday.of(birthday), Email.of(email), Password.of(rawPassword, birthday)
        alt 검증 실패 (형식 불일치, 생년월일 포함 등)
            VO-->>Service: throw IllegalArgumentException
            Service-->>API: 예외 전파
            API-->>User: 400 Bad Request
        else 검증 통과
            VO-->>Service: Value Objects
        end
    end

    rect rgb(255, 240, 245)
        Note right of Service: [책임 2] 중복 확인
        Service->>DB: existsById(userId)
        alt ID 중복
            DB-->>Service: true
            Service-->>API: throw IllegalArgumentException("이미 사용중인 ID 입니다.")
            API-->>User: 400 Bad Request
        else ID 사용 가능
            DB-->>Service: false
        end
    end

    rect rgb(255, 250, 205)
        Note right of Service: [책임 3] 암호화
        Service->>Encoder: encrypt(rawPassword)
        Encoder-->>Service: salt:hashedPassword
    end

    rect rgb(240, 255, 240)
        Note right of Service: [책임 4] 도메인 객체 생성 및 저장
        Service->>Service: User.register(userId, userName, encodedPassword, birth, email, wrongPasswordCount, now)
        Service->>DB: save(User)
        DB-->>Service: User
    end

    Service-->>API: void
    API-->>User: 200 OK
```

### Scenario 2 — 내 정보 조회 (Get My Info)

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant API as 🌐 UserController
    participant Auth as 🔐 AuthenticationService
    participant Query as 🔍 UserQueryService
    participant Encoder as 🛡️ PasswordEncoder
    participant DB as 💾 UserRepository

    User->>API: GET /api/v1/users/me (Header: X-Loopers-LoginId, X-Loopers-LoginPw)

    alt 필수 헤더 누락
        API-->>User: 400 Bad Request ("필수 헤더가 누락되었습니다")
    end

    rect rgb(255, 230, 230)
        Note right of API: [책임 1] 헤더 기반 인증
        API->>Auth: authenticate(userId, rawPassword)
        Auth->>DB: findById(userId)
        alt 유저 없음
            DB-->>Auth: Optional.empty()
            Auth-->>API: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")
            API-->>User: 400 Bad Request
        else 유저 존재
            DB-->>Auth: User
        end
        Auth->>Encoder: matches(rawPassword, encodedPassword)
        alt 비밀번호 불일치
            Encoder-->>Auth: false
            Auth-->>API: throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
            API-->>User: 400 Bad Request
        else 비밀번호 일치
            Encoder-->>Auth: true
        end
    end

    rect rgb(240, 248, 255)
        Note right of API: [책임 2] 사용자 정보 조회
        API->>Query: getUserInfo(userId)
        Query->>DB: findById(userId)
        DB-->>Query: User
        Note right of Query: 이름 마스킹: "홍길동" → "홍길*"
        Query-->>API: UserInfoResponse(loginId, maskedName, birthday, email)
    end

    API->>API: UserInfoResponse.from(userInfo) — birthday → "yyyyMMdd" 포맷
    API-->>User: 200 OK (JSON)
```

### Scenario 3 — 비밀번호 변경 (Update Password)

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant API as 🌐 UserController
    participant Auth as 🔐 AuthenticationService
    participant Service as 🔑 PasswordUpdateService
    participant VO as 🔒 Value Objects
    participant Encoder as 🛡️ PasswordEncoder
    participant DB as 💾 UserRepository

    User->>API: PUT /api/v1/users/me/password (Header: X-Loopers-LoginId, X-Loopers-LoginPw, Body: currentPassword, newPassword)

    rect rgb(255, 230, 230)
        Note right of API: [책임 1] 헤더 기반 인증
        API->>Auth: authenticate(userId, rawPassword)
        Auth->>DB: findById(userId)
        Auth->>Encoder: matches(rawPassword, encodedPassword)
        alt 인증 실패
            Auth-->>API: throw IllegalArgumentException
            API-->>User: 400 Bad Request
        end
    end

    API->>Service: updatePassword(userId, currentPassword, newPassword)

    rect rgb(240, 248, 255)
        Note right of Service: [책임 2] 사용자 조회 및 비밀번호 값 객체 검증
        Service->>DB: findById(userId)
        DB-->>Service: User
        Service->>VO: Password.of(currentRawPassword, birthday), Password.of(newRawPassword, birthday)
        alt 비밀번호 형식 오류 또는 생년월일 포함
            VO-->>Service: throw IllegalArgumentException
            Service-->>API: 예외 전파
            API-->>User: 400 Bad Request
        end
    end

    rect rgb(255, 240, 245)
        Note right of Service: [책임 3] 비밀번호 검증
        Service->>Encoder: matches(currentPassword, encodedPassword)
        alt 현재 비밀번호 불일치
            Encoder-->>Service: false
            Service-->>API: throw IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.")
            API-->>User: 400 Bad Request
        end

        Service->>Encoder: matches(newPassword, encodedPassword)
        alt 새 비밀번호가 기존과 동일
            Encoder-->>Service: true
            Service-->>API: throw IllegalArgumentException("현재 비밀번호는 사용할 수 없습니다.")
            API-->>User: 400 Bad Request
        end
    end

    rect rgb(240, 255, 240)
        Note right of Service: [책임 4] 암호화 및 저장
        Service->>Encoder: encrypt(newPassword)
        Encoder-->>Service: salt:hashedPassword
        Service->>Service: user.changePassword(encodedNewPassword)
        Service->>DB: save(updatedUser)
        DB-->>Service: User
    end

    Service-->>API: void
    API-->>User: 200 OK
```

---

## 5-2. 브랜드 및 상품 조회 (Public Read Flow)

**핵심 책임 객체:**

| 객체 | 책임 |
|------|------|
| `QueryHandler` | 복잡한 검색/필터링 쿼리 처리 (QueryDSL 등) |
| `DtoMapper` | 엔티티 → API 응답 객체 변환 (민감 정보 제외, 포맷팅) |

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 User
    participant API as 🌐 ProductController
    participant Service as 🛍️ ProductService
    participant Query as 🔍 QueryHandler
    participant Mapper as 🎨 DtoMapper
    participant DB as 💾 Repository

    Note over User, API: 인증 불필요 (Public API)

    User->>API: GET /products?brandId=1&sort=latest&page=0
    API->>Service: getProductList(filterCondition)

    rect rgb(240, 248, 255)
        Note right of Service: [책임 1] 데이터 조회
        Service->>Query: search(brandId, sort, page)
        Query->>DB: Dynamic Select Query
        DB-->>Query: List<ProductEntity>
        Query-->>Service: List<ProductEntity>
    end

    rect rgb(255, 240, 245)
        Note right of Service: [책임 2] 응답 데이터 가공
        Service->>Mapper: toSummaryDtoList(entities)
        Note right of Mapper: 품절 여부 계산, 이미지 URL 매핑
        Mapper-->>Service: List<ProductSummaryDto>
    end

    Service-->>API: PageResponse<ProductSummaryDto>
    API-->>User: 200 OK (JSON)
```

---

## 5-3. 브랜드 및 상품 등록 (Admin Write Flow)

**핵심 책임 객체:**

| 객체 | 책임 |
|------|------|
| `AdminGuard` | 관리자 권한 및 헤더 검증 (AOP/Interceptor) |
| `ImageUploader` | 이미지 파일 외부 저장소 업로드 (S3 등) |
| `CatalogService` | 브랜드 유효성 검증 및 상품 등록 오케스트레이션 |

```mermaid
sequenceDiagram
    autonumber
    actor Admin as 👨‍💼 Admin
    participant API as 🌐 AdminController
    participant Guard as 👮 AdminGuard
    participant Service as 📦 CatalogService
    participant Uploader as ☁️ ImageUploader
    participant DB as 💾 Repository

    Note over Admin, API: Header: X-Loopers-Ldap

    Admin->>API: POST /admin/products (Info, Images, BrandId)

    rect rgb(255, 230, 230)
        Note right of API: [책임 1] 관리자 권한 검증
        API->>Guard: checkAdminHeader(request)
        alt 권한 없음
            Guard-->>API: throw UnauthorizedException
            API-->>Admin: 403 Forbidden
        else 권한 확인됨
            Guard-->>API: AdminInfo
        end
    end

    API->>Service: registerProduct(dto, files)

    rect rgb(240, 248, 255)
        Note right of Service: [책임 2] 비즈니스 유효성 검사
        Service->>DB: existsBrand(brandId)
        alt 브랜드 없음
            DB-->>Service: false
            Service-->>API: throw InvalidBrandException
            API-->>Admin: 400 Bad Request
        else 브랜드 존재
            DB-->>Service: true
        end
    end

    rect rgb(255, 250, 205)
        Note right of Service: [책임 3] 리소스(이미지) 처리
        Service->>Uploader: uploadImages(files)
        Uploader-->>Service: List<ImageUrl>
    end

    rect rgb(240, 255, 240)
        Note right of Service: [책임 4] 데이터 영속화
        Service->>DB: save(ProductEntity + ImageEntities)
        DB-->>Service: Product ID
    end

    Service-->>API: ProductResponse
    API-->>Admin: 201 Created
```

---