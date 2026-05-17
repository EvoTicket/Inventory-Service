package com.capstone.inventoryservice.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "banks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {
    @Id
    private Integer id;

    private String name;
    private String code;
    private String bin;
    private String shortName;
    private String logo;
    private Integer transferSupported;
    private Integer lookupSupported;
    private Integer support;
    private Integer isTransfer;
    private String swiftCode;
}
