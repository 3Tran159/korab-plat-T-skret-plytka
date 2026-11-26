package macro;
import star.common.*;import java.lang.reflect.*;import java.nio.file.*;
public class ListStream extends StarMacro {
 void dump(StringBuilder b,String cn,boolean own){try{Class<?> c=Class.forName(cn);for(Method m:c.getMethods())if(!own||m.getDeclaringClass()==c)b.append(c.getSimpleName()).append(": ").append(m.toString().replace("public ","")).append("\n");}catch(Throwable e){b.append(cn+" "+e+"\n");}}
 public void execute(){
  StringBuilder b=new StringBuilder();Simulation s=getActiveSimulation();
  for(Method m:s.getPartManager().getClass().getMethods())if(m.getName().matches("create(Stream|Line|Point|Seed).*"))b.append("PM: ").append(m.toString()).append("\n");
  for(String cn:new String[]{"star.vis.StreamPart","star.vis.StreamDisplayer","star.vis.StreamDisplayerManager","star.common.LinePart","star.vis.LinePart","star.vis.StreamSeedPart"})dump(b,cn,true);
  for(Method m:star.vis.DisplayerManager.class.getMethods())if(m.getName().contains("Stream"))b.append("DM: ").append(m.toString()).append("\n");
  try{Files.writeString(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"STREAM_API.txt"),b.toString());}catch(Exception e){}
 }
}
