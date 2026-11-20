package com.airticket.config;

import com.airticket.service.SeatHoldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SeatHoldScheduler {

    @Autowired
    private SeatHoldService seatHoldService;

    @Scheduled(fixedRate = 60000)
    public void releaseExpiredHolds() {
        seatHoldService.releaseExpiredHolds();
    }
}
