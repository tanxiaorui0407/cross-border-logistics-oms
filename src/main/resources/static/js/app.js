/* 跨境物流订单管理系统 - 公共 JS */
const TOKEN_KEY = 'oms_token';
const USER_KEY = 'oms_user';

function getToken() { return localStorage.getItem(TOKEN_KEY); }
function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch (e) { return null; }
}
function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    location.href = '/login';
}

/* 所有 Ajax 自动携带 JWT；401 自动跳登录 */
$.ajaxSetup({
    beforeSend: function (xhr) {
        const t = getToken();
        if (t) { xhr.setRequestHeader('Authorization', 'Bearer ' + t); }
    },
    complete: function (x) {
        if (x.status === 401) { logout(); }
    }
});

function apiGet(url, params) {
    return $.ajax({ url: url, data: params || {}, method: 'GET', dataType: 'json' });
}
function apiPost(url, data) {
    return $.ajax({
        url: url, method: 'POST',
        contentType: 'application/json;charset=UTF-8',
        data: JSON.stringify(data || {}), dataType: 'json'
    });
}
function apiPostForm(url, formData) {
    return $.ajax({ url: url, method: 'POST', data: formData, processData: false, contentType: false, dataType: 'json' });
}

function fmtMoney(v) { return Number(v || 0).toFixed(2); }
function fmtTime(t) { return t ? String(t).replace('T', ' ') : '-'; }

const STATUS_LABEL = {
    DRAFT: '草稿', SUBMITTED: '已提交', PICKED_UP: '已揽收', IN_TRANSIT: '运输中',
    CUSTOMS: '清关中', DELIVERING: '派送中', SIGNED: '已签收',
    EXCEPTION: '异常', CANCELED: '已取消', REJECTED: '拒收',
    OPEN: '未支付', PAID: '已支付'
};
function statusLabel(s) { return STATUS_LABEL[s] || s; }
function statusClass(s) {
    const m = {
        DRAFT: 'secondary', SUBMITTED: 'primary', PICKED_UP: 'info', IN_TRANSIT: 'info',
        CUSTOMS: 'warning', DELIVERING: 'info', SIGNED: 'success',
        EXCEPTION: 'danger', CANCELED: 'secondary', REJECTED: 'danger',
        OPEN: 'warning', PAID: 'success'
    };
    return m[s] || 'secondary';
}

/* 带鉴权的文件下载（fetch 带 Authorization） */
function downloadAuth(url, filename) {
    fetch(url, { headers: { 'Authorization': 'Bearer ' + getToken() } })
        .then(r => { if (!r.ok) throw new Error(r.status); return r.blob(); })
        .then(b => {
            const a = document.createElement('a');
            a.href = URL.createObjectURL(b);
            a.download = filename || 'download.xlsx';
            a.click();
            URL.revokeObjectURL(a.href);
        })
        .catch(() => alert('下载失败，请检查权限'));
}

/* 顶部导航渲染 */
function renderNav(active) {
    const user = getUser();
    if (!user) return;
    const adminLinks = [
        { href: '/admin/users', label: '用户管理', key: 'users' },
        { href: '/admin/customers', label: '客户管理', key: 'customers' },
        { href: '/admin/channels', label: '渠道管理', key: 'channels' },
        { href: '/admin/rates', label: '价卡管理', key: 'rates' }
    ];
    const commonLinks = [
        { href: '/', label: '仪表盘', key: 'index' },
        { href: '/orders', label: '订单管理', key: 'orders' },
        { href: '/quote', label: '运费试算', key: 'quote' },
        { href: '/import', label: '批量导入', key: 'import' },
        { href: '/bills', label: '账单对账', key: 'bills' }
    ];
    let html = '';
    commonLinks.forEach(l => {
        html += `<li class="nav-item"><a class="nav-link ${active === l.key ? 'active' : ''}" href="${l.href}">${l.label}</a></li>`;
    });
    if (user.role === 'ADMIN') {
        adminLinks.forEach(l => {
            html += `<li class="nav-item"><a class="nav-link ${active === l.key ? 'active' : ''}" href="${l.href}">${l.label}</a></li>`;
        });
    }
    $('#navLinks').html(html);
    $('#navUser').html(
        `<span class="navbar-text me-3">${user.username} <span class="badge bg-secondary">${user.role}</span></span>` +
        `<button class="btn btn-outline-light btn-sm" onclick="logout()">退出</button>`
    );
}

/* 页面访问门禁：未登录一律回登录页 */
function requireLogin() {
    if (!getToken()) { location.href = '/login'; return false; }
    return true;
}

function showToast(msg, type) {
    const el = $('#toastMsg');
    if (el.length === 0) {
        $('body').append(
            `<div class="toast-container position-fixed top-0 end-0 p-3"><div id="toastMsg" class="toast" role="alert"><div class="toast-body"></div></div></div>`
        );
    }
    $('#toastMsg .toast-body').text(msg);
    $('#toastMsg').removeClass('text-bg-success text-bg-danger text-bg-warning text-bg-primary')
        .addClass('text-bg-' + (type || 'primary'));
    const t = bootstrap.Toast.getOrCreateInstance($('#toastMsg')[0]);
    t.show();
}

$(function () {
    if (getToken() && $('#navLinks').length) {
        renderNav($('body').data('page'));
    }
});
