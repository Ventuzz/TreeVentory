package com.inventario.sucursales.config;

import com.inventario.sucursales.entity.*;
import com.inventario.sucursales.repository.BranchRepository;
import com.inventario.sucursales.repository.InventoryRepository;
import com.inventario.sucursales.repository.ProductRepository;
import com.inventario.sucursales.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// # Inicializador de datos maestros (sucursales, catalogo, usuarios e inventario inicial)
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(BranchRepository branchRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           InventoryRepository inventoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (branchRepository.count() > 0) {
            logger.info("La base de datos ya contiene registros. Omitiendo inicializacion inicial.");
            return;
        }

        logger.info("Iniciando precarga de datos: 16 sucursales, productos, usuarios y stock inicial...");

        // # Inicialización del catálogo de sucursales en territorio nacional
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch(null, "SUC-01", "CDMX Centro", "Ciudad de México", "CDMX", "Av. Juárez 123, Centro Histórico", "55-1001-0001", true));
        branches.add(new Branch(null, "SUC-02", "CDMX Norte", "Ciudad de México", "CDMX", "Av. Insurgentes Norte 450, Gustavo A. Madero", "55-1001-0002", true));
        branches.add(new Branch(null, "SUC-03", "CDMX Sur", "Ciudad de México", "CDMX", "Av. Universidad 1800, Coyoacán", "55-1001-0003", true));
        branches.add(new Branch(null, "SUC-04", "CDMX Oriente", "Ciudad de México", "CDMX", "Calz. Ignacio Zaragoza 800", "55-1001-0004", true));
        branches.add(new Branch(null, "SUC-05", "Guadalajara Centro", "Guadalajara", "Jalisco", "Av. Juárez 450, Col. Centro", "33-3333-0105", true));
        branches.add(new Branch(null, "SUC-06", "Guadalajara Zapopan", "Zapopan", "Jalisco", "Av. Américas 1500", "33-3333-0106", true));
        branches.add(new Branch(null, "SUC-07", "Monterrey Centro", "Monterrey", "Nuevo León", "Av. Constitución 400", "81-8181-0107", true));
        branches.add(new Branch(null, "SUC-08", "Monterrey San Pedro", "San Pedro Garza García", "Nuevo León", "Calzada del Valle 320", "81-8181-0108", true));
        branches.add(new Branch(null, "SUC-09", "Puebla Angelópolis", "Puebla", "Puebla", "Boulevard del Niño Poblano 2510", "22-2222-0109", true));
        branches.add(new Branch(null, "SUC-10", "Tijuana Río", "Tijuana", "Baja California", "Paseo de los Héroes 95, Zona Río", "66-4664-0110", true));
        branches.add(new Branch(null, "SUC-11", "Cancún Bonampak", "Cancún", "Quintana Roo", "Av. Bonampak Sm 4, Mz 1", "99-8888-0111", true));
        branches.add(new Branch(null, "SUC-12", "Mérida Altabrisa", "Mérida", "Yucatán", "Calle 7 No. 451, Col. Altabrisa", "99-9999-0112", true));
        branches.add(new Branch(null, "SUC-13", "León Poliforum", "León", "Guanajuato", "Blvd. Adolfo López Mateos 1820", "47-7477-0113", true));
        branches.add(new Branch(null, "SUC-14", "Querétaro Quintana", "Santiago de Querétaro", "Querétaro", "Blvd. Bernardo Quintana 4000", "44-2424-0114", true));
        branches.add(new Branch(null, "SUC-15", "Hermosillo Kino", "Hermosillo", "Sonora", "Blvd. Francisco Eusebio Kino 300", "66-2662-0115", true));
        branches.add(new Branch(null, "SUC-16", "Chihuahua Centro", "Chihuahua", "Chihuahua", "Av. Universidad 2100", "61-4444-0116", true));

        List<Branch> savedBranches = branchRepository.saveAll(branches);
        logger.info("16 sucursales registradas exitosamente.");

        // # Precarga del catálogo de productos base con stock mínimo
        List<Product> products = new ArrayList<>();
        products.add(new Product(null, "ELC-001", "Laptop Dell Inspiron 15", "Laptop empresarial 16GB RAM SSD 512GB", "Electrónicos", new BigDecimal("12500.00"), "pza", 5, true));
        products.add(new Product(null, "ELC-002", "Monitor Samsung 24\"", "Panel FHD antirreflejo HDMI", "Electrónicos", new BigDecimal("4800.00"), "pza", 5, true));
        products.add(new Product(null, "ELC-003", "Teclado Mecánico Logitech", "Switches mecánicos táctiles en español", "Accesorios", new BigDecimal("1800.00"), "pza", 10, true));
        products.add(new Product(null, "ELC-004", "Mouse Inalámbrico Logitech", "Conectividad Bluetooth y USB 2.4GHz", "Accesorios", new BigDecimal("650.00"), "pza", 10, true));
        products.add(new Product(null, "ELC-005", "Audífonos Bluetooth Sony", "Cancelación de ruido activa", "Electrónicos", new BigDecimal("2200.00"), "pza", 8, true));
        products.add(new Product(null, "ELC-006", "Tablet Samsung Galaxy A8", "Pantalla 10.5 pulgadas 64GB", "Electrónicos", new BigDecimal("7500.00"), "pza", 5, true));
        products.add(new Product(null, "ELC-007", "Impresora HP LaserJet Pro", "Láser monocromática de alta velocidad", "Electrónicos", new BigDecimal("3200.00"), "pza", 3, true));
        products.add(new Product(null, "ELC-008", "Webcam Logitech C920", "Full HD 1080p con micrófono estéreo", "Accesorios", new BigDecimal("950.00"), "pza", 8, true));
        products.add(new Product(null, "ELC-009", "USB Hub 7 Puertos Anker", "Puertos USB 3.0 con alimentación propia", "Accesorios", new BigDecimal("380.00"), "pza", 15, true));
        products.add(new Product(null, "ELC-010", "Cable HDMI 4K 2m", "Conectores dorados alta velocidad", "Accesorios", new BigDecimal("180.00"), "pza", 20, true));
        products.add(new Product(null, "MOB-001", "Silla Ergonómica Pro", "Soporte lumbar ajustable y cabecera", "Mobiliario", new BigDecimal("5500.00"), "pza", 3, true));
        products.add(new Product(null, "MOB-002", "Escritorio Standing Desk", "Estructura metálica motorizada regulable", "Mobiliario", new BigDecimal("8200.00"), "pza", 2, true));
        products.add(new Product(null, "MOB-003", "Lámpara LED de Escritorio", "Temperatura de color regulable", "Mobiliario", new BigDecimal("420.00"), "pza", 10, true));
        products.add(new Product(null, "ALM-001", "Power Bank 20000mAh Anker", "Carga rápida PowerIQ 3.0", "Accesorios", new BigDecimal("850.00"), "pza", 12, true));
        products.add(new Product(null, "ALM-002", "Memoria USB 64GB Kingston", "USB 3.2 Gen 1 ultracompacta", "Accesorios", new BigDecimal("160.00"), "pza", 25, true));
        products.add(new Product(null, "ALM-003", "Tarjeta MicroSD 128GB", "Clase 10 U3 V30 para 4K", "Accesorios", new BigDecimal("290.00"), "pza", 20, true));

        List<Product> savedProducts = productRepository.saveAll(products);
        logger.info("Catálogo de productos inicializado.");

        // # Configuración de cuentas iniciales para administradores y gerentes
        List<User> users = new ArrayList<>();
        // Administrador Corporativo (Roberto Vargas)
        users.add(new User(null, "admin", passwordEncoder.encode("admin123"), "Roberto Vargas", "r.vargas@corp.mx", Role.ROLE_ADMIN, null, "Director de Operaciones", "55-1111-2222", true));

        // Gerentes de Sucursal
        users.add(new User(null, "gerente_cdmx", passwordEncoder.encode("gerente123"), "Laura Mendoza", "l.mendoza@corp.mx", Role.ROLE_GERENTE, savedBranches.get(0), "Gerente de Sucursal", "55-1001-0001", true));
        users.add(new User(null, "gerente_mty", passwordEncoder.encode("gerente123"), "Roberto Garza Salinas", "r.garza@corp.mx", Role.ROLE_GERENTE, savedBranches.get(6), "Gerente de Sucursal", "81-8181-0107", true));
        users.add(new User(null, "gerente_gdl", passwordEncoder.encode("gerente123"), "Diana Reyes", "d.reyes@corp.mx", Role.ROLE_GERENTE, savedBranches.get(4), "Gerente de Sucursal", "33-3333-0105", true));
        users.add(new User(null, "gerente_tij", passwordEncoder.encode("gerente123"), "Patricia Meza", "p.meza@corp.mx", Role.ROLE_GERENTE, savedBranches.get(9), "Gerente de Sucursal", "66-4664-0110", true));

        // Empleados de Sucursal (visibles y editables por Admin)
        users.add(new User(null, "jcastillo", passwordEncoder.encode("empleado123"), "Jorge Castillo", "j.castillo@corp.mx", Role.ROLE_GERENTE, savedBranches.get(0), "Encargado de Almacén", "55-1001-1002", true));
        users.add(new User(null, "mtorres", passwordEncoder.encode("empleado123"), "María Torres", "m.torres@corp.mx", Role.ROLE_GERENTE, savedBranches.get(0), "Vendedor", "55-1001-1003", true));
        users.add(new User(null, "aflores", passwordEncoder.encode("empleado123"), "Andrés Flores", "a.flores@corp.mx", Role.ROLE_GERENTE, savedBranches.get(1), "Gerente de Sucursal", "55-2001-1001", true));
        users.add(new User(null, "cramirez", passwordEncoder.encode("empleado123"), "Claudia Ramírez", "c.ramirez@corp.mx", Role.ROLE_GERENTE, savedBranches.get(1), "Encargado de Almacén", "55-2001-1002", true));
        users.add(new User(null, "hmorales", passwordEncoder.encode("empleado123"), "Héctor Morales", "h.morales@corp.mx", Role.ROLE_GERENTE, savedBranches.get(1), "Vendedor", "55-2001-1003", true));
        users.add(new User(null, "slopez", passwordEncoder.encode("empleado123"), "Sofía López", "s.lopez@corp.mx", Role.ROLE_GERENTE, savedBranches.get(2), "Gerente de Sucursal", "55-3001-1001", true));
        users.add(new User(null, "rjimenez", passwordEncoder.encode("empleado123"), "Raúl Jiménez", "r.jimenez@corp.mx", Role.ROLE_GERENTE, savedBranches.get(2), "Encargado de Almacén", "55-3001-1002", true));
        users.add(new User(null, "vcruz", passwordEncoder.encode("empleado123"), "Valeria Cruz", "v.cruz@corp.mx", Role.ROLE_GERENTE, savedBranches.get(3), "Gerente de Sucursal", "55-4001-1001", true));

        userRepository.saveAll(users);
        logger.info("Usuarios iniciales creados (admin, gerentes y trabajadores).");

        // # Asignación de stock inicial distribuido entre sucursales
        // Configuramos stock variado, incluyendo stock bajo (< minStockThreshold) en algunas sucursales para alertas visuales inmediatas
        List<Inventory> inventories = new ArrayList<>();
        for (int i = 0; i < savedBranches.size(); i++) {
            Branch b = savedBranches.get(i);
            for (int j = 0; j < savedProducts.size(); j++) {
                Product p = savedProducts.get(j);
                int stock;
                if (i == 0) {
                    // En Sucursal CDMX Centro (Sucursal de la Gerente Laura Mendoza)
                    switch (j) {
                        case 0: stock = 0; break;   // Laptop Dell: 0 (min 5) -> Sin stock
                        case 1: stock = 33; break;  // Monitor Samsung: 33 (min 5) -> Normal
                        case 2: stock = 25; break;  // Teclado: 25 (min 10) -> Normal
                        case 3: stock = 11; break;  // Mouse: 11 (min 10) -> Normal/Bajo
                        case 4: stock = 3; break;   // Audífonos: 3 (min 8) -> Crítico
                        case 5: stock = 34; break;  // Tablet: 34 (min 5) -> Normal
                        case 6: stock = 18; break;  // Impresora: 18 (min 3) -> Normal
                        case 7: stock = 20; break;  // Webcam: 20 (min 8) -> Normal
                        case 8: stock = 6; break;   // USB Hub: 6 (min 15) -> Crítico
                        case 9: stock = 140; break; // Cable HDMI: 140 (min 20) -> Normal
                        case 10: stock = 18; break; // Silla: 18 (min 3) -> Normal
                        case 11: stock = 2; break;  // Escritorio: 2 (min 2) -> Bajo
                        case 12: stock = 8; break;  // Lámpara: 8 (min 10) -> Bajo
                        case 13: stock = 15; break; // Power Bank: 15 (min 12) -> Normal
                        case 14: stock = 22; break; // Memoria USB: 22 (min 25) -> Bajo
                        case 15: stock = 0; break;  // MicroSD: 0 (min 20) -> Sin stock
                        default: stock = 30;
                    }
                } else if (i == 1 && (j == 0 || j == 6)) {
                    // En CDMX Norte
                    stock = (j == 0) ? 12 : 0;
                } else {
                    // Stock normal suficiente en otras sucursales
                    stock = 20 + ((i * 7 + j * 5) % 50);
                }
                inventories.add(new Inventory(null, b, p, stock, LocalDateTime.now()));
            }
        }
        inventoryRepository.saveAll(inventories);
        logger.info("Inventario inicial de 256 registros distribuido entre las 16 sucursales cargado con exito.");
    }
}
