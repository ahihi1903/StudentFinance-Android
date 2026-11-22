# Backlog, WBS và kế hoạch 14 ngày

Quy ước D1–D14 là 14 ngày làm việc mẫu, chưa gắn lịch cá nhân. TV1 phụ trách nền tảng, dữ liệu vay và tích hợp. TV2 phụ trách sổ thu chi và ngân sách. TV3 phụ trách phân tích, báo cáo và điều phối nghiệm thu. Mỗi hàng WBS có một người chịu trách nhiệm bàn giao.

## Product Backlog theo thứ tự tích hợp

| Thứ tự | Hạng mục | Ưu tiên | Story / tiêu chí hoàn thành | WBS |
|---|---|---|---|---|
| 1 | Khung Android, schema và hợp đồng đọc/ghi | P0 | Build debug, kiểm tra Room và khóa ngoại | W01–W03 |
| 2 | Hồ sơ vay và giải ngân | P0 | US1, nguồn đợt dùng được cho US2 | W04 |
| 3 | Thu chi và chọn nguồn | P0 | US2, các thay đổi phản ánh vào sổ | W05 |
| 4 | Ngân sách theo danh mục/tháng | P1 | US3, tính trên tất cả nguồn | W06 |
| 5 | Dashboard tháng | P1 | US4, khớp sổ và xử lý tháng rỗng | W07–W08 |
| 6 | Giải trình PDF/CSV | P1 | US5, đối chiếu snapshot với cả hai tệp | W09–W10 |
| 7 | Tích hợp, kiểm thử offline và bàn giao | P0 | N1–N5 và đủ US1–US5 | W11–W16 |

P0 là đường tích hợp cần ưu tiên. Cả năm chức năng đều thuộc bản bàn giao D14.

## WBS và trách nhiệm

| WBS | Công việc | Chủ trì | Lịch (hạn cuối) | Bàn giao kiểm tra được | Phụ thuộc |
|---|---|---|---|---|---|
| W01 | Chốt mô hình, cấu hình app, đường dẫn package và điều hướng | TV1 | D1–D2 | Build khung, sơ đồ màn hình, chữ ký repository | — |
| W02 | Ánh xạ ERD, cấu hình compiler, tạo DB, DAO, seed | TV1 | D3–D4 | Schema v1, test mở DB và quan hệ, API truy vấn nguồn | W01 |
| W03 | Chốt ví dụ nghiệp vụ và form sổ | TV2 | D1–D3 | Bộ dữ liệu US1–US4 và form nhập dùng dữ liệu mẫu | — |
| W04 | CRUD khoản vay và đợt, kiểm tra hạn mức | TV1 | D5–D7 | Luồng US1, PR có ca lỗi và kiểm tra sửa đợt | W02 |
| W05 | Ghi, sửa, xóa thu chi, kiểm tra nguồn | TV2 | D5–D8 | Sổ thật qua Room, test số dư sau sửa/xóa | W02, W03 |
| W06 | Ngân sách và cảnh báo vượt | TV2 | D9–D10 | Màn hình US3, test biên cuối tháng | W05 |
| W07 | Thiết kế Dashboard và bộ đối chiếu báo cáo | TV3 | D1–D4 | UI mẫu, bộ số kỳ rỗng và kỳ có tồn đầu | — |
| W08 | Truy vấn Dashboard, nối ViewModel | TV3 | D5–D7 | Dashboard US4 trên dữ liệu mẫu trong Room | W02, W07 |
| W09 | Truy vấn tồn đầu và phát sinh, xem trước | TV3 | D8–D9 | Snapshot US5, test lọc khoản vay và biên ngày | W08 |
| W10 | Xuất PDF/CSV qua URI | TV3 | D10–D11 | Hai tệp mẫu cùng snapshot, thử hủy/lỗi ghi | W09 |
| W11 | Tích hợp điều hướng và hợp nhất module | TV1 | D8–D11 | Nhánh tích hợp có đủ năm màn hình, build debug | W04, nhận dần W05/W06/W08/W10 |
| W12 | Kiểm thử dữ liệu, đồng thời và hồi quy sổ | TV2 | D11–D12 | Kết quả US1–US3, lỗi tái hiện được và bản sửa | W04–W06 |
| W13 | Chạy xuyên suốt offline, đối chiếu file | TV3 | D12–D13 | Biên bản US4–US5, N1/N3/N4 và tệp nhiều trang | W10, W11 |
| W14 | Sửa lỗi tích hợp và tạo bản ứng viên | TV1 | D12–D13 | APK ứng viên, kết quả build và kiểm tra manifest | W11, nhận lỗi W12/W13 |
| W15 | Chạy lại ca lỗi trên bản ứng viên | TV2 | D13–D14 | Bảng nghiệm thu ghi thiết bị, kết quả và lỗi còn mở | W12, W14 |
| W16 | Chốt bàn giao | TV1 | D14 | Commit/tag, APK, hướng dẫn chạy và biên bản tổng hợp | W13–W15 |

## Mốc từng ngày

| Ngày | Kết quả cần đạt cuối ngày |
|---|---|
| D1 | Chốt MVP, phân vai và danh mục dữ liệu mẫu. |
| D2 | Khung app build được, thống nhất kiểu tiền, ngày, ID và giao diện repository. |
| D3 | Khai báo entity, dựng form thu chi và dữ liệu kiểm thử. |
| D4 | Chạy được DAO đầu tiên, duyệt schema và tổng kỳ vọng. |
| D5 | Tạo khoản vay, ghi giao dịch và đọc tổng tháng trên Room. |
| D6 | Nối giải ngân với nguồn chi, kiểm tra tổng theo danh mục. |
| D7 | Duyệt US1 và Dashboard trên fixture. |
| D8 | Duyệt sửa/xóa thu chi, đi từ danh sách vay sang nguồn tương ứng. |
| D9 | Có truy vấn ngân sách và bản xem trước giải trình. |
| D10 | Nghiệm thu US3, xuất tệp đầu tiên từ snapshot. |
| D11 | Tích hợp năm chức năng, có cả PDF và CSV. |
| D12 | Đối chiếu trên thiết bị ở chế độ máy bay, phân loại lỗi. |
| D13 | Đóng lỗi chặn, tạo APK ứng viên và đối chiếu tệp nhiều trang. |
| D14 | Kiểm tra lại trên APK ứng viên, ghi kết quả và bàn giao phiên bản. |

## Nhánh Git và tích hợp

Dùng main làm nhánh tích hợp ổn định. Tạo nhánh ngắn từ main theo đơn vị bàn giao, ví dụ feat/w02-room, feat/w05-ledger, feat/w10-export, fix/w13-pdf. TV1 điều phối thay đổi schema, đánh phiên bản và migration khi đã có dữ liệu cần bảo toàn. Chốt interface ở D2 để UI và truy vấn có thể phát triển với fixture.

Mỗi PR ghi WBS/US, phạm vi, thay đổi dữ liệu, lệnh build và kết quả ca kiểm tra. Ít nhất một thành viên khác review. Tác giả đồng bộ main, xử lý xung đột và chạy lại assembleDebug cùng test liên quan trước khi squash merge. Tích hợp theo cột phụ thuộc, tránh dồn merge vào D14. Chỉ gắn tag v0.1.0 sau khi đối chiếu đủ tiêu chí, ghi nhận mọi ca còn mở.
