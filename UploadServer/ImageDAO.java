import java.io.*;
import java.util.Arrays;

public class ImageDAO {

   private final File imagesDir = new File("images");

   public void save(String fileName, byte[] data)
         throws IOException {

      if (!imagesDir.exists()) {
         imagesDir.mkdir();
      }

      FileOutputStream out =
         new FileOutputStream(new File(imagesDir, fileName));

      out.write(data);
      out.close();
   }

   public String[] getFiles() {

      String[] files = imagesDir.list();

      if (files == null) {
         return new String[0];
      }

      Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);

      return files;
   }

   public File getDirectory() {
      return imagesDir;
   }
}