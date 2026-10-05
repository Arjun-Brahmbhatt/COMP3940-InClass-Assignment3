public class ComponentLoader {

   public static HttpServlet loadServlet(String className)
         throws Exception {

      Class<?> servletClass =
         Class.forName(className);

      servletClass.getDeclaredConstructor();

      return UploadServletSingleton.getInstance();
   }
}