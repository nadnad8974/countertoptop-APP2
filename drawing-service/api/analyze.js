import { analyzeDrawingWithOpenAI, OpenAIServiceError } from "../lib/openai.js";
import { isAuthorizedDrawingDevice } from "../lib/device-auth.js";
import {
  DrawingRequestError,
  validateContentLength,
  validateDrawingRequest
} from "../lib/request.js";

export function createAnalyzeHandler(options = {}) {
  const analyze = options.analyze || analyzeDrawingWithOpenAI;
  const environment = options.environment || process.env;

  return async function handler(request, response) {
    setResponseHeaders(response);

    if (request.method !== "POST") {
      response.setHeader("Allow", "POST");
      return response.status(405).json({ error: "Method not allowed." });
    }
    if (!isAuthorizedDrawingDevice({
      deviceId: header(request, "x-ramsiers-device"),
      authorization: header(request, "authorization")
    }, environment)) {
      return response.status(401).json({ error: "Unauthorized." });
    }
    const contentType = String(header(request, "content-type") || "").toLowerCase();
    if (!contentType.startsWith("application/json")) {
      return response.status(415).json({ error: "JSON is required." });
    }

    try {
      validateContentLength(header(request, "content-length"));
      const drawingRequest = validateDrawingRequest(request.body);
      const result = await analyze(drawingRequest, environment);
      return response.status(200).json(result);
    } catch (error) {
      if (error instanceof DrawingRequestError) {
        return response.status(error.statusCode).json({ error: error.message });
      }
      if (error instanceof OpenAIServiceError) {
        if (error.kind === "timeout") {
          return response.status(504).json({
            error: "Drawing AI took too long. Please try the drawing again."
          });
        }
        if (error.kind === "configuration") {
          return response.status(503).json({
            error: "Drawing AI is not configured."
          });
        }
        return response.status(502).json({
          error: "The drawing could not be analyzed safely. Please try again."
        });
      }
      return response.status(503).json({
        error: "The drawing could not be analyzed. Please try again."
      });
    }
  };
}

function setResponseHeaders(response) {
  response.setHeader("Cache-Control", "no-store");
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.setHeader("X-Content-Type-Options", "nosniff");
  response.setHeader("Referrer-Policy", "no-referrer");
}

function header(request, name) {
  if (!request?.headers) return undefined;
  const lowerName = name.toLowerCase();
  for (const [key, value] of Object.entries(request.headers)) {
    if (key.toLowerCase() === lowerName) return value;
  }
  return undefined;
}

export default createAnalyzeHandler();
