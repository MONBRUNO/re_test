from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional, List
import uvicorn
from google import genai  # 최신 SDK
import PIL.Image
import io
import json
import re
import os
import requests
from urllib.parse import quote

# [필수: LangChain 및 Pydantic 임포트 추가]
from pydantic import BaseModel, Field
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import JsonOutputParser

app = FastAPI()

# [CORS 설정] 자바 백엔드와 통신 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# [API 키 로드]
def load_api_keys(filepath="api_key.txt"):
    if not os.path.exists(filepath): return
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if "=" in line:
                key, value = line.split("=", 1)
                os.environ[key.strip()] = value.strip()

load_api_keys()
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY") # 이 줄이 추가되었습니다.
FOOD_API_KEY = os.getenv("FOOD_NUTRITION_API_KEY")
client = genai.Client(api_key=GEMINI_API_KEY)

# ---------------------------------------------------------
# STEP 1: analyze_food_logic (이미지/텍스트 -> 지능형 키워드 추출)
# ---------------------------------------------------------
def analyze_food_logic(input_data, input_type="image"):
    """프론트에서 온 데이터를 제미나이가 1차로 분석하여 JSON으로 분류합니다."""
    prompt = """이미지 속 식재료를 분석해서 JSON 배열로만 출력해.
    1. "keyword": 수식어 없는 순수 이름 (예: 시금치, 배, 아보카도)
    2. "group": (농축수산물, 음식, 가공식품) 중 선택
    3. "subgroup": (과일류, 채소류, 곡류, 견과류, 해당없음) 중 가장 적절한 것 선택
    반드시 ```json [ ... ] ``` 형식으로만 답해."""
    
    try:
        if input_type == "image":
            img = PIL.Image.open(io.BytesIO(input_data))
            response = client.models.generate_content(model='gemini-2.5-flash', contents=[prompt, img])
        else:
            response = client.models.generate_content(model='gemini-2.5-flash', contents=f"{input_data}\n{prompt}")

        json_match = re.search(r'```json\n(.*?)\n```', response.text, re.DOTALL)
        return json.loads(json_match.group(1)) if json_match else json.loads(response.text.strip())
    except Exception as e:
        print(f"Error in Step 1: {e}")
        return []

# ---------------------------------------------------------
# STEP 2: get_public_api_candidates (공공데이터 검색 + 점수제 매칭)
# ---------------------------------------------------------
def get_public_api_candidates(keyword, target_sub):
    """1단계에서 나온 키워드로 공공데이터 API를 뒤져 후보군을 뽑습니다."""
    if not FOOD_API_KEY:
        # 키 설정 에러 표시 추가 
        print("⚠️ FOOD_NUTRITION_API_KEY 가 설정되지 않았습니다.")
        return []
    BASE_URL = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo02/getFoodNtrCpntDbInq02"
    candidates = []
    
    for page in range(1, 4):
        url = f"{BASE_URL}?serviceKey={FOOD_API_KEY.strip()}&FOOD_NM_KR={quote(keyword)}&type=json&pageNo={page}&numOfRows=50"
        try:
            res = requests.get(url, timeout=5).json()
            items = res.get('body', {}).get('items', [])
            if not isinstance(items, list): items = items.get('item', []) if isinstance(items, dict) else []
            if not items: break

            for it in items:
                fname, db_sub = it.get('FOOD_NM_KR', ''), it.get('FOOD_CAT1_NM', '')
                score = 0
                if target_sub in db_sub: score += 500000 # 중분류 일치 가산점
                if fname == keyword: score += 10000
                if "_생것" in fname or ", 생것" in fname: score += 1000000 # 원재료 가산점
                if score > 0: candidates.append({'data': it, 'score': score})
        except Exception as e: 
            print(f"⚠️ 공공데이터 API 호출 중 오류 발생: {e}")
            break
    
    candidates.sort(key=lambda x: x['score'], reverse=True)
    return [c['data'] for c in candidates[:10]]

# ---------------------------------------------------------
# STEP 3: 최종 오케스트레이터 (1->2->3단계 연쇄 실행)
# ---------------------------------------------------------
def final_total_process(input_data, input_type="image"):
    # [1단계 실행]
    parsed_food_list = analyze_food_logic(input_data, input_type)
    
    final_nutrition_json = []

    # [2 & 3단계 실행] 추출된 각 음식들에 대해
    for item in parsed_food_list:
        keyword, target_sub = item['keyword'], item.get('subgroup', '해당없음')
        
        # [2단계] API 검색
        candidates = get_public_api_candidates(keyword, target_sub)
        
        # [3단계] 제미나이 최종 검토 및 영양성분 정제
        refine_prompt = f"""당신은 최고의 영양 분석가입니다.
        [대상]: {keyword} ({target_sub})
        [API 후보]: {json.dumps(candidates, ensure_ascii=False)}
        임무: 후보 중 사진 속 '{keyword}'와 가장 잘 맞는 것을 고르거나, 없으면 지식으로 100g당 정보를 생성해.
        출력 형식: {{"food_name": "{keyword}", "is_corrected": "true/false", "cat": "분류", "cal": 0.0, "carbohydrate": 0.0, "protein": 0.0, "fat": 0.0}}"""
        
        try:
            refine_res = client.models.generate_content(model='gemini-2.5-flash', contents=refine_prompt)
            match = re.search(r'\{.*\}', refine_res.text, re.DOTALL)
            if match: 
                final_nutrition_json.append(json.loads(match.group()))
        except Exception as e:
            print(f"❌ Error in Step 3 (Refining): {e}")

    return final_nutrition_json

#음식 추천

class FoodRecommendation(BaseModel):
    dish_name: str = Field(description="추천 요리 이름")
    additional_ingredients: List[str] = Field(description="필요한 추가 재료 및 간단한 활용 방법")
    health_benefits: str = Field(description="음식 효능 및 추천이유 간단 명료 하게 설명")
    recipe_tip: str = Field(description="간단한 조리 팁 또는 주의 사항 간략하게 설명")

class FoodRecommendationList(BaseModel):
    recommendations: List[FoodRecommendation]
    
# 사용자 정보를 포함한 새로운 요청 모델
class RecommendationRequest(BaseModel):
    user_id: str
    food_name: str
    cat: str
    user_preference: str  # 양식, 일식, 한식 등
    nutrition_data: Optional[dict] = None # 기존 분석된 성분 데이터

# [추가] 냉장고 전체 식재료 기반 추천 요청 모델
class FridgeRecommendationRequest(BaseModel):
    user_id: str
    user_preference: str
    ingredients: List[str]

# 추천 agent 구축
class FoodRecommendationAgent:
    def __init__(self, api_key: str):
      self.llm = ChatGoogleGenerativeAI(
          model="gemini-2.5-flash",
          google_api_key=api_key,
          temperature=0.7
      )
      # json 출력 파서 초기화
      self.parser = JsonOutputParser(pydantic_object=FoodRecommendationList)

    def get_recommendation_chain(self):
      prompt = ChatPromptTemplate.from_template(
            "당신은 영양학과 요리에 정통한 AI 푸드 컨설턴트입니다.\n"
            "당신은 사용자의 취향을 완벽히 분석하는 AI 푸드 컨설턴트입니다.\n"
            "사용자({user_id})는 현재 '{user_preference}' 스타일의 음식을 선호합니다.\n\n"
            "[분석된 식재료 정보]:\n"
            "- 재료명: {food_name}\n"
            "- 카테고리: {cat}\n"
            "[현재 분석된 식재료 데이터]:\n{food_data}\n\n"
            "임무:\n"
            "1. 반드시 사용자의 취향인 '{user_preference}' 스타일의 요리 3가지를 추천할 것.\n"
            "2. {food_name}의 영양 성분을 고려하여 건강한 조리법을 제안할 것.\n"
            "3. 각 요리별 영양학적 효능과 추천 이유를 간단 명료하게 설명할 것.\n"
            "{format_instructions}"
      )
      # langchain 구성 : prompt -> llm -> output parser
      return prompt | self.llm | self.parser

# 기존 def generate_recommendations(analyzed_food_json: list) 방식에서
# id, preference, name, car, food_json 만 불러오게 수정
# 수정 이유 기존 food_json만 받기때문에 user_id name 등등 불러오다가 키에러 발생 위험 
def generate_recommendations(user_id: str, user_preference: str, food_name: str, cat: str, analyzed_food_json: list):
  # 기존 로직에서 추출 분석 결과를 입력받아 추천결과 반환

  api_key = os.getenv("GEMINI_API_KEY")
  if not api_key:
      return {"error": "GEMINI_API_KEY가 설정되지 않았습니다."}

  print("👩‍🍳 AI 푸드 컨설턴트가 레시피를 고민 중입니다...")

  agent = FoodRecommendationAgent(api_key)
  chain = agent.get_recommendation_chain()

  # id, name 등등 받아올 데이터가 늘어나서 이부분도 추가 수정
  try:
        result = chain.invoke({
            "user_id": user_id,
            "user_preference": user_preference,
            "food_name": food_name,
            "cat": cat,
            "food_data": json.dumps(analyzed_food_json, ensure_ascii=False),
            "format_instructions": agent.parser.get_format_instructions()
        })
        return result
  except Exception as e:
        print(f"❌ 추천 중 오류 발생: {e}")
        return {"error": str(e)}



# ---------------------------------------------------------
# API 엔드포인트
# ---------------------------------------------------------

# 기존 async def 에서 다중 처리 요청에도 fast api 안전하게 처리 하기 위해 def 로 구조 변경

@app.post("/analyze")
def handle_request(file: Optional[UploadFile] = File(None), text: Optional[str] = Form(None)):
    if file:
        content = file.file.read()
        data = final_total_process(content, "image")
        return {"status": "success", "data": data}
    elif text:
        data = final_total_process(text, "text")
        return {"status": "success", "data": data}
    return {"status": "error", "message": "데이터 없음"}

# 1. 냉장고 전체 식재료 기반 레시피 추천 (Java 백엔드와 연동)
@app.post("/api/recommend")
def get_fridge_recommendations(request: FridgeRecommendationRequest):
    """
    Java 백엔드로부터 냉장고의 모든 식재료 리스트를 전달받아
    사용자의 취향(DietGoal)을 고려한 맞춤 요리 3가지를 추천합니다.
    """
    if not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY가 설정되지 않았습니다.")

    try:
        agent = FoodRecommendationAgent(GEMINI_API_KEY)
        chain = agent.get_recommendation_chain()
        
        # 식재료 리스트를 하나의 문자열로 결합하여 context 제공
        ingredients_summary = ", ".join(request.ingredients)
        
        result = chain.invoke({
            "user_id": request.user_id,
            "user_preference": request.user_preference,
            "food_name": "냉장고 속 다양한 식재료",
            "cat": "복합 재료",
            "food_data": ingredients_summary,
            "format_instructions": agent.parser.get_format_instructions()
        })
        
        return {"status": "success", "recommendations": result["recommendations"]}
    except Exception as e:
        print(f"냉장고 기반 추천 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))

# 2. 특정 음식 기반 추천 엔드포인트
@app.post("/fdmake")
def get_recommendations(request: RecommendationRequest):
    """
    /analyze 에서 나온 영양 성분 JSON 리스트를 입력받아
    맞춤형 요리 추천 3가지를 반환합니다.
    """
    if not GEMINI_API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY가 설정되지 않았습니다.")

    try:
        agent = FoodRecommendationAgent(GEMINI_API_KEY)
        chain = agent.get_recommendation_chain()
        
        # 랭체인 실행 
        result = chain.invoke({
            "user_id": request.user_id,
            "user_preference": request.user_preference,
            "food_name": request.food_name,
            "cat": request.cat,
            "food_data": json.dumps(request.nutrition_data, ensure_ascii=False),
            "format_instructions": agent.parser.get_format_instructions()
        })
        
        return {"status": "success", "recommendations": result["recommendations"]}
    except Exception as e:
        print(f"추천 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)