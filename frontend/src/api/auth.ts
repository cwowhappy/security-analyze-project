import { client } from './axios'
import type { LoginForm, RegisterForm, AuthResponse, UserProfile } from '@/types/auth'

export async function login(data: LoginForm): Promise<AuthResponse> {
  const response = await client.post('/auth/login', data)
  return response.data
}

export async function register(data: RegisterForm): Promise<void> {
  await client.post('/auth/register', data)
}

export async function getCurrentUser(): Promise<UserProfile> {
  const response = await client.get('/auth/me')
  return response.data
}
