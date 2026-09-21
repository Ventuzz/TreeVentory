// Estado de la aplicación
const API_BASE = '/api';
let currentUser = null;
let currentToken = null;
let cachedBranches = [];
let cachedProducts = [];

// Inicialización al cargar la página
document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    setupEventListeners();
});

// -------------------------------------------------------------
// GESTIÓN DE AUTENTICACIÓN Y JWT
// -------------------------------------------------------------
function initAuth() {
    currentToken = localStorage.getItem('jwt_token');
    const userJson = localStorage.getItem('user_info');
    if (currentToken && userJson) {
        try {
            currentUser = JSON.parse(userJson);
            showDashboard();
            return;
        } catch (e) {
            logout();
        }
    }
    showLogin();
}

function showLogin() {
    document.getElementById('loginSection').classList.remove('d-none');
    document.getElementById('dashboardSection').classList.add('d-none');
    document.getElementById('navUserInfo').classList.add('d-none');
}

function showDashboard() {
    document.getElementById('loginSection').classList.add('d-none');
    document.getElementById('dashboardSection').classList.remove('d-none');
    document.getElementById('navUserInfo').classList.remove('d-none');

    // Mostrar datos del usuario en la barra
    document.getElementById('navUserName').textContent = currentUser.fullName;
    document.getElementById('navUserRole').textContent = currentUser.role === 'ROLE_ADMIN' ? 'ADMINISTRADOR' : 'GERENTE';
    document.getElementById('navUserBranch').textContent = currentUser.branchName || 'Acceso Global';

    // Ajustar visibilidad según el rol
    const isAdmin = currentUser.role === 'ROLE_ADMIN';
    document.querySelectorAll('.admin-only').forEach(el => {
        el.style.display = isAdmin ? '' : 'none';
    });

    // Cargar catálogos y datos iniciales
    loadBranches().then(() => {
        // Si es gerente, preseleccionar su sucursal fija
        if (!isAdmin && currentUser.branchId) {
            const branchSelect = document.getElementById('inventoryBranchSelect');
            if (branchSelect) branchSelect.value = currentUser.branchId;
        }
        refreshDashboardData();
    });
}

async function login(username, password) {
    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();
        if (!response.ok || !data.success) {
            showNotification(data.message || 'Error al iniciar sesión', 'danger');
            return;
        }

        const loginData = data.data;
        currentToken = loginData.token;
        currentUser = {
            username: loginData.username,
            fullName: loginData.fullName,
            role: loginData.role,
            branchId: loginData.branchId,
            branchName: loginData.branchName
        };

        localStorage.setItem('jwt_token', currentToken);
        localStorage.setItem('user_info', JSON.stringify(currentUser));

        showNotification('Bienvenido, ' + currentUser.fullName, 'success');
        showDashboard();
    } catch (err) {
        showNotification('Error de conexión con el servidor', 'danger');
        console.error(err);
    }
}

function logout() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_info');
    currentToken = null;
    currentUser = null;
    showLogin();
    showNotification('Sesión finalizada', 'info');
}

// Helper para peticiones HTTP autenticadas con JWT
async function authFetch(url, options = {}) {
    options.headers = options.headers || {};
    if (currentToken) {
        options.headers['Authorization'] = `Bearer ${currentToken}`;
    }
    if (!options.headers['Content-Type'] && !(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(url, options);

    if (response.status === 401) {
        showNotification('Su sesión ha expirado o el token es inválido. Inicie sesión nuevamente.', 'warning');
        logout();
        throw new Error('Unauthorized');
    }

    if (response.status === 403) {
        showNotification('Acceso denegado: No cuenta con el rol requerido para esta acción.', 'danger');
        throw new Error('Forbidden');
    }

    return response;
}

// -------------------------------------------------------------
// CARGA DE DATOS Y COMPONENTES
// -------------------------------------------------------------
async function loadBranches() {
    try {
        const res = await authFetch(`${API_BASE}/branches`);
        const json = await res.json();
        if (json.success) {
            cachedBranches = json.data;
            populateBranchSelectors();
        }
    } catch (e) {
        console.error('Error cargando sucursales', e);
    }
}

function populateBranchSelectors() {
    const invSelect = document.getElementById('inventoryBranchSelect');
    const reqDestSelect = document.getElementById('reqDestinationBranch');
    const reqOrigSelect = document.getElementById('reqOriginBranch');
    const empBranchSelect = document.getElementById('empBranch');
    const empFilterSelect = document.getElementById('empFilterBranch');

    const optionsHtml = cachedBranches.map(b => `<option value="${b.id}">${b.code} - ${b.name} (${b.city})</option>`).join('');

    if (invSelect) invSelect.innerHTML = `<option value="">Todas las sucursales (Global)</option>` + optionsHtml;
    if (reqDestSelect) reqDestSelect.innerHTML = `<option value="">Seleccione sucursal destino...</option>` + optionsHtml;
    if (reqOrigSelect) reqOrigSelect.innerHTML = `<option value="">Seleccione sucursal de origen...</option>` + optionsHtml;
    if (empBranchSelect) empBranchSelect.innerHTML = `<option value="">Sin sucursal asignada (Corporativo)</option>` + optionsHtml;
    if (empFilterSelect) empFilterSelect.innerHTML = `<option value="">Todas las sucursales</option>` + optionsHtml;
}

async function refreshDashboardData() {
    loadProducts();
    loadInventory();
    loadLowStockAlerts();
    loadRequests();
    if (currentUser && currentUser.role === 'ROLE_ADMIN') {
        loadEmployees();
    }
}

// -------------------------------------------------------------
// ALERTAS VISUALES DE STOCK BAJO (REQUERIMIENTO CLAVE)
// -------------------------------------------------------------
async function loadLowStockAlerts() {
    try {
        let url = `${API_BASE}/inventory/alerts`;
        // Si es gerente y tiene sucursal, consultar las de su sucursal
        if (currentUser.role === 'ROLE_GERENTE' && currentUser.branchId) {
            url = `${API_BASE}/inventory/alerts/${currentUser.branchId}`;
        }

        const res = await authFetch(url);
        const json = await res.json();
        const alertsContainer = document.getElementById('visualAlertsContainer');
        const alertBadge = document.getElementById('metricAlertsCount');

        if (json.success && json.data) {
            const alerts = json.data;
            alertBadge.textContent = alerts.length;

            if (alerts.length === 0) {
                alertsContainer.innerHTML = `
                    <div class="alert alert-success d-flex align-items-center mb-0 py-2">
                        <i class="bi bi-check-circle-fill me-2 fs-5"></i>
                        <div><strong>Niveles Óptimos:</strong> No hay productos con alertas de stock bajo en este momento.</div>
                    </div>
                `;
                return;
            }

            let html = `
                <div class="alert alert-danger alert-pulse mb-3">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <h6 class="alert-heading mb-0 fw-bold">
                            <i class="bi bi-exclamation-triangle-fill text-danger me-2 fs-5"></i>
                            ¡Atención! Se detectaron ${alerts.length} producto(s) con inventario crítico o por debajo del mínimo permitido:
                        </h6>
                        <span class="badge bg-danger">${alerts.length} ALERTAS</span>
                    </div>
                    <div class="table-responsive bg-white rounded shadow-sm">
                        <table class="table table-sm table-hover align-middle mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Nivel</th>
                                    <th>Sucursal</th>
                                    <th>SKU</th>
                                    <th>Producto</th>
                                    <th>Stock Actual</th>
                                    <th>Mínimo</th>
                                    <th>Diagnóstico</th>
                                    <th class="text-end">Acción Rápida</th>
                                </tr>
                            </thead>
                            <tbody>
            `;

            alerts.forEach(a => {
                const isCritical = a.alertLevel === 'CRITICAL';
                const badgeClass = isCritical ? 'badge-stock-critical' : 'badge-stock-warning';
                const icon = isCritical ? 'bi-x-octagon-fill text-danger' : 'bi-exclamation-circle-fill text-warning';

                html += `
                    <tr>
                        <td><span class="badge ${badgeClass}"><i class="bi ${icon} me-1"></i>${a.alertLevel}</span></td>
                        <td><strong>${a.branchName}</strong></td>
                        <td><code>${a.productSku}</code></td>
                        <td>${a.productName}</td>
                        <td class="fw-bold ${isCritical ? 'text-danger' : 'text-warning'}">${a.currentStock}</td>
                        <td>${a.minStockThreshold}</td>
                        <td><small class="text-muted">${a.alertMessage}</small></td>
                        <td class="text-end">
                            <button class="btn btn-sm btn-outline-primary" onclick="openQuickRestockModal(${a.branchId}, ${a.productId})">
                                <i class="bi bi-box-arrow-in-down me-1"></i>Solicitar Surtido
                            </button>
                        </td>
                    </tr>
                `;
            });

            html += `
                            </tbody>
                        </table>
                    </div>
                </div>
            `;
            alertsContainer.innerHTML = html;
        }
    } catch (e) {
        console.error('Error cargando alertas de stock', e);
    }
}

// -------------------------------------------------------------
// INVENTARIO POR SUCURSAL
// -------------------------------------------------------------
async function loadInventory() {
    try {
        const branchSelect = document.getElementById('inventoryBranchSelect');
        const branchId = branchSelect ? branchSelect.value : '';
        let url = branchId ? `${API_BASE}/inventory/branch/${branchId}` : `${API_BASE}/inventory/all`;

        const res = await authFetch(url);
        const json = await res.json();
        const tbody = document.getElementById('inventoryTableBody');
        tbody.innerHTML = '';

        if (json.success && json.data) {
            const list = json.data;
            document.getElementById('metricTotalInventory').textContent = list.reduce((acc, curr) => acc + curr.quantity, 0);

            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">No hay registros de inventario para la selección.</td></tr>`;
                return;
            }

            list.forEach(inv => {
                const isCritical = inv.quantity === 0;
                const isWarning = inv.quantity <= inv.product.minStockThreshold;
                let badge = `<span class="badge badge-stock-ok"><i class="bi bi-check2 me-1"></i>Óptimo</span>`;
                if (isCritical) {
                    badge = `<span class="badge badge-stock-critical"><i class="bi bi-x-circle me-1"></i>Agotado</span>`;
                } else if (isWarning) {
                    badge = `<span class="badge badge-stock-warning"><i class="bi bi-exclamation-triangle me-1"></i>Bajo</span>`;
                }

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><strong>${inv.branch.code}</strong> - ${inv.branch.name}</td>
                    <td><code>${inv.product.sku}</code></td>
                    <td>${inv.product.name}</td>
                    <td><span class="badge bg-secondary">${inv.product.category}</span></td>
                    <td class="fw-bold fs-6 ${isCritical ? 'text-danger' : (isWarning ? 'text-warning' : 'text-success')}">${inv.quantity} ${inv.product.unit || 'Pza'}</td>
                    <td>${inv.product.minStockThreshold}</td>
                    <td>${badge}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-secondary me-1" title="Solicitar Traslado" onclick="openTransferModal(${inv.branch.id}, ${inv.product.id})">
                            <i class="bi bi-arrow-left-right"></i>
                        </button>
                        ${currentUser.role === 'ROLE_ADMIN' ? `
                            <button class="btn btn-sm btn-outline-primary" title="Ajuste Directo (Admin)" onclick="openStockAdjustModal(${inv.branch.id}, ${inv.product.id}, ${inv.quantity}, '${inv.product.name}')">
                                <i class="bi bi-pencil"></i>
                            </button>
                        ` : ''}
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error('Error cargando inventario', e);
    }
}

// -------------------------------------------------------------
// CATÁLOGO DE PRODUCTOS (CRUD)
// -------------------------------------------------------------
async function loadProducts() {
    try {
        const res = await authFetch(`${API_BASE}/products`);
        const json = await res.json();
        if (json.success) {
            cachedProducts = json.data;
            document.getElementById('metricTotalProducts').textContent = cachedProducts.length;
            renderProductTable();
            populateProductSelectors();
        }
    } catch (e) {
        console.error('Error cargando productos', e);
    }
}

function renderProductTable() {
    const tbody = document.getElementById('productsTableBody');
    tbody.innerHTML = '';
    const isAdmin = currentUser.role === 'ROLE_ADMIN';

    cachedProducts.forEach(p => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><code>${p.sku}</code></td>
            <td><strong>${p.name}</strong></td>
            <td><span class="badge bg-primary-subtle text-primary border">${p.category}</span></td>
            <td>$${p.price.toFixed(2)}</td>
            <td>${p.unit}</td>
            <td><span class="badge bg-light text-dark border">${p.minStockThreshold}</span></td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditProductModal(${p.id})">
                    <i class="bi bi-pencil"></i> Editar
                </button>
                ${isAdmin ? `
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteProduct(${p.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                ` : ''}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function populateProductSelectors() {
    const reqProductSelect = document.getElementById('reqProduct');
    if (reqProductSelect) {
        reqProductSelect.innerHTML = `<option value="">Seleccione producto...</option>` +
            cachedProducts.map(p => `<option value="${p.id}">${p.sku} - ${p.name} (Mín: ${p.minStockThreshold})</option>`).join('');
    }
}

// -------------------------------------------------------------
// GESTIÓN DE SOLICITUDES (TRASLADOS Y PROVEEDORES)
// -------------------------------------------------------------
async function loadRequests() {
    try {
        const res = await authFetch(`${API_BASE}/requests`);
        const json = await res.json();
        const tbody = document.getElementById('requestsTableBody');
        tbody.innerHTML = '';

        if (json.success && json.data) {
            const list = json.data;
            const pendingCount = list.filter(r => r.status === 'PENDING').length;
            document.getElementById('metricPendingRequests').textContent = pendingCount;

            if (list.length === 0) {
                tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-4">No hay solicitudes registradas.</td></tr>`;
                return;
            }

            const isAdmin = currentUser.role === 'ROLE_ADMIN';

            list.forEach(req => {
                let statusBadge = `<span class="badge bg-warning text-dark"><i class="bi bi-hourglass-split me-1"></i>PENDIENTE</span>`;
                if (req.status === 'APPROVED') {
                    statusBadge = `<span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>APROBADA</span>`;
                } else if (req.status === 'RECHAZADA') {
                    statusBadge = `<span class="badge bg-danger"><i class="bi bi-x-circle me-1"></i>RECHAZADA</span>`;
                }

                const typeBadge = req.requestType === 'TRANSFER'
                    ? `<span class="badge bg-info text-dark"><i class="bi bi-arrow-left-right me-1"></i>Traslado</span>`
                    : `<span class="badge bg-purple text-white" style="background:#8b5cf6;"><i class="bi bi-truck me-1"></i>Proveedor</span>`;

                const originText = req.originBranch ? `${req.originBranch.code} - ${req.originBranch.name}` : '<em class="text-muted">Proveedor Externo</em>';

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>#${req.id}</td>
                    <td>${typeBadge}</td>
                    <td>${originText}</td>
                    <td><strong>${req.destinationBranch.code}</strong> - ${req.destinationBranch.name}</td>
                    <td>${req.product.name}</td>
                    <td class="fw-bold">${req.quantity}</td>
                    <td><small>${req.requester.fullName}</small></td>
                    <td>${statusBadge}</td>
                    <td class="text-end">
                        ${isAdmin && req.status === 'PENDING' ? `
                            <button class="btn btn-sm btn-success me-1" onclick="approveRequest(${req.id})" title="Aprobar y transferir stock">
                                <i class="bi bi-check-lg"></i> Aprobar
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="rejectRequest(${req.id})" title="Rechazar solicitud">
                                <i class="bi bi-x-lg"></i> Rechazar
                            </button>
                        ` : `
                            <button class="btn btn-sm btn-outline-secondary" onclick="viewRequestDetails(${req.id})">
                                <i class="bi bi-eye"></i> Detalle
                            </button>
                        `}
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error('Error cargando solicitudes', e);
    }
}

async function approveRequest(id) {
    const comments = prompt('Comentario de aprobación (opcional):', 'Aprobado y procesado por administración.');
    if (comments === null) return; // Cancelado

    try {
        const res = await authFetch(`${API_BASE}/requests/${id}/approve`, {
            method: 'PUT',
            body: JSON.stringify({ adminComments: comments })
        });
        const json = await res.json();
        if (json.success) {
            showNotification('Solicitud #' + id + ' aprobada exitosamente. Se ha actualizado el inventario.', 'success');
            refreshDashboardData();
        } else {
            showNotification(json.message || 'Error al aprobar solicitud', 'danger');
        }
    } catch (e) {
        showNotification('Error al procesar aprobación', 'danger');
    }
}

async function rejectRequest(id) {
    const comments = prompt('Motivo del rechazo:', 'Stock insuficiente o solicitud rechazada por política operativa.');
    if (comments === null) return;

    try {
        const res = await authFetch(`${API_BASE}/requests/${id}/reject`, {
            method: 'PUT',
            body: JSON.stringify({ adminComments: comments })
        });
        const json = await res.json();
        if (json.success) {
            showNotification('Solicitud #' + id + ' rechazada.', 'warning');
            refreshDashboardData();
        } else {
            showNotification(json.message || 'Error al rechazar solicitud', 'danger');
        }
    } catch (e) {
        showNotification('Error al procesar rechazo', 'danger');
    }
}

// -------------------------------------------------------------
// GESTIÓN DE EMPLEADOS (ROL EXCLUSIVO ADMIN)
// -------------------------------------------------------------
async function loadEmployees() {
    try {
        const filterSelect = document.getElementById('empFilterBranch');
        const branchId = filterSelect ? filterSelect.value : '';
        const url = branchId ? `${API_BASE}/employees/branch/${branchId}` : `${API_BASE}/employees`;

        const res = await authFetch(url);
        const json = await res.json();
        const tbody = document.getElementById('employeesTableBody');
        tbody.innerHTML = '';

        if (json.success && json.data) {
            json.data.forEach(emp => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><strong>${emp.username}</strong></td>
                    <td>${emp.fullName}</td>
                    <td><span class="badge ${emp.role === 'ROLE_ADMIN' ? 'bg-danger' : 'bg-primary'}">${emp.role}</span></td>
                    <td>${emp.branch ? `${emp.branch.code} - ${emp.branch.name}` : '<em class="text-muted">Corporativo</em>'}</td>
                    <td>${emp.position || '-'}</td>
                    <td>${emp.email || '-'}</td>
                    <td><span class="badge ${emp.active ? 'bg-success' : 'bg-secondary'}">${emp.active ? 'Activo' : 'Inactivo'}</span></td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary me-1" onclick="openEditEmployeeModal(${emp.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        ${emp.active ? `
                            <button class="btn btn-sm btn-outline-danger" onclick="deactivateEmployee(${emp.id})">
                                <i class="bi bi-person-x"></i>
                            </button>
                        ` : ''}
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error('Error cargando empleados', e);
    }
}

// -------------------------------------------------------------
// ACCIONES RÁPIDAS Y MODALES
// -------------------------------------------------------------
function openQuickRestockModal(branchId, productId) {
    const modal = new bootstrap.Modal(document.getElementById('newRequestModal'));
    document.getElementById('reqRequestType').value = 'SUPPLIER';
    document.getElementById('reqDestinationBranch').value = branchId;
    document.getElementById('reqProduct').value = productId;
    document.getElementById('reqQuantity').value = 25;
    document.getElementById('reqNotes').value = 'Pedido urgente generado automáticamente por alerta de stock bajo';
    toggleOriginBranchVisibility();
    modal.show();
}

function openTransferModal(destBranchId, productId) {
    const modal = new bootstrap.Modal(document.getElementById('newRequestModal'));
    document.getElementById('reqRequestType').value = 'TRANSFER';
    document.getElementById('reqDestinationBranch').value = destBranchId;
    document.getElementById('reqProduct').value = productId;
    document.getElementById('reqQuantity').value = 10;
    document.getElementById('reqNotes').value = 'Solicitud de traspaso de existencias entre sucursales';
    toggleOriginBranchVisibility();
    modal.show();
}

function toggleOriginBranchVisibility() {
    const type = document.getElementById('reqRequestType').value;
    const originDiv = document.getElementById('originBranchGroup');
    if (originDiv) {
        originDiv.style.display = (type === 'TRANSFER') ? 'block' : 'none';
    }
}

function openStockAdjustModal(branchId, productId, currentQty, productName) {
    const newQtyStr = prompt(`Ajustar stock directamente para "${productName}".\nStock actual: ${currentQty}.\nIngrese nueva cantidad:`, currentQty);
    if (newQtyStr === null) return;
    const newQty = parseInt(newQtyStr, 10);
    if (isNaN(newQty) || newQty < 0) {
        alert('Ingrese una cantidad entera válida mayor o igual a 0');
        return;
    }

    authFetch(`${API_BASE}/inventory/branch/${branchId}/product/${productId}`, {
        method: 'PUT',
        body: JSON.stringify({ quantity: newQty })
    }).then(res => res.json())
      .then(json => {
          if (json.success) {
              showNotification('Stock actualizado correctamente', 'success');
              refreshDashboardData();
          } else {
              showNotification(json.message || 'Error actualizando stock', 'danger');
          }
      });
}

function openEditProductModal(productId) {
    const p = cachedProducts.find(x => x.id === productId);
    if (!p) return;
    document.getElementById('prodId').value = p.id;
    document.getElementById('prodSku').value = p.sku;
    document.getElementById('prodName').value = p.name;
    document.getElementById('prodDescription').value = p.description || '';
    document.getElementById('prodCategory').value = p.category;
    document.getElementById('prodPrice').value = p.price;
    document.getElementById('prodUnit').value = p.unit;
    document.getElementById('prodMinStock').value = p.minStockThreshold;

    const modal = new bootstrap.Modal(document.getElementById('productModal'));
    modal.show();
}

async function deleteProduct(productId) {
    if (!confirm('¿Está seguro de eliminar este producto del catálogo centralizado?')) return;
    try {
        const res = await authFetch(`${API_BASE}/products/${productId}`, { method: 'DELETE' });
        const json = await res.json();
        if (json.success) {
            showNotification('Producto eliminado', 'success');
            loadProducts();
            loadInventory();
        } else {
            showNotification(json.message || 'Error al eliminar', 'danger');
        }
    } catch (e) {
        showNotification('Error al eliminar producto', 'danger');
    }
}

async function deactivateEmployee(id) {
    if (!confirm('¿Desea desactivar el acceso a este empleado?')) return;
    try {
        const res = await authFetch(`${API_BASE}/employees/${id}`, { method: 'DELETE' });
        const json = await res.json();
        if (json.success) {
            showNotification('Empleado desactivado exitosamente', 'success');
            loadEmployees();
        }
    } catch (e) {
        showNotification('Error al desactivar empleado', 'danger');
    }
}

// -------------------------------------------------------------
// EVENT LISTENERS & FORM BINDINGS
// -------------------------------------------------------------
function setupEventListeners() {
    // Formulario de login
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', e => {
            e.preventDefault();
            const u = document.getElementById('loginUsername').value.trim();
            const p = document.getElementById('loginPassword').value.trim();
            login(u, p);
        });
    }

    // Botones de inicio rápido
    document.querySelectorAll('.btn-quick-login').forEach(btn => {
        btn.addEventListener('click', () => {
            const u = btn.getAttribute('data-user');
            const p = btn.getAttribute('data-pass');
            login(u, p);
        });
    });

    // Logout
    const logoutBtn = document.getElementById('btnLogout');
    if (logoutBtn) logoutBtn.addEventListener('click', logout);

    // Cambio de sucursal en pestaña de inventario
    const invBranchSelect = document.getElementById('inventoryBranchSelect');
    if (invBranchSelect) invBranchSelect.addEventListener('change', loadInventory);

    // Cambio de tipo de solicitud (Traslado vs Proveedor)
    const reqTypeSelect = document.getElementById('reqRequestType');
    if (reqTypeSelect) reqTypeSelect.addEventListener('change', toggleOriginBranchVisibility);

    // Guardar solicitud
    const saveRequestBtn = document.getElementById('btnSaveRequest');
    if (saveRequestBtn) {
        saveRequestBtn.addEventListener('click', async () => {
            const payload = {
                requestType: document.getElementById('reqRequestType').value,
                originBranchId: document.getElementById('reqOriginBranch').value ? parseInt(document.getElementById('reqOriginBranch').value) : null,
                destinationBranchId: parseInt(document.getElementById('reqDestinationBranch').value),
                productId: parseInt(document.getElementById('reqProduct').value),
                quantity: parseInt(document.getElementById('reqQuantity').value),
                notes: document.getElementById('reqNotes').value
            };

            try {
                const res = await authFetch(`${API_BASE}/requests`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                const json = await res.json();
                if (json.success) {
                    showNotification('Solicitud creada exitosamente en estado PENDIENTE', 'success');
                    bootstrap.Modal.getInstance(document.getElementById('newRequestModal')).hide();
                    refreshDashboardData();
                } else {
                    showNotification(json.message || 'Error al crear solicitud', 'danger');
                }
            } catch (e) {
                showNotification('Error al crear solicitud', 'danger');
            }
        });
    }

    // Guardar producto
    const saveProductBtn = document.getElementById('btnSaveProduct');
    if (saveProductBtn) {
        saveProductBtn.addEventListener('click', async () => {
            const prodId = document.getElementById('prodId').value;
            const payload = {
                sku: document.getElementById('prodSku').value.trim(),
                name: document.getElementById('prodName').value.trim(),
                description: document.getElementById('prodDescription').value.trim(),
                category: document.getElementById('prodCategory').value.trim(),
                price: parseFloat(document.getElementById('prodPrice').value),
                unit: document.getElementById('prodUnit').value.trim(),
                minStockThreshold: parseInt(document.getElementById('prodMinStock').value)
            };

            const url = prodId ? `${API_BASE}/products/${prodId}` : `${API_BASE}/products`;
            const method = prodId ? 'PUT' : 'POST';

            try {
                const res = await authFetch(url, {
                    method: method,
                    body: JSON.stringify(payload)
                });
                const json = await res.json();
                if (json.success) {
                    showNotification(prodId ? 'Producto actualizado' : 'Producto creado en el catálogo', 'success');
                    bootstrap.Modal.getInstance(document.getElementById('productModal')).hide();
                    loadProducts();
                    loadInventory();
                } else {
                    showNotification(json.message || 'Error al guardar producto', 'danger');
                }
            } catch (e) {
                showNotification('Error al procesar producto', 'danger');
            }
        });
    }

    // Botón abrir modal nuevo producto
    const btnNewProduct = document.getElementById('btnNewProduct');
    if (btnNewProduct) {
        btnNewProduct.addEventListener('click', () => {
            document.getElementById('prodId').value = '';
            document.getElementById('productForm').reset();
            new bootstrap.Modal(document.getElementById('productModal')).show();
        });
    }

    // Filtro de empleados por sucursal
    const empFilter = document.getElementById('empFilterBranch');
    if (empFilter) empFilter.addEventListener('change', loadEmployees);
}

function showNotification(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 end-0 m-3 shadow`;
    alertDiv.style.zIndex = 9999;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    setTimeout(() => {
        alertDiv.classList.remove('show');
        setTimeout(() => alertDiv.remove(), 250);
    }, 4000);
}
