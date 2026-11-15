#pragma once
#include <cstdint>
constexpr int ESP_OK=0, LEDC_HIGH_SPEED_MODE=0, LEDC_TIMER_0=0,
              LEDC_CHANNEL_0=0, LEDC_CHANNEL_1=1, LEDC_APB_CLK=0;
inline int ledc_bind_channel_timer(int,int channel,int timer) { fake::bound[channel]=timer; return ESP_OK; }
inline int ledc_timer_set(int,int,uint32_t div,uint32_t,int) {
    if(fake::failTimer) return -1;
    fake::divider=div; return ESP_OK;
}
