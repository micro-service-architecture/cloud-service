# boot-catalog-service
## APIs
|기능|URI (API Gateway 사용시)|URL (API Gateway 미사용시)|HTTP Method|
|----|------------------------|-------------------------|-----------|
|작동 상태 확인|/order-service/health_check|/health_check|GET|
|사용자 별 상품 주문 등록|/order-service/{userId}/orders|/{userId}/orders|POST|
|사용자 별 상품 주문 내역 조회|/order-service/{userId}/orders|/{userId}/orders|GET|
