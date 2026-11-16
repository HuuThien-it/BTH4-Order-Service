# BTH4 - Order Management Service (SOA)

## Muc tieu

- Quan ly don hang: tao, xem danh sach, xem chi tiet, sua don dang PENDING, cap nhat trang thai, xoa don cho phep.
- **Order Service doc lap tren port 8082, database BTH4_ORDER.**
- **Product Service (BTH3) tren port 8081, database BTH3_PRODUCT.**
- Khi tao/sua don, Order Service goi `GET http://localhost:8081/products/{id}` de lay ten, gia, so luong ton kho.
- Khong doc truc tiep database cua Product Service. Don co nhieu items; tinh tong tien tu gia do Product Service tra ve.
- Gop cac muc co trung `productId` truoc khi kiem tra ton kho.
- API luu snapshot ten/gia cua san pham tai thoi diem tao hoac sua don.

## Kien truc

```text
Postman / HTTP Client
  |
  | POST /orders
  v
Order Service :8082 --> GET /products/{id} --> Product Service :8081
     |                                           |
     v                                           v
 BTH4_ORDER (MySQL)                        BTH3_PRODUCT (MySQL)
```

## Cai dat va chay tren Windows (IntelliJ / STS Eclipse)

1. Can JDK **21**, MySQL 8.x, Maven (hoac Maven tich hop IDE).
2. Chay BTH3 Product Service tren cong **8081** truoc. Vao `http://localhost:8081/products` de kiem tra.
3. Trong MySQL Workbench chay file `database.sql` tao database **BTH4_ORDER**.
4. Mo thu muc **BTH4-Order-Service** nhu mot Maven project trong IDE.
5. Tao Run Configuration cho `Bth4OrderApplication` va set environment variables `DB_USER=root` va `DB_PASSWORD=<mat khau MySQL>`. Hoac mo PowerShell:

   ```powershell
   $env:DB_USER="root"
   $env:DB_PASSWORD="<mat khau MySQL>"
   mvn spring-boot:run
   ```

6. Xac nhan Order Service chay port **8082** va test `GET http://localhost:8082/orders`, luc dau tra ve `[]`.
7. Neu BTH3 hien chua co san pham (vi da xoa product #2), tao product moi bang `POST http://localhost:8081/products`:

   ```json
   { "name":"Laptop Dell Inspiron", "description":"Laptop test BTH4", "price":18500000, "quantity":10 }
   ```

   **Ghi lai ID thuc te** tu response; KHONG gia dinh la `2`.
8. Thay `productId` trong `api-tests.http` bang ID san pham vua tao. Test `POST /orders` va cac API con lai.

## REST API

| Method | Endpoint | Chuc nang | HTTP |
|--------|----------|-----------|------|
| POST | `/orders` | Tao don, kiem tra ton kho BTH3 | 201 |
| GET | `/orders` | Danh sach don | 200 |
| GET | `/orders/{id}` | Xem chi tiet | 200 / 404 |
| PUT | `/orders/{id}` | Sua don dang PENDING (kiem tra kho lai) | 200 / 409 |
| PATCH | `/orders/{id}/status` | Doi trang thai | 200 / 409 |
| DELETE | `/orders/{id}` | Xoa don PENDING/CANCELLED | 204 / 409 |

Cac endpoint tren cung co alias `/api/orders`.

### JSON tao don

```http
POST http://localhost:8082/orders
Content-Type: application/json
```

```json
{
  "customerName": "Nguyen Van A",
  "items": [
    { "productId": 2, "quantity": 2 }
  ]
}
```

`productId = 2` chi la vi du. Phai dung ID dang ton tai trong BTH3.

### JSON cap nhat trang thai

```http
PATCH http://localhost:8082/orders/1/status
Content-Type: application/json
```

```json
{ "status": "CONFIRMED" }
```

Trang thai cho phep:

```text
PENDING -> CONFIRMED -> COMPLETED
   |          |
   v          v
CANCELLED <- CANCELLED
```

- `PENDING` va `CANCELLED`: co the xoa don.
- `COMPLETED` va `CANCELLED`: trang thai ket thuc; khong duoc chuyen tiep.
- `422 Unprocessable Entity`: product khong ton tai hoac so luong vuot ton kho.
- `503 Service Unavailable`: khong ket noi duoc Product Service.
- `400`: request khong hop le; `404`: don khong ton tai; `409`: trang thai khong hop le.

## Kiem tra tren MySQL Workbench

```sql
SELECT * FROM BTH4_ORDER.orders;
SELECT * FROM BTH4_ORDER.order_items;
SELECT * FROM BTH3_PRODUCT.products;
```

Bang `orders` va `order_items` luu trong **BTH4_ORDER**, trong khi san pham van nam o **BTH3_PRODUCT**.

## Test tu dong (H2 + Mock ProductClient)

```powershell
mvn test
```

`OrderApiIntegrationTest` khong can MySQL va Product Service thuc te (mock HTTP adapter).

## Luu y nghiep vu & pham vi bai thuc hanh

- **Kiem tra ton kho khong dong nghia voi dat giu/tru ton.** Sau khi tao don, Product Service **khong bi tru quantity**. Muon xu ly ban hang that, can API dat giu/tru kho o BTH3 voi giao dich va chong tranh chap dong thoi.
- Neu BTH3 tat hoac khong ket noi duoc, khong the tao/sua don, nhung van xem don trong BTH4_ORDER.
- Gia/tieu de san pham duoc luu snapshot, khong truy van truc tiep BTH3 khi xem don da tao.
- Vi la bai thuc hanh local, chua co authentication/authorization; can them neu trien khai cong khai len Internet.
- Khong commit file co mat khau, token hay .env. Cau hinh Spring doc mat khau qua `DB_PASSWORD`.
- Neu Hibernate `ddl-auto=update`, co the tao schema tu entity; file `database.sql` cung duoc cung cap de tao bang ro rang.
