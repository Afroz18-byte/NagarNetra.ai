import emailjs from '@emailjs/browser';

/**
 * Interface representing Email Parameters
 */
export interface EmailParams {
  to_email: string;
  to_name: string;
  subject: string;
  message_html: string;
}

/**
 * Service to handle dispatching automated emails using EmailJS
 */
export class EmailService {
  private static serviceId = process.env.VITE_EMAILJS_SERVICE_ID || "your-emailjs-service-id";
  private static templateId = process.env.VITE_EMAILJS_TEMPLATE_ID || "your-emailjs-template-id";
  private static publicKey = process.env.VITE_EMAILJS_PUBLIC_KEY || "your-emailjs-public-key";

  private static isConfigured(): boolean {
    return (
      this.serviceId !== "your-emailjs-service-id" &&
      this.templateId !== "your-emailjs-template-id" &&
      this.publicKey !== "your-emailjs-public-key" &&
      this.serviceId.trim() !== "" &&
      this.templateId.trim() !== "" &&
      this.publicKey.trim() !== ""
    );
  }

  /**
   * Dispatches an accountability email to a specified ward authority
   */
  public static async sendAccountabilityEmail(params: EmailParams): Promise<boolean> {
    if (!this.isConfigured()) {
      console.log(`🤖 [EmailJS Simulator Active] Simulated email successfully sent to ${params.to_email} (${params.to_name})`);
      console.log(`Subject: ${params.subject}`);
      console.log(`Content Preview: ${params.message_html.substring(0, 150)}...`);
      return true;
    }

    try {
      console.log(`→ Initiating EmailJS web dispatch to ${params.to_email}...`);
      const response = await emailjs.send(
        this.serviceId,
        this.templateId,
        {
          to_email: params.to_email,
          to_name: params.to_name,
          subject: params.subject,
          message_html: params.message_html
        },
        this.publicKey
      );
      console.log(`← EmailJS web dispatch response:`, response);
      return response.status === 200;
    } catch (error) {
      console.error(`← EmailJS web dispatch failed:`, error);
      return false;
    }
  }
}
