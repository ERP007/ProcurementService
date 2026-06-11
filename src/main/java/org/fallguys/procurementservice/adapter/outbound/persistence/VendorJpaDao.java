package org.fallguys.procurementservice.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorJpaDao extends JpaRepository<VendorEntity, String> {
    List<VendorEntity> findAllByActiveTrue();
    List<VendorEntity> findAllByActiveTrueAndNameContaining(String name);
    Optional<VendorEntity> findByCodeAndActiveTrue(String code);
    List<VendorEntity> findAllByNameContaining(String name);
}
