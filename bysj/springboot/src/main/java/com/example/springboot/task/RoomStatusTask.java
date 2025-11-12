package com.example.springboot.task;

import com.example.springboot.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RoomStatusTask {

    @Autowired
    private RoomService roomService;

    /**
     * 每分钟检查一次房间状态
     */
    @Scheduled(fixedRate = 60000)
    public void checkRoomStatus() {
        roomService.checkAndUpdateExpiredRooms();
    }
}
