package macro;
import star.common.*;import java.lang.reflect.*;import java.nio.file.*;
public class ListIso extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();StringBuilder b=new StringBuilder();
  for(Method m:s.getPartManager().getClass().getMethods())if(m.getName().toLowerCase().contains("iso"))b.append(m.toString()).append("\n");
  try{Class<?> c=Class.forName("star.vis.IsoPart");for(Method m:c.getMethods())if(m.getDeclaringClass()==c)b.append("IsoPart: ").append(m.toString()).append("\n");}catch(Throwable e){b.append(e).append("\n");}
  try{Class<?> c=Class.forName("star.vis.SingleIsoValue");for(Method m:c.getMethods())if(m.getDeclaringClass()==c)b.append("SingleIsoValue: ").append(m.toString()).append("\n");}catch(Throwable e){b.append(e).append("\n");}
  try{Files.writeString(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"ISO_API.txt"),b.toString());}catch(Exception e){}
 }
}
