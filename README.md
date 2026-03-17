# 서고 (Seogo)

안드로이드 앱 실행/디버깅 가이드입니다.

## 먼저: 지금 증상에 바로 해당되는 체크

> "Module: app 옵션이 없음", "Build Tool Window 없음", "File 메뉴에 Sync 없음"

이 3개가 동시에 보이면 대부분 **프로젝트가 Android/Gradle 프로젝트로 정상 인식되지 않은 상태**입니다.

### 1) 루트를 다시 열기 (가장 중요)

- Android Studio에서 현재 프로젝트 닫기: `File > Close Project`
- 웰컴 화면에서 `Open`
- 반드시 **`/workspace/sugo` (루트)** 선택
  - `settings.gradle`, `build.gradle`, `app/`가 보이는 폴더여야 함

`settings.gradle`에 `include ':app'`가 있으므로 루트를 열면 `app` 모듈이 잡혀야 정상입니다.

### 2) Gradle Sync 메뉴가 안 보일 때

Android Studio 버전에 따라 메뉴 위치가 바뀝니다.

- 상단 돋보기(검색) 또는 `Ctrl/Cmd + Shift + A`
- `Sync Project with Gradle Files` 검색해서 실행
- 안 나오면 `Reload All Gradle Projects` 검색해서 실행

### 3) Build 창이 안 보일 때

- `Ctrl/Cmd + Shift + A` → `Build` 검색
- `Build`, `Build Output`, `Build Analyzer` 중 표시 가능한 창 열기
- 또는 하단 바의 `Problems` 창에서 첫 에러 확인

## Run 버튼이 안 뜰 때 (정식 점검 순서)

1. **Android SDK 플러그인/프로젝트 인식 확인**
   - 좌측 프로젝트 패널에 `Gradle` 탭이 보이는지 확인
   - `app > Tasks > install > installDebug`가 보이면 인식 정상
2. **SDK / JDK 확인**
   - 이 프로젝트는 `compileSdk 34`, `targetSdk 34` 필요
   - Gradle JDK는 17 권장
3. **Run Configuration 수동 생성 (버전별 경로 차이 있음)**
   - `Run > Edit Configurations...` 또는 상단 실행 드롭다운의 `Edit Configurations`
   - `+` 눌렀을 때 `Android App`이 없으면 아직 Android 프로젝트 인식 실패 상태
   - 인식이 정상화되면 Module에서 `app` 선택 가능

## Build/Sync 에러를 강제로 보는 방법 (터미널)

이 저장소에는 `gradlew`(wrapper)가 없으므로, 로컬에 설치된 Gradle로 확인합니다.

```bash
gradle :app:tasks
gradle :app:assembleDebug --stacktrace
gradle :app:assembleDebug --stacktrace --info
```

- 첫 번째 명령에서 프로젝트 인식/설정 단계 오류를 바로 확인
- 두 번째/세 번째 명령으로 원인 상세 확인

## 기기 연결은 되는데 실행이 안 될 때

```bash
adb devices
gradle :app:installDebug
adb shell am start -n com.seogoapp/.MainActivity
```

- `adb devices` 결과가 `device`여야 정상
- `unauthorized`면 휴대폰에서 USB 디버깅 허용 팝업 재승인

## 자주 막히는 포인트

- SDK 34 미설치
- JDK 17 미설정
- 프로젝트 루트 대신 하위 폴더를 열어 모듈 인식 실패
- Sync 실패 후 Android 프로젝트 인식이 끊겨 `Android App` 구성 자체가 안 뜸
