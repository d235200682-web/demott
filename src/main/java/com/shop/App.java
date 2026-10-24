package com.shop;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
public class App {
  public static void main(String[] args) { SpringApplication.run(App.class, args); }

  @Bean CommandLineRunner seed(Repos.Users users, Repos.Products products, Repos.Categories cats) {
    return a -> {
      var enc = new BCryptPasswordEncoder();
      if (users.count() == 0) {
        mk(users, enc, "admin", "admin123", "Quản trị viên", "ADMIN");
        mk(users, enc, "nhanvien", "nv123456", "Nhân viên bán hàng", "STAFF");
        mk(users, enc, "khach", "khach123", "Khách hàng mẫu", "CUSTOMER");
      }
      if (cats.count() == 0) {
        var bep = c(cats, "Nhà bếp", "Nồi, chảo, dụng cụ nấu ăn"); var dien = c(cats, "Điện gia dụng", "Thiết bị điện dùng trong nhà");
        var ngu = c(cats, "Phòng ngủ", "Chăn ga gối nệm"); var tam = c(cats, "Phòng tắm", "Khăn tắm, phụ kiện");
        var don = c(cats, "Dọn dẹp", "Dụng cụ vệ sinh nhà cửa");
        p(products, "Nồi cơm điện Sunhouse 1.8L", dien, "🍚", 890000, 25);
        p(products, "Bộ nồi inox 3 đáy 5 món", bep, "🍲", 1290000, 15);
        p(products, "Chảo chống dính 28cm", bep, "🍳", 350000, 40);
        p(products, "Máy xay sinh tố đa năng", dien, "🥤", 790000, 20);
        p(products, "Ấm siêu tốc 1.8L", dien, "☕", 320000, 35);
        p(products, "Quạt đứng điều khiển từ xa", dien, "🌀", 1250000, 12);
        p(products, "Bộ chăn ga gối cotton", ngu, "🛏️", 1150000, 18);
        p(products, "Gối ngủ memory foam", ngu, "😴", 390000, 30);
        p(products, "Bộ khăn tắm cotton 3 chiếc", tam, "🛁", 280000, 50);
        p(products, "Chổi lau nhà xoay 360 độ", don, "🧹", 250000, 45);
        p(products, "Máy hút bụi cầm tay", don, "🧼", 1690000, 10);
      }
    };
  }

  static void mk(Repos.Users r, BCryptPasswordEncoder e, String u, String pw, String n, String role) {
    var x = new User(); x.username = u; x.password = e.encode(pw); x.fullName = n; x.role = role; r.save(x);
  }
  static Category c(Repos.Categories r, String n, String d) { var x = new Category(); x.name = n; x.description = d; return r.save(x); }
  static void p(Repos.Products r, String n, Category c, String img, long price, int stock) {
    var x = new Product(); x.name = n; x.category = c; x.image = img; x.price = price; x.stock = stock; r.save(x);
  }
}
