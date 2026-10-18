import net.neoforged.fml.startup.Entrypoint;
import net.neoforged.api.distmarker.Dist;
public final class ClientMixinProbe extends Entrypoint {
 public static void main(String[] args) throws Exception {
  try(var result = startup(new String[0], true, Dist.CLIENT, true)) {
   var loader = result.loader().getCurrentClassLoader();
   for(String name: args) {
    Class.forName(name, false, loader).getDeclaredMethods();
    System.out.println("CLIENT MIXIN TARGET PASS: " + name);
   }
  }
 }
}
