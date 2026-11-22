# Giao diện và lưu trữ

MainActivity và các DialogFragment lấy cùng FinanceViewModel qua ViewModelProvider ở phạm vi Activity. FinanceDatabase.open dùng application context để mở file riêng student-finance.db. Lần mở đầu tiên chỉ tạo danh mục thu chi tiếng Việt. Người dùng nhập số tiền thực tế qua form.

1. Vào Vốn vay, tạo khoản vay trước khi thêm đợt giải ngân. Mỗi khoản vay và đợt đều có nút sửa, xóa. Xóa bản ghi đang được tham chiếu sẽ hiển thị lý do bị chặn.
2. Trong Giao dịch, chọn loại thu/chi và danh mục từ Room. Với khoản chi, chọn Tiền cá nhân hoặc mục gồm tên khoản vay và tên đợt giải ngân. Sửa giao dịch có thể đổi nguồn. Xóa yêu cầu xác nhận.
3. Trong Ngân sách, chọn tháng và đặt hạn mức theo danh mục chi. Có thể sửa tháng, danh mục, hạn mức hoặc xóa. Tiến độ lấy từ chi thực tế trong sổ và hiển thị cả phần vượt.
4. Tổng quan lọc theo tháng. Giải trình lọc thêm theo khoản vay và liệt kê các khoản chi từ những đợt thuộc khoản vay đó. Form giao dịch có thể chọn ảnh hoặc PDF qua bộ chọn tệp Android. Mỗi lần lưu có thể thêm một chứng từ. Tên tệp, URI và kiểu MIME được lưu cùng giao dịch trong một transaction. Ứng dụng chỉ lưu metadata.

EditorDialog tạo lệnh ghi qua FinanceViewModel, giữ form trong lúc chờ kết quả và hiển thị lỗi nếu bị từ chối. Repository kiểm tra và ghi trong một Room transaction. Sau mỗi lệnh, ViewModel đọc snapshot nhất quán trên luồng I/O rồi phát LiveData cho các tab. Khi khởi động lại, ViewModel đọc lại file Room. Mỗi tháng, monthlySummary tính số dư, thu, chi, ngân sách và giải trình từ cùng một snapshot.

Chạy `:app:testDebugUnitTest` để kiểm tra Room với SQLite native, gồm đóng/mở lại database trên đĩa. Ảnh trong [screenshots](screenshots/) thuộc giai đoạn thiết kế UI trước khi tích hợp Room.
