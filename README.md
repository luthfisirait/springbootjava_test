# Dokumentasi Penggunaan Auth API

Backend ini menyediakan API `register` dan `login` menggunakan Java 17 Spring Boot.
Kedua endpoint memakai method `POST` dengan format request `application/x-www-form-urlencoded`.

Data email dan password tidak dikirim langsung sebagai form field terpisah. Client harus mengenkripsi JSON credentials memakai CryptoJS, lalu mengirim hasil enkripsi tersebut ke field form bernama `payload`.

## Daftar Isi

- [Prasyarat](#prasyarat)
- [Konfigurasi](#konfigurasi)
- [Menjalankan Aplikasi](#menjalankan-aplikasi)
- [API Cara Penggunaan](#api-cara-penggunaan)
- [Format Payload Terenkripsi](#format-payload-terenkripsi)
- [Endpoint Register](#endpoint-register)
- [Endpoint Login](#endpoint-login)
- [Contoh Client JavaScript](#contoh-client-javascript)
- [Contoh Postman](#contoh-postman)
- [Response Error](#response-error)
- [Database](#database)
- [Troubleshooting](#troubleshooting)

## Prasyarat

Pastikan sudah tersedia:

- Java JDK 17 atau lebih baru
- Maven 3.8 atau lebih baru
- Node.js hanya dibutuhkan jika ingin mencoba contoh enkripsi CryptoJS dari JavaScript

Cek instalasi:

```powershell
java -version
mvn -version
```

## Konfigurasi

Konfigurasi utama ada di `src/main/resources/application.properties`.

Default port:

```properties
server.port=8080
```

Secret untuk enkripsi request:

```properties
auth.crypto-secret=${AUTH_CRYPTO_SECRET:change-this-crypto-secret}
```

Secret untuk token login:

```properties
auth.token-secret=${AUTH_TOKEN_SECRET:change-this-token-secret}
```

Durasi token dalam detik:

```properties
auth.token-ttl-seconds=${AUTH_TOKEN_TTL_SECONDS:3600}
```

Sebelum menjalankan aplikasi, disarankan set environment variable:

```powershell
$env:AUTH_CRYPTO_SECRET="secret-yang-sama-dengan-client"
$env:AUTH_TOKEN_SECRET="secret-token-minimal-32-karakter"
$env:AUTH_TOKEN_TTL_SECONDS="3600"
```

Nilai `AUTH_CRYPTO_SECRET` wajib sama dengan passphrase yang dipakai client saat menjalankan:

```javascript
CryptoJS.AES.encrypt(JSON.stringify(credentials), CRYPTO_SECRET).toString()
```

## Menjalankan Aplikasi

Dari folder project:

```powershell
mvn spring-boot:run
```

Jika berhasil, API tersedia di:

```text
http://localhost:8080
```

## API Cara Penggunaan

Backend menyediakan endpoint dokumentasi penggunaan:

```http
GET /api/usage
```

Contoh akses:

```powershell
curl.exe "http://localhost:8080/api/usage"
```

Endpoint ini mengembalikan:

- `contentType` yang harus dipakai
- contoh credentials testing
- daftar endpoint register dan login
- `textBodyForTesting` yang bisa langsung dipakai sebagai body `application/x-www-form-urlencoded`
- `encryptedPayloadOnly` jika hanya membutuhkan nilai payload
- contoh kode CryptoJS

Contoh potongan response:

```json
{
  "success": true,
  "contentType": "application/x-www-form-urlencoded",
  "sampleCredentials": {
    "email": "user@example.com",
    "password": "password123",
    "jsonBeforeEncryption": "{\"email\":\"user@example.com\",\"password\":\"password123\"}"
  },
  "endpoints": [
    {
      "name": "register",
      "method": "POST",
      "path": "/api/auth/register",
      "header": "Content-Type: application/x-www-form-urlencoded",
      "textBodyForTesting": "payload=U2FsdGVkX1...",
      "encryptedPayloadOnly": "U2FsdGVkX1..."
    },
    {
      "name": "login",
      "method": "POST",
      "path": "/api/auth/login",
      "header": "Content-Type: application/x-www-form-urlencoded",
      "textBodyForTesting": "payload=U2FsdGVkX1...",
      "encryptedPayloadOnly": "U2FsdGVkX1..."
    }
  ]
}
```

Cara testing dari response `/api/usage`:

1. Copy `textBodyForTesting` dari endpoint `register`.
2. Kirim ke `POST http://localhost:8080/api/auth/register`.
3. Copy `textBodyForTesting` dari endpoint `login`.
4. Kirim ke `POST http://localhost:8080/api/auth/login`.

Contoh cURL dengan body dari `/api/usage`:

```powershell
curl.exe -X POST "http://localhost:8080/api/auth/register" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "payload=U2FsdGVkX1..."
```

## Format Payload Terenkripsi

JSON asli sebelum dienkripsi:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

JSON tersebut harus dienkripsi menjadi string CryptoJS AES:

```javascript
const payload = CryptoJS.AES
  .encrypt(JSON.stringify({
    email: "user@example.com",
    password: "password123"
  }), "secret-yang-sama-dengan-client")
  .toString();
```

Request body yang dikirim ke API:

```text
payload=<hasil-enkripsi-cryptojs>
```

Gunakan `URLSearchParams` atau form encoder lain agar karakter Base64 seperti `+`, `/`, dan `=` dikirim dengan benar.

## Endpoint Register

Mendaftarkan user baru.

```http
POST /api/auth/register
Content-Type: application/x-www-form-urlencoded
```

Form body:

```text
payload=<encrypted string dari CryptoJS>
```

Validasi:

- `email` wajib format email valid
- `password` minimal 8 karakter
- email tidak boleh sudah terdaftar

Response sukses:

```json
{
  "success": true,
  "userId": 1,
  "email": "user@example.com",
  "message": "Registration successful"
}
```

Contoh response ketika email sudah terdaftar:

```json
{
  "success": false,
  "code": "EMAIL_ALREADY_REGISTERED",
  "message": "Email already registered",
  "status": 409,
  "path": "/api/auth/register",
  "timestamp": "2026-05-05T06:00:00Z"
}
```

## Endpoint Login

Login memakai email dan password.

```http
POST /api/auth/login
Content-Type: application/x-www-form-urlencoded
```

Form body:

```text
payload=<encrypted string dari CryptoJS>
```

Response sukses:

```json
{
  "success": true,
  "userId": 1,
  "email": "user@example.com",
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Token dapat dipakai oleh endpoint lain sebagai header:

```http
Authorization: Bearer <jwt>
```

Catatan: project ini baru menyediakan pembuatan token saat login. Jika nanti ditambahkan endpoint lain yang butuh proteksi, tambahkan validasi token di filter/security layer.

## Contoh Client JavaScript

Install dependency:

```powershell
npm install crypto-js
```

Contoh fungsi helper:

```javascript
import CryptoJS from "crypto-js";

const API_BASE_URL = "http://localhost:8080";
const CRYPTO_SECRET = "secret-yang-sama-dengan-client";

function encryptedFormBody(email, password) {
  const credentials = { email, password };
  const payload = CryptoJS.AES
    .encrypt(JSON.stringify(credentials), CRYPTO_SECRET)
    .toString();

  const body = new URLSearchParams();
  body.set("payload", payload);
  return body;
}

async function register(email, password) {
  const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: encryptedFormBody(email, password)
  });

  return response.json();
}

async function login(email, password) {
  const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body: encryptedFormBody(email, password)
  });

  return response.json();
}

console.log(await register("user@example.com", "password123"));
console.log(await login("user@example.com", "password123"));
```

## Contoh cURL

cURL membutuhkan nilai `payload` yang sudah terenkripsi. Buat payload dengan Node.js:

```powershell
npm install crypto-js
node -e "const CryptoJS=require('crypto-js'); const payload=CryptoJS.AES.encrypt(JSON.stringify({email:'user@example.com',password:'password123'}),'secret-yang-sama-dengan-client').toString(); console.log(payload)"
```

Kirim register:

```powershell
curl.exe -X POST "http://localhost:8080/api/auth/register" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data-urlencode "payload=<PASTE_PAYLOAD_HASIL_ENKRIPSI>"
```

Kirim login:

```powershell
curl.exe -X POST "http://localhost:8080/api/auth/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  --data-urlencode "payload=<PASTE_PAYLOAD_HASIL_ENKRIPSI>"
```

## Contoh Postman

1. Pilih method `POST`.
2. Masukkan URL `http://localhost:8080/api/auth/register` atau `http://localhost:8080/api/auth/login`.
3. Buka tab `Body`.
4. Pilih `x-www-form-urlencoded`.
5. Tambahkan key `payload`.
6. Isi value dengan hasil `CryptoJS.AES.encrypt(...).toString()`.
7. Kirim request.

Header `Content-Type: application/x-www-form-urlencoded` biasanya otomatis dibuat oleh Postman saat memilih body `x-www-form-urlencoded`.

## Response Error

Format error umum:

```json
{
  "success": false,
  "code": "INVALID_CREDENTIALS",
  "message": "Email or password is incorrect",
  "status": 401,
  "path": "/api/auth/login",
  "timestamp": "2026-05-05T06:00:00Z"
}
```

Kode error yang mungkin muncul:

| HTTP | Code | Penyebab |
| --- | --- | --- |
| 400 | `MISSING_PAYLOAD` | Field form `payload` kosong |
| 400 | `MISSING_PARAMETER` | Field form `payload` tidak dikirim |
| 400 | `INVALID_ENCRYPTED_PAYLOAD` | Payload tidak bisa didekripsi |
| 400 | `UNSUPPORTED_PAYLOAD_FORMAT` | Payload bukan format CryptoJS passphrase AES |
| 400 | `INVALID_PAYLOAD` | Hasil dekripsi bukan JSON valid |
| 400 | `INVALID_EMAIL` | Format email tidak valid |
| 400 | `INVALID_PASSWORD` | Password kurang dari 8 karakter |
| 401 | `INVALID_CREDENTIALS` | Email atau password salah |
| 409 | `EMAIL_ALREADY_REGISTERED` | Email sudah terdaftar |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Content-Type bukan `application/x-www-form-urlencoded` |

## Database

Project memakai H2 file database:

```properties
spring.datasource.url=jdbc:h2:file:./data/authdb;DB_CLOSE_ON_EXIT=FALSE
```

File database akan dibuat di folder:

```text
data/
```

H2 console aktif di:

```text
http://localhost:8080/h2-console
```

Konfigurasi login H2 console:

```text
JDBC URL: jdbc:h2:file:./data/authdb
User Name: sa
Password:
```

## Troubleshooting

### `java` tidak dikenali

Install JDK 17 atau lebih baru, lalu pastikan `java` ada di PATH.

```powershell
java -version
```

### `mvn` tidak dikenali

Install Maven, lalu pastikan `mvn` ada di PATH.

```powershell
mvn -version
```

### Payload tidak bisa didekripsi

Pastikan:

- Client memakai `CryptoJS.AES.encrypt(JSON.stringify(credentials), CRYPTO_SECRET).toString()`
- Nilai `CRYPTO_SECRET` di client sama dengan `AUTH_CRYPTO_SECRET` di backend
- Request dikirim sebagai `application/x-www-form-urlencoded`
- Field form bernama `payload`
- Pengiriman memakai `URLSearchParams` atau `--data-urlencode`

### Login selalu gagal

Pastikan user sudah register dan password yang dipakai sama. Password disimpan dalam bentuk BCrypt hash, jadi password asli tidak bisa dilihat dari database.

## Struktur Project

```text
src/main/java/com/example/authapi
  auth/
    AuthController.java
    AuthService.java
    AuthCredentials.java
    LoginResponse.java
    RegisterResponse.java
  config/
    AppConfig.java
    AuthProperties.java
  crypto/
    CryptoJsAesDecryptor.java
  error/
    ApiException.java
    GlobalExceptionHandler.java
  token/
    TokenService.java
  usage/
    UsageController.java
  user/
    UserAccount.java
    UserAccountRepository.java
```
