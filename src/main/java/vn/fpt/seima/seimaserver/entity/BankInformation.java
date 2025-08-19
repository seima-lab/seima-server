package vn.fpt.seima.seimaserver.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "bank_information")
public class BankInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_id")
    private Integer bankId;

    @Column(name = "bank_code", length = 255, nullable = false)
    private String bankCode;

    @Column(name = "bank_logo_url", length = 512)
    private String bankLogoUrl;
}
