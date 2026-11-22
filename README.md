# StudentFinance

Ứng dụng Android offline bằng tiếng Việt cho sinh viên quản lý thu chi, ngân sách và việc sử dụng vốn vay. Năm tab Material XML dùng chung dữ liệu Room nội bộ. Màn Giải trình xuất PDF và CSV theo khoản vay và tháng qua bộ chọn tệp Android. Có thể thêm, sửa, xóa khoản vay, đợt giải ngân, giao dịch và ngân sách theo tháng.

## Bản demo 0.2

Cài [StudentFinance Demo](app/build/outputs/apk/demo/app-demo.apk) để mở sổ ví dụ riêng gồm 15 giao dịch. Chọn tháng mở app lần đầu để xem 14 giao dịch, thu cá nhân 2.100.000 đ, tổng chi 1.120.000 đ và vốn vay cuối kỳ 2.095.000 đ. Một giao dịch ở tháng trước minh họa chuyển số dư đầu kỳ. Để tự build, chạy `.\gradlew.bat :app:assembleDemo`.

## Mở và build

1. Mở thư mục chứa settings.gradle.kts trong Android Studio.
2. Chọn Gradle JDK 21, cài Android SDK Platform 36. SDK được dò bằng ANDROID_HOME hoặc sdk.dir trong local.properties riêng của máy.
3. Đồng bộ Gradle, chọn cấu hình app và thiết bị API 23 trở lên để chạy. Máy mới cần tải thư viện ở lần build đầu.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

APK nằm tại `app/build/outputs/apk/debug/app-debug.apk`. Để cài trên điện thoại Android 6.0 trở lên, bật USB debugging, chọn thiết bị trong Android Studio và nhấn Run. Với Android SDK Platform Tools trên PATH, có thể cài bằng lệnh sau rồi mở StudentFinance trên thiết bị.

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Bản debug cùng chữ ký sẽ nâng cấp dữ liệu Room v1 lên v2 khi mở ứng dụng.

## Môi trường và kết quả đã kiểm tra

Workspace ban đầu trống, chưa có Git repository. Tìm thấy Android Studio tại `F:\Android\Android Studio`, build 261.26222.65.2613.15948027. ANDROID_HOME trỏ tới `F:\Android\SDK`, có platform 36, 37.0 và build-tools 35.0.0, 36.0.0. JAVA_HOME trỏ đến JDK 21.0.8, còn java trên PATH báo 27. Gradle Wrapper dùng JAVA_HOME.

Build sử dụng Gradle 9.5.0, Android Gradle Plugin 9.3.3 với Kotlin tích hợp, compileSdk/targetSdk 36, minSdk 23 và Java target 17. Module khai báo AppCompat 1.7.1, Material 1.13.0, Lifecycle ViewModel 2.9.4 và Room runtime 2.6.1. Bản UI bổ sung Fragment 1.8.9 và LiveData 2.9.4.

## Cấu trúc

```text
gradle/wrapper/                     Gradle Wrapper dùng chung
app/src/main/AndroidManifest.xml    Activity khởi động
app/src/main/java/vn/studentfinance/
    ui/MainActivity.kt              Điều hướng giữa năm Fragment
    ui/{loan,ledger,budget,dashboard,report}/
    data/{local,repository}/
    report/
app/src/main/res/{layout,values}/   Giao diện XML và chuỗi tiếng Việt
docs/                               Đặc tả và kế hoạch
```

Các Fragment quan sát FinanceViewModel qua vòng đời của view.

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [Hướng dẫn sử dụng](docs/huong-dan-su-dung.md) | Lập sổ, chọn kỳ và nơi lưu, mở PDF/CSV |
| [Kịch bản demo 4 phút](docs/kich-ban-demo.md) | Chuẩn bị dữ liệu và đối chiếu từng bước |
| [Biên bản QA báo cáo tháng](docs/report-qa.md) | Test case, kết quả thực tế, bằng chứng và tình trạng lỗi |
| [Yêu cầu](docs/requirements.md) | Phạm vi, quy tắc nghiệp vụ và yêu cầu phi chức năng |
| [User Stories](docs/user-stories.md) | Tình huống nghiệm thu với số liệu đối chiếu |
| [Use Case](docs/use-cases.md) | Tác nhân, luồng chính, ngoại lệ và hậu điều kiện |
| [Bàn giao Room v1](docs/room-verification.md) | Kết quả build, unit test, kiểm tra emulator và danh sách tệp |
| [ERD](docs/erd.md) | Quan hệ dữ liệu, khóa, quy tắc tổng hợp |
| [Kết nối UI với Room](docs/ui-integration.md) | Luồng đọc, ghi, kiểm tra dữ liệu và cập nhật giao diện |
| [Kiến trúc](docs/architecture.md) | MVVM, Repository, Room và luồng xuất tệp |
| [Backlog và WBS](docs/backlog-wbs.md) | Phân công ba người, lịch 14 ngày, phụ thuộc và quy trình Git |

## Dữ liệu và kiểm thử

Schema xuất bởi Room compiler nằm trong [app/schemas](app/schemas/). Bộ [unit test](app/src/test/java/vn/studentfinance/FinanceRepositoryTest.kt) dùng Robolectric và SQLite native. Gradle tạo báo cáo tại `app/build/reports/tests/testDebugUnitTest/index.html`. Lần chạy đầu cần tải Android runtime của Robolectric.
