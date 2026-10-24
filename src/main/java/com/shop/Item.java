package com.shop;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class Item {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  public Long productId;
  public String productName;
  public long price;
  public int qty;
}
