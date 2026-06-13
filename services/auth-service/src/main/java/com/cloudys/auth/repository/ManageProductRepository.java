package com.cloudys.auth.repository;

import com.cloudys.auth.entity.ManageProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManageProductRepository extends JpaRepository<ManageProduct, String> {

    List<ManageProduct> findByStatus(String status);
}
