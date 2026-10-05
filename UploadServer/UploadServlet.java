import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;

public class UploadServlet extends HttpServlet {

   private static final Semaphore uploadSemaphore = new Semaphore(3);
   private final ImageDAO imageDAO = new ImageDAO();
   private String contentType;

   public void setContentType(String contentType) {
      this.contentType = contentType;
   }

   protected void doGet(HttpServletRequest request,
                        HttpServletResponse response) {

      log("GET request started");

      try {
         PrintWriter out =
            new PrintWriter(response.getOutputStream(), true);

         String html =
            "<!DOCTYPE html>" +
            "<html>" +
            "<body>" +
            "<form method='POST' action='/' enctype='multipart/form-data'>" +
            "<label>Caption:</label><br>" +
            "<input type='text' name='caption'><br><br>" +
            "<label>Date:</label><br>" +
            "<input type='date' name='date'><br><br>" +
            "<label>File:</label><br>" +
            "<input type='file' name='fileName'><br><br>" +
            "<input type='submit' value='Upload'>" +
            "</form>" +
            "</body>" +
            "</html>";

         out.print("HTTP/1.1 200 OK\r\n");
         out.print("Content-Type: text/html\r\n");
         out.print("Connection: close\r\n");
         out.print("\r\n");
         out.print(html);
         out.flush();

      } catch (Exception ex) {
         System.err.println(ex);

      } finally {
         log("GET request finished");
      }
   }

   protected void doPost(HttpServletRequest request,
                         HttpServletResponse response) {

      log("POST request started");

      boolean acquired = false;

      try {
         uploadSemaphore.acquire();
         acquired = true;

         String boundary = getBoundary();

         if (boundary == null) {
            throw new UploadException(
               "Multipart boundary not found."
            );
         }

         byte[] body =
            readAllBytes(request.getInputStream());

         String caption = "";
         String date = "";
         String fileName = "";
         byte[] fileData = null;

         byte[] boundaryBytes =
            ("--" + boundary).getBytes(
               StandardCharsets.ISO_8859_1
            );

         List<byte[]> parts =
            split(body, boundaryBytes);

         for (byte[] part : parts) {

            int headerEnd = indexOf(
               part,
               "\r\n\r\n".getBytes(
                  StandardCharsets.ISO_8859_1
               ),
               0
            );

            if (headerEnd == -1) {
               continue;
            }

            String headers = new String(
               part,
               0,
               headerEnd,
               StandardCharsets.ISO_8859_1
            );

            int dataStart = headerEnd + 4;
            int dataEnd = part.length;

            if (dataEnd >= 2 &&
                part[dataEnd - 2] == '\r' &&
                part[dataEnd - 1] == '\n') {

               dataEnd -= 2;
            }

            byte[] data =
               Arrays.copyOfRange(
                  part,
                  dataStart,
                  dataEnd
               );

            if (headers.contains(
                  "name=\"caption\"")) {

               caption = new String(
                  data,
                  StandardCharsets.UTF_8
               );

            } else if (headers.contains(
                  "name=\"date\"")) {

               date = new String(
                  data,
                  StandardCharsets.UTF_8
               );

            } else if (headers.contains(
                  "name=\"fileName\"")) {

               fileName = getFileName(headers);
               fileData = data;
            }
         }

         if (fileName == null ||
             fileName.equals("") ||
             fileData == null) {

            throw new UploadException(
               "No file was uploaded."
            );
         }

         String savedName =
            caption + "_" + date + "_" + fileName;

         imageDAO.save(savedName, fileData);

         sendDirectoryListing(response);

      } catch (Exception ex) {

         System.err.println(ex);

      } finally {

         if (acquired) {
            uploadSemaphore.release();
         }

         log("POST request finished");
      }
   }

   private String getBoundary() {

      if (contentType == null) {
         return null;
      }

      int index =
         contentType.indexOf("boundary=");

      if (index == -1) {
         return null;
      }

      String boundary =
         contentType.substring(index + 9).trim();

      if (boundary.startsWith("\"") &&
          boundary.endsWith("\"")) {

         boundary =
            boundary.substring(
               1,
               boundary.length() - 1
            );
      }

      return boundary;
   }

   private byte[] readAllBytes(InputStream in)
         throws IOException {

      ByteArrayOutputStream baos =
         new ByteArrayOutputStream();

      byte[] buffer = new byte[4096];
      int bytesRead;

      while ((bytesRead = in.read(buffer)) != -1) {

         baos.write(
            buffer,
            0,
            bytesRead
         );
      }

      return baos.toByteArray();
   }

   private List<byte[]> split(
         byte[] data,
         byte[] boundary) {

      List<byte[]> parts =
         new ArrayList<>();

      int start = 0;

      while (true) {

         int boundaryStart =
            indexOf(data, boundary, start);

         if (boundaryStart == -1) {
            break;
         }

         int partStart =
            boundaryStart + boundary.length;

         if (partStart + 1 < data.length &&
             data[partStart] == '-' &&
             data[partStart + 1] == '-') {

            break;
         }

         if (partStart + 1 < data.length &&
             data[partStart] == '\r' &&
             data[partStart + 1] == '\n') {

            partStart += 2;
         }

         int nextBoundary =
            indexOf(
               data,
               boundary,
               partStart
            );

         if (nextBoundary == -1) {
            break;
         }

         parts.add(
            Arrays.copyOfRange(
               data,
               partStart,
               nextBoundary
            )
         );

         start = nextBoundary;
      }

      return parts;
   }

   private int indexOf(
         byte[] data,
         byte[] pattern,
         int start) {

      for (int i = start;
           i <= data.length - pattern.length;
           i++) {

         boolean found = true;

         for (int j = 0;
              j < pattern.length;
              j++) {

            if (data[i + j] != pattern[j]) {

               found = false;
               break;
            }
         }

         if (found) {
            return i;
         }
      }

      return -1;
   }

   private String getFileName(
         String headers) {

      String marker = "filename=\"";

      int start =
         headers.indexOf(marker);

      if (start == -1) {
         return "";
      }

      start += marker.length();

      int end =
         headers.indexOf("\"", start);

      if (end == -1) {
         return "";
      }

      String fileName =
         headers.substring(start, end);

      fileName =
         new File(fileName).getName();

      return fileName;
   }

   private void sendDirectoryListing(
         HttpServletResponse response)
         throws IOException {

      String[] files =
         imageDAO.getFiles();

      StringBuilder html =
         new StringBuilder();

      html.append("<!DOCTYPE html>");
      html.append("<html>");
      html.append("<body>");
      html.append("<h2>Images</h2>");
      html.append("<ul>");

      Arrays.stream(files).forEach(file -> {
         html.append("<li>");
         html.append(file);
         html.append("</li>");
      });

      html.append("</ul>");
      html.append("</body>");
      html.append("</html>");

      PrintWriter out =
         new PrintWriter(
            response.getOutputStream(),
            true
         );

      out.print("HTTP/1.1 200 OK\r\n");
      out.print("Content-Type: text/html\r\n");
      out.print("Connection: close\r\n");
      out.print("\r\n");
      out.print(html.toString());
      out.flush();
   }

   private void log(String message) {
      System.out.println(
         "[UploadServer] " + message
      );
   }
}