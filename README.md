# 호텔_예약시스템
<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/134793042-4d9a897e-7d78-4038-b1e8-50f35390ac49.PNG">

# Table of contents
- [호텔_예약시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [분석/설계](#분석설계)
    - [AS-IS 조직 (Horizontally-Aligned)](#AS-IS-조직-Horizontally-Aligned)
    - [TO-BE 조직 (Vertically-Aligned)](#TO-BE-조직-Vertically-Aligned)
    - [Event Storming 결과](#Event-Storming-결과)
  - [구현](#구현)
    - [시나리오 흐름 테스트](#시나리오-흐름-테스트)
    - [DDD 의 적용](#ddd-의-적용)
    - [Gateway 적용](#Gateway-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [CQRS & Kafka](#CQRS--Kafka)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트](#비동기식-호출--시간적-디커플링--장애격리--최종-Eventual-일관성-테스트)
  - [운영](#운영)
    - [CI/CD 설정](#CICD-설정)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [Self-healing (Liveness Probe)](#Self-healing-Liveness-Probe)
    - [Zero-Downtime deploy (Readiness Probe)](#Zero-Downtime-deploy-Readiness-Probe)
    - [ConfigMap 사용](#ConfigMap-사용)
    - [서킷 브레이킹 / 장애격리](#동기식-호출--서킷-브레이킹--장애격리) 

     
# 서비스 시나리오
- 기능적 요구사항(전체) 
1. 호텔 관리자는 호텔을 등록한다.
2. 고객이 호텔을 선택하여 예약한다.
3. 고객이 예약한 호텔을 결제한다. 
4. 예약이 확정되어 호텔은 예약불가 상태로 바뀐다.
5. 고객이 확정된 예약을 취소할 수 있다.
6. 호텔은 예약가능 상태로 바뀐다.
7. 고객은 호텔 예약 정보를 확인 할 수 있다.

- 비기능적 요구사항
1. 트랜잭션
    1. 호텔 상태가 예약 가능상태가 아니면 아예 예약이 성립되지 않아야 한다  Sync 호출 
1. 장애격리
    1. 결제/마이페이지 기능이 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다.  Async (event-driven), Eventual Consistency
    1. 예약시스템이 과중되면 사용자를 잠시동안 받지 않고 잠시후에 하도록 유도한다.  Circuit breaker, fallback
1. 성능
    1. 고객이 자신의 예약 상태를 확인할 수 있도록 마이페이지가 제공 되어야 한다.  CQRS

# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)

  ![image](https://user-images.githubusercontent.com/88864523/133905974-70288d5e-4098-42da-8cdf-756c66315923.png)

## TO-BE 조직 (Vertically-Aligned)

  ![image](https://user-images.githubusercontent.com/88864523/133906012-be4472b2-d8c3-484d-a865-3bb99cf34c51.png)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과: https://labs.msaez.io/#/storming/H2AHB3tx1WMi5QaOZp6oF7wvlML2/957b75f0d219cf35ff67ded5672e6254


### 이벤트 도출
<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133906173-c73e1716-e64d-451f-981c-0dd14b7bc5f6.png">

### 이벤트 도출-부적격삭제
<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133906181-98112b39-998b-4c03-a308-457b5e8df816.png">

- 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
- 예약시 > 호텔이 조회됨 : 업무적인 의미 이벤트라기 보다 UI 이벤트에 가까워서 제외

### 액터, 커맨드 부착하여 읽기 좋게
<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133906197-29d46e0c-2f7d-4ed4-92ec-27f1e91db519.png">

### 어그리게잇으로 묶기
<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133906208-d5552ab9-9187-451d-a9b9-a5ea516a8168.png">

- command와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기
<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133906214-15b9f161-355c-484c-8d56-4f6b65c10a86.png">

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133906222-f3b47fef-1366-46fe-b897-f7eb43a2e7fe.png">

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133906231-77fe26b3-7c64-416e-a1ae-708d3eef4215.png">

### 완성된 모형
<img width="1113" alt="image" src="https://user-images.githubusercontent.com/88864523/133906240-e5d49d5b-d222-4d33-80b1-450ebfcfdc56.PNG">

- View Model 추가
- 도메인 서열
  - Core : reservation (없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 의 경우 1주일 1회 미만)
  - Supporting : hotel, mypage (경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 1주일 1회 이상을 기준으로 함)
  - General : payment (결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음)

## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/88864523/133906256-c0154d9b-8d6b-4917-bc49-55f9f3c19711.png)



    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다) 그 중 mypage는 CQRS를 위한 서비스이다.

```
cd gateway
mvn spring-boot:run

cd hotel
mvn spring-boot:run 

cd reservation
mvn spring-boot:run  

cd payment
mvn spring-boot:run

cd mypage
mvn spring-boot:run
```


## 시나리오 흐름 테스트 (PostMan 기준)
1. 호텔 관리자는 호텔을 등록한다. (hotel 서비스의 호텔 등록) 

```
- 2개의 호텔 등록 화면 캡쳐
```
<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133929998-ffb44594-5659-42c4-91ff-109c6b1b322a.PNG">
<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133930089-634b273e-9ace-4f3c-b00d-00f9201db58d.PNG">

```
- 호텔 등록 결과 화면 캡쳐
```
<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133930112-1ded2343-5b9a-4044-9fe0-7b9aa04079a1.PNG">


2. 고객이 호텔을 선택하여 예약한다. (reservation 서비스의 호텔 예약)

<img width="993" alt="image" src="https://user-images.githubusercontent.com/88864523/133934382-af977aad-1f93-4a07-a560-955ba09341e7.PNG">


3. 예약이 확정되어 해당 호텔은 예약불가 상태로 바뀐다. (hotelStatus = "Not Available" 상태로 변경됨)

<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133934486-d18f87fd-c02e-4e9e-bf7e-7f3fcb9d5396.PNG">


4. 고객이 확정된 예약을 취소할 수 있다.

<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133934557-112bc654-1aab-4905-840a-d9d21a2ce9ea.PNG">


5. 해당 호텔은 예약 가능한 상태로 바뀐다.

<img width="994" alt="image" src="https://user-images.githubusercontent.com/88864523/133934750-2c3144ce-41d2-4664-98af-f59cdfd560ad.PNG">



6. 고객은 호텔 예약 정보를 확인 할 수 있다.

<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133934863-f5c9f445-bbbd-4bca-864e-709c3bd7d5a6.PNG">



## DDD 의 적용
- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 Reservation 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어(유비쿼터스 랭귀지)를 영어로 번역하여 사용하였다.

```

package hotelreservation;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Reservation_table")
public class Reservation {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String hotelStatus;
    private String hotelType;
    private String hotelPeriod;
    private Float hotelPrice;
    private String memberName;

    @PostUpdate
    public void onPostUpdate(){
        ReservationCanceled reservationCanceled = new ReservationCanceled();
        BeanUtils.copyProperties(this, reservationCanceled);
        reservationCanceled.publishAfterCommit();

    }

    @PrePersist
    public void onPrePersist() throws Exception {
        hotelreservation.external.Hotel hotel = new hotelreservation.external.Hotel();
       
        System.out.print("#######hotelId="+hotel);
        //hotel 서비스에서 hotel의 상태를 가져옴
        hotel = ReservationApplication.applicationContext.getBean(hotelreservation.external.HotelService.class)
            .getHotelStatus(hotelId);

        // 예약 가능상태 여부에 따라 처리
        if ("Available".equals(hotel.gethotelStatus())){
            this.sethotelName(hotel.getHotelName());
            this.sethotelPeriod(hotel.gethotelPeriod());
            this.sethotelPrice(hotel.gethotelPrice());
            this.sethotelType(hotel.gethotelType());
            this.sethotelStatus("Confirmed");
        } else {
            throw new Exception("The hotel is not in a usable status.");
        }

    }

    @PostPersist
    public void onPostPersist() throws Exception {

        ReservationRegistered reservationRegistered = new ReservationRegistered();
        BeanUtils.copyProperties(this, reservationRegistered);
        reservationRegistered.publishAfterCommit();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long gethotelId() {
        return hotelId;
    }

    public void sethotelId(Long hotelId) {
        this.hotelId = hotelId;
    }
    public String gethotelName() {
        return hotelName;
    }

    public void sethotelName(String hotelName) {
        this.hotelName = hotelName;
    }
    public String gethotelStatus() {
        return hotelStatus;
    }

    public void sethotelStatus(String hotelStatus) {
        this.hotelStatus = hotelStatus;
    }
    public String gethotelType() {
        return hotelType;
    }

    public void sethotelType(String hotelType) {
        this.hotelType = hotelType;
    }
    public String gethotelPeriod() {
        return hotelPeriod;
    }

    public void sethotelPeriod(String hotelPeriod) {
        this.hotelPeriod = hotelPeriod;
    }
    public Float gethotelPrice() {
        return hotelPrice;
    }

    public void sethotelPrice(Float hotelPrice) {
        this.hotelPrice = hotelPrice;
    }
    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다.

```
package hotelreservation;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="reservations", path="reservations")
public interface ReservationRepository extends PagingAndSortingRepository<Reservation, Long>{

}

```

- 적용 후 REST API 의 테스트 (PostMan 기준)

```
# hotel 서비스의 호텔 등록
POST http://localhost:8082/hotels
{
  "hotelName": "Seoul Hotel",
  "hotelType": "A-type",
  "hotelPrice": 300000,
  "hotelStatus": "Available",
  "hotelPeriod": "2021 09/20~09/22"
}

# reservation 서비스의 호텔 예약
POST http://localhost:8081/reservations
{
  "hotelId": "2",
  "memberName": "Shin Seok Hyeon"
}

# hotel 예약 상태 확인
GET http://localhost:8082/hotels/2

```

## Gateway 적용
- API GateWay를 통하여 마이크로 서비스들의 진입점을 통일할 수 있다. 
다음과 같이 GateWay를 적용하였다.

```yaml
- gateway 서비스의 application.yml

server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: reservation
          uri: http://localhost:8081
          predicates:
            - Path=/reservations/** 
        - id: hotel
          uri: http://localhost:8082
          predicates:
            - Path=/hotels/** 
        - id: mypage
          uri: http://localhost:8083
          predicates:
            - Path= /myPages/**
        - id: payment
          uri: http://localhost:8084
          predicates:
            - Path=/payments/**  
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true
---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: reservation
          uri: http://reservation:8080
          predicates:
            - Path=/reservations/** 
        - id: hotel
          uri: http://hotel:8080
          predicates:
            - Path=/hotels/** 
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path= /myPages/**
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

## 폴리글랏 퍼시스턴스
- CQRS 를 위한 mypage 서비스만 DB를 구분하여 적용함. 인메모리 DB인 hsqldb 사용.
```
- mypage 서비스의 pom.xml
<!-- 
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
-->
    <dependency>
        <groupId>org.hsqldb</groupId>
        <artifactId>hsqldb</artifactId>
        <version>2.4.0</version>
        <scope>runtime</scope>
    </dependency>
```
## CQRS & Kafka
- 타 마이크로서비스의 데이터 원본에 접근없이 내 서비스의 화면 구성과 잦은 조회가 가능하게 mypage에 CQRS 구현하였다.
- 모든 정보는 비동기 방식으로 발행된 이벤트(예약, 예약취소, 가능상태변경)를 수신하여 처리된다.

예약 실행

<img width="993" alt="image" src="https://user-images.githubusercontent.com/88864523/133936889-df117105-cce5-4dfe-a019-3a02c30b1769.PNG">

카프카 메시지
<img width="962" alt="image" src="https://user-images.githubusercontent.com/88864523/133936956-417f87e6-c261-4da9-a5cb-c9f6b7f35ccc.PNG">
<img width="962" alt="image" src="https://user-images.githubusercontent.com/88864523/133936980-0e057966-cfdf-4e39-81d6-2f571860e3d6.PNG">
```bash
{"eventType":"ReservationRegistered","timestamp":"20210920022205","id":1,"hotelId":2,"hotelName":"Jeju Hotel","hotelStatus":"Confirmed","hotelType":"B-type","hotelPeriod":"2021 09/20~09/22","hotelPrice":600000.0,"memberName":"Shin Seok Hyeon"}
{"eventType":"HotelStatusChanged","timestamp":"20210920022205","id":2,"hotelName":"Jeju Hotel","hotelStatus":"Not Available","hotelType":"B-type","hotelPeriod":"2021 09/20~09/22","hotelPrice":600000.0}
{"eventType":"ReservationCanceled","timestamp":"20210920022235","id":1,"hotelId":2,"hotelName":"Jeju Hotel","hotelStatus":"Cancelled","hotelType":"B-type","hotelPeriod":"2021 09/20~09/22","hotelPrice":600000.0,"memberName":"Shin Seok Hyeon"}
{"eventType":"HotelStatusChanged","timestamp":"20210920022235","id":2,"hotelName":"Jeju Hotel","hotelStatus":"Available","hotelType":"B-type","hotelPeriod":"2021 09/20~09/22","hotelPrice":600000.0}
```

예약/예약취소 후 mypage 화면

<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133937101-0f51efbb-3dc2-482e-9c5e-f50990aa3bfb.PNG">


## 동기식 호출과 Fallback 처리

- 분석단계에서의 조건 중 하나로 예약(reservation) -> 호텔상태확인(hotel) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient를 이용하여 호출하였다

- 호텔서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```java
# (reservation) HotelService.java

package hotelreservation.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Date;

@FeignClient(name="hotel", url="${feign.hotel.url}")
public interface HotelService {

    @RequestMapping(method= RequestMethod.GET, value="/hotels/{id}", consumes = "application/json")
    public Hotel getHotelStatus(@PathVariable("id") Long id);

}
```

- 예약을 처리 하기 직전(@PrePersist)에 HotelSevice를 호출하여 서비스 상태와 Hotel 세부정보도 가져온다.
```java
# Reservation.java (Entity)

    @PrePersist
    public void onPrePersist() throws Exception {
        hotelreservation.external.Hotel hotel = new hotelreservation.external.Hotel();
       
        System.out.print("#######hotelId="+hotel);
        //Hotel 서비스에서 Hotel의 상태를 가져옴
        hotel = ReservationApplication.applicationContext.getBean(hotelreservation.external.HotelService.class)
            .getHotelStatus(hotelId);

        // 예약 가능상태 여부에 따라 처리
        if ("Available".equals(hotel.gethotelStatus())){
            this.sethotelName(hotel.getHotelName());
            this.sethotelPeriod(hotel.gethotelPeriod());
            this.sethotelPrice(hotel.gethotelPrice());
            this.sethotelType(hotel.gethotelType());
            this.sethotelStatus("Confirmed");
        } else {
            throw new Exception("The hotel is not in a usable status.");
        }

    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 시스템이 장애로 예약을 못받는다는 것을 확인
<img width="1019" alt="image" src="https://user-images.githubusercontent.com/88864523/133937569-5fdeb00c-d7eb-4ab5-95df-cb743105a813.PNG">


- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)




## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
- 예약이 이루어진 후에 결제시스템에 결제요청과 마이페이지시스템에 이력을 보내는 행위는 동기식이 아니라 비 동기식으로 처리하여 예약이 블로킹 되지 않아도록 처리한다.
- 이를 위하여 예약기록을 남긴 후에 곧바로 예약완료가 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```java
@Entity
@Table(name="Reservation_table")
public class Reservation {
 ...
    @PostPersist
    public void onPostPersist() throws Exception {
        ...
        ReservationRegistered reservationRegistered = new ReservationRegistered();
        BeanUtils.copyProperties(this, reservationRegistered);
        reservationRegistered.publishAfterCommit();
    }
}
```
- 결제시스템과 마이페이지시스템에서는 예약완료 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다

결제시스템
```java

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationRegistered_PaymentRequestPolicy(@Payload ReservationRegistered reservationRegistered){

        if(!reservationRegistered.validate()) return;
        System.out.println("\n\n##### listener PaymentRequestPolicy : " + reservationRegistered.toJson() + "\n\n");
        // Logic 구성 // 
    }
}
```
마이페이지시스템
```java
package hotelreservation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import hotelreservation.config.kafka.KafkaProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MyPageViewHandler {


    @Autowired
    private MyPageRepository myPageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationRegistered_then_CREATE_1 (@Payload ReservationRegistered reservationRegistered) {
        try {

            if (!reservationRegistered.validate()) return;

            // view 객체 생성
            MyPage myPage = new MyPage();
            // view 객체에 이벤트의 Value 를 set 함
            myPage.setId(reservationRegistered.getId());
            myPage.setMemberName(reservationRegistered.getMemberName());
            myPage.sethotelId(reservationRegistered.gethotelId());
            myPage.sethotelName(reservationRegistered.gethotelName());
            myPage.sethotelStatus(reservationRegistered.gethotelStatus());
            myPage.sethotelType(reservationRegistered.gethotelType());
            myPage.sethotelPeriod(reservationRegistered.gethotelPeriod());
            myPage.sethotelPrice(reservationRegistered.gethotelPrice());
            // view 레파지 토리에 save
            myPageRepository.save(myPage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenReservationCanceled_then_UPDATE_1(@Payload ReservationCanceled reservationCanceled) {
        try {
            if (!reservationCanceled.validate()) return;
                // view 객체 조회
            Optional<MyPage> myPageOptional = myPageRepository.findById(reservationCanceled.getId());
            if( myPageOptional.isPresent()) {
                MyPage myPage = myPageOptional.get();
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                    myPage.sethotelStatus(reservationCanceled.gethotelStatus());
                // view 레파지 토리에 save
                myPageRepository.save(myPage);
            }
            
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
```

- 예약 시스템은 결제시스템/마이페이지 시스템과 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 결제시스템/마이시스템이 유지보수로 인해 잠시 내려간 상태라도 예약을 받는데 문제가 없다
```bash
# 마이페이지/결제 서비스는 잠시 셧다운 시킨다.

1.호텔 등록(입력)

- (POST) http://localhost:8082/hotels
{
  "hotelName": "Seoul Hotel",
  "hotelType": "A-type",
  "hotelPrice": 300000,
  "hotelStatus": "Available",
  "hotelPeriod": "2021 09/20~09/22"
}

{
  "hotelName": "Jeju Hotel",
  "hotelType": "B-type",
  "hotelPrice": 600000,
  "hotelStatus": "Available",
  "hotelPeriod": "2021 09/20~09/22"
}
```

```bash
2.예약 등록(입력) 및 정상 처리 확인
```
<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133938246-aa2c8daa-4a97-4e61-8d11-8b5153edf6b2.PNG">

```bash
3.마이페이지/결제 서비스 기동
cd mypage
mvn spring-boot:run

cd payment
mvn spring-boot:run
```

```bash
4.마이페이지 확인
http localhost:8083/myPages #정상적으로 마이페이지에서 예약 이력이 확인 됨
```
<img width="992" alt="image" src="https://user-images.githubusercontent.com/88864523/133938594-722037a5-611e-4f70-be3d-66a3b688a71b.PNG">


# 운영

## CI/CD 설정

각 구현체들은 각자의 AWS의 ECR 에 구성되었고, 사용한 CI/CD 플랫폼은 AWS-CodeBuild를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 buildspec-kubectl.yaml 에 포함되었다.

- 레포지터리 생성 확인

<img width="1251" alt="(1) 레포지터리 생성 확인" src="https://user-images.githubusercontent.com/88864523/135371647-dc8ab29e-38c0-4897-aae7-195c8a1faf8d.PNG">

<br/>

- 생성 할 CodeBuild
  - user11-gateway
  - user11-hotel
  - user11-reservation
  - user11-mypage
  - user11-payment
<br/>

- github의 각 서비스의 서브 폴더에 buildspec-kubectl.yaml 위치.

<img width="315" alt="(2) buildspec-kubect yaml" src="https://user-images.githubusercontent.com/88864523/134779062-ede22cba-5e72-432a-8711-426a19b16745.PNG"><img width="321" alt="(3) buildspec-kubect yaml" src="https://user-images.githubusercontent.com/88864523/134779477-7cfa92f6-1a1d-486f-97ae-bc2ca8323bfd.PNG"><img width="312" alt="(4) buildspec-kubect yaml" src="https://user-images.githubusercontent.com/88864523/134779497-46fa67ab-41b0-476b-86fa-8a9d2dabbbe3.PNG">

<img width="316" alt="(5) buildspec-kubect yaml" src="https://user-images.githubusercontent.com/88864523/134779509-92a51da3-e807-47e1-86c7-03a0fd0a4148.PNG"><img width="320" alt="(6) buildspec-kubect yaml" src="https://user-images.githubusercontent.com/88864523/134779511-a6e7cadb-67d6-4b7d-a673-63f60e2dc1ef.PNG">

- 연결된 github에 Commit 진행시 5개의 서비스들 build 진행 여부 및 성공 확인 
<img width="1242" alt="(7) 5개 서비스 빌드 성공 확인" src="https://user-images.githubusercontent.com/88864523/135383246-b8c01574-0a7f-49b2-9ef4-f5a65f5d2474.PNG">


-	배포된 5개의 Service  확인
<img width="1062" alt="(8) 배포된 5개 서비스 확인" src="https://user-images.githubusercontent.com/88864523/135383283-997dace2-079b-4615-83bc-26302afa3304.PNG">




## 오토스케일 아웃
- 호텔 서비스(Hotel)에 대해 CPU Load 50%를 넘어서면 Replica를 10까지 늘려준다. 
  - buildspec-kubectl.yaml
```
          cat <<EOF | kubectl apply -f -
          apiVersion: autoscaling/v2beta2
          kind: HorizontalPodAutoscaler
          metadata:
            name: reservation-hpa
          spec:
            scaleTargetRef:
              apiVersion: apps/v1
              kind: Deployment
              name: $_POD_NAME
            minReplicas: 1
            maxReplicas: 10
            metrics:
            - type: Resource
              resource:
                name: cpu
                target:
                  type: Utilization
                  averageUtilization: 50
          EOF
```
- 호텔 서비스(Hotel)에 대한 CPU Resouce를 1000m으로 제한 한다.
  - buildspec-kubectl.yaml
```
                    resources:
                      limits:
                        cpu: 1000m
                        memory: 500Mi
                      requests:
                        cpu: 500m
                        memory: 300Mi
```

- Siege (로더제너레이터)를 설치하고 해당 컨테이너로 접속한다.
```
> kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
EOF

> kubectl exec -it siege -- /bin/bash

```
- 호텔 서비스(Hotel)에 워크 로드를 동시 사용자 100명 60초 동안 진행한다.
```
siege -c100 -t60S -v http://hotel:8080/hotels
```
<img width="582" alt="스크린샷 2021-09-15 오후 3 08 36" src="https://user-images.githubusercontent.com/88864523/135321167-a6586d82-e364-4afc-a61d-7987e84c155f.PNG"> 

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다 : 각각의 Terminal에 
  - 어느 정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다.

<img width="582" alt="스크린샷 2021-09-15 오후 3 08 36" src="https://user-images.githubusercontent.com/88864523/135320044-3ac01f59-1f26-4e20-afd6-5231e60d06ef.PNG"> 

<img width="582" alt="스크린샷 2021-09-15 오후 3 08 36" src="https://user-images.githubusercontent.com/88864523/135320217-763b4900-9934-4d76-b656-052d68de1ee6.PNG"> 




## Self-healing (Liveness Probe)
- 시나리오
  1. Reservation 서비스의 Liveness 설정을 확인힌다. 
  2. Reservation 서비스의 Liveness Probe는 actuator의 health 상태 확인이 설정되어 있어 actuator/health 확인.
  3. pod의 상태 모니터링
  4. Reservation 서비스의 Liveness Probe에서 임의로 path를 잘못된 값으로 변경 후, retry 시도 확인
  5. Reservation 서비스의 describe를 확인하여 Restart가 되는 부분을 확인한다.

<br/>

- Reservation 서비스의 Liveness probe 설정 확인
```
kubectl get deploy reservation -o yaml

                  :
        livenessProbe:
          failureThreshold: 5
          httpGet:
            path: /actuator/health
            port: 8080
            scheme: HTTP
          initialDelaySeconds: 120
          periodSeconds: 5
          successThreshold: 1
          timeoutSeconds: 2
                  :
```

- Httpie를 사용하기 위해 Siege를 설치하고 해당 컨테이너로 접속한다.
```
> kubectl create deploy siege --image=ghcr.io/acmexii/siege-nginx:latest
> kubectl exec pod/[SIEGE-POD객체] -it -- /bin/bash
```

- Liveness Probe 확인 
<img width="582" alt="스크린샷 2021-09-15 오후 3 11 13" src="https://user-images.githubusercontent.com/88864523/135328496-3cf36103-f7e6-4eba-8f00-e85e09641279.PNG">


- Reservation 서비스의 Liveness Probe에서 임의로 path를 잘못된 값으로 변경

```yml
          livenessProbe:
            httpGet:
              path: '/actuator/fakehealth' <-- path를 잘못된 값으로 변경
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

- Reservation Pod가 여러차례 재시작 한것을 확인할 수 있다.
<img width="757" alt="image" src="https://user-images.githubusercontent.com/88864523/135333326-a2f1dceb-fc0b-4b30-a393-a00b587ddd3d.PNG">

<img width="757" alt="image" src="https://user-images.githubusercontent.com/88864523/135333699-f6c23394-f611-4bab-9b77-c562177f7101.PNG">



## Zero-Downtime deploy (Readiness Probe)
- 시나리오
  1. 현재 구동중인 hotel 서비스에 부하를 준다. 
  2. Hotel pod의 상태 모니터링
  3. AWS > CodeBuild에 연결 되어있는 github의 코드를 commit한다.
  4. Codebuild를 통해 새로운 버전의 Hotel 서비스가 배포 된다. 
  5. pod 상태 모니터링에서 기존 Hotel 서비스가 Terminating 되고 새로운 Hotel 서비스가 Running하는 것을 확인한다.
  6. Readness에 의해서 새로운 서비스가 정상 동작할때까지 이전 버전의 서비스가 동작하여 seige의 Avality가 100%가 된다.

<br/>

- hotel 서비스의 Readness probe 설정 확인
  - buildspec_kubectl.yaml
```
                    readinessProbe:
                      httpGet:
                        path: /actuator/health
                        port: 8080
                      initialDelaySeconds: 10
                      timeoutSeconds: 2
                      periodSeconds: 5
                      failureThreshold: 10
```

- 현재 구동중인 Hotel 서비스에 1분 정도 부하를 준다. 
```
> siege -v -c1 -t60S http://hotel:8080/hotels  
```
<img width="794" alt="스크린샷 2021-09-15 오후 3 32 43" src="https://user-images.githubusercontent.com/88864523/135385364-710915bd-43a8-40e6-acaa-ad9b2b096541.PNG">

- AWS에 CodeBuild에 연결 되어있는 github의 코드를 commit한다.
  Hotel 서비스의 아무 코드나 수정하고 commit 한다. 
  배포 될때까지 잠시 기다린다. 
  Ex) buildspec-kubectl.yaml에 carrage return을 추가 commit 한다. 

- pod 상태 모니터링에서 기존 Hotel 서비스가 Terminating 되고 새로운 Hotel 서비스가 Running하는 것을 확인한다.
- pod의 상태 모니터링
<img width="794" alt="스크린샷 2021-09-15 오후 4 59 23" src="https://user-images.githubusercontent.com/88864523/135386347-7ba5dff0-a6fb-4e7f-9cd8-d72c9c904b75.PNG">




## ConfigMap 사용
- 시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리하고, Application에서 특정 도메인 URL을 ConfigMap 으로 설정하여 운영/개발등 목적에 맞게 변경 가능 하다.

- configMap 생성

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: hotel-cm
data:
    api.hotel.url: hotel:8080
EOF
```

- configmap 생성 후 조회
<img width="881" alt="image" src="https://user-images.githubusercontent.com/88864523/135391035-a39671ce-95d5-4e61-82b4-61697694cfb6.PNG">

- reservation > buildspec-kubectl.yaml 에 앞서 생성한 ConfigMap 정보 참조 추가
```yml
      containers:
          ...
          env:
            - name: feign.hotel.url
              valueFrom:
                configMapKeyRef:
                  name: hotel-cm
                  key: api.hotel.url
```


- ResortService.java내용

```java
@FeignClient(name="hotel", url="${feign.hotel.url}")
public interface HotelService {

    @RequestMapping(method= RequestMethod.GET, value="/hotels/{id}", consumes = "application/json")
    public Hotel getHotelStatus(@PathVariable("id") Long id);

}
```


생성된 Pod 상세 내용 확인
<img width="1036" alt="image" src="https://user-images.githubusercontent.com/88864523/135391488-fa7b7cbf-3022-4bb3-89c8-50f650492fc6.PNG">

## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이크 프레임워크 : Spring FeignClient + Hystrix 옵션을 사용

- 시나리오 : 예약(reservation) -> 호텔(hotel) 예약 시 RESTful Request/Response 로 구현 하였고, 예약 요청이 과도할 경우 circuit breaker 를 통하여 장애격리.
Hystrix 설정: 요청처리 쓰레드에서 처리시간이 610 밀리초가 넘어서기 시작하여 어느정도 유지되면 circuit breaker 수행됨

```yaml
# application.yml
feign:
  hystrix:
    enabled: true
    
hystrix:
  command:
    # 전역설정 timeout이 610ms 가 넘으면 CB 처리.
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```

피호출 서비스(호텔:hotel) 의 임의 부하 처리 - 400 밀리초 ~ 620밀리초의 지연시간 부여
```java
# (hotel) HotelController.java 

    @RequestMapping(method= RequestMethod.GET, value="/hotels/{id}")
        public Hotel getHotelStatus(@PathVariable("id") Long id){

            //hystix test code
             try {
                 Thread.currentThread().sleep((long) (400 + Math.random() * 220));
             } catch (InterruptedException e) { }

            return repository.findById(id).get();
        }
```

부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인 : 동시사용자 100명, 10초 동안 실시

```bash
$ siege -v -c100 -t10S -r10 --content-type "application/json" 'http://localhost:8081/reservations POST {"resortId":1, "memberName":"MK"}'

** SIEGE 4.0.5
** Preparing 100 concurrent users for battle.
The server is now under siege...

HTTP/1.1 201     3.64 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     3.64 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     3.70 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     3.94 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     3.98 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.00 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.05 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.05 secs:     343 bytes ==> POST http://localhost:8081/reservations

* 요청이 과도하여 CB를 동작함 요청을 차단

HTTP/1.1 500     4.07 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.07 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.07 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.07 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.07 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.08 secs:     183 bytes ==> POST http://localhost:8081/reservations

* 요청을 어느정도 돌려보내고나니, 기존에 밀린 일들이 처리되었고, 회로를 닫아 요청을 다시 받기 시작

HTTP/1.1 201     4.12 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.18 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.25 secs:     343 bytes ==> POST http://localhost:8081/reservations

* 다시 요청이 쌓이기 시작하여 건당 처리시간이 610 밀리를 살짝 넘기기 시작 => 회로 열기 => 요청 실패처리

HTTP/1.1 500     4.32 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.32 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.32 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.32 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.32 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.33 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.33 secs:     183 bytes ==> POST http://localhost:8081/reservations

* 다시 요청 처리 - (건당 (쓰레드당) 처리시간이 610 밀리 미만으로 회복) => 요청 수락 

HTTP/1.1 201     4.45 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.48 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.53 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.54 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.58 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.66 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.70 secs:     343 bytes ==> POST http://localhost:8081/reservations

* 이후 이러한 패턴이 계속 반복되면서 시스템은 도미노 현상이나 자원 소모의 폭주 없이 잘 운영됨

HTTP/1.1 201     4.39 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.50 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.64 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.65 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.66 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.67 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.38 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.83 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.46 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.08 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     3.92 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     3.91 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.46 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.47 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.57 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.58 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.65 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.68 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.68 secs:     343 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.66 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.69 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.40 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.40 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.34 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.50 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.42 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.54 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.52 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.21 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.52 secs:     345 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 500     4.36 secs:     183 bytes ==> POST http://localhost:8081/reservations
HTTP/1.1 201     4.35 secs:     345 bytes ==> POST http://localhost:8081/reservations

Lifting the server siege...
Transactions:                    152 hits
Availability:                  80.85 %
Elapsed time:                   9.66 secs
Data transferred:               0.06 MB
Response time:                  4.92 secs
Transaction rate:              15.73 trans/sec
Throughput:                     0.01 MB/sec
Concurrency:                   77.34
Successful transactions:         152
Failed transactions:              36
Longest transaction:            5.63
Shortest transaction:           1.33

```
- siege 수행 결과

![image](https://user-images.githubusercontent.com/58622901/125236603-40778c80-e31f-11eb-81a7-eeaa4863239d.png)

![image](https://user-images.githubusercontent.com/58622901/125236641-4ff6d580-e31f-11eb-8659-6886b5cfacc5.png)

