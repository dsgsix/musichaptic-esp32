# Verificación Vibrafil Dual 5.1

## Resultado

Verificación local: 13 de noviembre de 2026.

- `pio run -e esp32_brazalete`: **SUCCESS**.
- Firmware binario generado para `esp32dev`, Espressif32 6.5.0 y Arduino-ESP32 2.0.14. RAM: 38 424 de 327 680 bytes; flash: 1 126 281 de 3 145 728 bytes.
- `python tests/run_tests.py` con compilador C++ Zig 0.15.2: cinco ejecutables **PASS** (`protocol_test`, `model_test`, `module_test`, `dual_test`, `level_test`), con `-Wall -Wextra -Werror -pedantic`.
- Pruebas v4: 12 frecuencias × 4 ganancias × 3 niveles (25/60/100 %), STOP, checksum y ACK: **PASS**.

La compilación completa mostró un warning de la dependencia Arduino-ESP32 (`esp32-hal-uart.c`: retorno sin valor en `uartSetPins`). No impidió generar el binario.

## Qué se verificó en software

- GPIO izquierdo = 27; derecho = 16.
- Modo predeterminado = `FREQ`.
- Cada orden v4 conserva la frecuencia en centésimas de Hz.
- La intensidad se aplica después de `GAIN` y el reparto izquierdo/derecho conserva la proporción.
- Duty FREQ configurable = 50..90 % del contador (por defecto 75 %, 1535/2047); POWER máximo = 2047/2047.
- `FREQDUTY` cambia únicamente amplitud/duty; el temporizador sigue usando la frecuencia de la nota.
- El temporizador es común a ambos lados; no se permite canal central.
- Una orden inválida o vencida deja las salidas apagadas.

## Límite de esta verificación

Las pruebas no sustituyen una medición con acelerómetro u osciloscopio en el montaje real. `[APPLIED]` informa la señal PWM eléctrica calculada; no es una medición de aceleración, resonancia, voltaje del actuador ni nivel percibido.
