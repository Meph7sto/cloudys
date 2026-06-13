package com.cloudys.requirementanalysis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cloudys.requirementanalysis.entity.BundleItem;

public interface BundleItemRepository extends JpaRepository<BundleItem, Long> {

    List<BundleItem> findByBundleIdOrderByOrderIndexAsc(String bundleId);

    Optional<BundleItem> findByBundleIdAndSpanRef(String bundleId, String spanRef);

    Optional<BundleItem> findByBundleIdAndSpanId(String bundleId, String spanId);
}
