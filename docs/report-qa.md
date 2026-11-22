# Biên bản kiểm thử báo cáo tháng

Ngày chạy 22/11/2026. Bản debug StudentFinance, Room schema v2. Bộ test JVM dùng Robolectric API 34 với SQLite native. Kiểm thử thiết bị dùng AVD Android API 37 và trình chọn tệp hệ thống. Các số liệu bên dưới là fixture QA, tách khỏi logic ứng dụng.

## Dữ liệu dùng cho R01–R12

Khoản A giải ngân 900.000 đ ngày 01/10, chi 200.000 đ ngày 31/10. Tháng 11 nhận thêm 500.000 đ ngày 01/11; chi 120.000 đ ngày 01/11 từ đợt tháng 10 và 80.000 đ ngày 30/11 từ đợt mới. Ngày 01/12 chi thêm 10.000 đ. Khoản chi 120.000 đ có hai chứng từ. Cùng tháng 11 còn có thu cá nhân 100.000 đ, chi cá nhân 60.000 đ, khoản B giải ngân 400.000 đ và chi 30.000 đ. Người lập của A là Nguyễn Ánh, đơn vị cho vay là Quỹ học tập.

| ID | Dữ liệu và bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
|---|---|---|---|---|
| R01 | Đọc A, tháng 11. Đối chiếu đầu kỳ, nhận, chi, cuối kỳ. | 700.000 + 500.000 − 200.000 = 1.000.000 đ. | Cả bốn giá trị khớp. | PASS |
| R02 | Đọc A rồi B cùng tháng. Kiểm tra ID và thứ tự phát sinh. | A chỉ có đợt mới và hai khoản chi. B có đợt và khoản chi riêng. | A trả về new, first, last; B trả về other, otherLoan. Thu/chi cá nhân được loại. | PASS |
| R03 | Đổi A sang tháng 12. | Đầu 1.000.000, nhận 0, chi 10.000, cuối 990.000. | Khớp cả bốn giá trị, đúng biên ngày 01/12. | PASS |
| R04 | Chọn tháng 01/2027 không phát sinh. | Danh sách rỗng; đầu và cuối kỳ 990.000. | Rỗng, tồn đầu bằng tồn cuối. | PASS |
| R05 | Đọc dòng first, đã gắn Hóa đơn.pdf và Ảnh.jpg. | Một dòng Học tập, 120.000 đ và hai tên chứng từ. | Khớp danh mục, số tiền và danh sách hai tệp. | PASS |
| R06 | Ghi họ tên, đơn vị của A rồi đọc cả A và B. | A giữ đúng hai trường, B không dùng nhầm đơn vị của A. | Nguyễn Ánh / Quỹ học tập; đơn vị của B trống. | PASS |
| R07 | Lưu tên toàn dấu cách; thử xuất B chưa có thông tin. | Từ chối cả hai thao tác. | Hai thao tác báo lỗi kiểm tra dữ liệu. | PASS |
| R08 | Xuất CSV với tiếng Việt, dấu phẩy, dấu nháy, xuống dòng và hai tên tệp. | UTF-8 BOM, trường chứa dấu nháy được escape, đủ khai báo tự khai. | Kiểm tra byte/text đạt cho BOM, ghi chú, tên tệp nhiều dòng và khai báo. | PASS |
| R09 | Giữ bản xem trước A rồi chuyển first sang nguồn cá nhân và đọc lại. | Bản đã giữ chi 200.000; bản mới chi 80.000. | Hai số độc lập và đúng. | PASS |
| R10 | Đọc tháng 202613 và ID khoản vay không tồn tại. | Từ chối cả hai truy vấn. | Cả hai bị từ chối. | PASS |
| R11 | Cho luồng đích ném IOException khi ghi CSV. | Bộ ghi trả lỗi cho bên gọi. | Nhận đúng IOException. Đây là lỗi I/O được chủ động đưa vào test. | PASS |
| R12 | Tạo DB theo SQL schema v1, nhập khoản cũ và đợt 300 đ tháng 10, mở qua migration v2, ghi thông tin người lập. | Giữ nguyên tồn đầu 300 đ, lưu được đơn vị cho vay mới. | Room mở thành công; đọc lại 300 đ và đơn vị “Quỹ”. | PASS |

Mã tái hiện nằm trong [MonthlyReportTest.kt](../app/src/test/java/vn/studentfinance/MonthlyReportTest.kt). Bộ [FinanceRepositoryTest.kt](../app/src/test/java/vn/studentfinance/FinanceRepositoryTest.kt) kiểm tra hồi quy CRUD, ngân sách, lưu bền, phân nguồn, chi vượt vốn, đồng thời và biên số nguyên.

## Cách chạy lại

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class vn.studentfinance.ReportDeviceTest vn.studentfinance.test/androidx.test.runner.AndroidJUnitRunner
```

R13 dùng một database Room riêng trong bộ nhớ trên emulator. Tạo 18 khoản chi ngày 01–18/11/2026, mỗi khoản bằng số thứ tự × 10.000 đ. Dữ liệu tồn đầu 5.000.000 đ, một chứng từ cho mỗi giao dịch. Mã kiểm thử gọi đúng bộ ghi dùng trong ứng dụng và mở từng trang bằng Android PdfRenderer. Bài test lưu tệp vào cache của app debug để kiểm tra độc lập.

## Kiểm thử thiết bị và đọc lại tệp

| ID | Dữ liệu và bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
|---|---|---|---|---|
| R13 | Chạy ReportDeviceTest với 18 khoản chi; mở và render từng trang PDF. | Nhiều trang đọc được, tồn cuối 3.290.000 đ. | Android PdfRenderer mở và render đủ 4 trang A4. Instrumentation báo OK (1 test). | PASS |
| R14 | Render lại PDF bằng Poppler; xem bốn trang và trích chữ bằng pypdf. | Hiển thị đúng dấu, đủ họ tên, khai báo, số tiền và 18 tên chứng từ. | Đọc được Nguyễn Thị Ánh, Quỹ hỗ trợ sinh viên, 3.290.000 đ và Hóa đơn 1–18.pdf. Mỗi khoản chi trong fixture nằm trọn trên một trang. | PASS |
| R15 | Phân tích CSV từ cùng snapshot bằng csv.reader, UTF-8-sig, xử lý trường nhiều dòng. | 16 cột nhất quán, 1 dòng tổng và 18 phát sinh, tổng chi 1.710.000. | 20 bản ghi kể cả tiêu đề. Mỗi bản ghi 16 trường. Giữ đúng dấu phẩy, dấu nháy, xuống dòng và tên Hóa đơn 18.pdf. | PASS |
| R16 | Trên tab Giải trình, bấm PDF khi thiếu người lập. Điền hai trường, chọn CSV, bấm Back ở Downloads. | Giữ form khi thiếu tên. Hủy chọn nơi lưu không tạo tệp. | Focus về trường người lập; sau Back trở về Giải trình, không có CSV mới trong Downloads. | PASS |
| R17 | Với khoản RoomQA đã nhận 700.000 đ tháng 11, ghi chi Học tập 120.000 đ. Xem Ngân sách, Tổng quan, Giải trình, chuyển tháng 12 và quay lại. | Ngân sách 150.000 còn 30.000. Dashboard và Giải trình cùng còn 580.000. Tháng 12 chuyển tồn, tháng 11 còn dòng chi. | Cả ba màn khớp. Chuyển kỳ và quay lại đúng tháng, đúng số tiền. | PASS |
| R18 | Cài APK cuối đè bản cũ, mở lại. Xuất RoomQA tháng 11 vào Downloads bằng hai nút PDF/CSV, xác nhận SAVE. Đọc lại hai tệp bằng trình phân tích độc lập. | Giữ người lập, đơn vị; hai tệp có cùng kỳ, khai báo và số kỳ. | Đọc lại QA Student / Quy QA. PDF 91.199 byte, CSV 883 byte. Hai tệp khớp đầu 0, nhận 700.000, chi 120.000, cuối 580.000. Crash buffer rỗng sau luồng thử. | PASS |

**Tổng kết lần chạy cuối.** Build APK debug, APK androidTest và unit test thành công trong 22 giây. JUnit JVM ghi 22 test đạt (12 ca báo cáo và 10 hồi quy), 0 lỗi, 0 bỏ qua. Trên thiết bị có thêm một test instrumentation đạt. Ma trận báo cáo R01–R18 gồm 18 PASS, 0 FAIL, 0 BLOCKED; R14–R18 là các kiểm tra bổ sung tệp và giao diện, không cộng lần hai vào số unit test.

Bằng chứng gồm [kết quả instrumentation](qa/instrumentation.txt), [PDF 4 trang](qa/bao-cao-qa.pdf), [CSV cùng dữ liệu](qa/bao-cao-qa.csv) và [báo cáo JUnit cục bộ](../app/build/reports/tests/testDebugUnitTest/index.html). Hai tệp trong docs/qa là kết quả bộ 18 giao dịch trên Room thử nghiệm, độc lập với luồng chọn Downloads của RoomQA.

## Lỗi và tình trạng xử lý

| ID | Phát hiện | Cách xử lý và kiểm tra lại | Trạng thái |
|---|---|---|---|
| D01 | Bản build đầu không biên dịch được lời gọi use trên PdfDocument; test nâng cấp cũng gặp lỗi tương tự khi đóng RoomDatabase. | Dùng try/finally với close cho hai tài nguyên. Build cuối và test migration đạt. | Đã sửa |
| D02 | Khi render fixture ban đầu, chứng từ số 4, 10 và 16 nằm một mình ở trang sau. | Đo chiều cao nội dung trước khi bắt đầu mỗi giao dịch. Xuất lại và kiểm tra ảnh bốn trang. Các trang 1–3 kết thúc sau chứng từ số 3, 9, 15; trang 4 kết thúc sau số 18. | Đã sửa |
| D03 | Compiler cảnh báo SOFT_INPUT_ADJUST_RESIZE trong EditorDialog của các form nhập sổ. | Cảnh báo có từ phiên bản trước; build thành công và luồng nhập đã chạy trên API 37. | Còn mở, không chặn build |

## Phạm vi bàn giao

Đã hoàn thành báo cáo theo khoản vay và tháng, thông tin người lập/đơn vị, chi tiết đợt và giao dịch, danh mục và tên chứng từ, PDF, CSV, chọn nơi lưu và lưu bền thông tin báo cáo trong Room. Schema v2 thêm report_headers; migration v1 giữ sổ cũ. Không còn hạng mục chức năng báo cáo đang chờ triển khai trong yêu cầu này.

Kiểm chứng đã thực hiện trên SQLite native API 34 và AVD API 37 với bộ nhớ cục bộ. Chưa thử trên điện thoại vật lý, nhà cung cấp tệp đám mây hay dung lượng đĩa thực sự đầy. R11 kiểm tra luồng ghi lỗi có kiểm soát. Báo cáo liệt kê tên chứng từ; việc lưu trữ hoặc nhúng nội dung tài liệu gốc nằm ngoài phạm vi.
