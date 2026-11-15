# Validación técnica

## Resultados

Las compilaciones y pruebas de software se ejecutaron el **13 de noviembre de 2026** con la app MusicHaptic 2.5.0 y el firmware Vibrafil Dual 5.1.

| Comprobación | Resultado |
|---|---|
| PlatformIO, entorno `esp32_brazalete` | SUCCESS |
| Cinco suites C++ del firmware | PASS |
| Suites Java del núcleo | PASS |
| Interoperabilidad Java/C++ | PASS: 1002 tramas v1, 37 v3 y 38 v4, con confirmaciones verificadas |
| Gradle `testDebugUnitTest` | PASS: 13 pruebas, 0 fallos y 0 errores |
| Gradle `assembleDebug` | BUILD SUCCESSFUL |
| Python de referencia | Comprobación sintáctica correcta; procesamiento científico no ejecutado |
| Datos musicales | JSON válido con 12 notas y estructura válida de los 10 archivos MIDI |

Las suites verifican la codificación y validación de tramas, el checksum XOR, las confirmaciones, la selección temporal de notas, los niveles de intensidad y el cálculo de la señal PWM. La prueba de interoperabilidad intercambia órdenes y confirmaciones entre las implementaciones Java y C++; compara el duty con la configuración del firmware.

El firmware generado ocupa **38 424 bytes de RAM** y **1 126 281 bytes de flash**. La compilación completa mostró una advertencia de Arduino-ESP32 (`esp32-hal-uart.c`: retorno sin valor en `uartSetPins`); se generó el binario.

## Ejecución de la aplicación

El 14 de noviembre de 2026 se ejecutó MusicHaptic 2.5.0 en un emulador Android 15/API 35. Se comprobaron registro e inicio de sesión, exploración de notas, biblioteca, carga y reproducción MIDI, parada, desafío musical y selección de intensidad.

La importación de una muestra WAV de cinco tonos produjo cinco notas detectadas y habilitó su reproducción. La carga de Für Elise desde el catálogo preparó una secuencia de 74 notas. El recorrido se completó sin cierres de la aplicación.

Las capturas, la muestra WAV y el recorrido reproducible están en [Evidencias del prototipo](EVIDENCIAS.md). Esta sesión utilizó la salida Celular y no incluyó conexión a un ESP32 físico.

## Entorno

| Herramienta | Versión |
|---|---|
| PlatformIO Core | 6.2.0 |
| Plataforma Espressif32 | 6.5.0 |
| Arduino-ESP32 | 2.0.14 |
| JDK | 17 |
| Gradle | 8.11.1 |
| Android SDK Platform | 35 |
| Android Build Tools | 35.0.0 |
| Compilador C++ de escritorio | Zig 0.15.2 |

Los proyectos incluyen sus archivos de configuración de PlatformIO y Gradle. Los scripts de pruebas de escritorio requieren Python 3, un JDK completo y un compilador C++ disponible como `g++`.

## Comandos reproducibles

Desde la raíz del repositorio, con PlatformIO disponible:

```bash
python firmware/tests/run_tests.py
python android/tests/run_core_tests.py firmware/include
pio run -d firmware -e esp32_brazalete
```

Desde `android/`, en Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

En Linux/macOS:

```bash
./gradlew --no-daemon testDebugUnitTest assembleDebug
```

El flujo de GitHub Actions ejecuta las comprobaciones de compilación y pruebas en los cambios de `main` y en las solicitudes de cambios.

## Alcance y límites

Estos resultados corresponden a compilaciones y pruebas de software. Esta validación no incluye la carga del firmware en un ESP32 físico, la ejecución de la app en un teléfono físico ni mediciones de aceleración, resonancia, voltaje del módulo o latencia física. Tampoco acredita reconocimiento de notas, comodidad ni eficacia con usuarios.

La frecuencia informada por el firmware es una señal eléctrica calculada. La respuesta mecánica depende del actuador, el controlador, la alimentación y el montaje. Los dos canales comparten un temporizador y una frecuencia eléctrica de control.

El algoritmo Python de referencia no forma parte de la ejecución Android actual. Su procesamiento científico no se ejecutó en este entorno; la ruta activa de análisis está implementada en Kotlin/Java. La validación estructural de los archivos MIDI y del JSON no evalúa la precisión del reconocimiento musical.

Las cuentas locales almacenan contraseñas sin hash en SQLite y una actualización de versión de la base puede recrear las tablas. El enlace BLE no configura emparejamiento autenticado ni cifrado propio: el checksum y los ACK verifican tramas y recepción, no la identidad de un dispositivo. El prototipo requiere revisar estas funciones antes de utilizar datos reales o desplegarse fuera de demostraciones.

Los recursos musicales, iconos y dependencias mantienen las condiciones de sus respectivos autores. El repositorio no establece una licencia global para estos materiales.
