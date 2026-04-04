package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomepageResponse {
    private List<HomepageSectionResponse> sections;
}
