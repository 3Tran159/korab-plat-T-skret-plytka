package macro;
import star.common.*;import star.post.*;import java.nio.file.*;import java.lang.reflect.*;
public class ProbeHist extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();Path out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));StringBuilder b=new StringBuilder();
  try{
  Region reg=s.getRegionManager().getRegions().iterator().next();b.append("region "+reg.getPresentationName()+"\n");
  SolutionHistory sh=s.get(SolutionHistoryManager.class).createForFile(out.resolve("probe.simh").toAbsolutePath().toString(),false);
  for(Method m:sh.getClass().getMethods())if(m.getName().matches(".*(Input|Region|Part|Object|Function|Update|Record).*")&&m.getDeclaringClass()!=Object.class)b.append("SH: "+m+"\n");
  Object in=sh.getInputs();b.append("inputs class "+in.getClass()+"\n");
  for(Method m:in.getClass().getMethods())if(m.getName().matches("(set|get|add).*"))b.append("IN: "+m+"\n");
  sh.getInputs().setQuery(null);sh.getInputs().setObjects(reg);b.append("after setObjects: "+sh.getInputs().getObjects()+"\n");
  try{sh.getInputs().addObjects(reg);b.append("after add: "+sh.getInputs().getObjects()+"\n");}catch(Throwable e){b.append("add "+e+"\n");}
  }catch(Throwable e){b.append("ERR "+e+"\n");}
  try{Files.writeString(out.resolve("PROBE_HIST.txt"),b.toString());}catch(Exception e){}
 }
}
