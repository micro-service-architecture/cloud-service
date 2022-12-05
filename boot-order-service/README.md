# boot-order-service
## 목차
* **[APIs](#APIs)**
* **[mariaDB 연동](#mariaDB-연동)**
* **[데이터 동기화 문제](#데이터-동기화-문제)**
    * **[Kafka 를 활용한 데이터 동기화 해결하기](#Kafka-를-활용한-데이터-동기화-해결하기)**
    * **[Multiple Order Service에서의 데이터 동기화](#Multiple-Order-Service에서의-데이터-동기화)**
* **[애플리케이션 배포 구성](#애플리케이션-배포-구성)**
    * **[OrderService 배포](#OrderService 배포)**

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
각 서비스에서는 독립적으로 데이터베이스를 사용하고 있다. 만약 CatalogService에 100개의 상품을 가지고 있다. 여기서 10개의 상품을 주문한다면 Kafka를 통해서 상품 갯수를 90개로 상품 수량 업데이트하는 시나리오로 데이터 동기화를 확인한다. Kafka Connect를 사용하지 않았으므로 Zookeeper 서버와 kafka  서버만 기동하여 테스트를 진행한다.

![image](https://user-images.githubusercontent.com/31242766/200111653-97017f67-bcce-48b4-a7af-4a90cab3636a.png)

- OrderService에 요청된 주문의 수량 정보를 CatalogService에 반영      
OrderService에서 CATALOG-003 상품 수량을 15개 주문했다고 가정하자.

![image](https://user-images.githubusercontent.com/31242766/200149698-43708df4-2667-4d9f-bb9c-a77174c30156.png)

- OrderService에서 Kafka Topic으로 메시지 전송 -> Producer     
```java
@EnableKafka
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```
주문 API에 `Kafka Topic`에 메시지를 보낸다. (`/* send this order to the kafka */`)
```java
@PostMapping("/{userId}/orders")
public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
                                               @RequestBody RequestOrder orderDetails) {
   ModelMapper mapper = new ModelMapper();
   mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

   OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
   orderDto.setUserId(userId);

   /* jpa */
   OrderDto createdOrder = orderService.createOrder(orderDto);
   ResponseOrder responseOrder = mapper.map(createdOrder, ResponseOrder.class);

   /* send this order to the kafka */
   kafkaProducer.send("example-catalog-topic", orderDto);

   return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
}
```
```java
@Service
@Slf4j
public class KafkaProducer {
    private KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public OrderDto send(String topic, OrderDto orderDto) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(orderDto);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        kafkaTemplate.send(topic, jsonInString);
        log.info("Kafka Producer sent data from the Order microservice: " + orderDto);

        return orderDto;
    }
}
```

- CatalogService에서 Kafka Topic에 전송된 메시지 취득 -> Consumer
```java
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    // 관심정보가 있는 Topic을 등록하는 것이다.
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        // GROUP_ID 는 Kafka에서 Topic에 쌓여있는 메시지를 가져가는 Consumer들을 Grouping 할 수 있다.
        // 현재는 하나 밖에 존재하여 크게 의미는 없지만 나중에는 여러 개의 Consumer가 데이터를 가져갈 때
        // 특정한 Consumer Group을 만들어 놓고 전달하고자 하는 Group을 지정하여 가져갈 수 있다.
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumerGroupId");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    // Topic에 어떤 변경 사항이 있는지 지속적으로 Listening 하고 있는. 즉, 이벤트가 발생했을 때 그것을 catch할 수 있는 Listener 이다.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory
                = new ConcurrentKafkaListenerContainerFactory<>();
        kafkaListenerContainerFactory.setConsumerFactory(consumerFactory());

        return kafkaListenerContainerFactory;
    }
}
```
`example-catalog-topic` 에 들어온 메시지에 `productId`로 Catalog 데이터를 찾고 수량을 업데이트해준다.
```java
@Service
@Slf4j
public class KafkaConsumer {

    CatalogRepository repository;

    public KafkaConsumer(CatalogRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "example-catalog-topic")
    public void updateQty(String kafkaMessage) {
        log.info("Kafka Message: ->" + kafkaMessage);

        Map<String, Object> map = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(kafkaMessage, new TypeReference<Map<String, Object>>() {});
        } catch(JsonProcessingException e) {
            e.printStackTrace();
        }

        CatalogEntity entity = repository.findByProductId((String) map.get("productId"));
        if(entity != null) {
            entity.setStock(entity.getStock() - (Integer) map.get("qty"));
            repository.save(entity);
        }
    }
}
```
- Catalog-003 의 데이터가 줄어든 모습을 확인할 수 있다.     

![image](https://user-images.githubusercontent.com/31242766/200150037-e87520f2-83f9-4b76-9fe2-e034f190ec83.png)

![image](https://user-images.githubusercontent.com/31242766/200150046-1f79e357-2129-429c-876b-130316005ede.png)

#### 2. Multiple Order Service에서의 데이터 동기화
- OrderService의 JPA 데이터베이스 교체      
   - H2 DB -> MariaDB
   ```yml
   ...
   datasource:
      driver-class-name: org.h2.Driver 
      url: jdbc:h2:mem:testdb
      username: sa
   jpa:
      database-platform: org.hibernate.dialect.H2Dialect
   ...
   ```
   ```yml
   ...
   datasource:
      driver-class-name: org.mariadb.jdbc.Driver 
      url: jdbc:mariadb://localhost:3306/mydb
      username: root
      password: 비밀번호
   jpa:
      database-platform: org.hibernate.dialect.MySQL5InnoDBDialect
   ...
   ```
- OrderService에 요청된 주문 정보를 DB가 아니라 Kafka Topic으로 전송
   - OrderService Controller 수정 전
   ```java
   @PostMapping("/{userId}/orders")
    public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
                                                     @RequestBody RequestOrder orderDetails) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
        orderDto.setUserId(userId);

        /* jpa */
        OrderDto createdOrder = orderService.createOrder(orderDto);
        ResponseOrder responseOrder = mapper.map(createdOrder, ResponseOrder.class);

        /* send this order to the kafka */
        kafkaProducer.send("example-catalog-topic", orderDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
    }
   ```
   - OrderService Controller 수정 후
   ```java
   @PostMapping("/{userId}/orders")
   public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId,
                                                     @RequestBody RequestOrder orderDetails) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
        orderDto.setUserId(userId);

        /* jpa */
        //OrderDto createdOrder = orderService.createOrder(orderDto);
        //ResponseOrder responseOrder = mapper.map(createdOrder, ResponseOrder.class);

        /* kafka */
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());
        ResponseOrder responseOrder = mapper.map(orderDto, ResponseOrder.class);

        /* send this order to the kafka */
        kafkaProducer.send("example-catalog-topic", orderDto);
        orderProducer.send("orders", orderDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
    }
   ```
- OrderService의 Producer에서 발생하기 위한 메시지 등록    
우리가 가지고 있었던 주문 정보를 어떻게 Topic에 보낼 것인지 중요한 관건이 된다. Topic에 쌓였던 데이터. 즉 메시지들은 Sink Connect가 Topic에 있는 메시지 내용들을 확인하고 어떻게 저장되어있는지 파악하여 해당하는 JdbcConnector에 데이터를 저장하게 된다. 그런데 정해져있는 포맷대로 작성하지 않게 되면 데이터는 저장이 되지 않게 될 것이다. [Kafka Connect](https://github.com/haeyonghahn/TIL/blob/master/Kafka/04.%20Kafka%20Connect.md)에서 DB에 데이터를 삽입 후 `consummer 확인`했을 때의 포맷대로 메시지 내용을 전달해야한다.

![image](https://user-images.githubusercontent.com/31242766/200306383-63ed8731-ceaf-4ba7-903c-82725d775179.png)

- 주문 테스트
   - 2개의 OrderService 기동
   
   ![image](https://user-images.githubusercontent.com/31242766/201459172-dd7418bf-9b06-4fe2-95bf-a217ec052d9d.png)
   
   ![image](https://user-images.githubusercontent.com/31242766/201459226-36b2e831-05de-4e92-b9b5-513735d4c535.png)

   - 첫번째 OrderService 주문
   
   ![image](https://user-images.githubusercontent.com/31242766/201459635-000257cd-ec2a-4b21-a26f-6da4df1bfe09.png)
   
   ![tempsnip](https://user-images.githubusercontent.com/31242766/201459954-fbc6bf68-ae26-4a7f-9846-3a46746efc75.png)
   
   ![image](https://user-images.githubusercontent.com/31242766/201459722-3e250577-e08b-4a58-92a7-1c0873a2000b.png)

   - 두번째 OrderService 주문
   
   ![image](https://user-images.githubusercontent.com/31242766/201459781-ec84096c-d9c7-4b7c-aef8-94143835dc44.png)
   
   ![tempsnip](https://user-images.githubusercontent.com/31242766/201460054-473358cc-d948-4e0a-8891-a98947a83c35.png)

   ![image](https://user-images.githubusercontent.com/31242766/201460091-5c535e0d-e7bb-4116-bab8-9b066ef6de99.png)

## 애플리케이션 배포 구성
### OrderService 배포
