# Auth API Spring Boot

Backend Java 17 Spring Boot untuk register dan login dengan `POST` dan `Content-Type: application/x-www-form-urlencoded`.

Request body harus berisi field form `payload`. Nilai `payload` adalah hasil enkripsi CryptoJS dari JSON:

```json
{"email":"user@example.com","password":"password123"}
```

## Konfigurasi

Set secret lewat environment variable sebelum menjalankan aplikasi:

```powershell
$env:AUTH_CRYPTO_SECRET="secret-yang-sama-dengan-client"
$env:AUTH_TOKEN_SECRET="secret-token-minimal-32-karakter"
```

Nilai `AUTH_CRYPTO_SECRET` harus sama dengan passphrase yang dipakai di CryptoJS.

## Jalankan

```powershell
mvn spring-boot:run
```

API berjalan di `http://localhost:8080`.

## Endpoint

### Register

`POST /api/auth/register`

Headers:

```http
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
  "message": "Registration successful"
}
```

### Login

`POST /api/auth/login`

Headers:

```http
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

## Contoh Client CryptoJS

Install:

```powershell
npm install crypto-js
```

Contoh JavaScript:

```javascript
import CryptoJS from "crypto-js";

const API_URL = "http://localhost:8080/api/auth/register";
const CRYPTO_SECRET = "secret-yang-sama-dengan-client";

const credentials = {
  email: "user@example.com",
  password: "password123"
};

const payload = CryptoJS.AES
  .encrypt(JSON.stringify(credentials), CRYPTO_SECRET)
  .toString();

const body = new URLSearchParams();
body.set("payload", payload);

const response = await fetch(API_URL, {
  method: "POST",
  headers: {
    "Content-Type": "application/x-www-form-urlencoded"
  },
  body
});

console.log(await response.json());
```

Pakai `URLSearchParams` agar karakter Base64 seperti `+`, `/`, dan `=` dikirim dengan encoding form yang benar.
