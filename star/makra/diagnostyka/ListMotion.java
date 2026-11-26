package macro;
import star.common.*;import star.motion.*;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
public class ListMotion extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();List<String> L=new ArrayList<>();
  try{
   RotatingMotion rm=s.get(MotionManager.class).createMotion(RotatingMotion.class,"Rotation");
   for(Method m:rm.getClass().getMethods()){L.add(m.getName()+Arrays.toString(m.getParameterTypes())+" -> "+m.getReturnType().getName());
    if(m.getParameterCount()==0&&m.getName().startsWith("get")&&!m.getReturnType().isPrimitive())
     try{Object o=m.invoke(rm);if(o!=null)L.add("   = "+o.getClass().getName()+" : "+o);}catch(Throwable e){}}
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"MOTION_API.txt"),L);}catch(Exception e){}
 }
}
