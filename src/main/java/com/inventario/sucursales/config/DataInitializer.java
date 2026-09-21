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

        // 1. Inicializar las 16 Sucursales
        List<Branch> branches = new ArrayList<>();
        branches.add(new Branch(null, "SUC-01", "Sucursal CDMX Norte", "Ciudad de México", "CDMX", "Av. Insurgentes Norte 1200, Col. Lindavista", "55-5555-0101", true));
        branches.add(new Branch(null, "SUC-02", "Sucursal CDMX Sur", "Ciudad de México", "CDMX", "Av. Universidad 1800, Col. Coyoacán", "55-5555-0102", true));
        branches.add(new Branch(null, "SUC-03", "Sucursal Guadalajara Centro", "Guadalajara", "Jalisco", "Av. Juárez 450, Col. Centro", "33-3333-0103", true));
        branches.add(new Branch(null, "SUC-04", "Sucursal Monterrey Valle", "San Pedro Garza García", "Nuevo León", "Calzada del Valle 320, Col. del Valle", "81-8181-0104", true));
        branches.add(new Branch(null, "SUC-05", "Sucursal Puebla Angelópolis", "Puebla", "Puebla", "Boulevard del Niño Poblano 2510", "22-2222-0105", true));
        branches.add(new Branch(null, "SUC-06", "Sucursal Querétaro Quintana", "Santiago de Querétaro", "Querétaro", "Blvd. Bernardo Quintana 4000", "44-2424-0106", true));
        branches.add(new Branch(null, "SUC-07", "Sucursal Tijuana Río", "Tijuana", "Baja California", "Paseo de los Héroes 95, Zona Urbana Río", "66-4664-0107", true));
        branches.add(new Branch(null, "SUC-08", "Sucursal Mérida Altabrisa", "Mérida", "Yucatán", "Calle 7 No. 451, Col. Altabrisa", "99-9999-0108", true));
        branches.add(new Branch(null, "SUC-09", "Sucursal Cancún Bonampak", "Cancún", "Quintana Roo", "Av. Bonampak Sm 4, Mz 1", "99-8888-0109", true));
        branches.add(new Branch(null, "SUC-10", "Sucursal León Poliforum", "León", "Guanajuato", "Blvd. Adolfo López Mateos 1820", "47-7477-0110", true));
        branches.add(new Branch(null, "SUC-11", "Sucursal Toluca Metepec", "Metepec", "Estado de México", "Av. Benito Juárez García 900", "72-2722-0111", true));
        branches.add(new Branch(null, "SUC-12", "Sucursal Veracruz Boca del Río", "Boca del Río", "Veracruz", "Blvd. Manuel Ávila Camacho 200", "22-9229-0112", true));
        branches.add(new Branch(null, "SUC-13", "Sucursal Cd. Juárez Pronaf", "Ciudad Juárez", "Chihuahua", "Av. Lincoln 1100, Zona Pronaf", "65-6656-0113", true));
        branches.add(new Branch(null, "SUC-14", "Sucursal San Luis Tangamanga", "San Luis Potosí", "San Luis Potosí", "Av. Salvador Nava 3105", "44-4444-0114", true));
        branches.add(new Branch(null, "SUC-15", "Sucursal Hermosillo Kino", "Hermosillo", "Sonora", "Blvd. Francisco Eusebio Kino 300", "66-2662-0115", true));
        branches.add(new Branch(null, "SUC-16", "Sucursal Culiacán Tres Ríos", "Culiacán", "Sinaloa", "Blvd. Enrique Sánchez Alonso 2100", "66-7667-0116", true));

        List<Branch> savedBranches = branchRepository.saveAll(branches);
        logger.info("16 sucursales registradas exitosamente.");

        // 2. Inicializar Productos con categorías y umbrales mínimos
        List<Product> products = new ArrayList<>();
        products.add(new Product(null, "PROD-LAP-001", "Laptop Pro 15.6 FHD Core i7", "Laptop empresarial 16GB RAM SSD 512GB", "Electrónica", new BigDecimal("18500.00"), "Pza", 5, true));
        products.add(new Product(null, "PROD-MOU-002", "Mouse Inalámbrico Ergonómico", "Conectividad Bluetooth y USB 2.4GHz", "Electrónica", new BigDecimal("450.00"), "Pza", 15, true));
        products.add(new Product(null, "PROD-TEC-003", "Teclado Mecánico Retroiluminado", "Switches mecánicos táctiles en español", "Electrónica", new BigDecimal("980.00"), "Pza", 10, true));
        products.add(new Product(null, "PROD-MON-004", "Monitor IPS 24 Pulgadas 75Hz", "Panel antirreflejo HDMI y DisplayPort", "Electrónica", new BigDecimal("2750.00"), "Pza", 8, true));
        products.add(new Product(null, "PROD-CAF-005", "Café Gourmet de Altura 1Kg", "Café en grano tostado medio de Chiapas", "Alimentos", new BigDecimal("320.00"), "Bolsa", 20, true));
        products.add(new Product(null, "PROD-ACE-006", "Aceite de Oliva Extra Virgen 1L", "Primera prensada en frío importado", "Alimentos", new BigDecimal("260.00"), "Botella", 15, true));
        products.add(new Product(null, "PROD-SIL-007", "Silla Ejecutiva Ergonómica Malla", "Soporte lumbar ajustable y cabecera", "Mobiliario", new BigDecimal("3400.00"), "Pza", 6, true));
        products.add(new Product(null, "PROD-ESC-008", "Escritorio Elevable Ajustable 120cm", "Estructura metálica motorizada", "Mobiliario", new BigDecimal("6200.00"), "Pza", 4, true));
        products.add(new Product(null, "PROD-HOJ-009", "Paquete Hojas Blancas Carta 500h", "Papel bond 75g de alta blancura", "Papelería", new BigDecimal("125.00"), "Paquete", 25, true));
        products.add(new Product(null, "PROD-DES-010", "Kit Desinfectante Multiusos 5L", "Solución antiséptica con dispensador", "Limpieza", new BigDecimal("190.00"), "Bidón", 15, true));

        List<Product> savedProducts = productRepository.saveAll(products);
        logger.info("Catálogo de 10 productos inicializado.");

        // 3. Inicializar Usuarios y Roles
        List<User> users = new ArrayList<>();
        // Administrador Global
        users.add(new User(null, "admin", passwordEncoder.encode("admin123"), "Carlos Mendoza - Administrador Corporativo", "admin@empresa.com", Role.ROLE_ADMIN, null, "Director General de Operaciones", "55-1111-2222", true));

        // Gerentes de Sucursal
        users.add(new User(null, "gerente_cdmx", passwordEncoder.encode("gerente123"), "Ana Laura Morales", "gerente.cdmx@empresa.com", Role.ROLE_GERENTE, savedBranches.get(0), "Gerente de Sucursal CDMX Norte", "55-2222-3333", true));
        users.add(new User(null, "gerente_mty", passwordEncoder.encode("gerente123"), "Roberto Garza Salinas", "gerente.mty@empresa.com", Role.ROLE_GERENTE, savedBranches.get(3), "Gerente de Sucursal Monterrey", "81-3333-4444", true));
        users.add(new User(null, "gerente_gdl", passwordEncoder.encode("gerente123"), "Mariana Flores López", "gerente.gdl@empresa.com", Role.ROLE_GERENTE, savedBranches.get(2), "Gerente de Sucursal Guadalajara", "33-4444-5555", true));
        users.add(new User(null, "gerente_puebla", passwordEncoder.encode("gerente123"), "Javier Ramos Huerta", "gerente.puebla@empresa.com", Role.ROLE_GERENTE, savedBranches.get(4), "Gerente de Sucursal Puebla", "22-5555-6666", true));

        // Empleados de Sucursal (consultables por Admin)
        users.add(new User(null, "emp_cdmx_01", passwordEncoder.encode("empleado123"), "Luis Daniel Cruz", "lcruz@empresa.com", Role.ROLE_GERENTE, savedBranches.get(0), "Supervisor de Almacén", "55-9988-1122", true));
        users.add(new User(null, "emp_mty_01", passwordEncoder.encode("empleado123"), "Valeria Treviño", "vtrevino@empresa.com", Role.ROLE_GERENTE, savedBranches.get(3), "Especialista de Logística", "81-8877-3344", true));

        userRepository.saveAll(users);
        logger.info("Usuarios iniciales creados (admin y gerentes de sucursal).");

        // 4. Inicializar Inventario en las 16 Sucursales
        // Configuramos stock variado, incluyendo stock bajo (< minStockThreshold) en algunas sucursales para alertas visuales inmediatas
        List<Inventory> inventories = new ArrayList<>();
        for (int i = 0; i < savedBranches.size(); i++) {
            Branch b = savedBranches.get(i);
            for (int j = 0; j < savedProducts.size(); j++) {
                Product p = savedProducts.get(j);
                int stock;
                if (i == 0 && (j == 0 || j == 1 || j == 4)) {
                    // En Sucursal CDMX Norte, dejamos productos bajo el umbral mínimo para disparar alertas visuales inmediatas
                    stock = (j == 0) ? 2 : (j == 1 ? 0 : 5); // Laptop: 2 (min 5), Mouse: 0 (min 15), Café: 5 (min 20)
                } else if (i == 3 && (j == 6 || j == 8)) {
                    // En Monterrey, stock bajo en Sillas y Hojas
                    stock = (j == 6) ? 1 : 4;
                } else {
                    // Stock normal suficiente en otras sucursales
                    stock = 30 + (i * 3) + (j * 2);
                }
                inventories.add(new Inventory(null, b, p, stock, LocalDateTime.now()));
            }
        }
        inventoryRepository.saveAll(inventories);
        logger.info("Inventario inicial de 160 registros distribuido entre las 16 sucursales cargado con exito.");
    }
}
