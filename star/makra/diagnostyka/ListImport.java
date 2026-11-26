package macro;
import star.common.*;import star.meshing.*;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
public class ListImport extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();List<String> L=new ArrayList<>();
  try{Object m=s.get(PartImportManager.class);
   for(Method mt:m.getClass().getMethods())if(mt.getName().toLowerCase().contains("stl")||mt.getName().toLowerCase().contains("surface"))L.add(mt.getName()+Arrays.toString(mt.getParameterTypes()));
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"IMPORT_API.txt"),L);}catch(Exception e){}
 }
}
