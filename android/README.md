# MyMovieLog Android

MyMovieLog(앱 이름 **팝콘한알**, 영어 기기에서는 Popcorn Log)의 Android(Kotlin + Jetpack Compose) 버전입니다.
백엔드 준비와 API 키 발급은 [저장소 README](../README.md#공통-준비)를 먼저 참고하세요.

---

## 기술 스택

| 분류          | 사용 기술                                   |
| ------------- | ------------------------------------------- |
| 언어          | Kotlin                                      |
| UI            | Jetpack Compose + Material3                 |
| 아키텍처      | MVVM + Clean Architecture                   |
| DI            | Hilt                                        |
| 네트워크      | Retrofit2 + OkHttp                          |
| 이미지        | Coil                                        |
| 카메라        | Jetpack CameraX                             |
| 로컬 DB       | Room                                        |
| 서버          | Supabase (PostgreSQL + Auth + Storage)      |
| 영화 데이터   | TMDB API                                    |
| 공휴일 데이터 | 공공데이터포털 한국천문연구원 특일 정보 API |
| 캘린더        | kizitonwose/calendar-compose 2.6.1 (Compose 1.7 대응 버전) |
| 대화면/폴더블 | Material3 Adaptive 1.0 + Jetpack WindowManager 1.5 |
| 테스트        | JUnit, Robolectric, Compose UI Test, Hilt Testing, window-testing |

---

## 주요 기능

- **영화 검색**: TMDB API 기반 실시간 검색 (한국어), 무한 스크롤로 다음 페이지 자동 로드
- **영화 상세**: 백드롭·포스터·제목·평점·장르·줄거리 표시, 기록하기 / 기록 수정 버튼 제공  
  이미 기록된 영화는 기존 데이터가 자동으로 채워진 수정 시트로 열림
- **기록 추가/수정**: 봤어요 / 보고싶어요, 별점 (0.5점 단위), 감상 날짜, 리뷰·메모, 사진 첨부(카메라·앨범)  
  홈·라이브러리·캘린더 어디서든 포스터 탭 시 기록 조회 및 수정 가능
- **라이브러리**: 감상 완료 / 위시리스트 포스터 그리드  
  왼쪽 스와이프 → 삭제 (AlertDialog로 실수 방지), 홈 섹션 헤더 탭 시 해당 탭으로 바로 이동
- **캘린더**: 월별 감상 날짜 dot 마커, 날짜 탭 시 기록 목록 표시, 기록 클릭 시 수정 가능  
  좁은 화면에서는 BottomSheet, 넓은 화면에서는 캘린더 옆 패널(큰 화면은 기록 편집까지 3단)로 표시  
  공휴일·일요일 빨간색 / 토요일 파란색 표시, 현재 기준 과거 12개월 ~ 미래 3개월 탐색 가능
- **통계**: 총 감상 편수, 평균 평점, 월별 감상 편수 바 차트
- **인증**: 이메일 로그인/회원가입, Google OAuth (Supabase Auth)
- **오프라인 지원**: Room 로컬 캐시로 네트워크 없이도 기록 열람 가능
- **폴더블·대화면 대응**: 기기 모델이 아니라 현재 창의 크기·비율·접힘 상태를 기준으로 화면을 재배치  
  - 좁은 창: 하단 탭 바 / 넓은 창: 왼쪽 내비게이션 레일  
  - 검색: 넓은 창에서 목록 | 영화 상세 2단  
  - 영화 상세: 넓은 창에서 영화 정보 | 내 기록(시트 대신 화면 안에서 바로 작성)  
  - 홈·통계: 넓은 창에서 2단 배치, 라이브러리·사진 선택: 창 폭에 따라 열 수 증가  
  - 카메라: 반쯤 접어 세운 자세(tabletop)에서 미리보기는 위, 버튼은 아래  
  - 접기/펼치기·회전·창 크기 변경에도 작성 중인 기록, 선택한 영화·날짜, 검색어 유지

---

## 아키텍처

MVVM + Clean Architecture 3계층 구조로 의존성은 항상 안쪽(도메인)을 향합니다.

```
presentation/          ← UI (Compose Screen + ViewModel)
    ↓ UseCase 호출
domain/                ← 비즈니스 로직 (UseCase, Repository 인터페이스, 도메인 모델)
    ↓ Repository 구현체 주입 (Hilt)
data/                  ← 데이터 소스 (Room, Supabase, TMDB API, 공휴일 API)
```

### 계층별 역할

| 계층 | 패키지 | 설명 |
|------|--------|------|
| **Presentation** | `presentation/` | Compose Screen · ViewModel · UI 상태 관리. ViewModel은 UseCase만 호출하고 Android 프레임워크 의존성을 최소화. 창 크기·접힘 정보는 `presentation/adaptive/`에서 한 곳에서만 계산해 화면에 제공. 색·글꼴은 `presentation/ui/theme/` |
| **Domain** | `domain/` | UseCase, Repository 인터페이스, 도메인 모델(`Movie`, `MovieRecord`, `UserProfile`). 순수 Kotlin — 외부 프레임워크 의존성 없음 |
| **Data** | `data/` | Repository 구현체, Room Entity/DAO, Retrofit DTO, Supabase DTO, Mapper |

### 데이터 흐름

```
Screen
  │  UI 이벤트
  ▼
ViewModel  ──── UseCase ──── RepositoryImpl
                                  │
                    ┌─────────────┼──────────────┐
                    ▼             ▼              ▼
                Room (로컬)   Supabase       TMDB / 공휴일 API
                              (원격 DB)       (Retrofit)
```

- **오프라인 우선**: `MovieRecordRepositoryImpl`은 Supabase 동기화 후 Room에도 저장하여 네트워크 없이 기록 조회 가능
- **단방향 데이터 흐름**: ViewModel이 `StateFlow`로 UI 상태를 노출, Screen은 collect만 수행
- **DI**: Hilt로 Repository 구현체를 Domain 인터페이스에 바인딩 (`RepositoryModule`)

### 주요 UseCase

| UseCase | 설명 |
|---------|------|
| `UpsertRecordUseCase` | 영화 기록 추가/수정 (Supabase 저장 후 Room 반영). 수정 중 뺀 사진은 저장 후 Storage에서 삭제 |
| `DeleteRecordUseCase` | 기록 삭제 후 연결 사진을 Supabase Storage에서 삭제 (사진 삭제 실패는 기록 삭제를 되돌리지 않음) |
| `SearchMoviesUseCase` | TMDB Paging3 기반 무한 스크롤 검색 |
| `GetWatchedDatesByMonthUseCase` | 캘린더 dot 마커용 월별 감상 날짜 조회 |
| `GetHolidaysByMonthUseCase` | 공휴일 API → Room 캐시 후 반환 |
| `UploadPhotosUseCase` / `GetSignedPhotoUrlsUseCase` | Supabase Storage 사진 업로드·조회 |

---

## 실행 방법

### 환경 설정

[저장소 README의 API 키 설정](../README.md#api-키-설정)대로 `android/local.properties`를 만든 뒤, Android SDK 경로를 추가합니다.
Android Studio로 처음 열면 자동으로 추가되기도 합니다.

```properties
sdk.dir=/Users/your_username/Library/Android/sdk
```

### 빌드

```bash
# Android Studio에서 android/ 폴더 열기 → Gradle Sync → Run
```

또는 CLI (Gradle Wrapper, Gradle 8.7 / JDK 17):

```bash
cd android
./gradlew assembleDebug
```

### 테스트

```bash
cd android
./gradlew testDebugUnitTest          # 단위 테스트 + Robolectric 화면 레이아웃·창 전환 테스트
./gradlew connectedDebugAndroidTest  # 에뮬레이터/기기 테스트 (폴더블 창 전환·접힘 자세)
```

- 기기 테스트는 Repository를 가짜 구현으로 바꿔 실행하므로 Supabase 로그인이나 네트워크 없이 동작합니다.
- 폴더블 검증에는 Android Studio의 7.6" Foldable / 6.7" Foldable 에뮬레이터(API 34)를 사용했습니다.

---

## 주의사항

- `local.properties`는 반드시 직접 생성해야 합니다. 이 파일이 없으면 빌드가 실패합니다.
- 공공데이터포털 API는 HTTP 전용이므로 `network_security_config.xml`에서 `apis.data.go.kr`에 한해 cleartext 통신을 허용하고 있습니다.
- 공휴일 데이터는 월별로 Room에 캐시되며, 한국천문연구원 데이터는 연 1회 업데이트(차차년도 포함)됩니다.
- 프로젝트를 iCloud Drive 등 동기화 폴더 안에서 빌드하면 `app/build`에 `파일명 2.xml` 같은 중복 파일이 생겨 빌드가 실패할 수 있습니다. 동기화 폴더 밖에서 빌드하거나, 발생 시 `android/app/build`를 삭제 후 다시 빌드하세요.

---

## 버전 정책

[Semantic Versioning](https://semver.org) 을 따릅니다.

| 변경 유형                  | 버전 증가 | 예시          |
| -------------------------- | --------- | ------------- |
| 큰 구조 변경 / 호환성 깨짐 | major     | 1.0.0 → 2.0.0 |
| 새 기능 추가               | minor     | 1.0.0 → 1.1.0 |
| 버그 수정                  | patch     | 1.0.0 → 1.0.1 |

버전은 `android/gradle.properties`의 `VERSION_MAJOR / VERSION_MINOR / VERSION_PATCH`에서 관리합니다.
