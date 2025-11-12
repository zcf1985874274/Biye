package com.example.springboot.service;

import com.example.springboot.common.Result;
import com.example.springboot.model.Room;

import java.util.List;

public interface RoomService {
    Result<?> getAllRooms();
    Result<?> getAvailableRooms(Integer storeId);
    Result<?> getRoomById(Integer id);
    Result<?> addRoom(Room room);
    Result<?> updateRoom(Room room);
    Result<?> deleteRoom(Integer id);
    Result<?> updateRoomStatus(Integer id, String status);
    Result<?> getRoomsByStoreId(Integer storeId);

    void checkAndUpdateExpiredRooms();
}