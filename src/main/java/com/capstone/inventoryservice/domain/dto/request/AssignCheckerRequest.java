package com.capstone.inventoryservice.domain.dto.request;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignCheckerRequest {
    private Long checkerId;
}
