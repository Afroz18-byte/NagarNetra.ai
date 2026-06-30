import * as fs from 'fs';

/**
 * Standalone Gemini Helper for TypeScript Agent Environment.
 * Uses 'gemini-1.5-flash' specifically for municipal issue analysis, reporting, and details categorization.
 */

const GEMINI_API_KEY = process.env.GEMINI_API_KEY || process.env.FIREBASE_API_KEY || "";

/**
 * Safely converts a local file path to base64 with a retry mechanism.
 * Useful if the file is still being written/saved by an upstream agent/service.
 */
export async function encodeFileToBase64WithRetry(
  filePath: string,
  retries: number = 3,
  delayMs: number = 1000
): Promise<string> {
  let lastError: any = null;
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      if (fs.existsSync(filePath)) {
        const fileBuffer = fs.readFileSync(filePath);
        return fileBuffer.toString('base64');
      } else {
        throw new Error(`File does not exist at path: ${filePath}`);
      }
    } catch (error) {
      lastError = error;
      console.warn(`[Gemini Helper] Attempt ${attempt}/${retries} to encode file failed. Retrying in ${delayMs}ms...`);
      if (attempt < retries) {
        await new Promise((resolve) => setTimeout(resolve, delayMs));
      }
    }
  }
  throw new Error(`Failed to encode file to base64 after ${retries} attempts. Original error: ${lastError?.message}`);
}

/**
 * Safely encodes a Buffer/Uint8Array to base64 with a retry mechanism.
 */
export function encodeBufferToBase64WithRetry(
  buffer: Buffer | Uint8Array,
  retries: number = 3
): string {
  let lastError: any = null;
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const buf = Buffer.isBuffer(buffer) ? buffer : Buffer.from(buffer);
      const base64 = buf.toString('base64');
      if (!base64 || base64.trim() === "") {
        throw new Error("Encoded string is empty or invalid.");
      }
      return base64;
    } catch (error) {
      lastError = error;
      console.warn(`[Gemini Helper] Buffer base64 encoding attempt ${attempt}/${retries} failed.`);
    }
  }
  throw new Error(`Buffer base64 encoding failed after ${retries} attempts: ${lastError?.message}`);
}

/**
 * Analyzes an image (as base64 string) with the gemini-1.5-flash model.
 * Includes a robust fetch retry mechanism.
 */
export async function analyzeImageWithGemini(
  base64Data: string,
  prompt: string,
  mimeType: string = 'image/jpeg',
  maxRetries: number = 3,
  initialDelayMs: number = 1000
): Promise<string> {
  if (!GEMINI_API_KEY) {
    console.warn("[Gemini Helper] Warning: GEMINI_API_KEY is empty. The request might fail.");
  }

  // Ensure base64 string is cleaned of any data URI schemes (e.g. "data:image/jpeg;base64,")
  let cleanBase64 = base64Data;
  if (base64Data.includes(";base64,")) {
    cleanBase64 = base64Data.split(";base64,").pop() || base64Data;
  }

  // Construct standard direct REST payload
  const requestBody = {
    contents: [
      {
        parts: [
          { text: prompt },
          {
            inlineData: {
              mimeType: mimeType,
              data: cleanBase64
            }
          }
        ]
      }
    ]
  };

  const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`;

  let lastError: any = null;
  let delay = initialDelayMs;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`API returned status ${response.status}: ${errorText}`);
      }

      const jsonResponse: any = await response.json();
      
      const candidateText = jsonResponse?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (candidateText) {
        return candidateText;
      } else {
        throw new Error("No response text returned in the candidates array.");
      }
    } catch (error) {
      lastError = error;
      console.error(`[Gemini Helper] Attempt ${attempt}/${maxRetries} to call Gemini API failed:`, error);
      if (attempt < maxRetries) {
        console.log(`[Gemini Helper] Retrying API request in ${delay}ms...`);
        await new Promise((resolve) => setTimeout(resolve, delay));
        delay *= 2; // Exponential backoff
      }
    }
  }

  throw new Error(`Gemini image analysis failed after ${maxRetries} attempts. Last error: ${lastError?.message}`);
}

/**
 * Analyzes standard text prompt with the gemini-1.5-flash model.
 */
export async function analyzeTextWithGemini(
  prompt: string,
  maxRetries: number = 3,
  initialDelayMs: number = 1000
): Promise<string> {
  const requestBody = {
    contents: [
      {
        parts: [
          { text: prompt }
        ]
      }
    ]
  };

  const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`;

  let lastError: any = null;
  let delay = initialDelayMs;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`API returned status ${response.status}: ${errorText}`);
      }

      const jsonResponse: any = await response.json();
      const candidateText = jsonResponse?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (candidateText) {
        return candidateText;
      } else {
        throw new Error("No response text returned in the candidates array.");
      }
    } catch (error) {
      lastError = error;
      console.error(`[Gemini Helper] Text analysis attempt ${attempt}/${maxRetries} failed:`, error);
      if (attempt < maxRetries) {
        await new Promise((resolve) => setTimeout(resolve, delay));
        delay *= 2;
      }
    }
  }

  throw new Error(`Gemini text analysis failed after ${maxRetries} attempts. Last error: ${lastError?.message}`);
}
