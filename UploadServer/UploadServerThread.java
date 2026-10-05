import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;

public class UploadServerThread extends Thread {

   private Socket socket = null;
   private Semaphore connectionSemaphore;

   public UploadServerThread(Socket socket, Semaphore connectionSemaphore) {
      super("DirServerThread");
      this.socket = socket;
      this.connectionSemaphore = connectionSemaphore;
   }

   public void run() {

      try {
         BufferedInputStream in =
            new BufferedInputStream(socket.getInputStream());

         String firstLine = readLine(in);

         if (firstLine == null) {
            socket.close();
            return;
         }

         int contentLength = 0;
         String contentType = "";

         String headerLine;

         while ((headerLine = readLine(in)) != null &&
                !headerLine.equals("")) {

            if (headerLine.toLowerCase().startsWith("content-length:")) {
               contentLength = Integer.parseInt(
                  headerLine.substring(headerLine.indexOf(":") + 1).trim()
               );
            }

            if (headerLine.toLowerCase().startsWith("content-type:")) {
               contentType =
                  headerLine.substring(headerLine.indexOf(":") + 1).trim();
            }
         }

         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         HttpServletResponse res = new HttpServletResponse(baos);
         HttpServlet httpServlet =
            ComponentLoader.loadServlet("UploadServlet");

         String[] requestLine = firstLine.split(" ");

         if (requestLine[0].equals("GET") &&
             requestLine[1].equals("/")) {

            HttpServletRequest req =
               new HttpServletRequest(in);

            httpServlet.doGet(req, res);

         } else if (requestLine[0].equals("POST") &&
                    requestLine[1].equals("/")) {

            byte[] body = new byte[contentLength];

            int totalRead = 0;

            while (totalRead < contentLength) {

               int bytesRead =
                  in.read(body, totalRead, contentLength - totalRead);

               if (bytesRead == -1) {
                  break;
               }

               totalRead += bytesRead;
            }

            HttpServletRequest req =
               new HttpServletRequest(
                  new ByteArrayInputStream(body)
               );

            ((UploadServlet) httpServlet).setContentType(contentType);

            httpServlet.doPost(req, res);
         }

         OutputStream out = socket.getOutputStream();

         out.write(
            ((ByteArrayOutputStream) baos).toByteArray()
         );

         out.flush();
         socket.close();

      } catch (Exception e) {
         e.printStackTrace();

      } finally {
         connectionSemaphore.release();
      }
   }

   private String readLine(BufferedInputStream in)
         throws IOException {

      ByteArrayOutputStream line =
         new ByteArrayOutputStream();

      int current;

      while ((current = in.read()) != -1) {

         if (current == '\n') {
            break;
         }

         if (current != '\r') {
            line.write(current);
         }
      }

      if (current == -1 && line.size() == 0) {
         return null;
      }

      return line.toString();
   }
}