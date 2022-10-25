# boot-order-service
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
