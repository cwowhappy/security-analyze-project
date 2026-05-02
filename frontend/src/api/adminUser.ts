import { client } from './axios'

export interface UserListItem {
  id: number
  username: string
  realName: string
  role: string
  status: string
  createdAt: string
}

export async function getUserList(): Promise<UserListItem[]> {
  const response = await client.get('/admin/users')
  return response.data
}

export async function approveUser(id: number): Promise<void> {
  await client.put(`/admin/users/${id}/approve`)
}

export async function disableUser(id: number): Promise<void> {
  await client.put(`/admin/users/${id}/disable`)
}

export async function enableUser(id: number): Promise<void> {
  await client.put(`/admin/users/${id}/enable`)
}
