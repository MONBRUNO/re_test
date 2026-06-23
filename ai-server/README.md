# capstone-ai
캡스톤 ai파트

1. 간단한 설명
공공데이터 API를 통해 정확한 영양 성분을 추출하며, 나아가 사용자 맞춤형 음식 효능 및 활용 방법을 제안하는 AI 에이전트입니다.

2. 주요 기능 (Features)
개발하신 코드의 핵심 로직을 섹션으로 나누어 설명하세요.

Intelligent Image Analysis: Gemini 2.5 Flash를 활용한 식재료/음식 자동 분류 (Keyword, Group, Subgroup 추출).

Hybrid Data Pipeline: 공공데이터 API(식품의약품안전처)와 LLM의 지식을 결합한 하이브리드 영양 정보 검색 시스템.

Data Refinement: API 검색 결과가 부정확할 경우, LLM이 스스로 데이터를 검토하고 보정(Self-correction)하여 정제된 JSON 출력.

3. 데이터 파이프라인 구조 (Architecture)
텍스트로 흐름을 명시하면 전문가적인 느낌을 줍니다.

Input: 음식 이미지 혹은 텍스트 입력.

Step 1 (Gemini): 이미지 분석 및 검색 키워드 정제.

Step 2 (API): 공공데이터 API 연동 및 후보군 추출 (Score-based Matching).

Step 3 (Refinement): LLM을 통한 최종 데이터 검증 및 JSON 생성.

제마나이 apikey 받아오고 그다음 공공데이터 apikey 를 txt 파일에 넣습니다
ex)
GEMINI_API_KEY= apikey넣기
FOOD_NUTRITION_API_KEY= apikey넣기

넣은 txt 파일을 코랩 드라이브에 넣고 실행 하면 됩니다. 오류 발생시 apikey.txt 파일 경로 를 제대로 넣었는지 확인



오늘 진행한 핵심 작업들을 바탕으로, 프론트엔드-백엔드-AI 서버 간의 유기적인 흐름과 LangChain 기반의 맞춤형 추천 로직을 강조한 README 작성 예시입니다. 
프로젝트의 기술적 완성도를 보여주기에 아주 좋은 내용들입니다.
📝 Capstone Project: AI 기반 음식 분석 및 맞춤형 추천 시스템📅 작업 날짜: 2026-05-15✅ 

오늘 진행된 주요 업데이트
1. 데이터 파이프라인 구조 고도화분석 및 추천 로직의 분리: 단순한 분석을 넘어, 분석된 데이터를 기반으로 유저 맞춤형 추천을 제공하는 2단계 에이전트 구조를 확립했습니다.  
데이터 흐름(Data Flow):Frontend: 이미지/텍스트 입력 전송.Java Backend: 유저 세션 및 취향 정보 매핑.AI Server (FastAPI): 시각적 분석 → 공공데이터 매칭 → 개인화 추천 생성.  

2. LangChain 기반 Food Recommendation Agent 구축개인화 추천(Personalization): user_id, food_name, cat, user_preference 등 4가지 핵심 지표를 활용
사용자 취향(한식, 양식, 일식 등)에 최적화된 요리를 추천합니다.
지능형 프롬프트 설계:Persona: 영양학 및 요리에 정통한 AI 푸드 컨설턴트 역할 부여.
Multi-modal Context: 이미지 분석 결과에서 얻은 실제 영양 성분 데이터(food_data)를 참조하여 과학적 근거가 있는 건강 조리법을 제안합니다.

3. API 엔드포인트 및 데이터 모델링Pydantic 기반 객체 매핑: RecommendationRequest 모델을 도입하여 백엔드로부터 전달받는 데이터의 유효성을 검증하고 시스템 간의 통신 안정성을 확보했습니다.  JSON 구조화 출력: JsonOutputParser를 사용하여 AI의 답변을 Java 백엔드에서 즉시 처리 가능한 JSON 배열 형식으로 규격화했습니다.  
