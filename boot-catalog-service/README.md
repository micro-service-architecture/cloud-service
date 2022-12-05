# boot-catalog-service
## 목차
* **[APIs](#APIs)**
  * **[상품 목록 조회](#상품-목록-조회)**
* **[애플리케이션 배포 구성](#애플리케이션-배포-구성)**
  * **[OrderService 배포](#OrderService-배포)**

## APIs
|기능|URI (API Gateway 사용시)|URL (API Gateway 미사용시)|HTTP Method|
|----|------------------------|-------------------------|-----------|
|작동 상태 확인|/catalog-service/health_check|/health_check|GET|
|상품 목록 조회|/catalog-service/catalogs|/catalogs|GET|

## 상품 목록 조회
### response
![image](https://user-images.githubusercontent.com/31242766/194750839-b354374d-3468-455c-bdd7-11c54df55851.png)

## 애플리케이션 배포 구성
### OrderService 배포
#### Dockerfile 생성
```docker
FROM openjdk:17-ea-11-jdk-slim
VOLUME /tmp
COPY build/libs/boot-catalog-service-0.0.1-SNAPSHOT.jar CatalogService.jar
ENTRYPOINT ["java", "-jar", "CatalogService.jar"]
```
#### 도커 파일 빌드
```docker
docker build -t yong7317/catalog-service:1.0 .
```
#### docker hub 사이트에 업로드
```docker
docker push yong7317/catalog-service:1.0
```
#### 도커 파일 실행
```docker
docker run -d --network ecommerce-network --name catalog-service -e "eureka.client.serviceUrl.defaultZone=http://discovery-service:8761/eureka/" -e "logging.file=/api-logs/catalogs-ws.log" yong7317/catalog-service:1.0
```
