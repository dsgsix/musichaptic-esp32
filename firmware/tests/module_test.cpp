#include "ModulePwm.h"
#include "HapticProtocol.h"
#include <cassert>
#include <cstdio>
namespace fake {
uint32_t writes[2]={}, divider=0;
int pins[2]={},bound[2]={-1,-1}; bool failTimer=false;
}
int main() {
    ModulePwm pwm;
    assert(pwm.begin()); assert(fake::pins[0]==27 && fake::pins[1]==16);
    assert(fake::bound[0]==0 && fake::bound[1]==0);
    assert(fake::writes[0]==0 && fake::writes[1]==0);
    assert(pwm.frequencyMode()); // Modo FREQ al iniciar.
    assert(pwm.frequencyDutyPercent()==75);
    for(int pc=0;pc<12;++pc) {
        assert(pwm.set(100,100,uint16_t(std::lround(haptic::NOTE_HZ[pc]*100))));
        assert(std::fabs(pwm.actualHz()-haptic::NOTE_HZ[pc])<.02f);
        assert(fake::writes[0]==1535 && fake::writes[1]==1535);
    }
    pwm.setFrequencyMode(false);
    // Exercise actual adapter calls: TEST L vs DO at same requested level.
    assert(pwm.set(80,0,0)); const auto testDuty=fake::writes[0], testDivider=fake::divider;
    assert(pwm.set(80,0,6215));
    assert(fake::writes[0]==testDuty && fake::writes[1]==0 && fake::divider==testDivider);
    assert(pwm.set(0,80,55383)); assert(fake::writes[1]==testDuty && fake::writes[0]==0);
    assert(pwm.actualHz()==1000);
    for(int pc=0;pc<12;++pc) {
        haptic::Command c; haptic::notePan(pc,80,c);
        assert(pwm.set(c.left,c.right,c.carrierCentiHz));
        assert(fake::divider==testDivider);
    }
    assert(pwm.set(60,60,18553)); assert(fake::writes[0]==fake::writes[1]);
    pwm.setFrequencyMode(true); assert(fake::writes[0]==0 && fake::writes[1]==0);
    for(int pc=0;pc<12;++pc) {
        haptic::Command c; haptic::notePan(pc,100,c);
        assert(pwm.set(c.left,c.right,c.carrierCentiHz));
        assert(std::fabs(pwm.actualHz()-haptic::NOTE_HZ[pc])<.02f);
        assert(fake::writes[0]<=1535 && fake::writes[1]<=1535);
    }
    assert(!pwm.setFrequencyDutyPercent(49));
    assert(!pwm.setFrequencyDutyPercent(91));
    assert(pwm.setFrequencyDutyPercent(85));
    assert(pwm.frequencyDutyPercent()==85);
    assert(pwm.set(100,0,6000));
    assert(fake::writes[0]==1740 && fake::writes[1]==0);
    fake::failTimer=true;
    assert(!pwm.set(80,0,6215)); assert(fake::writes[0]==0 && fake::writes[1]==0);
    fake::failTimer=false;
    assert(pwm.set(80,0,6215));
    assert(!pwm.set(255,0,6215)); assert(fake::writes[0]==0 && fake::writes[1]==0);
    pwm.setFrequencyMode(false);
    assert(pwm.set(80,80,6215)); assert(fake::divider==testDivider);
    pwm.off(); assert(fake::writes[0]==0 && fake::writes[1]==0);
    assert(!pwm.set(80,80,1)); assert(fake::writes[0]==0 && fake::writes[1]==0);
    std::puts("PASS: actual PWM adapter with mocked hardware: TEST==NOTE in POWER, pins/timer, FREQ table, fail-safe OFF.");
}
