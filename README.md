# Asignación Microservicios — Productos y Órdenes con Quarkus

## Integrantes del equipo

- Freddy Ali Castro Roman
- Katia Ximena Návarez Espinoza
- Jesús Rodrigo Villegas Arguelles
- Ethan Gael Valdez Romero

## Descripción

**Asignación 1:** replicar el ejemplo de Productos y Órdenes usando Quarkus con base de datos,
implementando la persistencia con Panache y separando los 2 microservicios en proyectos y puertos distintos.

**Asignación 2:** tomar el resultado anterior y configurarlo para el uso de un **API Gateway**.
Se eligió implementar el gateway con **Quarkus** (proyecto `MSGateway`).

## Arquitectura

```
                    ┌─────────────────────┐
  Cliente ────────► │   MSGateway :8080   │   (único punto de entrada)
                    └──────────┬──────────┘
                 ┌─────────────┴─────────────┐
         /api/productos/**            /api/ordenes/**
                 ▼                           ▼
      ┌─────────────────────┐     ┌─────────────────────┐
      │  MSProductos :8081  │ ◄── │   MSOrdenes :8082   │
      │  BD: productos_db   │ REST│   BD: ordenes_db    │
      └─────────────────────┘     └─────────────────────┘
```

`MSOrdenes` consulta a `MSProductos` por REST para obtener los precios y calcular el total de la orden.

| Proyecto      | Puerto | Base de datos (MySQL) | Responsabilidad                                                  |
|---------------|--------|-----------------------|------------------------------------------------------------------|
| `MSGateway`   | 8080   | —                     | Recibe todas las peticiones y las redirige al microservicio correspondiente |
| `MSProductos` | 8081   | `productos_db`        | Alta y consulta de productos (Panache)                           |
| `MSOrdenes`   | 8082   | `ordenes_db`          | Alta y consulta de órdenes; consulta a Productos para calcular el total |

### Cómo funciona el gateway

- `ProductosClient` y `OrdenesClient` son clientes REST (MicroProfile Rest Client) hacia cada microservicio.
- `ProductoGatewayResource` y `OrdenGatewayResource` exponen las rutas `/api/...` y reenvían cada petición.
- `Reenvio` devuelve al cliente la respuesta tal cual la dio el microservicio (código, tipo y cuerpo),
  incluyendo errores como 400 o 404. Si un microservicio está apagado, el gateway responde **503**.
- Cada petición que pasa por el gateway se registra en la consola (access log).

## Tecnologías

- Java 17+
- Quarkus 3.15.1 (Quarkus REST, REST Client, Hibernate ORM con Panache)
- MySQL 8
- Maven

## Configuración

En `MSProductos/src/main/resources/application.properties` y `MSOrdenes/src/main/resources/application.properties`
cambiar la contraseña de MySQL:

```properties
quarkus.datasource.password=CAMBIAR
```

Las bases de datos `productos_db` y `ordenes_db` se crean automáticamente al iniciar.

Si los microservicios corren en otras máquinas, cambiar `localhost` por la IP correspondiente en
`MSGateway/src/main/resources/application.properties` (y en el de `MSOrdenes` para la URL de productos).

## Ejecución

Levantar los proyectos **en este orden**, cada uno en su propia terminal:

```bash
cd MSProductos && mvn quarkus:dev
cd MSOrdenes   && mvn quarkus:dev
cd MSGateway   && mvn quarkus:dev
```

En **IntelliJ IDEA**: abrir la carpeta raíz del repositorio, cargar los tres `pom.xml` como proyectos Maven y,
desde el panel *Maven*, ejecutar `Plugins → quarkus → quarkus:dev` en cada proyecto.

## Endpoints (a través del gateway)

| Método | Ruta                   | Cuerpo                                | Descripción                         |
|--------|------------------------|---------------------------------------|-------------------------------------|
| GET    | `/api/productos`       | —                                     | Lista todos los productos           |
| GET    | `/api/productos/{id}`  | —                                     | Consulta un producto                |
| POST   | `/api/productos`       | `{"name": "Lapiz", "price": 10.5}`    | Crea un producto                    |
| GET    | `/api/ordenes`         | —                                     | Lista todas las órdenes             |
| GET    | `/api/ordenes/{id}`    | —                                     | Consulta una orden                  |
| POST   | `/api/ordenes`         | `[1, 2, 3]`                           | Crea una orden con 3 productos      |

### Ejemplo de prueba

```bash
# 1. Crear 3 productos
curl -X POST http://localhost:8080/api/productos -H "Content-Type: application/json" -d '{"name":"Lapiz","price":10.5}'
curl -X POST http://localhost:8080/api/productos -H "Content-Type: application/json" -d '{"name":"Cuaderno","price":25}'
curl -X POST http://localhost:8080/api/productos -H "Content-Type: application/json" -d '{"name":"Borrador","price":5}'

# 2. Crear una orden con esos productos (el total se calcula consultando a MSProductos)
curl -X POST http://localhost:8080/api/ordenes -H "Content-Type: application/json" -d '[1,2,3]'

# 3. Consultar
curl http://localhost:8080/api/productos
curl http://localhost:8080/api/ordenes
```

### Respuestas de error

| Caso                                     | Código |
|------------------------------------------|--------|
| Producto u orden inexistente             | 404    |
| Orden con un número de productos distinto de 3 | 400    |
| Orden con un producto que no existe      | 404    |
| Microservicio apagado o inaccesible      | 503    |
