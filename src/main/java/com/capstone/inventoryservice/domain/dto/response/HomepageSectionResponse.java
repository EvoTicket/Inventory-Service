package com.capstone.inventoryservice.domain.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomepageSectionResponse {
    private String title;
    private String sectionId;
    private List<ListEventResponse> events;
}
