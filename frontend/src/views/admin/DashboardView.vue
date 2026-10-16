<template>
  <div class="dashboard">
    <!-- 统计卡 -->
    <div class="cards">
      <div v-for="c in cards" :key="c.label" class="stat-card">
        <span class="icon">{{ c.icon }}</span>
        <div class="num-box">
          <span class="num">{{ c.value.toLocaleString() }}</span>
          <span class="label">{{ c.label }}</span>
        </div>
      </div>
    </div>

    <!-- 图表 -->
    <div class="charts">
      <div class="chart-card">
        <div class="chart-head">
          <h3>近 7 天菜谱笔记发布趋势</h3>
          <span class="hint">以当天为基准动态生成，无发布的日期自动补 0</span>
        </div>
        <div ref="lineRef" class="chart" />
      </div>
      <div class="chart-card">
        <div class="chart-head">
          <h3>菜谱分类分布</h3>
          <span class="hint">各分类下的菜谱数量占比</span>
        </div>
        <div ref="pieRef" class="chart" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { adminDashboard } from '@/api/admin'

const cards = ref([
  { icon: '🍳', label: '菜谱笔记', value: 0 },
  { icon: '💬', label: '评论', value: 0 },
  { icon: '📍', label: '探店信息', value: 0 },
  { icon: '📅', label: '探店预约', value: 0 },
  { icon: '👥', label: '注册用户', value: 0 }
])

const lineRef = ref(null)
const pieRef = ref(null)
let lineChart = null
let pieChart = null

const ACCENT = '#e8590c'
const PALETTE = ['#e8590c', '#d97706', '#b45309', '#f28b3b', '#9a3412', '#f59e0b', '#ea580c', '#a16207']

function renderLine(weekly) {
  lineChart = echarts.init(lineRef.value)
  lineChart.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: weekly.map((d) => d.date), axisLine: { lineStyle: { color: '#bbb' } } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { type: 'dashed', color: '#eee' } } },
    series: [
      {
        name: '发布数量',
        type: 'line',
        smooth: true,
        data: weekly.map((d) => d.count),
        itemStyle: { color: ACCENT },
        lineStyle: { color: ACCENT, width: 3 },
        areaStyle: { opacity: 0.08, color: ACCENT },
        symbolSize: 7
      }
    ]
  })
}

function renderPie(categories) {
  pieChart = echarts.init(pieRef.value)
  const data = categories.filter((c) => c.count > 0)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}：{c} 道（{d}%）' },
    legend: { bottom: 0, icon: 'circle', textStyle: { fontSize: 12 } },
    color: PALETTE,
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b} {c}' },
        data
      }
    ]
  })
}

function handleResize() {
  lineChart && lineChart.resize()
  pieChart && pieChart.resize()
}

onMounted(async () => {
  const res = await adminDashboard()
  const d = res.data
  const labels = ['菜谱笔记', '评论', '探店信息', '探店预约', '注册用户']
  const values = [d.recipeCount, d.commentCount, d.storeVisitCount, d.reservationCount, d.userCount]
  cards.value = cards.value.map((c, i) => ({ ...c, label: labels[i], value: Number(values[i]) || 0 }))

  renderLine(d.weekly)
  renderPie(d.categories)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  lineChart && lineChart.dispose()
  pieChart && pieChart.dispose()
})
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--fc-sp-3-5);
  margin-bottom: var(--fc-sp-4);
}

.stat-card {
  background: #fff;
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-md);
  padding: 18px var(--fc-sp-4);
  display: flex;
  align-items: center;
  gap: var(--fc-sp-3-5);
}

.icon {
  font-size: var(--fc-fs-2xl);
}

.num-box {
  display: flex;
  flex-direction: column;
}

.num {
  font-family: var(--fc-font-display);
  font-size: var(--fc-fs-2xl);
  font-weight: 700;
  color: var(--fc-ink);
  line-height: 1.1;
}

.label {
  font-size: var(--fc-fs-sm);
  color: var(--fc-neutral);
}

.charts {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: var(--fc-sp-3-5);
}

.chart-card {
  background: #fff;
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-md);
  padding: var(--fc-sp-4);
}

.chart-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--fc-sp-2);
}

.chart-head h3 {
  margin: 0;
  font-size: var(--fc-fs-lg);
}

.hint {
  font-size: var(--fc-fs-xs);
  color: var(--fc-neutral);
}

.chart {
  width: 100%;
  height: 320px;
}

@media (max-width: 1000px) {
  .cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
