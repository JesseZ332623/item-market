package com.example.jesse.item_market.persistence.repos;

import com.example.jesse.item_market.persistence.entities.Inventory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/** 用户包裹实体仓储类。*/
@Repository
public interface InventoryRepository
    extends R2dbcRepository<Inventory, Long> {}
