# Componentes de terceros

Trajes Candelaria v1.0 (© 2026 Erick Reison Huanca Tiña) utiliza los siguientes componentes de terceros.
Cada uno pertenece a sus respectivos autores y se rige por su propia licencia; la licencia propietaria de
Trajes Candelaria (archivo `LICENSE`) no se aplica a ellos.

## Bibliotecas (dependencias del pom.xml)

### Dependencias directas

| Componente | Versión | Licencia | Enlace |
|---|---|---|---|
| Spring Boot Starter JDBC | 3.5.16 | Apache License 2.0 | https://spring.io/projects/spring-boot |
| Spring Security Crypto (BCrypt) | 6.5.11 | Apache License 2.0 | https://spring.io/projects/spring-security |
| PostgreSQL JDBC Driver | 42.7.11 | BSD 2-Clause | https://jdbc.postgresql.org |
| OpenJFX (javafx-controls, javafx-fxml, javafx-graphics) | 21.0.5 | GPL v2 con Classpath Exception | https://openjfx.io |
| Ikonli (ikonli-javafx) | 12.3.1 | Apache License 2.0 | https://kordamp.org/ikonli |
| Ikonli Feather Pack (íconos Feather) | 12.3.1 | Apache License 2.0 (íconos Feather: MIT) | https://github.com/kordamp/ikonli · https://feathericons.com |

### Dependencias transitivas (incluidas por las anteriores)

| Componente | Versión | Licencia | Enlace |
|---|---|---|---|
| Spring Framework (core, beans, context, aop, expression, jcl, jdbc, tx) | 6.2.19 | Apache License 2.0 | https://spring.io/projects/spring-framework |
| Spring Boot, Spring Boot Autoconfigure, Starter, Starter Logging | 3.5.16 | Apache License 2.0 | https://spring.io/projects/spring-boot |
| OpenJFX javafx-base | 21.0.5 | GPL v2 con Classpath Exception | https://openjfx.io |
| Ikonli Core | 12.3.1 | Apache License 2.0 | https://kordamp.org/ikonli |
| HikariCP | 6.3.3 | Apache License 2.0 | https://github.com/brettwooldridge/HikariCP |
| Logback (classic, core) | 1.5.34 | EPL 1.0 / LGPL 2.1 (doble licencia) | https://logback.qos.ch |
| SLF4J (slf4j-api, jul-to-slf4j) | 2.0.18 | MIT | https://www.slf4j.org |
| Apache Log4j (log4j-api, log4j-to-slf4j) | 2.24.3 | Apache License 2.0 | https://logging.apache.org/log4j |
| Micrometer (commons, observation) | 1.15.12 | Apache License 2.0 | https://micrometer.io |
| Jakarta Annotations API | 2.1.1 | EPL 2.0 / GPL v2 con Classpath Exception | https://github.com/jakartaee/common-annotations-api |
| SnakeYAML | 2.4 | Apache License 2.0 | https://bitbucket.org/snakeyaml/snakeyaml |

## Fuentes tipográficas (src/main/resources/fonts)

| Fuente | Versión | Licencia | Enlace |
|---|---|---|---|
| Playfair Display (Regular, SemiBold, Bold) — © 2017 The Playfair Display Project Authors | 1.203 | SIL Open Font License 1.1 (texto en `OFL-PlayfairDisplay.txt`) | https://github.com/clauseggers/Playfair-Display |
| Plus Jakarta Sans (Regular, Medium, SemiBold, Bold) — © 2020 The Plus Jakarta Sans Project Authors | 2.071 | SIL Open Font License 1.1 (texto en `OFL-PlusJakartaSans.txt`) | https://github.com/tokotype/PlusJakartaSans |

## Recursos de los renders del personalizador (src/main/resources/render)

Los renders del personalizador (`base.png` y las capas de prendas de cada danza y ángulo) los hizo el autor,
Erick Reison Huanca Tiña, en Blender, usando los recursos de la tabla siguiente. No se distribuye ningún
archivo de Blender, de MPFB ni de los paquetes de MakeHuman: solo las imágenes resultantes.

| Recurso | Versión | Licencia | Enlace |
|---|---|---|---|
| MPFB (MakeHuman Plugin for Blender), complemento con el que se generó el personaje base | 2.0.18 | GPL v3 o posterior (el complemento); los modelos que genera no quedan sujetos a la GPL | https://static.makehumancommunity.org/mpfb.html |
| MakeHuman System Assets (malla base, ojos, cejas, pestañas, dientes, lengua y cabello) | paquete de recursos de MakeHuman | CC0 1.0 (dominio público) | https://static.makehumancommunity.org/assets/assetpacks.html |
| MakeHuman Skins 01 (textura de piel del personaje; en Blender el material se llama «Human.body», nombre genérico de MPFB) | paquete de recursos de MakeHuman | CC0 1.0 (dominio público; no exige atribución) | https://static.makehumancommunity.org/assets/assetpacks.html |
| MakeHuman System Hair Materials 01 (materiales del cabello) | paquete de recursos de MakeHuman | CC0 1.0 (dominio público) | https://static.makehumancommunity.org/assets/assetpacks.html |
| Blender (herramienta de modelado y render; no se distribuye) | 5.2.3 | GPL v2 o posterior (el programa; los renders son del autor) | https://www.blender.org |

Agradecimiento: aunque estos recursos son CC0 y no exigen atribución, el autor agradece a la comunidad
MakeHuman (https://www.makehumancommunity.org) por crear y compartir MakeHuman, MPFB y sus paquetes de recursos.

## Herramientas de compilación (no se distribuyen con el programa)

| Herramienta | Versión | Licencia | Enlace |
|---|---|---|---|
| Maven Wrapper | incluido en `.mvn/` | Apache License 2.0 | https://maven.apache.org/wrapper |
| PostgreSQL (servidor de base de datos, se instala aparte) | 13 o superior | PostgreSQL License | https://www.postgresql.org |
