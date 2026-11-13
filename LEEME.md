# Sistema de Alquiler de Trajes de Danza

Aplicación de **escritorio** hecha con **JavaFX 21 + Spring Boot (solo JDBC) + PostgreSQL**.
Sirve para gestionar los trajes, los clientes, los conjuntos, las reservas, las entregas, las devoluciones con daños y los pagos de una tienda de alquiler de trajes de danza.
Incluye un **personalizador de trajes en 3D** donde el cliente diseña su traje (colores e implementos) y lo pide en alquiler o confección.

## Requisitos

- IntelliJ IDEA (Community o Ultimate)
- JDK 17 o superior (se recomienda 21)
- PostgreSQL (cualquier versión desde la 13)

## Cómo ponerlo en marcha

### 1. Crear la base de datos

En pgAdmin o en la ventana **Database** de IntelliJ, conectado a PostgreSQL:

```sql
CREATE DATABASE alquiler_trajes;
```

### 2. Cargar las tablas y los datos (dos scripts, en este orden)

Abre una consola sobre la base **alquiler_trajes** y ejecuta, **primero**:

```
src/main/resources/db/base_datos.sql
```

Crea las tablas, los triggers, la función, las vistas y carga los datos de ejemplo (50 trajes, 43 clientes, 58 alquileres...).

**Después**, ejecuta:

```
src/main/resources/db/personalizador.sql
```

Agrega el personalizador 3D sin borrar nada de lo anterior: partes por danza, 12 colores, 8 implementos,
los pedidos personalizados (con su trigger y su vista) y 5 pedidos de prueba.

Desde una terminal sería:

```
psql -U postgres -d alquiler_trajes -f src/main/resources/db/base_datos.sql
psql -U postgres -d alquiler_trajes -f src/main/resources/db/personalizador.sql
```

Los dos ya traen el inicio de sesión: la tabla `usuario` (con los usuarios de prueba), los estados
`SOLICITADO`/`RECHAZADO` de los alquileres y los estados nuevos de los pedidos.

**¿Ya tenías la base creada de antes?** No hace falta borrarla: ejecuta solo la migración (no borra datos y se puede
repetir):

```
psql -U postgres -d alquiler_trajes -f src/main/resources/db/login_roles.sql
psql -U postgres -d alquiler_trajes -f src/main/resources/db/portal_cliente.sql
psql -U postgres -d alquiler_trajes -f src/main/resources/db/panel_vendedor.sql
psql -U postgres -d alquiler_trajes -f src/main/resources/db/diseno_capas.sql
```

Los dos se pueden volver a ejecutar para dejar la base como al inicio.
**Si vuelves a ejecutar `base_datos.sql`, ejecuta también `personalizador.sql` después**, porque `base_datos.sql`
vuelve a crear las tablas `danza` y `cliente` de las que dependen los pedidos personalizados.

### 3. Poner tu contraseña de PostgreSQL (variable de entorno)

La contraseña **no** se escribe en el código: la aplicación la lee de la variable de entorno `DB_PASSWORD`
(y, si hace falta, `DB_USER` y `DB_URL`). Ver `src/main/resources/application-example.properties`.

- **IntelliJ:** Run → Edit Configurations → *Environment variables* → `DB_PASSWORD=tu_password_aqui`.
- **Terminal de Windows** (una sola vez; abre una terminal nueva después): `setx DB_PASSWORD "tu_password_aqui"`.

### 4. Abrir y ejecutar en IntelliJ

1. **File → Open** y elige la carpeta `alquiler-trajes` (la que tiene el `pom.xml`).
2. Espera a que Maven descargue las dependencias (barra de abajo). Si algo sale en rojo: clic derecho en `pom.xml` → **Maven → Reload project**.
3. Si pide el JDK: **File → Project Structure → SDK** y elige 17 o 21.
4. Abre `src/main/java/com/trajes/Launcher.java` y pulsa el botón verde ▶ junto a `main`.
5. Se abre la ventana **Trajes Candelaria** en la pantalla de inicio de sesión.

### 5. Ingresar

| Usuario    | Contraseña   | Rol      | Qué ve                                                            |
|------------|--------------|----------|-------------------------------------------------------------------|
| `admin`    | `admin123`   | ADMIN    | El panel de la tienda completo, con Reportes y Usuarios           |
| `vendedor` | `venta123`   | VENDEDOR | El panel de la tienda sin Reportes ni Usuarios                    |
| `cliente`  | `cliente123` | CLIENTE  | Su portal: Inicio, Catálogo, Diseña tu traje, Mis reservas, Mis diseños y Perfil (DNI 44268151) |

Desde el login también se puede **crear una cuenta de cliente** (nombre, DNI, celular, correo, usuario y contraseña):
se crean el cliente y su usuario en una sola transacción. Las contraseñas se guardan con BCrypt.

Cuando un cliente reserva desde su portal, el alquiler queda **SOLICITADO**: el traje ya queda apartado (el trigger
no deja que otro cliente lo pida en esas fechas) hasta que la tienda lo **aprueba** (pasa a RESERVADO) o lo
**rechaza** con un motivo que el cliente ve en «Mis reservas». En el panel de la tienda las solicitudes aparecen
arriba de «Próximos movimientos» como *Por aprobar*; doble clic para aprobarlas o rechazarlas.

> **Importante:** ejecuta siempre **`Launcher`**, no `AppFx`. `Launcher` no extiende `Application`, y así se evita el error
> *"JavaFX runtime components are missing"* sin tener que configurar `--module-path`.

También se puede ejecutar desde la ventana **Maven** de IntelliJ: **Plugins → javafx → javafx:run**, o en una terminal:

```
mvnw javafx:run
```

Si PostgreSQL no responde o la contraseña es incorrecta, la aplicación muestra un aviso y se cierra:
revisa la variable de entorno `DB_PASSWORD`.

## Qué se puede hacer

| Módulo | Funciones |
|---|---|
| Inicio | Resumen: trajes disponibles, reservas, alquileres en curso, devoluciones vencidas, ingresos del mes y próximos movimientos |
| Nueva reserva | Elegir cliente y fechas, ver solo los trajes **libres** en ese rango, marcar varios, ver el total y registrar adelanto |
| Alquileres | Lista con filtros por estado y búsqueda. Doble clic abre el detalle: entregar (cobra saldo y garantía), registrar adelanto, registrar devolución con daños por pieza y anular |
| Trajes | Registrar y editar trajes, sus piezas, enviarlos a mantenimiento, darlos de baja y ver su historial |
| Clientes | Registrar y editar (valida DNI de 8 dígitos y RUC de 11) y ver sus alquileres |
| Conjuntos | Agrupaciones con delegado; con 5 trajes o más tienen 10% de descuento |
| Diseña tu traje | Personalizador 3D: elegir cliente, danza, figura (varón o cholita), talla, tipo y fecha; pintar cada parte con los colores de la tabla `color`, agregar implementos y verlos en el maniquí; precio en vivo; guardar el diseño como PNG y registrar el pedido |
| Pedidos personalizados | Lista con filtro por estado y búsqueda. Doble clic abre el detalle con el maniquí 3D reconstruido, y permite avanzar el estado, registrar pagos a cuenta y anular |
| Reportes | Una pestaña por reporte: danzas más rentables, ingresos por mes, trajes más alquilados, piezas que más se dañan, devoluciones con retraso, rentabilidad por traje, pedidos personalizados por estado e implementos más pedidos |

## Flujo de un alquiler

```
RESERVADO ──entregar──► ENTREGADO ──devolver──► DEVUELTO
    │
    └──anular──► ANULADO
```

1. **Reserva:** se registra con adelanto (opcional).
2. **Entrega:** se cobra el saldo del alquiler y la garantía.
3. **Devolución:** se marcan las piezas dañadas. El monto se calcula solo (LEVE 10%, GRAVE 35%, PÉRDIDA 100% del valor de reposición) o se escribe a mano. Los daños se descuentan de la garantía y se devuelve el resto; si los daños superan la garantía, se cobra la diferencia.

## Personalizador por capas (Diseña tu traje)

El cliente diseña su traje desde su portal sobre fotos de render por capas:
`src/main/resources/render/<danza>/<ángulo>/base.png` + un PNG por prenda (`pantalon.png`, `chaqueta.png`...).
Una danza se activa sola cuando existe su carpeta (por ejemplo `render/caporal` → Caporales) y cada ángulo
(`frente`, `perfil`, `espalda`) agrega las flechas para girar. Las prendas se cobran según `parte_traje`
(el nombre del archivo debe coincidir con la parte, sin importar tildes ni números: `pantalon1.png` → Pantalón).
Al enviar, el diseño queda como pedido **PENDIENTE** con cada prenda y su color en hex, sus implementos y la
imagen (en la base y en la carpeta `disenos/`). La tienda lo ve en **Pedidos de diseño**, lo cotiza y el cliente
aprueba o rechaza el precio en **Mis diseños**.

> Para que el teñido sea correcto, cada PNG de prenda debe tener fondo transparente (en Blender:
> *Film → Transparent* y el cuerpo como *Holdout*).

## Personalizador de trajes en 3D (legado)

> Ya no aparece en ningún menú: su código quedó en `fx/legacy` por si se necesita. Lo que sigue describe
> cómo funcionaba.

**Maniquí.** Es una figura estilizada de unas 8 cabezas de alto, hecha con **mallas suaves** (`TriangleMesh` con
`VertexFormat.POINT_NORMAL_TEXCOORD`, 48 segmentos, normales promediadas por vértice y UV según la longitud real de cada pieza).
`GeneradorMallas` arma superficies de revolución a partir de perfiles con curvas Catmull-Rom (torso, piernas, cuello, pollera,
sombreros), tubos a lo largo de una curva (brazos doblados, trenzas, cuernos) y elipsoides (cabeza con mentón, manos, ojos).
Las prendas son mallas un poco más grandes que el cuerpo, no cajas:

| Danza | Forma |
|---|---|
| Caporales | Chaqueta entallada con hombreras y flecos, botones dorados, faja de aguayo, pantalón acampanado, botas altas con taco y sombrero de ala ancha |
| Morenada | Traje ancho y abultado tipo campana con pliegues, mangas anchas y matraca en la mano; máscara de moreno con ojos saltones y labios gruesos |
| Diablada | Máscara con cuernos curvos, ojos saltones, cejas, colmillos y orejas puntiagudas; pechera bordada y capa con pliegues por la espalda |
| Tinkus | Montera con ala levantada, camisa blanca, chaleco, faja de aguayo, pantalón corto, polainas y abarcas |
| Cholita (Caporales o Morenada) | Blusa con mangas abullonadas, mantilla, pollera amplia con vuelo y pliegues (corta en caporales, larga con enagua de encaje en morenada), bombín y trenzas con tullmas |

**Texturas.** `GeneradorTexturas` crea por código texturas de 512×512 (tela, terciopelo, cuero, piel, laca, metal, madera, aguayo,
encaje y plumas) y les pone encima el **bordado dorado andino** (grecas, rombos, soles y espirales) o las **lentejuelas** cuando se
eligen esos implementos. Cada material usa `diffuseMap` (teñido con el color elegido), `specularMap`, `specularPower` según el material
y `bumpMap` (mapa de normales sacado del relieve). Las imágenes se guardan en caché: al cambiar el color de una parte solo se genera
la textura de esa parte.

**Visor.** Es como un estudio fotográfico. Tiene una `SubScene` con depthBuffer y antialiasing `BALANCED` y una `PerspectiveCamera`
(fieldOfView 30) a la altura del pecho. La iluminación es de 3 puntos: una luz principal cálida, un relleno frío, una luz de contorno detrás
y una `AmbientLight` baja. El fondo es un degradado, la tarima es de madera y bajo los pies hay una sombra suave. El maniquí se gira
arrastrando el mouse (sigue girando un poco por inercia) y se acerca con la rueda, hasta un límite. El botón **Vista** pasa por frente,
lado y espalda, y también está **Girar automáticamente**. **Guardar diseño como imagen** saca la foto al doble de resolución.

**Implementos en 3D.** Cascabeles: esferas doradas en dos anillos alrededor de cada bota. Matraca: en la mano derecha. Pañuelo de encaje:
en la mano izquierda. Bordado dorado y lentejuelas: en la tela de la chaqueta (el bordado también en la pollera). Plumas, que son planos
con transparencia, y espejos: en el sombrero. Flecos dorados: en la cintura.

**Rendimiento.** Cada danza y figura se arma una sola vez (de 50 a 250 ms) y se reutiliza (unos 2 ms). La figura más pesada tiene
unos 120 mil triángulos y gira a unos 48 cuadros por segundo.

### Usar un modelo 3D propio (Blender)

Si existe `src/main/resources/modelos/<danza>.obj`, el visor usa ese modelo en lugar del maniquí generado por código.
Para la cholita se busca `<danza>_cholita.obj`. El nombre de la danza va en minúsculas, sin tildes y con `_` en vez de espacios.
Por ejemplo: `caporales.obj`, `morenada.obj`, `morenada_cholita.obj`, `diablada.obj`, `tinkus.obj`.

1. En Blender, modela al danzante de pie, mirando hacia **-Y** (la vista frontal, tecla 1 del teclado numérico). El tamaño no importa:
   se escala solo a la altura del visor y los pies se apoyan sobre la tarima.
2. Separa cada prenda en su propio objeto (o asígnale su propio material) y ponle el nombre de la parte del traje:
   `chaqueta`, `pantalon`, `pollera`, `botas`, `sombrero`, `capa`, `mascara`, `faja`. También sirven `blusa`, `chaleco`, `falda`,
   `zapatos`, `montera` y `bombin`. Para el cuerpo usa `piel` o `cuerpo`, para el pelo `pelo` y para los ojos `ojos`.
   Lo que tenga otro nombre se pinta de color maniquí.
3. **File → Export → Wavefront (.obj)** con *Forward: -Z*, *Up: Y* (los valores de siempre). Marca *Normals* y *UV Coordinates*, y
   en *Grouping* marca *Object Groups* (o *Material Groups*). Desmarca *Write Materials* si no quieres el `.mtl`, porque no se usa.
4. Copia el archivo a `src/main/resources/modelos/` y vuelve a ejecutar la aplicación.

Cada grupo se pinta con el color que elige el cliente para esa parte y se oculta si el traje no la lleva.
Los implementos se colocan a la altura estándar (manos, botas, sombrero). Si al modelo le faltan normales se calculan suavizadas,
y si le faltan coordenadas UV se proyectan alrededor del cuerpo. Si el modelo sale de espaldas, vuelve a exportarlo con *Forward: Z*.

**Precio.** El costo de confección es la suma del precio base de las partes más el precio por cantidad de los implementos.
Un pedido de **CONFECCION** paga el costo completo y uno de **ALQUILER** paga el **30%** del costo. La pantalla muestra el mismo cálculo
que hace el trigger.

**Flujo de un pedido:**

```
PENDIENTE ──► EN_CONFECCION ──► LISTO ──► ENTREGADO   (para entregar, el saldo debe estar pagado)
    └──────────────┴──────────────┴──anular──► ANULADO
```

## Reglas que controla la base de datos

- **Trigger `trg_validar_disponibilidad`:** no deja reservar un traje que ya está ocupado en esas fechas, ni uno que esté en mantenimiento o de baja.
- **Trigger `trg_actualizar_totales`:** recalcula el monto del alquiler, la garantía y los daños cada vez que cambia el detalle.
- **CHECK:** estados válidos, tallas, montos positivos, fechas coherentes.
- **Vistas:** `v_alquileres_resumen` y `v_trajes_estado`.
- **Función:** `fn_trajes_disponibles(desde, hasta, danza)`.
- **Trigger `trg_recalcular_pedido_parte` / `trg_recalcular_pedido_implemento`** (personalizador): recalculan `monto_total` del pedido cada vez que cambian sus partes o implementos.
- **Trigger `trg_validar_parte_pedido`:** no deja agregar a un pedido una parte que es de otra danza.
- **Vista `v_pedidos_resumen`:** cliente, danza, talla, estado, nº de partes, nº de implementos, total y saldo de cada pedido.

Si intentas reservar un traje ocupado, el sistema muestra el mensaje del trigger en una ventana de error y no guarda nada (la operación es una transacción con `@Transactional`).

## Estructura del proyecto

```
alquiler-trajes/
├── pom.xml
└── src/main/
    ├── java/com/trajes/
    │   ├── Launcher.java                    ← se ejecuta este
    │   ├── AlquilerTrajesApplication.java   (configuración de Spring: conexión y servicios)
    │   ├── modelo/      (records con los datos de cada pantalla)
    │   ├── servicio/    (consultas SQL con JdbcTemplate y reglas del negocio)
    │   └── fx/
    │       ├── AppFx.java              (arranca y cierra Spring, abre la ventana)
    │       ├── VentanaPrincipal.java   (menú lateral + pantalla del centro)
    │       ├── Ui.java                 (formatos S/ y dd/MM/yyyy, tablas, estados, mensajes)
    │       ├── Servicios.java
    │       ├── tresd/
    │       │   ├── Maniqui3D.java         (danzante 3D por danza y figura, materiales por parte e implementos)
    │       │   ├── Visor3D.java           (estudio: cámara, 3 luces, tarima, sombra, inercia, vistas y foto ×2)
    │       │   ├── GeneradorMallas.java   (torno, tubo, elipsoide y láminas con normales suaves y UV)
    │       │   ├── GeneradorTexturas.java (texturas procedurales 512×512 con caché: difusa, especular y relieve)
    │       │   └── ObjLoader.java         (lector de .obj con v, vt, vn, f, g y usemtl: un MeshView por grupo)
    │       └── vistas/     (una clase por pantalla o diálogo; DisenioTrajeVista, PedidosVista, PedidoDetalleDialogo...)
    └── resources/
        ├── application.properties           ← conexión a PostgreSQL
        ├── estilos.css                      ← estructura y componentes (colores por variables -c-*)
        ├── tema-guindo.css / tema-marino.css ← paletas; se elige en el menú del avatar (Tema)
        ├── fonts/                           ← Playfair Display y Plus Jakarta Sans (licencia OFL)
        ├── modelos/                         ← (opcional) modelos .obj por danza, ver «Usar un modelo 3D propio»
        ├── db/base_datos.sql                ← script de la base de datos (1.º)
        ├── db/personalizador.sql            ← personalizador 3D (2.º, después de base_datos.sql)
        └── db/consultas_ejemplo.sql         ← 15 consultas para la exposición
```

## Problemas comunes

| Mensaje | Solución |
|---|---|
| `password authentication failed` | La contraseña de `application.properties` no es la de tu PostgreSQL |
| `database "alquiler_trajes" does not exist` | Falta el paso 1 |
| `relation "traje" does not exist` | Falta el paso 2 (ejecutar `base_datos.sql` sobre `alquiler_trajes`) |
| "Faltan las tablas del personalizador" al abrir *Diseña tu traje* | Ejecuta `personalizador.sql` después de `base_datos.sql` |
| El maniquí no se ve (zona gris) | La tarjeta gráfica no soporta 3D: actualiza el controlador de video |
| `JavaFX runtime components are missing` | Estás ejecutando `AppFx`; ejecuta `Launcher` |
| Rojo en los `import org.springframework...` | Clic derecho en `pom.xml` → **Maven → Reload project** |
