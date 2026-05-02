export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  realName: string
}

export interface AdminLoginForm {
  username: string
  password: string
}

export interface UserProfile {
  id: number
  username: string
  realName: string
  status: string
  role: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  tokenType: string
}
