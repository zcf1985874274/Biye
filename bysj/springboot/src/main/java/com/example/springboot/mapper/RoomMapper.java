package com.example.springboot.mapper;

import com.example.springboot.model.Room;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoomMapper {
    List<Room> findAll();

    Room findById(@Param("roomId") Integer roomId);

    @Options(useGeneratedKeys = true, keyProperty = "roomId")
    int insert(Room room);

    int update(Room room);

    int deleteById(@Param("roomId") Integer roomId);

    List<Room> findAvailableRooms(@Param("storeId") Integer storeId);
    
    int updateStatus(@Param("roomId") Integer roomId, @Param("status") String status);
    
    List<Room> findByStatus(@Param("status") String status);

    List<Room> findByStoreId(@Param("storeId") Integer storeId);
}