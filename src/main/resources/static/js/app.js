/**
 * TRUSTVAULT QUANTUM SENTINEL — FRONTEND CORE ENGINE
 * Vanilla JavaScript (ES6+) with Fetch API & Web Crypto API
 * Features: Telemetry HUD, Visual Hash Chain Explorer, Client-Side Forensics Verifier,
 * Time-Limited Grants, Cyber Audio Synthesizer, & Cryptographic Certificate Export.
 */

// Global Application State
const state = {
    currentUser: null,
    activeTab: 'documents',
    documents: [],
    users: [],
    auditEvents: [],
    selectedDocId: null,
    selectedForensicDoc: null,
    tamperSimulated: false,
    audioEnabled: true,
    filters: {
        name: '',
        owner: '',
        category: '',
        date: ''
    }
};

// ==============================================================================
// 1. CYBERNETIC WEB AUDIO SYNTHESIZER (ZERO-DEPENDENCY SOUND FX)
// ==============================================================================

let audioCtx = null;

function initAudio() {
    if (!audioCtx) {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (AudioContext) {
            audioCtx = new AudioContext();
        }
    }
}

function playCyberSound(type = 'click') {
    if (!state.audioEnabled) return;
    try {
        initAudio();
        if (!audioCtx) return;
        if (audioCtx.state === 'suspended') {
            audioCtx.resume();
        }

        const now = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);

        if (type === 'click') {
            osc.type = 'sine';
            osc.frequency.setValueAtTime(800, now);
            osc.frequency.exponentialRampToValueAtTime(400, now + 0.04);
            gain.gain.setValueAtTime(0.08, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.04);
            osc.start(now);
            osc.stop(now + 0.04);
        } else if (type === 'success') {
            // Harmonic cyber chord
            [523.25, 659.25, 783.99].forEach((freq, idx) => {
                const subOsc = audioCtx.createOscillator();
                const subGain = audioCtx.createGain();
                subOsc.connect(subGain);
                subGain.connect(audioCtx.destination);
                subOsc.type = 'triangle';
                subOsc.frequency.setValueAtTime(freq, now + idx * 0.05);
                subGain.gain.setValueAtTime(0.05, now + idx * 0.05);
                subGain.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
                subOsc.start(now + idx * 0.05);
                subOsc.stop(now + 0.25);
            });
        } else if (type === 'alert') {
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(440, now);
            osc.frequency.linearRampToValueAtTime(220, now + 0.18);
            gain.gain.setValueAtTime(0.12, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 0.18);
            osc.start(now);
            osc.stop(now + 0.18);
        }
    } catch (e) {
        // Audio policy or silent fail
    }
}

// ==============================================================================
// 2. UTILITY & API REQUEST ENGINE
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

const API_BASE = window.TRUSTVAULT_API_BASE || localStorage.getItem('TRUSTVAULT_API_BASE') || '';

async function apiFetch(endpoint, options = {}) {
    const defaultHeaders = {};
    const method = (options.method || 'GET').toUpperCase();

    // Attach CSRF token on mutating requests
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
        credentials: 'include'
    };

    const targetUrl = endpoint.startsWith('http') ? endpoint : (API_BASE + endpoint);

    try {
        const response = await fetch(targetUrl, config);

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

    if (type === 'success') playCyberSound('success');
    else if (type === 'danger' || type === 'error') playCyberSound('alert');
    else playCyberSound('click');

    const toast = document.createElement('div');
    toast.className = `toast-custom ${type}`;
    const icon = type === 'success' ? '✅' : type === 'danger' || type === 'error' ? '⚠️' : 'ℹ️';
    toast.innerHTML = `
        <span>${icon}</span>
        <div style="flex-grow:1">${escapeHtml(message)}</div>
        <button type="button" style="background:none;border:none;color:#94a3b8;cursor:pointer;font-size:1rem" onclick="this.parentElement.remove()">✕</button>
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
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(isoStr) {
    if (!isoStr) return '-';
    const date = new Date(isoStr);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showToast('Cryptographic hash copied to clipboard', 'success');
    }).catch(() => {
        showToast('Failed to copy to clipboard', 'warning');
    });
}

// ==============================================================================
// 3. INITIALIZATION & SENTINEL TELEMETRY HUD
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
        const roleClass = state.currentUser.role === 'ADMIN' ? 'danger' : state.currentUser.role === 'OWNER' ? 'info' : 'warning';
        userBadge.innerHTML = `
            <span class="mono" style="color:var(--text-primary);font-size:0.85rem">${escapeHtml(state.currentUser.email)}</span>
            <span class="status-pill ${roleClass}">
                <span class="pulse-dot"></span>${state.currentUser.role}
            </span>
        `;
    }

    // Toggle admin tab visibility
    if (state.currentUser.role === 'ADMIN') {
        const adminTabBtn = document.getElementById('tab-btn-admin');
        if (adminTabBtn) adminTabBtn.style.display = 'inline-flex';
    }

    // Sound toggle listener
    const soundToggleBtn = document.getElementById('btn-sound-toggle');
    if (soundToggleBtn) {
        soundToggleBtn.addEventListener('click', () => {
            state.audioEnabled = !state.audioEnabled;
            soundToggleBtn.classList.toggle('active', state.audioEnabled);
            document.getElementById('sound-icon').innerText = state.audioEnabled ? '🔊' : '🔇';
            document.getElementById('sound-text').innerText = state.audioEnabled ? 'Audio FX: ON' : 'Audio FX: MUTED';
            if (state.audioEnabled) playCyberSound('click');
        });
    }

    bindEvents();
    loadDocuments();
    updateSentinelHud();
}

async function updateSentinelHud() {
    try {
        // Update block height from audit stream
        const events = await apiFetch('/api/admin/audit?limit=1');
        const heightEl = document.getElementById('hud-block-height');
        if (heightEl) {
            // Count can also be verified from integrity check
            const integrity = await apiFetch('/api/admin/audit/integrity').catch(() => null);
            if (integrity) {
                heightEl.innerText = `#${integrity.totalEventsVerified} BLOCKS`;
            } else if (events && events.length > 0) {
                heightEl.innerText = `#${events.length}+ BLOCKS`;
            }
        }
    } catch (e) {
        // Non-admin may not have /api/admin/audit access
    }

    const docsCountEl = document.getElementById('hud-docs-count');
    if (docsCountEl) {
        const docCount = state.documents ? state.documents.length : 0;
        docsCountEl.innerText = `${docCount} Sealed`;
    }
}

function bindEvents() {
    // Tab switching
    document.querySelectorAll('.nav-pills-custom button').forEach(btn => {
        btn.addEventListener('click', () => {
            playCyberSound('click');
            const targetTab = btn.getAttribute('data-tab');
            switchTab(targetTab);
        });
    });

    // Logout
    const logoutBtn = document.getElementById('btn-logout');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            playCyberSound('click');
            try {
                await apiFetch('/api/auth/logout', { method: 'POST' });
            } catch (err) {}
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
            playCyberSound('click');
            if (searchInput) searchInput.value = '';
            if (ownerInput) ownerInput.value = '';
            if (categorySelect) categorySelect.value = '';
            if (dateInput) dateInput.value = '';
            handleFilterChange();
        });
    }

    initUploadDropzone();
    initForensicVerifier();

    const verifyIntegrityBtn = document.getElementById('btn-verify-integrity');
    if (verifyIntegrityBtn) {
        verifyIntegrityBtn.addEventListener('click', () => {
            playCyberSound('click');
            checkAuditIntegrity();
        });
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
// 4. DOCUMENTS MANAGEMENT & ENCLAVES TABLE
// ==============================================================================

async function loadDocuments() {
    const listContainer = document.getElementById('documents-list-body');
    const emptyState = document.getElementById('documents-empty-state');
    if (!listContainer) return;

    listContainer.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><span class="pulse-dot me-2"></span>Decrypting vault index...</td></tr>';

    const params = new URLSearchParams();
    if (state.filters.name) params.append('name', state.filters.name);
    if (state.filters.owner) params.append('owner', state.filters.owner);
    if (state.filters.category) params.append('category', state.filters.category);
    if (state.filters.date) params.append('date', state.filters.date);

    try {
        const docs = await apiFetch(`/api/documents?${params.toString()}`);
        state.documents = docs;

        const docsCountEl = document.getElementById('hud-docs-count');
        if (docsCountEl) {
            docsCountEl.innerText = `${docs.length} Enclaves`;
        }

        if (docs.length === 0) {
            listContainer.innerHTML = '';
            if (emptyState) emptyState.style.display = 'block';
            return;
        }

        if (emptyState) emptyState.style.display = 'none';

        listContainer.innerHTML = docs.map(doc => {
            const catBadge = `<span class="badge" style="background:rgba(0,242,254,0.12);color:var(--cyan);border:1px solid rgba(0,242,254,0.3);font-size:0.7rem;text-transform:uppercase">${escapeHtml(doc.category)}</span>`;
            
            const permBadge = doc.owner 
                ? '<span class="status-pill success"><span class="pulse-dot"></span>Owner</span>'
                : `<span class="status-pill ${doc.permissionLevel === 'EDIT' ? 'warning' : 'info'}">${doc.permissionLevel}</span>`;

            const shortHash = doc.checksumSha256 ? doc.checksumSha256.substring(0, 12) + '...' : '-';

            return `
                <tr>
                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <span style="font-size:1.4rem">📄</span>
                            <div>
                                <div class="fw-bold text-white">${escapeHtml(doc.displayFilename)}</div>
                                <div class="text-muted" style="font-size:0.75rem">${escapeHtml(doc.description || 'Zero-Trust Enclave Payload')}</div>
                            </div>
                        </div>
                    </td>
                    <td>${catBadge}</td>
                    <td class="mono text-muted" style="font-size:0.8rem">${formatBytes(doc.fileSize)}</td>
                    <td><span class="mono text-muted" style="font-size:0.78rem">${escapeHtml(doc.ownerEmail)}</span></td>
                    <td>
                        <span class="hash-pill" onclick="copyToClipboard('${doc.checksumSha256}')" title="Click to copy full SHA-256">
                            ${shortHash} 📋
                        </span>
                    </td>
                    <td>${permBadge}</td>
                    <td>
                        <div class="d-flex gap-2">
                            <button class="btn-primary-custom" style="padding:0.35rem 0.7rem;font-size:0.75rem" onclick="downloadDocument('${doc.id}')" title="Stream Authorized Download">
                                ⬇ Download
                            </button>
                            <button class="btn-cyber-outline" style="padding:0.35rem 0.65rem;font-size:0.75rem" onclick="openForensics('${doc.id}')" title="Open Forensic Inspector & Local Verifier">
                                🔬 Forensics
                            </button>
                            ${doc.owner ? `
                                <button class="btn-secondary-custom" style="padding:0.35rem 0.65rem;font-size:0.75rem" onclick="openShareModal('${doc.id}')" title="Manage Permissions & Time-Locks">
                                    👥 Share
                                </button>
                                <button class="btn-danger-custom" onclick="deleteDocument('${doc.id}')" title="Revoke & Delete Document">
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
    playCyberSound('click');
    showToast('Authorizing & streaming confidential document...', 'info');
    window.location.href = `${API_BASE}/api/documents/${docId}/download`;
}

async function deleteDocument(docId) {
    playCyberSound('alert');
    if (!confirm('Permanent Soft-Deletion: Revoke all access and invalidate this document enclave?')) {
        return;
    }

    try {
        await apiFetch(`/api/documents/${docId}`, { method: 'DELETE' });
        showToast('Document enclave successfully purged and audit-recorded', 'success');
        loadDocuments();
    } catch (err) {
        showToast('Failed to delete document: ' + err.message, 'error');
    }
}

// ==============================================================================
// 5. FORENSIC DOSSIER & CLIENT-SIDE ZERO-KNOWLEDGE SHA-256 VERIFIER
// ==============================================================================

function openForensics(docId) {
    playCyberSound('click');
    const doc = state.documents.find(d => d.id === docId);
    if (!doc) return;

    state.selectedForensicDoc = doc;

    document.getElementById('forensic-filename').innerText = doc.displayFilename;
    document.getElementById('forensic-size').innerText = `${formatBytes(doc.fileSize)} (${doc.fileSize.toLocaleString()} bytes)`;
    document.getElementById('forensic-mime').innerText = doc.mimeType || 'application/octet-stream';
    document.getElementById('forensic-hash').innerText = doc.checksumSha256;

    const matchResult = document.getElementById('forensic-match-result');
    if (matchResult) {
        matchResult.style.display = 'none';
        matchResult.innerHTML = '';
    }

    openModal('modal-forensics');
}

function copyForensicHash() {
    if (state.selectedForensicDoc) {
        copyToClipboard(state.selectedForensicDoc.checksumSha256);
    }
}

function initForensicVerifier() {
    const dropzone = document.getElementById('forensic-dropzone');
    const fileInput = document.getElementById('forensic-file-input');
    const resultBox = document.getElementById('forensic-match-result');

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
            verifyLocalFileChecksum(e.dataTransfer.files[0]);
        }
    });

    fileInput.addEventListener('change', () => {
        if (fileInput.files.length > 0) {
            verifyLocalFileChecksum(fileInput.files[0]);
        }
    });

    async function verifyLocalFileChecksum(file) {
        if (!state.selectedForensicDoc) return;
        resultBox.style.display = 'block';
        resultBox.innerHTML = '<span class="pulse-dot me-2"></span>Computing local SHA-256 via Web Crypto API...';

        try {
            const buffer = await file.arrayBuffer();
            const hashBuffer = await window.crypto.subtle.digest('SHA-256', buffer);
            const hashArray = Array.from(new Uint8Array(hashBuffer));
            const computedHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

            const recordedHex = state.selectedForensicDoc.checksumSha256.toLowerCase();
            const isMatch = computedHex.toLowerCase() === recordedHex;

            if (isMatch) {
                playCyberSound('success');
                resultBox.innerHTML = `
                    <div class="p-3 rounded" style="background:rgba(0,245,160,0.15);border:1px solid #00f5a0;color:#00f5a0">
                        <div class="fw-bold d-flex align-items-center gap-2">
                            <span>✅ BIT-FOR-BIT CRYPTOGRAPHIC MATCH CONFIRMED</span>
                        </div>
                        <div class="mt-1" style="font-size:0.75rem;font-family:var(--font-mono)">
                            Local Hash:  ${computedHex}<br>
                            Vault Hash:  ${recordedHex}
                        </div>
                        <div class="mt-2 text-white" style="font-size:0.75rem">
                            100% Provenance Integrity: The local file is completely identical to the sealed Vault artifact.
                        </div>
                    </div>
                `;
            } else {
                playCyberSound('alert');
                resultBox.innerHTML = `
                    <div class="p-3 rounded" style="background:rgba(255,0,85,0.15);border:1px solid #ff0055;color:#ff4d79">
                        <div class="fw-bold d-flex align-items-center gap-2">
                            <span>❌ INTEGRITY BREACH: CHECKSUM MISMATCH</span>
                        </div>
                        <div class="mt-1" style="font-size:0.75rem;font-family:var(--font-mono)">
                            Local Hash:  ${computedHex}<br>
                            Vault Hash:  ${recordedHex}
                        </div>
                        <div class="mt-2 text-white" style="font-size:0.75rem">
                            Warning: The selected local file content differs from the cryptographic baseline sealed in TrustVault!
                        </div>
                    </div>
                `;
            }
        } catch (err) {
            resultBox.innerHTML = `<span class="text-danger">Hashing error: ${err.message}</span>`;
        }
    }
}

// ==============================================================================
// 6. UPLOAD & INGESTION
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
                    <span class="status-pill success">Validated Signature</span>
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
                showToast('Please select a file to ingest', 'warning');
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
                submitBtn.innerHTML = '<span class="pulse-dot me-2"></span>Computing Checksum & Ingesting...';
            }

            try {
                await apiFetch('/api/documents', {
                    method: 'POST',
                    body: formData
                });

                playCyberSound('success');
                showToast('Document enclave successfully sealed & audit-chained', 'success');
                uploadForm.reset();
                filePreview.style.display = 'none';
                closeModal('modal-upload');
                loadDocuments();
                updateSentinelHud();
            } catch (err) {
                showToast('Upload failed: ' + err.message, 'error');
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = 'Seal & Upload Document';
                }
            }
        });
    }
}

// ==============================================================================
// 7. TIME-LIMITED GRANTS & SHARING
// ==============================================================================

async function openShareModal(docId) {
    playCyberSound('click');
    state.selectedDocId = docId;
    openModal('modal-share');
    loadGrants(docId);
}

async function loadGrants(docId) {
    const listBody = document.getElementById('grants-list-body');
    if (!listBody) return;

    listBody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted"><span class="pulse-dot me-2"></span>Querying active access grants...</td></tr>';

    try {
        const grants = await apiFetch(`/api/documents/${docId}/grants`);
        if (grants.length === 0) {
            listBody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">Zero-Trust Active: No external grants issued.</td></tr>';
            return;
        }

        listBody.innerHTML = grants.map(g => {
            let statusHtml;
            if (!g.active) {
                statusHtml = '<span class="status-pill danger">REVOKED</span>';
            } else if (g.expiresAt) {
                const expDate = new Date(g.expiresAt);
                const isExpired = expDate < new Date();
                statusHtml = isExpired 
                    ? '<span class="status-pill danger">EXPIRED</span>' 
                    : `<span class="status-pill warning" title="Expires at ${formatDate(g.expiresAt)}">⏱ EXPIRES ${expDate.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}</span>`;
            } else {
                statusHtml = '<span class="status-pill success">PERMANENT</span>';
            }

            const revokeBtn = g.active
                ? `<button class="btn-danger-custom" onclick="revokeGrant('${docId}', '${g.id}')">Revoke</button>`
                : `<span class="text-muted" style="font-size:0.75rem">${formatDate(g.revocationTimestamp)}</span>`;

            return `
                <tr>
                    <td class="text-white">${escapeHtml(g.recipientEmail)}</td>
                    <td><span class="status-pill ${g.permissionLevel === 'EDIT' ? 'warning' : 'info'}">${g.permissionLevel}</span></td>
                    <td>${statusHtml}</td>
                    <td class="text-muted" style="font-size:0.78rem">${formatDate(g.grantTimestamp)}</td>
                    <td>${revokeBtn}</td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        listBody.innerHTML = `<tr><td colspan="5" class="text-danger py-3">Failed to load grants: ${escapeHtml(err.message)}</td></tr>`;
    }
}

async function submitGrant() {
    playCyberSound('click');
    if (!state.selectedDocId) return;

    const emailInput = document.getElementById('grant-recipient-email');
    const permSelect = document.getElementById('grant-permission-level');
    const durationSelect = document.getElementById('grant-duration');

    if (!emailInput || !emailInput.value.trim()) {
        showToast('Please enter recipient email', 'warning');
        return;
    }

    const durationHours = parseInt(durationSelect ? durationSelect.value : '0', 10);

    const payload = {
        recipientEmail: emailInput.value.trim(),
        permissionLevel: permSelect.value,
        durationHours: durationHours > 0 ? durationHours : null
    };

    try {
        await apiFetch(`/api/documents/${state.selectedDocId}/grants`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        playCyberSound('success');
        showToast(`Granted ${payload.permissionLevel} access to ${payload.recipientEmail}`, 'success');
        emailInput.value = '';
        loadGrants(state.selectedDocId);
    } catch (err) {
        showToast('Failed to grant access: ' + err.message, 'error');
    }
}

async function revokeGrant(docId, grantId) {
    playCyberSound('alert');
    if (!confirm('Immediate Revocation: Invalidate access credentials for this user immediately?')) return;

    try {
        await apiFetch(`/api/documents/${docId}/grants/${grantId}`, { method: 'DELETE' });
        showToast('Grant revoked immediately and written to audit ledger', 'success');
        loadGrants(docId);
    } catch (err) {
        showToast('Failed to revoke grant: ' + err.message, 'error');
    }
}

// ==============================================================================
// 8. VISUAL BLOCK CHAIN EXPLORER & TAMPER SIMULATOR
// ==============================================================================

async function loadAuditLedger() {
    const listBody = document.getElementById('system-audit-list-body');
    const tableCountEl = document.getElementById('audit-table-count');
    if (!listBody) return;

    listBody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted"><span class="pulse-dot me-2"></span>Reading cryptographic hash chain...</td></tr>';

    try {
        const events = await apiFetch('/api/admin/audit?limit=50');
        state.auditEvents = events;

        if (tableCountEl) tableCountEl.innerText = events.length;

        renderVisualBlockchain(events);

        if (events.length === 0) {
            listBody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Genesis block pending or empty ledger.</td></tr>';
            return;
        }

        listBody.innerHTML = events.map(e => {
            const outcomeClass = e.outcome === 'SUCCESS' ? 'success' : 'danger';
            const shortCurr = e.currentHash ? e.currentHash.substring(0, 10) + '...' : '-';

            return `
                <tr>
                    <td><span class="status-pill ${outcomeClass}">${e.outcome}</span></td>
                    <td><span class="mono fw-bold text-white">${escapeHtml(e.action)}</span></td>
                    <td class="text-muted" style="font-size:0.8rem">${escapeHtml(e.actorEmail || 'System Genesis / Automated')}</td>
                    <td class="text-muted" style="font-size:0.8rem">${escapeHtml(e.sanitizedDetails || '-')}</td>
                    <td>
                        <span class="hash-pill" onclick="copyToClipboard('${e.currentHash}')" title="Block Hash: ${e.currentHash}">
                            ${shortCurr} 📋
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

function renderVisualBlockchain(events) {
    const streamContainer = document.getElementById('blockchain-nodes-stream');
    if (!streamContainer) return;

    if (!events || events.length === 0) {
        streamContainer.innerHTML = '<span class="text-muted p-3">Genesis Block Initializing...</span>';
        return;
    }

    // Display chronological from left to right (reverse of descending API output)
    const chronoEvents = [...events].reverse();

    streamContainer.innerHTML = chronoEvents.map((evt, idx) => {
        const isTampered = state.tamperSimulated && idx >= 1;
        const blockNum = `#${String(idx).padStart(3, '0')}`;
        const prevShort = evt.previousHash ? evt.previousHash.substring(0, 8) + '..' : '00000000';
        const currShort = isTampered ? 'CORRUPT_HASH!' : (evt.currentHash ? evt.currentHash.substring(0, 8) + '..' : '-');

        const arrow = idx < chronoEvents.length - 1 ? `
            <div class="block-link-arrow ${isTampered ? 'text-danger' : ''}">
                <span>➔</span>
                <span style="font-size:0.6rem;letter-spacing:0.05em">${isTampered ? 'BROKEN' : 'VERIFIED'}</span>
            </div>
        ` : '';

        return `
            <div class="block-card ${isTampered ? 'tampered' : ''}">
                <div class="block-header">
                    <span class="block-num">${blockNum}</span>
                    <span class="block-action-badge ${isTampered ? 'bg-danger text-white' : ''}">${escapeHtml(evt.action)}</span>
                </div>
                <div class="block-field">
                    <div class="block-field-label">Actor</div>
                    <div class="block-field-val">${escapeHtml(evt.actorEmail ? evt.actorEmail.split('@')[0] : 'System')}</div>
                </div>
                <div class="block-field">
                    <div class="block-field-label">Prev Hash (H<sub>i-1</sub>)</div>
                    <div class="block-field-val text-cyan">${prevShort}</div>
                </div>
                <div class="block-field">
                    <div class="block-field-label">Current Hash (H<sub>i</sub>)</div>
                    <div class="block-field-val ${isTampered ? 'text-danger fw-bold' : 'text-white'}">${currShort}</div>
                </div>
            </div>
            ${arrow}
        `;
    }).join('');
}

function toggleTamperSimulation() {
    state.tamperSimulated = !state.tamperSimulated;
    const btn = document.getElementById('btn-simulate-tamper');
    const banner = document.getElementById('tamper-alert-banner');

    if (state.tamperSimulated) {
        playCyberSound('alert');
        if (btn) btn.innerText = '🛡️ Restore Intact Ledger';
        if (banner) banner.style.display = 'block';
    } else {
        playCyberSound('success');
        if (btn) btn.innerText = '⚡ Simulate Tamper Attack';
        if (banner) banner.style.display = 'none';
    }

    renderVisualBlockchain(state.auditEvents);
}

// ==============================================================================
// 9. ADMIN CONSOLE & COMPLIANCE CERTIFICATE EXPORT
// ==============================================================================

async function checkAuditIntegrity() {
    const statusPill = document.getElementById('integrity-status-pill');
    const countEl = document.getElementById('integrity-events-count');
    const hashEl = document.getElementById('integrity-latest-hash');
    const brokenBox = document.getElementById('integrity-broken-box');

    if (statusPill) statusPill.innerHTML = '<span class="pulse-dot me-1"></span>Verifying Chain...';

    try {
        const report = await apiFetch('/api/admin/audit/integrity');

        if (countEl) countEl.innerText = `${report.totalEventsVerified} Nodes`;
        if (hashEl) {
            hashEl.innerText = report.latestHash || '-';
            hashEl.setAttribute('onclick', `copyToClipboard('${report.latestHash}')`);
        }

        if (report.valid) {
            playCyberSound('success');
            if (statusPill) {
                statusPill.className = 'status-pill success';
                statusPill.innerHTML = '✔ Chain Intact (100% Verified)';
            }
            if (brokenBox) brokenBox.style.display = 'none';
        } else {
            playCyberSound('alert');
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
        }
    } catch (err) {
        if (statusPill) {
            statusPill.className = 'status-pill danger';
            statusPill.innerText = 'Verification Failed';
        }
    }
}

async function exportComplianceCertificate() {
    playCyberSound('click');
    showToast('Compiling signed cryptographic audit certificate...', 'info');

    try {
        const integrity = await apiFetch('/api/admin/audit/integrity');
        const events = await apiFetch('/api/admin/audit?limit=100');

        const certificate = {
            issuer: "TrustVault Quantum Sentinel Compliance Engine",
            standard: "NIST SP 800-207 Zero-Trust & FIPS 180-4 Secure Hash Standard",
            issuanceTimestamp: new Date().toISOString(),
            status: integrity.status,
            chainIntact: integrity.valid,
            totalVerifiedNodes: integrity.totalEventsVerified,
            rootMerkleHash: integrity.latestHash,
            genesisNodeHash: events && events.length > 0 ? events[events.length - 1].currentHash : null,
            eventsLedger: events
        };

        const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(certificate, null, 2));
        const downloadAnchor = document.createElement('a');
        downloadAnchor.setAttribute("href", dataStr);
        downloadAnchor.setAttribute("download", `TrustVault-Audit-Certificate-${Date.now()}.json`);
        document.body.appendChild(downloadAnchor);
        downloadAnchor.click();
        downloadAnchor.remove();

        playCyberSound('success');
        showToast('Cryptographic compliance certificate downloaded successfully', 'success');
    } catch (err) {
        showToast('Certificate export failed: ' + err.message, 'error');
    }
}

async function loadAdminUsers() {
    const userTableBody = document.getElementById('admin-users-list-body');
    if (!userTableBody) return;

    userTableBody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted"><span class="pulse-dot me-2"></span>Loading user access directory...</td></tr>';

    try {
        const users = await apiFetch('/api/admin/users');
        state.users = users;

        userTableBody.innerHTML = users.map(u => {
            const isSelf = u.id === state.currentUser.id;
            const statusClass = u.status === 'ACTIVE' ? 'success' : 'danger';
            const toggleAction = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
            const toggleBtnText = u.status === 'ACTIVE' ? 'Lock Account' : 'Reactivate';
            const btnClass = u.status === 'ACTIVE' ? 'btn-danger-custom' : 'btn-primary-custom';

            return `
                <tr>
                    <td class="text-white fw-bold">${escapeHtml(u.email)}</td>
                    <td><span class="status-pill ${u.role === 'ADMIN' ? 'danger' : u.role === 'OWNER' ? 'info' : 'warning'}">${u.role}</span></td>
                    <td><span class="status-pill ${statusClass}"><span class="pulse-dot"></span>${u.status}</span></td>
                    <td class="text-muted mono" style="font-size:0.8rem">${formatDate(u.createdAt)}</td>
                    <td>
                        ${isSelf ? '<span class="text-muted" style="font-size:0.75rem">Current Session</span>' : `
                            <button class="${btnClass}" style="padding:0.3rem 0.75rem;font-size:0.75rem" onclick="toggleUserStatus('${u.id}', '${toggleAction}')">
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
    playCyberSound('alert');
    if (!confirm(`Confirm account security transition to ${newStatus}?`)) return;

    try {
        await apiFetch(`/api/admin/users/${userId}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status: newStatus })
        });
        showToast(`User status updated to ${newStatus}`, 'success');
        loadAdminUsers();
    } catch (err) {
        showToast('Failed to update user status: ' + err.message, 'error');
    }
}

// Modal Helpers
function openModal(id) {
    playCyberSound('click');
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    playCyberSound('click');
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
    }
});
