//package com.capstone.inventoryservice.config;
//
//import com.capstone.inventoryservice.model.entity.Event;
//import com.capstone.inventoryservice.model.entity.Province;
//import com.capstone.inventoryservice.model.entity.Showtime;
//import com.capstone.inventoryservice.model.entity.TicketType;
//import com.capstone.inventoryservice.model.entity.Ward;
//import com.capstone.inventoryservice.model.enums.EventApprovalStatus;
//import com.capstone.inventoryservice.model.enums.EventCategory;
//import com.capstone.inventoryservice.model.enums.EventType;
//import com.capstone.inventoryservice.model.enums.TicketTypeStatus;
//import com.capstone.inventoryservice.model.repository.EventRepository;
//import com.capstone.inventoryservice.model.repository.ProvinceRepository;
//import com.capstone.inventoryservice.model.repository.WardRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.context.annotation.Profile;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@Component
//@Profile("!prod")
//@RequiredArgsConstructor
//@Slf4j
//public class DataInitializer implements ApplicationRunner {
//
//    private static final Long DEFAULT_BANK_INFO_ID = 1L;
//
//    private final EventRepository eventRepository;
//    private final ProvinceRepository provinceRepository;
//    private final WardRepository wardRepository;
//    private final JdbcTemplate jdbcTemplate;
//
//    private Optional<Long> resolveOrganizerProfileId() {
//        List<Long> ids = jdbcTemplate.queryForList("""
//                SELECT op.id
//                FROM iam_service.organization_profiles op
//                ORDER BY
//                    CASE WHEN op.status = 'VERIFIED' THEN 0 ELSE 1 END,
//                    op.id
//                LIMIT 1
//                """, Long.class);
//
//        return ids.stream().findFirst();
//    }
//
//    private void repairSeedEventOrganizerIds(List<Event> seedEvents, Long organizerProfileId) {
//        int updatedRows = 0;
//
//        for (Event event : seedEvents) {
//            updatedRows += jdbcTemplate.update("""
//                    UPDATE inventory_service.events
//                    SET organizer_id = ?,
//                        updated_at = NOW()
//                    WHERE event_name = ?
//                      AND organizer_id IS DISTINCT FROM ?
//                    """,
//                    organizerProfileId,
//                    event.getEventName(),
//                    organizerProfileId
//            );
//        }
//
//        if (updatedRows > 0) {
//            log.info("Repaired organizer_id for {} existing inventory seed events.", updatedRows);
//        }
//    }
//
//    @Override
//    @Transactional
//    public void run(ApplicationArguments args) {
//        Map<Integer, Province> provinces = seedLocations();
//
//        Optional<Long> organizerProfileId = resolveOrganizerProfileId();
//
//        if (organizerProfileId.isEmpty()) {
//            log.warn("""
//                    No organization profile found in iam_service.organization_profiles.
//                    Skipping inventory event initialization to avoid invalid organizer_id.
//                    """);
//            return;
//        }
//
//        List<Event> seedEvents = buildSeedEvents(provinces, organizerProfileId.get());
//
//        repairSeedEventOrganizerIds(seedEvents, organizerProfileId.get());
//
//        List<Event> newEvents = seedEvents.stream()
//                .filter(event -> !eventRepository.existsByEventName(event.getEventName()))
//                .toList();
//
//        if (newEvents.isEmpty()) {
//            log.info("Inventory seed data already exists. Skipping event initialization.");
//            return;
//        }
//
//        eventRepository.saveAll(newEvents);
//        log.info("Initialized {} inventory seed events.", newEvents.size());
//    }
//
//    private Map<Integer, Province> seedLocations() {
//        Province haNoi = getOrCreateProvince(1, "Ha Noi", "thanh_pho", "ha_noi", 24);
//        Province hoChiMinh = getOrCreateProvince(79, "Ho Chi Minh", "thanh_pho", "ho_chi_minh", 28);
//
//        getOrCreateWard(4, "Ward 4", "phuong", "ward_4", haNoi);
//        getOrCreateWard(8, "Ward 8", "phuong", "ward_8", haNoi);
//        getOrCreateWard(25, "Ward 25", "phuong", "ward_25", haNoi);
//        getOrCreateWard(25747, "Ben Nghe", "phuong", "ben_nghe", hoChiMinh);
//        getOrCreateWard(25813, "Da Kao", "phuong", "da_kao", hoChiMinh);
//        getOrCreateWard(26884, "Thao Dien", "phuong", "thao_dien", hoChiMinh);
//
//        return Map.of(
//                haNoi.getCode(), haNoi,
//                hoChiMinh.getCode(), hoChiMinh
//        );
//    }
//
//    private Province getOrCreateProvince(
//            Integer code,
//            String name,
//            String divisionType,
//            String codename,
//            Integer phoneCode
//    ) {
//        return provinceRepository.findByCode(code)
//                .orElseGet(() -> provinceRepository.save(Province.builder()
//                        .code(code)
//                        .name(name)
//                        .divisionType(divisionType)
//                        .codename(codename)
//                        .phoneCode(phoneCode)
//                        .build()));
//    }
//
//    private Ward getOrCreateWard(
//            Integer code,
//            String name,
//            String divisionType,
//            String codename,
//            Province province
//    ) {
//        return wardRepository.findByCode(code)
//                .orElseGet(() -> wardRepository.save(Ward.builder()
//                        .code(code)
//                        .name(name)
//                        .divisionType(divisionType)
//                        .codename(codename)
//                        .province(province)
//                        .build()));
//    }
//
//    private List<Event> buildSeedEvents(Map<Integer, Province> provinces, Long organizerProfileId) {
//        LocalDateTime now = LocalDateTime.now();
//
//        return List.of(
//                buildEvent(
//                        "Indie Night Saigon",
//                        "An intimate live music night with local indie artists.",
//                        "Indie bands, acoustic sets, and community booths.",
//                        "Nguyen Hue Walking Street",
//                        "Nguyen Hue Street",
//                        provinces.get(79),
//                        getWard(25747),
//                        EventType.OFFLINE,
//                        EventCategory.LIVESTAGE,
//                        now.plusDays(7),
//                        BigDecimal.valueOf(1500),
//                        true,
//                        organizerProfileId,
//                        BigDecimal.valueOf(10.776900), BigDecimal.valueOf(106.700900)
//                ),
//                buildEvent(
//                        "Contemporary Stage Showcase",
//                        "A compact showcase of modern theater and movement.",
//                        "Short-form theater, dance, and post-show discussion.",
//                        "Hanoi Youth Theater",
//                        "Ngo Quyen Street",
//                        provinces.get(1),
//                        getWard(4),
//                        EventType.OFFLINE,
//                        EventCategory.STAGE_ART,
//                        now.plusDays(12),
//                        BigDecimal.valueOf(2200),
//                        false,
//                        organizerProfileId,
//                        BigDecimal.valueOf(10.776900), BigDecimal.valueOf(106.700900)
//                ),
//                buildEvent(
//                        "Product Design Workshop",
//                        "Hands-on workshop for early-stage product teams.",
//                        "Practical design exercises, critique sessions, and templates.",
//                        "Thao Dien Creative Hub",
//                        "Xuan Thuy Street",
//                        provinces.get(79),
//                        getWard(26884),
//                        EventType.HYBRID,
//                        EventCategory.WORKSHOP,
//                        now.plusDays(18),
//                        BigDecimal.valueOf(3200),
//                        true,
//                        organizerProfileId,
//                        BigDecimal.valueOf(10.776900), BigDecimal.valueOf(106.700900)
//                ),
//                buildEvent(
//                        "Weekend Futsal Cup",
//                        "Community futsal tournament for amateur teams.",
//                        "Group stage, finals, and award ceremony.",
//                        "District Sports Center",
//                        "Tran Duy Hung Street",
//                        provinces.get(1),
//                        getWard(8),
//                        EventType.OFFLINE,
//                        EventCategory.SPORTS,
//                        now.plusDays(24),
//                        BigDecimal.valueOf(1800),
//                        false,
//                        organizerProfileId,
//                        BigDecimal.valueOf(10.776900), BigDecimal.valueOf(106.700900)
//                ),
//                buildEvent(
//                        "Urban Art Pop-up",
//                        "A small-format exhibition for young visual artists.",
//                        "Paintings, prints, installations, and artist talks.",
//                        "Da Kao Art Space",
//                        "Dien Bien Phu Street",
//                        provinces.get(79),
//                        getWard(25813),
//                        EventType.OFFLINE,
//                        EventCategory.EXHIBITION,
//                        now.plusDays(30),
//                        BigDecimal.valueOf(1200),
//                        true,
//                        organizerProfileId,
//                        BigDecimal.valueOf(10.776900), BigDecimal.valueOf(106.700900)
//                ),
//                buildEvent(
//                        "Startup Finance Clinic",
//                        "A practical finance clinic for founders and operators.",
//                        "Budget planning, pricing models, and funding readiness.",
//                        "Hanoi Innovation Lab",
//                        "Kim Ma Street",
//                        provinces.get(1),
//                        getWard(25),
//                        EventType.ONLINE,
//                        EventCategory.WORKSHOP,
//                        now.plusDays(35),
//                        BigDecimal.valueOf(4500),
//                        false,
//                        organizerProfileId,
//                        BigDecimal.valueOf(10.776900), BigDecimal.valueOf(106.700900)
//                )
//        );
//    }
//
//    private Ward getWard(Integer code) {
//        return wardRepository.findByCode(code)
//                .orElseThrow(() -> new IllegalStateException("Missing seed ward with code: " + code));
//    }
//
//    private Event buildEvent(
//            String eventName,
//            String shortDescription,
//            String description,
//            String venue,
//            String address,
//            Province province,
//            Ward ward,
//            EventType eventType,
//            EventCategory category,
//            LocalDateTime startDatetime,
//            BigDecimal basePrice,
//            boolean featured,
//            Long organizerProfileId,
//            BigDecimal latitude,
//            BigDecimal longitude
//    ) {
//        Event event = Event.builder()
//                .eventName(eventName)
//                .shortDescription(shortDescription)
//                .description(description)
//                .venue(venue)
//                .address(address)
//                .province(province)
//                .ward(ward)
//                .eventType(eventType)
//                .bannerImage("https://res.cloudinary.com/dvnnvjtau/image/upload/v1779873450/plt-flash-sale-banner_yarm9w.jpg")
//                .thumbnailImage("https://res.cloudinary.com/dvnnvjtau/image/upload/v1779873451/plt-resale-banner_upmvfz.jpg")
//                .totalSeats(200)
//                .organizerId(organizerProfileId)
//                .bankInfoId(DEFAULT_BANK_INFO_ID)
//                .isFeatured(featured)
//                .isCancelled(false)
//                .approvalStatus(EventApprovalStatus.PUBLISHED)
//                .currentStep(5L)
//                .category(category)
//                .contactEmail("organizer@evoticket.local")
//                .contactPhone("0900000000")
//                .allowMultipleTicketTypesPerOrder(true)
//                .allowDiscountCode(false)
//                .allowResale(false)
//                .build();
//
//        Showtime showtime = Showtime.builder()
//                .event(event)
//                .startDatetime(startDatetime)
//                .endDatetime(startDatetime.plusHours(3))
//                .venue(venue)
//                .address(address)
//                .province(province)
//                .ward(ward)
//                .isCancelled(false)
//                .build();
//
//        showtime.getTicketTypes().add(buildTicketType(
//                "Standard",
//                "General admission ticket",
//                basePrice,
//                150,
//                startDatetime,
//                showtime
//        ));
//        showtime.getTicketTypes().add(buildTicketType(
//                "Premium",
//                "Priority admission ticket",
//                basePrice.add(BigDecimal.valueOf(700)).min(BigDecimal.valueOf(4900)),
//                50,
//                startDatetime,
//                showtime
//        ));
//
//        event.getShowtimes().add(showtime);
//        return event;
//    }
//
//    private TicketType buildTicketType(
//            String typeName,
//            String description,
//            BigDecimal price,
//            Integer quantityTotal,
//            LocalDateTime eventStartDatetime,
//            Showtime showtime
//    ) {
//        return TicketType.builder()
//                .typeName(typeName)
//                .description(description)
//                .price(price)
//                .quantityTotal(quantityTotal)
//                .quantitySold(0)
//                .minPurchase(1)
//                .maxPurchase(4)
//                .saleStartDate(LocalDateTime.now().minusDays(1))
//                .saleEndDate(eventStartDatetime.minusHours(2))
//                .ticketTypeStatus(TicketTypeStatus.AVAILABLE)
//                .showtime(showtime)
//                .build();
//    }
//}
