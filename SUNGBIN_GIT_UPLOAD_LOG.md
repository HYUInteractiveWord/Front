# SUNGBIN_GIT_UPLOAD_LOG

## 현재 반영 사항
- AWS 배포 테스트용 API 주소를 `52.79.87.236:8000`으로 교체
- Android cleartext 허용 도메인에 `52.79.87.236` 추가
- 사전 후보 `추가` 버튼이 즉시 저장하지 않고 `preview/verify` 검증 화면으로 이동하도록 변경
- `dictionary/preview`, `dictionary/verify` 프론트 API 연결 추가
- verify 성공 시에만 최종 `createWord()`를 호출하는 사전 저장 게이트 추가

## 업로드 전 확인 필요
- 새 AWS 퍼블릭 IP가 실제 운영 주소로 확정되었는지 확인
- cleartext(`http`) 유지 여부 확인
- 실제 AWS 서버에서 `preview/verify` 응답 형식이 현재 프론트와 일치하는지 실기기/에뮬레이터 검증
- verify 성공 후 단어장 이동 시 UX가 팀 의도와 맞는지 확인
