# cloud-service
## 마이크로서비스 간의 통신
- RestTemplate 사용
- FeignClient 사용

### RestTemplate 이란?
- Srping 3.0 부터 지원하는 Spring HTTP 통신 템플릿이다.
- HTTP 요청 후 JSON, XML, String 과 같은 응답을 받을 수 있는 템플릿이다.
- Blocking I/O 기반의 동기방식을 사용하는 템플릿이다.
- Restful 형식에 맞추어진 템플릿이다.
- Header, Content-Type 등을 설정하여 외부 API 를 호출할 수 있다.
- Server to Server 통신에 사용한다.

#### RestTemplate 메소드
|메서드|HTTP|설명|
|----|------------------------|-------------------------|
|getForObject|GET|HTTP GET 요청 후 결과는 객체로 반환|
|getForEntity|GET|HTTP GET 요청 후 결과는 ResponseEntity로 반환|
|postForLocation|POST|HTTP POST 요청 후 결과는 헤더에 저장된 URL을 반환|
|postForObject|POST|HTTP POST 요청 후 결과는 객체로 반환|
|postForEntity|POST|HTTP POST 요청 후 결과는 ResponseEntity로 반환|
|delete|DELETE|HTTP DELETE 요청|
|headForHeaders|HEADER|HTTP HEAD 요청 후 헤더정보를 반환|
|put|PUT|HTTP PUT 요청|
|patchForObject|PATCH|HTTP PATCH 요청 후 결과는 객체로 반환|
|optionsForAllow|OPTIONS|지원하는 HTTP 메소드를 조회|
|exchange|Any|원하는 HTTP 메소드 요청 후 결과는 ResponseEntity로 반환|
|execute|Any|Request/Response의 콜백을 수정|
