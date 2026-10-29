import { describe, expect, it } from 'vitest';
import { api } from './api';

describe('api client', () => {
  it('uses the local backend endpoint by default', () => {
    expect(api.defaults.baseURL).toBe('http://localhost:8081/api');
    expect(api.defaults.timeout).toBe(60000);
  });
});
