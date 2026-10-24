# 🏠 HomeCare – Website bán đồ gia dụng (Java Spring Boot)

Java 25 (LTS) • Spring Boot 3 • JPA/Hibernate • SQLite • HTML/CSS/JS thuần.
Tài liệu thiết kế & báo cáo: [`docs/BAO-CAO.md`](docs/BAO-CAO.md) • SQL: [`docs/schema.sql`](docs/schema.sql)

## Chạy trên VS Code
1. Cài **JDK 25+**, **Maven**, extension *Extension Pack for Java*.
2. Mở thư mục này, chạy `mvn spring-boot:run` (hoặc mở `App.java` → Run).
3. Trang chủ http://localhost:8080 • Khuyến mãi `/khuyen-mai.html` • Cửa hàng `/shop.html`

Tài khoản mẫu: `admin/admin123` • `nhanvien/nv123456` • `khach/khach123`

**Cơ sở dữ liệu:** SQLite được tạo tự động tại `data/homecare.sqlite`; Hibernate cập nhật cấu trúc bảng khi khởi động. Dừng ứng dụng trước khi sao chép hoặc di chuyển tệp cơ sở dữ liệu.
**Docker:** `docker compose up --build`; dữ liệu SQLite được giữ trong volume `sqlite-data`.

## Hướng dẫn sử dụng
- **Khách:** Đăng ký → lọc danh mục/tìm kiếm/sắp xếp → *Thêm vào giỏ* → tab *Giỏ* sửa số lượng/xóa → nhập giao hàng, chọn thanh toán → *Xác nhận*. Xem lại ở *Đơn hàng*; hủy được đơn "Chờ xác nhận".
- **Nhân viên:** tab *Quản trị*: danh mục, sản phẩm (thêm mô tả tối đa 1000 ký tự; chọn ảnh JPG/PNG/WEBP tối đa 2MB để xem trước và tải lên), thống kê; tab *Đơn hàng* đổi trạng thái.
- **Admin:** thêm quyền xóa sản phẩm/danh mục và quản lý tài khoản (thêm, sửa tên, đổi vai trò, khóa/mở khóa).

| Chức năng | Khách | Nhân viên | Admin |
|---|:-:|:-:|:-:|
| Xem/tìm sản phẩm, giỏ hàng, đặt hàng, xem lịch sử | ✅ | ✅ | ✅ |
| Thêm/sửa danh mục, sản phẩm, upload ảnh | ❌ | ✅ | ✅ |
| Xóa sản phẩm/danh mục | ❌ | ❌ | ✅ |
| Cập nhật trạng thái mọi đơn, xem thống kê | ❌ | ✅ | ✅ |
| Quản lý tài khoản & phân quyền | ❌ | ❌ | ✅ |

## Kiểm thử
`mvn test` (JUnit + MockMvc, dùng H2 trong bộ nhớ). Bảng kiểm thử thủ công: `docs/BAO-CAO.md` mục 8.

## Đẩy lên GitHub
```bash
git init && git add . && git commit -m "feat: HomeCare"
git branch -M main
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin main
```

## Xử lý sự cố
- `mvn` không nhận ra → cài Maven, kiểm tra `mvn -v`, `java -version` (≥25).
- Cổng 8080 bận → trong PowerShell chạy `$env:PORT=8081; mvn spring-boot:run`, rồi mở http://localhost:8081.
- Lỗi khóa file DB → chỉ chạy một tiến trình ứng dụng trên cùng tệp SQLite; dừng ứng dụng trước khi sao chép tệp dữ liệu.
