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
         System.out.println("Thread running");

         BufferedInputStream in =
            new BufferedInputStream(socket.getInputStream());

         String requestLine = readLine(in);

         if (requestLine == null) {
            socket.close();
            return;
         }

         String[] requestParts = requestLine.split(" ");

         String method = requestParts[0];
         String uri = requestParts[1];

         HttpServletRequest req =
            new HttpServletRequest(in);

         ByteArrayOutputStream baos =
            new ByteArrayOutputStream();

         HttpServletResponse res =
            new HttpServletResponse(baos);

         // Component based architecture using Reflection
         HttpServlet httpServlet =
            ComponentLoader.loadServlet("UploadServletSingleton");

         if ("GET".equalsIgnoreCase(method) &&
             "/".equals(uri)) {

            System.out.println(
               "Calling UploadServlet's doGet"
            );

            httpServlet.doGet(req, res);

         } else if ("POST".equalsIgnoreCase(method)) {

            System.out.println(
               "Calling UploadServlet's doPost"
            );

            httpServlet.doPost(req, res);

         } else {

            res.getOutputStream().write(
               "HTTP/1.1 405 Method Not Allowed\r\n\r\n"
                  .getBytes()
            );
         }

         OutputStream out =
            socket.getOutputStream();

         out.write(baos.toByteArray());
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