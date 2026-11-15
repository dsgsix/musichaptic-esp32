# Protocolo Dual 5.1

## Servicio y características BLE

| Función | UUID | Acceso |
|---|---|---|
| Servicio háptico | `12345678-1234-1234-1234-1234567890ab` | Servicio GATT |
| Comandos | `abcdefab-1234-5678-1234-abcdefabcdef` | Escritura con o sin respuesta |
| Confirmaciones (ACK) | `abcdefac-1234-5678-1234-abcdefabcdef` | Lectura y notificaciones |
| Capacidades v2 | `abcdefad-1234-5678-1234-abcdefabcdef` | Lectura |
| Modelo musical v3 | `abcdefae-1234-5678-1234-abcdefabcdef` | Lectura |
| Intensidad global v4 | `abcdefaf-1234-5678-1234-abcdefabcdef` | Lectura |

La característica de intensidad devuelve `49 01 04 01` (hex). La app lee secuencialmente capacidades v2, modelo musical v3 y, si existe, intensidad v4. Mantiene una orden en vuelo y comprueba que el ACK coincida con lo solicitado.

## V4, 16 bytes, enteros little-endian

| Byte | Contenido |
|---|---|
| 0 | 0x48 comando / 0x41 ACK |
| 1 | 4 |
| 2 | Comando: 0 STOP / 1 PLAY. ACK: resultado |
| 3–4 | Secuencia |
| 5–6 | Niveles base L/R, 0–100 |
| 7 | Centro: debe ser 0 |
| 8–9 | Frecuencia central: 0 |
| 10–11 | Duración, 1–3000 ms al activar |
| 12–13 | Frecuencia solicitada, centésimas de Hz, 6000–60000 |
| 14 | Intensidad global, 0–100 |
| 15 | XOR de bytes 0–14 |

STOP usa campos 5–14 a cero. El emisor convierte intensidad cero a STOP. Se rechaza todo campo fuera de rango. Un error apaga salidas. Los ACK de error no contienen una orden activa. El XOR detecta ciertos errores de transmisión, no proporciona autenticación criptográfica.

## Aplicación de niveles

`scale = min(GAIN / 100, 100 / max(L,R)) * intensidad / 100`.

Ambos lados se multiplican por el mismo factor y se redondean. La intensidad no interviene en la frecuencia. En FREQ con portadora, `FREQDUTY` define el duty máximo entre 50 y 90 % del contador de 11 bits (por defecto 75 %, 1535 de 2047). En POWER, duty máximo = 2047. Un STOP pone duty a cero sin cambiar el temporizador. Frecuencia real de GPIO = APB / (divisor × 8) con resolución de 11 bits; la tabla solicitada se conserva en centésimas de Hz. No es realimentación de vibración mecánica.

El mapeo espacial sigue `d=10*pc/11`, `L=I*ln(11-d)/ln(11)`, `R=I*ln(1+d)/ln(11)`. `r=1` y Weber `0.22` son parámetros distintos. La app envía el patrón base y el selector global por separado. Se conservan tramas v1 de 10 bytes, v2 de 13 y v3 de 15, con su semántica previa de intensidad incorporada en L/R. No se ofrece canal central.
