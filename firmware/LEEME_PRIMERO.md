# Vibrafil Dual 5.1

Firmware para ESP32-WROOM con dos salidas hápticas controladas por BLE o USB. La app MusicHaptic procesa las notas y envía el reparto izquierdo/derecho, la frecuencia y la intensidad. El ESP32 aplica la señal PWM; no decodifica MP3 ni PCM.

## Montaje

| Conexión | GPIO |
|---|---:|
| Entrada IN/PWM del módulo izquierdo | 27 |
| Entrada IN/PWM del módulo derecho | 16 |

Se requieren módulos con etapa de control y entrada compatible con 3,3 V, con tierra común al ESP32. Una bobina o un actuador de dos cables no se conecta directamente al GPIO. La alimentación y el driver deben corresponder al módulo utilizado. No se inicializan GPIO25/26, DAC, I2S, micrófono ni MPU6050; el canal central está deshabilitado.

## Compilar y cargar

Abre la carpeta `firmware/` en VS Code con PlatformIO. El entorno `esp32_brazalete` usa `esp32dev`, Espressif32 6.5.0 y Arduino. Desde la terminal de PlatformIO:

```sh
pio run -e esp32_brazalete
pio run -e esp32_brazalete -t upload
pio device monitor -b 115200
```

También están disponibles las tareas Build, Upload y Monitor en el panel de PlatformIO. Si hay varios puertos, configura `upload_port` y `monitor_port` en `platformio.ini`. Cierra otros monitores antes de cargar.

Al reiniciar aparece `VIBRAFIL DUAL v5.1`, GPIO27/GPIO16 y `MODE=FREQ`. El monitor serial envía cada línea al pulsar Enter.

## Órdenes USB

| Orden | Función |
|---|---|
| `STATUS` | Estado BLE, salidas y ajustes |
| `LAST` | Última orden y PWM calculado |
| `TEST L`, `TEST R`, `TEST B` | Prueba izquierda, derecha o ambos |
| `PLAY DO`, `PLAY SI`, `PLAY MID` | Nota o posición central entre ambos módulos |
| `NOTE DO` | Equivalente a `PLAY DO` |
| `SWEEP` | Doce notas y MID, con pausas |
| `STOP` | Detiene las salidas y el barrido |
| `GAIN 125` | Ganancia de 0 a 300 % |
| `FREQDUTY 75` | Duty máximo en FREQ, de 50 a 90 % |
| `MODE FREQ` | PWM según la frecuencia de la nota |
| `MODE POWER` | PWM fijo a 1000 Hz para comparar amplitud |
| `SAVE` | Guarda GAIN y FREQDUTY en NVS |
| `DEFAULTS` | Ajustes predeterminados en RAM |
| `TRACE ON`, `TRACE OFF` | Registro periódico de órdenes |
| `HELP` | Ayuda de consola |

Los valores iniciales son `GAIN 125`, `FREQDUTY 75` y `MODE FREQ`. GAIN y FREQDUTY pueden recuperarse de NVS; el modo siempre vuelve a FREQ al reiniciar. `DEFAULTS` solo modifica RAM hasta ejecutar `SAVE`.

Ejemplo de comprobación:

```text
STATUS
TRACE ON
PLAY DO
LAST
PLAY SI
LAST
STOP
```

DO solicita 62,15 Hz y SI 553,83 Hz. `[APPLIED]` muestra la frecuencia eléctrica calculada del temporizador, con su cuantización. `LAST` conserva la última orden al terminar el pulso. `running=0` significa que no hay salida activa aunque el temporizador conserve la frecuencia anterior.

## Uso con MusicHaptic

La app está en la carpeta `android/` del repositorio. Tras instalarla, conecta el dispositivo **ESP32 Vibrafil** y selecciona Baja (25 %), Media (60 %) o Alta (100 %). La app comprueba las capacidades BLE antes de enviar órdenes musicales v4.

Para comparar niveles, conserva el mismo contacto del brazalete y repite DO en los tres niveles; después compara SI y las pruebas de izquierda/derecha/ambos. Al detener o desconectar, las salidas deben apagarse. Durante una canción, el cambio de intensidad llega en la siguiente renovación de la orden, nominalmente cada 250 ms más latencia BLE.

Con una orden DO v4 y GAIN 150:

| Intensidad | Niveles aplicados L/R | Duty izquierdo, FREQDUTY 75 % |
|---|---|---:|
| Baja, 25 % | 25 / 0 | 384 de 2047 |
| Media, 60 % | 60 / 0 | 921 de 2047 |
| Alta, 100 % | 100 / 0 | 1535 de 2047 |

La intensidad global se aplica después de limitar la ganancia, conservando la proporción espacial. Las tres órdenes mantienen la misma frecuencia. El 100 % del selector alcanza el máximo configurado por FREQDUTY; un duty del 100 % sería un nivel continuo.

## Frecuencias y reparto espacial

| Nota | Frecuencia háptica solicitada (Hz) | Audio musical del teléfono (Hz) |
|---|---:|---:|
| DO | 62.15 | 261.63 |
| DO♯ | 75.82 | 277.18 |
| RE | 92.50 | 293.66 |
| RE♯ | 112.85 | 311.13 |
| MI | 137.68 | 329.63 |
| FA | 167.97 | 349.23 |
| FA♯ | 204.92 | 369.99 |
| SOL | 250.00 | 392.00 |
| SOL♯ | 305.00 | 415.30 |
| LA | 372.10 | 440.00 |
| LA♯ | 453.96 | 466.16 |
| SI | 553.83 | 493.88 |

El reparto logarítmico desplaza DO hacia la izquierda y SI hacia la derecha. El punto de igualdad está entre FA y FA♯. Ambos canales comparten temporizador: una nota gobierna la frecuencia de los dos lados. La app prioriza la nota que comienza más recientemente; en un inicio simultáneo, la de mayor frecuencia háptica.

Las frecuencias de la tabla corresponden a la señal eléctrica de control. No equivalen a una medición de vibración mecánica ni a la afinación del altavoz. La respuesta del actuador depende de su resonancia, driver, alimentación y montaje. El firmware no incorpora realimentación de un sensor. La versión actual no acredita calibración física del parámetro espacial `r=1`.

## Pruebas

Las pruebas nativas requieren Python 3 y un compilador C++ compatible con `g++` en PATH, o indicado mediante la variable `CXX`:

```sh
python tests/run_tests.py
```

Cubren protocolo, modelo espacial, temporizador PWM con hardware simulado, límites, sesiones, vencimientos e intensidad global. Consulta [VERIFICACION.md](VERIFICACION.md) para los resultados y [PROTOCOLO_Y_MODELO.md](docs/PROTOCOLO_Y_MODELO.md) para el formato BLE.
