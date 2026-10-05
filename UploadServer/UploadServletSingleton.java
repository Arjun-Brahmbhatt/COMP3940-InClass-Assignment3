public class UploadServletSingleton {

   private static final UploadServlet instance =
      new UploadServlet();

   private UploadServletSingleton() {
   }

   public static UploadServlet getInstance() {
      return instance;
   }
}