// ================================
// k6 Load Test Script — Cloudys API
// ================================
// 使用方法: k6 run --env BASE_URL=http://localhost:25698 deploy/perf/k6-load-test.js
// ================================

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');
const apiDuration = new Trend('api_duration');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:25698';
const TEST_USER = __ENV.TEST_USER || 'admin_test';
const TEST_PASS = __ENV.TEST_PASS || 'AdminTest123!';
const TEST_DISPLAY = __ENV.TEST_DISPLAY || 'Load Test Admin';

export const options = {
    // Smoke test thresholds
    thresholds: {
        'errors': ['rate<0.1'],        // Error rate < 10%
        'login_duration': ['p(95)<3000'], // Login p95 < 3s
        'api_duration': ['p(95)<2000'],   // API p95 < 2s
        'http_req_duration': ['p(95)<3000'],
    },
    // Ramp-up scenario
    scenarios: {
        smoke_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 5 },   // Ramp to 5 users
                { duration: '20s', target: 5 },   // Stay at 5 users
                { duration: '5s', target: 0 },    // Ramp down
            ],
        },
    },
};

let authToken = '';
let userId = '';

export default function () {
    group('Auth Flow', function () {
        // Step 1: Register (idempotent — may fail if user exists)
        let registerRes = http.post(`${BASE_URL}/api/v2/auth/register`, JSON.stringify({
            username: `${TEST_USER}_${__VU}`,
            password: TEST_PASS,
            display_name: `${TEST_DISPLAY} ${__VU}`,
            role: 'super_admin',
        }), {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'register' },
        });
        // Register may 200 (new) or 409 (already exists) — both acceptable
        check(registerRes, {
            'register status ok': (r) => r.status === 200 || r.status === 409,
        }) || errorRate.add(1);

        // Step 2: Login
        let loginStart = Date.now();
        let loginRes = http.post(`${BASE_URL}/api/v2/auth/login`, JSON.stringify({
            username: `${TEST_USER}_${__VU}`,
            password: TEST_PASS,
        }), {
            headers: { 'Content-Type': 'application/json' },
            tags: { name: 'login' },
        });
        loginDuration.add(Date.now() - loginStart);

        let loginOk = check(loginRes, {
            'login status 200': (r) => r.status === 200,
            'has token': (r) => r.json('token') != null,
        });
        if (!loginOk) {
            errorRate.add(1);
            return;
        }
        authToken = loginRes.json('token');
        userId = loginRes.json('user_id');

        // Step 3: Verify token
        let verifyStart = Date.now();
        let verifyRes = http.get(`${BASE_URL}/api/v2/auth/verify`, {
            headers: { 'Authorization': `Bearer ${authToken}` },
            tags: { name: 'verify' },
        });
        apiDuration.add(Date.now() - verifyStart);
        check(verifyRes, { 'verify status 200': (r) => r.status === 200 }) || errorRate.add(1);
    });

    group('Project Flow', function () {
        if (!authToken) return;

        // Create product
        let prodRes = http.post(`${BASE_URL}/api/v2/product/products`, JSON.stringify({
            name: `Perf Test Product ${__VU}`,
            description: 'Load test product',
        }), {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`,
            },
            tags: { name: 'create_product' },
        });
        check(prodRes, { 'create product status 200': (r) => r.status === 200 }) || errorRate.add(1);
        let productId = prodRes.json('product_id');

        // List projects
        let listStart = Date.now();
        let listRes = http.get(`${BASE_URL}/api/v2/manage/projects`, {
            headers: { 'Authorization': `Bearer ${authToken}` },
            tags: { name: 'list_projects' },
        });
        apiDuration.add(Date.now() - listStart);
        check(listRes, { 'list projects status 200': (r) => r.status === 200 }) || errorRate.add(1);

        // Create project
        if (productId) {
            let projRes = http.post(`${BASE_URL}/api/v2/manage/projects`, JSON.stringify({
                name: `Perf Test Project ${__VU}`,
                description: 'Load test project',
                product_id: productId,
            }), {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${authToken}`,
                },
                tags: { name: 'create_project' },
            });
            check(projRes, { 'create project status 200': (r) => r.status === 200 }) || errorRate.add(1);
        }
    });

    group('Requirement Flow', function () {
        if (!authToken) return;

        // List requirements
        let reqStart = Date.now();
        let reqRes = http.get(`${BASE_URL}/api/v2/requirements/projects/dummy/requirements`, {
            headers: { 'Authorization': `Bearer ${authToken}` },
            tags: { name: 'list_requirements' },
        });
        apiDuration.add(Date.now() - reqStart);
        // May 200 or 404 (no project) — both acceptable for load test
    });

    group('Health Check', function () {
        let healthStart = Date.now();
        let healthRes = http.get(`${BASE_URL}/health`, {
            tags: { name: 'health' },
        });
        apiDuration.add(Date.now() - healthStart);
        check(healthRes, { 'health status 200': (r) => r.status === 200 }) || errorRate.add(1);
    });

    sleep(1);
}

export function setup() {
    console.log(`Starting Cloudys load test against ${BASE_URL}`);
    return {};
}

export function teardown() {
    console.log('Cloudys load test completed.');
}
