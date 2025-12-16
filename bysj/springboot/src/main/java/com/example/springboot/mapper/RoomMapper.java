package com.example.springboot.mapper;

import com.example.springboot.model.Room;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

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
    
    // 分页查询方法
    List<Room> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    List<Room> findAvailableRoomsWithPagination(@Param("storeId") Integer storeId, @Param("offset") int offset, @Param("limit") int limit);
    
    List<Room> findByStoreIdWithPagination(@Param("storeId") Integer storeId, @Param("offset") int offset, @Param("limit") int limit);
    
    // 统计总数方法
    int countAllRooms();
    
    int countAvailableRooms(@Param("storeId") Integer storeId);
    
    int countRoomsByStoreId(@Param("storeId") Integer storeId);
}