# boot-order-service
## 목차
* **[APIs](#APIs)**
* **[mariaDB 연동](#mariaDB-연동)**
* **[데이터 동기화 문제](#데이터-동기화-문제)**
    * **[Kafka 를 활용한 데이터 동기화 해결하기](#Kafka-를-활용한-데이터-동기화-해결하기)**

## APIs
|기능|URI (API Gateway 사용시)|URL (API Gateway 미사용시)|HTTP Method|
|----|------------------------|-------------------------|-----------|
|작동 상태 확인|/order-service/health_check|/health_check|GET|
|사용자 별 상품 주문 등록|/order-service/{userId}/orders|/{userId}/orders|POST|
|사용자 별 상품 주문 내역 조회|/order-service/{userId}/orders|/{userId}/orders|GET|

### 사용자 별 상품 주문 등록
#### request
- 예시 : localhost:8000/order-service/dd705d16-9a15-4325-9c56-3384a9db907b/orders
```json
{
    "productId" : "CATALOG-002",
    "qty" : 10,
    "unitPrice" : 1500
}
```

#### response
![image](https://user-images.githubusercontent.com/31242766/194751239-ed3124ee-65e3-4731-955d-2494ed40f459.png)

### 사용자 별 상품 주문 내역 조회
#### request
- 예시 : localhost:8000/order-service/dd705d16-9a15-4325-9c56-3384a9db907b/orders

#### response
![image](https://user-images.githubusercontent.com/31242766/194751323-16b3ced6-39be-4465-907c-dfbb7a4b880b.png)

## mariaDB 연동
먼저 [mariaDB 다운로드](https://github.com/haeyonghahn/TIL/blob/master/DB/mariaDB%20%EC%84%A4%EC%B9%98.md) 해보자.
### application.yml
```yml
...
spring:
  application:
    name: order-service
  h2:
    console:
      enabled: true
      settings:
        web-allow-others: true
      path: /h2-console
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb
    username: sa
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        #        show_sql: true
        format_sql: true
    defer-datasource-initialization: true
    ...
```
```gradle
dependencies {
    ...
    implementation 'org.mariadb.jdbc:mariadb-java-client'
    ...
}
```
![image](https://user-images.githubusercontent.com/31242766/198818864-85551ad5-fc00-42a8-a1f8-56019595fcc4.png)

## 데이터 동기화 문제
`Order Service`를 2개 기동해보자. `Order Service`는 h2 DB 를 이용하여 독립적인 데이터베이스를 가지고 있을 수 있도록 구성해 놓았다. 그리고. 만약 `Users`의 요청을 분산 처리하기 위해 `Order Service`로 요청이 들어오면 Orders 데이터는 분산 처리되어 저장된다. 

### 첫번째 Order Service
![image](https://user-images.githubusercontent.com/31242766/197792431-37539148-0eb0-453f-8bed-05497e74ef45.png)

### 두번째 Order Service
![image](https://user-images.githubusercontent.com/31242766/197792558-257e6ba8-f5a7-4048-8f73-968d7ba2dab5.png)

그렇다면 어떻게 해결해야 할까?
- 하나의 Database 사용

![image](https://user-images.githubusercontent.com/31242766/197783149-f0ca89e8-76ee-41e9-842c-7861c22107a0.png)

- Database 간의 동기화     
한쪽에서 발생한 데이터를 Message Queuinmg Server([RabbitMQ](https://github.com/haeyonghahn/TIL/tree/master/RabbitMQ), [Kafka](https://github.com/haeyonghahn/TIL/tree/master/Kafka))에 전달하여 변경된 데이터가 있다면 구독 신청한 또 다른 서버에 전달하여 자신의 데이터베이스에 업데이트해주는 방식이다.

![image](https://user-images.githubusercontent.com/31242766/197783364-71287695-9ba6-4b82-b760-df38ff8a6694.png)

- 하나의 Database 사용 + Database 간의 동기화 (DB + Kafka Connector)

![image](https://user-images.githubusercontent.com/31242766/197784544-c2777d13-ed15-423c-aa7d-49e7afb49f13.png)

### Kafka 를 활용한 데이터 동기화 해결하기
Kafka를 활용하기 위해 먼저, [Kafka](https://github.com/haeyonghahn/TIL/tree/master/Kafka) 에 대해 알아보고 OrderService 와 [CatalogService](https://github.com/multi-module-project/cloud-service/tree/master/boot-catalog-service) 를 통해 데이터 동기화 문제를 해결해보자.

#### 1. 데이터 동기화 Orders -> Catalogs
각 서비스에서는 독립적으로 데이터베이스를 사용하고 있다. 만약 CatalogService에 100개의 상품을 가지고 있다. 여기서 10개의 상품을 주문한다면 Kafka를 통해서 상품 갯수를 90개로 상품 수량 업데이트하는 시나리오로 데이터 동기화를 확인한다.

- OrderService에 요청된 주문의 수량 정보를 CatalogService에 반영
- OrderService에서 Kafka Topic으로 메시지 전송 -> Producer
- CatalogService에서 Kafka Topic에 전송된 메시지 취득 -> Consumer

![image](https://user-images.githubusercontent.com/31242766/200111653-97017f67-bcce-48b4-a7af-4a90cab3636a.png)

#### 2. Multiple Order Service에서의 데이터 동기화
