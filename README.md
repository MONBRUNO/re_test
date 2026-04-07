# Naengbuhae_Team_backend

## 기본 세팅하기 

## Java(JDK 17 버전을 사용) - Amazon Corretto 17   
링크: https://aws.amazon.com/ko/corretto/  
화면에서 'Amazon Corretto 17 다운로드' 링크나 버튼을 찾아서 클릭  
Windows x64 줄에 있는 .msi 파일을 클릭해서 다운로드  
다운받은 설치 파일을 실행   
설치가 다 끝났다면, 시스템이 자바를 인식할 수 있도록 명령 프롬프을 연다  
```
java -version
```
화면에 openjdk version "17.0.x" (그리고 옆에 Corretto 어쩌고) 하는 문구가 예쁘게 뜨면 대성공

## 개발 툴(IDE): IntelliJ IDEA (인텔리제이)를 사용 
터미널로 설치 
```
winget install JetBrains.IntelliJIDEA.Community
```
약관 동의(Y/N)가 나오면 Y를 누르고 조금만 기다리면, 알아서 다운로드 된다

## spring boot 세팅 

<img width="1919" height="948" alt="spring boot" src="https://github.com/user-attachments/assets/c4b2d36b-580f-48ad-b06f-0402c1b27ae5" />

## 데이터베이스 구축하기 

<img width="1919" height="881" alt="image" src="https://github.com/user-attachments/assets/79e92f87-0e9f-4a57-91f9-bf559dcd8126" />

<img width="1919" height="879" alt="image" src="https://github.com/user-attachments/assets/6c962256-c913-463c-bfe2-a1fa6cd1a241" />

<img width="1919" height="883" alt="image" src="https://github.com/user-attachments/assets/c6695bae-0ffa-401c-b291-d2190ed1d849" />

<img width="1919" height="876" alt="image" src="https://github.com/user-attachments/assets/72a180fb-2e02-4f07-be1f-f86526d50249" />



## 데이터 베이스랑 연결시키기 

🛠️ 1단계: 스프링 부트에게 PostgreSQL 번역기 달아주기
스프링 부트는 처음에 우리가 설정했던 H2 데이터베이스만 알고 있어서, PostgreSQL이랑 대화하려면 전용 번역기(드라이버)를 하나 달아줘야 해.

1. 인텔리제이 왼쪽 프로젝트 파일 목록에서 build.gradle 이라는 코끼리 모양 파일을 더블클릭해서 열어줘.

2. 코드 아래쪽으로 쭉 내리다 보면 dependencies { ... } 라고 적힌 블록이 보일 거야.

3. 그 괄호 { } 안에 아래 코드 한 줄을 복사해서 맨 밑에 추가해 줘.

```
runtimeOnly 'org.postgresql:postgresql'
```
4. [제일 중요 ⭐️] 코드를 붙여넣으면 인텔리제이 화면 오른쪽 위 구석에 작은 코끼리 아이콘(Load Gradle Changes) 🐘이 둥둥 뜰 거야. 그걸 무조건! 꼭! 눌러줘야 번역기가 다운로드 돼. (화면 아래쪽 상태 표시줄에 로딩 바가 다 지나갈 때까지 잠깐 기다려줘!)

<img width="959" height="503" alt="image" src="https://github.com/user-attachments/assets/6df1d047-a1bf-487b-8351-beac0cb7253f" />

🔗 2단계: 마법의 DB 주소 입력하기
이제 진짜 주소를 알려줄 차례야.

인텔리제이 왼쪽 파일 목록에서 src ➔ main ➔ resources 폴더를 열면, 그 안에 application.properties (또는 application.yml) 파일이 있을 거야. 더블클릭해서 열어줘.

파일 안에 아래 코드를 통째로 복사해서 붙여넣어 줘.

```
# 데이터베이스 연결 주소 (여기에 아까 만든 주소를 넣을 거야!) 이 주소 절대 공개 금지(털려서...) ai한테도 당장 금지 나중에 .env파일을 분리하겠습니
spring.datasource.url=여기를_지우고_아까_완성한_긴_주소를_통째로_붙여넣어주세요
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA (스프링 부트 <-> DB 번역기) 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 재생 버튼을 눌러서 재생하기 
<img width="1915" height="53" alt="image" src="https://github.com/user-attachments/assets/9ae59589-778e-4278-98b6-46890b617c89" />

나 같은 경우 이렇게 저장을 했는데 불이 안 들어 왔다   

🟢 잠든 재생 버튼 깨우고 서버 켜는 법
1. 인텔리제이 왼쪽 폴더 목록에서 src ➔ main ➔ java ➔ com.example.어쩌구 폴더를 차례대로 열어줘.
2. 그 안에 보면 NaengbuhaeApplication (또는 이름이 비슷한 ~Application.java) 이라는 자바 파일이 딱 하나 있을 거야. 그걸 더블클릭해서 열어!
3. 파일이 열리면 코드 창 왼쪽 줄 번호 옆을 잘 봐. public static void main(String[] args) 라고 적힌 줄 바로 옆에 **초록색 재생 버튼(▶️)**이 귀엽게 붙어있을 거야.
4. 그 초록색 버튼을 클릭하고, 'Run NaengbuhaeApplication.main()' 을 선택해 줘!

근데 
```
Caused by: org.postgresql.util.PSQLException: The connection attempt failed.
```
이런 에러가   

🕵️ 원인: Supabase의 최신 정책 (IPv6) vs 우리 집 인터넷 (IPv4)
최근에 Supabase가 속도를 높이려고 'Direct connection(직접 연결)' 방식을 최신 인터넷 주소망(IPv6)으로 강제 업데이트했어. 그런데 우리나라의 많은 가정용 인터넷이나 와이파이는 아직 구형 주소망(IPv4)을 쓰는 경우가 많아서, 서로 대화가 안 통하고 튕겨버리는 거야.

💡 해결책: 'Session pooler (구형 인터넷용 터널)'로 바꿔주기!
아까 네가 나한테 캡처해서 보여줬던 화면(Connection Method 고르는 창) 혹시 기억나? 그 화면으로 딱 한 번만 다시 돌아가 보자!

1. Supabase 대시보드에서 아까 접속했던 [Connect] 창을 다시 열어줘.
2. Connection Method에서 Direct connection 대신, 맨 아래에 있는 Session pooler를 선택해!
(아까 네 캡처 화면에도 자세히 보면 "IPv4 네트워크 환경에서 연결할 때 추천함"이라고 적혀있었어!)
3. Type은 똑같이 JDBC로 둔 상태에서, 새롭게 짠! 하고 나타난 새로운 긴 주소를 복사해 줘. (이번엔 주소 끝부분 포트 번호가 5432가 아니라 6543으로 바뀌어 있을 거야!)
4. 복사한 주소에 아까처럼 진짜 비밀번호를 다시 끼워 넣어줘. (대괄호 [] 지우는 거 잊지 말고!)

🛠️ 마무리 인텔리제이 수정
이제 인텔리제이의 application.properties로 돌아와서, 방금 만든 새로운 Session pooler 주소로 싹 갈아끼워줘. 
```
spring.application.name=Naengbuhae

# 💡 포트번호가 6543으로 끝나는 Session pooler 주소로 변경!
spring.datasource.url=여기를_지우고_아까_완성한_긴_주소를_통째로_붙여넣어주세요
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 해결 
```
spring.application.name=Naengbuhae

spring.datasource.url=jdbc:postgresql://db.lulvkjjxtmnvvqvnatbp.supabase.co:6543/postgres

spring.datasource.username=postgres
# 데이터베이스 연결 주소 이 주소 절대 공개 금지(털려서...) ai한테도 당장 금지 나중에 .env파일을 분리하겠습니다
spring.datasource.password=여기에_비번을_넣어요

spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

이걸로 하고 휴대폰 핫스팟을 켜서 되니 연결이 됨


## .env 파일로 민갑한 정보 숨기기 

🛡️ 1단계: 스프링 부트한테 .env 읽는 법 가르치기
원래 Node.js 같은 건 .env를 바로 읽지만, 스프링 부트는 돋보기(라이브러리)를 하나 달아줘야 해.

1. 인텔리제이 왼쪽 파일 목록에서 🐘 build.gradle 파일을 열어줘.
2. dependencies { ... } 라고 적힌 블록 안에 아래 코드를 한 줄 추가해!
```
implementation 'me.paulschwarz:spring-dotenv:4.0.0'
```
3. 코드를 넣으면 화면 오른쪽 위에 작게 코끼리 아이콘(🐘)이랑 새로고침 버튼이 뜰 거야. 그걸 꼭! 눌러서 라이브러리를 설치해 줘. (밑에 게이지 다 찰 때까지 대기!)


📝 2단계: 최상위 폴더에 .env 파일 만들기
이제 진짜 비밀번호를 담을 금고를 만들 차례야.

1. 인텔리제이 왼쪽 파일 목록에서 프로젝트의 맨 꼭대기(루트) 폴더를 우클릭해. (아마 Naengbuhae라고 적힌 제일 위쪽 폴더일 거야. src 폴더 안이 아니야!)
2. [New] ➔ [File] 을 누르고, 파일 이름을 정확히 .env 라고 적고 엔터를 쳐.
3. 만들어진 .env 파일 안에 아래처럼 네 진짜 정보를 적어줘! (여긴 띄어쓰기나 따옴표 없이 적는 게 좋아)

🙈 3단계: 깃허브에서 .env 완벽하게 숨기기 (가장 중요!!!)
금고를 만들었으니, 깃허브라는 공개 광장에 이 금고가 올라가지 않도록 투명 망토를 씌워야 해!

1. 프로젝트 맨 꼭대기 폴더에 보면 .gitignore 라는 파일이 이미 있을 거야. (이게 투명 망토 파일이야!) 열어줘.
2. 파일 맨 아래 빈 공간에 딱 이렇게 한 줄을 추가해 줘.

```
# 환경변수 파일 숨기기
.env
```
이러면 깃허브 데스크탑 같은 곳에서 .env 파일이 아예 안 보이게 돼서, 절대 실수로 올라갈 일이 없어!  

🔄 4단계: application.properties 수정하기
이제 원래 파일로 돌아가서, "내 진짜 비밀번호는 .env 금고 안에 있으니까 거기서 꺼내 써!" 라고 연결해 주면 끝이야.

application.properties 파일을 열고 아까 적었던 부분을 이렇게 수정해 줘:

```
spring.application.name=Naengbuhae

# 주소는 안 가려도 됨!
spring.datasource.url=jdbc:postgresql://db.lulvkjjxtmnvvqvnatbp.supabase.co:6543/postgres

# .env 금고에서 가져오기! (달러 기호랑 중괄호 필수)
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## 해결 

<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/ab1d3c4e-64e3-46ce-a471-e9894e1fe7ba" />

<img width="1198" height="990" alt="image" src="https://github.com/user-attachments/assets/933543ce-e8a6-4c0c-be90-da17866b4bf6" />
환경 변수에 직접 경로를 입력하니 해결이 되었다 

## 식제료 도메인 기초 세팅 

<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/b6394212-4c07-4a7b-a4d1-910358cce01e" />

Ingredient.java (domain 폴더) : 식재료 설계도
```
package com.example.Naengbuhae.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

// @Entity: "스프링아, 이 클래스 모양대로 Supabase DB에 '식재료' 테이블을 만들어줘!" 라는 뜻
@Entity
@Getter @Setter // 롬복(Lombok) 기능: 숨겨진 데이터(필드)를 꺼내고(Get) 바꿀(Set) 수 있게 해줌
@NoArgsConstructor // 롬복 기능: 텅 빈 기본 설계도(기본 생성자)를 알아서 만들어줌
public class Ingredient {

    // @Id: "이게 식재료들을 구분하는 고유 번호(주민등록번호)야!" 라는 뜻 (Primary Key)
    @Id
    // @GeneratedValue: "고유 번호는 내가 안 넣을 테니까, DB 네가 1, 2, 3... 알아서 1씩 올려가며 넣어줘!"
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: "이 데이터는 DB의 기둥(컬럼)이 될 건데, 이름은 무조건 있어야 해! (비어있으면 안 됨)"
    @Column(nullable = false)
    private String name; // 식재료 이름 (예: 계란)

    private Integer quantity; // 수량 (예: 10)

    private LocalDate expirationDate; // 유통기한 (예: 2026-04-15)

    // 식재료를 처음 만들 때 이름, 수량, 유통기한을 한 번에 쏙 넣기 위해 만든 틀(생성자)
    public Ingredient(String name, Integer quantity, LocalDate expirationDate) {
        this.name = name;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }
}
```

IngredientRepository.java (repository 폴더) : DB 창고지기
```
package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// @Repository: "스프링아, 얘는 DB 창고지기(Repository)니까 네가 관리해 줘!"
@Repository
// JpaRepository<Ingredient, Long>: 마법의 지팡이!
// "이 창고지기는 'Ingredient(식재료)' 데이터를 다룰 거고, 고유 번호는 'Long(숫자)' 타입이야."
// 이걸 상속(extends)받는 순간, 저장(save), 찾기(findById), 전체조회(findAll) 같은 SQL 코드를 안 짜도 다 쓸 수 있음!
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
}
```

IngredientService.java (service 폴더) : 프로젝트의 두뇌
```
package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @Service: "얘가 우리 프로그램의 비즈니스 로직(머리 쓰는 일)을 담당하는 애야!"
@Service
// @Transactional(readOnly = true): "여기 있는 기능들은 기본적으로 DB를 '읽기'만 할 거야. (조회 속도가 빨라짐!)"
@Transactional(readOnly = true)
// @RequiredArgsConstructor: 롬복 기능. 창고지기(Repository)를 자동으로 섭외해서 연결해 줌.
@RequiredArgsConstructor
public class IngredientService {

    // 두뇌(Service)가 일을 하려면 창고지기(Repository)가 무조건 필요함!
    private final IngredientRepository ingredientRepository;

    // --- 1. 식재료 저장 기능 ---
    // @Transactional: "이 기능은 DB에 데이터를 쓰는 거니까, 혹시 에러 나면 저장 취소(롤백)하고 완벽하게 처리해 줘!"
    @Transactional
    public Long saveIngredient(Ingredient ingredient) {
        ingredientRepository.save(ingredient); // 창고지기한테 "이 식재료 저장해!" 라고 시킴
        return ingredient.getId(); // 저장이 잘 끝났으면, DB가 부여한 고유 번호를 돌려줌
    }

    // --- 2. 식재료 전체 조회 기능 ---
    public List<Ingredient> findAllIngredients() {
        return ingredientRepository.findAll(); // 창고지기한테 "창고에 있는 식재료 싹 다 가져와!" 라고 시킴
    }
}
```

IngredientController.java (controller 폴더) : 레스토랑 안내 데스크
```
package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController: "얘는 외부(브라우저, Postman, AI 팀원)의 요청을 받는 API 안내 데스크야!"
@RestController
// @RequestMapping: "이 안내 데스크의 주소는 'http://localhost:8080/api/ingredients'야!"
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor // 두뇌(Service)를 자동으로 섭외해서 연결해 줌.
public class IngredientController {

    // 안내 데스크는 들어온 요청을 처리하기 위해 두뇌(Service)에게 일을 넘겨야 함!
    private final IngredientService ingredientService;

    // --- API 1: 식재료 새로 등록하기 (POST 요청) ---
    // @PostMapping: 누군가 이 주소로 POST(저장) 요청을 보내면 이 메서드가 실행됨
    @PostMapping
    // @RequestBody: "요청으로 날아온 JSON 데이터(계란 10개 등)를 Ingredient 객체로 찰떡같이 변환해서 받아줘!"
    public Long create(@RequestBody Ingredient ingredient) {
        // 두뇌(Service)에게 저장을 부탁하고, 성공하면 받은 고유 번호를 돌려줌
        return ingredientService.saveIngredient(ingredient);
    }

    // --- API 2: 냉장고 속 식재료 다 보기 (GET 요청) ---
    // @GetMapping: 누군가 이 주소로 GET(조회) 요청을 보내면 이 메서드가 실행됨
    @GetMapping
    public List<Ingredient> list() {
        // 두뇌(Service)에게 싹 다 찾아오라고 시킨 결과를 리스트 형태(JSON)로 뱉어줌
        return ingredientService.findAllIngredients();
    }
}
```



## 포트 번호 막히는 거 해결하기 
1️⃣ Supabase PostgREST API 사용하기 (비추천 🙅‍♂️)
원리: Supabase는 DB(5432 포트)를 직접 안 찔러도, 웹사이트 접속하는 것처럼 443 포트(HTTPS)로 데이터를 넣고 뺄 수 있는 'REST API' 기능을 기본으로 제공해. 학교 와이파이도 443 포트는 웹서핑을 해야 하니까 절대 못 막거든! 그래서 이 방법을 쓰면 와이파이에서도 뻥뻥 뚫려.

우리가 쓰면 안 되는 이유: 이걸 쓰려면 우리가 어제 피땀 흘려 만든 Spring Data JPA (IngredientRepository, @Entity 등)를 전부 다 버려야 해! 😭
JPA는 무조건 5432 포트로 DB랑 '직접 연결(JDBC)'을 해야만 작동하는 마법이거든. API 방식으로 바꾸면 코드를 처음부터 끝까지 다 갈아엎어야 해서 지금 상황에선 절대 비추천이야!

2️⃣ Cloudflare WARP 쓰기 (초강력 추천 🌟🌟🌟🌟🌟)
원리: 클라우드플레어(Cloudflare) 워프는 아주 쉽고 빠르고 **무료인 VPN(비밀 터널)**이야.

왜 해결될까?: 이걸 켜면 원희 컴퓨터에서 나가는 5432 포트 요청을 'Cloudflare'라는 거대한 비밀 보따리에 꽁꽁 싸매서 학교 와이파이 공유기를 통과해. 공유기는 "어? 그냥 클라우드플레어 웹사이트 가는 트래픽이네? 통과!" 하고 속아 넘어가는 거지!

개이득 포인트: 제일 중요한 건, 우리가 어제 짠 스프링 부트 코드를 단 1글자도 수정할 필요가 없다는 거야!! 게다가 최근에 Supabase가 무료 버전에서 IPv4 지원을 중단해서 연결이 까다로워졌는데, WARP를 쓰면 이 문제까지 한 방에 해결돼.  

## 테스트 
파워셀은 잘 안 되어서 cmd로 함
<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/988480ea-bbd1-47d4-b69f-2f787ffb6411" />

```
curl.exe -X POST http://localhost:8080/api/ingredients -H "Content-Type: application/json" -d "{\"name\": \"계란\", \"quantity\": 10, \"expirationDate\": \"2026-04-15\"}"
```

성공적으로 들어감

<img width="1919" height="998" alt="image" src="https://github.com/user-attachments/assets/afc74bb5-c4e3-418e-93fc-f5a7b3de9e75" />

## DTO(Data Transfer Object)를 사용해서 보안 올리기 
그러면 왜 써야 할까?  
DB에 있는 식재료 원본(Ingredient 엔티티)은 너무 소중해서 밖으로 함부로 내보내면 안 된다. 그래서 외부랑 데이터를 주고받을 때는 무조건 이 택배 상자(DTO)에 필요한 것만 딱 담아서 주고받는 거! (보안 + 깔끔함 상승!)  

IngredientRequestDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Ingredient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
public class IngredientRequestDto {

    // 외부에서 받을 데이터들만 딱 정의해 둬 (id는 DB가 알아서 넣을 거니까 안 받아도 됨!)
    private String name;
    private Integer quantity;
    private LocalDate expirationDate;

    // 편의 기능: "이 택배 상자(DTO)에 든 내용물을 실제 DB용 식재료(Entity)로 변환해 줘!"
    public Ingredient toEntity() {
        return new Ingredient(name, quantity, expirationDate);
    }
}
```
IngredientResponseDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Ingredient;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class IngredientResponseDto {
    private Long id;
    private String name;
    private Integer quantity;
    private LocalDate expirationDate;

    // 생성자: "DB에서 꺼낸 진짜 식재료(Entity)를 주면, 내가 택배 상자(DTO)에 예쁘게 옮겨 담을게!"
    public IngredientResponseDto(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.quantity = ingredient.getQuantity();
        this.expirationDate = ingredient.getExpirationDate();
    }
}
```
IngredientService.java(변경)
```
package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    // 1. 저장할 때: 원본 대신 '받는 택배 상자(RequestDto)'를 받음
    @Transactional
    public Long saveIngredient(IngredientRequestDto requestDto) {
        // 상자 내용물을 원본(Entity)으로 뜯어서 변환한 다음, DB 창고에 저장!
        return ingredientRepository.save(requestDto.toEntity()).getId();
    }

    // 2. 조회할 때: 원본 대신 '보내는 택배 상자(ResponseDto)' 리스트를 뱉음
    public List<IngredientResponseDto> findAllIngredients() {
        // DB 창고에서 원본들을 싹 꺼내온 다음, 하나하나 예쁜 택배 상자(DTO)에 옮겨 담아서(map) 반환!
        return ingredientRepository.findAll().stream()
                .map(IngredientResponseDto::new) // Ingredient 원본을 ResponseDto로 포장하는 마법의 코드
                .collect(Collectors.toList());
    }
}
```
IngredientController.java(변)
```
package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.IngredientRequestDto;
import com.example.Naengbuhae.dto.IngredientResponseDto;
import com.example.Naengbuhae.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    // POST: 저장 요청이 오면 '받는 택배 상자(RequestDto)'로 안전하게 받기
    @PostMapping
    public Long create(@RequestBody IngredientRequestDto requestDto) {
        return ingredientService.saveIngredient(requestDto);
    }

    // GET: 전체 조회 요청이 오면 원본 말고 '보내는 택배 상자(ResponseDto)' 리스트로 안전하게 내보내기
    @GetMapping
    public List<IngredientResponseDto> list() {
        return ingredientService.findAllIngredients();
    }
}
```

<img width="1919" height="1012" alt="image" src="https://github.com/user-attachments/assets/a50bee7f-79cb-40b4-a153-65166135613e" />

## 결과 

<img width="1919" height="1013" alt="image" src="https://github.com/user-attachments/assets/a5dbbb5a-0b6b-4ac8-8d8a-43e4fffaf055" />

<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/33dfc39e-44e5-4062-b7fb-ec75c05c4ca8" />

성공 

## 삭제하기 만들기 
IngredientService.java(수정)
```
// --- 3. 식재료 삭제 기능 ---
    @Transactional
    public void deleteIngredient(Long id) {
        // 창고지기한테 "이 번호표(id) 가진 식재료 찾아서 버려!" 라고 시킴
        ingredientRepository.deleteById(id);
    }
```

IngredientController.java(수정)
```
// --- API 3: 식재료 삭제하기 (DELETE 요청) ---
    // @DeleteMapping: 누군가 주소 뒤에 번호(id)를 달고 DELETE 요청을 보내면 실행됨
    // 예: /api/ingredients/1 (1번 지워줘!)
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return id + "번 식재료가 냉장고에서 삭제되었습니다! 🗑️";
    }
```


<img width="1919" height="1008" alt="image" src="https://github.com/user-attachments/assets/ee6a2ac1-8616-4992-9a36-718f2638072d" />

```
curl.exe -X DELETE http://localhost:8080/api/ingredients/1
```

<img width="1919" height="1003" alt="image" src="https://github.com/user-attachments/assets/7007d057-7d33-4c64-b9a2-0bc06b8d8926" />

## 식재료 수정(Update) 기능 추가 

IngredientService.java
```
// --- 4. 식재료 수정 기능 (Update) ---
    // @Transactional이 여기서 진짜 중요한 마법을 부림!
    @Transactional
    public Long updateIngredient(Long id, IngredientRequestDto requestDto) {
        // 1. 창고에서 수정할 식재료를 번호(id)로 찾아온다. (없으면 에러 뱉음!)
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 식재료가 없습니다. id=" + id));

        // 2. 찾아온 원본 식재료의 정보를 새 택배 상자(DTO)에 담긴 정보로 바꿔치기!
        ingredient.setName(requestDto.getName());
        ingredient.setQuantity(requestDto.getQuantity());
        ingredient.setExpirationDate(requestDto.getExpirationDate());

        // 3. 엥? 저장(save)을 안 하네?! 
        // 👉 맞음! 스프링 JPA의 '변경 감지' 마법 덕분에 값만 바꿔도 알아서 DB에 덮어씌워짐!
        return ingredient.getId();
    }
```

IngredientController.java
```
// --- API 4: 식재료 수정하기 (PUT 요청) ---
    // @PutMapping: 누군가 주소 뒤에 번호(id)를 달고 PUT(수정) 요청을 보내면 실행됨
    @PutMapping("/{id}")
    public Long update(@PathVariable Long id, @RequestBody IngredientRequestDto requestDto) {
        // 두뇌(Service)에게 "id번 식재료를 이 새 정보(requestDto)로 바꿔줘!" 라고 시킴
        return ingredientService.updateIngredient(id, requestDto);
    }
```

## 계란 10개 -> 8개로 줄여보기 테스트!

<img width="1919" height="1004" alt="image" src="https://github.com/user-attachments/assets/150c3bb3-9af1-4368-880c-a1bb591c0ab1" />

성공 

```
curl.exe -X PUT http://localhost:8080/api/ingredients/2 -H "Content-Type: application/json" -d "{\"name\": \"계란\", \"quantity\": 8, \"expirationDate\": \"2026-04-15\"}"
```

<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/385d271d-b2dd-4292-9153-41f1fcb94d17" />

## 스웨거(Swagger) 

build.gradle  
dependencies { ... } 에 추가
```
// Swagger (springdoc-openapi)
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0'
```

<img width="400" height="201" alt="image" src="https://github.com/user-attachments/assets/79858825-803b-4fad-a991-4a8236d92141" />
코끼리 누르기   


## 레시피(Recipe) 도메인 1단계: 설계도 & 창고지기 만들기

Recipe.java 
```
package com.example.Naengbuhae.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 요리 이름 (예: 계란말이)

    @Column(columnDefinition = "TEXT") 
    private String instructions; // 만드는 법 (글자가 길어질 수 있으니 TEXT 타입으로!)

    private Integer cookingTime; // 조리 시간(분 단위, 예: 15)

    // 레시피 생성자
    public Recipe(String title, String instructions, Integer cookingTime) {
        this.title = title;
        this.instructions = instructions;
        this.cookingTime = cookingTime;
    }
}
```
RecipeRepository.java
```
package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}
```

## 레시피 전용 택배 상자(DTO) 2개랑, 두뇌(Service), 안내 데스크(Controller) 코드

RecipeRequestDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Recipe;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RecipeRequestDto {

    private String title;
    private String instructions;
    private Integer cookingTime;

    // DTO를 DB용 엔티티로 찰떡 변환!
    public Recipe toEntity() {
        return new Recipe(title, instructions, cookingTime);
    }
}
```

RecipeResponseDto.java
```
package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Recipe;
import lombok.Getter;

@Getter
public class RecipeResponseDto {

    private Long id;
    private String title;
    private String instructions;
    private Integer cookingTime;

    // DB에서 꺼낸 엔티티를 이 DTO 상자에 예쁘게 포장!
    public RecipeResponseDto(Recipe recipe) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        this.instructions = recipe.getInstructions();
        this.cookingTime = recipe.getCookingTime();
    }
}
```

RecipeService.java
```
package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;

    // 1. 레시피 저장 (Create)
    @Transactional
    public Long saveRecipe(RecipeRequestDto requestDto) {
        return recipeRepository.save(requestDto.toEntity()).getId();
    }

    // 2. 레시피 전체 조회 (Read)
    public List<RecipeResponseDto> findAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(RecipeResponseDto::new)
                .collect(Collectors.toList());
    }
    
    // (일단 가장 기본이 되는 등록/조회만 뚫어둘게! 수정/삭제는 나중에 필요하면 추가!)
}
```

RecipeController.java
```
package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.RecipeRequestDto;
import com.example.Naengbuhae.dto.RecipeResponseDto;
import com.example.Naengbuhae.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// 주의: 주소가 이번엔 /api/recipes 야!
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    // POST: 레시피 등록 API
    @PostMapping
    public Long create(@RequestBody RecipeRequestDto requestDto) {
        return recipeService.saveRecipe(requestDto);
    }

    // GET: 레시피 전체 조회 API
    @GetMapping
    public List<RecipeResponseDto> list() {
        return recipeService.findAllRecipes();
    }
}
```

## 테스트 

이제 터미널에서 까만 화면 보면서 curl 칠 필요 없어! 아까 우리가 달아둔 스웨거 메뉴판으로 가서 바로 테스트해 보자.

1. 크롬에서 http://localhost:8080/swagger-ui/index.html 새로고침!
2. 화면에 ingredient-controller 밑에 새로 생긴 recipe-controller 메뉴가 짠! 하고 나타난 걸 확인해.
3. 초록색 POST /api/recipes 누르고 [Try it out] 클릭!
4. 데이터 칸(Request body)에 요리 이름(title), 만드는 법(instructions), 조리 시간(cookingTime) 적당히 입력하고 [Execute] 파란 버튼 클릭!
5. 밑에 응답 결과(Response body)에 숫자 **1**이 딱 떨어지면 완벽하게 성공한 거야.

<img width="1919" height="1007" alt="image" src="https://github.com/user-attachments/assets/d9b0b942-b6ab-499c-9e92-0b324ce5f8a8" />

## 지금까지 한 거 정리 
```
📦 Naengbuhae (스마트 냉장고 관리 백엔드)
 ┣ 📂 src/main/java/com/example/Naengbuhae
 │ ┣ 📂 controller       # 클라이언트의 요청을 받고 응답하는 안내 데스크
 │ │ ┣ 📜 IngredientController.java
 │ │ ┗ 📜 RecipeController.java
 │ │
 │ ┣ 📂 domain           # DB 테이블과 직접 연결되는 설계도 (Entity)
 │ │ ┣ 📜 Ingredient.java
 │ │ ┗ 📜 Recipe.java
 │ │
 │ ┣ 📂 dto              # 계층 간 데이터를 안전하게 주고받는 택배 상자
 │ │ ┣ 📜 IngredientRequestDto.java
 │ │ ┣ 📜 IngredientResponseDto.java
 │ │ ┣ 📜 RecipeRequestDto.java
 │ │ ┗ 📜 RecipeResponseDto.java
 │ │
 │ ┣ 📂 repository       # DB 창고에 접근해서 데이터를 넣고 빼는 창고지기
 │ │ ┣ 📜 IngredientRepository.java
 │ │ ┗ 📜 RecipeRepository.java
 │ │
 │ ┗ 📂 service          # 핵심 비즈니스 로직을 처리하는 두뇌
 │   ┣ 📜 IngredientService.java
 │   ┗ 📜 RecipeService.java
 │
 ┗ 📜 build.gradle       # 외부 라이브러리(Swagger 등) 의존성 관리
```
본 프로젝트는 엔티티(Entity)의 외부 노출을 막고 보안과 유연성을 높이기 위해, Controller와 Service 계층 간의 데이터 통신에 DTO(Data Transfer Object) 패턴을 적극적으로 도입하여 설계했습니다. 

DTO의 특징
- 데이터 전달 전용: 비즈니스 로직을 담지 않고, 오직 데이터만 담습니다.
- 보안성 강화: 엔티티를 그대로 노출하면 민감한 정보까지 외부에 드러날 수 있는데, DTO는 필요한 필드만 선택적으로 담아 전달합니다.
- 유연성 확보: 엔티티 구조가 바뀌더라도 DTO를 통해 외부 API나 클라이언트와의 계약을 안정적으로 유지할 수 있습니다.
- 변환 용이: 엔티티 ↔ DTO 간 변환을 통해 원하는 형태로 데이터를 가공할 수 있습니다.


## 코드 통합하기

com vs org, 뭐가 더 좋을까?
무조건 com으로 통일하는 걸 추천해! 1. 대세는 com: 졸업 프로젝트로 끝나는 게 아니라 나중에 앱이나 웹 서비스로 출시한다고 생각했을 때, 대부분의 스타트업이나 상용 서비스는 com을 표준으로 써.

JwtUtil.java,build.gradle,UserController.java,SecurityConfig.java등 바꾸고 .env파일 추

🛠️ 스웨거(메뉴판) '프리패스' 등록하기
config 폴더에 있는 SecurityConfig.java 파일을 열어서, .authorizeHttpRequests 부분을 아래처럼 살짝만 바꿔줘! 스웨거 관련 주소들을 프리패스(permitAll) 명단에 추가하는 거야.

[수정 전]
```
.authorizeHttpRequests(auth -> auth
                .requestMatchers("/user/signup", "/user/login").permitAll()
                .anyRequest().authenticated()
        )
```
[수정 후]
```
.authorizeHttpRequests(auth -> auth
                // 로그인, 회원가입 + 스웨거 관련 주소는 신분증 없이 프리패스!
                .requestMatchers(
                        "/user/signup", 
                        "/user/login",
                        "/swagger-ui/**", 
                        "/v3/api-docs/**", 
                        "/swagger-resources/**"
                ).permitAll()
                .anyRequest().authenticated() // 나머지는 다 신분증(JWT) 검사해!
        )
```



🚨 근데 잠깐! 스웨거에 '자물쇠'가 없네?!

이게 무슨 말이냐면, 로그인을 해서 'JWT 출입증'을 발급받아도, 지금 이 스웨거 메뉴판에는 그 출입증을 문지기한테 보여줄 구멍이 안 뚫려 있다는 뜻이야. 이대로 /api/ingredients에 재료를 추가하려고 하면 출입증을 못 내밀어서 또 403 거절을 당하게 돼!

스웨거한테 "우리 이제 출입증 검사하는 기능 생겼으니까, 출입증 넣는 버튼 좀 만들어줘!"라고 알려주는 설정 파일 추가 

스웨거 자물쇠(Authorize) 버튼 달아주기
```
package com.example.Naengbuhae.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT";
        
        // 1. 스웨거한테 "우리는 JWT라는 이름의 Bearer 토큰을 쓴다"고 알려주기
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer") // Bearer 토큰 방식
                        .bearerFormat("JWT")); // 토큰 형식은 JWT

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
```


