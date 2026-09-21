-- Script DDL de inicialización para MySQL 8
-- Base de Datos: inventario_db

CREATE DATABASE IF NOT EXISTS inventario_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE inventario_db;

-- Tabla de Sucursales (16 sucursales)
CREATE TABLE IF NOT EXISTS branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    address VARCHAR(200),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Tabla de Usuarios / Empleados
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    branch_id BIGINT,
    position VARCHAR(50),
    phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Tabla de Productos Centralizados
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    unit VARCHAR(20) DEFAULT 'Pza',
    min_stock_threshold INT NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Tabla de Inventario por Sucursal
CREATE TABLE IF NOT EXISTS inventories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_branch_product UNIQUE (branch_id, product_id),
    CONSTRAINT fk_inventories_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventories_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla de Solicitudes de Traslado y Pedidos a Proveedor
CREATE TABLE IF NOT EXISTS inventory_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_type VARCHAR(20) NOT NULL, -- 'TRANSFER' o 'SUPPLIER'
    origin_branch_id BIGINT, -- NULL si es proveedor
    destination_branch_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requester_id BIGINT NOT NULL,
    reviewer_id BIGINT,
    notes VARCHAR(500),
    admin_comments VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    CONSTRAINT fk_req_origin FOREIGN KEY (origin_branch_id) REFERENCES branches(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_dest FOREIGN KEY (destination_branch_id) REFERENCES branches(id) ON DELETE CASCADE,
    CONSTRAINT fk_req_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_req_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_req_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;
