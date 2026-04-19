import axios, { type AxiosRequestConfig } from 'axios'

export interface ApiResult<T> {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: number
}

const base = import.meta.env.VITE_API_BASE ?? ''

const raw = axios.create({
  baseURL: base,
  timeout: 120000,
})

raw.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body.success === 'boolean') {
      if (!body.success) {
        return Promise.reject(new Error(body.message || body.code || 'request failed'))
      }
      return body.data
    }
    return response.data
  },
  (err) => Promise.reject(err),
)

/** 响应已解包为 {@link ApiResult#data}，类型由调用方指定。 */
export const api = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return raw.get(url, config) as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return raw.post(url, data, config) as Promise<T>
  },
}
