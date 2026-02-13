# 향후 도메인 확장 클래스 다이어그램

요구사항 명세서(01-requirements.md)에 정의된 브랜드, 상품, 좋아요, 주문 도메인의 설계입니다.
현재 User 도메인과 동일한 클린 아키텍처 패턴(UseCase 인터페이스 + Service 구현 + Domain Port)을 적용합니다.

---

## 전체 도메인 맵

```mermaid
graph LR
    User[User 도메인] --> Like[Like 도메인]
    Brand[Brand 도메인] --> Product[Product 도메인]
    Product --> Like
    Product --> Order[Order 도메인]
    User --> Order
    Coupon[Coupon 도메인] --> Order

    style User fill:#e8f5e9,stroke:#43a047,color:#000
    style Brand fill:#e3f2fd,stroke:#1e88e5,color:#000
    style Product fill:#fff3e0,stroke:#ef6c00,color:#000
    style Like fill:#fce4ec,stroke:#c62828,color:#000
    style Order fill:#f3e5f5,stroke:#7b1fa2,color:#000
    style Coupon fill:#fffde7,stroke:#fbc02d,color:#000
```

---

## Part 1. Brand 도메인

### 1-1. Interfaces → Application

```mermaid
classDiagram
    direction LR

    %% Interfaces
    class BrandAdminController {
        <<RestController>>
        -CreateBrandUseCase createBrandUseCase
        -UpdateBrandUseCase updateBrandUseCase
        -DeleteBrandUseCase deleteBrandUseCase
        -BrandQueryUseCase brandQueryUseCase
        +createBrand(BrandCreateRequest) ResponseEntity
        +updateBrand(Long, BrandUpdateRequest) ResponseEntity
        +deleteBrand(Long) ResponseEntity
        +getBrands() ResponseEntity
    }
    class BrandController {
        <<RestController>>
        -BrandQueryUseCase brandQueryUseCase
        +getBrand(Long) ResponseEntity
    }

    %% Application
    class CreateBrandUseCase {
        <<interface>>
        +createBrand(String name, String description) void
    }
    class UpdateBrandUseCase {
        <<interface>>
        +updateBrand(Long brandId, String name, String description) void
    }
    class DeleteBrandUseCase {
        <<interface>>
        +deleteBrand(Long brandId) void
    }
    class BrandQueryUseCase {
        <<interface>>
        +getBrand(Long brandId) BrandInfo
        +getBrands() List~BrandInfo~
    }
    class BrandService {
        <<Service>>
        -BrandRepository brandRepository
        +createBrand(...) void
        +updateBrand(...) void
        +deleteBrand(...) void
        +getBrand(...) BrandInfo
        +getBrands() List~BrandInfo~
    }

    BrandAdminController ..> CreateBrandUseCase : uses
    BrandAdminController ..> UpdateBrandUseCase : uses
    BrandAdminController ..> DeleteBrandUseCase : uses
    BrandAdminController ..> BrandQueryUseCase : uses
    BrandController ..> BrandQueryUseCase : uses

    BrandService ..|> CreateBrandUseCase : implements
    BrandService ..|> UpdateBrandUseCase : implements
    BrandService ..|> DeleteBrandUseCase : implements
    BrandService ..|> BrandQueryUseCase : implements

    style BrandAdminController fill:#e3f2fd,stroke:#1e88e5,stroke-width:2px,color:#000
    style BrandController fill:#e3f2fd,stroke:#1e88e5,stroke-width:2px,color:#000
    style CreateBrandUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style UpdateBrandUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style DeleteBrandUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style BrandQueryUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style BrandService fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px,color:#000
```

### 1-2. Domain

```mermaid
classDiagram
    direction TB

    class Brand {
        <<Aggregate Root>>
        -Long id
        -BrandName name
        -String description
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -LocalDateTime deletedAt
        +create(BrandName, String) Brand$
        +update(BrandName, String) Brand
        +delete() Brand
        +isDeleted() boolean
    }
    class BrandName {
        <<Value Object>>
        -String value
        +of(String) BrandName$
    }
    class BrandRepository {
        <<interface>>
        <<Domain Port>>
        +save(Brand) Brand
        +findById(Long) Brand?
        +findAll() List~Brand~
        +deleteById(Long) void
    }

    Brand *-- "1" BrandName : -name
    BrandService --> BrandRepository : -brandRepository
    BrandService ..> Brand : uses

    class BrandService {
        <<Service>>
    }

    style Brand fill:#ffecb3,stroke:#ff6f00,stroke-width:3px,color:#000
    style BrandName fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style BrandRepository fill:#fffde7,stroke:#fdd835,stroke-width:2px,color:#000
    style BrandService fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px,color:#000
```

### 설계 포인트

- Admin과 User가 별도 Controller를 가진다. Admin은 CRUD 전체, User는 조회만 가능.
- Brand 삭제는 Soft Delete (`deletedAt` 설정). 하위 Product도 Cascade Soft Delete 처리 필요.
- `BrandName`은 Value Object로 중복 검증 로직을 캡슐화.

---

## Part 2. Product 도메인

### 2-1. Interfaces → Application

```mermaid
classDiagram
    direction LR

    %% Interfaces
    class ProductAdminController {
        <<RestController>>
        -CreateProductUseCase createProductUseCase
        -UpdateProductUseCase updateProductUseCase
        -DeleteProductUseCase deleteProductUseCase
        +createProduct(ProductCreateRequest) ResponseEntity
        +updateProduct(Long, ProductUpdateRequest) ResponseEntity
        +deleteProduct(Long) ResponseEntity
    }
    class ProductController {
        <<RestController>>
        -ProductQueryUseCase productQueryUseCase
        +getProducts(ProductSearchCondition) ResponseEntity
        +getProduct(Long) ResponseEntity
    }

    %% Application
    class CreateProductUseCase {
        <<interface>>
        +createProduct(Long brandId, String name, int price, int stock, String description) void
    }
    class UpdateProductUseCase {
        <<interface>>
        +updateProduct(Long productId, String name, int price, int stock, String description) void
    }
    class DeleteProductUseCase {
        <<interface>>
        +deleteProduct(Long productId) void
    }
    class ProductQueryUseCase {
        <<interface>>
        +getProducts(ProductSearchCondition) Page~ProductInfo~
        +getProduct(Long productId) ProductDetailInfo
    }
    class ProductService {
        <<Service>>
        -ProductRepository productRepository
        -BrandRepository brandRepository
        +createProduct(...) void
        +updateProduct(...) void
        +deleteProduct(...) void
    }
    class ProductQueryService {
        <<Service>>
        -ProductRepository productRepository
        +getProducts(...) Page~ProductInfo~
        +getProduct(...) ProductDetailInfo
    }

    ProductAdminController ..> CreateProductUseCase : uses
    ProductAdminController ..> UpdateProductUseCase : uses
    ProductAdminController ..> DeleteProductUseCase : uses
    ProductController ..> ProductQueryUseCase : uses

    ProductService ..|> CreateProductUseCase : implements
    ProductService ..|> UpdateProductUseCase : implements
    ProductService ..|> DeleteProductUseCase : implements
    ProductQueryService ..|> ProductQueryUseCase : implements

    style ProductAdminController fill:#e3f2fd,stroke:#1e88e5,stroke-width:2px,color:#000
    style ProductController fill:#e3f2fd,stroke:#1e88e5,stroke-width:2px,color:#000
    style CreateProductUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style UpdateProductUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style DeleteProductUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style ProductQueryUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style ProductService fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px,color:#000
    style ProductQueryService fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px,color:#000
```

### 2-2. Domain

```mermaid
classDiagram
    direction TB

    class Product {
        <<Aggregate Root>>
        -Long id
        -Long brandId
        -ProductName name
        -Price price
        -Stock stock
        -int likeCount
        -String description
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -LocalDateTime deletedAt
        +create(Long, ProductName, Price, Stock, String) Product$
        +update(ProductName, Price, Stock, String) Product
        +delete() Product
        +decreaseStock(int) Product
        +increaseLikeCount() Product
        +decreaseLikeCount() Product
    }
    class ProductName {
        <<Value Object>>
        -String value
        +of(String) ProductName$
    }
    class Price {
        <<Value Object>>
        -int value
        +of(int) Price$
    }
    class Stock {
        <<Value Object>>
        -int value
        +of(int) Stock$
        +decrease(int) Stock
        +hasEnough(int) boolean
    }
    class ProductImage {
        <<Entity>>
        -Long id
        -Long productId
        -String imageUrl
        -int sortOrder
    }
    class ProductRepository {
        <<interface>>
        <<Domain Port>>
        +save(Product) Product
        +findById(Long) Product?
        +findAll(ProductSearchCondition) Page~Product~
        +deleteById(Long) void
    }

    Product *-- "1" ProductName : -name
    Product *-- "1" Price : -price
    Product *-- "1" Stock : -stock
    Product o-- "0..*" ProductImage : -images

    style Product fill:#ffecb3,stroke:#ff6f00,stroke-width:3px,color:#000
    style ProductName fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style Price fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style Stock fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style ProductImage fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style ProductRepository fill:#fffde7,stroke:#fdd835,stroke-width:2px,color:#000
```

### 설계 포인트

- **Command/Query 분리**: `ProductService`(CUD)와 `ProductQueryService`(R)를 분리하여 읽기 최적화와 쓰기 트랜잭션을 독립시킨다.
- `brandId`는 Product가 Brand Aggregate를 직접 참조하지 않고 **ID 참조**로 연결. Aggregate 간 결합도를 낮춘다.
- `Stock` Value Object에 `decrease()`, `hasEnough()` 로직을 캡슐화하여 재고 관련 규칙이 도메인에 집중된다.
- `likeCount`는 비정규화 필드. LIKES 테이블과의 정합성은 서비스 레이어에서 트랜잭션으로 보장.
- Admin에서 상품 등록 시 `brandId` 존재 여부를 `BrandRepository`로 검증한다.
- 브랜드 변경 불가(`Immutable`) — `update()`에 brandId 파라미터 없음.

---

## Part 3. Like 도메인

### 3-1. Interfaces → Application

```mermaid
classDiagram
    direction LR

    class LikeController {
        <<RestController>>
        -LikeUseCase likeUseCase
        -UnlikeUseCase unlikeUseCase
        -LikeQueryUseCase likeQueryUseCase
        +like(HttpServletRequest, Long) ResponseEntity
        +unlike(HttpServletRequest, Long) ResponseEntity
        +getMyLikes(HttpServletRequest, LikeSearchCondition) ResponseEntity
    }

    class LikeUseCase {
        <<interface>>
        +like(UserId, Long productId) void
    }
    class UnlikeUseCase {
        <<interface>>
        +unlike(UserId, Long productId) void
    }
    class LikeQueryUseCase {
        <<interface>>
        +getMyLikes(UserId, LikeSearchCondition) List~LikeInfo~
    }
    class LikeService {
        <<Service>>
        -LikeRepository likeRepository
        -ProductRepository productRepository
        +like(...) void
        +unlike(...) void
        +getMyLikes(...) List~LikeInfo~
    }

    LikeController ..> LikeUseCase : uses
    LikeController ..> UnlikeUseCase : uses
    LikeController ..> LikeQueryUseCase : uses
    LikeService ..|> LikeUseCase : implements
    LikeService ..|> UnlikeUseCase : implements
    LikeService ..|> LikeQueryUseCase : implements

    style LikeController fill:#e3f2fd,stroke:#1e88e5,stroke-width:2px,color:#000
    style LikeUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style UnlikeUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style LikeQueryUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style LikeService fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px,color:#000
```

### 3-2. Domain

```mermaid
classDiagram
    direction TB

    class Like {
        <<Entity>>
        -Long id
        -UserId userId
        -Long productId
        -LocalDateTime createdAt
        +create(UserId, Long) Like$
    }
    class LikeRepository {
        <<interface>>
        <<Domain Port>>
        +save(Like) Like
        +findByUserIdAndProductId(UserId, Long) Like?
        +deleteByUserIdAndProductId(UserId, Long) void
        +findAllByUserId(UserId, LikeSearchCondition) List~Like~
        +existsByUserIdAndProductId(UserId, Long) boolean
    }

    Like ..> UserId : -userId

    style Like fill:#ffecb3,stroke:#ff6f00,stroke-width:3px,color:#000
    style LikeRepository fill:#fffde7,stroke:#fdd835,stroke-width:2px,color:#000
    style UserId fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
```

### 설계 포인트

- Like는 독립 Aggregate가 아닌 **Entity**. User와 Product 사이의 관계를 표현하되, 양쪽 Aggregate를 ID 참조로만 연결.
- **Idempotency**: `like()` 호출 시 이미 좋아요가 존재하면 예외 대신 무시 (또는 409 Conflict).
- Like 생성/삭제 시 `Product.likeCount`를 같은 트랜잭션에서 증감하여 정합성 보장.
- Controller는 `HttpServletRequest`에서 `authenticatedUserId`를 꺼내 사용 (Interceptor 패턴).

---

## Part 4. Order 도메인

### 4-1. Interfaces → Application

```mermaid
classDiagram
    direction LR

    class OrderController {
        <<RestController>>
        -CreateOrderUseCase createOrderUseCase
        -OrderQueryUseCase orderQueryUseCase
        +createOrder(HttpServletRequest, OrderCreateRequest) ResponseEntity
        +getMyOrders(HttpServletRequest, OrderSearchCondition) ResponseEntity
        +getOrder(HttpServletRequest, Long) ResponseEntity
    }
    class OrderAdminController {
        <<RestController>>
        -OrderQueryUseCase orderQueryUseCase
        +getAllOrders(OrderSearchCondition) ResponseEntity
    }

    class CreateOrderUseCase {
        <<interface>>
        +createOrder(UserId, OrderCommand) void
    }
    class OrderQueryUseCase {
        <<interface>>
        +getMyOrders(UserId, OrderSearchCondition) List~OrderSummary~
        +getOrder(UserId, Long orderId) OrderDetail
        +getAllOrders(OrderSearchCondition) List~OrderSummary~
    }
    class OrderService {
        <<Service>>
        -OrderRepository orderRepository
        -ProductRepository productRepository
        +createOrder(...) void
    }
    class OrderQueryService {
        <<Service>>
        -OrderRepository orderRepository
        +getMyOrders(...) List~OrderSummary~
        +getOrder(...) OrderDetail
        +getAllOrders(...) List~OrderSummary~
    }

    OrderController ..> CreateOrderUseCase : uses
    OrderController ..> OrderQueryUseCase : uses
    OrderAdminController ..> OrderQueryUseCase : uses

    OrderService ..|> CreateOrderUseCase : implements
    OrderQueryService ..|> OrderQueryUseCase : implements

    style OrderController fill:#e3f2fd,stroke:#1e88e5,stroke-width:2px,color:#000
    style OrderAdminController fill:#e3f2fd,stroke:#1e88e5,stroke-width:2px,color:#000
    style CreateOrderUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style OrderQueryUseCase fill:#e8f5e9,stroke:#43a047,stroke-width:2px,color:#000
    style OrderService fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px,color:#000
    style OrderQueryService fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px,color:#000
```

### 4-2. Domain

```mermaid
classDiagram
    direction TB

    class Order {
        <<Aggregate Root>>
        -Long id
        -UserId userId
        -ReceiverName receiverName
        -Address address
        -String deliveryRequest
        -PaymentMethod paymentMethod
        -Money totalAmount
        -Money discountAmount
        -Money paymentAmount
        -OrderStatus status
        -LocalDate desiredDeliveryDate
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        +create(UserId, List~OrderItemCommand~, DeliveryInfo, PaymentMethod) Order$
        +cancel() Order
        +updateDeliveryAddress(Address) Order
        +isCancellable() boolean
    }
    class OrderItem {
        <<Entity>>
        -Long id
        -Long productId
        -int quantity
        -Money unitPrice
    }
    class OrderSnapshot {
        <<Entity>>
        -Long id
        -String snapshotData
        -LocalDateTime createdAt
    }
    class OrderStatus {
        <<enumeration>>
        PAYMENT_COMPLETED
        PREPARING
        SHIPPING
        DELIVERED
    }
    class Money {
        <<Value Object>>
        -int value
        +of(int) Money$
        +add(Money) Money
        +subtract(Money) Money
        +multiply(int) Money
    }
    class ReceiverName {
        <<Value Object>>
        -String value
        +of(String) ReceiverName$
    }
    class Address {
        <<Value Object>>
        -String value
        +of(String) Address$
    }
    class PaymentMethod {
        <<enumeration>>
        CARD
        BANK_TRANSFER
    }
    class OrderRepository {
        <<interface>>
        <<Domain Port>>
        +save(Order) Order
        +findById(Long) Order?
        +findAllByUserId(UserId, OrderSearchCondition) List~Order~
        +findAll(OrderSearchCondition) List~Order~
    }

    Order *-- "1..*" OrderItem : -items
    Order *-- "0..1" OrderSnapshot : -snapshot
    Order *-- "1" OrderStatus : -status
    Order *-- "1" Money : -totalAmount
    Order *-- "1" Money : -discountAmount
    Order *-- "1" Money : -paymentAmount
    Order *-- "1" ReceiverName : -receiverName
    Order *-- "1" Address : -address
    Order *-- "1" PaymentMethod : -paymentMethod
    OrderItem *-- "1" Money : -unitPrice

    style Order fill:#ffecb3,stroke:#ff6f00,stroke-width:3px,color:#000
    style OrderItem fill:#fff9c4,stroke:#fbc02d,stroke-width:2px,color:#000
    style OrderSnapshot fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style OrderStatus fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style Money fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style ReceiverName fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style Address fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style PaymentMethod fill:#fff9c4,stroke:#fbc02d,stroke-width:1px,color:#000
    style OrderRepository fill:#fffde7,stroke:#fdd835,stroke-width:2px,color:#000
```

### 설계 포인트

- **Order가 Aggregate Root**, OrderItem과 OrderSnapshot은 Order 생명주기에 종속된 Entity.
- **주문 생성 프로세스**: 재고 확인 → 재고 차감 → (쿠폰 적용) → 금액 검증 → 주문 생성. 이 전체가 하나의 트랜잭션.
- `OrderItem.unitPrice`는 주문 시점 스냅샷. `Product.price` 변경에 영향받지 않음.
- `OrderSnapshot`은 주문 시점의 전체 상품 정보를 JSON으로 보관.
- **상태 전이 규칙**: `PAYMENT_COMPLETED`/`PREPARING`에서만 취소/배송지 변경 가능. `SHIPPING`/`DELIVERED`에서는 불가.
- `Money` Value Object로 금액 연산을 캡슐화하여 금액 관련 실수 방지.

---

## Part 5. 도메인 간 관계 종합

```mermaid
classDiagram
    direction TB

    class User {
        <<Aggregate Root>>
        -UserId userId
    }
    class Brand {
        <<Aggregate Root>>
        -Long id
        -BrandName name
    }
    class Product {
        <<Aggregate Root>>
        -Long id
        -Long brandId
        -ProductName name
        -Price price
        -Stock stock
    }
    class Like {
        <<Entity>>
        -UserId userId
        -Long productId
    }
    class Order {
        <<Aggregate Root>>
        -Long id
        -UserId userId
        -OrderStatus status
    }
    class OrderItem {
        <<Entity>>
        -Long productId
        -int quantity
        -Money unitPrice
    }

    %% Aggregate 간 관계 (ID 참조)
    Product ..> Brand : brandId 참조
    Like ..> User : userId 참조
    Like ..> Product : productId 참조
    Order ..> User : userId 참조
    Order *-- "1..*" OrderItem : 합성
    OrderItem ..> Product : productId 참조

    style User fill:#e8f5e9,stroke:#43a047,stroke-width:3px,color:#000
    style Brand fill:#e3f2fd,stroke:#1e88e5,stroke-width:3px,color:#000
    style Product fill:#fff3e0,stroke:#ef6c00,stroke-width:3px,color:#000
    style Like fill:#fce4ec,stroke:#c62828,stroke-width:3px,color:#000
    style Order fill:#f3e5f5,stroke:#7b1fa2,stroke-width:3px,color:#000
    style OrderItem fill:#f3e5f5,stroke:#7b1fa2,stroke-width:1px,color:#000
```

### Aggregate 간 참조 규칙

| 참조 | 방식 | 이유 |
|---|---|---|
| Product → Brand | `brandId` (Long) | 다른 Aggregate를 직접 참조하지 않아 결합도 최소화 |
| Like → User | `userId` (UserId) | User Aggregate의 식별자만 사용 |
| Like → Product | `productId` (Long) | Product Aggregate의 식별자만 사용 |
| Order → User | `userId` (UserId) | 주문자 식별 |
| OrderItem → Product | `productId` (Long) | 주문 시점 단가를 OrderItem에 스냅샷 |

---

## 향후 확장 패키지 구조

```
com.loopers/
├── application/
│   ├── service/
│   │   ├── UserService.java              (Register, Query, PasswordUpdate)
│   │   ├── AuthenticationService.java     (Authentication)
│   │   ├── BrandService.java             (Brand CRUD)
│   │   ├── ProductService.java           (Product CUD)
│   │   ├── ProductQueryService.java      (Product 조회)
│   │   ├── LikeService.java             (좋아요 등록/취소/조회)
│   │   ├── OrderService.java            (주문 생성)
│   │   └── OrderQueryService.java       (주문 조회)
│   ├── AuthenticationUseCase.java
│   ├── RegisterUseCase.java
│   ├── PasswordUpdateUseCase.java
│   ├── UserQueryUseCase.java
│   ├── CreateBrandUseCase.java
│   ├── UpdateBrandUseCase.java
│   ├── DeleteBrandUseCase.java
│   ├── BrandQueryUseCase.java
│   ├── CreateProductUseCase.java
│   ├── UpdateProductUseCase.java
│   ├── DeleteProductUseCase.java
│   ├── ProductQueryUseCase.java
│   ├── LikeUseCase.java
│   ├── UnlikeUseCase.java
│   ├── LikeQueryUseCase.java
│   ├── CreateOrderUseCase.java
│   └── OrderQueryUseCase.java
├── domain/
│   ├── model/
│   │   ├── user/     (User, UserId, UserName, Password, Email, Birthday, ...)
│   │   ├── brand/    (Brand, BrandName)
│   │   ├── product/  (Product, ProductName, Price, Stock, ProductImage)
│   │   ├── like/     (Like)
│   │   └── order/    (Order, OrderItem, OrderSnapshot, OrderStatus, Money, ...)
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── BrandRepository.java
│   │   ├── ProductRepository.java
│   │   ├── LikeRepository.java
│   │   └── OrderRepository.java
│   └── service/
│       └── PasswordEncoder.java
├── infrastructure/
│   ├── entity/        (각 도메인별 JPA Entity)
│   ├── repository/    (각 도메인별 JPA Repository)
│   └── security/
├── interfaces/api/
│   ├── UserController.java
│   ├── BrandController.java
│   ├── BrandAdminController.java
│   ├── ProductController.java
│   ├── ProductAdminController.java
│   ├── LikeController.java
│   ├── OrderController.java
│   ├── OrderAdminController.java
│   ├── interceptor/
│   │   └── AuthenticationInterceptor.java
│   ├── config/
│   │   └── WebMvcConfig.java
│   └── dto/
└── support/error/
```

### Interceptor 경로 확장

```java
// WebMvcConfig - 인증 필요 경로 등록
registry.addInterceptor(authenticationInterceptor)
    .addPathPatterns(
        "/api/v1/users/me",
        "/api/v1/users/me/**",
        "/api/v1/products/*/likes",
        "/api/v1/orders/**"
    );
```

### 설계 원칙 요약

| 원칙 | 적용 |
|---|---|
| **Aggregate 간 ID 참조** | 직접 객체 참조 대신 ID로 연결하여 결합도 최소화 |
| **UseCase 인터페이스 분리 (ISP)** | 각 기능별 인터페이스로 Controller가 필요한 것만 의존 |
| **Command/Query 분리** | 쓰기와 읽기 Service를 분리하여 각각 최적화 가능 |
| **Value Object 활용** | 도메인 규칙을 VO에 캡슐화하여 유효성 보장 |
| **횡단 관심사 분리** | 인증은 Interceptor, 비즈니스는 Service |
| **도메인별 Service** | 각 도메인이 독립 Service를 가져 비대화 방지 |
