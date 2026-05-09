import { config } from '@vue/test-utils'
import { vi } from 'vitest'

// Mock ResizeObserver for vue-echarts
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}
;(global as any).ResizeObserver = ResizeObserverMock

// Mock echarts modules to avoid Canvas initialization in tests
vi.mock('echarts/core', () => ({
  use: vi.fn(),
  __esModule: true,
  default: {},
}))
vi.mock('echarts/renderers', () => ({
  CanvasRenderer: vi.fn(),
  SVGRenderer: vi.fn(),
}))
vi.mock('echarts/charts', () => ({
  LineChart: vi.fn(),
  BarChart: vi.fn(),
  PieChart: vi.fn(),
}))
vi.mock('echarts/components', () => ({
  GridComponent: vi.fn(),
  TooltipComponent: vi.fn(),
  LegendComponent: vi.fn(),
  TitleComponent: vi.fn(),
  ToolboxComponent: vi.fn(),
  DataZoomComponent: vi.fn(),
}))
vi.mock('vue-echarts', () => ({
  __esModule: true,
  default: {
    name: 'VChart',
    props: ['option', 'autoresize'],
    template: '<div class="mock-v-chart" />',
  },
}))

// Mock external complex components; let Element Plus render normally
config.global.stubs = {
  'v-chart': true,
}

// Mock vue-router
config.global.mocks = {
  $route: {
    params: {},
    path: '/',
  },
  $router: {
    push: () => Promise.resolve(),
  },
}
