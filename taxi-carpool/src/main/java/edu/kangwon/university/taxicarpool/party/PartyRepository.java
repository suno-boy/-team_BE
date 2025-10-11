package edu.kangwon.university.taxicarpool.party;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, Long> {

    Optional<PartyEntity> findById(Long partyId);

    Optional<PartyEntity> findByIdAndIsDeletedFalse(Long partyId);

    Page<PartyEntity> findAllByIsDeletedFalse(Pageable pageable);

    // 모든 파라미터가 온 경우
    @Query(value = "SELECT p.*, " +
        " (ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.start_longitude, ' ', p.start_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDepartureLng, ' ', :userDepartureLat, ')'))" +
        " ) + " +
        " ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.end_longitude, ' ', p.end_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDestinationLng, ' ', :userDestinationLat, ')'))"
        +
        " )" +
        " ) AS total_distance " +
        "FROM party p " +
        "WHERE p.is_deleted = false " +
        "ORDER BY total_distance ASC, ABS(TIMESTAMPDIFF(MINUTE, p.start_date_time, :userDepartureTime)) ASC",
        countQuery = "SELECT COUNT(*) FROM party p WHERE p.is_deleted = false",
        nativeQuery = true)
    Page<PartyEntity> findCustomPartyList(
        @Param("userDepartureLng") Double userDepartureLng,
        @Param("userDepartureLat") Double userDepartureLat,
        @Param("userDestinationLng") Double userDestinationLng,
        @Param("userDestinationLat") Double userDestinationLat,
        @Param("userDepartureTime") LocalDateTime userDepartureTime,
        Pageable pageable
    );

    // 출발지 또는 도착지에 대한 파라미터가 오지 않은 경우(오버로딩)
    @Query(value = "SELECT p.*, " +
        " ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.end_longitude, ' ', p.end_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDestinationLng, ' ', :userDestinationLat, ')'))"
        +
        " ) AS total_distance " +
        "FROM party p " +
        "WHERE p.is_deleted = false " +
        "ORDER BY total_distance ASC, ABS(TIMESTAMPDIFF(MINUTE, p.start_date_time, :userDepartureTime)) ASC",
        countQuery = "SELECT COUNT(*) FROM party p WHERE p.is_deleted = false",
        nativeQuery = true)
    Page<PartyEntity> findCustomPartyList(
        @Param("userDestinationLng") Double userDestinationLng,
        @Param("userDestinationLat") Double userDestinationLat,
        @Param("userDepartureTime") LocalDateTime userDepartureTime,
        Pageable pageable
    );

    // 출발 시간 파라미터가 오지 않은 경우
    @Query(value = "SELECT p.*, " +
        " (ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.start_longitude, ' ', p.start_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDepartureLng, ' ', :userDepartureLat, ')'))" +
        " ) + " +
        " ST_Distance_Sphere(" +
        "    ST_GeomFromText(CONCAT('POINT(', p.end_longitude, ' ', p.end_latitude, ')')), " +
        "    ST_GeomFromText(CONCAT('POINT(', :userDestinationLng, ' ', :userDestinationLat, ')'))"
        +
        " )" +
        " ) AS total_distance " +
        "FROM party p " +
        "WHERE p.is_deleted = false " +
        "ORDER BY total_distance ASC",
        countQuery = "SELECT COUNT(*) FROM party p WHERE p.is_deleted = false",
        nativeQuery = true)
    Page<PartyEntity> findCustomPartyList(
        @Param("userDepartureLng") Double userDepartureLng,
        @Param("userDepartureLat") Double userDepartureLat,
        @Param("userDestinationLng") Double userDestinationLng,
        @Param("userDestinationLat") Double userDestinationLat,
        Pageable pageable
    );

    @Query("SELECT p FROM party p JOIN p.memberEntities m WHERE m.id = :memberId AND p.isDeleted = false ORDER BY p.startDateTime DESC")
    List<PartyEntity> findAllActivePartiesByMemberId(@Param("memberId") Long memberId);

    /**
     * 출발 알림을 보내야 하는 파티 목록을 조회합니다.
     * @param after 지금으로부터 10분 뒤 시간
     * @param before 지금으로부터 11분 뒤 시간
     * @return 조건에 맞는 파티 엔티티 목록
     */
    @Query("SELECT p FROM party p WHERE p.isDeleted = false AND p.departureNotificationSent = false AND p.startDateTime > :after AND p.startDateTime <= :before")
    List<PartyEntity> findPartiesForDepartureReminder(
        @Param("after") LocalDateTime after,
        @Param("before") LocalDateTime before
    );

}
