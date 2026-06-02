package org.fallguys.procurementservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Vendor {

    private final String code;
    private String name;
    private String contactPerson;
    private String phone;
    private String address;
    private boolean active;

    public void update(String name, String contactPerson, String phone, String address) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.address = address;
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }
}
