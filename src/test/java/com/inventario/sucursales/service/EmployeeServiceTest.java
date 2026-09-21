package com.inventario.sucursales.service;

import com.inventario.sucursales.dto.EmployeeDto;
import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.entity.Role;
import com.inventario.sucursales.entity.User;
import com.inventario.sucursales.repository.BranchRepository;
import com.inventario.sucursales.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// # Pruebas unitarias para gestión de empleados, altas, bajas y cambios de sucursal
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    private User sampleUser;
    private Branch sampleBranch;

    @BeforeEach
    void setUp() {
        sampleBranch = new Branch(1L, "SUC-01", "CDMX Norte", "CDMX", "CDMX", "Dir", "555", true);
        sampleUser = new User(1L, "emp1", "encoded", "Juan Perez", "juan@test.com", Role.ROLE_GERENTE, sampleBranch, "Gerente", "123", true);
    }

    @Test
    void testGetAllEmployees() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<User> list = employeeService.getAllEmployees();

        assertEquals(1, list.size());
    }

    @Test
    void testGetEmployeesByBranch() {
        when(userRepository.findByBranchId(1L)).thenReturn(List.of(sampleUser));

        List<User> list = employeeService.getEmployeesByBranch(1L);

        assertEquals(1, list.size());
    }

    @Test
    void testGetEmployeeByIdFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User found = employeeService.getEmployeeById(1L);

        assertNotNull(found);
        assertEquals("emp1", found.getUsername());
    }

    @Test
    void testGetEmployeeByIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> employeeService.getEmployeeById(99L));
    }

    @Test
    void testCreateEmployeeSuccess() {
        EmployeeDto dto = new EmployeeDto(null, "emp2", "pass123", "Maria Lopez", "maria@test.com", Role.ROLE_GERENTE, 1L, "Gerente", "555", true);

        when(userRepository.existsByUsername("emp2")).thenReturn(false);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));
        when(passwordEncoder.encode("pass123")).thenReturn("encoded123");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User created = employeeService.createEmployee(dto);

        assertNotNull(created);
        assertEquals("emp2", created.getUsername());
        assertEquals("encoded123", created.getPassword());
    }

    @Test
    void testCreateEmployeeDuplicateUsernameThrows() {
        EmployeeDto dto = new EmployeeDto(null, "emp1", "pass", "Otro", "o@test.com", Role.ROLE_ADMIN, null, "Puesto", "123", true);
        when(userRepository.existsByUsername("emp1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(dto));
    }

    @Test
    void testCreateEmployeeGerenteWithoutBranchThrows() {
        EmployeeDto dto = new EmployeeDto(null, "gerente_nobranch", "pass", "Nombre", "email", Role.ROLE_GERENTE, null, "Puesto", "123", true);
        when(userRepository.existsByUsername("gerente_nobranch")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> employeeService.createEmployee(dto));
    }

    @Test
    void testUpdateEmployee() {
        EmployeeDto dto = new EmployeeDto(1L, "emp1", null, "Juan Modificado", "nuevo@test.com", Role.ROLE_GERENTE, 1L, "Subdirector", "999", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(sampleBranch));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = employeeService.updateEmployee(1L, dto);

        assertEquals("Juan Modificado", updated.getFullName());
        assertEquals("Subdirector", updated.getPosition());
    }

    @Test
    void testDeleteEmployee() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        employeeService.deleteEmployee(1L);

        assertFalse(sampleUser.isActive());
        verify(userRepository).save(sampleUser);
    }
}
