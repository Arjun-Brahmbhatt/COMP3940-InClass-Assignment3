import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UploadClient {

   public UploadClient() { }

   public String uploadFile() {

      String listing = "";

      try {
         String boundary = "----JavaClientBoundary";
         String caption = "kaiser";
         String date = "2026-10-04";
         File file = new File("kaiser.txt");

         Socket socket = new Socket("localhost", 8082);

         OutputStream out = socket.getOutputStream();
         BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream())
         );

         ByteArrayOutputStream body =
            new ByteArrayOutputStream();

         writeTextPart(
            body, boundary, "caption", caption
         );

         writeTextPart(
            body, boundary, "date", date
         );

         body.write(
            ("--" + boundary + "\r\n")
               .getBytes(StandardCharsets.UTF_8)
         );

         body.write(
            ("Content-Disposition: form-data; " +
             "name=\"fileName\"; filename=\"" +
             file.getName() + "\"\r\n")
               .getBytes(StandardCharsets.UTF_8)
         );

         body.write(
            "Content-Type: application/octet-stream\r\n\r\n"
               .getBytes(StandardCharsets.UTF_8)
         );

         FileInputStream fis =
            new FileInputStream(file);

         byte[] buffer = new byte[4096];
         int bytesRead;

         while ((bytesRead = fis.read(buffer)) != -1) {
            body.write(buffer, 0, bytesRead);
         }

         fis.close();

         body.write(
            "\r\n".getBytes(StandardCharsets.UTF_8)
         );

         body.write(
            ("--" + boundary + "--\r\n")
               .getBytes(StandardCharsets.UTF_8)
         );

         byte[] bodyBytes = body.toByteArray();

         String headers =
            "POST / HTTP/1.1\r\n" +
            "Host: localhost:8082\r\n" +
            "Content-Type: multipart/form-data; boundary=" +
            boundary + "\r\n" +
            "Content-Length: " + bodyBytes.length + "\r\n" +
            "Connection: close\r\n" +
            "\r\n";

         out.write(
            headers.getBytes(StandardCharsets.UTF_8)
         );

         out.write(bodyBytes);
         out.flush();

         String line;

         while ((line = in.readLine()) != null) {
            listing += line + "\n";
         }

         socket.close();

      } catch (Exception e) {
         System.err.println(e);
      }

      return listing;
   }

   private void writeTextPart(
         ByteArrayOutputStream body,
         String boundary,
         String name,
         String value) throws IOException {

      body.write(
         ("--" + boundary + "\r\n")
            .getBytes(StandardCharsets.UTF_8)
      );

      body.write(
         ("Content-Disposition: form-data; name=\"" +
          name + "\"\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8)
      );

      body.write(
         value.getBytes(StandardCharsets.UTF_8)
      );

      body.write(
         "\r\n".getBytes(StandardCharsets.UTF_8)
      );
   }
}