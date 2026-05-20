package com.capstone.inventoryservice.domain.dto.response;

import com.capstone.inventoryservice.model.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasicEventInfoDto {
    Long eventId;
    Long currentStep;

    public static BasicEventInfoDto convertToDTO(Event event) {
        return BasicEventInfoDto.builder()
                .eventId(event.getId())
                .currentStep(event.getCurrentStep())
                .build();
    }
}
