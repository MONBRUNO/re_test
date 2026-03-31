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









