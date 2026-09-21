package com.inventario.sucursales.service;

import com.inventario.sucursales.dto.CreateRequestDto;
import com.inventario.sucursales.dto.ReviewRequestDto;
import com.inventario.sucursales.entity.*;
import com.inventario.sucursales.repository.BranchRepository;
import com.inventario.sucursales.repository.InventoryRequestRepository;
import com.inventario.sucursales.repository.ProductRepository;
import com.inventario.sucursales.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// # Servicio para procesamiento de solicitudes de traspaso inter-sucursal y pedidos a proveedor
@Service
public class RequestService {

    private final InventoryRequestRepository requestRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    public RequestService(InventoryRequestRepository requestRepository,
                          BranchRepository branchRepository,
                          ProductRepository productRepository,
                          UserRepository userRepository,
                          InventoryService inventoryService) {
        this.requestRepository = requestRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<InventoryRequest> getAllRequests(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        if (user.getRole() == Role.ROLE_ADMIN) {
            return requestRepository.findAllByOrderByCreatedAtDesc();
        } else {
            // # Los gerentes solo consultan las solicitudes vinculadas a su sucursal
            if (user.getBranch() == null) {
                return List.of();
            }
            return requestRepository.findByBranchInvolved(user.getBranch().getId());
        }
    }

    @Transactional(readOnly = true)
    public InventoryRequest getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada con id: " + id));
    }

    @Transactional
    public InventoryRequest createRequest(CreateRequestDto dto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        Branch destinationBranch = branchRepository.findById(dto.getDestinationBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Sucursal de destino no valida: " + dto.getDestinationBranchId()));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no valido: " + dto.getProductId()));

        Branch originBranch = null;
        if (dto.getRequestType() == RequestType.TRANSFER) {
            if (dto.getOriginBranchId() == null) {
                throw new IllegalArgumentException("Para traslados entre sucursales se debe indicar la sucursal de origen");
            }
            if (dto.getOriginBranchId().equals(dto.getDestinationBranchId())) {
                throw new IllegalArgumentException("La sucursal de origen no puede ser la misma que la sucursal destino");
            }
            originBranch = branchRepository.findById(dto.getOriginBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Sucursal de origen no valida: " + dto.getOriginBranchId()));

            // # Validación de stock suficiente en la sucursal de origen
            Inventory originStock = inventoryService.getInventoryByBranchAndProduct(originBranch.getId(), product.getId());
            if (originStock == null || originStock.getQuantity() < dto.getQuantity()) {
                int disponible = originStock != null ? originStock.getQuantity() : 0;
                throw new IllegalStateException("La sucursal de origen (" + originBranch.getName() +
                        ") no tiene suficiente stock. Disponible: " + disponible + ", Solicitado: " + dto.getQuantity());
            }
        }

        // # Si es Gerente, la sucursal destino debe ser su sucursal asignada
        if (user.getRole() == Role.ROLE_GERENTE) {
            if (user.getBranch() == null || !user.getBranch().getId().equals(destinationBranch.getId())) {
                throw new IllegalArgumentException("Como Gerente solo puede solicitar mercancia para su propia sucursal");
            }
        }

        // # Validación para evitar solicitudes pendientes duplicadas del mismo producto
        boolean alreadyPending = requestRepository.existsByDestinationBranchIdAndProductIdAndStatus(
                destinationBranch.getId(), product.getId(), RequestStatus.PENDING);
        if (alreadyPending) {
            throw new IllegalStateException("Ya existe una solicitud pendiente de reposición para el producto '" +
                    product.getName() + "' en la sucursal " + destinationBranch.getName());
        }

        InventoryRequest request = new InventoryRequest();
        request.setRequestType(dto.getRequestType());
        request.setOriginBranch(originBranch);
        request.setDestinationBranch(destinationBranch);
        request.setProduct(product);
        request.setQuantity(dto.getQuantity());
        request.setStatus(RequestStatus.PENDING);
        request.setRequester(user);
        request.setNotes(dto.getNotes());
        request.setCreatedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Transactional
    public InventoryRequest approveRequest(Long id, ReviewRequestDto reviewDto, String adminUsername) {
        InventoryRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden aprobar solicitudes en estado PENDIENTE. Estado actual: " + request.getStatus());
        }

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario revisor no encontrado: " + adminUsername));

        if (request.getRequestType() == RequestType.TRANSFER) {
            // # Ajuste atómico de inventario por traspaso (origen (-) y destino (+))
            inventoryService.adjustStock(request.getOriginBranch().getId(), request.getProduct().getId(), -request.getQuantity());
            inventoryService.adjustStock(request.getDestinationBranch().getId(), request.getProduct().getId(), request.getQuantity());
        } else if (request.getRequestType() == RequestType.SUPPLIER) {
            // # Ajuste de inventario por recepción de proveedor (destino (+))
            inventoryService.adjustStock(request.getDestinationBranch().getId(), request.getProduct().getId(), request.getQuantity());
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewer(admin);
        request.setAdminComments(reviewDto != null ? reviewDto.getAdminComments() : "Aprobado por Administrador");
        request.setResolvedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Transactional
    public InventoryRequest rejectRequest(Long id, ReviewRequestDto reviewDto, String adminUsername) {
        InventoryRequest request = getRequestById(id);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden rechazar solicitudes en estado PENDIENTE. Estado actual: " + request.getStatus());
        }

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario revisor no encontrado: " + adminUsername));

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewer(admin);
        request.setAdminComments(reviewDto != null && reviewDto.getAdminComments() != null ?
                reviewDto.getAdminComments() : "Rechazado por Administrador");
        request.setResolvedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }
}
