# Ficha para LinkedIn

## Nombre del proyecto

Sintiendo la música: app Android y brazalete háptico con ESP32

## Descripción

Integración de una aplicación Android con un prototipo de dos actuadores vibrotáctiles para representar notas y secuencias musicales mediante el tacto. La app permite explorar notas, importar MIDI y analizar archivos de audio; envía por Bluetooth Low Energy órdenes de frecuencia, intensidad y duración a un ESP32.

El trabajo incluye desarrollo móvil en Kotlin y Java, firmware en C++ con PlatformIO, detección de tono YIN, almacenamiento local en SQLite y pruebas de comunicación entre la app y el firmware. El proyecto está orientado a explorar la accesibilidad musical para personas sordas. La caracterización mecánica y la evaluación con usuarios constituyen la siguiente etapa.

## Enlace

https://github.com/dsgsix/musichaptic-esp32

## Aptitudes relacionadas

Android · Kotlin · Java · C++ · ESP32 · PlatformIO · Bluetooth Low Energy · Procesamiento de señales · Sistemas embebidos · Accesibilidad

## Evidencias

- [Montaje y capturas de la aplicación](EVIDENCIAS.md).
- [Informe del proyecto](informe-sintiendo-la-musica.pdf).
- [Validación técnica y comandos de prueba](VALIDACION.md).

La app y el firmware compilan correctamente. La validación incluye 13 pruebas Gradle y el intercambio de 1077 tramas con sus confirmaciones entre los núcleos Java y C++. Las mediciones mecánicas y la evaluación con usuarios quedan como siguiente etapa.
