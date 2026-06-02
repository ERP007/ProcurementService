package org.fallguys.procurementservice.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.fallguys.procurementservice.domain.model.Vendor;

@Entity
@Table(name = "vendors")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VendorEntity {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active;

    public Vendor toDomain() {
        return new Vendor(code, name, contactPerson, phone, address, active);
    }

    public static VendorEntity from(Vendor vendor) {
        return new VendorEntity(
                vendor.getCode(),
                vendor.getName(),
                vendor.getContactPerson(),
                vendor.getPhone(),
                vendor.getAddress(),
                vendor.isActive()
        );
    }

    public VendorEntity update(Vendor vendor) {
        this.name = vendor.getName();
        this.contactPerson = vendor.getContactPerson();
        this.phone = vendor.getPhone();
        this.address = vendor.getAddress();
        this.active = vendor.isActive();
        return this;
    }
}
