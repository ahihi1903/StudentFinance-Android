# Yêu cầu MVP StudentFinance

Sinh viên ghi nhận tiền thực nhận, theo dõi tiền đã dùng và tự lập bảng giải trình. Một người dùng trên một thiết bị, nhập liệu thủ công, đơn vị đồng Việt Nam. Hồ sơ khoản vay chỉ theo dõi hạn mức và giải ngân. Tính lãi, lịch trả nợ, kết nối ngân hàng, tài khoản trực tuyến và đồng bộ nằm ngoài phạm vi.

| ID | Chức năng | Dữ liệu và luồng chính | Đầu ra để nghiệm thu |
|---|---|---|---|
| F1 | Khoản vay và giải ngân | Nhập tên, hạn mức → chọn khoản vay → ghi ngày và số tiền từng đợt | Hiện đã nhận và hạn mức chưa giải ngân. Tổng các đợt không vượt hạn mức. |
| F2 | Thu chi và nguồn tiền | Chọn thu/chi, ngày, danh mục, số tiền và nguồn → lưu → xem hoặc sửa, xóa sau xác nhận | Thu cá nhân hoặc chi từ đúng một nguồn. Mọi thay đổi cập nhật số dư và thống kê. |
| F3 | Ngân sách tháng | Chọn tháng và danh mục chi → đặt hoặc sửa hạn mức → cộng các khoản chi trong tháng | Hiện đã chi, còn lại và số vượt. Mỗi cặp tháng–danh mục có một hạn mức. |
| F4 | Dashboard | Chọn tháng → đọc tổng thu cá nhân, giải ngân, chi và chi theo danh mục → tính chênh lệch trong tháng | Các tổng khớp sổ, tháng rỗng hiển thị 0. |
| F5 | Giải trình vốn vay | Chọn khoản vay và khoảng ngày → xem trước từng đợt, khoản chi và tổng → chọn PDF/CSV và nơi lưu | Hai định dạng dùng cùng dữ liệu. Tệp phục vụ tự đối chiếu, việc chấp nhận tùy đơn vị tiếp nhận. |

## Quy tắc nghiệp vụ

- Mỗi đợt giải ngân là một nguồn vốn riêng. Nguồn cá nhân gồm tiền thu cá nhân trừ chi cá nhân. Giải ngân được ghi một lần trong bảng đợt, không tạo thêm dòng thu.
- Một khoản chi dùng đúng một nguồn. Nếu dùng hai nguồn, người dùng ghi hai khoản chi. Chi vốn vay phải từ ngày nhận tiền trở đi và tổng chi của đợt không vượt số đã nhận. Cho phép số dư cá nhân âm để người dùng ghi lại chi tiêu khi chưa nhập đủ lịch sử.
- Số tiền, hạn mức đều là số nguyên dương VND. Ngày nghiệp vụ do người dùng chọn theo lịch địa phương. Khoảng báo cáo gồm cả hai đầu. Thống kê tháng từ ngày 1 đến trước ngày 1 tháng sau.
- Chặn xóa khoản vay đã có đợt, xóa đợt đã có chi, giảm hạn mức dưới tổng đã nhận hoặc sửa đợt làm mất tính hợp lệ của chi đã ghi. Khi đổi nguồn hay sửa giao dịch, kiểm tra trên trạng thái sau thay đổi.
- Ngân sách cộng chi từ mọi nguồn. Vượt ngân sách chỉ cảnh báo, vẫn cho lưu nếu nguồn hợp lệ. Dashboard tách thu cá nhân và giải ngân. Chênh lệch tháng = thu cá nhân + giải ngân − tổng chi.
- Báo cáo theo khoản vay gồm tồn đầu kỳ (giải ngân trừ chi trước kỳ), nhận trong kỳ, chi trong kỳ và tồn cuối kỳ. Liệt kê ngày, tên đợt hoặc mã đợt, loại dòng, danh mục khi có, số tiền và ghi chú. Khoản chi trong kỳ vẫn xuất hiện khi đợt tương ứng giải ngân trước kỳ.

## Yêu cầu phi chức năng và cách kiểm tra

| ID | Yêu cầu | Kiểm chứng khi triển khai |
|---|---|---|
| N1 | Toàn bộ luồng nghiệp vụ chạy offline và lưu trong Room nội bộ | Bật chế độ máy bay, ghi dữ liệu, đóng ứng dụng, mở lại và xuất báo cáo. |
| N2 | Toàn vẹn dữ liệu | Ràng buộc khóa ngoại, khóa duy nhất và transaction cho kiểm tra–ghi. Thử ghi vượt vốn hoặc trùng ngân sách, kiểm tra không có dữ liệu dở dang. |
| N3 | Giao diện XML Material tiếng Việt | Kiểm tra nhãn, lỗi ở trường nhập, nút có tên đọc được, số tiền có đơn vị và màn hình rỗng trên thiết bị minSdk. |
| N4 | Truy vấn và xuất tệp ngoài luồng UI | Khi xuất báo cáo nhiều trang vẫn thao tác được với hệ thống. Chỉ báo lưu xong sau khi đóng tệp thành công. |
| N5 | Quyền truy cập tối thiểu | Dữ liệu riêng của ứng dụng, chọn tệp đích qua bộ chọn Android. Kiểm tra manifest hợp nhất, không yêu cầu mạng hoặc quyền đọc toàn bộ bộ nhớ cho nghiệp vụ. |

Các tình huống nghiệm thu chi tiết ở [User Stories](user-stories.md).
