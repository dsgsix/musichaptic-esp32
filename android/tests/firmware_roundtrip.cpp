// Integration against the supplied firmware, including real PWM adapter with mocked pins.
#include "HapticEngine.h"
#include "ModulePwm.h"
#include "OutputMath.h"
namespace fake {
uint32_t writes[2]={},divider=0; int pins[2]={},bound[2]={-1,-1}; bool failTimer=false;
}
#include <fstream>
#include <iostream>
#include <stdexcept>

int main(int argc, char** argv) {
    if (argc != 4) return 2;
    const int size = std::stoi(argv[3]);
    if (size != 10 && size != 15 && size != 16) return 2;
    std::ifstream input(argv[1], std::ios::binary);
    std::ofstream output(argv[2], std::ios::binary);
    if (!input || !output) return 2;
    unsigned char frame[16]{}, reply[16]{};
    haptic::Engine engine;
    ModulePwm pwm; if(!pwm.begin()) return 4;
    int count = 0;
    while (input.read(reinterpret_cast<char*>(frame), size)) {
        haptic::Command command;
        auto result = haptic::decode(frame, size, command);
        if (result != haptic::OK || command.center || command.centerHz)
            throw std::runtime_error("Frame invalid or central output requested");
        if (size == 15 && command.duration) {
            int pc = (count / 3) % 12;
            int intensity = (count % 3 == 0) ? 25 : (count % 3 == 1) ? 60 : 100;
            auto levels = haptic::noteLevels(pc, intensity);
            if (command.left != std::lround(levels.left) || command.right != std::lround(levels.right)
                || command.carrierCentiHz != std::lround(haptic::noteFrequency(pc)*100))
                throw std::runtime_error("Java and firmware musical mapping differ");
        }
        if(size==16 && command.duration) {
            const int pc=(count/3)%12, intensity=count%3==0?25:count%3==1?60:100;
            auto base=haptic::noteLevels(pc,100);
            if(command.left!=std::lround(base.left) || command.right!=std::lround(base.right)
                || command.intensity!=intensity || command.carrierCentiHz!=std::lround(haptic::noteFrequency(pc)*100))
                throw std::runtime_error("v4 base/intensity/frequency mismatch");
            for(int gain:{125,150,300}) {
                auto levels=haptic::sideLevels(command.left,command.right,gain,command.intensity);
                if(std::max(levels.left,levels.right)>intensity || (gain>=150 && std::max(levels.left,levels.right)!=intensity)) throw std::runtime_error("GAIN erased master intensity");
                if(!pwm.set(levels.left,levels.right,command.carrierCentiHz)
                    || std::fabs(pwm.actualHz()-haptic::noteFrequency(pc))>.02
                    || std::max(fake::writes[0],fake::writes[1])!=haptic::moduleDuty(std::max(levels.left,levels.right),true,true,DEFAULT_FREQUENCY_DUTY_PERCENT))
                    throw std::runtime_error("PWM frequency/duty mismatch");
            }
        }
        result = engine.apply(command, result, 100, 100);
        if (result != haptic::OK) throw std::runtime_error("Firmware rejected application frame");
        auto length = haptic::ack(command, result, reply);
        output.write(reinterpret_cast<char*>(reply), length);
        if (command.duration && (command.left || command.right)) {
            if (engine.tick(100+command.duration-1) || !engine.tick(100+command.duration) || engine.running)
                throw std::runtime_error("Output deadline failed");
        } else if (engine.running) throw std::runtime_error("STOP left output active");
        ++count;
    }
    if (!input.eof() || input.gcount() != 0 || count == 0) return 3;
    std::cout << "PASS: " << count << " application frames accepted by v5.1 engine; center off, deadlines and ACKs.\n";
}
