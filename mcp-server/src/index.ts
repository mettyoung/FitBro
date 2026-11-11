import { PUBLIC_ORIGIN, createWorker } from "./worker";

// Only the default export: workerd treats every named export of the entry module as an entrypoint.
export default createWorker(PUBLIC_ORIGIN);
