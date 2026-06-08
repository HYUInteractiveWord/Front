# InteractiveWord — Android 앱

한국어 학습자를 위한 AI 기반 단어 학습 앱입니다.  
주변 오디오(마이크·미디어 파일)에서 한국어 단어를 자동으로 추출하고, 사전 검색·단어장 관리·발음 연습까지 지원합니다.

---

## 프로젝트 구조

```
Android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/interactiveword/
│       │   ├── MainActivity.kt                  # 앱 진입점, Bottom Navigation
│       │   │
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── ApiService.kt            # Retrofit 엔드포인트 인터페이스
│       │   │   │   └── RetrofitClient.kt        # Retrofit 싱글톤 (JWT 인터셉터 포함)
│       │   │   ├── local/
│       │   │   │   └── TokenDataStore.kt        # JWT 토큰 로컬 저장 (DataStore)
│       │   │   ├── model/
│       │   │   │   └── Models.kt                # 전체 데이터 클래스 정의
│       │   │   └── repository/
│       │   │       ├── UserRepository.kt
│       │   │       ├── WordRepository.kt
│       │   │       ├── ScanRepository.kt
│       │   │       └── MissionRepository.kt
│       │   │
│       │   ├── service/
│       │   │   ├── CaptureService.kt            # MediaProjection 기반 오디오 캡처
│       │   │   ├── AudioCaptureService.kt       # 알림창 컨트롤 포그라운드 서비스
│       │   │   └── MediaWatcherService.kt       # 미디어 세션 감지 서비스
│       │   │
│       │   └── ui/
│       │       ├── navigation/
│       │       │   └── AppNavigation.kt         # 라우트 정의 + NavHost
│       │       ├── theme/
│       │       │   ├── Color.kt                 # 게임 민트 라이트 테마 컬러
│       │       │   ├── Theme.kt                 # Material3 테마 설정
│       │       │   ├── Type.kt                  # 타이포그래피
│       │       │   └── Shape.kt                 # 모양 정의
│       │       ├── components/
│       │       │   ├── UserHeader.kt
│       │       │   ├── WordCardItem.kt
│       │       │   └── MissionCardItem.kt
│       │       └── screens/
│       │           ├── login/                   # 로그인 / 회원가입
│       │           ├── home/                    # 홈 대시보드
│       │           ├── scan/                    # 단어 스캔 (마이크 / 미디어 / Live)
│       │           ├── dictionary/              # 사전 검색
│       │           ├── collection/              # 단어장
│       │           ├── wordcard/                # 단어 카드 상세 (발음 연습 포함)
│       │           └── profile/                 # 미션 및 퀴즈 (POS / Vocab / Example)
│       │
│       └── res/
│           └── xml/
│               └── network_security_config.xml  # 개발용 cleartext HTTP 허용
```

---

## 아키텍처

**MVVM + Repository 패턴** 기반으로 설계되었습니다.

```
[Composable Screen]
        ↓  observe StateFlow
[ViewModel]  ←→  viewModelScope (Coroutine)
        ↓
[Repository]
        ↓
[ApiService (Retrofit)]  →  FastAPI 백엔드 (port 8000)
[TokenDataStore]         →  로컬 DataStore (JWT 저장)
```

### 상태 관리
각 화면마다 ViewModel이 `MutableStateFlow`로 UI 상태를 관리하며, Composable은 `collectAsState()`로 구독합니다.

---

## 화면 구성

| 화면 | 라우트 | 주요 기능 |
|---|---|---|
| **로그인** | `login` | 로그인 / 회원가입, 토큰 자동 복원 |
| **홈** | `home` | 사용자 정보, 오늘의 미션, 최근 단어 |
| **스캔** | `scan` | 마이크 / 미디어 / YouTube / Live(알림창) 스캔 및 결과 확인 |
| **사전 검색** | `dictionary` | 다국어 입력 → 한국어 후보 검색 및 검증 후 추가 |
| **단어장** | `collection` | 저장된 단어 목록 관리 (등급별 색상 표시) |
| **단어 카드** | `word_card/{id}` | 뜻·예문 확인, TTS 재생, 발음 연습 및 히스토리 |
| **미션** | `profile` | 일일/전체 미션 목록, 퀴즈 진입점, XP 보상 |
| **퀴즈** | `pos_quiz` 등 | 품사(POS) / 어휘(Vocab) / 예문(Example) 퀴즈 |

**Bottom Navigation:** 홈 · 사전 · **스캔(FAB)** · 단어장 · 미션

---

## 스캔 기능 상세

### [1] 마이크 스캔
```
마이크 버튼 클릭
  → RECORD_AUDIO 권한 요청
  → AudioRecord (16kHz, Mono, PCM 16-bit) 녹음 (최대 30초)
  → PCM → WAV 변환
  → POST /api/scan/upload
  → 백엔드: Whisper STT → LLM 어휘 추출 → 국어사전 검증
  → 단어 카드 결과 표시
```

### [2] 미디어 스캔
```
미디어 버튼 클릭
  → 파일 피커 (audio/*, video/*)
  → 바텀시트: 스캔 구간 슬라이더 (10~60초)
  → MediaExtractor + MediaCodec으로 오디오 디코딩
  → 16kHz Mono로 리샘플링 → WAV 변환
  → POST /api/scan/upload → 동일 파이프라인 처리
```

### [3] Live 모드 (알림 컨트롤)
```
상단 알림창 '시작' 클릭
  → AudioCaptureService (포그라운드 서비스) 실행
  → MediaWatcherService가 현재 재생 중인 미디어(YouTube 등) 감지
  → 알림창 내 컨트롤러(◀ 시작, 종료 ▶)로 캡처 구간(최대 30초) 조정
  → '✓ 캡처' 클릭 시 앱으로 복귀하여 자동 스캔 진행
```

### [4] YouTube 공유 스캔
YouTube 앱에서 '공유' → 'InteractiveWord' 선택 시 해당 영상의 URL을 자동으로 읽어와 서버에서 오디오를 추출하고 스캔합니다.

---

## API 엔드포인트

**Base URL:** `http://10.0.2.2:8000/` (에뮬레이터) / PC IP (실기기)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/auth/register` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 → JWT 발급 |
| `GET` | `/api/auth/me` | 현재 사용자 정보 |
| `GET` | `/api/dictionary/search?word=` | 단어 검색 (LLM 기반) |
| `POST` | `/api/dictionary/preview` | 단어 상세 정보 미리보기 (사전 추가 전) |
| `GET` | `/api/words/` | 단어장 조회 |
| `POST` | `/api/words/` | 단어 추가 |
| `POST` | `/api/words/quiz-result` | 퀴즈 결과 제출 및 통계 반영 |
| `POST` | `/api/scan/youtube` | YouTube URL 기반 오디오 스캔 |
| `POST` | `/api/scan/upload` | 오디오 파일 업로드 → STT + 어휘 추출 |
| `POST` | `/api/scan/process` | 추출 단어 → 단어 카드 생성 |
| `POST` | `/api/pronunciation/submit` | 발음 점수 및 녹음 파일 제출 |
| `GET` | `/api/pronunciation/{id}/history` | 특정 단어의 발음 연습 기록 조회 |
| `GET` | `/api/missions/daily` | 오늘의 미션 |
| `POST` | `/api/missions/{id}/complete` | 미션 완료 처리 |

---

## 🛠기술 스택

| 분류 | 라이브러리 |
|---|---|
| **UI** | Jetpack Compose, Material3 |
| **네비게이션** | Navigation Compose |
| **상태 관리** | ViewModel, StateFlow, Coroutines |
| **네트워크** | Retrofit 2, OkHttp, Gson |
| **로컬 저장소** | DataStore Preferences |
| **오디오** | AudioRecord, MediaExtractor, MediaCodec |
| **최소 SDK** | 29 (Android 10 — AudioPlaybackCapture 지원) |
| **타겟 SDK** | 35 |

---

## 디자인 시스템

'게임 민트(Game Mint)' 라이트 테마를 적용하여 밝고 경쾌한 학습 환경을 제공합니다.

| 역할 | 색상 | HEX | 비고 |
|---|---|---|---|
| 배경 | GameBgMain | `#F5F7FF` | 연한 블루/그레이 톤 |
| 카드 배경 | GameBgCard | `#FFFFFF` | 순백색 |
| 주요 강조 | GameMint | `#00C896` | 메인 민트 컬러 |
| 포인트 컬러 | GameCoral | `#FF6B6B` | 강조 및 에러 |
| 보조 포인트 | GameGold | `#FFD700` | 레어 등급 및 미션 |
| 텍스트 (메인) | GameTextDark | `#1A1A2E` | 짙은 네이비 |
| 텍스트 (보조) | GameTextLight | `#9999BB` | 연한 블루그레이 |
| 테두리 | GameBorder | `#EEEEF5` | 은은한 경계선 |

---

## 개발 환경 설정

### 필수 조건
- Android Studio Hedgehog 이상
- JDK 11
- Android 에뮬레이터 (API 29+) 또는 실기기

### 실행 방법
1. 백엔드 서버를 먼저 실행합니다 (`Backend/Server/start.bat`)
2. `Android/` 폴더를 Android Studio에서 엽니다
3. `app/src/main/java/.../data/api/RetrofitClient.kt`에서 `BASE_URL` 확인
   - 에뮬레이터: `http://10.0.2.2:8000/` (기본값)
   - 실기기: PC의 로컬 IP로 변경 (`http://192.168.x.x:8000/`)
4. Run (▶) 또는 `Shift+F10`으로 빌드 및 설치

### 권한
앱 기능을 온전히 사용하려면 다음 권한을 허용해야 합니다:
- **마이크 (`RECORD_AUDIO`)** — 마이크 스캔
- **알림 (`POST_NOTIFICATIONS`)** — Live 모드 컨트롤 (Android 13+)
- **알림 접근 (`NotificationListener`)** — 미디어 재생 정보 감지
- **다른 앱 위에 그리기** — (선택) 백그라운드 캡처 서비스 지원

---

## 개발 현황

- 로그인 / 회원가입
- 홈 대시보드
- 마이크 스캔
- 미디어 파일 스캔
- YouTube URL / 공유 스캔
- Live 모드 (백그라운드 서비스 + 알림창 컨트롤러)
- 사전 검색 (LLM 기반 다국어 >> 한국어)
- 단어장 조회 / 삭제 (등급 시스템 포함)
- 미션 및 퀴즈 (POS, Vocab, Example 모드)
- 단어 카드 TTS 재생
- 발음 연습 (피치 분석 및 히스토리 기록)
- 디자인 시스템 (게임 민트 라이트 테마 교체)
