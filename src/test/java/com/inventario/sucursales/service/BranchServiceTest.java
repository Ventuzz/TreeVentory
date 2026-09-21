package com.inventario.sucursales.service;

import com.inventario.sucursales.entity.Branch;
import com.inventario.sucursales.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchService branchService;

    private Branch branch1;

    @BeforeEach
    void setUp() {
        branch1 = new Branch(1L, "SUC-01", "CDMX Norte", "CDMX", "CDMX", "Insurgentes", "555", true);
    }

    @Test
    void testGetAllBranches() {
        when(branchRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(branch1));

        List<Branch> list = branchService.getAllBranches();

        assertEquals(1, list.size());
        assertEquals("SUC-01", list.get(0).getCode());
    }

    @Test
    void testGetBranchByIdFound() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));

        Branch found = branchService.getBranchById(1L);

        assertNotNull(found);
        assertEquals("CDMX Norte", found.getName());
    }

    @Test
    void testGetBranchByIdNotFound() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> branchService.getBranchById(99L));
    }

    @Test
    void testCreateBranchSuccess() {
        Branch newBranch = new Branch(null, "SUC-02", "CDMX Sur", "CDMX", "CDMX", "Universidad", "555", true);
        when(branchRepository.findByCode("SUC-02")).thenReturn(Optional.empty());
        when(branchRepository.save(newBranch)).thenReturn(newBranch);

        Branch saved = branchService.createBranch(newBranch);

        assertNotNull(saved);
        assertTrue(saved.isActive());
        verify(branchRepository).save(newBranch);
    }

    @Test
    void testCreateBranchDuplicateCode() {
        Branch dupBranch = new Branch(null, "SUC-01", "CDMX Repetida", "CDMX", "CDMX", "Dir", "555", true);
        when(branchRepository.findByCode("SUC-01")).thenReturn(Optional.of(branch1));

        assertThrows(IllegalArgumentException.class, () -> branchService.createBranch(dupBranch));
    }

    @Test
    void testUpdateBranch() {
        Branch updatedData = new Branch(null, "SUC-01", "CDMX Norte Actualizada", "CDMX", "CDMX", "Nueva Dir", "999", true);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));
        when(branchRepository.save(any(Branch.class))).thenAnswer(i -> i.getArgument(0));

        Branch result = branchService.updateBranch(1L, updatedData);

        assertEquals("CDMX Norte Actualizada", result.getName());
        assertEquals("Nueva Dir", result.getAddress());
    }

    @Test
    void testDeleteBranch() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch1));
        when(branchRepository.save(any(Branch.class))).thenAnswer(i -> i.getArgument(0));

        branchService.deleteBranch(1L);

        assertFalse(branch1.isActive());
        verify(branchRepository).save(branch1);
    }
}
