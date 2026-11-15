#pragma once
#include <cstdint>
// Modulos de tres pines con driver y entrada IN/PWM compatible con 3.3 V.
// NO conectar una bobina o un LRA de dos cables directamente al GPIO.
constexpr int MOTOR_LEFT_PIN = 27;
constexpr int MOTOR_RIGHT_PIN = 16;
constexpr bool MODULE_ACTIVE_HIGH = true;
constexpr int MOTOR_PWM_HZ = 1000;
constexpr int MOTOR_PWM_BITS = 11;
constexpr uint16_t DEFAULT_SIDE_GAIN = 125; // Ganancia inicial de los canales laterales.
constexpr bool DEFAULT_FREQUENCY_MODE = true; // FREQ al encender: aplica la frecuencia electrica de la nota.
constexpr uint8_t DEFAULT_FREQUENCY_DUTY_PERCENT = 75; // Aumenta amplitud sin cambiar Hz.
constexpr uint8_t MIN_FREQUENCY_DUTY_PERCENT = 50;
constexpr uint8_t MAX_FREQUENCY_DUTY_PERCENT = 90;
constexpr bool LEGACY_ROUTE_BY_NOTE = true;
static_assert(MOTOR_LEFT_PIN != MOTOR_RIGHT_PIN, "Los canales necesitan pines distintos");
static_assert(MOTOR_PWM_BITS == 11, "El calculo del divisor usa 11 bits");
// GPIO25/26, microfono y MPU6050 no se inicializan en esta version dual.
