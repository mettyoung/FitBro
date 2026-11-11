// Node stand-in for the `cloudflare:workers` module the OAuth provider imports.
export class WorkerEntrypoint<Env = unknown> {
  constructor(
    public ctx: ExecutionContext,
    public env: Env,
  ) {}
}
