# MyMovieLog iOS

MyMovieLog(앱 이름 **팝콘한알**, 영어 기기에서는 Popcorn Log)의 iOS(SwiftUI) 버전입니다. Android 앱과 같은 Supabase 프로젝트와 TMDB API를 쓰기 때문에,
같은 계정으로 로그인하면 두 앱에서 같은 기록이 보입니다.

---

## 기술 스택

| 분류        | 사용 기술                                       |
| ----------- | ----------------------------------------------- |
| 언어        | Swift 6 (strict concurrency)                    |
| UI          | SwiftUI + Observation (`@Observable`)           |
| 아키텍처    | MVVM + Clean Architecture (Swift Package 3모듈) |
| 서버        | supabase-swift 2.x (Auth + PostgREST)           |
| 영화 데이터 | TMDB API (URLSession)                           |
| 로컬 캐시   | JSON 파일 (Application Support)                 |
| 테스트      | Swift Testing                                   |
| 프로젝트    | XcodeGen (`project.yml`)                        |
| 최소 버전   | iOS 17 / iPadOS 17                              |

---

## 구현 범위

| 기능                                                          | 상태    |
| ------------------------------------------------------------- | ------- |
| 이메일 로그인/회원가입, Google OAuth                          | ✅      |
| 영화 검색 (400ms 디바운스, 무한 스크롤)                       | ✅      |
| 영화 상세, 기록하기/기록 수정                                 | ✅      |
| 기록 편집 (봤어요/보고싶어요, 0.5점 별점, 감상 날짜, 리뷰·메모) | ✅      |
| 홈 요약 (총 감상, 이번 달, 최근 본 영화, 보고싶은 영화)       | ✅      |
| 라이브러리 (감상 완료/위시리스트 그리드, 삭제)                | ✅      |
| 관람 캘린더 (dot 마커, 날짜별 기록 시트, 좌우 스와이프)       | ✅      |
| 넓은 화면 (iPad·폴더블: 홈·상세 두 칸, 검색·캘린더 목록+상세) | ✅      |
| 버터 크림 디자인 (Android와 같은 색·Jua 글꼴)                 | ✅      |
| 오프라인 열람 (로그인 시 동기화, 로그아웃 시 캐시 삭제)       | ✅      |
| 통계                                                          | 다음 단계 |
| 사진 첨부 (카메라/앨범)                                       | 다음 단계 — Android에서 첨부한 사진은 수정해도 유지 |
| 공휴일 표시                                                   | 다음 단계 |

---

## 구조

```
ios/
├── project.yml                 ← XcodeGen 정의 (MyMovieLog.xcodeproj 생성)
├── Config/
│   ├── App.xcconfig            ← Secrets.xcconfig 를 include
│   └── Secrets.xcconfig        ← API 키 (git 제외, 스크립트로 생성)
├── scripts/generate_secrets.sh ← android/local.properties → Secrets.xcconfig
├── MyMovieLog/                 ← 앱 타깃: @main, 의존성 조립(AppContainer), Info.plist
└── MyMovieLogKit/              ← Swift Package
    ├── Sources/Domain          ← Model, Repository 프로토콜, UseCase
    ├── Sources/DataLayer       ← TMDB/Supabase 구현, 로컬 캐시, Mapper
    ├── Sources/Presentation    ← ViewModel + SwiftUI 화면
    └── Tests/                  ← 계층별 테스트
```

Android 앱과 같은 규칙을 따릅니다.

- 의존성은 `Presentation → Domain ← DataLayer` 방향으로만 흐릅니다.
- ViewModel은 Repository를 직접 받지 않고 UseCase만 주입받습니다(`UseCaseContainer`).
- 읽기는 로컬 캐시에서 하고, 쓰기는 Supabase에 먼저 반영한 뒤 캐시를 갱신합니다.
- 화면은 `AsyncStream`을 `.task`로 구독하므로, 화면이 사라지면 구독도 함께 취소됩니다.

---

## 실행 방법

### 사전 준비

1. App Store에서 **Xcode 16 이상**을 설치합니다.
2. [저장소 README의 공통 준비](../README.md#공통-준비)를 마칩니다. Supabase 설정과 Google 로그인 Redirect URL은 Android 앱과 같습니다.
3. `android/local.properties`에 `TMDB_API_KEY`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`가 채워져 있어야 합니다. 아래 스크립트가 이 값을 읽어 갑니다.

### 빌드

```bash
cd ios
./scripts/generate_secrets.sh     # Config/Secrets.xcconfig 생성
open MyMovieLog.xcodeproj         # 시뮬레이터 선택 후 Run
```

`project.yml`을 수정했다면 `brew install xcodegen` 후 `xcodegen`으로 프로젝트를 다시 만듭니다.

### 테스트

```bash
cd ios/MyMovieLogKit
swift test                        # Domain / DataLayer / Presentation 테스트
```

패키지는 macOS에서도 빌드되도록 구성해서, 시뮬레이터 없이 `swift test`로 계층별 테스트를 돌릴 수 있습니다.
Xcode에서는 `MyMovieLog` 스킴의 Test(⌘U)로 실행합니다.
