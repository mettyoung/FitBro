/** In-memory KVNamespace with TTLs — enough for the OAuth provider and the sign-in lockout. */
export function createTestKv(now: () => number = Date.now): KVNamespace {
  const store = new Map<string, { value: string; expires?: number; metadata?: unknown }>();
  const live = (key: string) => {
    const item = store.get(key);
    if (item?.expires !== undefined && item.expires <= now()) {
      store.delete(key);
      return undefined;
    }
    return item;
  };
  const kv = {
    async get(key: string, options?: unknown) {
      const item = live(key);
      if (!item) return null;
      const type = typeof options === "string" ? options : (options as { type?: string } | undefined)?.type;
      return type === "json" ? JSON.parse(item.value) : item.value;
    },
    async getWithMetadata(key: string, options?: unknown) {
      const item = live(key);
      return { value: item ? await kv.get(key, options) : null, metadata: item?.metadata ?? null };
    },
    async put(key: string, value: string, options?: { expirationTtl?: number; expiration?: number; metadata?: unknown }) {
      const expires = options?.expirationTtl
        ? now() + options.expirationTtl * 1000
        : options?.expiration
          ? options.expiration * 1000
          : undefined;
      store.set(key, { value: String(value), expires, metadata: options?.metadata });
    },
    async delete(key: string) {
      store.delete(key);
    },
    async list(options?: { prefix?: string; limit?: number; cursor?: string }) {
      const keys = [...store.keys()]
        .filter((k) => live(k) && k.startsWith(options?.prefix ?? ""))
        .sort()
        .map((name) => ({ name, metadata: store.get(name)?.metadata }));
      return { keys, list_complete: true, cacheStatus: null };
    },
  };
  return kv as unknown as KVNamespace;
}
