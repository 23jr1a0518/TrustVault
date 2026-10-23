/**
 * TRUSTVAULT - FRONTEND CORE CONTROLLER
 * Vanilla JavaScript (ES6+) with Fetch API & Relative Backend Endpoints
 */

// Global Application State
const state = {
    currentUser: null,
    activeTab: 'documents',
    documents: [],
    users: [],
    selectedDocId: null,
    filters: {
        name: '',
        owner: '',
        category: '',
        date: ''
    }
};

// ==============================================================================
// 1. UTILITY & API REQUEST ENGINE
// ==============================================================================

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

function getCsrfToken() {
    return getCookie('XSRF-TOKEN');
}

async function apiFetch(endpoint, options = {}) {
    const defaultHeaders = {};
    const method = (options.method || 'GET').toUpperCase();

    // Attach CSRF token on mutating HTTP requests
    if (method !== 'GET' && method !== 'HEAD') {
        const csrfToken = getCsrfToken();
        if (csrfToken) {
            defaultHeaders['X-XSRF-TOKEN'] = csrfToken;
        }
    }

    if (!(options.body instanceof FormData)) {
        defaultHeaders['Content-Type'] = 'application/json';
    }

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers
        },
        credentials: 'same-origin'
    };

    try {
        const response = await fetch(endpoint, config);

        if (response.status === 401) {
            console.warn('Session expired or unauthorized. Redirecting to login.');
            window.location.href = '/login.html';
            throw new Error('Unauthorized');
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get('content-type') || '';
        let data = null;
        if (contentType.includes('application/json')) {
            data = await response.json();
        } else if (contentType.includes('text/')) {
            data = await response.text();
        }

        if (!response.ok) {
            const errorMsg = data && (data.message || data.error) ? (data.message || data.error) : `HTTP ${response.status}`;
            throw new Error(errorMsg);
        }

        return data;
    } catch (err) {
        throw err;
    }
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast-item ${type}`;
    toast.innerHTML = `
        <span>${escapeHtml(message)}</span>
        <button type="button" style="background:none;border:none;color:#94a3b8;cursor:pointer;margin-left:10px" onclick="this.parentElement.remove()">✕</button>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentElement) toast.remove();
    }, 4500);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>'"]/g, 
        tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
    );
}

function formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(isoStr) {
    if (!isoStr) return '-';
    const date = new Date(isoStr);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showToast('Checksum copied to clipboard', 'success');
    }).catch(() => {
        showToast('Failed to copy to clipboard', 'warning');
    });
}

// ==============================================================================
// 2. INITIALIZATION & AUTH CHECK
// ==============================================================================

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const user = await apiFetch('/api/auth/me');
        state.currentUser = user;
        initDashboard();
    } catch (err) {
        window.location.href = '/login.html';
    }
});

function initDashboard() {
    // Render current user badge
    const userBadge = document.getElementById('user-profile-badge');
    if (userBadge) {
        userBadge.innerHTML = `
            <span class="mono" style="color:var(--text-primary)">${escapeHtml(state.currentUser.email)}</span>
            <span class="status-pill ${state.currentUser.role === 'ADMIN' ? 'danger' : state.currentUser.role === 'OWNER' ? 'info' : 'warning'}">
                ${state.currentUser.role}
            </span>
        `;
    }

    // Toggle admin tab visibility
    if (state.currentUser.role === 'ADMIN') {
        const adminTabBtn = document.getElementById('tab-btn-admin');
        if (adminTabBtn) adminTabBtn.style.display = 'inline-flex';
    }

    // Bind event listeners
    bindEvents();

    // Initial load of documents
    loadDocuments();
}

function bindEvents() {
    // Tab switching
    document.querySelectorAll('.nav-pills-custom button').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const targetTab = btn.getAttribute('data-tab');
            switchTab(targetTab);
        });
    });

    // Logout
    const logoutBtn = document.getElementById('btn-logout');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            try {
                await apiFetch('/api/auth/logout', { method: 'POST' });
            } catch (err) {
                console.error(err);
            }
            window.location.href = '/login.html';
        });
    }

    // Search filters
    const searchInput = document.getElementById('filter-name');
    const ownerInput = document.getElementById('filter-owner');
    const categorySelect = document.getElementById('filter-category');
    const dateInput = document.getElementById('filter-date');
    const clearFiltersBtn = document.getElementById('btn-clear-filters');

    const handleFilterChange = () => {
        state.filters.name = searchInput ? searchInput.value : '';
        state.filters.owner = ownerInput ? ownerInput.value : '';
        state.filters.category = categorySelect ? categorySelect.value : '';
        state.filters.date = dateInput ? dateInput.value : '';
        loadDocuments();
    };

    if (searchInput) searchInput.addEventListener('input', debounce(handleFilterChange, 300));
    if (ownerInput) ownerInput.addEventListener('input', debounce(handleFilterChange, 300));
    if (categorySelect) categorySelect.addEventListener('change', handleFilterChange);
    if (dateInput) dateInput.addEventListener('change', handleFilterChange);

    if (clearFiltersBtn) {
        clearFiltersBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            if (ownerInput) ownerInput.value = '';
            if (categorySelect) categorySelect.value = '';
            if (dateInput) dateInput.value = '';
            handleFilterChange();
        });
    }

    // Upload Modal bindings
    initUploadDropzone();

    // Verify Integrity Button
    const verifyIntegrityBtn = document.getElementById('btn-verify-integrity');
    if (verifyIntegrityBtn) {
        verifyIntegrityBtn.addEventListener('click', checkAuditIntegrity);
    }
}

function debounce(func, delay) {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), delay);
    };
}

function switchTab(tabId) {
    state.activeTab = tabId;

    document.querySelectorAll('.nav-pills-custom button').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
    });

    document.querySelectorAll('.tab-content-panel').forEach(panel => {
        panel.style.display = panel.id === `panel-${tabId}` ? 'block' : 'none';
    });

    if (tabId === 'documents') {
        loadDocuments();
    } else if (tabId === 'audit') {
        loadAuditLedger();
    } else if (tabId === 'admin') {
        loadAdminUsers();
        checkAuditIntegrity();
    }
}

// ==============================================================================
// 3. DOCUMENTS MANAGEMENT & SEARCH
// ==============================================================================

async function loadDocuments() {
    const listContainer = document.getElementById('documents-list-body');
    const emptyState = document.getElementById('documents-empty-state');
    if (!listContainer) return;

    listContainer.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><span class="pulse-dot me-2"></span>Loading encrypted documents...</td></tr>';

    const params = new URLSearchParams();
    if (state.filters.name) params.append('name', state.filters.name);
    if (state.filters.owner) params.append('owner', state.filters.owner);
    if (state.filters.category && state.filters.category !== 'ALL') params.append('category', state.filters.category);
    if (state.filters.date) params.append('date', state.filters.date);

    try {
        const docs = await apiFetch(`/api/documents?${params.toString()}`);
        state.documents = docs;

        if (docs.length === 0) {
            listContainer.innerHTML = '';
            if (emptyState) emptyState.style.display = 'block';
            return;
        }

        if (emptyState) emptyState.style.display = 'none';

        listContainer.innerHTML = docs.map(doc => {
            const catClass = doc.category ? doc.category.toLowerCase() : 'general';
            const permBadge = doc.owner 
                ? '<span class="status-pill success"><span class="pulse-dot"></span>Owner</span>'
                : `<span class="status-pill ${doc.permissionLevel === 'EDIT' ? 'warning' : 'info'}">${doc.permissionLevel}</span>`;

            const shortHash = doc.checksumSha256 ? doc.checksumSha256.substring(0, 10) + '...' : '-';

            return `
                <tr>
                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <span style="font-size:1.25rem">📄</span>
                            <div>
                                <div class="fw-semibold text-white">${escapeHtml(doc.displayFilename)}</div>
                                <div class="text-muted" style="font-size:0.75rem">${escapeHtml(doc.description || 'No description')}</div>
                            </div>
                        </div>
                    </td>
                    <td><span class="badge-cat ${catClass}">${escapeHtml(doc.category)}</span></td>
                    <td class="mono text-muted">${formatBytes(doc.fileSize)}</td>
                    <td><span class="mono text-muted" style="font-size:0.8rem">${escapeHtml(doc.ownerEmail)}</span></td>
                    <td>
                        <span class="hash-pill" onclick="copyToClipboard('${doc.checksumSha256}')" title="Click to copy full SHA-256">
                            ${shortHash} 📋
                        </span>
                    </td>
                    <td>${permBadge}</td>
                    <td>
                        <div class="d-flex gap-2">
                            <button class="btn-primary-custom" style="padding:0.35rem 0.65rem;font-size:0.75rem" onclick="downloadDocument('${doc.id}')" title="Download Document">
                                ⬇ Download
                            </button>
                            ${doc.owner ? `
                                <button class="btn-secondary-custom" style="padding:0.35rem 0.65rem;font-size:0.75rem" onclick="openShareModal('${doc.id}')" title="Manage Sharing Grants">
                                    👥 Share
                                </button>
                            ` : ''}
                            ${(doc.owner || doc.permissionLevel === 'EDIT') ? `
                                <button class="btn-secondary-custom" style="padding:0.35rem 0.65rem;font-size:0.75rem" onclick="openEditModal('${doc.id}')" title="Edit Metadata">
                                    ✏️ Edit
                                </button>
                            ` : ''}
                            <button class="btn-secondary-custom" style="padding:0.35rem 0.65rem;font-size:0.75rem" onclick="viewDocAudit('${doc.id}', '${escapeHtml(doc.displayFilename)}')" title="Audit Trail">
                                📜 Audit
                            </button>
                            ${doc.owner ? `
                                <button class="btn-danger-custom" onclick="deleteDocument('${doc.id}')" title="Delete Document">
                                    🗑
                                </button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        listContainer.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">Failed to load documents: ${escapeHtml(err.message)}</td></tr>`;
        showToast('Error loading documents: ' + err.message, 'error');
    }
}

async function downloadDocument(docId) {
    try {
        showToast('Initiating secure authenticated download...', 'info');
        // Direct stream download via browser navigation to authorized backend endpoint
        window.location.href = `/api/documents/${docId}/download`;
    } catch (err) {
        showToast('Download error: ' + err.message, 'error');
    }
}

async function deleteDocument(docId) {
    if (!confirm('Are you sure you want to delete this document? It will be permanently soft-deleted and access revoked.')) {
        return;
    }

    try {
        await apiFetch(`/api/documents/${docId}`, { method: 'DELETE' });
        showToast('Document deleted successfully', 'success');
        loadDocuments();
    } catch (err) {
        showToast('Failed to delete document: ' + err.message, 'error');
    }
}

// ==============================================================================
// 4. SECURE DOCUMENT UPLOAD
// ==============================================================================

function initUploadDropzone() {
    const dropzone = document.getElementById('upload-dropzone');
    const fileInput = document.getElementById('file-upload-input');
    const filePreview = document.getElementById('file-preview-info');
    const uploadForm = document.getElementById('form-upload-doc');

    if (!dropzone || !fileInput) return;

    dropzone.addEventListener('click', () => fileInput.click());

    dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.classList.add('dragover');
    });

    dropzone.addEventListener('dragleave', () => {
        dropzone.classList.remove('dragover');
    });

    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            fileInput.files = e.dataTransfer.files;
            updateFilePreview();
        }
    });

    fileInput.addEventListener('change', updateFilePreview);

    function updateFilePreview() {
        if (fileInput.files.length > 0) {
            const file = fileInput.files[0];
            filePreview.style.display = 'block';
            filePreview.innerHTML = `
                <div class="glass-panel p-2 mt-2 d-flex justify-content-between align-items-center">
                    <div>
                        <span class="text-white fw-medium">📎 ${escapeHtml(file.name)}</span>
                        <span class="text-muted ms-2 mono">${formatBytes(file.size)}</span>
                    </div>
                    <span class="status-pill success">Ready for upload</span>
                </div>
            `;
        } else {
            filePreview.style.display = 'none';
        }
    }

    if (uploadForm) {
        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!fileInput.files || fileInput.files.length === 0) {
                showToast('Please select a file to upload', 'warning');
                return;
            }

            const file = fileInput.files[0];
            if (file.size > 20 * 1024 * 1024) {
                showToast('File size exceeds the 20MB limit', 'danger');
                return;
            }

            const category = document.getElementById('upload-category').value;
            const description = document.getElementById('upload-description').value;

            const formData = new FormData();
            formData.append('file', file);
            formData.append('category', category);
            formData.append('description', description);

            const submitBtn = document.getElementById('btn-submit-upload');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="pulse-dot me-2"></span>Encrypting & Uploading...';
            }

            try {
                await apiFetch('/api/documents', {
                    method: 'POST',
                    body: formData
                });

                showToast('Document encrypted and stored securely', 'success');
                uploadForm.reset();
                filePreview.style.display = 'none';
                closeModal('modal-upload');
                loadDocuments();
            } catch (err) {
                showToast('Upload failed: ' + err.message, 'error');
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = 'Upload Document';
                }
            }
        });
    }
}

// ==============================================================================
// 5. DOCUMENT SHARING & GRANTS MODAL
// ==============================================================================

async function openShareModal(docId) {
    state.selectedDocId = docId;
    const modal = document.getElementById('modal-share');
    if (!modal) return;

    modal.classList.add('active');
    loadGrants(docId);
}

async function loadGrants(docId) {
    const listBody = document.getElementById('grants-list-body');
    if (!listBody) return;

    listBody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">Loading access grants...</td></tr>';

    try {
        const grants = await apiFetch(`/api/documents/${docId}/grants`);
        if (grants.length === 0) {
            listBody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">No grants created yet. Only you have access.</td></tr>';
            return;
        }

        listBody.innerHTML = grants.map(g => {
            const statusHtml = g.active
                ? '<span class="status-pill success">ACTIVE</span>'
                : '<span class="status-pill danger">REVOKED</span>';

            const revokeBtn = g.active
                ? `<button class="btn-danger-custom" onclick="revokeGrant('${docId}', '${g.id}')">Revoke</button>`
                : `<span class="text-muted" style="font-size:0.75rem">${formatDate(g.revocationTimestamp)}</span>`;

            return `
                <tr>
                    <td class="text-white">${escapeHtml(g.recipientEmail)}</td>
                    <td><span class="status-pill ${g.permissionLevel === 'EDIT' ? 'warning' : 'info'}">${g.permissionLevel}</span></td>
                    <td>${statusHtml}</td>
                    <td class="text-muted" style="font-size:0.8rem">${formatDate(g.grantTimestamp)}</td>
                    <td>${revokeBtn}</td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        listBody.innerHTML = `<tr><td colspan="5" class="text-danger py-3">Failed to load grants: ${escapeHtml(err.message)}</td></tr>`;
    }
}

async function submitNewGrant() {
    if (!state.selectedDocId) return;

    const emailInput = document.getElementById('grant-recipient-email');
    const permSelect = document.getElementById('grant-permission-level');

    if (!emailInput || !emailInput.value.trim()) {
        showToast('Please enter recipient email', 'warning');
        return;
    }

    const payload = {
        recipientEmail: emailInput.value.trim(),
        permissionLevel: permSelect.value
    };

    try {
        await apiFetch(`/api/documents/${state.selectedDocId}/grants`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        showToast(`Granted ${payload.permissionLevel} access to ${payload.recipientEmail}`, 'success');
        emailInput.value = '';
        loadGrants(state.selectedDocId);
    } catch (err) {
        showToast('Failed to grant access: ' + err.message, 'error');
    }
}

async function revokeGrant(docId, grantId) {
    if (!confirm('Revoke access immediately for this user?')) return;

    try {
        await apiFetch(`/api/documents/${docId}/grants/${grantId}`, { method: 'DELETE' });
        showToast('Access revoked immediately', 'success');
        loadGrants(docId);
    } catch (err) {
        showToast('Failed to revoke grant: ' + err.message, 'error');
    }
}

// ==============================================================================
// 6. METADATA EDIT MODAL
// ==============================================================================

async function openEditModal(docId) {
    state.selectedDocId = docId;
    const doc = state.documents.find(d => d.id === docId);
    if (!doc) return;

    document.getElementById('edit-doc-filename').value = doc.displayFilename || '';
    document.getElementById('edit-doc-category').value = doc.category || 'GENERAL';
    document.getElementById('edit-doc-description').value = doc.description || '';

    const modal = document.getElementById('modal-edit');
    if (modal) modal.classList.add('active');
}

async function submitEditMetadata() {
    if (!state.selectedDocId) return;

    const filename = document.getElementById('edit-doc-filename').value.trim();
    const category = document.getElementById('edit-doc-category').value;
    const description = document.getElementById('edit-doc-description').value.trim();

    try {
        await apiFetch(`/api/documents/${state.selectedDocId}`, {
            method: 'PATCH',
            body: JSON.stringify({ displayFilename: filename, category, description })
        });

        showToast('Document metadata updated', 'success');
        closeModal('modal-edit');
        loadDocuments();
    } catch (err) {
        showToast('Update failed: ' + err.message, 'error');
    }
}

// ==============================================================================
// 7. AUDIT TRAIL & LEDGER
// ==============================================================================

async function viewDocAudit(docId, docName) {
    const titleEl = document.getElementById('audit-modal-title');
    if (titleEl) titleEl.innerText = `Audit Trail: ${docName}`;

    const bodyEl = document.getElementById('doc-audit-list-body');
    if (bodyEl) {
        bodyEl.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">Loading audit records...</td></tr>';
    }

    const modal = document.getElementById('modal-doc-audit');
    if (modal) modal.classList.add('active');

    try {
        const events = await apiFetch(`/api/documents/${docId}/audit`);
        if (events.length === 0) {
            bodyEl.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">No audit events recorded for this document.</td></tr>';
            return;
        }

        bodyEl.innerHTML = events.map(e => {
            const outcomeClass = e.outcome === 'SUCCESS' ? 'success' : 'danger';
            return `
                <tr>
                    <td><span class="mono fw-semibold text-white">${escapeHtml(e.action)}</span></td>
                    <td><span class="status-pill ${outcomeClass}">${e.outcome}</span></td>
                    <td class="text-muted" style="font-size:0.8rem">${escapeHtml(e.actorEmail || 'Anonymous')}</td>
                    <td class="text-muted" style="font-size:0.8rem">${escapeHtml(e.sanitizedDetails || '-')}</td>
                    <td class="text-muted mono" style="font-size:0.75rem">${formatDate(e.serverTimestamp)}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        bodyEl.innerHTML = `<tr><td colspan="5" class="text-danger py-3">Error loading audit: ${escapeHtml(err.message)}</td></tr>`;
    }
}

async function loadAuditLedger() {
    const listBody = document.getElementById('system-audit-list-body');
    if (!listBody) return;

    listBody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted"><span class="pulse-dot me-2"></span>Loading audit ledger...</td></tr>';

    try {
        const events = await apiFetch('/api/admin/audit?limit=50');
        if (events.length === 0) {
            listBody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No system audit records found.</td></tr>';
            return;
        }

        listBody.innerHTML = events.map(e => {
            const outcomeClass = e.outcome === 'SUCCESS' ? 'success' : 'danger';
            const shortPrev = e.previousHash ? e.previousHash.substring(0, 8) + '...' : '-';
            const shortCurr = e.currentHash ? e.currentHash.substring(0, 8) + '...' : '-';

            return `
                <tr>
                    <td><span class="status-pill ${outcomeClass}">${e.outcome}</span></td>
                    <td><span class="mono fw-semibold text-white">${escapeHtml(e.action)}</span></td>
                    <td class="text-muted">${escapeHtml(e.actorEmail || 'System / Unauth')}</td>
                    <td class="text-muted" style="font-size:0.8rem">${escapeHtml(e.sanitizedDetails || '-')}</td>
                    <td>
                        <span class="hash-pill" onclick="copyToClipboard('${e.currentHash}')" title="Hash: ${e.currentHash}">
                            ${shortCurr}
                        </span>
                    </td>
                    <td class="text-muted mono" style="font-size:0.75rem">${formatDate(e.serverTimestamp)}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        listBody.innerHTML = `<tr><td colspan="6" class="text-danger py-4">Failed to load audit ledger: ${escapeHtml(err.message)}</td></tr>`;
    }
}

// ==============================================================================
// 8. ADMINISTRATOR CONTROLS & CRYPTOGRAPHIC HASH CHAIN VERIFIER
// ==============================================================================

async function loadAdminUsers() {
    const userTableBody = document.getElementById('admin-users-list-body');
    if (!userTableBody) return;

    userTableBody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">Loading user accounts...</td></tr>';

    try {
        const users = await apiFetch('/api/admin/users');
        state.users = users;

        userTableBody.innerHTML = users.map(u => {
            const isSelf = u.id === state.currentUser.id;
            const statusClass = u.status === 'ACTIVE' ? 'success' : 'danger';
            const toggleAction = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
            const toggleBtnText = u.status === 'ACTIVE' ? 'Disable' : 'Enable';
            const btnClass = u.status === 'ACTIVE' ? 'btn-danger-custom' : 'btn-primary-custom';

            return `
                <tr>
                    <td class="text-white fw-medium">${escapeHtml(u.email)}</td>
                    <td><span class="status-pill ${u.role === 'ADMIN' ? 'danger' : u.role === 'OWNER' ? 'info' : 'warning'}">${u.role}</span></td>
                    <td><span class="status-pill ${statusClass}">${u.status}</span></td>
                    <td class="text-muted mono" style="font-size:0.8rem">${formatDate(u.createdAt)}</td>
                    <td>
                        ${isSelf ? '<span class="text-muted" style="font-size:0.75rem">Current User</span>' : `
                            <button class="${btnClass}" style="padding:0.3rem 0.7rem;font-size:0.75rem" onclick="toggleUserStatus('${u.id}', '${toggleAction}')">
                                ${toggleBtnText}
                            </button>
                        `}
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        userTableBody.innerHTML = `<tr><td colspan="5" class="text-danger py-3">Error loading users: ${escapeHtml(err.message)}</td></tr>`;
    }
}

async function toggleUserStatus(userId, newStatus) {
    if (!confirm(`Are you sure you want to change user status to ${newStatus}?`)) return;

    try {
        await apiFetch(`/api/admin/users/${userId}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status: newStatus })
        });
        showToast(`User status set to ${newStatus}`, 'success');
        loadAdminUsers();
    } catch (err) {
        showToast('Failed to update user status: ' + err.message, 'error');
    }
}

async function checkAuditIntegrity() {
    const statusPill = document.getElementById('integrity-status-pill');
    const countEl = document.getElementById('integrity-total-count');
    const hashEl = document.getElementById('integrity-latest-hash');
    const brokenBox = document.getElementById('integrity-broken-box');

    if (statusPill) statusPill.innerHTML = '<span class="pulse-dot me-1"></span>Verifying Chain...';

    try {
        const report = await apiFetch('/api/admin/audit/integrity');

        if (countEl) countEl.innerText = report.totalEventsVerified;
        if (hashEl) {
            hashEl.innerText = report.latestHash ? report.latestHash.substring(0, 24) + '...' : '-';
            hashEl.setAttribute('onclick', `copyToClipboard('${report.latestHash}')`);
        }

        if (report.valid) {
            if (statusPill) {
                statusPill.className = 'status-pill success';
                statusPill.innerHTML = '✔ Chain Intact (Verified)';
            }
            if (brokenBox) brokenBox.style.display = 'none';
        } else {
            if (statusPill) {
                statusPill.className = 'status-pill danger';
                statusPill.innerHTML = '⚠ TAMPERING DETECTED';
            }
            if (brokenBox) {
                brokenBox.style.display = 'block';
                brokenBox.innerHTML = `
                    <div class="glass-panel p-3 border-danger">
                        <div class="text-danger fw-bold">⚠ Cryptographic Hash Mismatch Detected!</div>
                        <div class="mono text-muted mt-1" style="font-size:0.8rem">
                            Broken Event ID: <span class="text-white">${escapeHtml(report.brokenAtEventId)}</span><br>
                            Calculated Hash: <span class="text-danger">${escapeHtml(report.calculatedHash)}</span><br>
                            Recorded Hash:   <span class="text-warning">${escapeHtml(report.recordedHash)}</span>
                        </div>
                    </div>
                `;
            }
            showToast('Integrity Alert: Cryptographic chain mismatch!', 'danger');
        }
    } catch (err) {
        if (statusPill) {
            statusPill.className = 'status-pill danger';
            statusPill.innerText = 'Verification Failed';
        }
        showToast('Failed to verify audit chain: ' + err.message, 'error');
    }
}

// Modal Helper Functions
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

// Close modals when clicking outside modal box
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
    }
});
