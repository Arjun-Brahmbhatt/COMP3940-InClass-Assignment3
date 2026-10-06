import java.io.*;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class UploadServlet extends HttpServlet {

   private static final Semaphore uploadSemaphore =
      new Semaphore(3);

   private final ImageDAO imageDAO =
      new ImageDAO();

   protected void doGet(
         HttpServletRequest request,
         HttpServletResponse response) {

      log("GET request started");

      try {

         OutputStream out =
            response.getOutputStream();

         File htmlFile =
            new File("Form.html");

         FileInputStream fileInputStream =
            new FileInputStream(htmlFile);

         String content =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html\r\n" +
            "Content-Length: " +
            htmlFile.length() + "\r\n" +
            "\r\n";

         out.write(content.getBytes());

         byte[] buffer =
            new byte[4096];

         int bytesRead;

         while ((bytesRead =
               fileInputStream.read(buffer)) != -1) {

            out.write(
               buffer,
               0,
               bytesRead
            );
         }

         fileInputStream.close();
         out.flush();

      } catch (Exception ex) {

         System.err.println(ex);

      } finally {

         log("GET request finished");
      }
   }

   protected void doPost(
         HttpServletRequest request,
         HttpServletResponse response) {

      boolean acquired = false;

      log("POST request started");

      try {

         uploadSemaphore.acquire();
         acquired = true;

         InputStream in =
            request.getInputStream();

         ByteArrayOutputStream baos =
            new ByteArrayOutputStream();

         String line;

         int contentLength = 0;

         String boundary = "";
         String caption = "";
         String date = "";
         String filename = "";

         while ((line = readRequestLine(in)) != null &&
                line.length() > 0) {

            String[] lineParts =
               line.split(" ");

            String header =
               lineParts[0];

            if (header.equals("Content-Type:")) {

               if (lineParts.length > 1 &&
                   lineParts[1].equals(
                      "multipart/form-data;")) {

                  String boundaryLine =
                     lineParts[2];

                  boundary =
                     boundaryLine.substring(
                        "boundary=".length()
                     );
               }
            }

            if (header.equals("Content-Length:")) {

               if (lineParts.length > 1) {

                  contentLength =
                     Integer.parseInt(
                        lineParts[1]
                     );
               }
            }
         }

         if (boundary.equals("")) {

            throw new UploadException(
               "Multipart boundary not found."
            );
         }

         boolean inFileContent = false;

         while (!inFileContent &&
                (line = readRequestLine(in)) != null) {

            contentLength -=
               line.length() + 2;

            String[] lineParts =
               line.split(" ");

            String header =
               lineParts[0];

            if (header.equals(
                  "Content-Disposition:")) {

               if (lineParts.length > 1 &&
                   lineParts[1].equals(
                      "form-data;")) {

                  if (lineParts.length == 3) {

                     String[] nameParts =
                        lineParts[2].split("=");

                     String field =
                        nameParts[1];

                     line =
                        readRequestLine(in);

                     contentLength -=
                        line.length() + 2;

                     line =
                        readRequestLine(in);

                     contentLength -=
                        line.length() + 2;

                     if (field.equals(
                           "\"caption\"")) {

                        caption = line;

                     } else if (field.equals(
                           "\"date\"")) {

                        date = line;
                     }

                  } else if (
                        lineParts.length == 4) {

                     String[] filenameParts =
                        lineParts[3].split("=");

                     filename =
                        filenameParts[1]
                           .substring(
                              1,
                              filenameParts[1]
                                 .length() - 1
                           );

                     line =
                        readRequestLine(in);

                     contentLength -=
                        line.length() + 2;

                     line =
                        readRequestLine(in);

                     contentLength -=
                        line.length() + 2;

                     inFileContent = true;
                  }
               }
            }
         }

         if (filename.equals("")) {

            throw new UploadException(
               "No file was uploaded."
            );
         }

         System.out.println(
            "CAPTION: " + caption
         );

         System.out.println(
            "DATE: " + date
         );

         System.out.println(
            "FILENAME: " + filename
         );

         String finalBoundary =
            "--" + boundary + "--";

         int endLength =
            finalBoundary.length() + 4;

         byte[] content =
            new byte[1];

         while (contentLength > endLength &&
                in.read(content, 0, 1) != -1) {

            contentLength--;

            baos.write(
               content,
               0,
               content.length
            );
         }

         String savedName =
            caption + "_" +
            date + "_" +
            filename;

         imageDAO.save(
            savedName,
            baos.toByteArray()
         );

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

   private void sendDirectoryListing(
         HttpServletResponse response)
         throws IOException {

      String[] files =
         imageDAO.getFiles();

      StringBuilder middlePart =
         new StringBuilder();

      Arrays.stream(files).forEach(file -> {
         middlePart.append("<li>");
         middlePart.append(file);
         middlePart.append("</li>");
      });

      String topPart =
         "<!DOCTYPE html><html><body><ul>";

      String bottomPart =
         "</ul></body></html>";

      String html =
         topPart +
         middlePart +
         bottomPart;

      OutputStream out =
         response.getOutputStream();

      out.write(
         "HTTP/1.1 200 OK\r\n".getBytes()
      );

      out.write(
         "Content-Type: text/html\r\n"
            .getBytes()
      );

      out.write(
         ("Content-Length: " +
          html.getBytes().length +
          "\r\n").getBytes()
      );

      out.write("\r\n".getBytes());

      out.write(html.getBytes());

      out.flush();
   }

   private String readRequestLine(
         InputStream is) {

      char c;
      String s = "";
      boolean hitReturn = false;

      do {

         try {

            c = (char) is.read();

         } catch (Exception e) {

            return null;
         }

         if (c == '\r') {

            hitReturn = true;

         } else if (hitReturn &&
                    c == '\n') {

            return s;

         } else {

            s += c + "";
         }

      } while (c != -1);

      if (s.length() > 0) {
         return s;
      }

      return null;
   }

   private void log(String message) {

      System.out.println(
         "[UploadServer] " + message
      );
   }
}