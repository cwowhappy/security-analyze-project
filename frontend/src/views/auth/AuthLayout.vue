<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'

const isDark = ref(false)

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  try {
    localStorage.setItem('sai-theme', isDark.value ? 'dark' : 'light')
  } catch (e) {
    /* ignore */
  }
}

onMounted(() => {
  try {
    const saved = localStorage.getItem('sai-theme')
    if (saved === 'dark') {
      isDark.value = true
      document.documentElement.setAttribute('data-theme', 'dark')
    }
  } catch (e) {
    /* ignore */
  }
})
</script>

<template>
  <div class="auth-container">
    <button class="theme-toggle auth-theme" @click="toggleTheme" title="切换主题">
      {{ isDark ? '🌙' : '☀' }}
    </button>
    <div class="auth-brand">
      <div class="auth-logo">SAI</div>
      <div class="auth-brand-t">证券分析与投资</div>
      <div class="auth-brand-s">
        Security Analyze & Invest<br />专业的证券投资分析平台
      </div>
    </div>
    <div class="auth-form-wrap">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

.auth-brand {
  flex: 1;
  background: linear-gradient(135deg, var(--primary) 0%, #8b7fff 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.auth-brand::before {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.07);
  top: -100px;
  right: -100px;
}

.auth-brand::after {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  bottom: -80px;
  left: -60px;
}

.auth-logo {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 28px;
  position: relative;
  z-index: 1;
}

.auth-brand-t {
  font-size: 32px;
  font-weight: 800;
  margin-bottom: 10px;
  position: relative;
  z-index: 1;
  text-align: center;
}

.auth-brand-s {
  font-size: 14px;
  opacity: 0.8;
  position: relative;
  z-index: 1;
  text-align: center;
  line-height: 1.6;
}

.auth-form-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--bg);
  transition: background 0.25s;
  min-width: 340px;
}

.auth-theme {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all 0.15s;
  font-size: 16px;
  flex-shrink: 0;
}

.auth-theme:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .auth-brand {
    display: none;
  }
  .auth-form-wrap {
    min-width: auto;
    padding: 24px;
  }
  .auth-container {
    min-height: 100vh;
  }
}
</style>
