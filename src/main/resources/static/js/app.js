// Treeventory - Client Application Logic
const API_BASE = '/api';
let currentUser = null;
let currentToken = null;

// Caché en memoria de catálogos y datos
let cachedBranches = [];
let cachedProducts = [];
let cachedInventories = [];
let cachedRequests = [];
let cachedEmployees = [];
let cachedAlerts = [];

// Filtros activos
let activeManagerStockFilter = 'all';
let activeRequestStatusFilter = 'all';
let activeRequestTypeFilter = 'all';
let currentActiveView = 'dashboard';
let criticalAlertDismissedByUser = false;

function dismissCriticalAlertBanner() {
    criticalAlertDismissedByUser = true;
    const banner = document.getElementById('gerenteCriticalAlertBanner');
    if (banner) {
        banner.classList.add('d-none');
    }
}

function getActivePendingRequest(branchId, productId) {
    if (!cachedRequests || !cachedRequests.length) return null;
    return cachedRequests.find(r =>
        r.destinationBranch && r.destinationBranch.id === branchId &&
        r.product && r.product.id === productId &&
        r.status === 'PENDING'
    ) || null;
}

function hasActiveRequest(branchId, productId) {
    return !!getActivePendingRequest(branchId, productId);
}

document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    setupEventListeners();
});

// # Gestión de autenticación, control de sesión y persistencia del token JWT
function initAuth() {
    currentToken = localStorage.getItem('treeventory_token');
    const userJson = localStorage.getItem('treeventory_user');
    if (currentToken && userJson) {
        try {
            currentUser = JSON.parse(userJson);
            renderAuthenticatedWorkspace();
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
}

function renderAuthenticatedWorkspace() {
    document.getElementById('loginSection').classList.add('d-none');
    document.getElementById('dashboardSection').classList.remove('d-none');

    const isAdmin = currentUser.role === 'ROLE_ADMIN';

    // Configurar encabezado y etiquetas del Sidebar
    const roleLabel = document.getElementById('sidebarRoleLabel');
    const branchBadge = document.getElementById('sidebarBranchBadge');
    const avatar = document.getElementById('userAvatarInitials');
    const nameEl = document.getElementById('userProfileName');
    const emailEl = document.getElementById('userProfileEmail');

    roleLabel.textContent = isAdmin ? 'ADMINISTRADOR' : 'GERENTE DE SUCURSAL';
    if (!isAdmin && currentUser.branchName) {
        branchBadge.textContent = currentUser.branchName;
        branchBadge.classList.remove('d-none');
    } else {
        branchBadge.classList.add('d-none');
    }

    // Avatar con iniciales
    const initials = currentUser.fullName.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase();
    avatar.textContent = initials || (isAdmin ? 'RV' : 'LM');
    nameEl.textContent = currentUser.fullName;
    emailEl.textContent = currentUser.username.includes('@') ? currentUser.username : `${currentUser.username}@corp.mx`;

    // Visibilidad de menú según el rol
    document.querySelectorAll('.admin-only').forEach(el => {
        if (isAdmin) el.classList.remove('d-none');
        else el.classList.add('d-none');
    });

    document.querySelectorAll('.gerente-only').forEach(el => {
        if (!isAdmin) el.classList.remove('d-none');
        else el.classList.add('d-none');
    });

    // Cargar datos y mostrar vista inicial
    loadAllData().then(() => {
        switchView('dashboard');
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
            alert(data.message || 'Credenciales incorrectas');
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

        localStorage.setItem('treeventory_token', currentToken);
        localStorage.setItem('treeventory_user', JSON.stringify(currentUser));

        renderAuthenticatedWorkspace();
    } catch (err) {
        alert('Error al conectar con el servidor.');
        console.error(err);
    }
}

function logout() {
    localStorage.removeItem('treeventory_token');
    localStorage.removeItem('treeventory_user');
    currentToken = null;
    currentUser = null;
    criticalAlertDismissedByUser = false;
    showLogin();
}

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
        logout();
        throw new Error('Unauthorized');
    }
    if (response.status === 403) {
        alert('Acceso denegado: No posee los permisos requeridos.');
        throw new Error('Forbidden');
    }
    return response;
}

// # Carga centralizada de catálogos y datos vía REST API
async function loadAllData() {
    try {
        const [branchesRes, productsRes, invRes, requestsRes, alertsRes] = await Promise.all([
            authFetch(`${API_BASE}/branches`).then(r => r.json()),
            authFetch(`${API_BASE}/products`).then(r => r.json()),
            authFetch(`${API_BASE}/inventory/all`).then(r => r.json()),
            authFetch(`${API_BASE}/requests`).then(r => r.json()),
            authFetch(`${API_BASE}/inventory/alerts`).then(r => r.json())
        ]);

        if (branchesRes.success) cachedBranches = branchesRes.data;
        if (productsRes.success) cachedProducts = productsRes.data;
        if (invRes.success) cachedInventories = invRes.data;
        if (requestsRes.success) cachedRequests = requestsRes.data;
        if (alertsRes.success) cachedAlerts = alertsRes.data;

        if (currentUser.role === 'ROLE_ADMIN') {
            const empRes = await authFetch(`${API_BASE}/employees`).then(r => r.json());
            if (empRes.success) cachedEmployees = empRes.data;
        }

        updateTopAlertPill();
        populateModalDropdowns();
    } catch (e) {
        console.error('Error cargando datos de Treeventory', e);
    }
}

function updateTopAlertPill() {
    const isGerente = currentUser && currentUser.role === 'ROLE_GERENTE';
    const relevantAlerts = (isGerente && currentUser.branchId) ?
        cachedAlerts.filter(a => a.branchId === currentUser.branchId) : cachedAlerts;

    // Solo contar alertas sin atender (las que NO tienen solicitud pendiente de reposición)
    const unattendedAlerts = relevantAlerts.filter(a => !hasActiveRequest(a.branchId, a.productId));

    const pill = document.getElementById('topbarAlertPill');
    const textEl = document.getElementById('topbarAlertText');
    const iconEl = document.getElementById('topbarAlertIcon');
    const navBadge = document.getElementById('navAlertBadge');

    if (navBadge) {
        navBadge.textContent = unattendedAlerts.length;
    }

    if (!pill || !textEl) return;

    if (unattendedAlerts.length === 0) {
        pill.classList.add('d-none');
        return;
    }

    pill.classList.remove('d-none');

    const criticalAlerts = unattendedAlerts.filter(a => a.alertLevel === 'CRITICAL' || a.currentStock === 0);
    const lowAlerts = unattendedAlerts.filter(a => a.alertLevel !== 'CRITICAL' && a.currentStock > 0);

    if (criticalAlerts.length > 0) {
        pill.classList.remove('pill-warning');
        pill.classList.add('pill-danger');
        if (iconEl) {
            iconEl.className = 'bi bi-exclamation-octagon-fill text-danger';
        }
        textEl.textContent = `${criticalAlerts.length} ${criticalAlerts.length === 1 ? 'alerta crítica' : 'alertas críticas'}`;
    } else {
        pill.classList.remove('pill-danger');
        pill.classList.add('pill-warning');
        if (iconEl) {
            iconEl.className = 'bi bi-exclamation-triangle-fill text-warning';
        }
        textEl.textContent = `${lowAlerts.length} ${lowAlerts.length === 1 ? 'alerta de stock bajo' : 'alertas de stock bajo'}`;
    }
}

function populateModalDropdowns() {
    // Sucursales
    const reqDestSelect = document.getElementById('modalReqDestBranch');
    const reqOrigSelect = document.getElementById('modalReqOriginBranch');
    const empBranchSelect = document.getElementById('modalEmpBranch');
    const otherBranchSelect = document.getElementById('otherBranchSelect');
    const empFilterBranch = document.getElementById('empBranchFilter');

    const branchOptions = cachedBranches.map(b => `<option value="${b.id}">${b.name}</option>`).join('');

    if (reqDestSelect) reqDestSelect.innerHTML = branchOptions;
    if (reqOrigSelect) reqOrigSelect.innerHTML = `<option value="">— Seleccionar origen —</option>` + branchOptions;
    if (empBranchSelect) empBranchSelect.innerHTML = `<option value="">Sin sucursal (Corporativo)</option>` + branchOptions;
    if (empFilterBranch) empFilterBranch.innerHTML = `<option value="">Todas las sucursales</option>` + branchOptions;

    if (otherBranchSelect) {
        const otherBranchList = (currentUser.role === 'ROLE_GERENTE' && currentUser.branchId) ?
            cachedBranches.filter(b => b.id !== currentUser.branchId) : cachedBranches;
        otherBranchSelect.innerHTML = otherBranchList.map(b => `<option value="${b.id}">${b.name}</option>`).join('');
    }

    // Si es gerente, fijar destino a su sucursal
    if (currentUser.role === 'ROLE_GERENTE' && currentUser.branchId && reqDestSelect) {
        reqDestSelect.value = currentUser.branchId;
    }

    // Productos
    const reqProdSelect = document.getElementById('modalReqProduct');
    if (reqProdSelect) {
        reqProdSelect.innerHTML = cachedProducts.map(p =>
            `<option value="${p.id}">${p.name} (${p.sku})</option>`).join('');
    }

    // Categorías en filtros
    const categories = Array.from(new Set(cachedProducts.map(p => p.category)));
    const catOptions = `<option value="">Todas las categorías</option>` +
        categories.map(c => `<option value="${c}">${c}</option>`).join('');

    const adminCatFilter = document.getElementById('adminInvCategoryFilter');
    const managerCatFilter = document.getElementById('managerInvCategoryFilter');
    const otherBranchCatFilter = document.getElementById('otherBranchCategoryFilter');
    if (adminCatFilter) adminCatFilter.innerHTML = catOptions;
    if (managerCatFilter) managerCatFilter.innerHTML = catOptions;
    if (otherBranchCatFilter) otherBranchCatFilter.innerHTML = catOptions;
}

// # Enrutamiento dinámico entre vistas del panel (SPA)
function switchView(viewName) {
    currentActiveView = viewName;
    document.querySelectorAll('.view-panel').forEach(p => p.classList.add('d-none'));
    document.querySelectorAll('.nav-menu-btn').forEach(b => b.classList.remove('active'));

    const titleEl = document.getElementById('topbarSectionTitle');
    const isAdmin = currentUser.role === 'ROLE_ADMIN';

    switch (viewName) {
        case 'dashboard':
            document.getElementById('viewDashboard').classList.remove('d-none');
            document.getElementById('navBtnDashboard').classList.add('active');
            titleEl.textContent = 'Dashboard';
            renderDashboard();
            break;

        case 'inventario_admin':
            if (!isAdmin) return;
            document.getElementById('viewInventarioAdmin').classList.remove('d-none');
            document.getElementById('navBtnInventarioAdmin').classList.add('active');
            titleEl.textContent = 'Inventario';
            renderAdminMatrix();
            break;

        case 'mi_inventario':
            if (isAdmin) return;
            document.getElementById('viewMiInventario').classList.remove('d-none');
            document.getElementById('navBtnMiInventario').classList.add('active');
            titleEl.textContent = `Mi Inventario — ${currentUser.branchName || 'CDMX Centro'}`;
            document.getElementById('miInventarioTitle').textContent = `Mi Inventario — ${currentUser.branchName || 'CDMX Centro'}`;
            renderManagerInventoryTable();
            break;

        case 'otras_sucursales':
            if (isAdmin) return;
            document.getElementById('viewOtrasSucursales').classList.remove('d-none');
            document.getElementById('navBtnOtrasSucursales').classList.add('active');
            titleEl.textContent = 'Otras Sucursales';
            renderOtherBranchesView();
            break;

        case 'solicitudes':
            document.getElementById('viewSolicitudes').classList.remove('d-none');
            document.getElementById('navBtnSolicitudes').classList.add('active');
            titleEl.textContent = 'Solicitudes';
            renderRequestsView();
            break;

        case 'empleados':
            if (!isAdmin) return;
            document.getElementById('viewEmpleados').classList.remove('d-none');
            document.getElementById('navBtnEmpleados').classList.add('active');
            titleEl.textContent = 'Empleados';
            renderEmployeesView();
            break;
    }
}

// # Métricas, gráficos y componentes del Dashboard principal
function renderDashboard() {
    const isAdmin = currentUser.role === 'ROLE_ADMIN';

    // Banners específicos de Gerente
    const activeBranchBanner = document.getElementById('gerenteActiveBranchBanner');
    const criticalAlertBanner = document.getElementById('gerenteCriticalAlertBanner');

    if (!isAdmin) {
        activeBranchBanner.classList.remove('d-none');
        document.getElementById('branchBannerName').textContent = currentUser.branchName || 'CDMX Centro';

        const myBranch = cachedBranches.find(b => b.id === currentUser.branchId) || cachedBranches[0];
        if (myBranch) {
            document.getElementById('branchBannerAddress').textContent = myBranch.address || 'Av. Juárez 123, Centro Histórico';
            document.getElementById('branchBannerPhone').textContent = myBranch.phone || '55-1001-0001';
        }

        // Alertas del gerente (separando las pendientes sin atender de las que ya tienen solicitud en proceso)
        const myAlerts = cachedAlerts.filter(a => a.branchId === (currentUser.branchId || 1));
        const unattendedAlerts = myAlerts.filter(a => !hasActiveRequest(a.branchId, a.productId));
        const unattendedCriticals = unattendedAlerts.filter(a => a.alertLevel === 'CRITICAL' || a.currentStock === 0);
        const criticalCount = unattendedCriticals.length;

        if (criticalCount > 0 && !criticalAlertDismissedByUser) {
            criticalAlertBanner.classList.remove('d-none');
            const names = unattendedCriticals.slice(0, 2).map(a => a.productName);
            let sub = names.join(', ');
            if (criticalCount > 2) {
                sub += ' y más';
            }
            document.getElementById('criticalBannerTitle').textContent = `${criticalCount} ${criticalCount === 1 ? 'producto en estado crítico' : 'productos en estado crítico'}`;
            document.getElementById('criticalBannerSubtitle').textContent = sub || 'Atención requerida para reabastecimiento';
        } else {
            criticalAlertBanner.classList.add('d-none');
        }

        // KPIs de Gerente
        const myInventories = cachedInventories.filter(i => i.branch.id === (currentUser.branchId || 1));
        const totalUnits = myInventories.reduce((acc, curr) => acc + curr.quantity, 0);

        document.getElementById('kpiLabel1').textContent = 'UNIDADES EN SUCURSAL';
        document.getElementById('kpiValue1').textContent = totalUnits.toLocaleString();
        document.getElementById('kpiSub1').textContent = `${myInventories.length} tipos de producto`;

        document.getElementById('kpiValue2').textContent = unattendedAlerts.length;
        document.getElementById('kpiSub2').textContent = `${criticalCount} críticos sin atender`;

        const myReqs = cachedRequests.filter(r => r.destinationBranch && r.destinationBranch.id === (currentUser.branchId || 1));
        const myPending = myReqs.filter(r => r.status === 'PENDING').length;

        document.getElementById('kpiLabel3').textContent = 'MIS SOLICITUDES';
        document.getElementById('kpiValue3').textContent = myReqs.length;
        document.getElementById('kpiSub3').textContent = `${myPending} pendiente(s)`;

        document.getElementById('kpiLabel4').textContent = 'EMPLEADOS';
        document.getElementById('kpiValue4').textContent = '4';
        document.getElementById('kpiSub4').textContent = 'activos en sucursal';

        // Panel Izquierdo: Productos con stock bajo (Tabla completa, con estado visual según solicitud)
        document.getElementById('dashboardPanelLeftTitle').textContent = 'Productos con stock bajo';
        document.getElementById('dashboardPanelLeftTag').textContent = `${unattendedAlerts.length} sin atender`;
        renderManagerDashboardLowStock(myAlerts);

        // Panel Derecho: Solicitudes Recientes de la Sucursal
        document.getElementById('dashboardPanelRightTitle').textContent = 'Mis solicitudes recientes';
        document.getElementById('dashboardPanelRightBadge').className = 'badge-status badge-pendiente';
        document.getElementById('dashboardPanelRightBadge').textContent = `${myPending} pendientes`;
        renderManagerDashboardRecentReqs(myReqs);

    } else {
        // Vista Administrador
        activeBranchBanner.classList.add('d-none');
        criticalAlertBanner.classList.add('d-none');

        const totalUnitsGlobal = cachedInventories.reduce((acc, curr) => acc + curr.quantity, 0);
        document.getElementById('kpiLabel1').textContent = 'UNIDADES TOTALES';
        document.getElementById('kpiValue1').textContent = totalUnitsGlobal.toLocaleString();
        document.getElementById('kpiSub1').textContent = `${cachedProducts.length} productos distintos`;

        const unattendedGlobalAlerts = cachedAlerts.filter(a => !hasActiveRequest(a.branchId, a.productId));
        const unattendedGlobalCriticals = unattendedGlobalAlerts.filter(a => a.alertLevel === 'CRITICAL' || a.currentStock === 0);

        document.getElementById('kpiValue2').textContent = unattendedGlobalAlerts.length;
        document.getElementById('kpiSub2').textContent = `${unattendedGlobalCriticals.length} críticos sin atender`;

        const pendingCount = cachedRequests.filter(r => r.status === 'PENDING').length;
        document.getElementById('kpiLabel3').textContent = 'SOLICITUDES PENDIENTES';
        document.getElementById('kpiValue3').textContent = pendingCount;
        document.getElementById('kpiSub3').textContent = 'requieren aprobación';

        document.getElementById('kpiLabel4').textContent = 'EMPLEADOS ACTIVOS';
        document.getElementById('kpiValue4').textContent = cachedEmployees.length || 36;
        document.getElementById('kpiSub4').textContent = `en ${cachedBranches.length} sucursales`;

        // Panel Izquierdo: Gráfica de barras de inventario por sucursal
        document.getElementById('dashboardPanelLeftTitle').textContent = 'Inventario por sucursal';
        document.getElementById('dashboardPanelLeftTag').textContent = 'unidades totales';
        renderAdminBranchBars();

        // Panel Derecho: Alertas Críticas Globales Sin Atender
        document.getElementById('dashboardPanelRightTitle').textContent = 'Alertas críticas';
        document.getElementById('dashboardPanelRightBadge').className = 'badge-status badge-sin-stock';
        document.getElementById('dashboardPanelRightBadge').textContent = `${unattendedGlobalCriticals.length} sin stock`;
        renderAdminCriticalAlerts(unattendedGlobalCriticals);
    }

    // Tabla inferior de solicitudes recientes
    renderRecentRequestsBottomTable();
}

function renderAdminBranchBars() {
    const container = document.getElementById('dashboardPanelLeftContent');
    const branchTotals = cachedBranches.map(b => {
        const branchInvs = cachedInventories.filter(i => i.branch.id === b.id);
        const total = branchInvs.reduce((acc, curr) => acc + curr.quantity, 0);
        const alerts = cachedAlerts.filter(a => a.branchId === b.id).length;
        return { name: b.name.replace('Sucursal ', ''), total, alerts };
    }).sort((a, b) => b.total - a.total).slice(0, 10);

    const maxTotal = Math.max(...branchTotals.map(b => b.total), 100);

    let html = `<div class="d-flex flex-column gap-2">`;
    branchTotals.forEach(item => {
        const pct = Math.min(100, Math.round((item.total / maxTotal) * 100));
        html += `
            <div class="d-flex align-items-center justify-content-between small">
                <span class="text-secondary" style="width: 140px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${item.name}</span>
                <div class="flex-grow-1 mx-3" style="background: #1e293b; height: 18px; border-radius: 4px; overflow: hidden; position: relative;">
                    <div style="background: #f59e0b; width: ${pct}%; height: 100%; border-radius: 4px; display: flex; align-items: center; padding-left: 8px; font-weight: 700; font-size: 0.7rem; color: #000;">
                        ${item.total}
                    </div>
                </div>
                <span class="text-danger fw-bold" style="width: 35px; text-align: right;">${item.alerts} ⚠️</span>
            </div>
        `;
    });
    html += `</div>`;
    container.innerHTML = html;
}

function renderAdminCriticalAlerts(criticals) {
    const container = document.getElementById('dashboardPanelRightContent');
    if (criticals.length === 0) {
        container.innerHTML = `<div class="text-secondary small py-4 text-center">No hay productos en estado crítico.</div>`;
        return;
    }

    let html = `<div class="d-flex flex-column gap-3">`;
    criticals.slice(0, 5).forEach(c => {
        html += `
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <div class="fw-semibold text-white small">
                        <span class="status-dot status-dot-red"></span>${c.productName}
                    </div>
                    <div class="text-secondary" style="font-size: 0.75rem; padding-left: 1rem;">
                        ${c.branchName} · ${c.currentStock}/${c.minStockThreshold} mín.
                    </div>
                </div>
                <button class="btn btn-sm btn-outline-primary py-0 px-2" style="font-size: 0.72rem;" onclick="openNewRequestModal(${c.productId}, 'SUPPLIER', ${c.branchId})">
                    Reponer
                </button>
            </div>
        `;
    });
    html += `</div>`;
    container.innerHTML = html;
}

function renderManagerDashboardLowStock(myAlerts) {
    const container = document.getElementById('dashboardPanelLeftContent');
    if (myAlerts.length === 0) {
        container.innerHTML = `<div class="text-secondary small py-4 text-center">Inventario óptimo. No hay stock bajo.</div>`;
        return;
    }

    let html = `
        <div class="table-responsive">
            <table class="table-dark-custom">
                <thead>
                    <tr>
                        <th>PRODUCTO</th>
                        <th>STOCK</th>
                        <th>MÍN.</th>
                        <th>ESTADO</th>
                        <th class="text-end">ACCIÓN</th>
                    </tr>
                </thead>
                <tbody>
    `;

    myAlerts.slice(0, 8).forEach(a => {
        const isSinStock = a.currentStock === 0;
        const pendingReq = getActivePendingRequest(a.branchId, a.productId);
        const isPending = !!pendingReq;

        let statusBadge = isSinStock ?
            `<span class="badge-status badge-sin-stock">Sin stock</span>` :
            `<span class="badge-status badge-bajo">Bajo</span>`;

        let actionHtml = `
            <button class="btn btn-sm btn-pill-red py-1 px-2" style="font-size: 0.75rem;" onclick="openNewRequestModal(${a.productId}, 'SUPPLIER')">
                Solicitar
            </button>
        `;

        if (isPending) {
            statusBadge = `<span class="badge-status badge-pendiente">Pendiente</span>`;
            actionHtml = `<span class="badge-status badge-pendiente py-1 px-2" style="font-size: 0.75rem;"><i class="bi bi-hourglass-split me-1"></i>En proceso</span>`;
        }

        html += `
            <tr>
                <td>
                    <div class="fw-semibold text-white">${a.productName}</div>
                    <div class="text-secondary" style="font-size: 0.75rem; font-weight: 500;">
                        ${a.productSku}${isPending ? ' · <span class="text-warning">Solicitud #r' + pendingReq.id + '</span>' : ''}
                    </div>
                </td>
                <td class="fw-bold ${isSinStock ? 'text-danger' : 'text-warning'}">${a.currentStock}</td>
                <td>${a.minStockThreshold}</td>
                <td>${statusBadge}</td>
                <td class="text-end">
                    ${actionHtml}
                </td>
            </tr>
        `;
    });

    html += `</tbody></table></div>`;
    container.innerHTML = html;
}

function renderManagerDashboardRecentReqs(myReqs) {
    const container = document.getElementById('dashboardPanelRightContent');
    if (myReqs.length === 0) {
        container.innerHTML = `<div class="text-secondary small py-4 text-center">No hay solicitudes registradas para esta sucursal.</div>`;
        return;
    }

    let html = `<div class="d-flex flex-column gap-2">`;
    myReqs.slice(0, 4).forEach(r => {
        const typeBadge = r.requestType === 'TRANSFER' ?
            `<span class="badge-status badge-traslado">Traslado</span>` :
            `<span class="badge-status badge-proveedor">Proveedor</span>`;
        const statusBadge = `<span class="badge-status badge-pendiente">${r.status}</span>`;

        html += `
            <div class="p-2 rounded card-dark border-0 d-flex justify-content-between align-items-center">
                <div>
                    <div class="fw-semibold text-white small">${r.product.name}</div>
                    <div class="text-secondary" style="font-size: 0.75rem;">Cant: ${r.quantity} · ${typeBadge}</div>
                </div>
                <div>${statusBadge}</div>
            </div>
        `;
    });
    html += `</div>`;
    container.innerHTML = html;
}

function renderRecentRequestsBottomTable() {
    const tbody = document.getElementById('dashboardRecentRequestsTbody');
    tbody.innerHTML = '';
    const isGerente = currentUser.role === 'ROLE_GERENTE';
    const list = isGerente && currentUser.branchId ?
        cachedRequests.filter(r => r.destinationBranch && r.destinationBranch.id === currentUser.branchId) : cachedRequests;

    document.getElementById('recentRequestsCountBadge').textContent = `${list.length} total`;

    list.slice(0, 6).forEach(req => {
        const typeBadge = req.requestType === 'TRANSFER' ?
            `<span class="badge-status badge-traslado">Traslado</span>` :
            `<span class="badge-status badge-proveedor">Proveedor</span>`;

        let statusBadge = `<span class="badge-status badge-pendiente">Pendiente</span>`;
        if (req.status === 'APPROVED') statusBadge = `<span class="badge-status badge-normal">Aprobada</span>`;
        if (req.status === 'REJECTED') statusBadge = `<span class="badge-status badge-rechazada">Rechazada</span>`;

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>#r${req.id}</td>
            <td>${typeBadge}</td>
            <td><strong>${req.product.name}</strong></td>
            <td>${req.destinationBranch ? req.destinationBranch.name : 'Corporativo'}</td>
            <td class="fw-bold">${req.quantity}</td>
            <td>${req.createdAt ? req.createdAt.substring(0, 10) : '2026-09-21'}</td>
            <td>${statusBadge}</td>
        `;
        tbody.appendChild(tr);
    });
}

// # Matriz global de inventario por sucursales para el Administrador
function renderAdminMatrix() {
    const thead = document.getElementById('matrixTableHead');
    const tbody = document.getElementById('matrixTableBody');

    const search = (document.getElementById('adminInvSearch').value || '').toLowerCase();
    const catFilter = document.getElementById('adminInvCategoryFilter').value;

    let filteredProducts = cachedProducts.filter(p => {
        const matchSearch = p.name.toLowerCase().includes(search) || p.sku.toLowerCase().includes(search);
        const matchCat = !catFilter || p.category === catFilter;
        return matchSearch && matchCat;
    });

    // Construir cabecera con las sucursales
    let headHtml = `
        <tr>
            <th>SKU</th>
            <th>PRODUCTO</th>
            <th>CATEGORÍA</th>
    `;
    cachedBranches.forEach(b => {
        headHtml += `<th class="text-center">${b.name.toUpperCase()}</th>`;
    });
    headHtml += `</tr>`;
    thead.innerHTML = headHtml;

    // Construir filas
    tbody.innerHTML = '';
    filteredProducts.forEach(p => {
        const tr = document.createElement('tr');
        let rowHtml = `
            <td><code>${p.sku}</code></td>
            <td class="fw-semibold text-white">${p.name}</td>
            <td><span class="badge-status badge-completada">${p.category}</span></td>
        `;

        cachedBranches.forEach(b => {
            const inv = cachedInventories.find(i => i.product.id === p.id && i.branch.id === b.id);
            const qty = inv ? inv.quantity : 0;
            const min = p.minStockThreshold || 10;

            let barClass = 'stock-bar-green';
            let numColor = 'text-white';
            if (qty === 0) {
                barClass = 'stock-bar-red';
                numColor = 'text-danger';
            } else if (qty <= min) {
                barClass = 'stock-bar-amber';
                numColor = 'text-warning';
            }

            const pct = Math.min(100, Math.round((qty / (min * 3)) * 100));

            rowHtml += `
                <td class="text-center">
                    <div class="d-flex align-items-center justify-content-center">
                        <div class="stock-bar-container">
                            <div class="stock-bar-fill ${barClass}" style="width: ${pct}%;"></div>
                        </div>
                        <span class="fw-bold ${numColor} small">${qty}</span>
                    </div>
                    <a href="javascript:void(0)" class="quick-stock-edit-btn" onclick="openEditStockModal(${b.id}, ${p.id}, ${qty}, '${p.name.replace(/'/g, "\\'")}', '${b.name.replace(/'/g, "\\'")}')">editar</a>
                </td>
            `;
        });

        tr.innerHTML = rowHtml;
        tbody.appendChild(tr);
    });
}

let stockModalInstance = null;
function openEditStockModal(branchId, productId, currentQty, prodName, branchName) {
    document.getElementById('adjustStockBranchId').value = branchId;
    document.getElementById('adjustStockProductId').value = productId;
    document.getElementById('adjustStockBranchName').value = branchName || `Sucursal #${branchId}`;
    document.getElementById('adjustStockProductName').value = prodName;
    document.getElementById('adjustStockCurrentQty').value = currentQty;
    document.getElementById('adjustStockNewQty').value = currentQty;

    const modalEl = document.getElementById('modalAjustarStock');
    if (!stockModalInstance && window.bootstrap) {
        stockModalInstance = new bootstrap.Modal(modalEl);
    }
    if (stockModalInstance) {
        stockModalInstance.show();
    }
    setTimeout(() => {
        const input = document.getElementById('adjustStockNewQty');
        if (input) {
            input.focus();
            input.select();
        }
    }, 300);
}

function quickEditStockAdmin(branchId, productId, currentQty, prodName, branchName) {
    openEditStockModal(branchId, productId, currentQty, prodName, branchName);
}

// # Gestión del inventario local de la sucursal activa y alertas
function filterManagerStock(filter) {
    activeManagerStockFilter = filter;
    document.querySelectorAll('.chips-group .filter-chip').forEach(c => c.classList.remove('active'));
    if (filter === 'all') document.getElementById('chipAll')?.classList.add('active');
    if (filter === 'critico') document.getElementById('chipCritico')?.classList.add('active');
    if (filter === 'bajo') document.getElementById('chipBajo')?.classList.add('active');
    if (filter === 'pendiente') document.getElementById('chipPendiente')?.classList.add('active');
    if (filter === 'normal') document.getElementById('chipNormal')?.classList.add('active');
    renderManagerInventoryTable();
}

function renderManagerInventoryTable() {
    const tbody = document.getElementById('managerInventoryTbody');
    tbody.innerHTML = '';

    const branchId = currentUser.branchId || 1;
    const branchInvs = cachedInventories.filter(i => i.branch.id === branchId);

    const search = (document.getElementById('managerInvSearch').value || '').toLowerCase();
    const catFilter = document.getElementById('managerInvCategoryFilter').value;

    let items = branchInvs.map(inv => {
        const p = inv.product;
        const qty = inv.quantity;
        const min = p.minStockThreshold || 10;
        const pendingReq = getActivePendingRequest(branchId, p.id);
        const hasPending = !!pendingReq;

        let estado = 'Normal';
        let level = 'normal';

        if (qty === 0) {
            estado = 'Sin stock';
            level = 'critico';
        } else if (qty <= 3 || qty <= Math.floor(min / 2)) {
            estado = 'Crítico';
            level = 'critico';
        } else if (qty <= min) {
            estado = 'Bajo';
            level = 'bajo';
        }

        // Si tiene solicitud en proceso, el estado cambia a Pendiente
        if (hasPending) {
            estado = 'Pendiente';
        }

        return { inv, product: p, quantity: qty, min, estado, level, hasPending, pendingReq };
    });

    // Actualizar contadores de chips
    const countAll = items.length;
    const countCritico = items.filter(i => i.level === 'critico').length;
    const countBajo = items.filter(i => i.level === 'bajo').length;
    const countPendiente = items.filter(i => i.hasPending).length;
    const countNormal = items.filter(i => i.level === 'normal').length;

    const chipAll = document.getElementById('chipAll');
    const chipCritico = document.getElementById('chipCritico');
    const chipBajo = document.getElementById('chipBajo');
    const chipPendiente = document.getElementById('chipPendiente');
    const chipNormal = document.getElementById('chipNormal');

    if (chipAll) chipAll.textContent = `Todo (${countAll})`;
    if (chipCritico) chipCritico.textContent = `Crítico (${countCritico})`;
    if (chipBajo) chipBajo.textContent = `Bajo (${countBajo})`;
    if (chipPendiente) chipPendiente.textContent = `Pendiente (${countPendiente})`;
    if (chipNormal) chipNormal.textContent = `Normal (${countNormal})`;

    // Aplicar filtros
    if (activeManagerStockFilter === 'pendiente') {
        items = items.filter(i => i.hasPending);
    } else if (activeManagerStockFilter !== 'all') {
        items = items.filter(i => i.level === activeManagerStockFilter);
    }
    if (search) {
        items = items.filter(i => i.product.name.toLowerCase().includes(search) || i.product.sku.toLowerCase().includes(search));
    }
    if (catFilter) {
        items = items.filter(i => i.product.category === catFilter);
    }

    if (items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="10" class="text-center py-4 text-secondary">No se encontraron productos coincidentes.</td></tr>`;
        return;
    }

    items.forEach(item => {
        const p = item.product;
        const isUrgent = item.level === 'critico';
        const isLow = item.level === 'bajo';
        const hasPending = item.hasPending;
        const pendingReq = item.pendingReq;

        let dotClass = 'status-dot-green';
        let barClass = 'stock-bar-green';
        let badgeClass = 'badge-normal';
        let subtext = '';

        if (item.level === 'critico') {
            dotClass = 'status-dot-red';
            barClass = 'stock-bar-red';
            badgeClass = 'badge-sin-stock';
            subtext = `<div class="text-danger" style="font-size: 0.72rem;">⚠️ Requiere reposición inmediata</div>`;
        } else if (item.level === 'bajo') {
            dotClass = 'status-dot-amber';
            barClass = 'stock-bar-amber';
            badgeClass = 'badge-bajo';
            subtext = `<div class="text-warning" style="font-size: 0.72rem;">⚠️ Considera hacer una solicitud</div>`;
        }

        if (hasPending) {
            badgeClass = 'badge-pendiente';
            subtext = `<div class="text-warning" style="font-size: 0.72rem;"><i class="bi bi-clock-history me-1"></i>Solicitud #r${pendingReq.id} en proceso</div>`;
        }

        const pct = Math.min(100, Math.round((item.quantity / (item.min * 3)) * 100));

        let actionBtn = `<span class="text-secondary small">-</span>`;
        if (hasPending) {
            actionBtn = `
                <span class="badge-status badge-pendiente py-1 px-2" style="font-size: 0.75rem;">
                    <i class="bi bi-hourglass-split me-1"></i>En proceso
                </span>
            `;
        } else if (isUrgent || isLow) {
            actionBtn = `
                <button class="btn btn-sm btn-pill-red py-1 px-2" style="font-size: 0.75rem;" onclick="openNewRequestModal(${p.id}, 'SUPPLIER')">
                    <i class="bi bi-box-arrow-in-down me-1"></i>Solicitar
                </button>
            `;
        }

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><span class="status-dot ${dotClass}"></span><code>${p.sku}</code></td>
            <td>
                <div class="fw-semibold text-white">${p.name}</div>
                ${subtext}
            </td>
            <td><span class="badge-status badge-completada">${p.category}</span></td>
            <td class="fw-bold ${isUrgent ? 'text-danger' : (isLow ? 'text-warning' : 'text-white')}">${item.quantity} ${p.unit}</td>
            <td>
                <div class="stock-bar-container">
                    <div class="stock-bar-fill ${barClass}" style="width: ${pct}%;"></div>
                </div>
                <span class="small text-secondary fw-semibold">${item.quantity}</span>
            </td>
            <td>${item.min}</td>
            <td>$${p.price.toFixed(2)}</td>
            <td class="small text-secondary">2026-09-20</td>
            <td><span class="badge-status ${badgeClass}">${item.estado}</span></td>
            <td class="text-end">${actionBtn}</td>
        `;
        tbody.appendChild(tr);
    });
}

// # Consulta de inventarios en otras sucursales para traspasos
function renderOtherBranchesView() {
    const grid = document.getElementById('branchesMiniGrid');
    grid.innerHTML = '';

    const otherBranches = cachedBranches.filter(b => b.id !== currentUser.branchId);

    otherBranches.forEach(b => {
        const invs = cachedInventories.filter(i => i.branch.id === b.id);
        const totalUnits = invs.reduce((acc, curr) => acc + curr.quantity, 0);
        const alertCount = cachedAlerts.filter(a => a.branchId === b.id).length;

        const col = document.createElement('div');
        col.className = 'col-md-3 col-sm-6';
        col.innerHTML = `
            <div class="card-dark p-3 cursor-pointer h-100" onclick="selectOtherBranch(${b.id})" style="cursor: pointer;">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="small text-light fw-bold">${b.name}</span>
                </div>
                <div class="d-flex justify-content-between align-items-baseline">
                    <h5 class="fw-bold text-white mb-0">${totalUnits}</h5>
                    <span class="text-danger small fw-bold">${alertCount} ⚠️</span>
                </div>
            </div>
        `;
        grid.appendChild(col);
    });

    if (otherBranches.length > 0) {
        selectOtherBranch(otherBranches[0].id);
    }
}

function selectOtherBranch(branchId) {
    const select = document.getElementById('otherBranchSelect');
    if (select) select.value = branchId;
    onOtherBranchChange();
}

function onOtherBranchChange() {
    const select = document.getElementById('otherBranchSelect');
    const branchId = parseInt(select.value);
    const branch = cachedBranches.find(b => b.id === branchId);
    if (!branch) return;

    document.getElementById('otherBranchDir').textContent = branch.address || 'Dirección corporativa';
    document.getElementById('otherBranchTel').textContent = branch.phone || '55-0000-0000';

    const branchInvs = cachedInventories.filter(i => i.branch.id === branchId);
    const totalUnits = branchInvs.reduce((acc, curr) => acc + curr.quantity, 0);
    const alertCount = cachedAlerts.filter(a => a.branchId === branchId).length;

    document.getElementById('otherBranchUnits').textContent = totalUnits;
    document.getElementById('otherBranchAlerts').textContent = alertCount;
    document.getElementById('otherBranchTableTitle').textContent = `Inventario de ${branch.name} — solo lectura`;

    const searchInput = document.getElementById('otherBranchSearch');
    const catSelect = document.getElementById('otherBranchCategoryFilter');
    const search = (searchInput && searchInput.value ? searchInput.value : '').toLowerCase().trim();
    const catFilter = (catSelect && catSelect.value) ? catSelect.value : '';

    const filteredInvs = branchInvs.filter(inv => {
        const p = inv.product;
        const matchSearch = p.name.toLowerCase().includes(search) || p.sku.toLowerCase().includes(search);
        const matchCat = !catFilter || p.category === catFilter;
        return matchSearch && matchCat;
    });

    const tbody = document.getElementById('otherBranchInventoryTbody');
    tbody.innerHTML = '';

    if (filteredInvs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-secondary">No se encontraron productos coincidentes en esta sucursal.</td></tr>`;
        return;
    }

    filteredInvs.forEach(inv => {
        const p = inv.product;
        const qty = inv.quantity;
        const min = p.minStockThreshold || 10;
        const isUrgent = qty === 0 || qty <= 3;
        const isLow = qty <= min;

        let estado = 'Normal';
        let barClass = 'stock-bar-green';
        let badgeClass = 'badge-normal';
        if (isUrgent) {
            estado = qty === 0 ? 'Sin stock' : 'Crítico';
            barClass = 'stock-bar-red';
            badgeClass = 'badge-sin-stock';
        } else if (isLow) {
            estado = 'Bajo';
            barClass = 'stock-bar-amber';
            badgeClass = 'badge-bajo';
        }

        const pct = Math.min(100, Math.round((qty / (min * 3)) * 100));

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><code>${p.sku}</code></td>
            <td class="fw-semibold text-white">${p.name}</td>
            <td><span class="badge-status badge-completada">${p.category}</span></td>
            <td class="fw-bold ${isUrgent ? 'text-danger' : (isLow ? 'text-warning' : 'text-white')}">${qty}</td>
            <td>
                <div class="stock-bar-container">
                    <div class="stock-bar-fill ${barClass}" style="width: ${pct}%;"></div>
                </div>
                <span class="small text-secondary fw-semibold">${qty}</span>
            </td>
            <td>${min}</td>
            <td><span class="badge-status ${badgeClass}">${estado}</span></td>
            <td class="small text-secondary">2026-09-20</td>
        `;
        tbody.appendChild(tr);
    });
}

// # Visualización y resolución de solicitudes de inventario
function filterRequestsByStatus(status) {
    activeRequestStatusFilter = status;
    document.querySelectorAll('#requestStatusTabs .filter-chip').forEach(c => c.classList.remove('active'));
    event.target.classList.add('active');
    renderRequestsView();
}

function filterRequestsByType(type) {
    activeRequestTypeFilter = type;
    document.querySelectorAll('#requestTypeTabs .filter-chip').forEach(c => c.classList.remove('active'));
    event.target.classList.add('active');
    renderRequestsView();
}

function renderRequestsView() {
    const isAdmin = currentUser.role === 'ROLE_ADMIN';
    const isGerente = !isAdmin;

    const noticeBanner = document.getElementById('requestsNoticeBanner');
    const noticeText = document.getElementById('requestsNoticeText');
    const pendingCount = cachedRequests.filter(r => r.status === 'PENDING').length;

    if (isAdmin) {
        noticeBanner.className = 'alert-banner-warning mb-3';
        noticeText.textContent = `${pendingCount} solicitudes requieren tu aprobación`;
        document.getElementById('requestsHeaderTitle').textContent = 'Solicitudes';
        document.getElementById('requestsHeaderSubtitle').textContent = `${pendingCount} solicitudes pendientes`;
    } else {
        noticeBanner.className = 'alert-banner-warning mb-3';
        noticeText.textContent = `Las solicitudes de traslado y pedidos a proveedor que realices serán revisados por el administrador antes de procesarse.`;
        document.getElementById('requestsHeaderTitle').textContent = `Mis solicitudes — ${currentUser.branchName || 'CDMX Centro'}`;
        document.getElementById('requestsHeaderSubtitle').textContent = `${cachedRequests.length} solicitudes registradas`;
    }

    const tbody = document.getElementById('requestsFullTbody');
    tbody.innerHTML = '';

    let list = cachedRequests;
    if (isGerente && currentUser.branchId) {
        list = list.filter(r => (r.destinationBranch && r.destinationBranch.id === currentUser.branchId) ||
                                (r.originBranch && r.originBranch.id === currentUser.branchId));
    }

    if (activeRequestStatusFilter !== 'all') {
        list = list.filter(r => r.status === activeRequestStatusFilter);
    }
    if (activeRequestTypeFilter !== 'all') {
        list = list.filter(r => r.requestType === activeRequestTypeFilter);
    }

    list.forEach(req => {
        const typeBadge = req.requestType === 'TRANSFER' ?
            `<span class="badge-status badge-traslado"><i class="bi bi-arrow-left-right me-1"></i>Traslado</span>` :
            `<span class="badge-status badge-proveedor"><i class="bi bi-briefcase-fill me-1"></i>Proveedor</span>`;

        let statusBadge = `<span class="badge-status badge-pendiente">Pendiente</span>`;
        if (req.status === 'APPROVED') statusBadge = `<span class="badge-status badge-normal">Aprobada</span>`;
        if (req.status === 'REJECTED') statusBadge = `<span class="badge-status badge-rechazada">Rechazada</span>`;
        if (req.status === 'COMPLETED') statusBadge = `<span class="badge-status badge-completada">Completada</span>`;

        const originText = req.originBranch ? req.originBranch.name : 'TechMex Distribuidora';
        const destText = req.destinationBranch ? req.destinationBranch.name : 'CDMX Centro';

        let actionsHtml = `<span class="text-secondary small">-</span>`;
        if (isAdmin && req.status === 'PENDING') {
            actionsHtml = `
                <button class="btn btn-sm btn-outline-success me-1 py-0 px-2" title="Aprobar solicitud" onclick="approveRequest(${req.id})">✓</button>
                <button class="btn btn-sm btn-outline-danger me-1 py-0 px-2" title="Rechazar solicitud" onclick="rejectRequest(${req.id})">✕</button>
            `;
        }

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>#r${req.id}</td>
            <td>${typeBadge}</td>
            <td class="fw-semibold text-white">${req.product.name}</td>
            <td>${originText}</td>
            <td>${destText}</td>
            <td class="fw-bold">${req.quantity}</td>
            <td class="small text-secondary">${req.requester.fullName}</td>
            <td class="small text-secondary">${req.createdAt ? req.createdAt.substring(0, 10) : '2026-09-20'}</td>
            <td>${statusBadge}</td>
            <td class="text-end">${actionsHtml}</td>
        `;
        tbody.appendChild(tr);
    });
}

async function approveRequest(id) {
    const comments = prompt('Comentario de aprobación (opcional):', 'Aprobado');
    if (comments === null) return;
    const res = await authFetch(`${API_BASE}/requests/${id}/approve`, {
        method: 'PUT',
        body: JSON.stringify({ adminComments: comments })
    }).then(r => r.json());

    if (res.success) {
        alert('Solicitud aprobada y stock actualizado automáticamente.');
        loadAllData().then(renderRequestsView);
    } else {
        alert(res.message);
    }
}

async function rejectRequest(id) {
    const comments = prompt('Motivo del rechazo:', 'Stock insuficiente o inviable');
    if (comments === null) return;
    const res = await authFetch(`${API_BASE}/requests/${id}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ adminComments: comments })
    }).then(r => r.json());

    if (res.success) {
        alert('Solicitud rechazada.');
        loadAllData().then(renderRequestsView);
    } else {
        alert(res.message);
    }
}

// # Administración de usuarios y personal por sucursal
function renderEmployeesView() {
    const grid = document.getElementById('empBranchMiniCards');
    grid.innerHTML = '';

    cachedBranches.slice(0, 16).forEach(b => {
        const emps = cachedEmployees.filter(e => e.branch && e.branch.id === b.id);
        const col = document.createElement('div');
        col.className = 'col-md-3 col-sm-4';
        col.innerHTML = `
            <div class="card-dark p-2 text-center">
                <div class="small text-secondary fw-semibold text-truncate">${b.name}</div>
                <div class="fw-bold text-white fs-5">${emps.length || 2}</div>
            </div>
        `;
        grid.appendChild(col);
    });

    renderEmployeesTable();
}

function renderEmployeesTable() {
    const tbody = document.getElementById('employeesFullTbody');
    tbody.innerHTML = '';

    const search = (document.getElementById('empSearchInput').value || '').toLowerCase();
    const branchFilter = document.getElementById('empBranchFilter').value;
    const roleFilter = document.getElementById('empRoleFilter').value;

    let list = cachedEmployees;
    if (search) {
        list = list.filter(e => e.fullName.toLowerCase().includes(search) || (e.email && e.email.toLowerCase().includes(search)));
    }
    if (branchFilter) {
        list = list.filter(e => e.branch && e.branch.id === parseInt(branchFilter));
    }
    if (roleFilter) {
        list = list.filter(e => e.role === roleFilter);
    }

    document.getElementById('employeesCountSubtitle').textContent = `${list.length} resultados`;

    list.forEach(emp => {
        const initials = emp.fullName.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase();
        const roleBadge = emp.role === 'ROLE_ADMIN' ?
            `<span class="badge-status badge-sin-stock">Admin</span>` :
            `<span class="badge-status badge-pendiente">Gerente</span>`;

        const statusBadge = emp.active ?
            `<span class="badge-status badge-normal">Activo</span>` :
            `<span class="badge-status badge-completada">Inactivo</span>`;

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>
                <div class="d-flex align-items-center gap-2">
                    <div class="user-avatar" style="width: 28px; height: 28px; font-size: 0.72rem;">${initials}</div>
                    <span class="fw-semibold text-white">${emp.fullName}</span>
                </div>
            </td>
            <td class="text-secondary small">${emp.email || `${emp.username}@corp.mx`}</td>
            <td>${emp.position || 'Empleado'}</td>
            <td>${roleBadge}</td>
            <td>${emp.branch ? emp.branch.name : 'Corporativo'}</td>
            <td class="small text-secondary">${emp.phone || '55-0000-0000'}</td>
            <td class="small text-secondary">2026-09-20</td>
            <td>${statusBadge}</td>
            <td class="text-end">
                <button class="btn-action-icon" onclick="openEditEmployeeModal(${emp.id})"><i class="bi bi-pencil"></i></button>
                <button class="btn-action-icon text-danger" onclick="deleteEmployee(${emp.id})"><i class="bi bi-trash"></i></button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function openNewEmployeeModal() {
    document.getElementById('empEditId').value = '';
    document.getElementById('formNuevoEmpleado').reset();
    document.getElementById('modalEmpActive').checked = true;
    new bootstrap.Modal(document.getElementById('modalNuevoEmpleado')).show();
}

function openEditEmployeeModal(id) {
    const emp = cachedEmployees.find(e => e.id === id);
    if (!emp) return;

    document.getElementById('empEditId').value = emp.id;
    document.getElementById('modalEmpFullName').value = emp.fullName;
    document.getElementById('modalEmpEmail').value = emp.email || '';
    document.getElementById('modalEmpUsername').value = emp.username;
    document.getElementById('modalEmpPassword').value = '';
    if (emp.branch) document.getElementById('modalEmpBranch').value = emp.branch.id;
    document.getElementById('modalEmpRole').value = emp.role;
    document.getElementById('modalEmpPosition').value = emp.position || '';
    document.getElementById('modalEmpPhone').value = emp.phone || '';
    document.getElementById('modalEmpActive').checked = emp.active;

    new bootstrap.Modal(document.getElementById('modalNuevoEmpleado')).show();
}

async function deleteEmployee(id) {
    if (!confirm('¿Desea desactivar a este empleado?')) return;
    const res = await authFetch(`${API_BASE}/employees/${id}`, { method: 'DELETE' }).then(r => r.json());
    if (res.success) {
        alert('Empleado desactivado.');
        loadAllData().then(renderEmployeesView);
    }
}

// # Diálogos modales para creación de solicitudes y productos
function selectRequestType(type) {
    document.getElementById('modalReqType').value = type;
    const cardSup = document.getElementById('toggleCardSupplier');
    const cardTra = document.getElementById('toggleCardTransfer');
    const supGroup = document.getElementById('fieldSupplierGroup');
    const traGroup = document.getElementById('fieldTransferOriginGroup');

    if (type === 'SUPPLIER') {
        cardSup.classList.add('active-supplier');
        cardTra.classList.remove('active-transfer');
        supGroup.classList.remove('d-none');
        traGroup.classList.add('d-none');
    } else {
        cardTra.classList.add('active-transfer');
        cardSup.classList.remove('active-supplier');
        supGroup.classList.add('d-none');
        traGroup.classList.remove('d-none');
    }
}

function openNewRequestModal(productId = null, defaultType = 'SUPPLIER', targetBranchId = null) {
    selectRequestType(defaultType);

    const destSelect = document.getElementById('modalReqDestBranch');
    const destBranchId = targetBranchId || (currentUser && currentUser.branchId ? currentUser.branchId : (destSelect ? parseInt(destSelect.value) : 1));
    if (targetBranchId) {
        destSelect.value = targetBranchId;
    } else if (currentUser.branchId) {
        destSelect.value = currentUser.branchId;
    }

    if (productId && destBranchId && hasActiveRequest(destBranchId, productId)) {
        const existing = getActivePendingRequest(destBranchId, productId);
        alert(`Ya existe una solicitud pendiente de reposición para este producto (#r${existing.id}). No es posible duplicar solicitudes en proceso.`);
        return;
    }

    if (productId) {
        document.getElementById('modalReqProduct').value = productId;
        document.getElementById('modalReqQuantity').value = 10;
        document.getElementById('modalReqNotes').value = 'Solicitud de reposición automática por estado bajo/crítico.';
    } else {
        document.getElementById('modalReqQuantity').value = 1;
        document.getElementById('modalReqNotes').value = '';
    }

    new bootstrap.Modal(document.getElementById('modalNuevaSolicitud')).show();
}

function openNewProductModal() {
    document.getElementById('prodEditId').value = '';
    document.getElementById('formNuevoProducto').reset();
    new bootstrap.Modal(document.getElementById('modalNuevoProducto')).show();
}

// # Inicialización de escuchadores de eventos y formularios
function setupEventListeners() {
    // Login Form
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', e => {
            e.preventDefault();
            const u = document.getElementById('loginUsername').value.trim();
            const p = document.getElementById('loginPassword').value.trim();
            login(u, p);
        });
    }

    // Logout
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) btnLogout.addEventListener('click', logout);

    // Click en Alertas Topbar -> Redirigir a Inventario
    const topAlertPill = document.getElementById('topbarAlertPill');
    if (topAlertPill) {
        topAlertPill.style.cursor = 'pointer';
        topAlertPill.addEventListener('click', () => {
            if (currentUser) {
                if (currentUser.role === 'ROLE_ADMIN') {
                    switchView('inventario_admin');
                } else {
                    switchView('mi_inventario');
                }
            }
        });
    }

    // Guardar Solicitud
    const btnEnviarSolicitud = document.getElementById('btnEnviarSolicitud');
    if (btnEnviarSolicitud) {
        btnEnviarSolicitud.addEventListener('click', async () => {
            const destBranchId = parseInt(document.getElementById('modalReqDestBranch').value);
            const prodId = parseInt(document.getElementById('modalReqProduct').value);

            if (hasActiveRequest(destBranchId, prodId)) {
                const existing = getActivePendingRequest(destBranchId, prodId);
                alert(`Ya existe una solicitud pendiente de reposición para este producto (#r${existing.id}). No es posible crear solicitudes duplicadas.`);
                return;
            }

            const reqType = document.getElementById('modalReqType').value;
            const payload = {
                requestType: reqType,
                originBranchId: reqType === 'TRANSFER' ? parseInt(document.getElementById('modalReqOriginBranch').value) : null,
                destinationBranchId: destBranchId,
                productId: prodId,
                quantity: parseInt(document.getElementById('modalReqQuantity').value),
                notes: document.getElementById('modalReqNotes').value
            };

            try {
                const res = await authFetch(`${API_BASE}/requests`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                }).then(r => r.json());

                if (res.success) {
                    alert('Solicitud enviada exitosamente en estado PENDIENTE.');
                    bootstrap.Modal.getInstance(document.getElementById('modalNuevaSolicitud')).hide();
                    loadAllData().then(() => switchView(currentActiveView));
                } else {
                    alert(res.message);
                }
            } catch (err) {
                alert('Error al enviar la solicitud.');
            }
        });
    }

    // Guardar Producto
    const btnGuardarProducto = document.getElementById('btnGuardarProducto');
    if (btnGuardarProducto) {
        btnGuardarProducto.addEventListener('click', async () => {
            const editId = document.getElementById('prodEditId').value;
            const payload = {
                sku: document.getElementById('modalProdSku').value.trim(),
                name: document.getElementById('modalProdName').value.trim(),
                category: document.getElementById('modalProdCategory').value,
                unit: document.getElementById('modalProdUnit').value,
                minStockThreshold: parseInt(document.getElementById('modalProdMinStock').value),
                price: parseFloat(document.getElementById('modalProdPrice').value)
            };

            const url = editId ? `${API_BASE}/products/${editId}` : `${API_BASE}/products`;
            const method = editId ? 'PUT' : 'POST';

            const res = await authFetch(url, {
                method,
                body: JSON.stringify(payload)
            }).then(r => r.json());

            if (res.success) {
                alert(editId ? 'Producto actualizado' : 'Producto creado exitosamente');
                bootstrap.Modal.getInstance(document.getElementById('modalNuevoProducto')).hide();
                loadAllData().then(() => switchView(currentActiveView));
            } else {
                alert(res.message);
            }
        });
    }

    // Guardar Empleado (con definición directa de contraseña)
    const btnGuardarEmpleado = document.getElementById('btnGuardarEmpleado');
    if (btnGuardarEmpleado) {
        btnGuardarEmpleado.addEventListener('click', async () => {
            const editId = document.getElementById('empEditId').value;
            const payload = {
                id: editId ? parseInt(editId) : null,
                fullName: document.getElementById('modalEmpFullName').value.trim(),
                email: document.getElementById('modalEmpEmail').value.trim(),
                username: document.getElementById('modalEmpUsername').value.trim(),
                password: document.getElementById('modalEmpPassword').value.trim(),
                branchId: document.getElementById('modalEmpBranch').value ? parseInt(document.getElementById('modalEmpBranch').value) : null,
                role: document.getElementById('modalEmpRole').value,
                position: document.getElementById('modalEmpPosition').value.trim(),
                phone: document.getElementById('modalEmpPhone').value.trim(),
                active: document.getElementById('modalEmpActive').checked
            };

            const url = editId ? `${API_BASE}/employees/${editId}` : `${API_BASE}/employees`;
            const method = editId ? 'PUT' : 'POST';

            const res = await authFetch(url, {
                method,
                body: JSON.stringify(payload)
            }).then(r => r.json());

            if (res.success) {
                alert('Empleado guardado exitosamente con sus credenciales.');
                bootstrap.Modal.getInstance(document.getElementById('modalNuevoEmpleado')).hide();
                loadAllData().then(renderEmployeesView);
            } else {
                alert(res.message);
            }
        });
    }

    // Guardar Ajuste de Existencias / Inventario
    const btnGuardarAjusteStock = document.getElementById('btnGuardarAjusteStock');
    if (btnGuardarAjusteStock) {
        btnGuardarAjusteStock.addEventListener('click', async () => {
            const branchId = document.getElementById('adjustStockBranchId').value;
            const productId = document.getElementById('adjustStockProductId').value;
            const newQty = parseInt(document.getElementById('adjustStockNewQty').value, 10);

            if (isNaN(newQty) || newQty < 0) {
                alert('Ingrese una cantidad válida mayor o igual a 0');
                return;
            }

            try {
                const res = await authFetch(`${API_BASE}/inventory/branch/${branchId}/product/${productId}`, {
                    method: 'PUT',
                    body: JSON.stringify({ quantity: newQty })
                }).then(r => r.json());

                if (res.success) {
                    if (stockModalInstance) stockModalInstance.hide();
                    await loadAllData();
                    renderAdminMatrix();
                } else {
                    alert(res.message || 'Error al actualizar existencias.');
                }
            } catch (err) {
                alert('Error al comunicarse con el servidor.');
            }
        });
    }
}
