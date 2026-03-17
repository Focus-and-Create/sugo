# 서고 (Seogo)

안드로이드 앱 실행/디버깅 가이드입니다.

## Run 버튼이 안 뜰 때 (Android Studio)

1. **프로젝트 루트를 열었는지 확인**
   - 반드시 `settings.gradle`이 있는 루트 폴더를 엽니다.
2. **Gradle Sync 강제 실행**
   - `File > Sync Project with Gradle Files`
3. **SDK / JDK 확인**
   - `compileSdk 34`, `targetSdk 34`가 필요하므로 SDK 34 설치
   - Gradle JDK는 17 사용
4. **Run Configuration 수동 생성**
   - `Run > Edit Configurations... > + > Android App`
   - Module: `app`

## Build/Sync 에러 확인 방법

### Android Studio에서 보기

- `View > Tool Windows > Build` 열기
- 상단에서 `Build` 혹은 `Sync` 로그 확인
- 우상단 필터를 `All`로 바꾸고, 빨간 줄(첫 번째 원인)부터 확인

### 터미널에서 보기 (권장)

```bash
./gradlew :app:tasks
./gradlew :app:assembleDebug --stacktrace
./gradlew :app:assembleDebug --stacktrace --info
```

- 첫 번째 명령에서 Gradle 구성이 정상인지 빠르게 확인
- 두 번째/세 번째 명령으로 상세 원인 확인

## 기기 연결은 되는데 실행이 안 될 때

```bash
adb devices
./gradlew :app:installDebug
adb shell am start -n com.seogoapp/.MainActivity
```

- `adb devices` 결과가 `device`여야 정상
- `unauthorized`면 휴대폰에서 USB 디버깅 허용 팝업 재승인

## 자주 막히는 포인트

- SDK 34 미설치
- JDK 17 미설정
- 프로젝트 루트 대신 하위 폴더를 열어 모듈 인식 실패
- 최초 Sync 실패 후 Run Configuration 자동 생성 누락
