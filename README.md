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

**Asignación 3 (Equipo 3):** exposición del paradigma de mensajería **Routing** de RabbitMQ, con demo
integrado a este sistema → ver [Paradigma Routing con RabbitMQ](#paradigma-routing-con-rabbitmq).

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

---

## Paradigma Routing con RabbitMQ

Basado en el tutorial oficial [Routing (Java)](https://www.rabbitmq.com/tutorials/tutorial-four-java).
En Publish/Subscribe un exchange `fanout` manda **todos** los mensajes a **todas** las colas. Con Routing
cada consumidor recibe **solo los mensajes que le interesan**: el productor etiqueta cada mensaje con una
**routing key** y un exchange de tipo **`direct`** lo entrega únicamente a las colas cuyo **binding key**
es **exactamente igual**.

### Cómo lo aplicamos a este sistema

`MSOrdenes` publica un evento cada vez que alguien intenta crear una orden, usando la **severidad** como
routing key. Tres receptores se suscriben a distintas severidades:

```
                                         exchange "direct_logs" (direct)
 Cliente ─► MSGateway ─► MSOrdenes ───►  ┌──────────────────────────────┐
                        (productor)      │  info ─────────────────────► cola_monitor   ─► Monitor   (ve todo)
                                         │  warning ──┬───────────────► cola_monitor
                                         │            └───────────────► cola_auditoria ─► Auditoría (guarda en archivo)
                                         │  error ────┬───────────────► cola_monitor
                                         │            ├───────────────► cola_auditoria
                                         │            └───────────────► cola_alertas   ─► Alertas   (solo errores)
                                         │  debug ──► ✗ ninguna cola coincide → descartado
                                         └──────────────────────────────┘
```

| Evento en `MSOrdenes`                                  | HTTP | Routing key | Monitor | Auditoría | Alertas |
|--------------------------------------------------------|------|-------------|:-------:|:---------:|:-------:|
| Orden creada                                           | 201  | `info`      | ✓       |           |         |
| Orden con un número de productos distinto de 3         | 400  | `warning`   | ✓       | ✓         |         |
| Orden con un producto que no existe                    | 404  | `warning`   | ✓       | ✓         |         |
| `MSProductos` apagado (no se puede calcular el total)  | 503  | `error`     | ✓       | ✓         | ✓       |
| Mensaje manual con `debug` (nadie hizo binding)        | —    | `debug`     |         |           |         |

### Elementos del paradigma en el código

| Elemento | Dónde | Qué hace |
|----------|-------|----------|
| **Productor** | `MSOrdenes/.../mensajeria/PublicadorLogs.java`, `RoutingRabbitMQ/.../EmisorLogsDirect.java` | Publica con `basicPublish(exchange, routingKey, props, body)` |
| **Exchange `direct`** | `exchangeDeclare("direct_logs", BuiltinExchangeType.DIRECT)` | Compara routing key == binding key; si nadie coincide, descarta |
| **Routing key** | 2º parámetro de `basicPublish` | Etiqueta del mensaje: `info`, `warning`, `error` |
| **Cola** | `queueDeclare("cola_<nombre>", false, true, true, null)` | Buzón propio de cada consumidor (exclusiva, se borra al desconectarse) |
| **Binding / binding key** | `queueBind(cola, "direct_logs", severidad)` | "Esta cola quiere los mensajes con esta key" (puede haber varios por cola) |
| **Consumidor** | `RoutingRabbitMQ/.../ReceptorLogsDirect.java` | `basicConsume` + `DeliverCallback` procesa cada mensaje |

### Requisitos adicionales

- RabbitMQ en `localhost:5672`. Lo más fácil es Docker (desde la raíz del repo):

  ```bash
  docker compose up -d
  ```

  Consola web: <http://localhost:15672> (usuario `guest`, contraseña `guest`).
  Sin Docker: instalar RabbitMQ (requiere Erlang) desde <https://www.rabbitmq.com/docs/download>.
- Si RabbitMQ está en otra máquina: cambiar `rabbitmq.host` en
  `MSOrdenes/src/main/resources/application.properties` y, para los receptores, usar la variable de entorno
  `RABBITMQ_HOST` (o `-Drabbitmq.host=IP`).

### Demo paso a paso

1. **RabbitMQ:** `docker compose up -d`
2. **Receptores** (cada uno en su terminal, dentro de `RoutingRabbitMQ/`):

   ```bash
   mvn -q compile exec:java@monitor      # binding keys: info, warning, error
   mvn -q compile exec:java@auditoria    # binding keys: warning, error  (también escribe auditoria.log)
   mvn -q compile exec:java@alertas      # binding key:  error
   ```

   En la consola web → *Exchanges* → `direct_logs` → *Bindings* se ven las 6 bindings.
3. **Microservicios:** levantar `MSProductos`, `MSOrdenes` y `MSGateway` como siempre (`mvn quarkus:dev`).
4. **Generar eventos reales** a través del gateway:

   ```bash
   # info    -> solo Monitor
   curl -X POST http://localhost:8080/api/ordenes -H "Content-Type: application/json" -d '[1,2,3]'
   # warning -> Monitor y Auditoría
   curl -X POST http://localhost:8080/api/ordenes -H "Content-Type: application/json" -d '[1,2]'
   curl -X POST http://localhost:8080/api/ordenes -H "Content-Type: application/json" -d '[1,2,99]'
   # error   -> Monitor, Auditoría y Alertas  (primero apagar MSProductos con Ctrl+C)
   curl -X POST http://localhost:8080/api/ordenes -H "Content-Type: application/json" -d '[1,2,3]'
   ```

   > En PowerShell usar `curl.exe` y escapar el JSON, o usar Postman.

5. **Emisor manual** (opcional, no necesita los microservicios ni MySQL — sirve de respaldo para el demo):

   ```bash
   cd RoutingRabbitMQ
   mvn -q compile exec:java@emisor
   > info Orden #10 creada
   > error Se cayó la base de datos
   > debug nadie escucha esta key        # -> el exchange lo descarta
   ```

Si `MSOrdenes` no encuentra RabbitMQ, las órdenes **se siguen creando** normalmente; solo muestra una
advertencia en su consola.
