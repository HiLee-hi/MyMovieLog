# MyMovieLog

나만의 영화 감상 기록 Android 앱.  
TMDB에서 영화를 검색하고, 감상 기록·별점·리뷰를 저장하며, 캘린더와 통계로 한눈에 확인할 수 있습니다.

---

## 기술 스택

| 분류 | 사용 기술 |
|------|-----------|
| 언어 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 아키텍처 | MVVM + Clean Architecture |
| DI | Hilt |
| 네트워크 | Retrofit2 + OkHttp |
| 이미지 | Coil |
| 로컬 DB | Room |
| 서버 | Supabase (PostgreSQL + Auth + Storage) |
| 영화 데이터 | TMDB API |
| 캘린더 | kizitonwose/calendar-compose |

---

## 주요 기능

- **영화 검색**: TMDB API 기반 실시간 검색 (한국어)
- **기록 추가**: 봤어요 / 보고싶어요, 별점 (0.5점 단위), 감상 날짜, 리뷰 작성
- **라이브러리**: 감상 완료 / 위시리스트 포스터 그리드
- **캘린더**: 월별 감상 날짜 dot 마커, 날짜 탭 시 기록 목록 BottomSheet
- **통계**: 총 감상 편수, 평균 평점, 월별 바 차트
- **인증**: 이메일 로그인/회원가입, Google OAuth (Supabase Auth)
- **오프라인 지원**: Room 로컬 캐시로 네트워크 없이도 기록 열람 가능

---

## 실행 방법

### 사전 준비

1. [TMDB](https://developer.themoviedb.org) 에서 API 키 발급
2. [Supabase](https://supabase.com) 에서 프로젝트 생성 후 아래 테이블 생성:

```sql
-- profiles 테이블
create table profiles (
  id uuid references auth.users primary key,
  email text,
  display_name text,
  avatar_url text,
  created_at timestamptz default now()
);

-- movie_records 테이블
create table movie_records (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users not null,
  tmdb_id integer not null,
  title text not null,
  original_title text,
  poster_path text,
  genre_ids integer[],
  status text not null check (status in ('watched', 'wishlist')),
  rating real,
  review text,
  memo text,
  watched_at date,
  created_at timestamptz default now(),
  unique (user_id, tmdb_id)
);

-- RLS 활성화
alter table movie_records enable row level security;
create policy "Users can manage their own records"
  on movie_records for all
  using (auth.uid() = user_id);
```

### 환경 설정

프로젝트 루트에 `local.properties` 파일을 생성하고 아래 값을 채워넣습니다.  
(`local.properties.template` 참고)

```properties
TMDB_API_KEY=발급받은_TMDB_API_KEY
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key
sdk.dir=/Users/your_username/Library/Android/sdk
```

> `local.properties`는 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.

### 빌드

```bash
# Android Studio에서 프로젝트 열기 → Gradle Sync → Run
```

또는 CLI:

```bash
./gradlew assembleDebug
```

---

## 주의사항

- `local.properties`는 반드시 직접 생성해야 합니다. 이 파일이 없으면 빌드가 실패합니다.
- Supabase RLS 정책이 적용되어 있어야 다른 사용자의 데이터에 접근할 수 없습니다.
- Google OAuth 사용 시 Supabase 대시보드 > Authentication > Providers에서 Google 설정이 필요합니다.

---

## 버전 정책

[Semantic Versioning](https://semver.org) 을 따릅니다.

| 변경 유형 | 버전 증가 | 예시 |
|-----------|-----------|------|
| 큰 구조 변경 / 호환성 깨짐 | major | 1.0.0 → 2.0.0 |
| 새 기능 추가 | minor | 1.0.0 → 1.1.0 |
| 버그 수정 | patch | 1.0.0 → 1.0.1 |

버전은 `gradle.properties`의 `VERSION_MAJOR / VERSION_MINOR / VERSION_PATCH`에서 관리합니다.

---

## 라이선스

MIT
