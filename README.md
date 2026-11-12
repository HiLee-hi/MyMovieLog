# MyMovieLog

<img src="docs/brand/app-icon-popcorn-1024.png" width="96" alt="팝콘한알 앱 아이콘">

앱 이름: **팝콘한알** (영어 기기: Popcorn Log). 저장소와 패키지 이름은 MyMovieLog를 유지합니다.

나만의 영화 감상 기록 앱.  
TMDB에서 영화를 검색하고, 감상 기록·별점·리뷰를 저장하며, 캘린더와 통계로 한눈에 확인할 수 있습니다.

같은 Supabase 백엔드를 쓰는 Android 앱과 iOS 앱으로 구성됩니다. 같은 계정으로 로그인하면 두 앱에서 같은 기록이 보입니다.

| 앱 | 언어 / UI | 폴더 | 문서 |
| --- | --- | --- | --- |
| Android | Kotlin + Jetpack Compose | [`android/`](android/) | [android/README.md](android/README.md) |
| iOS | Swift + SwiftUI | [`ios/`](ios/) | [ios/README.md](ios/README.md) |

```
MyMovieLog/
├── android/   ← Android 앱 (Gradle 프로젝트, Android Studio에서 이 폴더를 연다)
├── ios/       ← iOS 앱 (Xcode에서 ios/MyMovieLog.xcodeproj 를 연다)
├── docs/      ← 앱 아이콘 등 공용 자료
└── licenses/  ← 글꼴 라이선스
```

두 앱은 같은 "버터 크림" 디자인(크림 바탕, 버터 노랑, 캐러멜 글자, 제목 글꼴 Jua, 라이트 테마)을 씁니다.

---

## 공통 준비

두 앱 모두 아래 준비가 끝나야 실행할 수 있습니다. 플랫폼별 빌드 방법은 각 폴더의 README를 참고하세요.

### API 키 발급

1. [TMDB](https://developer.themoviedb.org) 에서 API 키 발급
2. [공공데이터포털](https://data.go.kr) 에서 **한국천문연구원\_특일 정보 서비스** 활용 신청 후 인증키 발급 (Android 공휴일 표시용)
3. [Supabase](https://supabase.com) 에서 프로젝트 생성

### Supabase 설정

아래 테이블을 만듭니다.

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
  rating numeric,
  review text,
  memo text,
  watched_at date,
  photo_urls text[] default '{}',   -- movie-photos 버킷 안의 사진 경로
  created_at timestamptz default now(),
  unique (user_id, tmdb_id)
);

-- RLS 활성화
alter table movie_records enable row level security;
create policy "Users can manage their own records"
  on movie_records for all
  using (auth.uid() = user_id);
```

사진 첨부용으로 Storage에 **비공개** 버킷 `movie-photos`를 만듭니다. 사진은 `{user_id}/{tmdb_id}/{uuid}.jpg` 경로에 저장되므로, 첫 폴더가 본인 id인 파일만 다루도록 정책을 겁니다. 기록을 삭제하거나 수정 중 사진을 빼면 앱이 Storage에서도 파일을 지우므로 delete 정책도 필요합니다.

```sql
create policy "Users can manage their own photos"
  on storage.objects for all
  using (bucket_id = 'movie-photos' and (storage.foldername(name))[1] = auth.uid()::text)
  with check (bucket_id = 'movie-photos' and (storage.foldername(name))[1] = auth.uid()::text);
```

- RLS 정책이 적용되어 있어야 다른 사용자의 데이터에 접근할 수 없습니다.
- Google 로그인을 쓰려면 Supabase 대시보드 > Authentication > Providers에서 Google을 설정하고, URL Configuration의 Redirect URLs에 `mymovie://login-callback`을 등록합니다. 두 앱이 같은 주소를 씁니다.

### API 키 설정

`android/local.properties` 파일을 만들고 아래 값을 채워넣습니다(`android/local.properties.template` 참고).
iOS 앱도 이 파일에서 키를 읽어 갑니다(`ios/scripts/generate_secrets.sh`).

```properties
TMDB_API_KEY=발급받은_TMDB_API_KEY
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key
PUBLIC_DATA_API_KEY=공공데이터포털_디코딩_인증키
```

> `PUBLIC_DATA_API_KEY`는 공공데이터포털 마이페이지 → 인증키 발급현황 → **[디코딩]** 탭의 키를 사용합니다.  
> 키가 없으면 캘린더 공휴일 색상 표시만 비활성화되고 나머지 기능은 정상 동작합니다.

> `local.properties`는 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.

---

## 라이선스

MIT

제목 글꼴 [Jua](https://fonts.google.com/specimen/Jua)는 SIL Open Font License 1.1을 따릅니다 ([licenses/OFL-Jua.txt](licenses/OFL-Jua.txt)).
