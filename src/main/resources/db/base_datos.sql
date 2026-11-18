-- Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor.

-- =====================================================================
-- SISTEMA DE ALQUILER DE TRAJES DE DANZA (PostgreSQL)
-- Alquiler de trajes para la festividad de la Virgen de la Candelaria
-- y otras presentaciones de conjuntos de danza.
--
-- Tablas: danza, tipo_pieza, traje, pieza, cliente, conjunto,
--         alquiler, detalle_alquiler, danio, pago, mantenimiento, usuario
--
-- Uso:  psql -U postgres -c "CREATE DATABASE alquiler_trajes;"
--       psql -U postgres -d alquiler_trajes -f base_datos.sql
-- =====================================================================

DROP VIEW IF EXISTS v_alquileres_resumen, v_trajes_estado CASCADE;
DROP TABLE IF EXISTS usuario, pago, danio, detalle_alquiler, alquiler, mantenimiento,
                     pieza, traje, conjunto, cliente, tipo_pieza, danza CASCADE;
DROP FUNCTION IF EXISTS fn_validar_disponibilidad() CASCADE;
DROP FUNCTION IF EXISTS fn_actualizar_totales() CASCADE;
DROP FUNCTION IF EXISTS fn_trajes_disponibles(DATE, DATE, VARCHAR) CASCADE;

-- ===================== CATÁLOGOS =====================

-- Danzas que se atienden (diablada, morenada, caporales, ...)
CREATE TABLE danza (
    id_danza      SERIAL PRIMARY KEY,
    nombre        VARCHAR(50)  NOT NULL UNIQUE,
    tipo          VARCHAR(20)  NOT NULL CHECK (tipo IN ('TRAJE DE LUCES', 'AUTOCTONA')),
    descripcion   VARCHAR(200)
);

-- Partes que forman un traje (máscara, chaqueta, pollera, botas, ...)
CREATE TABLE tipo_pieza (
    id_tipo_pieza SERIAL PRIMARY KEY,
    nombre        VARCHAR(40)  NOT NULL UNIQUE
);

-- ===================== TRAJES =====================

-- Cada traje es un conjunto completo que se alquila como una unidad
CREATE TABLE traje (
    id_traje          SERIAL PRIMARY KEY,
    codigo            VARCHAR(10)  NOT NULL UNIQUE,          -- ej. DIA-001
    id_danza          INTEGER      NOT NULL REFERENCES danza(id_danza),
    personaje         VARCHAR(50)  NOT NULL,                 -- ej. Diablo, Achachi, Caporal
    genero            VARCHAR(6)   NOT NULL CHECK (genero IN ('VARON', 'MUJER', 'UNISEX')),
    talla             VARCHAR(3)   NOT NULL CHECK (talla IN ('XS', 'S', 'M', 'L', 'XL')),
    color             VARCHAR(30),
    precio_alquiler   NUMERIC(8,2) NOT NULL CHECK (precio_alquiler > 0),   -- por todo el periodo
    garantia          NUMERIC(8,2) NOT NULL CHECK (garantia >= 0),         -- se devuelve si no hay daños
    fecha_adquisicion DATE,
    estado            VARCHAR(15)  NOT NULL DEFAULT 'DISPONIBLE'
                      CHECK (estado IN ('DISPONIBLE', 'MANTENIMIENTO', 'BAJA'))
);

-- Piezas de cada traje, para registrar daños por pieza
CREATE TABLE pieza (
    id_pieza          SERIAL PRIMARY KEY,
    id_traje          INTEGER      NOT NULL REFERENCES traje(id_traje) ON DELETE CASCADE,
    id_tipo_pieza     INTEGER      NOT NULL REFERENCES tipo_pieza(id_tipo_pieza),
    descripcion       VARCHAR(100),
    estado            VARCHAR(10)  NOT NULL DEFAULT 'BUENO'
                      CHECK (estado IN ('BUENO', 'REGULAR', 'DANADO', 'PERDIDO')),
    valor_reposicion  NUMERIC(8,2) NOT NULL CHECK (valor_reposicion >= 0),
    UNIQUE (id_traje, id_tipo_pieza)          -- un traje no repite el mismo tipo de pieza
);

-- ===================== CLIENTES =====================

CREATE TABLE cliente (
    id_cliente        SERIAL PRIMARY KEY,
    tipo_documento    VARCHAR(3)   NOT NULL CHECK (tipo_documento IN ('DNI', 'RUC', 'CE')),
    numero_documento  VARCHAR(11)  NOT NULL UNIQUE,
    nombres           VARCHAR(100) NOT NULL,
    apellidos         VARCHAR(100),
    telefono          VARCHAR(15),
    email             VARCHAR(100),
    direccion         VARCHAR(150),
    fecha_registro    DATE         NOT NULL DEFAULT CURRENT_DATE
);

-- Conjuntos (agrupaciones) que alquilan en bloque; su delegado es un cliente
CREATE TABLE conjunto (
    id_conjunto       SERIAL PRIMARY KEY,
    nombre            VARCHAR(120) NOT NULL UNIQUE,
    id_danza          INTEGER      NOT NULL REFERENCES danza(id_danza),
    id_delegado       INTEGER      REFERENCES cliente(id_cliente),
    telefono          VARCHAR(15)
);

-- ===================== ALQUILERES =====================

CREATE TABLE alquiler (
    id_alquiler                SERIAL PRIMARY KEY,
    id_cliente                 INTEGER      NOT NULL REFERENCES cliente(id_cliente),
    id_conjunto                INTEGER      REFERENCES conjunto(id_conjunto),   -- NULL si es particular
    fecha_registro             DATE         NOT NULL DEFAULT CURRENT_DATE,
    fecha_salida               DATE         NOT NULL,
    fecha_devolucion_prevista  DATE         NOT NULL,
    fecha_devolucion_real      DATE,
    -- SOLICITADO: pedido por el cliente desde su portal, pendiente de aprobación
    estado                     VARCHAR(10)  NOT NULL DEFAULT 'RESERVADO'
                               CHECK (estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO', 'DEVUELTO',
                                                 'ANULADO', 'RECHAZADO')),
    monto_alquiler             NUMERIC(10,2) NOT NULL DEFAULT 0,
    monto_garantia             NUMERIC(10,2) NOT NULL DEFAULT 0,
    observacion                VARCHAR(200),
    motivo_rechazo             VARCHAR(200),                                 -- solo si RECHAZADO
    creado_en                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- fecha y hora de registro
    CHECK (estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL),
    CHECK (fecha_devolucion_prevista >= fecha_salida),
    CHECK (fecha_devolucion_real IS NULL OR fecha_devolucion_real >= fecha_salida),
    CHECK (estado <> 'DEVUELTO' OR fecha_devolucion_real IS NOT NULL)
);

CREATE TABLE detalle_alquiler (
    id_detalle        SERIAL PRIMARY KEY,
    id_alquiler       INTEGER      NOT NULL REFERENCES alquiler(id_alquiler) ON DELETE CASCADE,
    id_traje          INTEGER      NOT NULL REFERENCES traje(id_traje),
    precio            NUMERIC(8,2) NOT NULL CHECK (precio >= 0),
    garantia          NUMERIC(8,2) NOT NULL CHECK (garantia >= 0),
    UNIQUE (id_alquiler, id_traje)            -- el mismo traje no se repite en un alquiler
);

-- Daños o pérdidas detectados al devolver un traje
CREATE TABLE danio (
    id_danio          SERIAL PRIMARY KEY,
    id_detalle        INTEGER      NOT NULL REFERENCES detalle_alquiler(id_detalle) ON DELETE CASCADE,
    id_pieza          INTEGER      NOT NULL REFERENCES pieza(id_pieza),
    tipo              VARCHAR(10)  NOT NULL CHECK (tipo IN ('LEVE', 'GRAVE', 'PERDIDA')),
    descripcion       VARCHAR(150),
    monto_cobro       NUMERIC(8,2) NOT NULL CHECK (monto_cobro >= 0)
);

CREATE TABLE pago (
    id_pago           SERIAL PRIMARY KEY,
    id_alquiler       INTEGER      NOT NULL REFERENCES alquiler(id_alquiler) ON DELETE CASCADE,
    fecha_pago        DATE         NOT NULL,
    concepto          VARCHAR(20)  NOT NULL
                      CHECK (concepto IN ('ADELANTO', 'SALDO', 'GARANTIA', 'COBRO_DANIO', 'DEVOLUCION_GARANTIA')),
    monto             NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    metodo            VARCHAR(15)  NOT NULL CHECK (metodo IN ('EFECTIVO', 'YAPE', 'PLIN', 'TRANSFERENCIA'))
);

-- Lavado y reparación de trajes entre temporadas
CREATE TABLE mantenimiento (
    id_mantenimiento  SERIAL PRIMARY KEY,
    id_traje          INTEGER      NOT NULL REFERENCES traje(id_traje),
    tipo              VARCHAR(12)  NOT NULL CHECK (tipo IN ('LAVADO', 'REPARACION', 'BORDADO')),
    fecha_inicio      DATE         NOT NULL,
    fecha_fin         DATE,
    costo             NUMERIC(8,2) NOT NULL DEFAULT 0 CHECK (costo >= 0),
    detalle           VARCHAR(150),
    CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE INDEX idx_alquiler_fechas  ON alquiler (fecha_salida, fecha_devolucion_prevista);
CREATE INDEX idx_detalle_traje    ON detalle_alquiler (id_traje);
CREATE INDEX idx_traje_danza      ON traje (id_danza, talla);

-- ===================== REGLAS DE NEGOCIO (TRIGGERS) =====================

-- Regla 1: un traje no puede estar en dos alquileres activos con fechas
-- que se crucen, ni alquilarse si está en mantenimiento o de baja.
CREATE FUNCTION fn_validar_disponibilidad() RETURNS TRIGGER AS $$
DECLARE
    v_salida   DATE;
    v_retorno  DATE;
    v_estado   VARCHAR(15);
    v_choque   INTEGER;
BEGIN
    SELECT fecha_salida, fecha_devolucion_prevista
      INTO v_salida, v_retorno
      FROM alquiler WHERE id_alquiler = NEW.id_alquiler;

    SELECT estado INTO v_estado FROM traje WHERE id_traje = NEW.id_traje;
    IF v_estado <> 'DISPONIBLE' THEN
        RAISE EXCEPTION 'El traje % no se puede alquilar: está en estado %', NEW.id_traje, v_estado;
    END IF;

    SELECT a.id_alquiler INTO v_choque
      FROM detalle_alquiler d
      JOIN alquiler a ON a.id_alquiler = d.id_alquiler
     WHERE d.id_traje = NEW.id_traje
       AND d.id_detalle <> COALESCE(NEW.id_detalle, -1)
       AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')   -- SOLICITADO también ocupa
       AND a.fecha_salida <= v_retorno
       AND a.fecha_devolucion_prevista >= v_salida
     LIMIT 1;

    IF v_choque IS NOT NULL THEN
        RAISE EXCEPTION 'El traje % ya está reservado en esas fechas (alquiler %)', NEW.id_traje, v_choque;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_disponibilidad
BEFORE INSERT OR UPDATE ON detalle_alquiler
FOR EACH ROW EXECUTE FUNCTION fn_validar_disponibilidad();

-- Regla 2: los montos de la cabecera del alquiler se mantienen iguales a
-- la suma de sus detalles (alquiler y garantía).
CREATE FUNCTION fn_actualizar_totales() RETURNS TRIGGER AS $$
DECLARE
    v_id INTEGER := COALESCE(NEW.id_alquiler, OLD.id_alquiler);
BEGIN
    UPDATE alquiler
       SET monto_alquiler = COALESCE((SELECT SUM(precio)   FROM detalle_alquiler WHERE id_alquiler = v_id), 0),
           monto_garantia = COALESCE((SELECT SUM(garantia) FROM detalle_alquiler WHERE id_alquiler = v_id), 0)
     WHERE id_alquiler = v_id;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_actualizar_totales
AFTER INSERT OR UPDATE OR DELETE ON detalle_alquiler
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_totales();

-- ===================== FUNCIÓN DE CONSULTA =====================

-- Trajes libres entre dos fechas (opcionalmente de una danza)
CREATE FUNCTION fn_trajes_disponibles(p_desde DATE, p_hasta DATE, p_danza VARCHAR DEFAULT NULL)
RETURNS TABLE (codigo VARCHAR, danza VARCHAR, personaje VARCHAR, genero VARCHAR,
               talla VARCHAR, precio_alquiler NUMERIC, garantia NUMERIC) AS $$
    SELECT t.codigo, d.nombre, t.personaje, t.genero, t.talla, t.precio_alquiler, t.garantia
      FROM traje t
      JOIN danza d ON d.id_danza = t.id_danza
     WHERE t.estado = 'DISPONIBLE'
       AND (p_danza IS NULL OR d.nombre = p_danza)
       AND NOT EXISTS (SELECT 1
                         FROM detalle_alquiler da
                         JOIN alquiler a ON a.id_alquiler = da.id_alquiler
                        WHERE da.id_traje = t.id_traje
                          AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')
                          AND a.fecha_salida <= p_hasta
                          AND a.fecha_devolucion_prevista >= p_desde)
     ORDER BY d.nombre, t.personaje, t.talla;
$$ LANGUAGE sql;
-- ===================== DATOS DE PRUEBA =====================

INSERT INTO danza (nombre, tipo, descripcion) VALUES
('Diablada', 'TRAJE DE LUCES', 'Danza de diablos y chinas diablas, con máscaras de yeso y bordados de pedrería'),
('Morenada', 'TRAJE DE LUCES', 'Danza de morenos con trajes de rueda, matracas y cholitas de pollera'),
('Caporales', 'TRAJE DE LUCES', 'Danza de caporales con botas de cascabeles y cholitas de falda corta'),
('Tinkus', 'TRAJE DE LUCES', 'Danza de origen guerrero con monteras y abarcas'),
('Kullawada', 'TRAJE DE LUCES', 'Danza de hilanderos con ruecas y sombreros bordados'),
('Waca Waca', 'TRAJE DE LUCES', 'Danza satírica de toreros y wacas (toros)'),
('Llamerada', 'TRAJE DE LUCES', 'Danza de pastores de llamas con hondas'),
('Ayarachi', 'AUTOCTONA', 'Danza autóctona con grandes tocados de plumas de suri'),
('Q''ajelo', 'AUTOCTONA', 'Danza autóctona de jinetes con ponchos y chicotes');

INSERT INTO tipo_pieza (nombre) VALUES
('Máscara'),
('Chaqueta'),
('Pantalón'),
('Pollera'),
('Capa'),
('Pechera'),
('Botas'),
('Sombrero'),
('Matraca'),
('Mantilla'),
('Faja'),
('Poncho');

INSERT INTO traje (codigo, id_danza, personaje, genero, talla, color, precio_alquiler, garantia, fecha_adquisicion) VALUES
('DIA-001', 1, 'Diablo', 'VARON', 'S', 'Rojo y dorado', 350.00, 500.00, '2021-11-17'),
('DIA-002', 1, 'Diablo', 'VARON', 'M', 'Azul y dorado', 350.00, 500.00, '2019-06-20'),
('DIA-003', 1, 'Diablo', 'VARON', 'M', 'Azul y dorado', 350.00, 500.00, '2023-09-26'),
('DIA-004', 1, 'Diablo', 'VARON', 'L', 'Azul y dorado', 350.00, 500.00, '2023-10-25'),
('DIA-005', 1, 'Diablo', 'VARON', 'L', 'Azul y dorado', 350.00, 500.00, '2022-06-01'),
('DIA-006', 1, 'Diablo', 'VARON', 'XL', 'Azul y dorado', 350.00, 500.00, '2019-04-10'),
('DIA-007', 1, 'China diabla', 'MUJER', 'XS', 'Rojo y dorado', 250.00, 350.00, '2022-03-27'),
('DIA-008', 1, 'China diabla', 'MUJER', 'S', 'Fucsia y plata', 250.00, 350.00, '2024-08-07'),
('DIA-009', 1, 'China diabla', 'MUJER', 'M', 'Fucsia y plata', 250.00, 350.00, '2021-08-12'),
('DIA-010', 1, 'China diabla', 'MUJER', 'M', 'Fucsia y plata', 250.00, 350.00, '2024-11-21'),
('MOR-001', 2, 'Moreno', 'VARON', 'M', 'Morado y dorado', 300.00, 450.00, '2024-08-03'),
('MOR-002', 2, 'Moreno', 'VARON', 'L', 'Blanco y plata', 300.00, 450.00, '2021-10-05'),
('MOR-003', 2, 'Moreno', 'VARON', 'L', 'Blanco y plata', 300.00, 450.00, '2019-08-12'),
('MOR-004', 2, 'Moreno', 'VARON', 'XL', 'Blanco y plata', 300.00, 450.00, '2022-04-13'),
('MOR-005', 2, 'Moreno', 'VARON', 'XL', 'Blanco y plata', 300.00, 450.00, '2019-09-17'),
('MOR-006', 2, 'Cholita morenada', 'MUJER', 'S', 'Verde esmeralda', 200.00, 300.00, '2022-09-17'),
('MOR-007', 2, 'Cholita morenada', 'MUJER', 'M', 'Celeste', 200.00, 300.00, '2022-12-16'),
('MOR-008', 2, 'Cholita morenada', 'MUJER', 'M', 'Verde esmeralda', 200.00, 300.00, '2023-03-19'),
('MOR-009', 2, 'Cholita morenada', 'MUJER', 'L', 'Guinda', 200.00, 300.00, '2020-03-23'),
('CAP-001', 3, 'Caporal', 'VARON', 'S', 'Azul eléctrico', 180.00, 250.00, '2024-04-23'),
('CAP-002', 3, 'Caporal', 'VARON', 'M', 'Azul eléctrico', 180.00, 250.00, '2022-10-05'),
('CAP-003', 3, 'Caporal', 'VARON', 'M', 'Rojo y negro', 180.00, 250.00, '2020-04-21'),
('CAP-004', 3, 'Caporal', 'VARON', 'L', 'Azul eléctrico', 180.00, 250.00, '2022-10-23'),
('CAP-005', 3, 'Caporal', 'VARON', 'L', 'Azul eléctrico', 180.00, 250.00, '2021-11-13'),
('CAP-006', 3, 'Caporal', 'VARON', 'XL', 'Azul eléctrico', 180.00, 250.00, '2021-11-26'),
('CAP-007', 3, 'Cholita caporal', 'MUJER', 'XS', 'Rojo y negro', 160.00, 220.00, '2022-09-07'),
('CAP-008', 3, 'Cholita caporal', 'MUJER', 'S', 'Rojo y negro', 160.00, 220.00, '2021-03-02'),
('CAP-009', 3, 'Cholita caporal', 'MUJER', 'S', 'Blanco y dorado', 160.00, 220.00, '2024-12-15'),
('CAP-010', 3, 'Cholita caporal', 'MUJER', 'M', 'Rojo y negro', 160.00, 220.00, '2021-06-09'),
('CAP-011', 3, 'Cholita caporal', 'MUJER', 'M', 'Rojo y negro', 160.00, 220.00, '2022-11-25'),
('TIN-001', 4, 'Tinku varón', 'VARON', 'M', 'Multicolor', 120.00, 150.00, '2022-11-22'),
('TIN-002', 4, 'Tinku varón', 'VARON', 'L', 'Multicolor', 120.00, 150.00, '2021-08-15'),
('TIN-003', 4, 'Tinku varón', 'VARON', 'L', 'Multicolor', 120.00, 150.00, '2022-11-09'),
('TIN-004', 4, 'Tinku mujer', 'MUJER', 'S', 'Multicolor', 120.00, 150.00, '2020-09-27'),
('TIN-005', 4, 'Tinku mujer', 'MUJER', 'M', 'Multicolor', 120.00, 150.00, '2019-07-16'),
('KUL-001', 5, 'Kullawa', 'VARON', 'M', 'Turquesa', 150.00, 200.00, '2019-06-24'),
('KUL-002', 5, 'Kullawa', 'VARON', 'L', 'Turquesa', 150.00, 200.00, '2020-10-01'),
('KUL-003', 5, 'Cholita kullawada', 'MUJER', 'S', 'Rosado y plata', 150.00, 200.00, '2019-10-19'),
('KUL-004', 5, 'Cholita kullawada', 'MUJER', 'M', 'Rosado y plata', 150.00, 200.00, '2023-06-08'),
('WAC-001', 6, 'Waca', 'UNISEX', 'M', 'Negro y blanco', 160.00, 200.00, '2023-03-25'),
('WAC-002', 6, 'Waca', 'UNISEX', 'L', 'Negro y blanco', 160.00, 200.00, '2024-08-05'),
('LLA-001', 7, 'Llamero', 'VARON', 'M', 'Verde y dorado', 130.00, 180.00, '2021-12-19'),
('LLA-002', 7, 'Llamero', 'VARON', 'L', 'Verde y dorado', 130.00, 180.00, '2024-05-18'),
('AYA-001', 8, 'Ayarachi', 'UNISEX', 'M', 'Natural', 100.00, 150.00, '2022-12-05'),
('AYA-002', 8, 'Ayarachi', 'UNISEX', 'L', 'Natural', 100.00, 150.00, '2019-09-22'),
('AYA-003', 8, 'Ayarachi', 'UNISEX', 'L', 'Natural', 100.00, 150.00, '2020-12-10'),
('QAJ-001', 9, 'Q''ajelo', 'VARON', 'M', 'Rojo y negro', 100.00, 150.00, '2022-05-17'),
('QAJ-002', 9, 'Q''ajelo', 'VARON', 'L', 'Rojo y negro', 100.00, 150.00, '2022-03-04');

INSERT INTO pieza (id_traje, id_tipo_pieza, descripcion, valor_reposicion) VALUES
(1, 1, 'Máscara de diablo', 600.00),
(1, 6, 'Pechera de diablo', 350.00),
(1, 5, 'Capa de diablo', 400.00),
(1, 3, 'Pantalón de diablo', 150.00),
(1, 7, 'Botas de diablo', 120.00),
(2, 1, 'Máscara de diablo', 600.00),
(2, 6, 'Pechera de diablo', 350.00),
(2, 5, 'Capa de diablo', 400.00),
(2, 3, 'Pantalón de diablo', 150.00),
(2, 7, 'Botas de diablo', 120.00),
(3, 1, 'Máscara de diablo', 600.00),
(3, 6, 'Pechera de diablo', 350.00),
(3, 5, 'Capa de diablo', 400.00),
(3, 3, 'Pantalón de diablo', 150.00),
(3, 7, 'Botas de diablo', 120.00),
(4, 1, 'Máscara de diablo', 600.00),
(4, 6, 'Pechera de diablo', 350.00),
(4, 5, 'Capa de diablo', 400.00),
(4, 3, 'Pantalón de diablo', 150.00),
(4, 7, 'Botas de diablo', 120.00),
(5, 1, 'Máscara de diablo', 600.00),
(5, 6, 'Pechera de diablo', 350.00),
(5, 5, 'Capa de diablo', 400.00),
(5, 3, 'Pantalón de diablo', 150.00),
(5, 7, 'Botas de diablo', 120.00),
(6, 1, 'Máscara de diablo', 600.00),
(6, 6, 'Pechera de diablo', 350.00),
(6, 5, 'Capa de diablo', 400.00),
(6, 3, 'Pantalón de diablo', 150.00),
(6, 7, 'Botas de diablo', 120.00),
(7, 1, 'Máscara de china diabla', 450.00),
(7, 2, 'Chaqueta de china diabla', 250.00),
(7, 4, 'Pollera de china diabla', 200.00),
(7, 7, 'Botas de china diabla', 120.00),
(8, 1, 'Máscara de china diabla', 450.00),
(8, 2, 'Chaqueta de china diabla', 250.00),
(8, 4, 'Pollera de china diabla', 200.00),
(8, 7, 'Botas de china diabla', 120.00),
(9, 1, 'Máscara de china diabla', 450.00),
(9, 2, 'Chaqueta de china diabla', 250.00),
(9, 4, 'Pollera de china diabla', 200.00),
(9, 7, 'Botas de china diabla', 120.00),
(10, 1, 'Máscara de china diabla', 450.00),
(10, 2, 'Chaqueta de china diabla', 250.00),
(10, 4, 'Pollera de china diabla', 200.00),
(10, 7, 'Botas de china diabla', 120.00),
(11, 1, 'Máscara de moreno', 500.00),
(11, 2, 'Chaqueta de moreno', 450.00),
(11, 3, 'Pantalón de moreno', 150.00),
(11, 9, 'Matraca de moreno', 180.00),
(12, 1, 'Máscara de moreno', 500.00),
(12, 2, 'Chaqueta de moreno', 450.00),
(12, 3, 'Pantalón de moreno', 150.00),
(12, 9, 'Matraca de moreno', 180.00),
(13, 1, 'Máscara de moreno', 500.00),
(13, 2, 'Chaqueta de moreno', 450.00),
(13, 3, 'Pantalón de moreno', 150.00),
(13, 9, 'Matraca de moreno', 180.00),
(14, 1, 'Máscara de moreno', 500.00),
(14, 2, 'Chaqueta de moreno', 450.00),
(14, 3, 'Pantalón de moreno', 150.00),
(14, 9, 'Matraca de moreno', 180.00),
(15, 1, 'Máscara de moreno', 500.00),
(15, 2, 'Chaqueta de moreno', 450.00),
(15, 3, 'Pantalón de moreno', 150.00),
(15, 9, 'Matraca de moreno', 180.00),
(16, 4, 'Pollera de cholita morenada', 250.00),
(16, 10, 'Mantilla de cholita morenada', 180.00),
(16, 8, 'Sombrero de cholita morenada', 120.00),
(17, 4, 'Pollera de cholita morenada', 250.00),
(17, 10, 'Mantilla de cholita morenada', 180.00),
(17, 8, 'Sombrero de cholita morenada', 120.00),
(18, 4, 'Pollera de cholita morenada', 250.00),
(18, 10, 'Mantilla de cholita morenada', 180.00),
(18, 8, 'Sombrero de cholita morenada', 120.00),
(19, 4, 'Pollera de cholita morenada', 250.00),
(19, 10, 'Mantilla de cholita morenada', 180.00),
(19, 8, 'Sombrero de cholita morenada', 120.00),
(20, 2, 'Chaqueta de caporal', 220.00),
(20, 3, 'Pantalón de caporal', 120.00),
(20, 7, 'Botas de caporal', 200.00),
(20, 8, 'Sombrero de caporal', 90.00),
(21, 2, 'Chaqueta de caporal', 220.00),
(21, 3, 'Pantalón de caporal', 120.00),
(21, 7, 'Botas de caporal', 200.00),
(21, 8, 'Sombrero de caporal', 90.00),
(22, 2, 'Chaqueta de caporal', 220.00),
(22, 3, 'Pantalón de caporal', 120.00),
(22, 7, 'Botas de caporal', 200.00),
(22, 8, 'Sombrero de caporal', 90.00),
(23, 2, 'Chaqueta de caporal', 220.00),
(23, 3, 'Pantalón de caporal', 120.00),
(23, 7, 'Botas de caporal', 200.00),
(23, 8, 'Sombrero de caporal', 90.00),
(24, 2, 'Chaqueta de caporal', 220.00),
(24, 3, 'Pantalón de caporal', 120.00),
(24, 7, 'Botas de caporal', 200.00),
(24, 8, 'Sombrero de caporal', 90.00),
(25, 2, 'Chaqueta de caporal', 220.00),
(25, 3, 'Pantalón de caporal', 120.00),
(25, 7, 'Botas de caporal', 200.00),
(25, 8, 'Sombrero de caporal', 90.00),
(26, 2, 'Chaqueta de cholita caporal', 180.00),
(26, 4, 'Pollera de cholita caporal', 150.00),
(26, 7, 'Botas de cholita caporal', 160.00),
(26, 8, 'Sombrero de cholita caporal', 90.00),
(27, 2, 'Chaqueta de cholita caporal', 180.00),
(27, 4, 'Pollera de cholita caporal', 150.00),
(27, 7, 'Botas de cholita caporal', 160.00),
(27, 8, 'Sombrero de cholita caporal', 90.00),
(28, 2, 'Chaqueta de cholita caporal', 180.00),
(28, 4, 'Pollera de cholita caporal', 150.00),
(28, 7, 'Botas de cholita caporal', 160.00),
(28, 8, 'Sombrero de cholita caporal', 90.00),
(29, 2, 'Chaqueta de cholita caporal', 180.00),
(29, 4, 'Pollera de cholita caporal', 150.00),
(29, 7, 'Botas de cholita caporal', 160.00),
(29, 8, 'Sombrero de cholita caporal', 90.00),
(30, 2, 'Chaqueta de cholita caporal', 180.00),
(30, 4, 'Pollera de cholita caporal', 150.00),
(30, 7, 'Botas de cholita caporal', 160.00),
(30, 8, 'Sombrero de cholita caporal', 90.00),
(31, 8, 'Sombrero de tinku varón', 80.00),
(31, 2, 'Chaqueta de tinku varón', 120.00),
(31, 3, 'Pantalón de tinku varón', 80.00),
(31, 11, 'Faja de tinku varón', 50.00),
(32, 8, 'Sombrero de tinku varón', 80.00),
(32, 2, 'Chaqueta de tinku varón', 120.00),
(32, 3, 'Pantalón de tinku varón', 80.00),
(32, 11, 'Faja de tinku varón', 50.00),
(33, 8, 'Sombrero de tinku varón', 80.00),
(33, 2, 'Chaqueta de tinku varón', 120.00),
(33, 3, 'Pantalón de tinku varón', 80.00),
(33, 11, 'Faja de tinku varón', 50.00),
(34, 8, 'Sombrero de tinku mujer', 80.00),
(34, 2, 'Chaqueta de tinku mujer', 110.00),
(34, 4, 'Pollera de tinku mujer', 110.00),
(35, 8, 'Sombrero de tinku mujer', 80.00),
(35, 2, 'Chaqueta de tinku mujer', 110.00),
(35, 4, 'Pollera de tinku mujer', 110.00),
(36, 2, 'Chaqueta de kullawa', 160.00),
(36, 3, 'Pantalón de kullawa', 90.00),
(36, 8, 'Sombrero de kullawa', 110.00),
(37, 2, 'Chaqueta de kullawa', 160.00),
(37, 3, 'Pantalón de kullawa', 90.00),
(37, 8, 'Sombrero de kullawa', 110.00),
(38, 4, 'Pollera de cholita kullawada', 160.00),
(38, 10, 'Mantilla de cholita kullawada', 120.00),
(38, 8, 'Sombrero de cholita kullawada', 100.00),
(39, 4, 'Pollera de cholita kullawada', 160.00),
(39, 10, 'Mantilla de cholita kullawada', 120.00),
(39, 8, 'Sombrero de cholita kullawada', 100.00),
(40, 1, 'Máscara de waca', 250.00),
(40, 2, 'Chaqueta de waca', 150.00),
(40, 4, 'Pollera de waca', 120.00),
(41, 1, 'Máscara de waca', 250.00),
(41, 2, 'Chaqueta de waca', 150.00),
(41, 4, 'Pollera de waca', 120.00),
(42, 8, 'Sombrero de llamero', 90.00),
(42, 2, 'Chaqueta de llamero', 130.00),
(42, 3, 'Pantalón de llamero', 80.00),
(43, 8, 'Sombrero de llamero', 90.00),
(43, 2, 'Chaqueta de llamero', 130.00),
(43, 3, 'Pantalón de llamero', 80.00),
(44, 8, 'Sombrero de ayarachi', 200.00),
(44, 12, 'Poncho de ayarachi', 120.00),
(44, 11, 'Faja de ayarachi', 40.00),
(45, 8, 'Sombrero de ayarachi', 200.00),
(45, 12, 'Poncho de ayarachi', 120.00),
(45, 11, 'Faja de ayarachi', 40.00),
(46, 8, 'Sombrero de ayarachi', 200.00),
(46, 12, 'Poncho de ayarachi', 120.00),
(46, 11, 'Faja de ayarachi', 40.00),
(47, 8, 'Sombrero de q''ajelo', 90.00),
(47, 12, 'Poncho de q''ajelo', 140.00),
(47, 3, 'Pantalón de q''ajelo', 70.00),
(48, 8, 'Sombrero de q''ajelo', 90.00),
(48, 12, 'Poncho de q''ajelo', 140.00),
(48, 3, 'Pantalón de q''ajelo', 70.00);

INSERT INTO cliente (tipo_documento, numero_documento, nombres, apellidos, telefono, email, direccion, fecha_registro) VALUES
('DNI', '63287973', 'Luis', 'Condori Mamani', '913461507', NULL, 'Psje. Los Incas 1021', '2024-06-04'),
('DNI', '50327422', 'Roxana', 'Calla Huanca', '937781143', 'roxana345@gmail.com', 'Av. El Sol 1489', '2024-12-03'),
('DNI', '53119375', 'Alex', 'Machaca Mamani', '933268510', NULL, 'Jr. Lampa 1025', '2024-11-07'),
('DNI', '75563440', 'Roxana', 'Quispe Apaza', '979370789', 'roxana142@gmail.com', NULL, '2024-03-13'),
('DNI', '67788893', 'Carlos', 'Sucasaca Ramos', '961430266', NULL, 'Psje. Los Incas 1050', '2024-08-25'),
('DNI', '71386841', 'Rosa', 'Sucasaca Coaquira', '979703978', 'rosa123@gmail.com', 'Psje. Los Incas 1771', '2024-02-28'),
('DNI', '46249150', 'Jorge', 'Choque Quispe', '996661596', NULL, 'Jr. Moquegua 155', '2024-06-28'),
('DNI', '78408566', 'María', 'Chambi Ramos', '939063835', NULL, 'Av. El Sol 1267', '2024-05-19'),
('DNI', '46808579', 'Carlos', 'Ramos Apaza', '922942714', 'carlos176@gmail.com', NULL, '2024-01-25'),
('DNI', '73999142', 'Gladys', 'Huanca Ticona', '988934594', 'gladys299@gmail.com', 'Jr. Moquegua 634', '2024-05-03'),
('DNI', '67818520', 'Víctor', 'Sucasaca Pari', '917692234', NULL, NULL, '2024-02-04'),
('DNI', '61137576', 'Ana', 'Ramos Chambi', '991504394', 'ana732@gmail.com', 'Av. Huancané 747', '2024-10-07'),
('DNI', '48015499', 'Hugo', 'Calla Sucasaca', '977278217', 'hugo946@gmail.com', 'Psje. Los Incas 1772', '2024-06-02'),
('DNI', '58886510', 'Karina', 'Sucasaca Quispe', '948863507', 'karina788@gmail.com', 'Av. Circunvalación 708', '2024-07-11'),
('DNI', '62983701', 'Hugo', 'Quispe Ramos', '917535674', 'hugo116@gmail.com', NULL, '2024-02-04'),
('DNI', '63759265', 'Lucía', 'Apaza Flores', '998019964', NULL, 'Jr. Moquegua 829', '2024-07-13'),
('DNI', '54734777', 'Luis', 'Choque Sucasaca', '934624539', 'luis678@gmail.com', 'Av. Huancané 1920', '2024-05-26'),
('DNI', '41113725', 'María', 'Choque Quispe', '974269259', 'maria982@gmail.com', 'Av. Circunvalación 1819', '2024-03-18'),
('DNI', '62864863', 'Víctor', 'Flores Pari', '952613852', NULL, 'Av. El Sol 1063', '2024-01-08'),
('DNI', '66781424', 'Ana', 'Flores Apaza', '958309143', 'ana239@gmail.com', 'Psje. Los Incas 803', '2024-04-12'),
('DNI', '76829043', 'Rubén', 'Calla Calla', '987924088', NULL, NULL, '2024-07-05'),
('DNI', '75113214', 'Roxana', 'Quispe Chambi', '915034008', 'roxana87@gmail.com', 'Psje. Los Incas 382', '2024-12-24'),
('DNI', '54570849', 'Raúl', 'Quispe Ramos', '988166830', NULL, NULL, '2024-07-13'),
('DNI', '74142404', 'Elena', 'Flores Huanca', NULL, NULL, 'Psje. Los Incas 735', '2024-01-24'),
('DNI', '59657063', 'Hugo', 'Ccama Huanca', '948329736', 'hugo997@gmail.com', 'Jr. Lampa 1835', '2024-10-21'),
('DNI', '76062495', 'Roxana', 'Sucasaca Chambi', '979281900', NULL, 'Jr. Moquegua 435', '2024-01-04'),
('DNI', '44268151', 'Alex', 'Quispe Chambi', '939016711', 'alex614@gmail.com', 'Jr. Moquegua 317', '2024-06-16'),
('DNI', '57620854', 'Yeny', 'Condori Machaca', '996912096', NULL, 'Jr. San Martín 347', '2024-09-17'),
('DNI', '79279836', 'Jorge', 'Chambi Calla', '919265701', 'jorge90@gmail.com', 'Psje. Los Incas 643', '2024-11-08'),
('DNI', '73975345', 'María', 'Yupanqui Sucasaca', '953311204', 'maria742@gmail.com', 'Jr. San Martín 925', '2024-06-06'),
('DNI', '63390564', 'Jorge', 'Calla Choque', '956990221', 'jorge823@gmail.com', 'Av. El Sol 416', '2024-08-24'),
('DNI', '57960278', 'Sonia', 'Apaza Sucasaca', '925817033', NULL, 'Jr. San Martín 639', '2024-03-18'),
('DNI', '62264817', 'Miguel', 'Pari Choque', '985336401', 'miguel275@gmail.com', 'Jr. San Martín 1396', '2024-07-13'),
('DNI', '67491830', 'Lucía', 'Ramos Yupanqui', '976395419', 'lucia868@gmail.com', 'Av. El Sol 969', '2024-06-21'),
('DNI', '51607785', 'Rubén', 'Mamani Machaca', NULL, 'ruben259@gmail.com', NULL, '2024-10-21'),
('DNI', '72408059', 'Sonia', 'Sucasaca Pacho', '915032570', 'sonia652@gmail.com', NULL, '2024-05-11'),
('DNI', '74557142', 'Víctor', 'Ccama Calla', '994579966', 'victor570@gmail.com', 'Jr. Lampa 295', '2024-03-11'),
('DNI', '51460694', 'Carmen', 'Calla Ccama', '921312162', 'carmen729@gmail.com', 'Jr. Moquegua 321', '2024-01-09'),
('DNI', '76925467', 'Luis', 'Mamani Huanca', '941051805', 'luis933@gmail.com', 'Jr. Tacna 803', '2024-06-11'),
('DNI', '72770337', 'Sonia', 'Pacho Pari', '943568149', NULL, 'Jr. Lampa 1084', '2024-06-09'),
('RUC', '20601548723', 'Asociación Cultural Sentimiento Altiplánico', NULL, '972561166', NULL, 'Av. El Sol 852', '2024-03-17'),
('RUC', '20448871204', 'Institución Educativa Señor de los Milagros', NULL, '963677153', NULL, 'Psje. Los Incas 547', '2024-05-02'),
('RUC', '20553108841', 'Club Departamental Andino', NULL, '965144515', NULL, 'Av. Circunvalación 1586', '2024-06-21');

INSERT INTO conjunto (nombre, id_danza, id_delegado, telefono) VALUES
('Diablada Hijos del Altiplano', 1, 1, '998739352'),
('Diablada Juventud Ferroviaria', 1, 4, '978311606'),
('Morenada Señorial Bella Vista', 2, 6, '981440325'),
('Morenada Unión Juventud', 2, 9, '950078202'),
('Caporales Corazón Andino', 3, 12, '974773101'),
('Caporales Estrella del Sur', 3, 15, '992733156'),
('Tinkus Fuerza Andina', 4, 18, '922176004'),
('Kullawada Hilanderos del Lago', 5, 21, '978401935'),
('Llamerada Tradicional Pastores', 7, 24, '980121102'),
('Agrupación Autóctona Ayarachis', 8, 27, '989971027');

INSERT INTO alquiler (id_cliente, id_conjunto, fecha_registro, fecha_salida, fecha_devolucion_prevista, fecha_devolucion_real, estado, observacion) VALUES
(15, 6, '2025-01-17', '2025-02-09', '2025-02-17', NULL, 'ANULADO', 'Anulado por el cliente antes de la entrega'),
(21, 8, '2024-12-10', '2025-02-07', '2025-02-18', '2025-02-18', 'DEVUELTO', NULL),
(27, 10, '2025-01-22', '2025-02-09', '2025-02-17', '2025-02-21', 'DEVUELTO', 'Devuelto con 4 día(s) de retraso'),
(24, 9, '2024-12-30', '2025-02-07', '2025-02-18', '2025-02-20', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(6, 3, '2024-12-18', '2025-02-09', '2025-02-17', '2025-02-17', 'DEVUELTO', NULL),
(18, 7, '2025-01-18', '2025-02-07', '2025-02-17', '2025-02-19', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(4, 2, '2025-01-23', '2025-02-09', '2025-02-19', '2025-02-19', 'DEVUELTO', NULL),
(1, 1, '2025-01-30', '2025-02-09', '2025-02-17', '2025-02-17', 'DEVUELTO', NULL),
(43, NULL, '2024-12-12', '2025-02-08', '2025-02-19', '2025-02-21', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(6, NULL, '2025-01-26', '2025-02-07', '2025-02-18', '2025-02-18', 'DEVUELTO', NULL),
(2, NULL, '2025-01-03', '2025-02-07', '2025-02-17', '2025-02-18', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(16, NULL, '2024-12-17', '2025-02-09', '2025-02-17', '2025-02-17', 'DEVUELTO', NULL),
(21, 8, '2025-05-13', '2025-06-22', '2025-06-24', '2025-06-24', 'DEVUELTO', NULL),
(9, 4, '2025-05-19', '2025-06-21', '2025-06-25', '2025-06-26', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(6, NULL, '2025-06-08', '2025-06-20', '2025-06-26', '2025-06-30', 'DEVUELTO', 'Devuelto con 4 día(s) de retraso'),
(30, NULL, '2025-05-31', '2025-06-22', '2025-06-26', '2025-06-26', 'DEVUELTO', NULL),
(35, NULL, '2025-05-06', '2025-06-20', '2025-06-25', '2025-06-26', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(24, 9, '2025-06-30', '2025-07-25', '2025-07-31', NULL, 'ANULADO', 'Anulado por el cliente antes de la entrega'),
(15, 6, '2025-07-06', '2025-07-26', '2025-07-31', '2025-08-01', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(23, NULL, '2025-05-29', '2025-07-25', '2025-07-30', '2025-07-31', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(12, NULL, '2025-07-09', '2025-07-27', '2025-07-29', NULL, 'ANULADO', 'Anulado por el cliente antes de la entrega'),
(43, NULL, '2025-06-13', '2025-07-25', '2025-07-31', '2025-07-31', 'DEVUELTO', NULL),
(20, NULL, '2025-06-02', '2025-07-25', '2025-07-29', '2025-07-30', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(6, 3, '2025-08-21', '2025-10-12', '2025-10-13', '2025-10-13', 'DEVUELTO', NULL),
(21, 8, '2025-09-04', '2025-10-12', '2025-10-13', '2025-10-17', 'DEVUELTO', 'Devuelto con 4 día(s) de retraso'),
(7, NULL, '2025-09-18', '2025-10-11', '2025-10-14', '2025-10-16', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(7, NULL, '2025-09-08', '2025-10-12', '2025-10-15', '2025-10-15', 'DEVUELTO', NULL),
(27, 10, '2026-01-01', '2026-01-30', '2026-02-11', '2026-02-11', 'DEVUELTO', NULL),
(24, 9, '2025-12-27', '2026-02-01', '2026-02-11', '2026-02-13', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(12, 5, '2025-12-26', '2026-02-01', '2026-02-13', '2026-02-13', 'DEVUELTO', NULL),
(21, 8, '2026-01-20', '2026-01-31', '2026-02-12', '2026-02-12', 'DEVUELTO', NULL),
(18, 7, '2025-12-10', '2026-01-31', '2026-02-11', '2026-02-13', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(9, 4, '2026-01-13', '2026-02-01', '2026-02-11', '2026-02-15', 'DEVUELTO', 'Devuelto con 4 día(s) de retraso'),
(6, 3, '2025-12-21', '2026-01-30', '2026-02-11', '2026-02-11', 'DEVUELTO', NULL),
(4, 2, '2026-01-16', '2026-01-31', '2026-02-11', '2026-02-12', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(15, 6, '2025-12-20', '2026-02-01', '2026-02-13', '2026-02-13', 'DEVUELTO', NULL),
(1, NULL, '2025-12-14', '2026-01-31', '2026-02-11', '2026-02-13', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(25, NULL, '2025-12-02', '2026-01-31', '2026-02-12', '2026-02-12', 'DEVUELTO', NULL),
(15, 6, '2026-07-11', '2026-07-25', '2026-07-29', '2026-07-29', 'DEVUELTO', NULL),
(18, 7, '2026-05-28', '2026-07-24', '2026-07-28', '2026-07-28', 'DEVUELTO', NULL),
(27, NULL, '2026-07-07', '2026-07-26', '2026-07-29', '2026-07-30', 'DEVUELTO', 'Devuelto con 1 día(s) de retraso'),
(29, NULL, '2026-06-28', '2026-07-26', '2026-07-29', '2026-07-29', 'DEVUELTO', NULL),
(14, NULL, '2026-07-05', '2026-07-25', '2026-07-30', '2026-08-01', 'DEVUELTO', 'Devuelto con 2 día(s) de retraso'),
(26, NULL, '2026-06-22', '2026-07-25', '2026-07-29', '2026-07-29', 'DEVUELTO', NULL),
(4, 2, '2026-09-10', '2026-10-19', '2026-10-25', NULL, 'ENTREGADO', NULL),
(12, 5, '2026-09-13', '2026-10-17', '2026-10-23', NULL, 'ENTREGADO', NULL),
(13, NULL, '2026-09-28', '2026-10-19', '2026-10-24', NULL, 'ENTREGADO', NULL),
(40, NULL, '2026-09-03', '2026-10-17', '2026-10-25', NULL, 'ENTREGADO', NULL),
(31, NULL, '2026-09-29', '2026-10-18', '2026-10-23', NULL, 'ENTREGADO', NULL),
(18, 7, '2026-10-17', '2027-01-31', '2027-02-11', NULL, 'RESERVADO', NULL),
(4, 2, '2026-10-21', '2027-01-29', '2027-02-10', NULL, 'RESERVADO', NULL),
(9, 4, '2026-10-05', '2027-01-30', '2027-02-10', NULL, 'RESERVADO', NULL),
(6, 3, '2026-10-06', '2027-01-31', '2027-02-11', NULL, 'RESERVADO', NULL),
(1, 1, '2026-10-04', '2027-01-30', '2027-02-10', NULL, 'RESERVADO', NULL),
(27, 10, '2026-10-02', '2027-01-31', '2027-02-12', NULL, 'RESERVADO', NULL),
(26, NULL, '2026-10-13', '2027-01-30', '2027-02-10', NULL, 'RESERVADO', NULL),
(8, NULL, '2026-10-05', '2027-01-29', '2027-02-11', NULL, 'RESERVADO', NULL),
(13, NULL, '2026-10-12', '2027-01-30', '2027-02-10', NULL, 'RESERVADO', NULL);

-- Los montos de cada alquiler se calculan solos con el trigger trg_actualizar_totales

INSERT INTO detalle_alquiler (id_alquiler, id_traje, precio, garantia) VALUES
(1, 28, 144.00, 220.00),
(1, 30, 144.00, 220.00),
(1, 26, 144.00, 220.00),
(1, 21, 162.00, 250.00),
(1, 24, 162.00, 250.00),
(2, 37, 150.00, 200.00),
(2, 38, 150.00, 200.00),
(2, 36, 150.00, 200.00),
(2, 39, 150.00, 200.00),
(3, 46, 100.00, 150.00),
(3, 44, 100.00, 150.00),
(3, 45, 100.00, 150.00),
(4, 43, 130.00, 180.00),
(4, 42, 130.00, 180.00),
(5, 14, 270.00, 450.00),
(5, 16, 180.00, 300.00),
(5, 15, 270.00, 450.00),
(5, 11, 270.00, 450.00),
(5, 19, 180.00, 300.00),
(6, 32, 108.00, 150.00),
(6, 31, 108.00, 150.00),
(6, 35, 108.00, 150.00),
(6, 33, 108.00, 150.00),
(6, 34, 108.00, 150.00),
(7, 9, 225.00, 350.00),
(7, 8, 225.00, 350.00),
(7, 2, 315.00, 500.00),
(7, 10, 225.00, 350.00),
(7, 6, 315.00, 500.00),
(7, 7, 225.00, 350.00),
(8, 4, 350.00, 500.00),
(8, 5, 350.00, 500.00),
(8, 3, 350.00, 500.00),
(8, 1, 350.00, 500.00),
(9, 41, 160.00, 200.00),
(10, 29, 160.00, 220.00),
(11, 21, 180.00, 250.00),
(12, 26, 160.00, 220.00),
(12, 28, 160.00, 220.00),
(13, 37, 150.00, 200.00),
(13, 36, 150.00, 200.00),
(13, 39, 150.00, 200.00),
(13, 38, 150.00, 200.00),
(14, 11, 270.00, 450.00),
(14, 16, 180.00, 300.00),
(14, 15, 270.00, 450.00),
(14, 17, 180.00, 300.00),
(14, 12, 270.00, 450.00),
(14, 19, 180.00, 300.00),
(14, 13, 270.00, 450.00),
(15, 24, 180.00, 250.00),
(15, 22, 180.00, 250.00),
(16, 9, 250.00, 350.00),
(16, 1, 350.00, 500.00),
(17, 23, 180.00, 250.00),
(18, 43, 130.00, 180.00),
(18, 42, 130.00, 180.00),
(19, 27, 160.00, 220.00),
(19, 29, 160.00, 220.00),
(19, 22, 180.00, 250.00),
(20, 2, 350.00, 500.00),
(21, 41, 160.00, 200.00),
(21, 40, 160.00, 200.00),
(22, 35, 120.00, 150.00),
(22, 33, 120.00, 150.00),
(23, 34, 120.00, 150.00),
(24, 18, 200.00, 300.00),
(24, 13, 300.00, 450.00),
(24, 15, 300.00, 450.00),
(24, 17, 200.00, 300.00),
(25, 36, 150.00, 200.00),
(25, 39, 150.00, 200.00),
(25, 38, 150.00, 200.00),
(25, 37, 150.00, 200.00),
(26, 47, 100.00, 150.00),
(27, 23, 180.00, 250.00),
(28, 45, 100.00, 150.00),
(28, 44, 100.00, 150.00),
(28, 46, 100.00, 150.00),
(29, 43, 130.00, 180.00),
(29, 42, 130.00, 180.00),
(30, 25, 162.00, 250.00),
(30, 30, 144.00, 220.00),
(30, 26, 144.00, 220.00),
(30, 23, 162.00, 250.00),
(30, 20, 162.00, 250.00),
(30, 27, 144.00, 220.00),
(30, 24, 162.00, 250.00),
(31, 39, 150.00, 200.00),
(31, 38, 150.00, 200.00),
(31, 36, 150.00, 200.00),
(31, 37, 150.00, 200.00),
(32, 33, 108.00, 150.00),
(32, 31, 108.00, 150.00),
(32, 34, 108.00, 150.00),
(32, 35, 108.00, 150.00),
(32, 32, 108.00, 150.00),
(33, 12, 300.00, 450.00),
(33, 16, 200.00, 300.00),
(33, 19, 200.00, 300.00),
(34, 15, 270.00, 450.00),
(34, 11, 270.00, 450.00),
(34, 18, 180.00, 300.00),
(34, 13, 270.00, 450.00),
(34, 17, 180.00, 300.00),
(34, 14, 270.00, 450.00),
(35, 6, 350.00, 500.00),
(35, 2, 350.00, 500.00),
(35, 7, 250.00, 350.00),
(35, 10, 250.00, 350.00),
(36, 29, 160.00, 220.00),
(36, 22, 180.00, 250.00),
(36, 21, 180.00, 250.00),
(36, 28, 160.00, 220.00),
(37, 47, 100.00, 150.00),
(38, 9, 250.00, 350.00),
(38, 4, 350.00, 500.00),
(39, 24, 162.00, 250.00),
(39, 20, 162.00, 250.00),
(39, 23, 162.00, 250.00),
(39, 28, 144.00, 220.00),
(39, 29, 144.00, 220.00),
(40, 31, 120.00, 150.00),
(40, 35, 120.00, 150.00),
(40, 33, 120.00, 150.00),
(41, 15, 300.00, 450.00),
(42, 47, 100.00, 150.00),
(42, 48, 100.00, 150.00),
(43, 38, 150.00, 200.00),
(44, 2, 350.00, 500.00),
(44, 1, 350.00, 500.00),
(45, 9, 250.00, 350.00),
(45, 2, 350.00, 500.00),
(45, 4, 350.00, 500.00),
(46, 23, 180.00, 250.00),
(46, 28, 160.00, 220.00),
(46, 24, 180.00, 250.00),
(46, 21, 180.00, 250.00),
(47, 30, 160.00, 220.00),
(48, 38, 150.00, 200.00),
(49, 42, 130.00, 180.00),
(49, 43, 130.00, 180.00),
(50, 34, 120.00, 150.00),
(50, 33, 120.00, 150.00),
(50, 32, 120.00, 150.00),
(51, 7, 225.00, 350.00),
(51, 2, 315.00, 500.00),
(51, 4, 315.00, 500.00),
(51, 6, 315.00, 500.00),
(51, 9, 225.00, 350.00),
(52, 16, 200.00, 300.00),
(52, 12, 300.00, 450.00),
(52, 13, 300.00, 450.00),
(53, 19, 180.00, 300.00),
(53, 18, 180.00, 300.00),
(53, 14, 270.00, 450.00),
(53, 11, 270.00, 450.00),
(53, 17, 180.00, 300.00),
(53, 15, 270.00, 450.00),
(54, 5, 315.00, 500.00),
(54, 3, 315.00, 500.00),
(54, 10, 225.00, 350.00),
(54, 8, 225.00, 350.00),
(54, 1, 315.00, 500.00),
(55, 45, 100.00, 150.00),
(55, 46, 100.00, 150.00),
(55, 44, 100.00, 150.00),
(56, 47, 100.00, 150.00),
(56, 48, 100.00, 150.00),
(57, 40, 160.00, 200.00),
(58, 35, 120.00, 150.00);

INSERT INTO danio (id_detalle, id_pieza, tipo, descripcion, monto_cobro) VALUES
(6, 146, 'GRAVE', 'Sombrero rota, requiere reparación', 38.50),
(11, 167, 'GRAVE', 'Faja rota, requiere reparación', 14.00),
(13, 162, 'GRAVE', 'Sombrero rota, requiere reparación', 31.50),
(16, 69, 'LEVE', 'Sombrero con bordado descosido', 12.00),
(17, 63, 'LEVE', 'Máscara con bordado descosido', 50.00),
(22, 139, 'GRAVE', 'Chaqueta rota, requiere reparación', 38.50),
(27, 7, 'LEVE', 'Pechera con bordado descosido', 35.00),
(34, 2, 'LEVE', 'Pechera con bordado descosido', 35.00),
(35, 157, 'LEVE', 'Chaqueta con bordado descosido', 15.00),
(42, 151, 'GRAVE', 'Mantilla rota, requiere reparación', 42.00),
(44, 48, 'LEVE', 'Chaqueta con bordado descosido', 45.00),
(53, 39, 'LEVE', 'Máscara con bordado descosido', 45.00),
(54, 2, 'LEVE', 'Pechera con bordado descosido', 35.00),
(66, 137, 'LEVE', 'Pollera con bordado descosido', 11.00),
(67, 75, 'LEVE', 'Sombrero con bordado descosido', 12.00),
(68, 57, 'LEVE', 'Pantalón con bordado descosido', 15.00),
(78, 166, 'GRAVE', 'Poncho rota, requiere reparación', 42.00),
(88, 96, 'LEVE', 'Pantalón con bordado descosido', 12.00),
(89, 151, 'LEVE', 'Mantilla con bordado descosido', 12.00),
(104, 56, 'LEVE', 'Chaqueta con bordado descosido', 45.00),
(106, 60, 'GRAVE', 'Chaqueta rota, requiere reparación', 157.50),
(116, 42, 'LEVE', 'Botas con bordado descosido', 12.00),
(122, 115, 'LEVE', 'Chaqueta con bordado descosido', 18.00),
(123, 123, 'GRAVE', 'Sombrero rota, requiere reparación', 28.00),
(126, 66, 'GRAVE', 'Matraca rota, requiere reparación', 63.00);

INSERT INTO pago (id_alquiler, fecha_pago, concepto, monto, metodo) VALUES
(1, '2025-01-17', 'ADELANTO', 378.00, 'EFECTIVO'),
(2, '2024-12-10', 'ADELANTO', 300.00, 'PLIN'),
(2, '2025-02-07', 'SALDO', 300.00, 'TRANSFERENCIA'),
(2, '2025-02-07', 'GARANTIA', 800.00, 'EFECTIVO'),
(2, '2025-02-18', 'DEVOLUCION_GARANTIA', 761.50, 'EFECTIVO'),
(3, '2025-01-22', 'ADELANTO', 150.00, 'YAPE'),
(3, '2025-02-09', 'SALDO', 150.00, 'EFECTIVO'),
(3, '2025-02-09', 'GARANTIA', 450.00, 'EFECTIVO'),
(3, '2025-02-21', 'DEVOLUCION_GARANTIA', 436.00, 'EFECTIVO'),
(4, '2024-12-30', 'ADELANTO', 130.00, 'PLIN'),
(4, '2025-02-07', 'SALDO', 130.00, 'YAPE'),
(4, '2025-02-07', 'GARANTIA', 360.00, 'EFECTIVO'),
(4, '2025-02-20', 'DEVOLUCION_GARANTIA', 328.50, 'EFECTIVO'),
(5, '2024-12-18', 'ADELANTO', 585.00, 'YAPE'),
(5, '2025-02-09', 'SALDO', 585.00, 'YAPE'),
(5, '2025-02-09', 'GARANTIA', 1950.00, 'EFECTIVO'),
(5, '2025-02-17', 'DEVOLUCION_GARANTIA', 1888.00, 'EFECTIVO'),
(6, '2025-01-18', 'ADELANTO', 270.00, 'EFECTIVO'),
(6, '2025-02-07', 'SALDO', 270.00, 'YAPE'),
(6, '2025-02-07', 'GARANTIA', 750.00, 'EFECTIVO'),
(6, '2025-02-19', 'DEVOLUCION_GARANTIA', 711.50, 'EFECTIVO'),
(7, '2025-01-23', 'ADELANTO', 765.00, 'YAPE'),
(7, '2025-02-09', 'SALDO', 765.00, 'EFECTIVO'),
(7, '2025-02-09', 'GARANTIA', 2400.00, 'EFECTIVO'),
(7, '2025-02-19', 'DEVOLUCION_GARANTIA', 2365.00, 'EFECTIVO'),
(8, '2025-01-30', 'ADELANTO', 700.00, 'YAPE'),
(8, '2025-02-09', 'SALDO', 700.00, 'YAPE'),
(8, '2025-02-09', 'GARANTIA', 2000.00, 'EFECTIVO'),
(8, '2025-02-17', 'DEVOLUCION_GARANTIA', 1965.00, 'EFECTIVO'),
(9, '2024-12-12', 'ADELANTO', 80.00, 'PLIN'),
(9, '2025-02-08', 'SALDO', 80.00, 'PLIN'),
(9, '2025-02-08', 'GARANTIA', 200.00, 'EFECTIVO'),
(9, '2025-02-21', 'DEVOLUCION_GARANTIA', 185.00, 'EFECTIVO'),
(10, '2025-01-26', 'ADELANTO', 80.00, 'YAPE'),
(10, '2025-02-07', 'SALDO', 80.00, 'TRANSFERENCIA'),
(10, '2025-02-07', 'GARANTIA', 220.00, 'EFECTIVO'),
(10, '2025-02-18', 'DEVOLUCION_GARANTIA', 220.00, 'EFECTIVO'),
(11, '2025-01-03', 'ADELANTO', 90.00, 'EFECTIVO'),
(11, '2025-02-07', 'SALDO', 90.00, 'EFECTIVO'),
(11, '2025-02-07', 'GARANTIA', 250.00, 'EFECTIVO'),
(11, '2025-02-18', 'DEVOLUCION_GARANTIA', 250.00, 'EFECTIVO'),
(12, '2024-12-17', 'ADELANTO', 160.00, 'YAPE'),
(12, '2025-02-09', 'SALDO', 160.00, 'EFECTIVO'),
(12, '2025-02-09', 'GARANTIA', 440.00, 'EFECTIVO'),
(12, '2025-02-17', 'DEVOLUCION_GARANTIA', 440.00, 'EFECTIVO'),
(13, '2025-05-13', 'ADELANTO', 300.00, 'PLIN'),
(13, '2025-06-22', 'SALDO', 300.00, 'YAPE'),
(13, '2025-06-22', 'GARANTIA', 800.00, 'EFECTIVO'),
(13, '2025-06-24', 'DEVOLUCION_GARANTIA', 758.00, 'EFECTIVO'),
(14, '2025-05-19', 'ADELANTO', 810.00, 'EFECTIVO'),
(14, '2025-06-21', 'SALDO', 810.00, 'YAPE'),
(14, '2025-06-21', 'GARANTIA', 2700.00, 'EFECTIVO'),
(14, '2025-06-26', 'DEVOLUCION_GARANTIA', 2655.00, 'EFECTIVO'),
(15, '2025-06-08', 'ADELANTO', 180.00, 'EFECTIVO'),
(15, '2025-06-20', 'SALDO', 180.00, 'YAPE'),
(15, '2025-06-20', 'GARANTIA', 500.00, 'EFECTIVO'),
(15, '2025-06-30', 'DEVOLUCION_GARANTIA', 500.00, 'EFECTIVO'),
(16, '2025-05-31', 'ADELANTO', 300.00, 'PLIN'),
(16, '2025-06-22', 'SALDO', 300.00, 'TRANSFERENCIA'),
(16, '2025-06-22', 'GARANTIA', 850.00, 'EFECTIVO'),
(16, '2025-06-26', 'DEVOLUCION_GARANTIA', 770.00, 'EFECTIVO'),
(17, '2025-05-06', 'ADELANTO', 90.00, 'YAPE'),
(17, '2025-06-20', 'SALDO', 90.00, 'EFECTIVO'),
(17, '2025-06-20', 'GARANTIA', 250.00, 'EFECTIVO'),
(17, '2025-06-26', 'DEVOLUCION_GARANTIA', 250.00, 'EFECTIVO'),
(18, '2025-06-30', 'ADELANTO', 130.00, 'YAPE'),
(19, '2025-07-06', 'ADELANTO', 250.00, 'TRANSFERENCIA'),
(19, '2025-07-26', 'SALDO', 250.00, 'TRANSFERENCIA'),
(19, '2025-07-26', 'GARANTIA', 690.00, 'EFECTIVO'),
(19, '2025-08-01', 'DEVOLUCION_GARANTIA', 690.00, 'EFECTIVO'),
(20, '2025-05-29', 'ADELANTO', 175.00, 'EFECTIVO'),
(20, '2025-07-25', 'SALDO', 175.00, 'YAPE'),
(20, '2025-07-25', 'GARANTIA', 500.00, 'EFECTIVO'),
(20, '2025-07-31', 'DEVOLUCION_GARANTIA', 500.00, 'EFECTIVO'),
(21, '2025-07-09', 'ADELANTO', 160.00, 'YAPE'),
(22, '2025-06-13', 'ADELANTO', 120.00, 'TRANSFERENCIA'),
(22, '2025-07-25', 'SALDO', 120.00, 'YAPE'),
(22, '2025-07-25', 'GARANTIA', 300.00, 'EFECTIVO'),
(22, '2025-07-31', 'DEVOLUCION_GARANTIA', 300.00, 'EFECTIVO'),
(23, '2025-06-02', 'ADELANTO', 60.00, 'EFECTIVO'),
(23, '2025-07-25', 'SALDO', 60.00, 'YAPE'),
(23, '2025-07-25', 'GARANTIA', 150.00, 'EFECTIVO'),
(23, '2025-07-30', 'DEVOLUCION_GARANTIA', 139.00, 'EFECTIVO'),
(24, '2025-08-21', 'ADELANTO', 500.00, 'YAPE'),
(24, '2025-10-12', 'SALDO', 500.00, 'TRANSFERENCIA'),
(24, '2025-10-12', 'GARANTIA', 1500.00, 'EFECTIVO'),
(24, '2025-10-13', 'DEVOLUCION_GARANTIA', 1473.00, 'EFECTIVO'),
(25, '2025-09-04', 'ADELANTO', 300.00, 'YAPE'),
(25, '2025-10-12', 'SALDO', 300.00, 'YAPE'),
(25, '2025-10-12', 'GARANTIA', 800.00, 'EFECTIVO'),
(25, '2025-10-17', 'DEVOLUCION_GARANTIA', 800.00, 'EFECTIVO'),
(26, '2025-09-18', 'ADELANTO', 50.00, 'YAPE'),
(26, '2025-10-11', 'SALDO', 50.00, 'EFECTIVO'),
(26, '2025-10-11', 'GARANTIA', 150.00, 'EFECTIVO'),
(26, '2025-10-16', 'DEVOLUCION_GARANTIA', 150.00, 'EFECTIVO'),
(27, '2025-09-08', 'ADELANTO', 90.00, 'YAPE'),
(27, '2025-10-12', 'SALDO', 90.00, 'EFECTIVO'),
(27, '2025-10-12', 'GARANTIA', 250.00, 'EFECTIVO'),
(27, '2025-10-15', 'DEVOLUCION_GARANTIA', 250.00, 'EFECTIVO'),
(28, '2026-01-01', 'ADELANTO', 150.00, 'PLIN'),
(28, '2026-01-30', 'SALDO', 150.00, 'YAPE'),
(28, '2026-01-30', 'GARANTIA', 450.00, 'EFECTIVO'),
(28, '2026-02-11', 'DEVOLUCION_GARANTIA', 408.00, 'EFECTIVO'),
(29, '2025-12-27', 'ADELANTO', 130.00, 'PLIN'),
(29, '2026-02-01', 'SALDO', 130.00, 'YAPE'),
(29, '2026-02-01', 'GARANTIA', 360.00, 'EFECTIVO'),
(29, '2026-02-13', 'DEVOLUCION_GARANTIA', 360.00, 'EFECTIVO'),
(30, '2025-12-26', 'ADELANTO', 540.00, 'YAPE'),
(30, '2026-02-01', 'SALDO', 540.00, 'PLIN'),
(30, '2026-02-01', 'GARANTIA', 1660.00, 'EFECTIVO'),
(30, '2026-02-13', 'DEVOLUCION_GARANTIA', 1648.00, 'EFECTIVO'),
(31, '2026-01-20', 'ADELANTO', 300.00, 'TRANSFERENCIA'),
(31, '2026-01-31', 'SALDO', 300.00, 'EFECTIVO'),
(31, '2026-01-31', 'GARANTIA', 800.00, 'EFECTIVO'),
(31, '2026-02-12', 'DEVOLUCION_GARANTIA', 788.00, 'EFECTIVO'),
(32, '2025-12-10', 'ADELANTO', 270.00, 'YAPE'),
(32, '2026-01-31', 'SALDO', 270.00, 'YAPE'),
(32, '2026-01-31', 'GARANTIA', 750.00, 'EFECTIVO'),
(32, '2026-02-13', 'DEVOLUCION_GARANTIA', 750.00, 'EFECTIVO'),
(33, '2026-01-13', 'ADELANTO', 350.00, 'YAPE'),
(33, '2026-02-01', 'SALDO', 350.00, 'EFECTIVO'),
(33, '2026-02-01', 'GARANTIA', 1050.00, 'EFECTIVO'),
(33, '2026-02-15', 'DEVOLUCION_GARANTIA', 1050.00, 'EFECTIVO'),
(34, '2025-12-21', 'ADELANTO', 720.00, 'YAPE'),
(34, '2026-01-30', 'SALDO', 720.00, 'YAPE'),
(34, '2026-01-30', 'GARANTIA', 2400.00, 'EFECTIVO'),
(34, '2026-02-11', 'DEVOLUCION_GARANTIA', 2197.50, 'EFECTIVO'),
(35, '2026-01-16', 'ADELANTO', 600.00, 'EFECTIVO'),
(35, '2026-01-31', 'SALDO', 600.00, 'TRANSFERENCIA'),
(35, '2026-01-31', 'GARANTIA', 1700.00, 'EFECTIVO'),
(35, '2026-02-12', 'DEVOLUCION_GARANTIA', 1700.00, 'EFECTIVO'),
(36, '2025-12-20', 'ADELANTO', 340.00, 'TRANSFERENCIA'),
(36, '2026-02-01', 'SALDO', 340.00, 'PLIN'),
(36, '2026-02-01', 'GARANTIA', 940.00, 'EFECTIVO'),
(36, '2026-02-13', 'DEVOLUCION_GARANTIA', 940.00, 'EFECTIVO'),
(37, '2025-12-14', 'ADELANTO', 50.00, 'TRANSFERENCIA'),
(37, '2026-01-31', 'SALDO', 50.00, 'EFECTIVO'),
(37, '2026-01-31', 'GARANTIA', 150.00, 'EFECTIVO'),
(37, '2026-02-13', 'DEVOLUCION_GARANTIA', 150.00, 'EFECTIVO'),
(38, '2025-12-02', 'ADELANTO', 300.00, 'TRANSFERENCIA'),
(38, '2026-01-31', 'SALDO', 300.00, 'YAPE'),
(38, '2026-01-31', 'GARANTIA', 850.00, 'EFECTIVO'),
(38, '2026-02-12', 'DEVOLUCION_GARANTIA', 838.00, 'EFECTIVO'),
(39, '2026-07-11', 'ADELANTO', 387.00, 'EFECTIVO'),
(39, '2026-07-25', 'SALDO', 387.00, 'YAPE'),
(39, '2026-07-25', 'GARANTIA', 1190.00, 'EFECTIVO'),
(39, '2026-07-29', 'DEVOLUCION_GARANTIA', 1172.00, 'EFECTIVO'),
(40, '2026-05-28', 'ADELANTO', 180.00, 'YAPE'),
(40, '2026-07-24', 'SALDO', 180.00, 'PLIN'),
(40, '2026-07-24', 'GARANTIA', 450.00, 'EFECTIVO'),
(40, '2026-07-28', 'DEVOLUCION_GARANTIA', 422.00, 'EFECTIVO'),
(41, '2026-07-07', 'ADELANTO', 150.00, 'EFECTIVO'),
(41, '2026-07-26', 'SALDO', 150.00, 'YAPE'),
(41, '2026-07-26', 'GARANTIA', 450.00, 'EFECTIVO'),
(41, '2026-07-30', 'DEVOLUCION_GARANTIA', 387.00, 'EFECTIVO'),
(42, '2026-06-28', 'ADELANTO', 100.00, 'YAPE'),
(42, '2026-07-26', 'SALDO', 100.00, 'TRANSFERENCIA'),
(42, '2026-07-26', 'GARANTIA', 300.00, 'EFECTIVO'),
(42, '2026-07-29', 'DEVOLUCION_GARANTIA', 300.00, 'EFECTIVO'),
(43, '2026-07-05', 'ADELANTO', 75.00, 'EFECTIVO'),
(43, '2026-07-25', 'SALDO', 75.00, 'EFECTIVO'),
(43, '2026-07-25', 'GARANTIA', 200.00, 'EFECTIVO'),
(43, '2026-08-01', 'DEVOLUCION_GARANTIA', 200.00, 'EFECTIVO'),
(44, '2026-06-22', 'ADELANTO', 350.00, 'YAPE'),
(44, '2026-07-25', 'SALDO', 350.00, 'YAPE'),
(44, '2026-07-25', 'GARANTIA', 1000.00, 'EFECTIVO'),
(44, '2026-07-29', 'DEVOLUCION_GARANTIA', 1000.00, 'EFECTIVO'),
(45, '2026-09-10', 'ADELANTO', 475.00, 'PLIN'),
(45, '2026-10-19', 'SALDO', 475.00, 'YAPE'),
(45, '2026-10-19', 'GARANTIA', 1350.00, 'EFECTIVO'),
(46, '2026-09-13', 'ADELANTO', 350.00, 'YAPE'),
(46, '2026-10-17', 'SALDO', 350.00, 'PLIN'),
(46, '2026-10-17', 'GARANTIA', 970.00, 'EFECTIVO'),
(47, '2026-09-28', 'ADELANTO', 80.00, 'YAPE'),
(47, '2026-10-19', 'SALDO', 80.00, 'YAPE'),
(47, '2026-10-19', 'GARANTIA', 220.00, 'EFECTIVO'),
(48, '2026-09-03', 'ADELANTO', 75.00, 'EFECTIVO'),
(48, '2026-10-17', 'SALDO', 75.00, 'PLIN'),
(48, '2026-10-17', 'GARANTIA', 200.00, 'EFECTIVO'),
(49, '2026-09-29', 'ADELANTO', 130.00, 'YAPE'),
(49, '2026-10-18', 'SALDO', 130.00, 'YAPE'),
(49, '2026-10-18', 'GARANTIA', 360.00, 'EFECTIVO'),
(50, '2026-10-17', 'ADELANTO', 180.00, 'TRANSFERENCIA'),
(51, '2026-10-21', 'ADELANTO', 697.50, 'EFECTIVO'),
(52, '2026-10-05', 'ADELANTO', 400.00, 'YAPE'),
(53, '2026-10-06', 'ADELANTO', 675.00, 'TRANSFERENCIA'),
(54, '2026-10-04', 'ADELANTO', 697.50, 'EFECTIVO'),
(55, '2026-10-02', 'ADELANTO', 150.00, 'EFECTIVO'),
(56, '2026-10-13', 'ADELANTO', 100.00, 'EFECTIVO'),
(57, '2026-10-05', 'ADELANTO', 80.00, 'YAPE'),
(58, '2026-10-12', 'ADELANTO', 60.00, 'YAPE');

UPDATE pieza p SET estado = CASE d.tipo WHEN 'PERDIDA' THEN 'REGULAR' WHEN 'GRAVE' THEN 'REGULAR' ELSE 'BUENO' END
FROM danio d WHERE d.id_pieza = p.id_pieza;

INSERT INTO mantenimiento (id_traje, tipo, fecha_inicio, fecha_fin, costo, detalle) VALUES
(1, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(2, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(3, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(4, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(5, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(6, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(7, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(8, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(9, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(10, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(11, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(14, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(15, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(16, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(19, 'LAVADO', '2025-02-20', '2025-03-05', 25.00, 'Lavado en seco después de la Candelaria'),
(21, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(26, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(28, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(29, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(31, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(32, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(33, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(34, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(35, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(36, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(37, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(38, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(39, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(41, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(42, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(43, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(44, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(45, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(46, 'LAVADO', '2025-02-20', '2025-03-05', 15.00, 'Lavado en seco después de la Candelaria'),
(2, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(4, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(6, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(7, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(9, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(10, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(11, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(12, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(13, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(14, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(15, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(16, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(17, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(18, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(19, 'LAVADO', '2026-02-16', '2026-03-02', 25.00, 'Lavado en seco después de la Candelaria'),
(20, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(21, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(22, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(23, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(24, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(25, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(26, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(27, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(28, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(29, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(30, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(31, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(32, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(33, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(34, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(35, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(36, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(37, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(38, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(39, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(42, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(43, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(44, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(45, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(46, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(47, 'LAVADO', '2026-02-16', '2026-03-02', 15.00, 'Lavado en seco después de la Candelaria'),
(37, 'REPARACION', '2025-02-21', '2025-03-05', 23.10, 'Sombrero rota, requiere reparación'),
(44, 'REPARACION', '2025-02-24', '2025-03-08', 8.40, 'Faja rota, requiere reparación'),
(43, 'REPARACION', '2025-02-23', '2025-03-07', 18.90, 'Sombrero rota, requiere reparación'),
(35, 'REPARACION', '2025-02-22', '2025-03-06', 23.10, 'Chaqueta rota, requiere reparación'),
(39, 'REPARACION', '2025-06-27', '2025-07-09', 25.20, 'Mantilla rota, requiere reparación'),
(44, 'REPARACION', '2026-02-14', '2026-02-26', 25.20, 'Poncho rota, requiere reparación'),
(14, 'REPARACION', '2026-02-14', '2026-02-26', 94.50, 'Chaqueta rota, requiere reparación'),
(31, 'REPARACION', '2026-07-31', '2026-08-12', 16.80, 'Sombrero rota, requiere reparación'),
(15, 'REPARACION', '2026-08-02', '2026-08-14', 37.80, 'Matraca rota, requiere reparación'),
(20, 'REPARACION', '2026-10-05', NULL, 0.00, 'Cambio de pedrería y forro interior'),
(22, 'REPARACION', '2026-10-05', NULL, 0.00, 'Cambio de pedrería y forro interior');

UPDATE traje SET estado = 'MANTENIMIENTO' WHERE codigo IN ('CAP-001', 'CAP-003');

-- El traje más antiguo sin reservas pendientes se da de baja

UPDATE traje SET estado = 'BAJA'
WHERE id_traje = (SELECT t.id_traje FROM traje t
                  WHERE t.estado = 'DISPONIBLE'
                    AND NOT EXISTS (SELECT 1 FROM detalle_alquiler d JOIN alquiler a ON a.id_alquiler = d.id_alquiler
                                    WHERE d.id_traje = t.id_traje AND a.estado IN ('RESERVADO', 'ENTREGADO'))
                  ORDER BY t.fecha_adquisicion LIMIT 1);

-- Trajes nuevos comprados en setiembre de 2026 (todavía sin alquilar)
INSERT INTO traje (codigo, id_danza, personaje, genero, talla, color, precio_alquiler, garantia, fecha_adquisicion)
VALUES ('TIN-006', (SELECT id_danza FROM danza WHERE nombre = 'Tinkus'), 'Tinku varón', 'VARON', 'M', 'Multicolor', 120.00, 150.00, '2026-09-15'),
       ('TIN-007', (SELECT id_danza FROM danza WHERE nombre = 'Tinkus'), 'Tinku mujer', 'MUJER', 'S', 'Multicolor', 120.00, 150.00, '2026-09-15');
INSERT INTO pieza (id_traje, id_tipo_pieza, descripcion, valor_reposicion)
SELECT t.id_traje, tp.id_tipo_pieza, tp.nombre || ' de ' || LOWER(t.personaje), x.valor
FROM traje t
JOIN (VALUES ('TIN-006', 'Sombrero', 80), ('TIN-006', 'Chaqueta', 120), ('TIN-006', 'Pantalón', 80), ('TIN-006', 'Faja', 50),
             ('TIN-007', 'Sombrero', 80), ('TIN-007', 'Chaqueta', 110), ('TIN-007', 'Pollera', 110)) AS x (codigo, pieza, valor)
     ON x.codigo = t.codigo
JOIN tipo_pieza tp ON tp.nombre = x.pieza;


-- =====================================================================
-- VISTAS
-- =====================================================================

-- Resumen de cada alquiler: cliente, conjunto, trajes, montos, daños y pagos
CREATE VIEW v_alquileres_resumen AS
SELECT a.id_alquiler,
       a.id_cliente,
       a.estado,
       a.fecha_salida,
       a.fecha_devolucion_prevista,
       a.fecha_devolucion_real,
       CONCAT_WS(' ', c.nombres, c.apellidos)               AS cliente,
       COALESCE(cj.nombre, 'PARTICULAR')                     AS conjunto,
       (SELECT COUNT(*) FROM detalle_alquiler d WHERE d.id_alquiler = a.id_alquiler) AS num_trajes,
       a.monto_alquiler,
       a.monto_garantia,
       COALESCE((SELECT SUM(dn.monto_cobro)
                   FROM danio dn
                   JOIN detalle_alquiler d ON d.id_detalle = dn.id_detalle
                  WHERE d.id_alquiler = a.id_alquiler), 0)  AS monto_danios,
       COALESCE((SELECT SUM(p.monto) FROM pago p
                  WHERE p.id_alquiler = a.id_alquiler
                    AND p.concepto IN ('ADELANTO', 'SALDO')), 0) AS pagado_alquiler,
       a.observacion
FROM alquiler a
JOIN cliente c        ON c.id_cliente = a.id_cliente
LEFT JOIN conjunto cj ON cj.id_conjunto = a.id_conjunto;

-- Estado actual de cada traje y su próxima reserva
CREATE VIEW v_trajes_estado AS
SELECT t.codigo, d.nombre AS danza, t.personaje, t.genero, t.talla, t.estado,
       (SELECT MIN(a.fecha_salida)
          FROM detalle_alquiler da
          JOIN alquiler a ON a.id_alquiler = da.id_alquiler
         WHERE da.id_traje = t.id_traje
           AND a.estado IN ('SOLICITADO', 'RESERVADO', 'ENTREGADO')) AS proxima_salida,
       (SELECT COUNT(*) FROM detalle_alquiler da
         WHERE da.id_traje = t.id_traje)               AS veces_alquilado
FROM traje t
JOIN danza d ON d.id_danza = t.id_danza;


-- Los alquileres de ejemplo se "registraron" en su fecha_registro (no en el momento de cargar el script)
UPDATE alquiler SET creado_en = fecha_registro::timestamp + TIME '10:00';

-- =====================================================================
-- USUARIOS Y ROLES (inicio de sesión)
-- Contraseñas en BCrypt. Un usuario CLIENTE siempre está vinculado a un cliente.
--   admin / admin123 (ADMIN) · vendedor / venta123 (VENDEDOR)
--   cliente / cliente123 (CLIENTE, cliente con DNI 44268151)
-- =====================================================================

CREATE TABLE usuario (
    id_usuario     SERIAL PRIMARY KEY,
    username       VARCHAR(30)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,                       -- BCrypt
    rol            VARCHAR(10)  NOT NULL CHECK (rol IN ('ADMIN', 'VENDEDOR', 'CLIENTE')),
    id_cliente     INTEGER      UNIQUE REFERENCES cliente(id_cliente),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (rol <> 'CLIENTE' OR id_cliente IS NOT NULL)
);

INSERT INTO usuario (username, password_hash, rol, id_cliente) VALUES
('admin',    '$2a$10$ULHsRAWwvjt4K7jCO8W6eOF2DsD9HzMmDi6OPOQvg6m5cgRvcJ6Ry', 'ADMIN',    NULL),
('vendedor', '$2a$10$moz1T4gCf7KAC9lOxzGS.OSMQeBf28/Zj9JstTPoJAYxpv7Nj2s2m', 'VENDEDOR', NULL),
('cliente',  '$2a$10$WwDgWNmOw.yfBz2RttqeWePT2mBbEU3s37jjNRUL5ey7PE1EU1n0q', 'CLIENTE',
    COALESCE((SELECT id_cliente FROM cliente WHERE numero_documento = '44268151'),
             (SELECT MIN(id_cliente) FROM cliente)));
