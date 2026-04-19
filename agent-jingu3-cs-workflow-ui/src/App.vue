<template>
  <el-config-provider :locale="zhCn">
    <div class="layout">
      <header class="top">
        <strong>JinGu3 BPMN</strong>
        <nav>
          <router-link to="/">使用 / 试运行</router-link>
          <router-link to="/design">设计 / 部署</router-link>
        </nav>
      </header>
      <main class="main">
        <router-view />
      </main>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<style>
html,
body,
#app {
  margin: 0;
  height: 100%;
  font-family:
    system-ui,
    -apple-system,
    'Segoe UI',
    Roboto,
    'Helvetica Neue',
    Arial,
    sans-serif;
}
.layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}
.top {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}
.top nav {
  display: flex;
  gap: 1rem;
}
.top a {
  color: #409eff;
  text-decoration: none;
}
.top a.router-link-active {
  font-weight: 600;
}
.main {
  flex: 1;
  padding: 1rem 1.25rem 2rem;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}
</style>
