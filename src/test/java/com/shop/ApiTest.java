package com.shop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class ApiTest {
  @Autowired MockMvc mvc;
  static final MediaType J = MediaType.APPLICATION_JSON;

  MockHttpSession login(String u, String p) throws Exception {
    var s = new MockHttpSession();
    mvc.perform(post("/api/login").session(s).contentType(J).content("{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}")).andExpect(status().isOk());
    return s;
  }
  MockHttpSession staff() throws Exception { return login("nhanvien", "nv123456"); }
  MockHttpSession customer() throws Exception { return login("khach", "khach123"); }
  MockHttpSession admin() throws Exception { return login("admin", "admin123"); }
  static String order(int qty) {
    return "{\"name\":\"A\",\"phone\":\"0912345678\",\"address\":\"Thai Nguyen\",\"payment\":\"COD\",\"items\":[{\"id\":1,\"qty\":" + qty + "}]}";
  }

  // ----- tài khoản & phân quyền -----
  @Test void wrongPasswordRejected() throws Exception {
    mvc.perform(post("/api/login").contentType(J).content("{\"username\":\"admin\",\"password\":\"sai\"}")).andExpect(status().isUnauthorized());
  }
  @Test void registerValidation() throws Exception {
    mvc.perform(post("/api/register").contentType(J).content("{\"username\":\"abc\",\"password\":\"123456\",\"fullName\":\"A\"}")).andExpect(status().isBadRequest());
    mvc.perform(post("/api/register").contentType(J).content("{\"username\":\"newuser1\",\"password\":\"123\",\"fullName\":\"A\"}")).andExpect(status().isBadRequest());
    mvc.perform(post("/api/register").contentType(J).content("{\"username\":\"khach\",\"password\":\"123456\",\"fullName\":\"A\"}")).andExpect(status().isConflict());
  }
  @Test void guestCannotListUsers() throws Exception { mvc.perform(get("/api/users")).andExpect(status().isUnauthorized()); }
  @Test void customerAndStaffCannotListUsers() throws Exception {
    mvc.perform(get("/api/users").session(customer())).andExpect(status().isForbidden());
    mvc.perform(get("/api/users").session(staff())).andExpect(status().isForbidden());
  }
  @Test void adminCanListUsers() throws Exception { mvc.perform(get("/api/users").session(admin())).andExpect(status().isOk()); }
  @Test void lockedUserCannotLogin() throws Exception {
    String body = mvc.perform(post("/api/register").contentType(J).content("{\"username\":\"locktest\",\"password\":\"123456\",\"fullName\":\"L\"}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    int id = JsonPath.read(body, "$.id");
    mvc.perform(put("/api/users/" + id + "/lock").session(admin())).andExpect(status().isOk());
    mvc.perform(post("/api/login").contentType(J).content("{\"username\":\"locktest\",\"password\":\"123456\"}")).andExpect(status().isForbidden());
  }

  // ----- danh mục & sản phẩm -----
  @Test void categoriesAndSearchArePublic() throws Exception {
    mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").exists());
    mvc.perform(get("/api/products").param("q", "nồi")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").exists());
    mvc.perform(get("/api/products").param("category", "1")).andExpect(status().isOk());
  }
  @Test void productDescriptionCanBeSavedEditedAndSearched() throws Exception {
    String body = mvc.perform(post("/api/products").session(staff()).contentType(J)
        .content("{\"name\":\"Bình giữ nhiệt\",\"description\":\"Giữ ấm nước cả ngày\",\"categoryId\":1,\"price\":1000,\"stock\":3}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Giữ ấm nước cả ngày"))
        .andReturn().getResponse().getContentAsString();
    int id = JsonPath.read(body, "$.id");
    mvc.perform(get("/api/products").param("q", "giữ ấm"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
    mvc.perform(put("/api/products/" + id).session(staff()).contentType(J)
        .content("{\"name\":\"Bình giữ nhiệt\",\"description\":\"Dung tích 1 lít\",\"categoryId\":1,\"price\":1000,\"stock\":3}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Dung tích 1 lít"));
  }
  @Test void productDescriptionLengthIsValidated() throws Exception {
    String description = "x".repeat(1001);
    mvc.perform(post("/api/products").session(staff()).contentType(J)
        .content("{\"name\":\"Bình\",\"description\":\"" + description + "\",\"categoryId\":1,\"price\":1000,\"stock\":3}"))
        .andExpect(status().isBadRequest());
  }
  @Test void customerCannotAddProductOrCategory() throws Exception {
    mvc.perform(post("/api/products").session(customer()).contentType(J).content("{\"name\":\"X\",\"categoryId\":1,\"price\":1,\"stock\":1}")).andExpect(status().isForbidden());
    mvc.perform(post("/api/categories").session(customer()).contentType(J).content("{\"name\":\"X\"}")).andExpect(status().isForbidden());
  }
  @Test void invalidProductRejected() throws Exception {
    mvc.perform(post("/api/products").session(staff()).contentType(J).content("{\"name\":\"X\",\"categoryId\":1,\"price\":-5,\"stock\":1}")).andExpect(status().isBadRequest());
    mvc.perform(post("/api/products").session(staff()).contentType(J).content("{\"name\":\"\",\"categoryId\":1,\"price\":5,\"stock\":1}")).andExpect(status().isBadRequest());
    mvc.perform(post("/api/products").session(staff()).contentType(J).content("{\"name\":\"X\",\"categoryId\":9999,\"price\":5,\"stock\":1}")).andExpect(status().isBadRequest());
  }
  @Test void staffCanCrudProductButOnlyAdminDeletes() throws Exception {
    String body = mvc.perform(post("/api/products").session(staff()).contentType(J).content("{\"name\":\"Mon thu\",\"categoryId\":1,\"price\":1000,\"stock\":3}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    int id = JsonPath.read(body, "$.id");
    mvc.perform(put("/api/products/" + id).session(staff()).contentType(J).content("{\"name\":\"Mon thu 2\",\"categoryId\":1,\"price\":2000,\"stock\":3}")).andExpect(status().isOk());
    mvc.perform(delete("/api/products/" + id).session(staff())).andExpect(status().isForbidden());
    mvc.perform(delete("/api/products/" + id).session(admin())).andExpect(status().isOk());
  }
  @Test void categoryWithProductsCannotBeDeleted() throws Exception {
    mvc.perform(delete("/api/categories/1").session(admin())).andExpect(status().isConflict());
  }
  @Test void uploadRejectsNonImage() throws Exception {
    mvc.perform(multipart("/api/products/1/image").file(new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes())).session(staff())).andExpect(status().isBadRequest());
  }

  // ----- giỏ hàng / đặt hàng -----
  @Test void emptyCartRejected() throws Exception {
    mvc.perform(post("/api/orders").session(customer()).contentType(J)
        .content("{\"name\":\"A\",\"phone\":\"0912345678\",\"address\":\"Thai Nguyen\",\"payment\":\"COD\",\"items\":[]}")).andExpect(status().isBadRequest());
  }
  @Test void invalidPhoneAndOverStockRejected() throws Exception {
    mvc.perform(post("/api/orders").session(customer()).contentType(J).content(order(1).replace("0912345678", "123"))).andExpect(status().isBadRequest());
    mvc.perform(post("/api/orders").session(customer()).contentType(J).content(order(99999))).andExpect(status().isBadRequest());
  }
  @Test void orderFlowAndStatusUpdate() throws Exception {
    var c = customer();
    String body = mvc.perform(post("/api/orders").session(c).contentType(J).content(order(2)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")).andReturn().getResponse().getContentAsString();
    int id = JsonPath.read(body, "$.id");
    mvc.perform(put("/api/orders/" + id + "/status").session(c).contentType(J).content("{\"status\":\"SHIPPING\"}")).andExpect(status().isForbidden());
    mvc.perform(put("/api/orders/" + id + "/status").session(staff()).contentType(J).content("{\"status\":\"SHIPPING\"}")).andExpect(status().isOk());
    mvc.perform(put("/api/orders/" + id + "/status").session(staff()).contentType(J).content("{\"status\":\"DELIVERED\"}")).andExpect(status().isOk());
    mvc.perform(put("/api/orders/" + id + "/status").session(staff()).contentType(J).content("{\"status\":\"CANCELLED\"}")).andExpect(status().isBadRequest());
  }
  @Test void customerCancelRestoresStock() throws Exception {
    var c = customer();
    String body = mvc.perform(post("/api/orders").session(c).contentType(J).content(order(1))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    int id = JsonPath.read(body, "$.id");
    int before = stock1();
    mvc.perform(put("/api/orders/" + id + "/status").session(c).contentType(J).content("{\"status\":\"CANCELLED\"}")).andExpect(status().isOk());
    int after = stock1();
    org.junit.jupiter.api.Assertions.assertEquals(before + 1, after);
  }
  int stock1() throws Exception {
    java.util.List<Integer> l = JsonPath.read(mvc.perform(get("/api/products")).andReturn().getResponse().getContentAsString(), "$[?(@.id==1)].stock");
    return l.get(0);
  }
  @Test void statsOnlyForStaff() throws Exception {
    mvc.perform(get("/api/stats").session(customer())).andExpect(status().isForbidden());
    mvc.perform(get("/api/stats").session(staff())).andExpect(status().isOk()).andExpect(jsonPath("$.products").exists());
  }
}
