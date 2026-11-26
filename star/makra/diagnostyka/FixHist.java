package macro;
import star.common.*;import star.post.*;import java.nio.file.*;import java.lang.reflect.*;
public class FixHist extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();Path out=Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."));StringBuilder b=new StringBuilder();
  Region reg=s.getRegionManager().getRegion("Subtract");
  for(Object o:s.get(SolutionHistoryManager.class).getObjects()){SolutionHistory sh=(SolutionHistory)o;
   for(Method m:sh.getInputs().getClass().getMethods())if(m.getName().equals("setQuery")&&m.getParameterCount()==1){try{m.invoke(sh.getInputs(),new Object[]{null});b.append("setQuery ok\n");}catch(Throwable e){b.append("setQuery "+e+"\n");}}
   sh.getRegions().setQuery(null);sh.getRegions().setObjects(reg);b.append(sh.getPresentationName()+" regions="+sh.getRegions().getObjects()+" inputs="+sh.getInputs().getObjects()+" path="+sh.getPath()+"\n");}
  s.saveState(out.resolve("Y04_SETUP.sim").toString());
  try{Files.writeString(out.resolve("FIXHIST.txt"),b.toString());}catch(Exception e){}
 }
}
