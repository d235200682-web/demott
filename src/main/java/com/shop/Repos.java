package com.shop;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface Repos {
  interface Users extends JpaRepository<User, Long> { Optional<User> findByUsername(String u); }

  interface Categories extends JpaRepository<Category, Long> { Optional<Category> findByNameIgnoreCase(String n); }

  interface Products extends JpaRepository<Product, Long> {
    // c = 0 nghĩa là "tất cả danh mục"; q đã được chuyển thành chữ thường ("" = không lọc)
    @Query("select p from Product p where (:c = 0L or p.category.id = :c) and (lower(p.name) like concat('%', :q, '%') or lower(coalesce(p.description, '')) like concat('%', :q, '%')) order by p.id desc")
    List<Product> search(@Param("c") long c, @Param("q") String q);
    long countByCategoryId(Long id);
  }

  interface Orders extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByIdDesc(Long uid);
    List<Order> findAllByOrderByIdDesc();
  }
}
