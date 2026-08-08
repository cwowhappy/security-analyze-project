<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">Security Analyze</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/events">
          <el-icon><Warning /></el-icon>
          <span>安全事件</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">安全分析平台</el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #001529;
  color: #fff;
}
.logo {
  height: 64px;
  line-height: 64px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  line-height: 60px;
  font-weight: bold;
}
</style>
