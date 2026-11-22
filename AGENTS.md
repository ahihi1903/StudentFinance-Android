# StudentFinance-Android

Repository của nhóm là https://github.com/ahihi1903/StudentFinance-Android.git. Nhánh ổn định là `main`. Áp dụng quy trình sau cho mỗi nhiệm vụ lập trình, kể cả khi tiếp tục một phiên đã gián đoạn.

## Bắt đầu nhiệm vụ

1. Kiểm tra `git status --short --branch`, `git branch -vv`, `git remote -v` và commit HEAD. Xác minh URL fetch và push của origin trước khi gửi mã. Báo cho người dùng trước khi đổi remote khác đích.
2. Chạy `git fetch origin`. So sánh HEAD, upstream và `origin/main` bằng `git log --oneline --left-right HEAD...@{upstream}` (quote biểu thức ref trong PowerShell). Nếu nhánh chưa có upstream, kiểm tra ref tương ứng trên origin trước khi tạo mới.
3. Khi working tree sạch và nhánh có thể cập nhật fast-forward, chạy `git pull --ff-only` từ đúng upstream. Bảo toàn thay đổi chưa commit, cả phần đã stage và file chưa track. Nếu local đang bận, dùng worktree riêng từ nền đã đồng bộ khi phù hợp. Không tự stash, reset, clean hoặc đổi nhánh đè lên việc đang làm của người khác.
4. Chỉ bắt đầu sửa khi đã xác định rõ nhánh, nền remote và phạm vi thay đổi. Nếu lịch sử phân kỳ, kiểm tra từng phía và tích hợp trên nhánh làm việc. Khi không thể xác định cách giữ đúng ý nghĩa của hai bên, báo file/commit xung đột và yêu cầu quyết định.

## Nhánh và pull request

| Phạm vi | Nhánh |
|---|---|
| Vốn vay và giải ngân | `feature/loans` |
| Giao dịch và nguồn tiền | `feature/transactions` |
| Ngân sách | `feature/budget` |
| Dashboard | `feature/dashboard` |
| Giải trình PDF/CSV | `feature/reports` |

Tạo nhánh tính năng mới từ `origin/main` vừa fetch. Nếu nhánh đã tồn tại trên GitHub, theo dõi nhánh đó và kiểm tra công việc đang diễn ra. Khi cần làm riêng, tạo nhánh con theo nhiệm vụ từ nền phù hợp. Push lên nhánh làm việc và tạo hoặc cập nhật PR vào main khi có quyền. PR ghi rõ phạm vi, kết quả test và điểm chưa xác minh. Không tự merge PR vào main.

## Kết thúc nhiệm vụ

1. Đọc lại diff, chạy build/test liên quan. Với thay đổi dùng chung cho cả hai bản, chạy Gradle Wrapper với `:app:testDebugUnitTest :app:testDemoUnitTest :app:assembleDebug :app:assembleDemo`. Ghi lại test thực sự chạy, lỗi và các phần bị chặn.
2. Kiểm tra `git diff --check`, `git status`, tên và nội dung các file sẽ commit. Không đưa khóa ký, token, mật khẩu, local.properties, kho dữ liệu cá nhân, cache, APK hoặc thư mục build vào Git.
3. Stage bằng path cụ thể. Xem `git diff --cached` và chạy `git diff --cached --check`. Nếu index có sẵn thay đổi ngoài nhiệm vụ, giữ nguyên phần của người khác và tách commit của mình.
4. Khi có thay đổi đã hoàn thành, tự commit với tiền tố `feat:`, `fix:`, `refactor:`, `test:`, `docs:` hoặc `chore:`. Fetch lại và kiểm tra remote có tiến lên trước khi push. Dùng push thông thường, thiết lập upstream khi push nhánh mới. Khi bị từ chối non-fast-forward, fetch và tích hợp an toàn; không force push, xóa lịch sử hoặc ghi đè code đồng đội.
5. Sau push, so sánh `git rev-parse HEAD` với SHA của đúng `refs/heads/<nhánh>` từ `git ls-remote --heads origin`. Nếu remote đã tiến thêm, fetch và kiểm tra commit còn là tổ tiên của nhánh đó. Báo nhánh, SHA, kết quả push, PR, build/test và vấn đề còn mở. Không suy đoán đã push thành công từ trạng thái local hoặc từ phiên trước.
