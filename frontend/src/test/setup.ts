import { config } from '@vue/test-utils'

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
