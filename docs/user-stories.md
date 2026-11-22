# User Stories và Acceptance Criteria

Tác nhân của tất cả câu chuyện là sinh viên. Mỗi hàng là một kịch bản có thể kiểm tra bằng giao diện. Số tiền trong bảng tính bằng đồng.

| Story | Tôi muốn… để… | Cho trước | Khi | Thì |
|---|---|---|---|---|
| US1 / F1 | Ghi từng đợt để biết đã nhận bao nhiêu | Vay A có hạn mức 5.000.000 | Ghi hai đợt 2.000.000 và 1.000.000 | Đã nhận 3.000.000, còn có thể giải ngân 2.000.000, có hai nguồn vốn. |
| US1 | Bảo toàn lịch sử vay | Vay A như trên | Nhập thêm 2.000.001 hoặc xóa A | Báo lỗi tiếng Việt, giữ nguyên hai đợt. |
| US2 / F2 | Chọn nguồn cho từng khoản chi để biết vốn còn lại | Đợt D nhận 2.000.000 ngày 03/04 | Ghi chi học tập 300.000 ngày 04/04 từ D, sửa thành 350.000 | D còn 1.650.000, danh sách có một khoản chi 350.000. |
| US2 | Sửa sai và xóa giao dịch | Có khoản chi 350.000 nêu trên | Chuyển nguồn sang cá nhân rồi xác nhận xóa | D trở lại 2.000.000 ngay sau chuyển. Sau xóa, chi cá nhân giảm 350.000. |
| US2 | Kiểm tra dữ liệu trước khi lưu | D còn nguyên 2.000.000 | Thử chi 2.000.001, chi trước 03/04 hoặc nhập số 0 | Từng lần bị từ chối, số dư giữ nguyên. |
| US3 / F3 | Đặt hạn mức để theo dõi chi theo tháng | Ngân sách học tập tháng 4 là 500.000 | Chi cá nhân 200.000 và chi vốn vay 350.000 trong tháng, chi thêm 100.000 ngày 01/05 | Tháng 4 đã chi 550.000, vượt 50.000. Sửa hạn mức thành 600.000 thì còn 50.000 và chỉ có một ngân sách. |
| US4 / F4 | Xem dòng tiền tháng để đối chiếu sổ | Trong tháng chỉ thu cá nhân 500.000, nhận giải ngân 2.000.000, chi học tập 300.000 | Mở Dashboard tháng đó rồi chọn một tháng rỗng | Hiện lần lượt 500.000, 2.000.000, 300.000 và chênh lệch +2.200.000. Học tập chiếm toàn bộ chi. Tháng rỗng có các tổng bằng 0. |
| US5 / F5 | Xuất giải trình để kiểm tra cách dùng vốn của một khoản vay | Vay A tồn đầu 700.000, nhận 500.000 ngày đầu kỳ, chi 200.000 ngày cuối kỳ. Có thêm chi cá nhân và chi của vay B | Chọn A, xem trước, xuất cả PDF và CSV | Cả hai tệp mở được, chỉ có hai dòng phát sinh của A, tồn cuối 1.000.000. PDF đọc được tiếng Việt, CSV UTF-8 giữ đúng dấu, dấu phẩy và dấu ngoặc kép trong ghi chú. |
| US5 | Kiểm soát nơi lưu | Có bản xem trước, kể cả kỳ rỗng | Hủy bộ chọn tệp hoặc gặp lỗi ghi | Không báo xuất thành công. Khi kỳ rỗng vẫn xem được tồn đầu và cuối bằng nhau. |

Chạy lại một vòng US1–US5 trong chế độ máy bay sau khi khởi động lại ứng dụng. Các ví dụ là dữ liệu nghiệm thu dự kiến, chưa phải kết quả chạy thử.
