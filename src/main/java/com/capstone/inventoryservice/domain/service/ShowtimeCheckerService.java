package com.capstone.inventoryservice.domain.service;

import com.capstone.inventoryservice.domain.dto.request.AssignCheckerRequest;
import com.capstone.inventoryservice.domain.dto.request.ApproveCheckerRequest;
import com.capstone.inventoryservice.domain.dto.response.EventResponse;
import com.capstone.inventoryservice.domain.dto.response.ShowtimeResponse;
import com.capstone.inventoryservice.domain.dto.response.ShowtimeCheckerResponse;
import com.capstone.inventoryservice.domain.mapper.ShowtimeCheckerMapper;
import com.capstone.inventoryservice.domain.util.ShowtimeUtil;
import com.capstone.inventoryservice.exception.AppException;
import com.capstone.inventoryservice.exception.ErrorCode;
import com.capstone.inventoryservice.model.entity.Showtime;
import com.capstone.inventoryservice.model.entity.ShowtimeChecker;
import com.capstone.inventoryservice.model.enums.CheckerAssignmentStatus;
import com.capstone.inventoryservice.model.repository.ShowtimeCheckerRepository;
import com.capstone.inventoryservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeCheckerService {

    private final ShowtimeCheckerRepository showtimeCheckerRepository;
    private final ShowtimeUtil showtimeUtil;
    private final ShowtimeCheckerMapper showtimeCheckerMapper;
    private final JwtUtil jwtUtil;
    private final EventService eventService;

    @Transactional
    public ShowtimeCheckerResponse assignChecker(Long showtimeId, AssignCheckerRequest request) {
        // Validate showtime
        Showtime showtime = showtimeUtil.getShowtimeOrElseThrow(showtimeId);

        // Get admin ID
        Long adminId = jwtUtil.getDataFromAuth().userId();
        if (adminId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu đăng nhập quản trị viên");
        }

        // Check if checker already assigned or registered
        Optional<ShowtimeChecker> existing = showtimeCheckerRepository
                .findByShowtimeIdAndCheckerId(showtimeId, request.getCheckerId());

        ShowtimeChecker assignment;
        if (existing.isPresent()) {
            assignment = existing.get();
            assignment.setStatus(CheckerAssignmentStatus.APPROVED);
            assignment.setAssignedBy(adminId);
        } else {
            assignment = ShowtimeChecker.builder()
                    .showtime(showtime)
                    .checkerId(request.getCheckerId())
                    .status(CheckerAssignmentStatus.APPROVED)
                    .assignedBy(adminId)
                    .build();
        }

        ShowtimeChecker saved = showtimeCheckerRepository.save(assignment);
        log.info("Admin {} assigned checker {} to showtime {}", adminId, request.getCheckerId(), showtimeId);
        return showtimeCheckerMapper.convertToDTO(saved);
    }

    @Transactional
    public ShowtimeCheckerResponse registerChecker(Long showtimeId) {
        // Validate showtime
        Showtime showtime = showtimeUtil.getShowtimeOrElseThrow(showtimeId);

        // Get checker ID from token
        Long checkerId = jwtUtil.getDataFromAuth().userId();
        if (checkerId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu đăng nhập để đăng ký");
        }

        Optional<ShowtimeChecker> existing = showtimeCheckerRepository
                .findByShowtimeIdAndCheckerId(showtimeId, checkerId);

        if (existing.isPresent()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Bạn đã đăng ký tham gia showtime này rồi");
        }

        ShowtimeChecker registration = ShowtimeChecker.builder()
                .showtime(showtime)
                .checkerId(checkerId)
                .status(CheckerAssignmentStatus.PENDING)
                .build();

        ShowtimeChecker saved = showtimeCheckerRepository.save(registration);
        log.info("Checker {} registered for showtime {}", checkerId, showtimeId);
        return showtimeCheckerMapper.convertToDTO(saved);
    }

    @Transactional
    public ShowtimeCheckerResponse approveChecker(Long showtimeId, Long checkerId, ApproveCheckerRequest request) {
        // Validate showtime
        showtimeUtil.getShowtimeOrElseThrow(showtimeId);

        // Get admin ID
        Long adminId = jwtUtil.getDataFromAuth().userId();
        if (adminId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Yêu cầu đăng nhập quản trị viên");
        }

        ShowtimeChecker assignment = showtimeCheckerRepository
                .findByShowtimeIdAndCheckerId(showtimeId, checkerId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy lượt đăng ký của checker này"));

        assignment.setStatus(request.getStatus());
        assignment.setAssignedBy(adminId);

        ShowtimeChecker saved = showtimeCheckerRepository.save(assignment);
        log.info("Admin {} {} checker {} for showtime {}", adminId, request.getStatus(), checkerId, showtimeId);
        return showtimeCheckerMapper.convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ShowtimeCheckerResponse> getCheckersByShowtime(Long showtimeId) {
        // Validate showtime
        showtimeUtil.getShowtimeOrElseThrow(showtimeId);

        return showtimeCheckerRepository.findByShowtimeId(showtimeId).stream()
                .map(showtimeCheckerMapper::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeCheckerResponse> getShowtimesForChecker(Long checkerId) {
        return showtimeCheckerRepository.findByCheckerId(checkerId).stream()
                .map(showtimeCheckerMapper::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getApprovedEventsForChecker(Long checkerId) {
        return showtimeCheckerRepository.findByCheckerId(checkerId).stream()
                .filter(sc -> sc.getStatus() == CheckerAssignmentStatus.APPROVED)
                .map(sc -> sc.getShowtime().getEvent())
                .distinct()
                .map(eventService::convertToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getApprovedShowtimesForChecker(Long checkerId) {
        return showtimeCheckerRepository.findByCheckerId(checkerId).stream()
                .filter(sc -> sc.getStatus() == CheckerAssignmentStatus.APPROVED)
                .map(sc -> eventService.convertShowtimeToDTO(sc.getShowtime()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isCheckerAssigned(Long showtimeId, Long checkerId) {
        return showtimeCheckerRepository
                .findByShowtimeIdAndCheckerId(showtimeId, checkerId)
                .map(sc -> sc.getStatus() == CheckerAssignmentStatus.APPROVED)
                .orElse(false);
    }
}
