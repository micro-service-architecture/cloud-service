package com.boot.catalog.service;

import com.boot.catalog.entity.CatalogEntity;

public interface CatalogService {
    Iterable<CatalogEntity> getAllCatalogs();
}
