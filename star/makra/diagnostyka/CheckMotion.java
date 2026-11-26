package macro;
import star.common.*;import star.base.neo.*;import star.motion.*;
import java.nio.file.*;import java.util.*;
public class CheckMotion extends StarMacro {
 public void execute(){
  Simulation s=getActiveSimulation();List<String> L=new ArrayList<>();
  try{
   for(Object o:s.get(MotionManager.class).getObjects()){L.add("motion "+((NamedObject)o).getPresentationName()+" "+o.getClass().getSimpleName());
    if(o instanceof RotatingMotion){RotatingMotion rm=(RotatingMotion)o;
     L.add(" origin="+rm.getAxisOrigin()+" dir="+rm.getAxisDirection());
     try{L.add(" origin def="+rm.getAxisOrigin().getDefinition());}catch(Throwable e){L.add(" "+e);}
     try{L.add(" rate def="+((RotationRate)rm.getRotationSpecification()).getRotationRate().getDefinition());}catch(Throwable e){L.add(" rate "+e);}}}
   for(Region r:s.getRegionManager().getRegions())L.add("region "+r.getPresentationName()+" motion="+r.getValues().get(MotionSpecification.class).getMotion());
  }catch(Throwable e){L.add("ERROR "+e);}
  try{Files.write(Paths.get(System.getenv().getOrDefault("MINI_CASE_DIR","."),"MOTION_CHECK.txt"),L);}catch(Exception e){}
 }
}
