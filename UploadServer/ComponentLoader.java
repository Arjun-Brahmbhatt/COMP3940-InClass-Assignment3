public class ComponentLoader {

   public static HttpServlet loadServlet(String className)
         throws Exception {

      Class<?> componentClass =
         Class.forName(className);

      return (HttpServlet)
         componentClass
            .getMethod("getInstance")
            .invoke(null);
   }
}