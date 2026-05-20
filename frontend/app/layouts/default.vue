<template>
  <div class="flex flex-col min-h-screen">
    <!-- Header -->
    <header class="sticky top-0 z-50 w-full border-b border-slate-200/80 bg-white/75 backdrop-blur-md transition-all duration-300">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div class="flex h-16 items-center justify-between">
          <!-- Logo & Brand -->
          <div class="flex items-center gap-8">
            <NuxtLink to="/" class="flex items-center gap-2 font-bold text-xl tracking-tight text-slate-900 hover:opacity-85 transition-opacity">
              <span class="bg-indigo-600 text-white w-8 h-8 rounded-lg flex items-center justify-center shadow-md shadow-indigo-200 font-extrabold text-lg">S</span>
              <span>Shop<span class="text-indigo-600 font-semibold">Sys</span></span>
            </NuxtLink>
            <!-- Desktop Navigation -->
            <nav class="hidden md:flex items-center gap-6">
              <NuxtLink to="/" class="text-sm font-medium text-slate-600 hover:text-indigo-600 transition-colors" active-class="text-indigo-600 font-semibold">
                探索商品
              </NuxtLink>
              <NuxtLink v-if="authStore.isAuthenticated" to="/orders" class="text-sm font-medium text-slate-600 hover:text-indigo-600 transition-colors" active-class="text-indigo-600 font-semibold">
                我的訂單
              </NuxtLink>
            </nav>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-4">
            <!-- Cart Icon (Authenticated only) -->
            <NuxtLink
              v-if="authStore.isAuthenticated"
              to="/cart"
              class="relative p-2 text-slate-600 hover:text-indigo-600 hover:bg-slate-50 rounded-full transition-all"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.8" stroke="currentColor" class="w-6 h-6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" />
              </svg>
              <span
                v-if="cartStore.totalCount > 0"
                class="absolute -top-1 -right-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-indigo-600 px-1 text-[10px] font-bold text-white shadow-sm ring-2 ring-white"
              >
                {{ cartStore.totalCount }}
              </span>
            </NuxtLink>

            <!-- User Menu -->
            <div class="hidden md:flex items-center gap-3">
              <template v-if="authStore.isAuthenticated">
                <el-dropdown trigger="click">
                  <span class="flex items-center gap-2 cursor-pointer outline-none">
                    <div class="w-8 h-8 rounded-full bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-700 font-bold text-sm select-none">
                      {{ authStore.user?.name ? authStore.user.name[0] : 'U' }}
                    </div>
                    <span class="text-sm font-medium text-slate-700 hover:text-slate-900">{{ authStore.user?.name }}</span>
                    <el-icon class="text-slate-400"><arrow-down /></el-icon>
                  </span>
                  <template #dropdown>
                    <el-dropdown-menu class="min-w-44">
                      <div class="px-4 py-2 border-b border-slate-50 text-xs text-slate-400">
                        角色: {{ authStore.role }}
                      </div>
                      <el-dropdown-item @click="navigateTo('/profile')">
                        <span class="flex items-center gap-2">個人設定</span>
                      </el-dropdown-item>
                      <!-- Admin features -->
                      <template v-if="authStore.isProductManager">
                        <el-dropdown-item divided @click="navigateTo('/admin/products')">
                          <span class="text-indigo-600 font-medium">商品管理後台</span>
                        </el-dropdown-item>
                        <el-dropdown-item @click="navigateTo('/admin/logs')">
                          <span class="text-indigo-600">庫存稽核日誌</span>
                        </el-dropdown-item>
                      </template>
                      <template v-if="authStore.isSuperAdmin">
                        <el-dropdown-item @click="navigateTo('/admin/users')">
                          <span class="text-rose-600">會員權限管理</span>
                        </el-dropdown-item>
                      </template>
                      <el-dropdown-item divided @click="authStore.logout()">
                        <span class="text-slate-500">登出帳戶</span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <template v-else>
                <NuxtLink to="/login" class="text-sm font-medium text-slate-600 hover:text-indigo-600 transition-colors">
                  登入
                </NuxtLink>
                <NuxtLink to="/register" class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-md shadow-indigo-100 hover:bg-indigo-700 transition-all hover:shadow-lg">
                  註冊
                </NuxtLink>
              </template>
            </div>

            <!-- Mobile Hamburger Menu Button -->
            <button
              class="md:hidden p-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
              @click="mobileMenuOpen = !mobileMenuOpen"
            >
              <svg v-if="!mobileMenuOpen" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Mobile Menu -->
      <transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="-translate-y-4 opacity-0"
        enter-to-class="translate-y-0 opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="translate-y-0 opacity-100"
        leave-to-class="-translate-y-4 opacity-0"
      >
        <div v-if="mobileMenuOpen" class="md:hidden border-t border-slate-100 bg-white px-4 py-4 space-y-3 shadow-lg">
          <NuxtLink
            to="/"
            class="block py-2 text-base font-medium text-slate-700 hover:text-indigo-600 hover:bg-slate-50 px-3 rounded-lg"
            @click="mobileMenuOpen = false"
          >
            探索商品
          </NuxtLink>
          <NuxtLink
            v-if="authStore.isAuthenticated"
            to="/orders"
            class="block py-2 text-base font-medium text-slate-700 hover:text-indigo-600 hover:bg-slate-50 px-3 rounded-lg"
            @click="mobileMenuOpen = false"
          >
            我的訂單
          </NuxtLink>
          
          <div class="border-t border-slate-100 pt-3">
            <template v-if="authStore.isAuthenticated">
              <div class="px-3 py-2 text-xs text-slate-400">
                Hi, {{ authStore.user?.name }} ({{ authStore.role }})
              </div>
              <NuxtLink
                to="/profile"
                class="block py-2 text-base font-medium text-slate-700 hover:text-indigo-600 hover:bg-slate-50 px-3 rounded-lg"
                @click="mobileMenuOpen = false"
              >
                個人設定
              </NuxtLink>
              <template v-if="authStore.isProductManager">
                <NuxtLink
                  to="/admin/products"
                  class="block py-2 text-base font-medium text-indigo-600 hover:bg-slate-50 px-3 rounded-lg font-semibold"
                  @click="mobileMenuOpen = false"
                >
                  商品管理後台
                </NuxtLink>
                <NuxtLink
                  to="/admin/logs"
                  class="block py-2 text-base font-medium text-indigo-600 hover:bg-slate-50 px-3 rounded-lg"
                  @click="mobileMenuOpen = false"
                >
                  庫存稽核日誌
                </NuxtLink>
              </template>
              <template v-if="authStore.isSuperAdmin">
                <NuxtLink
                  to="/admin/users"
                  class="block py-2 text-base font-medium text-rose-600 hover:bg-slate-50 px-3 rounded-lg"
                  @click="mobileMenuOpen = false"
                >
                  會員權限管理
                </NuxtLink>
              </template>
              <button
                class="w-full text-left block py-2 text-base font-medium text-slate-500 hover:text-slate-800 hover:bg-slate-50 px-3 rounded-lg mt-2"
                @click="authStore.logout(); mobileMenuOpen = false;"
              >
                登出帳戶
              </button>
            </template>
            <template v-else>
              <div class="grid grid-cols-2 gap-3 px-3 pt-2">
                <NuxtLink
                  to="/login"
                  class="flex justify-center items-center py-2 text-center text-sm font-semibold text-slate-700 border border-slate-200 rounded-lg hover:bg-slate-50"
                  @click="mobileMenuOpen = false"
                >
                  登入
                </NuxtLink>
                <NuxtLink
                  to="/register"
                  class="flex justify-center items-center py-2 text-center text-sm font-semibold text-white bg-indigo-600 rounded-lg hover:bg-indigo-700"
                  @click="mobileMenuOpen = false"
                >
                  註冊
                </NuxtLink>
              </div>
            </template>
          </div>
        </div>
      </transition>
    </header>

    <!-- Main Content -->
    <main class="flex-grow">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8">
        <slot />
      </div>
    </main>

    <!-- Footer -->
    <footer class="border-t border-slate-200/80 bg-white py-6">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 text-center text-xs text-slate-400">
        <p>&copy; 2026 ShopSys E-Commerce Platform. All rights reserved.</p>
        <p class="mt-1 font-mono text-[10px]">Made by ❤️</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '~/stores/auth'
import { useCartStore } from '~/stores/cart'

const authStore = useAuthStore()
const cartStore = useCartStore()
const mobileMenuOpen = ref(false)
</script>
