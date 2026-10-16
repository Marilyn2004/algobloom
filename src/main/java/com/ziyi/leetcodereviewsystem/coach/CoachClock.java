package com.ziyi.leetcodereviewsystem.coach;
import org.springframework.context.annotation.*;
import java.time.Clock;
@Configuration
class CoachClock { @Bean Clock learningClock(){return Clock.systemUTC();} }
