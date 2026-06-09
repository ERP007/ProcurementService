package org.fallguys.procurementservice.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorJpaDao extends JpaRepository<VendorEntity, String> {
    List<VendorEntity> findAllByActiveTrue();
    List<VendorEntity> findAllByActiveTrueAndNameContaining(String name);
}
