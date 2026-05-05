# InteractiveWord — Android 앱

한국어 학습자를 위한 AI 기반 단어 학습 앱입니다.  
주변 오디오(마이크·미디어 파일)에서 한국어 단어를 자동으로 추출하고, 사전 검색·단어장 관리·발음 연습까지 지원합니다.

---

## 📁 프로젝트 구조

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
│       │   │   └── CaptureService.kt            # 백그라운드 오디오 캡처 서비스 (Live 모드)
│       │   │
│       │   └── ui/
│       │       ├── navigation/
│       │       │   └── AppNavigation.kt         # 라우트 정의 + NavHost
│       │       ├── theme/
│       │       │   ├── Color.kt                 # 다크 테마 컬러 팔레트
│       │       │   ├── Theme.kt                 # Material3 다크 테마 설정
│       │       │   ├── Type.kt                  # 타이포그래피
│       │       │   └── Shape.kt                 # 모양 정의
│       │       ├── components/
│       │       │   ├── UserHeader.kt
│       │       │   ├── WordCardItem.kt
│       │       │   └── MissionCardItem.kt
│       │       └── screens/
│       │           ├── login/                   # 로그인 / 회원가입
│       │           ├── home/                    # 홈 대시보드
│       │           ├── scan/                    # 단어 스캔 (마이크 / 미디어)
│       │           ├── dictionary/              # 사전 검색
│       │           ├── collection/              # 단어장
│       │           ├── wordcard/                # 단어 카드 상세
│       │           └── profile/                 # 미션
│       │
│       └── res/
│           └── xml/
│               └── network_security_config.xml  # 개발용 cleartext HTTP 허용
```

---

## 🏛️ 아키텍처

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

## 📱 화면 구성

| 화면 | 라우트 | 주요 기능 |
|---|---|---|
| **로그인** | `login` | 로그인 / 회원가입, 토큰 자동 복원 |
| **홈** | `home` | 사용자 정보, 오늘의 미션, 최근 단어 |
| **스캔** | `scan` | 마이크 녹음, 로컬 미디어 파일 스캔, 단어 추출 결과 확인 |
| **사전 검색** | `dictionary` | 영어/러시아어 입력 → 한국어 후보 단어 검색 및 단어장 추가 |
| **단어장** | `collection` | 저장된 단어 목록, 단어 삭제 |
| **단어 카드** | `word_card/{id}` | 품사·뜻·예문 확인, TTS 재생, 발음 연습 |
| **미션** | `profile` | 일일 미션 / 전체 미션 목록, XP 보상 |

**Bottom Navigation:** 홈 · 사전 · **스캔(FAB)** · 단어장 · 미션

---

## ⚙️ 스캔 기능 상세

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

### [3] Live 모드 (예정)
백그라운드 포그라운드 서비스 + 상단 알림창 상시 표시.  
`NotificationListenerService` + `MediaSession`으로 현재 재생 중인 앱의 타임스탬프를 읽고, yt-dlp로 해당 구간 오디오를 자동 추출 예정.

---

## 🌐 API 엔드포인트

**Base URL:** `http://10.0.2.2:8000/` (에뮬레이터) / PC IP (실기기)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/auth/register` | 회원가입 |
| `POST` | `/api/auth/login` | 로그인 → JWT 발급 |
| `GET` | `/api/auth/me` | 현재 사용자 정보 |
| `GET` | `/api/dictionary/search?word=` | 단어 검색 (LLM 기반) |
| `GET` | `/api/words/` | 단어장 조회 |
| `POST` | `/api/words/` | 단어 추가 |
| `DELETE` | `/api/words/{id}` | 단어 삭제 |
| `POST` | `/api/scan/upload` | 오디오 업로드 → STT + 어휘 추출 |
| `POST` | `/api/scan/process` | 추출 단어 → 단어 카드 생성 |
| `POST` | `/api/pronunciation/submit` | 발음 점수 제출 |
| `GET` | `/api/missions/daily` | 오늘의 미션 |
| `POST` | `/api/missions/{id}/complete` | 미션 완료 처리 |

---

## 🛠️ 기술 스택

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

## 🎨 디자인 시스템

다크 테마 전용입니다.

| 역할 | 색상 | HEX |
|---|---|---|
| 주요 강조 (버튼·완료) | BrandGreenLight | `#82E0A8` |
| 보조 강조 (미디어·슬라이더) | BrandAmberLight | `#E8C97A` |
| 배경 | DarkBackground | `#1A1A1A` |
| 카드/서피스 | DarkSurface | `#222222` |
| 보조 텍스트 | DarkMutedText | `#B3B3B3` |

---

## 🚀 개발 환경 설정

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
앱 최초 실행 시 다음 권한을 허용해야 합니다:
- **마이크** — 마이크 스캔 기능
- **알림** — Live 모드 상단 알림 (Android 13+)

---

## 📌 개발 현황

- [x] 로그인 / 회원가입
- [x] 홈 대시보드
- [x] 마이크 스캔
- [x] 미디어 파일 스캔
- [x] 사전 검색 (LLM 기반 다국어 → 한국어)
- [x] 단어장 조회 / 삭제
- [x] 미션 목록
- [ ] Live 모드 (백그라운드 서비스 + 알림창)
- [ ] 단어 카드 TTS 재생
- [ ] 발음 연습 (피치 분석)
