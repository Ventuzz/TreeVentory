package com.inventario.sucursales.controller;

import com.inventario.sucursales.dto.ApiResponse;
import com.inventario.sucursales.dto.EmployeeDto;
import com.inventario.sucursales.entity.User;
import com.inventario.sucursales.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// # Controlador REST para la gestión y aprovisionamiento de empleados por el Administrador
@RestController
@RequestMapping("/api/employees")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllEmployees() {
        List<User> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(ApiResponse.success("Empleados obtenidos con exito", employees));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<User>>> getEmployeesByBranch(@PathVariable Long branchId) {
        List<User> employees = employeeService.getEmployeesByBranch(branchId);
        return ResponseEntity.ok(ApiResponse.success("Empleados de la sucursal obtenidos", employees));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getEmployeeById(@PathVariable Long id) {
        User employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success("Empleado encontrado", employee));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createEmployee(@Valid @RequestBody EmployeeDto dto) {
        User created = employeeService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Empleado registrado exitosamente", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeDto dto) {
        User updated = employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Empleado actualizado exitosamente", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Empleado desactivado exitosamente"));
    }
}
