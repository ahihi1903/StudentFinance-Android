# Bàn giao Room v1

## Build và unit test

Lệnh đã chạy trên Windows với JDK 21 và Gradle Wrapper 9.5.0:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --offline --console=plain --max-workers=2
```

Build cuối thành công trong 32 giây. JUnit ghi nhận 10 test, 0 thất bại, 0 lỗi, 0 bỏ qua. Bộ test chạy trong 19,343 giây trên Robolectric API 34 với SQLite native. Test lưu bền dùng database trên đĩa, đóng instance rồi mở lại. Chín test còn lại dùng Room in-memory. Cache đã có đủ thành phần khi chạy lệnh offline.

| Nội dung đã kiểm tra | Kết quả quan sát |
|---|---|
| CRUD khoản vay, đợt, giao dịch, ngân sách | Tạo, đọc lại, cập nhật, xóa qua repository và DAO thật. |
| Phân tách nguồn | Thu và chi cá nhân giữ nguyên vốn. Sửa khoản chi sang nguồn khác hoặc xóa cập nhật vốn khả dụng. |
| Kiểm tra giới hạn | Từ chối số tiền âm, bằng 0, nguồn trống, thiếu đợt, thu từ vốn vay, ngày sai, vượt hạn mức hoặc vượt vốn. Ghi thất bại giữ nguyên sổ. |
| Bảo toàn quan hệ | Chặn sửa ngày hoặc giảm số tiền đợt nếu làm sai lịch sử chi. Chặn xóa đợt/khoản vay đang được dùng. |
| Ngân sách và báo cáo | Kiểm tra chi cả hai nguồn, loại trừ danh mục và tháng khác, theo dõi phần vượt, cấm ngân sách trùng. Tách hai khoản vay và chuyển tồn sang tháng sau. |
| Đồng thời và độ chính xác | Hai lệnh chi cạnh tranh chỉ một lệnh được ghi khi vốn không đủ. Kiểm tra vốn còn đúng 1 đồng sát Long.MAX_VALUE và tổng nhiều dòng lớn hơn Long.MAX_VALUE. |
| Chứng từ | Metadata hợp lệ đọc lại qua Room. Xóa giao dịch xóa metadata. Metadata sai làm rollback cả giao dịch. |
| Mở lại database | Giữ khoản vay, giao dịch, khóa nguồn, số vốn và ngân sách sau khi đóng/mở lại. |

## Kiểm tra trên emulator API 37

Nhập khoản vay hạn mức 1.000.000 đ, giải ngân 700.000 đ, chi Học tập 120.000 đ từ đợt đó. Dashboard hiển thị vốn còn 580.000 đ, thu cá nhân 0 đ. Đặt ngân sách 150.000 đ cho Học tập, màn hình hiện còn 30.000 đ. Sau `am force-stop` và mở lại, số vốn vẫn là 580.000 đ, báo cáo khoản vay hiển thị chi 120.000 đ.

Trên APK cuối, sửa khoản chi sang tiền cá nhân và chọn RoomQA.png qua bộ chọn tệp Android. Vốn trở lại 700.000 đ. Mở lại giao dịch đọc được tên chứng từ. Hủy xác nhận xóa giữ bản ghi; xác nhận xóa đưa ngân sách về còn 150.000 đ.

## Tệp tạo hoặc sửa

Đường dẫn Kotlin/Java dưới đây tính từ `app/src/main/java/vn/studentfinance/`, XML tính từ `app/src/main/res/`.

| Nhóm | Tệp |
|---|---|
| Database và cấu hình | `app/build.gradle.kts`, `data/local/FinanceDatabase.java`, `data/local/FinanceDao.java`, [schema JSON v1](../app/schemas/vn.studentfinance.data.local.FinanceDatabase/1.json) |
| Domain và ViewModel | `data/repository/FinanceRepository.kt`, `ui/FinanceViewModel.kt` |
| Giao diện Kotlin | `ui/FinanceFragment.kt`, `ui/EditorDialog.kt`, `ui/SpendingChart.kt`, `ui/budget/BudgetFragment.kt`, `ui/ledger/TransactionsFragment.kt`, `ui/loan/LoansFragment.kt`, `ui/dashboard/DashboardFragment.kt` |
| XML | `layout/fragment_dashboard.xml`, `layout/fragment_budget.xml`, `layout/form_loan.xml`, `layout/form_transaction.xml`, `layout/item_budget.xml`, `layout/item_record.xml`, `layout/record_actions.xml`, `values/strings.xml` |
| Test | [FinanceRepositoryTest.kt](../app/src/test/java/vn/studentfinance/FinanceRepositoryTest.kt) |
| Tài liệu | `README.md`, `docs/architecture.md`, `docs/erd.md`, `docs/ui-integration.md`, `docs/room-verification.md` |

ReportsFragment tiếp tục nhận ScreenData qua FinanceFragment nên dùng được dữ liệu Room qua đường đọc chung. Bản v1 là phiên bản database đầu tiên. Khi thay đổi schema ở bản tiếp theo, cần bổ sung migration từ v1.

APK debug ở `app/build/outputs/apk/debug/app-debug.apk`. Kết quả JUnit và báo cáo HTML ở `app/build/test-results/testDebugUnitTest/` và `app/build/reports/tests/testDebugUnitTest/`. Compiler còn cảnh báo API cũ SOFT_INPUT_ADJUST_RESIZE trong form.
