#pragma once
#include <Arduino.h>
#include <driver/ledc.h>
#include "HapticConfig.h"
#include "PwmMath.h"

class ModulePwm {
public:
    bool begin() {
        pinMode(MOTOR_LEFT_PIN, OUTPUT); pinMode(MOTOR_RIGHT_PIN, OUTPUT);
        digitalWrite(MOTOR_LEFT_PIN, MODULE_ACTIVE_HIGH ? LOW : HIGH);
        digitalWrite(MOTOR_RIGHT_PIN, MODULE_ACTIVE_HIGH ? LOW : HIGH);
        if (!ledcSetup(0, MOTOR_PWM_HZ, MOTOR_PWM_BITS) ||
            !ledcSetup(1, MOTOR_PWM_HZ, MOTOR_PWM_BITS)) return false;
        // Arduino 2.x comparte timer por cada pareja de canales. Se hace
        // explicito para que ambos lados siempre tengan la misma portadora.
        if (ledc_bind_channel_timer(LEDC_HIGH_SPEED_MODE, LEDC_CHANNEL_0, LEDC_TIMER_0) != ESP_OK ||
            ledc_bind_channel_timer(LEDC_HIGH_SPEED_MODE, LEDC_CHANNEL_1, LEDC_TIMER_0) != ESP_OK) return false;
        ready_ = true;
        off();
        ledcAttachPin(MOTOR_LEFT_PIN, 0); ledcAttachPin(MOTOR_RIGHT_PIN, 1);
        return set(0, 0, 0);
    }
    void setFrequencyMode(bool enabled) { off(); frequencyMode_ = enabled; }
    bool frequencyMode() const { return frequencyMode_; }
    bool setFrequencyDutyPercent(uint8_t percent) {
        if (percent < MIN_FREQUENCY_DUTY_PERCENT ||
            percent > MAX_FREQUENCY_DUTY_PERCENT) return false;
        off();
        frequencyDutyPercent_ = percent;
        return true;
    }
    uint8_t frequencyDutyPercent() const { return frequencyDutyPercent_; }
    bool set(uint8_t left, uint8_t right, uint16_t centiHz) {
        if (!ready_ || left > 100 || right > 100 ||
            (centiHz && (centiHz < 6000 || centiHz > 60000))) {
            off(); return false;
        }
        // A STOP disables duty without replacing the last note's timer with 1 kHz.
        // STATUS can show the last configured frequency; running=0 means no vibration.
        if (!left && !right && !centiHz) { off(); return true; }
        const bool musical = frequencyMode_ && centiHz != 0;
        const uint32_t target = musical ? centiHz : MOTOR_PWM_HZ * 100;
        if (target != frequency_) {
            off();
            const uint32_t divider = haptic::pwmDivider(getApbFrequency(), target);
            if (divider < 256 || divider > 0x3ffff ||
                ledc_timer_set(LEDC_HIGH_SPEED_MODE, LEDC_TIMER_0, divider,
                    MOTOR_PWM_BITS, LEDC_APB_CLK) != ESP_OK) {
                frequency_ = 0; return false;
            }
            frequency_ = target;
            actualHz_ = haptic::pwmFrequency(getApbFrequency(), divider);
        }
        ledcWrite(0, haptic::moduleDuty(left, musical, MODULE_ACTIVE_HIGH,
                                         frequencyDutyPercent_));
        ledcWrite(1, haptic::moduleDuty(right, musical, MODULE_ACTIVE_HIGH,
                                         frequencyDutyPercent_));
        return true;
    }
    float actualHz() const { return actualHz_; }
    void off() {
        if (!ready_) return;
        ledcWrite(0, MODULE_ACTIVE_HIGH ? 0 : 2047);
        ledcWrite(1, MODULE_ACTIVE_HIGH ? 0 : 2047);
    }
private:
    bool ready_ = false, frequencyMode_ = DEFAULT_FREQUENCY_MODE;
    uint8_t frequencyDutyPercent_ = DEFAULT_FREQUENCY_DUTY_PERCENT;
    uint32_t frequency_ = 0;
    float actualHz_ = MOTOR_PWM_HZ;
};
