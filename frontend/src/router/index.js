import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

import GuideView from '../views/GuideView.vue'
import LoginView from '../views/beta/LoginView.vue'
import RegisterView from '../views/beta/RegisterView.vue'
import DashboardView from '../views/beta/DashboardView.vue'
import ProjectManageView from '../views/beta/ProjectManageView.vue'
import ProductManageView from '../views/beta/ProductManageView.vue'
import RequirementsCollectionView from '../views/beta/RequirementsCollectionView.vue'
import RequirementsAnalysisView from '../views/beta/RequirementsAnalysisView.vue'
import RequirementsView from '../views/beta/RequirementsView.vue'
import RequirementsSessionView from '../views/beta/RequirementsSessionView.vue'
import AccountView from '../views/beta/AccountView.vue'
import UserManageView from '../views/beta/UserManageView.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            redirect: '/guide',
        },
        {
            path: '/guide',
            name: 'guide',
            component: GuideView,
            meta: { requiresAuth: false },
        },
        {
            path: '/beta',
            name: 'beta',
            redirect: '/beta/login',
            children: [
                {
                    path: 'login',
                    name: 'beta-login',
                    component: LoginView,
                    meta: { requiresAuth: false },
                },
                {
                    path: 'register',
                    name: 'beta-register',
                    component: RegisterView,
                    meta: { requiresAuth: false },
                },
                {
                    path: 'dashboard',
                    name: 'beta-dashboard',
                    component: DashboardView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'project',
                    name: 'beta-project',
                    component: ProjectManageView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'products',
                    name: 'beta-products',
                    component: ProductManageView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-collection',
                    name: 'beta-requirements-collection',
                    component: RequirementsCollectionView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-analysis',
                    name: 'beta-requirements-analysis',
                    component: RequirementsAnalysisView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements',
                    name: 'beta-requirements',
                    component: RequirementsView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-session',
                    name: 'beta-requirements-session',
                    component: RequirementsSessionView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'account',
                    name: 'beta-account',
                    component: AccountView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'user-manage',
                    name: 'beta-user-manage',
                    component: UserManageView,
                    meta: {
                        requiresAuth: true,
                        requiredSystemIdentities: ['SYS_ADMIN'],
                    },
                },
            ],
        },
        {
            path: '/:pathMatch(.*)*',
            redirect: '/guide',
        },
    ],
})

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()
    authStore.init()

    const requiresAuth = to.meta?.requiresAuth !== false
    if (requiresAuth) {
        const isAuthorized = await authStore.ensureValidSession()
        if (!isAuthorized) {
            next({
                name: 'beta-login',
                query: { redirect: to.fullPath },
            })
            return
        }
    }

    if (Array.isArray(to.meta?.requiredSystemIdentities) && to.meta.requiredSystemIdentities.length > 0) {
        const currentIdentity = authStore.systemIdentity
        if (!to.meta.requiredSystemIdentities.includes(currentIdentity)) {
            next({ name: 'beta-dashboard' })
            return
        }
    }

    next()
})

export default router
