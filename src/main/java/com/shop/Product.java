package com.shop;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products", indexes = @Index(columnList = "category_id"))
public class Product {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  public String name, image;
  @Column(length = 1000) public String description;
  public long price;
  public int stock;
  @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "category_id") public Category category;
}
