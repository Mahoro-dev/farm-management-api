# Farm Management API

Spring Boot REST API for one organization and many cattle farms. Every monetary amount is Rwandan francs (RWF). There is no currency field and no conversion.

## Run

Use a local MySQL 8 server. Connection settings are in `src/main/resources/application.properties` (`localhost:3306/farm_management`, user `root`, password `root`). Flyway creates the schema in that database on startup.

Run the API (Java 21):

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Interactive docs are at `http://localhost:8080/swagger-ui.html`. The JWT secret in `application.properties` must be at least 32 characters.

## First request

`POST /api/auth/bootstrap` once:

```json
{
  "organizationName": "Green Valley Farms",
  "timezone": "Africa/Kigali",
  "admin": {
    "fullName": "Aline Uwase",
    "email": "aline@example.com",
    "password": "change-me-now"
  },
  "farm": {
    "name": "Kigali Dairy",
    "farmCode": "KGL-01",
    "district": "Gasabo",
    "province": "Kigali"
  }
}
```

Then `POST /api/auth/login` and send `Authorization: Bearer <token>`.

An active `ORG_ADMIN` assignment grants access to every farm. Other roles apply only to the farms they are assigned to.

## What it covers

Animals and ear tags, farm transfers, milk shifts, veterinary visits, vaccinations, treatments, medicine stock, breeding with an ancestor check, pregnancy, calving, weights, events, and reports. Stock movements cannot be edited or deleted; post a correcting movement instead. Medicine `unitCost` is RWF per base unit.
