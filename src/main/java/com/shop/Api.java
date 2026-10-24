package com.shop;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
public class Api {
  final Repos.Users users; final Repos.Products products; final Repos.Orders orders; final Repos.Categories cats;
  final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
  static final List<String> ROLES = List.of("ADMIN", "STAFF", "CUSTOMER");
  static final List<String> STATUS = List.of("PENDING", "SHIPPING", "DELIVERED", "CANCELLED");
  static final List<String> PAY = List.of("COD", "BANK", "MOMO");

  Api(Repos.Users u, Repos.Products p, Repos.Orders o, Repos.Categories c) { users = u; products = p; orders = o; cats = c; }

  // ---------- helpers ----------
  static ResponseStatusException err(int c, String m) { return new ResponseStatusException(HttpStatus.valueOf(c), m); }
  static String str(Map<String, Object> b, String k) { Object v = b.get(k); return v == null ? "" : v.toString().trim(); }
  static long num(Map<String, Object> b, String k) {
    try { return Long.parseLong(str(b, k)); } catch (Exception e) { throw err(400, "Giá trị '" + k + "' không hợp lệ"); }
  }
  User me(HttpSession s) {
    Object id = s.getAttribute("uid");
    if (id == null) throw err(401, "Vui lòng đăng nhập");
    User u = users.findById((Long) id).orElseThrow(() -> err(401, "Phiên đăng nhập hết hạn"));
    if (u.locked) { s.invalidate(); throw err(403, "Tài khoản đã bị khóa"); }
    return u;
  }
  User need(HttpSession s, String... roles) {
    User u = me(s);
    if (!List.of(roles).contains(u.role)) throw err(403, "Bạn không có quyền thực hiện thao tác này");
    return u;
  }
  void checkPw(String pw) { if (pw.length() < 6) throw err(400, "Mật khẩu tối thiểu 6 ký tự"); }

  // ---------- auth ----------
  @PostMapping("/register")
  public User register(@RequestBody Map<String, Object> b, HttpSession s) {
    String un = str(b, "username"), pw = str(b, "password"), fn = str(b, "fullName");
    if (!un.matches("[A-Za-z0-9_]{4,20}")) throw err(400, "Tên đăng nhập 4-20 ký tự (chữ, số, _)");
    checkPw(pw);
    if (fn.isEmpty()) throw err(400, "Vui lòng nhập họ tên");
    if (users.findByUsername(un).isPresent()) throw err(409, "Tên đăng nhập đã tồn tại");
    User u = new User(); u.username = un; u.password = enc.encode(pw); u.fullName = fn;
    users.save(u); s.setAttribute("uid", u.id); return u;
  }
  @PostMapping("/login")
  public User login(@RequestBody Map<String, Object> b, HttpSession s) {
    User u = users.findByUsername(str(b, "username")).orElse(null);
    if (u == null || !enc.matches(str(b, "password"), u.password)) throw err(401, "Sai tên đăng nhập hoặc mật khẩu");
    if (u.locked) throw err(403, "Tài khoản đã bị khóa");
    s.setAttribute("uid", u.id); return u;
  }
  @PostMapping("/logout") public Map<String, Object> logout(HttpSession s) { s.invalidate(); return Map.of("ok", true); }
  @GetMapping("/me") public User current(HttpSession s) { return me(s); }

  // ---------- quản lý tài khoản (ADMIN) ----------
  @GetMapping("/users") public List<User> listUsers(HttpSession s) { need(s, "ADMIN"); return users.findAll(); }

  @PostMapping("/users")
  public User addUser(@RequestBody Map<String, Object> b, HttpSession s) {
    need(s, "ADMIN");
    String un = str(b, "username"), role = str(b, "role");
    if (!un.matches("[A-Za-z0-9_]{4,20}")) throw err(400, "Tên đăng nhập 4-20 ký tự (chữ, số, _)");
    checkPw(str(b, "password"));
    if (!ROLES.contains(role)) throw err(400, "Vai trò không hợp lệ");
    if (users.findByUsername(un).isPresent()) throw err(409, "Tên đăng nhập đã tồn tại");
    User u = new User(); u.username = un; u.password = enc.encode(str(b, "password"));
    u.fullName = str(b, "fullName"); u.role = role; return users.save(u);
  }
  @PutMapping("/users/{id}")
  public User editUser(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpSession s) {
    User admin = need(s, "ADMIN");
    User u = users.findById(id).orElseThrow(() -> err(404, "Không tìm thấy người dùng"));
    if (!str(b, "fullName").isEmpty()) u.fullName = str(b, "fullName");
    String role = str(b, "role");
    if (!role.isEmpty()) {
      if (!ROLES.contains(role)) throw err(400, "Vai trò không hợp lệ");
      if (u.id.equals(admin.id) && !role.equals("ADMIN")) throw err(400, "Không thể tự hạ quyền của mình");
      u.role = role;
    }
    if (!str(b, "password").isEmpty()) { checkPw(str(b, "password")); u.password = enc.encode(str(b, "password")); }
    return users.save(u);
  }
  @PutMapping("/users/{id}/lock")
  public User lock(@PathVariable Long id, HttpSession s) {
    User admin = need(s, "ADMIN");
    User u = users.findById(id).orElseThrow(() -> err(404, "Không tìm thấy người dùng"));
    if (u.id.equals(admin.id)) throw err(400, "Không thể khóa chính mình");
    u.locked = !u.locked; return users.save(u);
  }

  // ---------- danh mục ----------
  @GetMapping("/categories") public List<Category> categories() { return cats.findAll(Sort.by("name")); }

  Category catFill(Category c, Map<String, Object> b) {
    String n = str(b, "name");
    if (n.isEmpty() || n.length() > 50) throw err(400, "Tên danh mục phải từ 1-50 ký tự");
    var ex = cats.findByNameIgnoreCase(n);
    if (ex.isPresent() && !ex.get().id.equals(c.id)) throw err(409, "Danh mục đã tồn tại");
    c.name = n; c.description = str(b, "description"); return cats.save(c);
  }
  @PostMapping("/categories") public Category addCat(@RequestBody Map<String, Object> b, HttpSession s) {
    need(s, "ADMIN", "STAFF"); return catFill(new Category(), b);
  }
  @PutMapping("/categories/{id}") public Category editCat(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpSession s) {
    need(s, "ADMIN", "STAFF");
    return catFill(cats.findById(id).orElseThrow(() -> err(404, "Không tìm thấy danh mục")), b);
  }
  @DeleteMapping("/categories/{id}") public Map<String, Object> delCat(@PathVariable Long id, HttpSession s) {
    need(s, "ADMIN");
    if (products.countByCategoryId(id) > 0) throw err(409, "Danh mục còn sản phẩm, không thể xóa");
    cats.deleteById(id); return Map.of("ok", true);
  }

  // ---------- sản phẩm ----------
  @GetMapping("/products")
  public List<Product> listProducts(@RequestParam(required = false) Long category, @RequestParam(required = false) String q) {
    return products.search(category == null ? 0L : category, q == null ? "" : q.trim().toLowerCase());
  }
  Product fill(Product p, Map<String, Object> b) {
    String n = str(b, "name");
    if (n.isEmpty() || n.length() > 150) throw err(400, "Tên sản phẩm phải từ 1-150 ký tự");
    String description = str(b, "description");
    if (description.length() > 1000) throw err(400, "Mô tả sản phẩm tối đa 1000 ký tự");
    Category c = cats.findById(num(b, "categoryId")).orElseThrow(() -> err(400, "Danh mục không tồn tại"));
    long price = num(b, "price"), stock = num(b, "stock");
    if (price <= 0 || price > 1_000_000_000L) throw err(400, "Giá phải lớn hơn 0 và hợp lệ");
    if (stock < 0 || stock > 100000) throw err(400, "Tồn kho phải từ 0 đến 100000");
    p.name = n; p.description = description; p.category = c; p.price = price; p.stock = (int) stock;
    String img = str(b, "image");
    if (!img.isEmpty() && !img.startsWith("/")) p.image = img; else if (p.image == null) p.image = "🏠";
    return products.save(p);
  }
  void rmImg(Product p) {
    try { if (p.image != null && p.image.startsWith("/uploads/")) Files.deleteIfExists(Path.of("uploads", Path.of(p.image).getFileName().toString())); }
    catch (IOException e) { /* bỏ qua */ }
  }
  @PostMapping("/products") public Product addProduct(@RequestBody Map<String, Object> b, HttpSession s) {
    need(s, "ADMIN", "STAFF"); return fill(new Product(), b);
  }
  @PutMapping("/products/{id}") public Product editProduct(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpSession s) {
    need(s, "ADMIN", "STAFF");
    return fill(products.findById(id).orElseThrow(() -> err(404, "Không tìm thấy sản phẩm")), b);
  }
  @PostMapping("/products/{id}/image") public Product upload(@PathVariable Long id, @RequestParam("file") MultipartFile f, HttpSession s) throws IOException {
    need(s, "ADMIN", "STAFF");
    Product p = products.findById(id).orElseThrow(() -> err(404, "Không tìm thấy sản phẩm"));
    String ct = f.getContentType();
    String ext = "image/jpeg".equals(ct) ? ".jpg" : "image/png".equals(ct) ? ".png" : "image/webp".equals(ct) ? ".webp" : null;
    if (ext == null || f.isEmpty()) throw err(400, "Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP");
    Files.createDirectories(Path.of("uploads"));
    String name = UUID.randomUUID() + ext; // tên ngẫu nhiên, tránh path traversal
    f.transferTo(Path.of("uploads", name).toAbsolutePath());
    rmImg(p); p.image = "/uploads/" + name; return products.save(p);
  }
  @DeleteMapping("/products/{id}") public Map<String, Object> delProduct(@PathVariable Long id, HttpSession s) {
    need(s, "ADMIN");
    products.findById(id).ifPresent(p -> { rmImg(p); products.delete(p); });
    return Map.of("ok", true);
  }
  @GetMapping("/stats") public Map<String, Object> stats(HttpSession s) {
    need(s, "ADMIN", "STAFF");
    var all = orders.findAll(); Map<String, Object> m = new LinkedHashMap<>();
    m.put("orders", all.size());
    m.put("pending", all.stream().filter(o -> o.status.equals("PENDING")).count());
    m.put("revenue", all.stream().filter(o -> !o.status.equals("CANCELLED")).mapToLong(o -> o.total).sum());
    m.put("products", products.count()); m.put("users", users.count()); return m;
  }

  // ---------- đơn hàng ----------
  @PostMapping("/orders") @Transactional
  @SuppressWarnings("unchecked")
  public Order placeOrder(@RequestBody Map<String, Object> b, HttpSession s) {
    User u = me(s);
    String name = str(b, "name"), phone = str(b, "phone"), addr = str(b, "address"), pay = str(b, "payment");
    if (name.isEmpty() || addr.length() < 5) throw err(400, "Vui lòng nhập họ tên và địa chỉ giao hàng");
    if (!phone.matches("0\\d{9}")) throw err(400, "Số điện thoại phải gồm 10 số, bắt đầu bằng 0");
    if (!PAY.contains(pay)) throw err(400, "Phương thức thanh toán không hợp lệ");
    List<Map<String, Object>> lines = (List<Map<String, Object>>) b.get("items");
    if (lines == null || lines.isEmpty()) throw err(400, "Giỏ hàng đang trống");
    Order o = new Order(); o.userId = u.id; o.customerName = name; o.phone = phone; o.address = addr; o.payment = pay;
    for (var l : lines) {
      Product p = products.findById(num(l, "id")).orElseThrow(() -> err(404, "Sản phẩm không tồn tại"));
      int qty = (int) num(l, "qty");
      if (qty < 1) throw err(400, "Số lượng không hợp lệ");
      if (qty > p.stock) throw err(400, "'" + p.name + "' chỉ còn " + p.stock + " sản phẩm");
      p.stock -= qty; products.save(p);
      Item i = new Item(); i.productId = p.id; i.productName = p.name; i.price = p.price; i.qty = qty;
      o.items.add(i); o.total += p.price * qty; // giá tính phía server
    }
    return orders.save(o);
  }
  @GetMapping("/orders") public List<Order> listOrders(HttpSession s) {
    User u = me(s);
    return u.role.equals("CUSTOMER") ? orders.findByUserIdOrderByIdDesc(u.id) : orders.findAllByOrderByIdDesc();
  }
  @PutMapping("/orders/{id}/status") @Transactional
  public Order setStatus(@PathVariable Long id, @RequestBody Map<String, Object> b, HttpSession s) {
    User u = me(s);
    Order o = orders.findById(id).orElseThrow(() -> err(404, "Không tìm thấy đơn hàng"));
    String st = str(b, "status");
    if (!STATUS.contains(st)) throw err(400, "Trạng thái không hợp lệ");
    boolean staff = !u.role.equals("CUSTOMER");
    if (!staff && !(o.userId.equals(u.id) && st.equals("CANCELLED") && o.status.equals("PENDING")))
      throw err(403, "Khách hàng chỉ được hủy đơn đang chờ xác nhận của mình");
    if (o.status.equals("CANCELLED") || o.status.equals("DELIVERED")) throw err(400, "Đơn hàng đã kết thúc, không thể đổi trạng thái");
    if (st.equals("CANCELLED"))
      for (Item i : o.items) products.findById(i.productId).ifPresent(p -> { p.stock += i.qty; products.save(p); });
    o.status = st; return orders.save(o);
  }
}
