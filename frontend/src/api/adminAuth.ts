import { client } from './axios'
import type { AdminLoginForm, AuthResponse } from '@/types/auth'

export async function adminLogin(data: AdminLoginForm): Promise<AuthResponse> {
  const response = await client.post('/admin/auth/login', data)
  return response.data
}

export async function registerAdmin(data: { username: string; password: string; realName: string }): Promise<void> {
  await client.post('/admin/auth/register', data)
}
