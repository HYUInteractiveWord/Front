# SUNGBIN_WORKLOG

## 2026-05-07
- `aws_deploy` 기반 사전 페이지 테스트 진행
- `RetrofitClient.kt`의 `BASE_URL`을 AWS 퍼블릭 IP 기준으로 조정
- `network_security_config.xml`에 AWS 퍼블릭 IP cleartext 허용 추가
- 현재 확인 포인트: 로그인/사전 검색이 최신 AWS 주소로 실제 연결되는지 재검증

## 2026-05-08
- `preview/verify` 백엔드(`Server main: 1d2a509`) 기준으로 사전 추가 전 검증 흐름 프론트 작업 시작
- `Models.kt`, `ApiService.kt`, `WordRepository.kt`에 `dictionary/preview`, `dictionary/verify` 모델/endpoint/레포지토리 함수 추가
- `AppNavigation.kt`에 `dictionary_verify` route 추가
- 사전 화면의 `추가` 버튼을 즉시 저장에서 verify 화면 이동으로 변경
- `DictionaryVerifyScreen.kt`, `DictionaryVerifyViewModel.kt` 신규 추가
- verify 화면에서 preview 조회, TTS 재생, 1회 마이크 녹음, Whisper verify 성공 시에만 `createWord()`로 저장되도록 구현
- `bash ./gradlew :app:compileDebugKotlin` 기준 빌드 성공 확인

## 2026-05-10
- 실제 구현 브랜치(`sungbin/dict-verify-flow-20260508`)에서 AWS 퍼블릭 IP를 `3.39.234.68:8000`으로 갱신
- `network_security_config.xml`에 새 퍼블릭 IP `3.39.234.68` 추가
