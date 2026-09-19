export type ApiErrorPayload = {
  error?: {
    code?: string;
    message?: string;
    details?: string[];
  };
};

export class ApiRequestError extends Error {
  readonly code?: string;

  constructor(message: string, code?: string) {
    super(message);
    this.name = "ApiRequestError";
    this.code = code;
  }
}
