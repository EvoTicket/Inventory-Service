package com.capstone.inventoryservice.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDraftStep2Request {

    @Valid
    @NotEmpty(message = "Sự kiện phải có ít nhất một suất diễn")
    private List<CreateShowtimeRequest> showtimes;

    @NotNull(message = "Tổng số ghế không được để trống")
    @Min(value = 1, message = "Tổng số ghế phải ít nhất là 1")
    private Integer totalSeats;
}
