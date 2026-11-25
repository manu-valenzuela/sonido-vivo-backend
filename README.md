# 🎸 Sonido Vivo - Backend (microservicios Spring Boot)

Ocho microservicios **Spring Boot 4.1** (Java 17) para la tienda Sonido Vivo, cada uno con su propia base **MySQL**, autenticación con **JWT** y documentación **Swagger**. Lo consume el frontend React del repositorio [sonido-vivo-frontend](https://github.com/manu-valenzuela/sonido-vivo-frontend). Proyecto de **Desarrollo FullStack II** (Duoc UC, EA3).

---

## 📌 Estado del proyecto
1. **Servicios:** los 8 completos, con capas controller, service, repository, DTO, validación, manejo de errores y Swagger.
2. **Datos:** `db.sql` crea las bases, usuarios y tablas; `datos.sql` carga regiones, comunas, 10 categorías, 86 marcas, 340 productos, 3 artículos de blog y 3 mensajes de contacto.
3. **Integración con el front:** completa y probada de punta a punta (registro, login, compra, compra rechazada, paneles de trabajador y administrador, reportes).
4. **Despliegue en AWS:** configurado con el perfil `prod` (`application-prod.properties`), pendiente de ejecutar según la Guía 18. Ver [Despliegue](#-despliegue-en-aws-ec2).

---

## 🧩 Servicios
| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| servicio-autenticacion | 8081 | `sonido_autenticacion` | Credenciales y emisión del JWT |
| servicio-usuarios | 8082 | `sonido_usuarios` | Perfiles, registro, regiones y comunas |
| servicio-productos | 8083 | `sonido_productos` | Productos, categorías, marcas y stock |
| servicio-ventas | 8084 | `sonido_ventas` | Carrito, ventas y compras rechazadas |
| servicio-boletas | 8085 | `sonido_boletas` | Emisión y anulación de boletas |
| servicio-blog | 8086 | `sonido_blog` | Artículos del blog |
| servicio-contacto | 8087 | `sonido_contacto` | Mensajes del formulario de contacto |
| servicio-reportes | 8088 | `sonido_reportes` | Reportes de ventas, inventario y usuarios |

Cada servicio tiene su propio `pom.xml`, base y usuario MySQL. Todos validan el mismo `JWT_SECRET`, pero solo autenticación lo emite.

**Comunicación entre servicios** (por HTTP, reenviando el JWT del usuario):
- **usuarios → autenticación:** sincroniza credenciales y rol al registrar o editar un usuario (el vínculo es el `email`).
- **ventas → usuarios, productos y boletas:** datos del comprador, descuento o reposición de stock y emisión de la boleta.
- **reportes → usuarios, productos y ventas:** consolida los indicadores.

---

## 🔐 Roles
`CLIENTE`, `TRABAJADOR` y `ADMINISTRADOR`. Las reglas están en cada `SecurityConfig` y en `@PreAuthorize`; el catálogo, el blog, el registro y el envío de contacto son públicos. Al iniciar, autenticación y usuarios crean la cuenta `admin@duoc.cl` / `Admin1234!`, que debe cambiarse después del despliegue.

---

## 🚀 Ejecución local
Requisitos: **Java 17+** y **MySQL 8**.

1. Crear bases y cargar datos, en este orden (ambos scripts usan `SET NAMES utf8mb4` para conservar los acentos):

```bash
mysql -u root -p < db.sql
```

```bash
mysql -u root -p < datos.sql
```

2. Iniciar cada servicio en su propia terminal. Autenticación debe partir antes que usuarios. En Windows:

```bash
cd servicio-autenticacion && mvnw.cmd spring-boot:run
```

En desarrollo, `application.properties` trae valores por defecto para las bases, el `JWT_SECRET` y el origen CORS `http://localhost:5173`, así que no se necesitan variables de entorno.

Swagger de cada servicio: `http://localhost:<puerto>/swagger-ui/index.html`.

---

## ☁️ Despliegue en AWS (EC2)
Según la Guía 18, en una sola instancia EC2 con Ubuntu:

1. **Perfil `prod`:** cada servicio escucha solo en `127.0.0.1` y lee todo desde variables de entorno, sin valores por defecto para credenciales: `<SERVICIO>_DB_URL`, `<SERVICIO>_DB_USER`, `<SERVICIO>_DB_PASSWORD`, `JWT_SECRET` (igual en los 8) y `APP_CORS_ALLOWED_ORIGIN` (`http://IP_PUBLICA`). Se entregan con un `EnvironmentFile` por servicio en `/etc/sonido-vivo/` (permisos 600).
2. **systemd:** una unidad por servicio, con `-Xmx256m` para que los 8 procesos quepan en memoria (se recomienda una instancia de 4 GB, como `t3.medium`).
3. **Nginx:** único punto público. Publica el front en el puerto **80** y reenvía `/api/<servicio>-service/` a `127.0.0.1:<puerto>/api/`.
4. **Grupo de seguridad:** solo puertos **80** y **22**. Los puertos 8081 a 8088 y 3306 quedan cerrados; Swagger se consulta con un túnel SSH.
5. **Rutas internas:** Nginx rechaza desde Internet `/api/auth/sincronizar` y `PATCH /api/productos/{id}/stock`, que solo se usan entre servicios por `127.0.0.1`.
