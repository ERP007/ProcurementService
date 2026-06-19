package org.fallguys.procurementservice.domain.model.vendor;

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

}
